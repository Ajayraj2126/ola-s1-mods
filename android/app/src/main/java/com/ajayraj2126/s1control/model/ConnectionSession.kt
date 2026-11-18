package com.ajayraj2126.s1control.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicLong

data class SessionEndpoint(val host: String, val scope: Int? = null)

sealed interface SessionState {
    data object Disconnected : SessionState
    data class WaitingForNetwork(val endpoint: SessionEndpoint?) : SessionState
    data class Connecting(val endpoint: SessionEndpoint) : SessionState
    data class Connected(val endpoint: SessionEndpoint) : SessionState
    data class Reconnecting(val endpoint: SessionEndpoint?) : SessionState
    data class Failed(val message: String) : SessionState
}

/** Stores only connection preferences. No pending command or mutation is persisted. */
interface SessionPersistence {
    fun loadEndpoint(): SessionEndpoint?
    fun saveEndpoint(endpoint: SessionEndpoint)
    fun isReconnectAllowed(): Boolean
    fun setReconnectAllowed(allowed: Boolean)
}

/**
 * A verified diagnostic session, independent of Android and any vehicle mutations.
 * A resume makes at most two handshakes: the saved endpoint, then one discovery result.
 */
class ConnectionSession<C : Any>(
    private val persistence: SessionPersistence,
    private val handshake: suspend (SessionEndpoint) -> C,
    private val closeClient: (C) -> Unit,
    private val discover: suspend () -> SessionEndpoint?,
    private val handshakeTimeoutMs: Long = 8_000,
    private val discoveryTimeoutMs: Long = 12_000,
    private val onStateChanged: (SessionState) -> Unit = {}
) {
    private val connectionMutex = Mutex()
    private val generation = AtomicLong()
    private val publicationLock = Any()
    private val _state = MutableStateFlow<SessionState>(SessionState.Disconnected)
    val state = _state.asStateFlow()

    @Volatile private var stopped = false
    @Volatile var client: C? = null
        private set
    @Volatile var endpoint: SessionEndpoint? = persistence.loadEndpoint()
        private set

    private fun publish(state: SessionState) {
        _state.value = state
        onStateChanged(state)
    }

    private fun closeCurrent() {
        val previous = client
        client = null
        previous?.let(closeClient)
    }

    private suspend fun establish(target: SessionEndpoint, requestGeneration: Long): C {
        require(target.host.isNotBlank()) { "Enter a scooter address first" }
        val candidate = try {
            withTimeout(handshakeTimeoutMs) { handshake(target) }
        } catch (timeout: TimeoutCancellationException) {
            throw IllegalStateException("Diagnostic handshake timed out at ${target.host}", timeout)
        }
        synchronized(publicationLock) {
            if (stopped || generation.get() != requestGeneration) {
                closeClient(candidate)
                throw CancellationException("Connection request was cancelled")
            }
            client = candidate
            endpoint = target
            persistence.saveEndpoint(target)
            publish(SessionState.Connected(target))
        }
        return candidate
    }

    suspend fun connect(target: SessionEndpoint): C = connectionMutex.withLock {
        check(!stopped) { "Session is closed" }
        val requestGeneration = generation.get()
        persistence.setReconnectAllowed(true)
        closeCurrent()
        publish(SessionState.Connecting(target))
        try {
            establish(target, requestGeneration)
        } catch (cancelled: CancellationException) {
            if (generation.get() == requestGeneration) publish(SessionState.Disconnected)
            throw cancelled
        } catch (failure: Exception) {
            if (generation.get() == requestGeneration) {
                publish(SessionState.Failed(failure.message ?: "Connection failed"))
            }
            throw failure
        }
    }

    /** Returns immediately if a connect is in progress or the user disconnected. */
    suspend fun reconnect(): Boolean {
        if (state.value is SessionState.Connected && client != null) return true
        if (!connectionMutex.tryLock()) return false
        try {
            if (stopped || !persistence.isReconnectAllowed()) return false
            val saved = endpoint ?: return false
            val requestGeneration = generation.get()
            closeCurrent()
            publish(SessionState.Reconnecting(saved))
            try {
                try {
                    establish(saved, requestGeneration)
                    return true
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // A stale hotspot address is expected; try one bounded discovery.
                }
                val found = try {
                    withTimeout(discoveryTimeoutMs) { discover() }
                } catch (timeout: TimeoutCancellationException) {
                    throw IllegalStateException("Scooter discovery timed out", timeout)
                } ?: throw IllegalStateException("Saved scooter is unavailable. Check the hotspot and scooter display.")
                establish(found, requestGeneration)
                return true
            } catch (cancelled: CancellationException) {
                if (generation.get() == requestGeneration) publish(SessionState.Disconnected)
                throw cancelled
            } catch (failure: Exception) {
                if (generation.get() == requestGeneration) {
                    publish(SessionState.Failed(failure.message ?: "Reconnect failed"))
                }
                return false
            }
        } finally {
            connectionMutex.unlock()
        }
    }

    /** Drop only the transport. Reconnect permission and the verified endpoint remain saved. */
    fun networkUnavailable() = synchronized(publicationLock) {
        if (stopped || !persistence.isReconnectAllowed()) return@synchronized
        generation.incrementAndGet()
        closeCurrent()
        publish(SessionState.WaitingForNetwork(endpoint))
    }

    fun reconnectAllowed(): Boolean = !stopped && persistence.isReconnectAllowed()

    /** An explicit disconnect remains respected across Activity and process recreation. */
    fun disconnect() = synchronized(publicationLock) {
        generation.incrementAndGet()
        persistence.setReconnectAllowed(false)
        closeCurrent()
        publish(SessionState.Disconnected)
    }

    /** Owner disposal releases resources, without changing the user's saved preference. */
    fun close() = synchronized(publicationLock) {
        stopped = true
        generation.incrementAndGet()
        closeCurrent()
        publish(SessionState.Disconnected)
    }
}
