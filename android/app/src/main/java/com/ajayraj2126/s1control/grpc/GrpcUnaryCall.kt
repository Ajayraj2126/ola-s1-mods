package com.ajayraj2126.s1control.grpc

import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.Status
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Await exactly one response and a successful final status without blocking a thread.
 * The caller supplies a call with its deadline already configured in CallOptions.
 */
internal suspend fun <Request : Any, Response : Any> awaitUnaryCall(
    call: ClientCall<Request, Response>,
    request: Request
): Response = suspendCancellableCoroutine { continuation ->
    val finished = AtomicBoolean(false)

    fun cancelCall(message: String, cause: Throwable?) {
        try {
            call.cancel(message, cause)
        } catch (_: Exception) {
            // A closing channel must not replace the original failure/cancellation.
        }
    }

    fun fail(failure: Throwable, cancelMessage: String? = null) {
        if (finished.compareAndSet(false, true)) {
            continuation.resumeWithException(failure)
            if (cancelMessage != null) cancelCall(cancelMessage, failure)
        }
    }

    continuation.invokeOnCancellation { cause ->
        if (finished.compareAndSet(false, true)) {
            cancelCall("Coroutine cancelled unary RPC", cause)
        }
    }

    if (!continuation.isActive) return@suspendCancellableCoroutine

    // gRPC serializes listener callbacks. Cancellation can race on another thread,
    // so terminal completion is guarded separately from the response buffer.
    val listener = object : ClientCall.Listener<Response>() {
        private var response: Response? = null

        override fun onMessage(message: Response) {
            if (finished.get()) return
            if (response != null) {
                fail(
                    Status.INTERNAL.withDescription("Unary RPC received more than one response")
                        .asRuntimeException(),
                    "Too many responses for unary RPC"
                )
            } else {
                response = message
            }
        }

        override fun onClose(status: Status, trailers: Metadata) {
            if (!status.isOk) {
                fail(status.asRuntimeException(trailers))
                return
            }
            val value = response
            if (value == null) {
                fail(
                    Status.INTERNAL.withDescription("Unary RPC completed without a response")
                        .asRuntimeException(trailers)
                )
            } else if (finished.compareAndSet(false, true)) {
                // A message alone is insufficient: its final status can still fail.
                continuation.resume(value)
            }
        }
    }

    try {
        call.start(listener, Metadata())
        if (!continuation.isActive || finished.get()) return@suspendCancellableCoroutine
        // Requesting two allows us to detect a malformed multi-message unary reply.
        call.request(2)
        if (!continuation.isActive || finished.get()) return@suspendCancellableCoroutine
        call.sendMessage(request)
        if (!continuation.isActive || finished.get()) return@suspendCancellableCoroutine
        call.halfClose()
    } catch (failure: Throwable) {
        fail(failure, "Failed to start unary RPC")
    }
}
