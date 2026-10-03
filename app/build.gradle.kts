import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val signingPropertiesFile = rootProject.file("signing.properties")
val signingProperties = Properties().apply {
    if (signingPropertiesFile.isFile) {
        signingPropertiesFile.inputStream().use(::load)
    }
}

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties().apply {
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun signingValue(key: String): String = signingProperties.getProperty(key)
    ?: error("Missing '$key' in signing.properties")

/**
 * Reads a value from -P<key>=... first and then local.properties, so CI can
 * override a checked-out machine's settings without editing the file. Falls
 * back to [fallback], which keeps an unconfigured build working on test ads.
 */
fun adValue(key: String, fallback: String): String =
    (project.findProperty(key) as String?)?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: fallback

/** Google's sample app ID. Debug builds always run against test ads. */
val testAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerUnitId = "ca-app-pub-3940256099942544/9214589741"
val testRewardedUnitId = "ca-app-pub-3940256099942544/5224354917"
val testInterstitialUnitId = "ca-app-pub-3940256099942544/1033173712"

/**
 * This app's own AdMob values, from local.properties (`admob.appId`,
 * `admob.bannerUnitId`, `admob.rewardedUnitId`, `admob.interstitialUnitId`)
 * or the matching -P properties. The defaults are the published production
 * units; the app ID has no safe default and stays on the test one until it is
 * set, because a mismatched app ID makes the SDK refuse to serve anything.
 */
val releaseAdMobAppId = adValue("admob.appId", testAdMobAppId)
val bannerUnitId = adValue("admob.bannerUnitId", "ca-app-pub-3319834061576964/5314734915")
val rewardedUnitId = adValue("admob.rewardedUnitId", "ca-app-pub-3319834061576964/1474813821")
val interstitialUnitId =
    adValue("admob.interstitialUnitId", "ca-app-pub-3319834061576964/2352781229")

if (releaseAdMobAppId == testAdMobAppId) {
    logger.warn(
        "AdMob: no app ID configured. Add 'admob.appId=ca-app-pub-3319834061576964~<digits>' " +
            "to local.properties; release builds will serve test ads until then."
    )
}

/** Public privacy-policy URL required by the Play Store listing and Settings. */
val privacyPolicyUrl = adValue("privacyPolicyUrl", "")

/** BuildConfig string fields need their value quoted and escaped. */
fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.gopu.arrow.puzzle.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gopu.arrow.puzzle.game"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // Empty until a public policy page exists; Settings hides the row.
        buildConfigField("String", "PRIVACY_POLICY_URL", privacyPolicyUrl.asBuildConfigString())
    }

    signingConfigs {
        create("release") {
            if (signingPropertiesFile.isFile) {
                storeFile = rootProject.file(signingValue("storeFile"))
                storePassword = signingValue("storePassword")
                keyAlias = signingValue("keyAlias")
                keyPassword = signingValue("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = testAdMobAppId
            // Test units only, so a debug build can never serve or click a live ad.
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", testBannerUnitId.asBuildConfigString())
            buildConfigField("String", "ADMOB_REWARDED_UNIT_ID", testRewardedUnitId.asBuildConfigString())
            buildConfigField(
                "String",
                "ADMOB_INTERSTITIAL_UNIT_ID",
                testInterstitialUnitId.asBuildConfigString()
            )
        }
        release {
            isMinifyEnabled = false
            manifestPlaceholders["admobAppId"] = releaseAdMobAppId
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", bannerUnitId.asBuildConfigString())
            buildConfigField("String", "ADMOB_REWARDED_UNIT_ID", rewardedUnitId.asBuildConfigString())
            buildConfigField(
                "String",
                "ADMOB_INTERSTITIAL_UNIT_ID",
                interstitialUnitId.asBuildConfigString()
            )
            if (signingPropertiesFile.isFile) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    // The celebration and the board build real Compose Paths, whose android
    // counterparts are not implemented off device. Returning defaults instead of
    // throwing lets the JVM tests exercise the timing maths those objects sit in.
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.play.services.ads)
    implementation(libs.play.services.games.v2)
    implementation(libs.user.messaging.platform)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation("junit:junit:4.13.2")
}
