package com.dj.photobooth.ads

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dj.photobooth.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
actual fun AdBanner(modifier: Modifier) {
    // Withhold the ad request entirely until MainActivity's UMP consent flow has resolved at
    // least once - see AdConsent's doc comment for why this isn't optional.
    val canRequestAds by AdConsent.canRequestAds.collectAsState()
    if (!canRequestAds) return

    val context = LocalContext.current
    // Anchored adaptive banner: sized to the actual available width rather than a fixed
    // 320x50, which is what AdMob currently recommends over the old fixed AdSize.BANNER for
    // higher fill/eCPM. Keyed on width so a configuration change that resizes the window (e.g.
    // split-screen) gets a freshly-sized ad, not a stale one.
    val widthDp = LocalConfiguration.current.screenWidthDp
    val adSize = remember(widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }

    // Built once per adSize (not per recomposition) and torn down explicitly - AdView holds a
    // live ad-serving connection, and AndroidView alone never calls destroy() on the View it
    // created when this leaves composition (e.g. navigating off the Gallery tab).
    val adView = remember(adSize) {
        AdView(context).apply {
            setAdSize(adSize)
            adUnitId = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
            // No visibility into ad load outcomes otherwise - a silent no-fill/misconfigured
            // ad-unit-ID looks identical to "working as designed" (collapsed to zero height)
            // from the UI alone, see AdBanner.kt's doc comment.
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d("AdBanner", "ad loaded: unitId=$adUnitId")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(
                        "AdBanner",
                        "ad failed to load: unitId=$adUnitId code=${error.code} " +
                            "domain=${error.domain} message=${error.message}",
                    )
                }
            }
        }
    }
    DisposableEffect(adView) {
        adView.loadAd(AdRequest.Builder().build())
        onDispose { adView.destroy() }
    }

    AndroidView(modifier = modifier.fillMaxWidth().height(adSize.height.dp), factory = { adView })
}
