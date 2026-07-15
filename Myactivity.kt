package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.SettingsEntity
import com.example.service.DynamicIslandAccessibilityService
import com.example.service.DynamicIslandForegroundService
import com.example.state.DynamicIslandStateManager
import com.example.ui.DynamicIslandView
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DynamicIslandStateManager.init(this)
        
        // Start Foreground Service for battery/media
        try {
            val serviceIntent = Intent(this, DynamicIslandForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    DashboardScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by DynamicIslandStateManager.settings.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    // Timer simulation state
    var timerJob by remember { mutableStateOf<Job?>(null) }
    var musicJob by remember { mutableStateOf<Job?>(null) }

    // State of permissions checked periodically
    var isAccessibilityGranted by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isNotificationGranted by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isOverlayGranted by remember { mutableStateOf(checkOverlayPermission(context)) }

    // Recheck permissions on resume simulation
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityGranted = isAccessibilityServiceEnabled(context)
            isNotificationGranted = isNotificationServiceEnabled(context)
            isOverlayGranted = checkOverlayPermission(context)
            delay(2000)
        }
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App top preview section (Always pinned, looking premium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1013))
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Interactive Preview Label
                Text(
                    text = "LIVE DYNAMIC PREVIEW",
                    color = Color(0xFF42A5F5),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                
                // Embedded preview of the Dynamic Island
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.TopCenter
                ) {
                    DynamicIslandView(isPreviewMode = true)
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 60.dp)
                    ) {
                        Text(
                            text = "Tap pill to expand • Hold to customize • Long press to open controls",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }

        // Scrollable settings panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Hero Banner Card
            HeroBannerCard()

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Permission status cards
            PermissionsPanel(
                context = context,
                isAccessibilityGranted = isAccessibilityGranted,
                isNotificationGranted = isNotificationGranted,
                isOverlayGranted = isOverlayGranted
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Interactive Simulator
            SimulatorPanel(
                onSimulateMusic = { isPlay ->
                    musicJob?.cancel()
                    if (isPlay) {
                        var progress = 0.35f
                        DynamicIslandStateManager.updateMusic(
                            title = "Blinding Lights",
                            artist = "The Weeknd",
                            isPlaying = true,
                            progress = progress,
                            packageName = "com.spotify.music"
                        )
                        // Animate music progress slider
                        musicJob = scope.launch {
                            while (progress < 1.0f) {
                                delay(1000)
                                progress += 0.01f
                                DynamicIslandStateManager.updateMusic(
                                    title = "Blinding Lights",
                                    artist = "The Weeknd",
                                    isPlaying = true,
                                    progress = progress,
                                    packageName = "com.spotify.music"
                                )
                            }
                        }
                    } else {
                        DynamicIslandStateManager.updateMusic(
                            title = "Blinding Lights",
                            artist = "The Weeknd",
                            isPlaying = false,
                            progress = 0.35f,
                            packageName = "com.spotify.music"
                        )
                    }
                },
                onSimulateCall = {
                    DynamicIslandStateManager.postIncomingCall(
                        callerName = "Surya Yadav",
                        phoneNumber = "+91 98765 43210"
                    )
                },
                onSimulateNotification = {
                    DynamicIslandStateManager.postNotification(
                        appName = "WhatsApp",
                        packageName = "com.whatsapp",
                        title = "Surya MD",
                        text = "Hey! The Dynamic Island is working perfectly on Android! 🚀🔥"
                    )
                },
                onSimulateTimer = {
                    timerJob?.cancel()
                    var rem = 300
                    DynamicIslandStateManager.updateTimer(rem, 300, "Daily Workout")
                    timerJob = scope.launch {
                        while (rem > 0) {
                            delay(1000)
                            rem--
                            DynamicIslandStateManager.updateTimer(rem, 300, "Daily Workout")
                        }
                    }
                },
                onSimulateCharging = {
                    DynamicIslandStateManager.updateBattery(88, true)
                },
                onClearAll = {
                    timerJob?.cancel()
                    musicJob?.cancel()
                    DynamicIslandStateManager.clearAll()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Size and Position customizer
            CustomizerPanel(settings = settings)

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Feature Toggle settings
            FeaturesTogglePanel(settings = settings)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HeroBannerCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2025))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Illustration loaded locally
            Image(
                painter = rememberAsyncImagePainter(
                    model = "/app/src/main/res/drawable/dynamic_island_hero_1784100963871.jpg"
                ),
                contentDescription = "Futuristic banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Linear glass layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            // Banner Title Info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Dynamic Island surya MD",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Custom iOS Notch replication engine • Premium animations",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun PermissionsPanel(
    context: Context,
    isAccessibilityGranted: Boolean,
    isNotificationGranted: Boolean,
    isOverlayGranted: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "System Setup",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "System Setup & Permissions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "These system services are required to draw the island overlay and display notifications.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            PermissionRow(
                title = "Accessibility Overlay",
                description = "Required to draw over the lock screen",
                isGranted = isAccessibilityGranted,
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        Toast.makeText(context, "Locate 'Dynamic Island surya MD' in services and enable it.", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            PermissionRow(
                title = "Notification Listener",
                description = "Required to capture incoming push notifications",
                isGranted = isNotificationGranted,
                onClick = {
                    try {
                        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open notification settings", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            PermissionRow(
                title = "Draw Over Other Apps",
                description = "Required to paint floating pill window",
                isGranted = isOverlayGranted,
                onClick = {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open overlay settings", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) Color(0xFF34A853) else Color(0xFFEA4335))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(text = description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) Color(0xFF34A853).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary,
                contentColor = if (isGranted) Color(0xFF34A853) else MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(28.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(text = if (isGranted) "Enabled" else "Grant", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SimulatorPanel(
    onSimulateMusic: (Boolean) -> Unit,
    onSimulateCall: () -> Unit,
    onSimulateNotification: () -> Unit,
    onSimulateTimer: () -> Unit,
    onSimulateCharging: () -> Unit,
    onClearAll: () -> Unit
) {
    var isMusicPlaying by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = "Simulator",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Interactive Live Simulator",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Simulate various events below to preview the pill size, split multi-active layout, animations, and expanded menus immediately in the live preview window.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Buttons Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SimulatorButton(
                    icon = if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    label = if (isMusicPlaying) "Pause Spotify" else "Spotify Music",
                    color = Color(0xFF1DB954),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        isMusicPlaying = !isMusicPlaying
                        onSimulateMusic(isMusicPlaying)
                    }
                )
                SimulatorButton(
                    icon = Icons.Default.Call,
                    label = "Simulate Call",
                    color = Color(0xFF34A853),
                    modifier = Modifier.weight(1f),
                    onClick = onSimulateCall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SimulatorButton(
                    icon = Icons.Default.ChatBubbleOutline,
                    label = "WhatsApp Msg",
                    color = Color(0xFF25D366),
                    modifier = Modifier.weight(1f),
                    onClick = onSimulateNotification
                )
                SimulatorButton(
                    icon = Icons.Default.Timer,
                    label = "Workout Timer",
                    color = Color(0xFFFF9500),
                    modifier = Modifier.weight(1f),
                    onClick = onSimulateTimer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SimulatorButton(
                    icon = Icons.Default.Bolt,
                    label = "Charging Plug",
                    color = Color(0xFF00E676),
                    modifier = Modifier.weight(1f),
                    onClick = onSimulateCharging
                )
                SimulatorButton(
                    icon = Icons.Default.Block,
                    label = "Clear All States",
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        isMusicPlaying = false
                        onClearAll()
                    }
                )
            }
        }
    }
}

@Composable
fun SimulatorButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(38.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun CustomizerPanel(settings: SettingsEntity) {
    val themesList = listOf("Glassmorphic iOS", "Cosmic Slate", "Neon Eclipse", "Classic Black")
    val speedList = listOf("Springy", "Fluid", "Standard")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Customizer",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Styling & Customizations",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Theme selection
            Text(text = "Style Theme Preset", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                themesList.forEach { th ->
                     val isSelected = settings.theme == th
                     Box(
                         modifier = Modifier
                             .weight(1f)
                             .height(28.dp)
                             .clip(RoundedCornerShape(8.dp))
                             .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                             .clickable {
                                 DynamicIslandStateManager.updateSettings(settings.copy(theme = th))
                             },
                         contentAlignment = Alignment.Center
                     ) {
                         Text(
                             text = th.substringBefore(" "),
                             color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                             fontSize = 9.sp,
                             fontWeight = FontWeight.Bold
                         )
                     }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Width slider
            Text(text = "Island Width: ${settings.width.toInt()} dp", fontSize = 11.sp)
            Slider(
                value = settings.width,
                onValueChange = { DynamicIslandStateManager.updateSettings(settings.copy(width = it)) },
                valueRange = 120f..320f,
                modifier = Modifier.height(32.dp)
            )

            // Height slider
            Text(text = "Island Height: ${settings.height.toInt()} dp", fontSize = 11.sp)
            Slider(
                value = settings.height,
                onValueChange = { DynamicIslandStateManager.updateSettings(settings.copy(height = it)) },
                valueRange = 28f..48f,
                modifier = Modifier.height(32.dp)
            )

            // Offset Y slider
            Text(text = "Vertical Notch Position: ${settings.yOffset.toInt()} dp", fontSize = 11.sp)
            Slider(
                value = settings.yOffset,
                onValueChange = { DynamicIslandStateManager.updateSettings(settings.copy(yOffset = it)) },
                valueRange = 0f..100f,
                modifier = Modifier.height(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Animation transition selection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Animation Feel", fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    speedList.forEach { speed ->
                        val isSel = settings.animationSpeed == speed
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    DynamicIslandStateManager.updateSettings(settings.copy(animationSpeed = speed))
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = speed,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturesTogglePanel(settings: SettingsEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Features",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Feature Toggles",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            FeatureToggleRow(
                icon = Icons.Outlined.MusicNote,
                title = "Music Controls",
                checked = settings.showMusic,
                onCheckedChange = { DynamicIslandStateManager.updateSettings(settings.copy(showMusic = it)) }
            )

            FeatureToggleRow(
                icon = Icons.Outlined.Notifications,
                title = "Push Notifications",
                checked = settings.showNotifications,
                onCheckedChange = { DynamicIslandStateManager.updateSettings(settings.copy(showNotifications = it)) }
            )

            FeatureToggleRow(
                icon = Icons.Outlined.Timer,
                title = "Countdown Timers",
                checked = settings.showTimer,
                onCheckedChange = { DynamicIslandStateManager.updateSettings(settings.copy(showTimer = it)) }
            )

            FeatureToggleRow(
                icon = Icons.Outlined.FlashOn,
                title = "Charging Visualizer",
                checked = settings.showBattery,
                onCheckedChange = { DynamicIslandStateManager.updateSettings(settings.copy(showBattery = it)) }
            )

            FeatureToggleRow(
                icon = Icons.Outlined.VisibilityOff,
                title = "Hide Notch When Idle",
                checked = settings.hideWhenIdle,
                onCheckedChange = { DynamicIslandStateManager.updateSettings(settings.copy(hideWhenIdle = it)) }
            )
        }
    }
}

@Composable
fun FeatureToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.height(28.dp)
        )
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val service = "${context.packageName}/${DynamicIslandAccessibilityService::class.java.canonicalName}"
    val settingValue = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    if (settingValue != null) {
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(settingValue)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(service, ignoreCase = true)) {
                return true
            }
        }
    }
    return false
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val packageNames = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return packageNames != null && packageNames.contains(context.packageName)
}

fun checkOverlayPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}
