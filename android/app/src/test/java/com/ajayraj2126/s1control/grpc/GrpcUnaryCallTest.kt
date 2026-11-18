package com.ajayraj2126.s1control.grpc

import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GrpcUnaryCallTest {
    @Test fun responseWaitsForSuccessfulFinalStatus() = runBlocking {
        val call = FakeCall()
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            awaitUnaryCall(call, "request")
        }
        assertEquals("request", call.sentMessage)
        assertEquals(2, call.requestedMessages)
        assertTrue(call.halfClosed)
        call.message("response")
        assertFalse(result.isCompleted)
        call.close(Status.OK)
        assertEquals("response", withTimeout(1_000) { result.await() })
        assertEquals(0, call.cancelCount)
    }

    @Test fun successfulCloseWithoutMessageFailsInsteadOfHanging() = runBlocking {
        val call = FakeCall().apply { whenHalfClosed = { close(Status.OK) } }
        val failure = runCatching {
            withTimeout(1_000) { awaitUnaryCall(call, "request") }
        }.exceptionOrNull()
        assertEquals(Status.Code.INTERNAL, Status.fromThrowable(failure!!).code)
        assertTrue(failure.message!!.contains("without a response"))
    }

    @Test fun failedFinalStatusIsNotHiddenByAnEarlierMessage() = runBlocking {
        val trailerKey = Metadata.Key.of("reason", Metadata.ASCII_STRING_MARSHALLER)
        val trailers = Metadata().apply { put(trailerKey, "retry-later") }
        val call = FakeCall().apply {
            whenHalfClosed = {
                message("partial response")
                close(Status.UNAVAILABLE.withDescription("connection lost"), trailers)
            }
        }
        val failure = runCatching { awaitUnaryCall(call, "request") }.exceptionOrNull()
            as StatusRuntimeException
        assertEquals(Status.Code.UNAVAILABLE, failure.status.code)
        assertEquals("connection lost", failure.status.description)
        assertEquals("retry-later", failure.trailers!!.get(trailerKey))
    }

    @Test fun deadlineStatusIsPropagated() = runBlocking {
        val call = FakeCall().apply {
            whenHalfClosed = { close(Status.DEADLINE_EXCEEDED.withDescription("deadline elapsed")) }
        }
        val failure = runCatching { awaitUnaryCall(call, "request") }.exceptionOrNull()
        assertEquals(Status.Code.DEADLINE_EXCEEDED, Status.fromThrowable(failure!!).code)
    }

    @Test fun multipleResponsesFailAndCancelTheCallOnce() = runBlocking {
        val call = FakeCall().apply {
            whenHalfClosed = {
                message("first")
                message("second")
                close(Status.OK)
            }
        }
        val failure = runCatching { awaitUnaryCall(call, "request") }.exceptionOrNull()
        assertEquals(Status.Code.INTERNAL, Status.fromThrowable(failure!!).code)
        assertEquals(1, call.cancelCount)
    }

    @Test fun coroutineCancellationCancelsTheRpcAndIgnoresLateCallbacks() = runBlocking {
        val call = FakeCall()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            awaitUnaryCall(call, "request")
        }
        withTimeout(1_000) { job.cancelAndJoin() }
        assertTrue(job.isCancelled)
        assertEquals(1, call.cancelCount)
        call.message("late response")
        call.close(Status.OK)
        call.close(Status.UNAVAILABLE)
        assertEquals(1, call.cancelCount)
    }

    @Test fun synchronousStartFailureIsPropagatedAndCancelsTheCall() = runBlocking {
        val expected = IllegalStateException("start failed")
        val call = FakeCall().apply { whenStarted = { throw expected } }
        val failure = runCatching { awaitUnaryCall(call, "request") }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertEquals(expected.message, failure?.message)
        assertEquals(1, call.cancelCount)
        assertEquals(null, call.sentMessage)
    }

    @Test fun synchronousSendFailureIsPropagatedAndCancelsTheCall() = runBlocking {
        val expected = IllegalStateException("send failed")
        val call = FakeCall().apply { whenSent = { throw expected } }
        val failure = runCatching { awaitUnaryCall(call, "request") }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertEquals(expected.message, failure?.message)
        assertEquals(1, call.cancelCount)
    }

    private class FakeCall : ClientCall<String, String>() {
        private lateinit var listener: Listener<String>
        var requestedMessages = 0
        var sentMessage: String? = null
        var halfClosed = false
        var cancelCount = 0
        var whenStarted: () -> Unit = {}
        var whenSent: () -> Unit = {}
        var whenHalfClosed: FakeCall.() -> Unit = {}

        override fun start(responseListener: Listener<String>, headers: Metadata) {
            listener = responseListener
            whenStarted()
        }

        override fun request(numMessages: Int) {
            requestedMessages += numMessages
        }

        override fun cancel(message: String?, cause: Throwable?) {
            cancelCount++
            if (::listener.isInitialized) {
                listener.onClose(Status.CANCELLED.withDescription(message).withCause(cause), Metadata())
            }
        }

        override fun halfClose() {
            halfClosed = true
            whenHalfClosed()
        }

        override fun sendMessage(message: String) {
            sentMessage = message
            whenSent()
        }

        fun message(message: String) = listener.onMessage(message)

        fun close(status: Status, trailers: Metadata = Metadata()) = listener.onClose(status, trailers)
    }
}
