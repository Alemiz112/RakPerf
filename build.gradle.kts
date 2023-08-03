plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.0"
}

group = "eu.mizerak.alemiz.rakperf"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://repo.opencollab.dev/maven-snapshots/")
    }
}

dependencies {
    implementation("net.sf.jopt-simple:jopt-simple:5.0.4")
    implementation("com.nimbusds", "nimbus-jose-jwt", "9.10.1")
    implementation("org.cloudburstmc.protocol:bedrock-connection:3.0.0.Beta1-SNAPSHOT")
    implementation("org.cloudburstmc.netty:netty-transport-raknet:1.0.0.CR1-SNAPSHOT")

    // Netty transport codecs
    implementation("io.netty:netty-transport-native-epoll:4.1.89.Final:linux-x86_64")
    implementation("io.netty.incubator:netty-incubator-transport-native-io_uring:0.0.17.Final:linux-x86_64")

    // Lombok
    implementation("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.6")
    implementation("org.slf4j:slf4j-simple:2.0.6")

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

application {
    mainClass.set("eu.mizerak.alemiz.rakperf.Bootstrap")
}