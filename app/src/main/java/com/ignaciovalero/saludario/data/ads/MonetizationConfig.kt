package com.ignaciovalero.saludario.data.ads

import com.ignaciovalero.saludario.BuildConfig

object MonetizationConfig {
    val graphInterstitialAdId: String = BuildConfig.ADMOB_GRAPH_INTERSTITIAL_ID
    val useTestAds: Boolean = BuildConfig.USE_TEST_ADS
    val defaultGraphAdCooldownMinutes: Int = BuildConfig.DEFAULT_GRAPH_AD_COOLDOWN_MINUTES
}