package com.ajayraj2126.s1control.model

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionSessionTest {
    @Test fun networkLossKeepsReconnectPermissionAndSavedEndpoint() = runBlocking {
        val persistence = MemoryPersistence()
        var closed = 0
        val session = ConnectionSession(
            persistence = persistence,
            handshake = { "client:${it.host}" },
            closeClient = { closed++ },
            discover = { null }
        )

        session.connect(SessionEndpoint("192.168.43.10", 4))
        session.networkUnavailable()

        assertTrue(session.state.value is SessionState.WaitingForNetwork)
        assertNull(session.client)
        assertTrue(session.reconnectAllowed())
        assertEquals(SessionEndpoint("192.168.43.10", 4), persistence.endpoint)
        assertEquals(1, closed)
        assertTrue(session.reconnect())
        assertTrue(session.state.value is SessionState.Connected)
    }

    @Test fun manualDisconnectSuppressesAutomaticReconnect() = runBlocking {
        val persistence = MemoryPersistence()
        var handshakes = 0
        val session = ConnectionSession(
            persistence = persistence,
            handshake = { handshakes++; Any() },
            closeClient = {},
            discover = { SessionEndpoint("discovered") }
        )

        session.connect(SessionEndpoint("saved"))
        session.disconnect()
        session.networkUnavailable()

        assertFalse(session.reconnectAllowed())
        assertFalse(session.reconnect())
        assertEquals(1, handshakes)
        assertTrue(session.state.value is SessionState.Disconnected)
    }

    @Test fun reconnectTriesSavedEndpointThenOneDiscovery() = runBlocking {
        val persistence = MemoryPersistence(SessionEndpoint("stale"), true)
        val attempted = mutableListOf<String>()
        val session = ConnectionSession(
            persistence = persistence,
            handshake = {
                attempted += it.host
                if (it.host == "stale") error("unreachable")
                "connected"
            },
            closeClient = {},
            discover = { SessionEndpoint("fresh") }
        )

        assertTrue(session.reconnect())
        assertEquals(listOf("stale", "fresh"), attempted)
        assertEquals(SessionEndpoint("fresh"), persistence.endpoint)
    }

    private class MemoryPersistence(
        var endpoint: SessionEndpoint? = null,
        private var allowed: Boolean = false
    ) : SessionPersistence {
        override fun loadEndpoint() = endpoint
        override fun saveEndpoint(endpoint: SessionEndpoint) { this.endpoint = endpoint }
        override fun isReconnectAllowed() = allowed
        override fun setReconnectAllowed(allowed: Boolean) { this.allowed = allowed }
    }
}
