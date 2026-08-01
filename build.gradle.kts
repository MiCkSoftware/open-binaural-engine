plugins {
    kotlin("jvm") version "2.4.10"
    `java-library`
}

group = "com.micksoftware.openbinaural"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}
