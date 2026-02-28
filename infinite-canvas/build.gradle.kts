import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)
}

val isMacOs = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release")
    }

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    if (isMacOs) {
        iosArm64()
        iosSimulatorArm64()
        macosArm64()
        macosX64()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
            api(libs.androidx.lifecycle.viewmodelCompose)
        }
    }
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("io.github.xingray", "compose-infinite-canvas", "0.1.2")

    pom {
        name.set("Compose Infinite Canvas")
        description.set("An infinite canvas component for Compose Multiplatform")
        url.set("https://github.com/XingRay/compose-infinite-canvas")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("XingRay")
                name.set("XingRay")
                url.set("https://github.com/XingRay")
            }
        }

        scm {
            url.set("https://github.com/XingRay/compose-infinite-canvas")
            connection.set("scm:git:git://github.com/XingRay/compose-infinite-canvas.git")
            developerConnection.set("scm:git:ssh://git@github.com/XingRay/compose-infinite-canvas.git")
        }
    }
}

android {
    namespace = "io.github.xingray.compose.infinitecanvas"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
