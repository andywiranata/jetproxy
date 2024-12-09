plugins {
    id("application")
    id("java")
    id("io.freefair.lombok") version "8.0.1"
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

version = "1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // Ensure compatibility with Java 21
    }
}

repositories {
    mavenCentral()
}

configurations {
    create("agent") // Configuration for the OpenTelemetry Java agent
}
val otelInstrumentationVersion: String by extra("2.9.0-alpha")

dependencies {
    // Jetty dependencies
    implementation("org.eclipse.jetty:jetty-server:11.0.15")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.15")
    implementation("org.eclipse.jetty:jetty-security:11.0.15")
    implementation("org.eclipse.jetty:jetty-servlets:11.0.15") // Required for CrossOriginFilter
    // Jetty Proxy module for ProxyServlet
    implementation("org.eclipse.jetty:jetty-proxy:11.0.14")

    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
    implementation("io.github.resilience4j:resilience4j-core:2.0.2")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.0.2")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.0.2")
    implementation("io.github.resilience4j:resilience4j-bulkhead:2.0.2")
    implementation("io.github.resilience4j:resilience4j-retry:2.0.2")


    // SnakeYAML for parsing YAML files
    implementation("org.yaml:snakeyaml:2.0")
    implementation("org.apache.commons:commons-jexl3:3.2.1")

    // SLF4J and Logback
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("org.brotli:dec:0.1.2")
    implementation("redis.clients:jedis:5.2.0")
    implementation("com.timgroup:java-statsd-client:3.1.0")
    implementation("org.projectlombok:lombok:1.18.34")

    // OpenTelemetry Instrumentation BOM for managing dependencies
    implementation(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:$otelInstrumentationVersion"))
    implementation("io.opentelemetry:opentelemetry-api")

    // OpenTelemetry Java agent
    configurations["agent"].dependencies.add(
            project.dependencies.create(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:$otelInstrumentationVersion"))
    )
    configurations["agent"].dependencies.add(
            project.dependencies.create("io.opentelemetry.javaagent:opentelemetry-javaagent")
    )
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.4.0")
    testImplementation("org.mockito:mockito-inline:5.0.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
}

tasks.register<Copy>("copyAgent") {
    group = "build"
    description = "Copies the OpenTelemetry Java agent to the build directory"
    from(configurations["agent"].singleFile)
    into(layout.buildDirectory.dir("agent"))
    rename("opentelemetry-javaagent-.*\\.jar", "opentelemetry-javaagent.jar")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "proxy.MainProxy" // Update with your main class
    }
}

tasks.named<JavaExec>("run") {
    dependsOn("copyAgent")
    val agentPath = layout.buildDirectory.file("agent/opentelemetry-javaagent.jar").get().asFile.absolutePath
    doFirst {
        println("Using OpenTelemetry agent located at: $agentPath")
    }
    // Load environment variables from .env file
// Load `.env` manually
    val dotenvFile = file(".env")
    if (dotenvFile.exists()) {
        dotenvFile.forEachLine { line ->
            // Skip empty lines and comments
            if (line.isNotBlank() && !line.trim().startsWith("#")) {
                val keyValue = line.split("=", limit = 2) // Split into two parts only
                if (keyValue.size == 2) {
                    val key = keyValue[0].trim()
                    val value = keyValue[1].trim()
                    println("$key $value")
                    environment(key, value)
                } else {
                    println("Warning: Skipping malformed line in .env file: $line")
                }
            }
        }
    } else {
        println("Warning: .env file not found!")
    }

    jvmArgs("-javaagent:$agentPath")
    // Optionally set OpenTelemetry environment variables


}

tasks.register("checkAgentPath") {
    group = "verification"
    description = "Verifies if the OpenTelemetry Java agent exists"
    doLast {
        val agentPath = layout.buildDirectory.file("agent/opentelemetry-javaagent.jar").get().asFile
        if (agentPath.exists()) {
            println("Agent JAR is present at: ${agentPath.absolutePath}")
        } else {
            println("Agent JAR not found. Please run the 'copyAgent' task first.")
        }
    }
}

application {
    mainClass.set("proxy.MainProxy") // Replace with your main class
}
