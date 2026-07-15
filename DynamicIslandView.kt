package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.state.DynamicIslandStateManager

@Composable
fun DynamicIslandView(isPreviewMode: Boolean = false) {
    val settings by DynamicIslandStateManager.settings.collectAsState()
    val musicState by DynamicIslandStateManager.musicState.collectAsState()
    val callState by DynamicIslandStateManager.callState.collectAsState()
    val notificationState by DynamicIslandStateManager.notificationState.collectAsState()
    val timerState by DynamicIslandStateManager.timerState.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }

    // Dynamic Island sizing logic based on active states
    val targetWidth = when {
        isExpanded -> 320f
        callState.isActive -> 280f
        notificationState.isActive -> 300f
        musicState.isPlaying -> 240f
        timerState.isActive -> 180f
        else -> settings.width
    }

    val targetHeight = when {
        isExpanded -> 120f
        callState.isActive || notificationState.isActive || musicState.isPlaying -> 60f
        else -> settings.height
    }

    val widthAnim by animateFloatAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
    )
    val heightAnim by animateFloatAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
    )

    Box(
        modifier = Modifier
            .padding(top = if (isPreviewMode) 20.dp else 0.dp)
            .width(widthAnim.dp)
            .height(heightAnim.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
            .clickable { isExpanded = !isExpanded },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                callState.isActive -> {
                    Text(text = "📞 Incoming Call", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = callState.callerName, color = Color.White, fontSize = 14.sp)
                }
                notificationState.isActive -> {
                    Text(text = "💬 ${notificationState.appName}", color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = notificationState.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                musicState.isPlaying -> {
                    Text(text = "🎵 Now Playing", color = Color(0xFF1DB954), fontSize = 11.sp)
                    Text(text = musicState.title, color = Color.White, fontSize = 13.sp, maxLines = 1)
                }
                timerState.isActive -> {
                    Text(text = "⏳ Timer: ${timerState.remainingSeconds}s", color = Color.Yellow, fontSize = 13.sp)
                }
                else -> {
                    // Idle state pill content
                    Text(text = "Dynamic Island surya MD", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                }
            }
        }
    }
}
