package com.dj.photobooth.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether the app is currently allowed to request ads, per Google's User Messaging Platform
 * (UMP) consent flow run once in MainActivity.onCreate via
 * UserMessagingPlatform.loadAndShowConsentFormIfRequired. AdBanner.android.kt reads this and
 * withholds its ad request until it flips true.
 *
 * Defaults to false, not true: a banner must never fire an ad request before the consent flow
 * has had a chance to run at least once this process lifetime. This isn't defensive
 * over-caution - EEA/UK consent gating is a hard AdMob/GDPR policy requirement, and requesting
 * ads before it resolves is a policy violation, not just a UX nicety.
 */
object AdConsent {
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds

    fun update(canRequestAds: Boolean) {
        _canRequestAds.value = canRequestAds
    }
}
