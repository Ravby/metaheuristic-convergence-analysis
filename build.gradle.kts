plugins {
    id("java")
    kotlin("jvm") version "1.9.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation("org.jfree:jfreechart-swt:1.0")
    implementation("org.jfree:swtgraphics2d:1.0")
    implementation("org.jfree:jfreesvg:3.3")
    implementation("org.jfree:jcommon:1.0.24")
    implementation("org.jfree:jfreechart:1.0.19")
    // EARS dependencies
    implementation(project(":EARS"))
    implementation(kotlin("stdlib-jdk8"))
    // Lets-Plot Kotlin dependencies
    implementation("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:4.9.3")
    implementation("org.jetbrains.lets-plot:lets-plot-image-export:4.5.2")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}