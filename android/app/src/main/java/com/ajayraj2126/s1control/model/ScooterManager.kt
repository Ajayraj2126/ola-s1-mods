package com.ajayraj2126.s1control.model

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.graphics.Bitmap
import com.ajayraj2126.s1control.adb.AdbManager
import com.ajayraj2126.s1control.discovery.DiscoveredScooter
import com.ajayraj2126.s1control.discovery.NetworkScanner
import com.ajayraj2126.s1control.grpc.S1GrpcClient
import com.ajayraj2126.s1control.storage.ChangeJournalRepository
import com.ajayraj2126.s1control.storage.PreferenceChangeJournalStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

enum class SetupStep(val stepNum: Int, val title: String) {
    VERIFY_CONNECTION(1, "Detect Scooter & Connect"),
    UNLOCK_ADB(2, "In-Memory ADB Unlock"),
    INSTALL_APKS(3, "Install Selected Mod APKs"),
    GRANT_PERMISSIONS(4, "Grant System Permissions"),
    FINALIZE_HOME(5, "Initialize & Return to Home")
}

enum class ModApp(
    val id: String,
    val displayName: String,
    val assetFileName: String,
    val packageName: String,
    val isMandatory: Boolean,
    val description: String
) {
    BALLS(
        id = "balls",
        displayName = "Balls Navigation",
        assetFileName = "balls-1.1.0-lite-release.apk",
        packageName = "com.asjohn720.ball",
        isMandatory = true,
        description = "Floating nav buttons"
    ),
    ANDROID_AUTO(
        id = "android_auto",
        displayName = "Android Auto",
        assetFileName = "android-auto.apk",
        packageName = "com.asjohn720.androidauto",
        isMandatory = false,
        description = "Maps, music and calls"
    ),
    MUSIC(
        id = "music",
        displayName = "Music Mod",
        assetFileName = "musicmod-release.apk",
        packageName = "com.asjohn720.musicmod",
        isMandatory = false,
        description = "Ad-free music"
    )
}

data class ScooterInfo(
    val uid: String = "",
    val androidVersion: String = "",
    val buildId: String = "",
    val model: String = "",
    val scooterMode: String = ""
)

data class RamInfo(
    val totalMb: Int,
    val usedMb: Int,
    val freeMb: Int
)

/**
 * Minimal vehicle identity shown on the connect/setup card.
 * supported = MoveOS >= 4.0.5 (MoveOS 4.0.5+ and 5.x; older builds are not supported).
 */
data class VehicleSummary(
    val model: String,
    val moveOs: String,
    val majorVersion: Int?,
    val supported: Boolean
)

class ScooterManager(private val context: Context) {

    private val adbManager = AdbManager(context)
    private val journal = ChangeJournalRepository(PreferenceChangeJournalStore(context))
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutationMutex = Mutex()
    private var setupJob: Job? = null
    private var removalJob: Job? = null
    private var reconnectJob: Job? = null
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val wifiNetworks = ConcurrentHashMap.newKeySet<Network>()

    private val sessionPreferences = context.getSharedPreferences("aj_session_v1", Context.MODE_PRIVATE)
    private val sessionPersistence = object : SessionPersistence {
        override fun loadEndpoint(): SessionEndpoint? {
            val host = sessionPreferences.getString("last_host", null)?.takeIf { it.isNotBlank() } ?: return null
            val scope = sessionPreferences.getInt("last_scope", -1).takeIf { it >= 0 }
            return SessionEndpoint(host, scope)
        }
        override fun saveEndpoint(endpoint: SessionEndpoint) {
            check(sessionPreferences.edit().putString("last_host", endpoint.host)
                .putInt("last_scope", endpoint.scope ?: -1).commit()) { "Could not save scooter address" }
        }
        override fun isReconnectAllowed(): Boolean = sessionPreferences.getBoolean("reconnect_allowed", false)
        override fun setReconnectAllowed(allowed: Boolean) {
            check(sessionPreferences.edit().putBoolean("reconnect_allowed", allowed).commit()) {
                "Could not save reconnect preference"
            }
        }
    }
    private val savedEndpoint = sessionPersistence.loadEndpoint()

    val scooterIp = MutableStateFlow(savedEndpoint?.host.orEmpty())
    val scooterScope = MutableStateFlow(savedEndpoint?.scope)
    val isConnected = MutableStateFlow(false)

    val currentStep = MutableStateFlow<SetupStep?>(null)
    val isSetupRunning = MutableStateFlow(false)
    val setupSuccess = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)
    val isRemovalRunning = MutableStateFlow(false)
    val removalPreview = MutableStateFlow<RemovalPreview?>(null)
    val removalProgress = MutableStateFlow<RemovalProgress?>(null)
    val removalResult = MutableStateFlow<RemovalResult?>(null)

    // Last read model + MoveOS, shared by the connect card and the Setup "My Scooter" card.
    val vehicleSummary = MutableStateFlow<VehicleSummary?>(null)

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val session = ConnectionSession(
        persistence = sessionPersistence,
        handshake = { endpoint ->
            val client = S1GrpcClient(endpoint.host, 50005, endpoint.scope)
            try {
                client.connect(timeoutSeconds = 6L)
                client
            } catch (failure: Throwable) {
                client.close()
                throw failure
            }
        },
        closeClient = S1GrpcClient::close,
        discover = {
            NetworkScanner.scanForScooter(context)?.let { SessionEndpoint(it.ip, it.scope) }
        },
        onStateChanged = { state ->
            isConnected.value = state is SessionState.Connected
            when (state) {
                is SessionState.Connected -> {
                    scooterIp.value = state.endpoint.host
                    scooterScope.value = state.endpoint.scope
                }
                else -> Unit
            }
        }
    )
    val sessionState = session.state

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            wifiNetworks += network
            scheduleReconnect()
        }

        override fun onLost(network: Network) {
            wifiNetworks -= network
            if (wifiNetworks.isEmpty()) {
                reconnectJob?.cancel()
                session.networkUnavailable()
            }
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    /** Retry only the verified diagnostic handshake; never replay a scooter command. */
    private fun scheduleReconnect() {
        if (!session.reconnectAllowed() || isSetupRunning.value || isRemovalRunning.value ||
            sessionState.value is SessionState.Connected || reconnectJob?.isActive == true
        ) return
        reconnectJob = managerScope.launch {
            val delaysMs = longArrayOf(0, 2_000, 5_000, 10_000, 20_000, 30_000, 30_000, 30_000)
            for (delayMs in delaysMs) {
                if (!session.reconnectAllowed() || wifiNetworks.isEmpty()) return@launch
                if (delayMs > 0) delay(delayMs)
                if (session.reconnect()) {
                    runCatching { fetchVehicleSummary() }
                    return@launch
                }
            }
        }
    }

    fun log(msg: String) {
        _logs.value = _logs.value + msg
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    /** Initializes and verifies the active diagnostic connection. */
    suspend fun connect(
        ip: String = scooterIp.value,
        scope: Int? = scooterScope.value,
        @Suppress("UNUSED_PARAMETER") markConnected: Boolean = true
    ) = withContext(Dispatchers.IO) {
        log("[*] Connecting to vehicle at $ip (Scope: ${scope ?: "auto"}) ...")
        vehicleSummary.value = null
        session.connect(SessionEndpoint(ip, scope))
        log("[+] Successfully connected to MoveOS Diagnostics daemon (Port 50005)!")
    }

    /**
     * Returns a live gRPC client, building one from the saved address if needed.
     * A reachability test alone does not create a diagnostic session, so callers
     * that need gRPC establish and verify it here.
     */
    private suspend fun ensureGrpc(): S1GrpcClient = withContext(Dispatchers.IO) {
        session.client?.let { return@withContext it }
        if (scooterIp.value.isBlank()) throw IllegalStateException("Not connected to vehicle")
        connect()
        session.client ?: throw IllegalStateException("Not connected to vehicle")
    }

    /** Called from the foreground lifecycle. It never replays a scooter mutation. */
    fun onResume() {
        if (isSetupRunning.value || isRemovalRunning.value || sessionState.value is SessionState.Connected) return
        scheduleReconnect()
    }

    /** Setup execution survives navigation between Compose screens. */
    fun startFullSetup(selectedApps: Set<ModApp> = ModApp.values().toSet()): Boolean {
        if (setupJob?.isActive == true || removalJob?.isActive == true) return false
        setupJob = managerScope.launch { runFullSetup(selectedApps) }
        return true
    }

    /**
     * Auto-discovers the scooter on the local Wi-Fi / Hotspot network.
     */
    suspend fun autoDiscover(): DiscoveredScooter? = withContext(Dispatchers.IO) {
        log("[*] Scanning network for vehicle...")
        val discovered = NetworkScanner.scanForScooter(context)
        if (discovered != null) {
            scooterIp.value = discovered.ip
            scooterScope.value = discovered.scope
            log("[+] Discovered Vehicle: ${discovered.ip} (Scope: ${discovered.scope ?: "none"}, Source: ${discovered.source})")
        } else {
            log("[!] Could not auto-detect vehicle on port 50005.")
            log("    Tip: Check 'Connected Devices' in your Hotspot settings and enter the MAC or IP.")
        }
        discovered
    }

    /**
     * Quick test to verify if port 50005 is reachable.
     */
    suspend fun testConnection(ip: String = scooterIp.value, scope: Int? = scooterScope.value): Boolean = withContext(Dispatchers.IO) {
        log("[*] Testing port 50005 on $ip...")
        val open = NetworkScanner.testPort(ip, 50005, scope, 1000)
        if (open) {
            log("[+] Port 50005 is OPEN and responsive!")
        } else {
            log("[!] Port 50005 is unreachable on $ip. Ensure scooter screen is awake!")
        }
        open
    }

    /**
     * Executes the complete 1-Click Automated Setup Wizard.
     * @param selectedApps The set of applications chosen to be installed.
     */
    suspend fun runFullSetup(selectedApps: Set<ModApp> = ModApp.values().toSet()) = withContext(Dispatchers.IO) {
        mutationMutex.withLock {
        isSetupRunning.value = true
        setupSuccess.value = false
        errorMessage.value = null
        clearLogs()

        log("==================================================")
        log("       S1MODS AUTOMATED INSTALLATION WIZARD       ")
        log("==================================================")
        log("[*] Selected apps to install: ${selectedApps.joinToString { it.displayName }}")

        try {
            // Step 1: Verify Connection
            currentStep.value = SetupStep.VERIFY_CONNECTION
            log("\n[1/5] Verifying connection to vehicle...")
            if (scooterIp.value.isBlank()) {
                log("[*] No address entered. Attempting auto-discovery...")
                val found = autoDiscover()
                if (found == null || scooterIp.value.isBlank()) {
                    throw IllegalStateException("No vehicle detected. Please tap 'Auto Scan' or use Manual Mode to enter your scooter's IP/MAC address.")
                }
            }
            if (session.client == null || !isConnected.value) {
                connect()
            }
            val client = ensureGrpc()
            val deviceId = deviceId(client)
            log("[+] gRPC link confirmed alive.")

            // Step 2: Unlock ADB (In-Memory RAM Bind Mount)
            currentStep.value = SetupStep.UNLOCK_ADB
            log("\n[2/5] Authorizing ADB key via in-memory RAM mount...")
            val modeAddress = SettingAddress(SettingKind.PROPERTY, "persist.ola.security.scootermode")
            val originalMode = readSetting(client, modeAddress)
            journal.beforeSetting(deviceId, modeAddress, originalMode, "serv")
            val (_, pubKey) = adbManager.getOrCreateAdbKeyPair()
            val unlocked = client.unlockAdb(pubKey)
            journal.afterSetting(deviceId, modeAddress, readSetting(client, modeAddress))
            if (!unlocked) {
                log("[*] Warning: bind mount script returned non-zero, trying ADB connect...")
            }
            log("[*] Connecting Kotlin ADB client over TCP Port 5555...")
            adbManager.connect(scooterIp.value, 5555)
            log("[+] ADB connected successfully as 'device'!")

            // Step 3: Extract and Install Chosen APKs
            currentStep.value = SetupStep.INSTALL_APKS
            log("\n[3/5] Installing selected Mod APKs...")

            for (app in selectedApps) {
                val before = adbManager.observePackage(app.packageName)
                journal.beforePackage(deviceId, app.packageName, app.displayName, before.installed)
                if (before.installed) {
                    log("[*] ${app.displayName} already exists. Updating in place to preserve its app data.")
                } else {
                    log("[*] ${app.displayName} not found on vehicle. Direct fresh install...")
                }
                installAssetApk(app.assetFileName, app.displayName)
                val after = adbManager.observePackage(app.packageName)
                check(after.installed) { "${app.displayName} installation could not be verified" }
                journal.afterPackage(deviceId, app.packageName, after.fingerprint)
            }

            // Step 4: Grant Permissions
            currentStep.value = SetupStep.GRANT_PERMISSIONS
            log("\n[4/5] Granting required permissions via ADB...")
            if (selectedApps.contains(ModApp.BALLS)) {
                log("[*] Configuring Balls Navigation (Overlay, Settings & Accessibility)...")
                recordBallSettingsBefore(deviceId)
                recordAccessBefore(deviceId, client, ModApp.BALLS.packageName, appOps = ballAppOps)
                adbManager.grantBallPermissions()
                recordBallSettingsAfter(deviceId)
                recordAccessAfter(deviceId, client, ModApp.BALLS.packageName, appOps = ballAppOps)
                log("[+] Balls Navigation permissions granted & initialized.")
            }

            if (selectedApps.contains(ModApp.ANDROID_AUTO)) {
                log("[*] Configuring Android Auto permissions (Microphone, Location, Overlay, Storage)...")
                recordAccessBefore(
                    deviceId, client, ModApp.ANDROID_AUTO.packageName,
                    appOps = androidAutoAppOps, permissions = androidAutoPermissions
                )
                adbManager.grantAndroidAutoPermissions()
                recordAccessAfter(
                    deviceId, client, ModApp.ANDROID_AUTO.packageName,
                    appOps = androidAutoAppOps, permissions = androidAutoPermissions
                )
                log("[+] Android Auto permissions granted.")
            }

            // Step 5: Finalize & Return to Home
            currentStep.value = SetupStep.FINALIZE_HOME
            log("\n[5/5] Returning vehicle to Dashboard Home...")
            openDashboardHome()

            log("\n==================================================")
            log("     CONGRATULATIONS! ALL MODS INSTALLED!         ")
            log("==================================================")
            log("Installation complete. Your selected apps are ready on screen.")
            log("[*] Note: Use mod apps with the scooter stand UP. Lowering the side stand triggers auto-lock.")
            setupSuccess.value = true

        } catch (e: Exception) {
            val err = e.localizedMessage ?: e.toString()
            log("\n[!] Setup failed: $err")
            errorMessage.value = err
            setupSuccess.value = false
        } finally {
            isSetupRunning.value = false
        }
        }
    }

    /**
     * Extracts an APK bundled in assets to temporary storage and installs it via ADB.
     */
    private suspend fun installAssetApk(assetFileName: String, friendlyName: String) = withContext(Dispatchers.IO) {
        log("[*] Preparing $friendlyName...")
        val tempFile = File(context.cacheDir, assetFileName)
        context.assets.open(assetFileName).use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        val sizeMb = String.format("%.1f", tempFile.length() / (1024.0 * 1024.0))
        log("    Streaming $assetFileName ($sizeMb MB) via ADB...")
        val result = adbManager.installApk(tempFile)
        tempFile.delete()
        log("    [+] $friendlyName result: $result")
    }

    private suspend fun deviceId(client: S1GrpcClient): String {
        val serial = client.shell("getprop ro.serialno", 8L).second.trim().takeUnless { it == "null" }
        val androidId = client.shell("settings get secure android_id", 8L).second.trim().takeUnless { it == "null" }
        val identity = listOfNotNull(serial?.takeIf(String::isNotBlank), androidId?.takeIf(String::isNotBlank))
            .joinToString("|")
        check(identity.isNotBlank()) { "Vehicle identity is unavailable; AJ will not make untracked changes." }
        return MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private val servicesAddress = SettingAddress(
        SettingKind.SETTING, "enabled_accessibility_services", namespace = "secure"
    )
    private val accessibilityAddress = SettingAddress(
        SettingKind.SETTING, "accessibility_enabled", namespace = "secure"
    )
    private val ballService = "com.asjohn720.ball/com.asjohn720.ball.core.QuickBallService"
    private val ballAppOps = listOf("SYSTEM_ALERT_WINDOW", "WRITE_SETTINGS")
    private val androidAutoAppOps = listOf("SYSTEM_ALERT_WINDOW", "WRITE_SETTINGS", "MANAGE_EXTERNAL_STORAGE")
    private val androidAutoPermissions = listOf(
        "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.NEARBY_WIFI_DEVICES",
        "android.permission.BLUETOOTH_CONNECT",
        "android.permission.BLUETOOTH_SCAN"
    )

    private suspend fun recordBallSettingsBefore(deviceId: String) {
        val originalServices = adbManager.readSecureSetting(servicesAddress.name)
        val originalEnabled = adbManager.readSecureSetting(accessibilityAddress.name)
        val appliedServices = ChangeJournalPlanner.mergeAccessibilityServices(originalServices, ballService)
        journal.beforeSetting(deviceId, servicesAddress, originalServices, appliedServices)
        journal.beforeSetting(deviceId, accessibilityAddress, originalEnabled, "1")
    }

    private suspend fun recordBallSettingsAfter(deviceId: String) {
        journal.afterSetting(deviceId, servicesAddress, adbManager.readSecureSetting(servicesAddress.name))
        journal.afterSetting(deviceId, accessibilityAddress, adbManager.readSecureSetting(accessibilityAddress.name))
    }

    private suspend fun readSetting(client: S1GrpcClient, address: SettingAddress): String? {
        val command = when (address.kind) {
            SettingKind.PROPERTY -> "getprop ${address.name}"
            SettingKind.SETTING -> "settings get ${address.namespace} ${address.name}"
            SettingKind.APP_OP -> "appops get ${address.packageName} ${address.name}"
            SettingKind.PERMISSION -> "pm check-permission ${address.name} ${address.packageName}"
        }
        val output = client.shell(command, 8L).second.trim()
        return when (address.kind) {
            SettingKind.APP_OP -> Regex(""":\s*([A-Za-z_]+)""").find(output)?.groupValues?.get(1) ?: "default"
            SettingKind.PERMISSION -> when {
                output.contains("granted", true) -> "granted"
                output.contains("denied", true) -> "denied"
                else -> null
            }
            else -> output.takeUnless { it.isBlank() || it == "null" }
        }
    }

    private suspend fun recordAccessBefore(
        deviceId: String,
        client: S1GrpcClient,
        packageName: String,
        appOps: List<String> = emptyList(),
        permissions: List<String> = emptyList()
    ) {
        appOps.forEach { name ->
            val address = SettingAddress(SettingKind.APP_OP, name, packageName = packageName)
            journal.beforeSetting(deviceId, address, readSetting(client, address), "allow")
        }
        permissions.forEach { name ->
            val address = SettingAddress(SettingKind.PERMISSION, name, packageName = packageName)
            journal.beforeSetting(deviceId, address, readSetting(client, address), "granted")
        }
    }

    private suspend fun recordAccessAfter(
        deviceId: String,
        client: S1GrpcClient,
        packageName: String,
        appOps: List<String> = emptyList(),
        permissions: List<String> = emptyList()
    ) {
        appOps.forEach { name ->
            val address = SettingAddress(SettingKind.APP_OP, name, packageName = packageName)
            journal.afterSetting(deviceId, address, readSetting(client, address))
        }
        permissions.forEach { name ->
            val address = SettingAddress(SettingKind.PERMISSION, name, packageName = packageName)
            journal.afterSetting(deviceId, address, readSetting(client, address))
        }
    }

    private suspend fun observePackage(client: S1GrpcClient, packageName: String): PackageObservation {
        require(packageName.matches(Regex("[A-Za-z0-9_.]+")))
        val installed = client.shell("pm list packages $packageName", 10L).second.lineSequence()
            .any { it.trim() == "package:$packageName" }
        if (!installed) return PackageObservation(false)
        val dump = client.shell("dumpsys package $packageName", 15L).second
        val version = Regex("""versionCode=(\d+)""").find(dump)?.groupValues?.get(1)
        val updated = Regex("""lastUpdateTime=([^\r\n]+)""").find(dump)?.groupValues?.get(1)?.trim()
        return PackageObservation(true, if (version != null && updated != null) PackageFingerprint(version, updated) else null)
    }

    /**
     * Ensures ADB session is unlocked and connected.
     */
    suspend fun ensureAdb(): Boolean = withContext(Dispatchers.IO) {
        if (session.client == null || !isConnected.value) {
            connect()
        }
        val client = ensureGrpc()
        val id = deviceId(client)
        val modeAddress = SettingAddress(SettingKind.PROPERTY, "persist.ola.security.scootermode")
        val originalMode = readSetting(client, modeAddress)
        journal.beforeSetting(id, modeAddress, originalMode, "serv")
        val (_, pubKey) = adbManager.getOrCreateAdbKeyPair()
        client.unlockAdb(pubKey)
        journal.afterSetting(id, modeAddress, readSetting(client, modeAddress))
        adbManager.connect(scooterIp.value, 5555)
        true
    }

    /**
     * Checks all currently installed packages on the vehicle.
     */
    suspend fun fetchInstalledPackages(): Set<String> = withContext(Dispatchers.IO) {
        val packages = mutableSetOf<String>()
        try {
            if (session.client != null && isConnected.value) {
                val (_, out) = session.client!!.shell("pm list packages", 10L)
                out.lines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("package:")) {
                        packages.add(trimmed.removePrefix("package:").trim())
                    }
                }
            } else if (isConnected.value) {
                val out = adbManager.shell("pm list packages")
                out.lines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("package:")) {
                        packages.add(trimmed.removePrefix("package:").trim())
                    }
                }
            }
            if (packages.isNotEmpty()) {
                log("[+] Checked packages: ${packages.size} packages installed on vehicle.")
            }
        } catch (e: Exception) {
            log("[!] Could not inspect packages: ${e.message}")
        }
        packages
    }

    /**
     * Installs a single bundled ModApp from assets and automatically applies its permissions.
     */
    suspend fun installSingleModApp(app: ModApp): String = withContext(Dispatchers.IO) {
        mutationMutex.withLock {
        log("[*] Installing ${app.displayName} (${app.packageName})...")
        val client = ensureGrpc()
        val id = deviceId(client)
        ensureAdb()

        val before = adbManager.observePackage(app.packageName)
        journal.beforePackage(id, app.packageName, app.displayName, before.installed)
        if (before.installed) {
            log("[*] Existing installation found; updating in place without deleting app data.")
        }

        installAssetApk(app.assetFileName, app.displayName)
        val after = adbManager.observePackage(app.packageName)
        check(after.installed) { "${app.displayName} installation could not be verified" }
        journal.afterPackage(id, app.packageName, after.fingerprint)

        // Automatically apply required permissions
        when (app) {
            ModApp.BALLS -> {
                log("[*] Configuring Balls Navigation overlay & permissions...")
                recordBallSettingsBefore(id)
                recordAccessBefore(id, client, app.packageName, appOps = ballAppOps)
                adbManager.grantBallPermissions()
                recordBallSettingsAfter(id)
                recordAccessAfter(id, client, app.packageName, appOps = ballAppOps)
                log("[+] Balls permissions configured.")
            }
            ModApp.ANDROID_AUTO -> {
                log("[*] Configuring Android Auto permissions...")
                recordAccessBefore(id, client, app.packageName, androidAutoAppOps, androidAutoPermissions)
                adbManager.grantAndroidAutoPermissions()
                recordAccessAfter(id, client, app.packageName, androidAutoAppOps, androidAutoPermissions)
                log("[+] Android Auto permissions configured.")
            }
            ModApp.MUSIC -> {
                log("[*] Configuring Music Mod permissions...")
                val musicPermissions = listOf("android.permission.READ_EXTERNAL_STORAGE")
                recordAccessBefore(id, client, app.packageName, permissions = musicPermissions)
                try {
                    adbManager.shell("pm grant ${app.packageName} android.permission.READ_EXTERNAL_STORAGE")
                } catch (_: Exception) {}
                recordAccessAfter(id, client, app.packageName, permissions = musicPermissions)
                log("[+] Music Mod configured.")
            }
        }
        log("[+] ${app.displayName} installed and configured successfully!")
        "Installed"
        }
    }

    /**
     * Uninstalls any package cleanly from the vehicle.
     */
    suspend fun uninstallPackage(packageName: String): String = withContext(Dispatchers.IO) {
        log("[*] Uninstalling $packageName...")
        ensureAdb()
        val res = adbManager.uninstallPackage(packageName)
        if (res.contains("Success", ignoreCase = true)) {
            runCatching { journal.removedPackage(deviceId(ensureGrpc()), packageName) }
        }
        log("[+] Uninstall result: $res")
        res
    }

    /**
     * Installs an APK from local file storage onto the vehicle.
     */
    suspend fun installApkFile(file: File, friendlyName: String = file.name): String = withContext(Dispatchers.IO) {
        mutationMutex.withLock {
        val sizeMb = String.format("%.1f", file.length() / (1024.0 * 1024.0))
        log("[*] Installing $friendlyName ($sizeMb MB)...")
        val archive = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
        } ?: throw IllegalArgumentException("Cannot read the selected APK identity")
        val packageName = archive.packageName?.takeIf { it.matches(Regex("[A-Za-z0-9_.]+")) }
            ?: throw IllegalArgumentException("The selected APK has no safe package identity")
        val client = ensureGrpc()
        val id = deviceId(client)
        ensureAdb()
        val before = adbManager.observePackage(packageName)
        journal.beforePackage(id, packageName, friendlyName, before.installed)
        val res = adbManager.installApk(file)
        val after = adbManager.observePackage(packageName)
        check(after.installed) { "$friendlyName installation could not be verified" }
        journal.afterPackage(id, packageName, after.fingerprint)
        log("[+] Installed $friendlyName: $res")
        res
        }
    }

    /** Reads live state and shows only changes that AJ can safely prove it owns. */
    fun requestRemovalPreview(): Boolean {
        if (setupJob?.isActive == true || removalJob?.isActive == true) return false
        removalResult.value = null
        removalPreview.value = null
        removalJob = managerScope.launch {
            runCatching { buildRemovalPreview() }
                .onSuccess { removalPreview.value = it }
                .onFailure { errorMessage.value = it.message ?: "Could not inspect AJ changes" }
        }
        return true
    }

    private suspend fun buildRemovalPreview(): RemovalPreview {
        val client = ensureGrpc()
        val id = deviceId(client)
        val record = journal.get(id)
        val packages = record.packages.associate { it.packageName to observePackage(client, it.packageName) }
        val settings = record.settings.associate { it.address.id to readSetting(client, it.address) }
        return ChangeJournalPlanner.preview(record, packages, settings)
    }

    fun startRemoveAjChanges(): Boolean {
        if (setupJob?.isActive == true || removalJob?.isActive == true) return false
        removalJob = managerScope.launch {
            mutationMutex.withLock { removeAjChanges() }
        }
        return true
    }

    private suspend fun removeAjChanges() {
        isRemovalRunning.value = true
        removalResult.value = null
        val removed = mutableListOf<String>()
        val restored = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        val failures = mutableListOf<String>()
        try {
            val client = ensureGrpc()
            val preview = buildRemovalPreview().also { removalPreview.value = it }
            var completed = 0
            suspend fun restore(entry: SettingRestoration) {
                removalProgress.value = RemovalProgress(completed, preview.totalChanges, "Restoring ${entry.label}")
                val success = runCatching {
                    if (entry.address.kind == SettingKind.PROPERTY) {
                        client.restoreAdb(entry.restoreValue)
                    } else {
                        writeSetting(client, entry.address, entry.restoreValue)
                        readSetting(client, entry.address) == entry.restoreValue
                    }
                }.getOrElse {
                    failures += "${entry.label}: ${it.message ?: "restore failed"}"
                    false
                }
                if (success) {
                    journal.restoredSetting(preview.deviceId, entry.address)
                    restored += entry.label
                } else if (failures.none { it.startsWith(entry.label) }) {
                    failures += "${entry.label}: restore could not be verified"
                }
                completed++
            }

            // Restore permissions/settings while their owning package still exists.
            preview.settings.filterNot { it.address.kind == SettingKind.PROPERTY }.forEach { restore(it) }
            for (entry in preview.packages) {
                removalProgress.value = RemovalProgress(completed, preview.totalChanges, "Removing ${entry.displayName}")
                val (status, output) = client.shell("pm uninstall ${entry.packageName}", 30L)
                if (status in 0..1 && output.contains("Success", ignoreCase = true)) {
                    journal.removedPackage(preview.deviceId, entry.packageName)
                    removed += entry.displayName
                } else {
                    failures += "${entry.displayName}: ${output.ifBlank { "uninstall failed" }}"
                }
                completed++
            }
            // Diagnostic mode and temporary ADB mounts are always restored last.
            preview.settings.filter { it.address.kind == SettingKind.PROPERTY }.forEach { restore(it) }
            if (preview.settings.any { it.address.kind == SettingKind.PROPERTY }) adbManager.disconnect()
            removalProgress.value = RemovalProgress(preview.totalChanges, preview.totalChanges, "Restoration finished")
            removalResult.value = RemovalResult(removed, restored, skipped, failures)
            removalPreview.value = buildRemovalPreview()
        } catch (failure: Exception) {
            failures += failure.message ?: "Restoration failed"
            removalResult.value = RemovalResult(removed, restored, skipped, failures)
        } finally {
            isRemovalRunning.value = false
        }
    }

    private suspend fun writeSetting(client: S1GrpcClient, address: SettingAddress, value: String?) {
        require(address.name.matches(Regex("[A-Za-z0-9_.-]+"))) { "Unsafe setting name" }
        require(address.packageName.matches(Regex("[A-Za-z0-9_.]*"))) { "Unsafe package name" }
        val command = when (address.kind) {
            SettingKind.SETTING -> {
                require(address.namespace == "secure") { "Unsupported setting namespace" }
                if (value == null) "settings delete secure ${address.name}" else {
                    require(value.matches(Regex("[A-Za-z0-9_./:$,-]*"))) { "Unsafe setting value" }
                    "settings put secure ${address.name} $value"
                }
            }
            SettingKind.APP_OP -> {
                require(value != null && value.matches(Regex("[A-Za-z_]+"))) { "Unsafe app-op value" }
                "appops set ${address.packageName} ${address.name} $value"
            }
            SettingKind.PERMISSION -> when (value) {
                "granted" -> "pm grant ${address.packageName} ${address.name}"
                "denied" -> "pm revoke ${address.packageName} ${address.name}"
                else -> throw IllegalArgumentException("Unsafe permission value")
            }
            SettingKind.PROPERTY -> throw IllegalArgumentException("Property must be restored last")
        }
        val (status, output) = client.shell(command, 10L)
        check(status in 0..1) { output.ifBlank { "Setting restore failed" } }
    }

    /**
     * Launches an app on the scooter display via ADB intent.
     */
    suspend fun openApp(packageName: String, friendlyName: String = packageName): Boolean = withContext(Dispatchers.IO) {
        log("[*] Launching $friendlyName ($packageName) on scooter display...")
        try {
            val client = ensureGrpc()
            val launchCmd = when (packageName) {
                "com.asjohn720.ball" -> "am start -n com.asjohn720.ball/.ui.MainActivity"
                else -> "monkey -p $packageName -c android.intent.category.LAUNCHER 1"
            }
            val out = client.shell(launchCmd).second
            val isError = out.contains("No activities found") || out.contains("monkey aborted")
            if (isError) {
                log("[!] Could not launch $friendlyName via launcher: $out")
                false
            } else {
                log("[+] $friendlyName launched successfully on screen.")
                true
            }
        } catch (e: Exception) {
            log("[!] Failed to launch $friendlyName: ${e.message}")
            false
        }
    }

    /**
     * Installs any user-selected APK file from phone storage.
     */
    /**
     * Launches specific Android Settings screen on the scooter and returns execution confirmation.
     */
    suspend fun openSettings(action: String): Boolean = withContext(Dispatchers.IO) {
        log("[*] Launching settings: $action...")
        val client = ensureGrpc()
        val (exitCode, _) = client.shell("am start -a $action", 15L)
        val success = exitCode in 0..1
        if (success) {
            log("[+] Settings screen opened: $action")
        } else {
            log("[!] Could not open settings: $action (exit: $exitCode)")
        }
        success
    }

    /**
     * Returns scooter to Speedometer Home dashboard and returns confirmation.
     */
    suspend fun openDashboardHome(): Boolean = withContext(Dispatchers.IO) {
        log("[*] Bringing Dashboard Home to front...")
        val client = ensureGrpc()
        val (exitCode, _) = client.shell("am start -n com.olaelectric.hmiclient/.MainActivity", 15L)
        client.shell("input keyevent 3", 10L)
        val success = exitCode in 0..1
        if (success) {
            log("[+] Speedometer Home dashboard active.")
        }
        success
    }

    /**
     * Captures a screenshot from the scooter.
     */
    suspend fun captureScreenshot(): Bitmap = withContext(Dispatchers.IO) {
        log("[*] Capturing vehicle display screenshot...")
        val client = ensureGrpc()
        val bmp = client.takeScreenshot()
        log("[+] Screenshot captured successfully (${bmp.width}x${bmp.height} px).")
        bmp
    }

    /**
     * Executes arbitrary root shell command and returns output.
     */
    suspend fun runRootShell(command: String): String = withContext(Dispatchers.IO) {
        log("[*] Root shell command: $command")
        val client = ensureGrpc()
        val (_, out) = client.shell(command, 30L)
        val preview = if (out.length > 200) out.take(200) + "..." else out
        log("[+] Shell output: $preview")
        out
    }

    /**
     * Reads /proc/meminfo from the scooter to calculate live Total, Used, and Free RAM.
     */
    suspend fun fetchRamInfo(): RamInfo? = withContext(Dispatchers.IO) {
        try {
            val output = if (session.client != null && isConnected.value) {
                session.client!!.shell("cat /proc/meminfo", 8L).second
            } else if (isConnected.value) {
                adbManager.shell("cat /proc/meminfo")
            } else {
                ""
            }
            if (output.isBlank()) return@withContext null

            var totalKb = 0L
            var freeKb = 0L
            var memFreeKb = 0L
            var buffersKb = 0L
            var cachedKb = 0L
            var hasAvailable = false

            output.lines().forEach { line ->
                val parts = line.split(":")
                if (parts.size >= 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim().split("\\s+".toRegex()).firstOrNull()?.toLongOrNull() ?: 0L
                    when (key) {
                        "MemTotal" -> totalKb = value
                        "MemAvailable" -> {
                            freeKb = value
                            hasAvailable = true
                        }
                        "MemFree" -> memFreeKb = value
                        "Buffers" -> buffersKb = value
                        "Cached" -> cachedKb = value
                    }
                }
            }

            if (!hasAvailable) {
                freeKb = memFreeKb + buffersKb + cachedKb
            }

            if (totalKb > 0) {
                val totalMb = (totalKb / 1024).toInt()
                val freeMb = (freeKb / 1024).toInt()
                val usedMb = (totalMb - freeMb).coerceAtLeast(0)
                val info = RamInfo(totalMb, usedMb, freeMb)
                log("[+] Live RAM: Total ${totalMb} MB, Used ${usedMb} MB, Free ${freeMb} MB")
                info
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reads just model + MoveOS version for the connect/setup card.
     * MoveOS version comes from the daemon's GetSwVersion RPC (like Axiom);
     * model from the vehicle-variant XML, falling back to ro.product.model.
     */
    suspend fun fetchVehicleSummary(): VehicleSummary = withContext(Dispatchers.IO) {
        val client = ensureGrpc()

        val swRaw = try { client.getSwVersion() } catch (_: Exception) { "" }
        val firstLine = swRaw.lines().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: ""
        val moveOs = when {
            firstLine.isBlank() -> "MoveOS —"
            firstLine.startsWith("MoveOS", ignoreCase = true) -> firstLine
            else -> "MoveOS $firstLine"
        }
        val major = Regex("""(\d+)""").find(firstLine)?.groupValues?.get(1)?.toIntOrNull()

        val model = fetchModelName(client)
        log("[+] $model, $moveOs")
        val supported = isMoveOsSupported(firstLine)
        val result = VehicleSummary(model, moveOs, major, supported)
        vehicleSummary.value = result
        result
    }

    private fun isMoveOsSupported(versionStr: String): Boolean {
        // Supported on MoveOS 4.0.5 and above
        val match = Regex("""(\d+)(?:\.(\d+))?(?:\.(\d+))?""").find(versionStr) ?: return false
        val major = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return false
        val minor = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        val patch = match.groupValues.getOrNull(3)?.toIntOrNull() ?: 0

        return when {
            major > 4 -> true
            major == 4 -> if (minor > 0) true else patch >= 5
            else -> false
        }
    }

    private suspend fun fetchModelName(client: S1GrpcClient): String {
        val xml = try {
            client.shell("cat /data/data/com.olaelectric.vehiclerangeservice/shared_prefs/com.olaelectric.dataanalyser.xml", 10L).second
        } catch (_: Exception) { "" }
        val variant = Regex("""VEHICLE_VARIANT[^>]*>([^<]+)<""", RegexOption.IGNORE_CASE).find(xml)?.groupValues?.get(1)
            ?: Regex("""name="[^"]*VARIANT[^"]*">([^<]+)<""", RegexOption.IGNORE_CASE).find(xml)?.groupValues?.get(1)
        val raw = (variant ?: try { client.shell("getprop ro.product.model", 8L).second } catch (_: Exception) { "" }).trim()
        val clean = raw.replace("_", " ")
        val base = when {
            clean.contains("S1 Pro", ignoreCase = true) -> "S1 Pro"
            clean.contains("S1 Air", ignoreCase = true) -> "S1 Air"
            clean.contains("S1 X", ignoreCase = true) -> "S1 X"
            clean.contains("S1", ignoreCase = true) -> "S1"
            clean.isNotBlank() -> clean
            else -> "Ola S1"
        }
        // Append generation if the variant string carries it, e.g. "S1 Pro Gen 2".
        val gen = Regex("""gen\s*(\d+)""", RegexOption.IGNORE_CASE).find(clean)?.groupValues?.get(1)
        return if (gen != null && !base.contains("Gen", ignoreCase = true)) "$base Gen $gen" else base
    }

    /**
     * Fetches technical specifications and system properties of the vehicle.
     */
    suspend fun fetchScooterInfo(): ScooterInfo = withContext(Dispatchers.IO) {
        log("[*] Querying vehicle hardware specs and MoveOS version...")
        val client = ensureGrpc()
        val uid = client.shell("id -u").second.trim()
        val android = client.shell("getprop ro.build.version.release").second.trim()
        val build = client.shell("getprop ro.build.display.id").second.trim()
        val model = client.shell("getprop ro.product.model").second.trim()
        val mode = client.shell("getprop persist.ola.security.scootermode").second.trim()
        val info = ScooterInfo(uid = uid, androidVersion = android, buildId = build, model = model, scooterMode = mode)
        log("[+] Model: $model | MoveOS: $build | Android: $android | Root UID: $uid")
        info
    }

    fun disconnect() {
        reconnectJob?.cancel()
        session.disconnect()
        adbManager.disconnect()
        vehicleSummary.value = null
    }

    fun close() {
        setupJob?.cancel()
        removalJob?.cancel()
        reconnectJob?.cancel()
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        session.close()
        adbManager.disconnect()
        managerScope.cancel()
    }
}
