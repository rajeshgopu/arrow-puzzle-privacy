package com.gopu.arrow.puzzle.game.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdView

/**
 * Adaptive banner slot for Home and level select. Renders nothing until consent
 * allows ad requests, and nothing when no ad provider is bound, so the layout
 * never shifts for a slot that will stay empty.
 */
@Composable
fun BannerAdSlot(adHost: AdHost?, modifier: Modifier = Modifier) {
    if (adHost == null) return
    val canRequestAds by adHost.canRequestAds.collectAsState(initial = false)
    if (!canRequestAds) return

    val context = LocalContext.current
    val banner = remember(adHost) { adHost.createBannerView(context) } ?: return

    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { banner },
            modifier = Modifier.fillMaxWidth(),
            onRelease = { (banner as? AdView)?.destroy() }
        )
    }
}