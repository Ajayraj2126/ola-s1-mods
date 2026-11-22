package com.ajayraj2126.s1control.ui.screens

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajayraj2126.s1control.model.ScooterManager
import com.ajayraj2126.s1control.ui.theme.*
import com.ajayraj2126.s1control.util.ApkDownloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * Fetches only the public, official S1Mods v1.5 release.
 * It never commits/rehosts upstream binaries or installs anything automatically.
 * The known full-container hash is from the GitHub Actions binary audit.
 */
internal object OfficialV15Mods {
    const val RELEASE_URL =
        "https://github.com/Asjohn720/ola-s1-mods/releases/download/v1.5/S1Mods-v1.5.apk"
    const val SHA256 =
        "3f859bd2cf3b6d0a0e94303ab19e39ea8cae42bbb0988a9fb13a4593364deb9f"

    data class Mod(val label: String, val asset: String, val packageName: String)

    val catalog = listOf(
        Mod("SmartBall 1.5", "smartball-latest.apk", "com.asjohn720.smartball"),
        Mod("Android Auto", "com.asjohn720.androidauto.apk", "com.asjohn720.androidauto"),
        Mod("Music Mod 1.5", "musicmod-release.apk", "com.asjohn720.musicmod"),
        Mod("OpenPlay (CarPlay)", "openplay-latest.apk", "com.asjohn720.openplay")
    )
    data class Prepared(val spec: Mod, val apk: File)

    fun existing(context: Context): List<Prepared> {
        val dir = File(context.filesDir, "aj-official-v15")
        return catalog.mapNotNull { spec ->
            val target = File(dir, spec.asset)
            if (target.isFile && target.length() > 100_000L &&
                context.packageManager.getPackageArchiveInfo(target.absolutePath, 0)
                    ?.packageName == spec.packageName) Prepared(spec, target)
            else null
        }
    }

    suspend fun fetch(context: Context): List<Prepared> = withContext(Dispatchers.IO) {
        val work = File(context.cacheDir, "S1Mods-v1.5-verified.apk")
        try {
            ApkDownloader.download(RELEASE_URL, work) { _, _, _ -> }
            if (work.length() !in 1L..60_000_000L) {
                throw IOException("Unexpected release size")
            }
            val digest = MessageDigest.getInstance("SHA-256")
            work.inputStream().buffered().use { input ->
                val chunk = ByteArray(32 * 1024)
                while (true) {
                    val length = input.read(chunk)
                    if (length < 0) break
                    digest.update(chunk, 0, length)
                }
            }
            val hex = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
            if (hex != SHA256) throw IOException("Official release SHA-256 mismatch")

            val dir = File(context.filesDir, "aj-official-v15").apply { mkdirs() }
            ZipFile(work).use { zip ->
                catalog.map { spec ->
                    val entry = zip.getEntry("assets/" + spec.asset)
                        ?: throw IOException("Release missing " + spec.asset)
                    if (entry.size > 30_000_000L) throw IOException("Unexpected mod size")
                    val dest = File(dir, spec.asset)
                    val temp = File(dir, spec.asset + ".tmp")
                    var size = 0L
                    try {
                        zip.getInputStream(entry).use { input ->
                            temp.outputStream().use { output ->
                                val buffer = ByteArray(32 * 1024)
                                while (true) {
                                    val n = input.read(buffer)
                                    if (n < 0) break
                                    size += n
                                    if (size > 30_000_000L) throw IOException("Oversized nested APK")
                                    output.write(buffer, 0, n)
                                }
                            }
                        }
                        val identity = context.packageManager
                            .getPackageArchiveInfo(temp.absolutePath, 0)?.packageName
                        if (identity != spec.packageName) {
                            throw IOException("Invalid APK package for " + spec.label)
                        }
                        if (dest.exists()) dest.delete()
                        if (!temp.renameTo(dest)) {
                            temp.copyTo(dest, overwrite = true)
                            temp.delete()
                        }
                    } finally {
                        temp.delete()
                    }
                    Prepared(spec, dest)
                }
            }
        } finally {
            work.delete()
        }
    }
}

/**
 * Optional, on-demand mod updater. The official S1Mods APK is downloaded directly
 * by the user's phone. No automatic scooter writes or command execution occur.
 */
@Composable
fun OfficialV15ModsCard(manager: ScooterManager) {
    val context = LocalContext.current
    val connected by manager.isConnected.collectAsStateWithLifecycle()
    val setupRunning by manager.isSetupRunning.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var prepared by remember { mutableStateOf(OfficialV15Mods.existing(context)) }
    var downloading by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<OfficialV15Mods.Prepared?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, AccentMint.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = AccentMint)
                Spacer(Modifier.width(8.dp))
                Text("Official S1Mods v1.5 mods", fontSize = 15.sp,
                    color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            Text(
                "Download the original v1.5 companion APK from Asjohn720's GitHub " +
                    "release. AJ verifies the release fingerprint and extracts the four " +
                    "official mod APKs locally on your phone. Each installation needs confirmation.",
                fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp
            )

            Button(
                onClick = {
                    scope.launch {
                        downloading = true
                        message = null
                        try {
                            prepared = OfficialV15Mods.fetch(context)
                            message = "Verified 4 mod APKs. Choose an app below."
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            message = "Download/verification failed: ${e.message}"
                        } finally {
                            downloading = false
                        }
                    }
                },
                enabled = !downloading && installing == null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark)
            ) {
                if (downloading) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = BgDark, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Download, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text(if (downloading) "Downloading & verifying..." else "Get verified v1.5 mods")
            }

            message?.let { Text(it, fontSize = 12.sp, color = TextSecondary) }
            if (prepared.isNotEmpty()) {
                prepared.forEach { item ->
                    HorizontalDivider(color = DividerColor)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.spec.label, color = TextPrimary,
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(item.spec.packageName, color = TextMuted, fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = { pending = item },
                            enabled = connected && !setupRunning && installing == null &&
                                !downloading,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (installing == item.spec.packageName) "Installing"
                                 else "Install", fontSize = 12.sp)
                        }
                    }
                }
            }
            Text(
                "Use only while parked. SmartBall has a NEW package ID; the older " +
                    "Balls app is not removed automatically. Some mods may need " +
                    "overlay, accessibility or Bluetooth permissions on the scooter. " +
                    "This imports official binaries; it does not reproduce their source code.",
                fontSize = 11.sp, color = StatusWarning, lineHeight = 16.sp
            )
        }
    }

    pending?.let { item ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Install ${item.spec.label}?", color = TextPrimary) },
            text = {
                Text(
                    "Install the verified official v1.5 APK on your connected scooter? " +
                        "This changes installed apps and may persist after reboot. " +
                        "Existing app data is not guaranteed to be preserved. " +
                        "Never use this while riding.",
                    color = TextSecondary, fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch {
                        installing = item.spec.packageName
                        try {
                            val result = manager.installApkFile(item.apk, item.spec.label)
                            message = "${item.spec.label}: ${result.take(160)}"
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            message = "${item.spec.label}: ${e.message}"
                        } finally {
                            installing = null
                        }
                    }
                }) { Text("Install", color = AccentMint) }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
