import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val hasGoogleServicesConfig = file("google-services.json").exists()
if (hasGoogleServicesConfig) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

val releaseKeystorePath = providers.gradleProperty("RELEASE_STORE_FILE")
    .orElse(providers.environmentVariable("RELEASE_STORE_FILE"))
    .orNull
val releaseStorePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD")
    .orElse(providers.environmentVariable("RELEASE_STORE_PASSWORD"))
    .orNull
val releaseKeyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS")
    .orElse(providers.environmentVariable("RELEASE_KEY_ALIAS"))
    .orNull
val releaseKeyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD")
    .orElse(providers.environmentVariable("RELEASE_KEY_PASSWORD"))
    .orNull
val releaseAdMobAppId = providers.gradleProperty("RELEASE_ADMOB_APP_ID")
    .orElse(providers.environmentVariable("RELEASE_ADMOB_APP_ID"))
    .orNull
val releaseGraphInterstitialAdId = providers.gradleProperty("RELEASE_ADMOB_GRAPH_INTERSTITIAL_ID")
    .orElse(providers.environmentVariable("RELEASE_ADMOB_GRAPH_INTERSTITIAL_ID"))
    .orNull
val configuredGraphAdCooldownMinutes = providers.gradleProperty("GRAPH_AD_COOLDOWN_MINUTES")
    .orElse(providers.environmentVariable("GRAPH_AD_COOLDOWN_MINUTES"))
    .orNull
    ?.toIntOrNull()
    ?.coerceAtLeast(1)
    ?: 180

val testAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val testGraphInterstitialAdId = "ca-app-pub-3940256099942544/1033173712"
val releaseUsesTestAdIds = releaseAdMobAppId.isNullOrBlank() || releaseGraphInterstitialAdId.isNullOrBlank()

val keystoreProps = Properties()
val keystorePropsFile = rootProject.file("keystore.properties")
if (keystorePropsFile.exists()) {
    keystorePropsFile.inputStream().use(keystoreProps::load)
}

val storeFilePath = releaseKeystorePath ?: keystoreProps.getProperty("storeFile")
val resolvedStorePassword = releaseStorePassword ?: keystoreProps.getProperty("storePassword")
val resolvedKeyAlias = releaseKeyAlias ?: keystoreProps.getProperty("keyAlias")
val resolvedKeyPassword = releaseKeyPassword ?: keystoreProps.getProperty("keyPassword")

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.ignaciovalero.saludario"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.ignaciovalero.saludario"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["adMobAppId"] = testAdMobAppId
        buildConfigField("int", "DEFAULT_GRAPH_AD_COOLDOWN_MINUTES", configuredGraphAdCooldownMinutes.toString())
    }

    signingConfigs {
        create("release") {
            if (!storeFilePath.isNullOrBlank()) {
                storeFile = file(storeFilePath)
                this.storePassword = resolvedStorePassword
                this.keyAlias = resolvedKeyAlias
                this.keyPassword = resolvedKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["adMobAppId"] = testAdMobAppId
            buildConfigField("String", "ADMOB_GRAPH_INTERSTITIAL_ID", "\"$testGraphInterstitialAdId\"")
            buildConfigField("boolean", "USE_TEST_ADS", "true")
        }

        release {
            manifestPlaceholders["adMobAppId"] = releaseAdMobAppId ?: testAdMobAppId
            buildConfigField(
                "String",
                "ADMOB_GRAPH_INTERSTITIAL_ID",
                "\"${releaseGraphInterstitialAdId ?: testGraphInterstitialAdId}\""
            )
            buildConfigField("boolean", "USE_TEST_ADS", releaseUsesTestAdIds.toString())
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (!storeFilePath.isNullOrBlank()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.google.mobile.ads)
    implementation(libs.google.ump)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.analytics.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

if (!hasGoogleServicesConfig) {
    logger.warn("google-services.json no encontrado en app/. Firebase Crashlytics quedará desactivado en runtime.")
}

if (releaseUsesTestAdIds) {
    logger.warn("RELEASE_ADMOB_APP_ID o RELEASE_ADMOB_GRAPH_INTERSTITIAL_ID no configurados. release usara IDs de prueba de AdMob.")
}