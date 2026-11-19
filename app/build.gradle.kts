plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.atlas.launcher"
 compileSdk = 35
 defaultConfig {
  applicationId = "com.atlas.launcher"
  minSdk = 29
  targetSdk = 35
  versionCode = 4
  versionName = "0.4.0"
  ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 signingConfigs {
  create("persistent") {
   System.getenv("ATLAS_KEYSTORE")?.let { storeFile = file(it) }
   storePassword = System.getenv("ATLAS_STORE_PASSWORD")
   keyAlias = System.getenv("ATLAS_KEY_ALIAS")
   keyPassword = System.getenv("ATLAS_KEY_PASSWORD")
  }
 }
 buildTypes { getByName("release") { signingConfig = signingConfigs.getByName("persistent") } }
}
dependencies {
 implementation("androidx.fragment:fragment-ktx:1.7.1")
 implementation("org.godotengine:godot:4.4.1.stable")
 implementation(project(":atlas-plugin"))
}
tasks.register("requireGodotPack") {
 doLast { check(file("src/main/assets/atlas.pck").isFile) { "Export Godot atlas.pck before building. See README." } }
}
tasks.named("preBuild") { dependsOn("requireGodotPack") }
