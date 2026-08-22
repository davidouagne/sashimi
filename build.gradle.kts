
plugins {
    kotlin("jvm") version "2.4.0"
    id("io.quarkus") version "3.38.3"
}

group = "fr.aphp"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val quarkusVersion = "3.38.3"
val hapiVersion = "8.10.1"
val jooqVersion = "3.21.7"

dependencies {
    // quarkus
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-picocli")
    implementation("io.quarkus:quarkus-arc")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.13")

    // jOOQ – parseur SQL standalone (pas besoin de datasource)
    implementation("org.jooq:jooq:$jooqVersion")

    // HAPI FHIR R4 – modèle objet + structures
    implementation("ca.uhn.hapi.fhir:hapi-fhir-base:$hapiVersion")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:$hapiVersion")

    // Sérialization JSON (utilisé par HAPI en interne)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Tests
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}