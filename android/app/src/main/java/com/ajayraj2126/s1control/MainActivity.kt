package com.ajayraj2126.s1control

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajayraj2126.s1control.model.ScooterManager
import com.ajayraj2126.s1control.ui.components.OpenSourceLicensesButton
import com.ajayraj2126.s1control.ui.screens.ConnectVehicleScreen
import com.ajayraj2126.s1control.ui.screens.AjHomeScreen
import com.ajayraj2126.s1control.ui.screens.CustomApkScreen
import com.ajayraj2126.s1control.ui.screens.SetupScreen
import com.ajayraj2126.s1control.ui.screens.ShellScreen
import com.ajayraj2126.s1control.ui.screens.ToolboxScreen
import com.ajayraj2126.s1control.ui.theme.*

enum class AppTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    SETUP("Setup", Icons.Default.Bolt),
    TOOLBOX("Tools", Icons.Default.Dashboard),
    CUSTOM_APK("Apps", Icons.Default.Download),
    TERMINAL("Shell", Icons.Default.Terminal)
}

class MainActivity : ComponentActivity() {

    private lateinit var scooterManager: ScooterManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scooterManager = ScooterManager(applicationContext)

        setContent {
            S1ModsTheme {
                MainAppScaffold(scooterManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scooterManager.disconnect()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(manager: ScooterManager) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("s1mods_prefs", android.content.Context.MODE_PRIVATE) }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showOnboardingDialog by remember {
        mutableStateOf(!prefs.getBoolean("has_seen_onboarding", false))
    }
    val isConnected by manager.isConnected.collectAsStateWithLifecycle()
    val isSetupRunning by manager.isSetupRunning.collectAsStateWithLifecycle()
    var bypassConnectionGate by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isConnected) {
        if (!isConnected) showDisconnectDialog = false
    }

    // Background update check: silent until a newer release exists.
    var updateRelease by remember { mutableStateOf<com.ajayraj2126.s1control.util.AppRelease?>(null) }
    LaunchedEffect(Unit) {
        val rel = com.ajayraj2126.s1control.util.UpdateChecker.fetchLatest()
        if (rel != null && com.ajayraj2126.s1control.util.UpdateChecker.isNewer(rel.tag)) {
            if (prefs.getString("update_skip_tag", null) != rel.tag) {
                updateRelease = rel
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        permissionLauncher.launch(permissions)
    }

    if (!isConnected && !bypassConnectionGate) {
        ConnectVehicleScreen(
            manager = manager,
            onBypass = { bypassConnectionGate = true },
            onOpenHelp = { showOnboardingDialog = true }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.heightIn(min = 48.dp)
                                .clickable(role = Role.Button) { showAboutDialog = true },
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "S1 Control",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "by AJ",
                                fontSize = 11.sp,
                                color = AccentMint,
                                maxLines = 1
                            )
                        }
                    },
                    actions = {
                        // Keep disconnect deliberate so a status tap cannot interrupt setup.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceVariantDark)
                                .clickable(role = Role.Button) {
                                    if (isConnected) showDisconnectDialog = true
                                    else bypassConnectionGate = false
                                }
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) StatusOk else StatusDanger)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isConnected) "Connected" else "Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isConnected) TextPrimary else TextMuted
                            )
                        }

                        IconButton(onClick = { showOnboardingDialog = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Help",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = { showAboutDialog = true }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "About",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BgDark,
                        titleContentColor = TextPrimary
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = BgDark,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(width = 1.dp, color = DividerColor, shape = RoundedCornerShape(0.dp))
                ) {
                    AppTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TextPrimary,
                                selectedTextColor = TextPrimary,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = AccentMintSubtle
                            )
                        )
                    }
                }
            },
            containerColor = BgDark
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    AppTab.HOME -> AjHomeScreen(manager) { selectedTab = it }
                    AppTab.SETUP -> SetupScreen(manager)
                    AppTab.TOOLBOX -> ToolboxScreen(manager)
                    AppTab.CUSTOM_APK -> CustomApkScreen(manager)
                    AppTab.TERMINAL -> ShellScreen(manager)
                }
            }
        }
    }

    if (showDisconnectDialog && isConnected) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect scooter?", color = TextPrimary) },
            text = {
                Text(
                    if (isSetupRunning) "Setup is running. Disconnecting may interrupt the current step."
                    else "Return to the connection screen to reconnect or choose another scooter.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDisconnectDialog = false
                    manager.disconnect()
                    bypassConnectionGate = false
                }) { Text("Disconnect", color = StatusDanger) }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Stay connected", color = AccentMint)
                }
            },
            containerColor = SurfaceDark
        )
    }

    updateRelease?.let { rel ->
        AlertDialog(
            onDismissRequest = { updateRelease = null },   // tap outside = Close (reappears next launch)
            title = { Text("AJ Edition update available", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary) },
            text = {
                Text(
                    "S1 Control by AJ ${rel.tag} is available. Open the release page to review the changes and download the AJ Edition APK.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rel.pageUrl)))
                        updateRelease = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("View release", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        prefs.edit().putString("update_skip_tag", rel.tag).apply()
                        updateRelease = null
                    }) {
                        Text("Don't ask again", color = TextMuted, fontSize = 12.sp)
                    }
                    TextButton(onClick = { updateRelease = null }) {
                        Text("Close", color = TextPrimary, fontSize = 13.sp)
                    }
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(12.dp)
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text("S1 Control by AJ • ${BuildConfig.VERSION_NAME}", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("S1 Control by AJ · @Ajayraj2126", fontSize = 13.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SocialButton("Repository", "https://github.com/Ajayraj2126/ola-s1-mods", context, modifier = Modifier.weight(1f))
                        SocialButton("Releases", "https://github.com/Ajayraj2126/ola-s1-mods/releases", context, modifier = Modifier.weight(1f))
                    }
                    OpenSourceLicensesButton()
                    Text(
                        text = "MIT License. Unofficial and not affiliated with, endorsed by, or supported by Ola Electric. 'Ola' and 'MoveOS' are trademarks of Ola Electric; 'S1' and model names are used descriptively. Modifying your scooter may void its warranty; use at your own risk.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = TextPrimary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(12.dp)
        )
    }

    if (showOnboardingDialog) {
        AlertDialog(
            onDismissRequest = {
                prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                showOnboardingDialog = false
            },
            title = {
                Text("How to connect", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OnboardingStepItem("1", "On your phone, turn on the Wi-Fi hotspot")
                    OnboardingStepItem("2", "On the scooter, open Wi-Fi and connect to that hotspot")
                    OnboardingStepItem("3", "Come back here and tap Auto Scan")
                    OnboardingStepItem("4", "Not found? Use Manual and enter the IP or MAC")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                        showOnboardingDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got it", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
fun OnboardingStepItem(step: String, title: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(1.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, fontSize = 14.sp, color = TextPrimary)
    }
}

@Composable
fun SocialButton(label: String, url: String, context: android.content.Context, modifier: Modifier = Modifier) {
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        },
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = TextPrimary)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
