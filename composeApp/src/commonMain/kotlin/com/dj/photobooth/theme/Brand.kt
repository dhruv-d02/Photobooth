package com.dj.photobooth.theme

/**
 * The decided app name (design/handoff/README.md's Naming Concepts: "Boothie" is the chosen
 * concept, over "4EVER"/"Picnic"). One constant rather than an AppSettings/DataStore layer -
 * there's no user-facing rename feature, this just replaces the two former placeholder
 * strings ("Photobooth" in StripPreviewUiState.brand, "FOURFRAME" in LandingScreen.kt).
 */
object Brand {
    const val NAME = "Boothie"
}
