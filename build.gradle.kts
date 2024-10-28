plugins {
    id("application")
    id("java")
    id("io.freefair.lombok") version "8.0.1" // or the latest version
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

//group = "me.andywiranata.open"
version = "1.0"

java {
//    withJavadocJar()
//    withSourcesJar()
}


repositories {
    mavenCentral()
}

dependencies {
    // Jetty dependencies
    implementation("org.eclipse.jetty:jetty-server:11.0.15")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.15")
    implementation("org.eclipse.jetty:jetty-security:11.0.15")
    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.0.2")
    implementation("io.github.resilience4j:resilience4j-core:2.0.2")

    // Jetty Proxy module for ProxyServlet
    implementation("org.eclipse.jetty:jetty-proxy:11.0.14")
    // SnakeYAML for parsing YAML files
    implementation("org.yaml:snakeyaml:2.0")
    implementation("org.apache.commons:commons-jexl3:3.2.1")
    // SLF4J API
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("org.brotli:dec:0.1.2")
    implementation("redis.clients:jedis:5.2.0")
    implementation("com.timgroup:java-statsd-client:3.1.0")
    implementation("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.4.0") // Use the latest version
    testImplementation("org.mockito:mockito-inline:5.0.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0") // Use the latest version

}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22)) // Adjust based on your project's JDK
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "proxy.MainProxy" // Replace with your main class
    }
}

tasks.named("delombok") {
    enabled = false
}
application {
    // Replace with your actual main class package name
    mainClass.set("proxy.MainProxy")
}
