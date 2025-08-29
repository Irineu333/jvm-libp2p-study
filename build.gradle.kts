plugins {
    kotlin("jvm") version "2.2.0"
}

group = "com.neoutils"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://dl.cloudsmith.io/public/libp2p/jvm-libp2p/maven/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://artifacts.consensys.net/public/maven/maven/") }
}

dependencies {
    implementation("io.libp2p:jvm-libp2p:1.2.2-RELEASE")
}

kotlin {
    jvmToolchain(17)
}