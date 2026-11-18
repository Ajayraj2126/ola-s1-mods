plugins { alias(libs.plugins.android.application) }

fun signingInput(name: String): String? = providers.environmentVariable(name).orNull
    ?: providers.environmentVariable("${name}_FILE").orNull?.let { file(it).readText().trimEnd() }

android {
    namespace = "com.ajayraj2126.s1control"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.aj.s1control"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "0.3.0-aj"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    val store = providers.environmentVariable("AJ_RELEASE_STORE_FILE").orNull
    if (store != null) signingConfigs.create("ajRelease") {
        storeFile = file(store)
        storePassword = requireNotNull(signingInput("AJ_RELEASE_STORE_PASSWORD"))
        keyAlias = requireNotNull(signingInput("AJ_RELEASE_KEY_ALIAS"))
        keyPassword = requireNotNull(signingInput("AJ_RELEASE_KEY_PASSWORD"))
    }
    buildTypes {
        debug { applicationIdSuffix = ".preview"; versionNameSuffix = "-preview" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("ajRelease")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
