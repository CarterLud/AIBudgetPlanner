plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    application
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.3")
    implementation("io.ktor:ktor-server-netty:2.3.3")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.3")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("org.apache.pdfbox:pdfbox:2.0.29")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // SQLite
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")

    // PostgreSQL
    implementation("org.postgresql:postgresql:42.7.2")

    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:0.49.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.49.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.49.0")

    // Logging (optional)
    implementation("ch.qos.logback:logback-classic:1.4.11")
}