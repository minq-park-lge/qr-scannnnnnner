plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.snapqr.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.snapqr.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // 지원 언어만 APK 에 포함 (라이브러리의 불필요한 번역 리소스 제거)
        resourceConfigurations += listOf("en", "fr", "es", "ja", "ko", "zh")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 로컬 테스트 편의를 위해 debug 키로 서명. 배포 시 별도 signingConfig 지정.
            signingConfig = signingConfigs.getByName("debug")
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
        compose = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.camera.mlkit.vision)
    // Bundled 모델: Play Services 모델 다운로드 대기 없이 첫 실행부터 즉시 인식
    implementation(libs.mlkit.barcode)

    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.androidx.browser)
    implementation(libs.kotlinx.coroutines.android)
}
