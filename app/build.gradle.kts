import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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

/** Google's sample app ID. Debug builds always run against test ads. */
val testAdMobAppId = "ca-app-pub-3940256099942544~3347511713"

/** This app's AdMob App ID, from local.properties (`admob.appId`) or -Padmob.appId. */
val releaseAdMobAppId: String = (project.findProperty("admob.appId") as String?)
    ?: localProperties.getProperty("admob.appId")
    ?: testAdMobAppId

if (releaseAdMobAppId == testAdMobAppId) {
    logger.warn(
        "AdMob: no app ID configured. Add 'admob.appId=ca-app-pub-3319834061576964~<digits>' " +
            "to local.properties; release builds will serve test ads until then."
    )
}

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
        }
        release {
            isMinifyEnabled = false
            manifestPlaceholders["admobAppId"] = releaseAdMobAppId
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
    implementation(libs.user.messaging.platform)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation("junit:junit:4.13.2")
}
