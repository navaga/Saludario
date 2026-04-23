package com.ignaciovalero.saludario.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ignaciovalero.saludario.data.preferences.UserPreferencesDataSource
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MonetizationManager(
    private val userPreferencesDataSource: UserPreferencesDataSource
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var mobileAdsInitialized = false

    @Volatile
    private var canRequestAds = false

    @Volatile
    private var privacyOptionsRequired = false

    @Volatile
    private var graphInterstitialAd: InterstitialAd? = null

    @Volatile
    private var isGraphInterstitialLoading = false

    suspend fun refreshConsent(activity: Activity) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        suspendCancellableCoroutine<Unit> { continuation ->
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                        scope.launch {
                            updateConsentSnapshot(activity.applicationContext, consentInformation)
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    }
                },
                { error ->
                    Log.w(TAG, "No se pudo actualizar el consentimiento: ${error.message}")
                    scope.launch {
                        updateConsentSnapshot(activity.applicationContext, consentInformation)
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            )
        }
    }

    suspend fun maybeShowGraphEntryAd(activity: Activity): Boolean {
        if (!canRequestAds || userPreferencesDataSource.isPremiumNoAds.first()) return false

        val ad = graphInterstitialAd ?: run {
            preloadGraphInterstitialIfEligible(activity.applicationContext)
            return false
        }

        return suspendCancellableCoroutine { continuation ->
            var resumed = false

            fun resumeOnce(result: Boolean) {
                if (!resumed) {
                    resumed = true
                    if (continuation.isActive) continuation.resume(result)
                }
            }

            graphInterstitialAd = null
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    scope.launch {
                        userPreferencesDataSource.setGraphAdLastShownAtMillis(System.currentTimeMillis())
                    }
                }

                override fun onAdDismissedFullScreenContent() {
                    scope.launch {
                        preloadGraphInterstitialIfEligible(activity.applicationContext)
                    }
                    resumeOnce(true)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "No se pudo mostrar el anuncio de gráfica: ${adError.message}")
                    scope.launch {
                        preloadGraphInterstitialIfEligible(activity.applicationContext)
                    }
                    resumeOnce(false)
                }
            }

            runCatching { ad.show(activity) }
                .onFailure {
                    Log.w(TAG, "Fallo inesperado al mostrar el anuncio de gráfica: ${it.message}")
                    scope.launch {
                        preloadGraphInterstitialIfEligible(activity.applicationContext)
                    }
                    resumeOnce(false)
                }
        }
    }

    suspend fun showPrivacyOptionsForm(activity: Activity): Boolean {
        if (!privacyOptionsRequired) return false

        suspendCancellableCoroutine<Unit> { continuation ->
            UserMessagingPlatform.showPrivacyOptionsForm(activity) {
                scope.launch {
                    val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
                    updateConsentSnapshot(activity.applicationContext, consentInformation)
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }

        return true
    }

    private suspend fun updateConsentSnapshot(
        context: Context,
        consentInformation: ConsentInformation
    ) {
        val status = when (consentInformation.consentStatus) {
            ConsentInformation.ConsentStatus.REQUIRED -> AdConsentStatus.REQUIRED
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> AdConsentStatus.NOT_REQUIRED
            ConsentInformation.ConsentStatus.OBTAINED -> AdConsentStatus.OBTAINED
            else -> AdConsentStatus.UNKNOWN
        }

        val privacyRequired = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

        canRequestAds = consentInformation.canRequestAds()
        privacyOptionsRequired = privacyRequired

        userPreferencesDataSource.setAdConsentStatus(status)
        userPreferencesDataSource.setAdPrivacyOptionsRequired(privacyRequired)

        if (canRequestAds) {
            initializeMobileAdsIfNeeded(context)
            preloadGraphInterstitialIfEligible(context)
        } else {
            graphInterstitialAd = null
        }
    }

    private fun initializeMobileAdsIfNeeded(context: Context) {
        if (mobileAdsInitialized) return

        synchronized(this) {
            if (mobileAdsInitialized) return
            MobileAds.initialize(context) {}
            mobileAdsInitialized = true
        }
    }

    private suspend fun preloadGraphInterstitialIfEligible(context: Context) {
        if (!canRequestAds || graphInterstitialAd != null || isGraphInterstitialLoading) return
        if (userPreferencesDataSource.isPremiumNoAds.first()) return

        isGraphInterstitialLoading = true
        InterstitialAd.load(
            context,
            MonetizationConfig.graphInterstitialAdId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    graphInterstitialAd = interstitialAd
                    isGraphInterstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "No se pudo precargar el anuncio de gráfica: ${error.message}")
                    graphInterstitialAd = null
                    isGraphInterstitialLoading = false
                }
            }
        )
    }

    private companion object {
        const val TAG = "MonetizationManager"
    }
}