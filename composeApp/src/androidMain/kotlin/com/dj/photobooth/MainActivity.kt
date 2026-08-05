package com.dj.photobooth

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dj.photobooth.ads.AdConsent
import com.dj.photobooth.camera.AndroidCameraController
import com.dj.photobooth.export.AndroidMediaRepo
import com.dj.photobooth.export.AndroidShareSheet
import com.google.android.ump.UserMessagingPlatform

// The only thing Android-specific about launching this app is the Activity itself, plus
// constructing the platform implementations App() needs (camera, media store, share sheet) -
// everything rendered (App()) lives in commonMain and is identical on iOS. Keeping this class
// down to "host the shared Composable" is what the commonMain-first convention in CLAUDE.md
// means in practice.
//
// Note what is NOT built here: the database/GalleryRepo. Those are process-wide singletons
// owned by PhotoboothApplication, because onCreate runs again on every configuration change
// and rebuilding a RoomDatabase per rotation leaks a connection pool each time.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate() per the core-splashscreen library's documented
        // usage - it reads the activity's theme (Theme.Boothie.Splash, set in
        // AndroidManifest.xml) and swaps to postSplashScreenTheme once the splash is dismissed.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // AdMob consent (GDPR/UK/CCPA) - must run before any ad request, and needs an Activity
        // (to host the consent form UI if one's required), which is why this lives here and not
        // in PhotoboothApplication.onCreate alongside MobileAds.initialize(). AdConsent starts
        // false, so AdBanner withholds its ad request until this callback fires at least once.
        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { formError ->
            // formError is only non-null if the form itself failed to load/show (e.g. no
            // network) - consentInformation.canRequestAds() is still authoritative either way
            // (mirrors Google's own quickstart sample), so proceed regardless rather than
            // leaving AdConsent stuck at its false default on a transient failure.
            Log.d(
                "AdConsent",
                "consent flow resolved: formError=${formError?.message} " +
                    "canRequestAds=${consentInformation.canRequestAds()} " +
                    "status=${consentInformation.consentStatus}",
            )
            AdConsent.update(consentInformation.canRequestAds())
        }

        val cameraController = AndroidCameraController(this)
        val galleryRepo = (application as PhotoboothApplication).galleryRepo
        val mediaRepo = AndroidMediaRepo(this)
        // Both take applicationContext internally, deliberately. They end up held by
        // ViewModels scoped to a NavBackStackEntry, and that store survives configuration
        // changes - so an Activity captured here would still be the destroyed pre-rotation one
        // the next time the user taps share.
        //
        // AndroidCameraController above is the exception: it genuinely must hold *this*
        // Activity, because registerForActivityResult needs an ActivityResultRegistry owner and
        // the CameraX preview binds to this Activity's lifecycle. That makes the instance built
        // here valid only until the next configuration change, which is why CaptureViewModel
        // does not keep the controller it was constructed with - PhotoboothNavHost re-points it
        // at the current instance on every composition pass, via
        // CaptureViewModel.onCameraControllerChanged. Without that, the surviving ViewModel
        // would drive a destroyed Activity's unbound ImageCapture and silently emit blank frames.
        val shareSheet = AndroidShareSheet(this)
        enableEdgeToEdge()
        setContent {
            App(
                cameraController = cameraController,
                galleryRepo = galleryRepo,
                mediaRepo = mediaRepo,
                shareSheet = shareSheet,
            )
        }
    }
}
