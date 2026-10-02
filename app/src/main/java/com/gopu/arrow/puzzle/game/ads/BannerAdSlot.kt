package com.gopu.arrow.puzzle.game.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        BannerView(adHost)
    }
}

/**
 * Banner slot for a screen that cannot afford to resize around a late fill.
 *
 * The band is reserved at the exact height [AdController.bannerHeight] reports,
 * on the first frame, and never changes: a banner that arrives mid-level cannot
 * resize the board or slide a control out from under a tap. The ad view is
 * pinned to the same height, so a creative taller than the reserved band is
 * cropped by it rather than spilling over whatever sits below. The band is
 * only reserved when a banner can be sized, so an unbound provider costs
 * nothing.
 */
@Composable
fun ReservedBannerAdSlot(adHost: AdHost?, modifier: Modifier = Modifier) {
    if (adHost == null) return
    val context = LocalContext.current
    val bandHeight = remember(adHost, context) { adHost.bannerHeight(context) } ?: return
    val canRequestAds by adHost.canRequestAds.collectAsState(initial = false)

    Box(
        modifier = modifier.fillMaxWidth().height(bandHeight.dp),
        contentAlignment = Alignment.Center
    ) {
        // Mounted only once consent allows it; the band is already sized.
        if (canRequestAds) BannerView(adHost, Modifier.height(bandHeight.dp))
    }
}

/**
 * The ad view itself, sized by its caller. It is remembered against the host so
 * a recomposition never rebuilds it, and released with the slot.
 */
@Composable
private fun BannerView(adHost: AdHost, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val banner = remember(adHost) { adHost.createBannerView(context) } ?: return
    AndroidView(
        factory = { banner },
        modifier = modifier.fillMaxWidth(),
        onRelease = { (banner as? AdView)?.destroy() }
    )
}