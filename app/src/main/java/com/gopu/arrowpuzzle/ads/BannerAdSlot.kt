package com.gopu.arrowpuzzle.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gopu.arrowpuzzle.ui.theme.InkSoft

/**
 * Adaptive banner slot for Home and level select. Collapses to nothing when no
 * banner is available, so the layout never shifts and ads never block play.
 */
@Composable
fun BannerAdSlot(available: Boolean, modifier: Modifier = Modifier) {
    if (!available) return
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Once AdMob is bound this Surface is replaced by an AndroidView(AdView).
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 2.dp) {
            Text(
                text = "AD",
                color = InkSoft.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 7.dp)
            )
        }
    }
}
