plugins { id("com.android.application") }
android {
    namespace = "com.pawan.parkedphotos"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.pawan.parkedphotos"
        minSdk = 33
        targetSdk = 35
        versionCode = 6
        versionName = "0.6"
        testInstrumentationRunner = "com.pawan.parkedphotos.ZoomLayoutTest"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
