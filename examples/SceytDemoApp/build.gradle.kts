import com.sceyt.chat.Config

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    compileSdk = Config.compileSdk
    namespace = "com.sceyt.chat.demo"

    defaultConfig {
        applicationId = "com.sceyt.chat.demo"
        minSdk = Config.minSdk
        targetSdk = Config.targetSdk
        versionCode = Config.versionCode
        versionName = Config.versionName

        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true
        signingConfig = signingConfigs.getByName("debug")

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            debugSymbolLevel = "FULL"
        }
    }

    flavorDimensions += listOf("environment")

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "false"
        }
    }

    productFlavors {
        create("prod") {
            dimension = "environment"
            resValue("string", "app_name", "Sceyt Chat")

            isDefault = true
            buildConfigField("String", "API_URL", "\"https://us-ohio-api.sceyt.com\"")
            buildConfigField("String", "APP_ID", "\"8lwox2ge93\"")
            buildConfigField("String", "GEN_TOKEN_BASE_URL", "\"https://tlnig20qy7.execute-api.us-east-2.amazonaws.com\"")
            buildConfigField("String", "GEN_TOKEN_ENDPOINT", "\"/dev/user/genToken\"")
            buildConfigField("String", "VALIDATION_API_URL", "\"https://ebttn1ks2l.execute-api.us-east-2.amazonaws.com/\"")
        }

        create("staging") {
            dimension = "environment"
            resValue("string", "app_name", "Sceyt Chat Staging")

            buildConfigField("String", "API_URL", "\"https://uk-london-south-api-2-staging.waafi.com\"")
            buildConfigField("String", "APP_ID", "\"yzr58x11rm\"")
            buildConfigField("String", "GEN_TOKEN_BASE_URL", "\"https://hm25ehfh6i.execute-api.eu-central-1.amazonaws.com/\"")
            buildConfigField("String", "GEN_TOKEN_ENDPOINT", "\"/load-test/user/genToken\"")
            buildConfigField("String", "VALIDATION_API_URL", "\"https://ebttn1ks2l.execute-api.us-east-2.amazonaws.com/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":SceytChatUiKit"))

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.core.ktx)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.glide)
    implementation(libs.okhttp)
    implementation(libs.koin.android)
    implementation(libs.converter.moshi)
    implementation(libs.jsoup)
    implementation(libs.sdp.android)
    implementation(libs.ssp.android)
    implementation(libs.converter.gson)
    implementation(libs.ucrop)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging.ktx)
}