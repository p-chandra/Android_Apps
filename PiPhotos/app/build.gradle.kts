plugins { id("com.android.application") }
android {
    namespace = "com.pawan.piphotos"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.pawan.piphotos"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "com.pawan.piphotos.UploadSmokeTest"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
