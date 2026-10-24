package com.mamad.peugeot405

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Bg = Color(0xFF060709)
val Panel = Color(0xFF101318)
val Silver = Color(0xFFE6E9F0)
val Steel = Color(0xFF8B93A5)
val Cyan = Color(0xFF4FC3F7)
val Violet = Color(0xFFB388FF)
val Gold = Color(0xFFFFC857)

/**
 * Branded animated stage. The previous car-photo montage is replaced with
 * Peugeot lion branding and PEUGEOT 405 typography while control animations remain active.
 */
@Composable
fun CarStage(
    posF: () -> Float,
    posB: () -> Float,
    maybach: Boolean,
    modifier: Modifier = Modifier
) {
    val gold by animateFloatAsState(if (maybach) 0.30f else 0f, tween(600), label = "gold")
    val front by animateFloatAsState((posF() - 80f) / 32f, tween(420), label = "frontMotion")
    val rear by animateFloatAsState((posB() - 84f) / 31f, tween(420), label = "rearMotion")
    BoxWithConstraints(
        modifier.fillMaxWidth().aspectRatio(1080f / 630f)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF171C25), Bg)))
            .border(1.dp, Cyan.copy(alpha = 0.18f + gold * 0.35f), RoundedCornerShape(22.dp))
    ) {
        // Fine ambient glow changes with Maybach mode.
        Box(Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Gold.copy(alpha = gold), Color.Transparent), radius = maxWidth.value * 1.4f)
        ))
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth()
                .graphicsLayer {
                    translationY = ((front + rear) * -2.5f).dp.toPx()
                    scaleX = 0.98f + 0.02f * ((front + rear) / 2f)
                    scaleY = 0.98f + 0.02f * ((front + rear) / 2f)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.peugeot_lion),
                contentDescription = "Peugeot lion emblem",
                modifier = Modifier.heightIn(max = maxHeight * 0.52f).fillMaxHeight(0.52f)
            )
            Spacer(Modifier.height(8.dp))
            Text("PEUGEOT", color = Silver, fontSize = 14.sp, letterSpacing = 5.sp, fontWeight = FontWeight.Medium)
            Text("405", color = Color.White, fontSize = 36.sp, letterSpacing = 8.sp, fontWeight = FontWeight.Black)
            Text(
                tr("سیستم کنترل تعلیق", "SUSPENSION CONTROL"),
                color = if (maybach) Gold else Steel,
                fontSize = 9.sp,
                letterSpacing = 2.sp
            )
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, if (maybach) Gold else Cyan, Color.Transparent))))
    }
}
