plugins {
    id("application")
    id("java")
    id("com.google.protobuf") version "0.9.4" // Plugin for gRPC code generation
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
    implementation("org.eclipse.jetty:jetty-server:11.0.18")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.18")
    implementation("org.eclipse.jetty:jetty-security:11.0.18")
    implementation("org.eclipse.jetty:jetty-servlets:11.0.18")
    // Jetty Proxy module for ProxyServlet
    implementation("org.eclipse.jetty:jetty-proxy:11.0.14")

    // gRPC runtime dependencies
    implementation("io.grpc:grpc-netty:1.58.0")
    implementation("io.grpc:grpc-protobuf:1.58.0")
    implementation("io.grpc:grpc-stub:1.58.0")

    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5") // For JSON processing

    implementation("com.nimbusds:nimbus-jose-jwt:9.31")
    // For gRPC reflection support
    implementation("io.grpc:grpc-services:1.58.0")

    // Protocol Buffers
    implementation("com.google.protobuf:protobuf-java:3.24.3")

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
    // Load `.env` manually
    val dotenvFile = file(".env")
    if (dotenvFile.exists()) {
        dotenvFile.forEachLine { line ->
            if (line.isNotBlank() && !line.trim().startsWith("#")) {
                val keyValue = line.split("=", limit = 2)
                if (keyValue.size == 2) {
                    environment(keyValue[0].trim(), keyValue[1].trim())
                }
            }
        }
    } else {
        println("Warning: .env file not found!")
    }

    jvmArgs("-javaagent:$agentPath")
}

tasks.register("checkAgentPath") {
    group = "verification"
    description = "Verifies if the OpenTelemetry Java agent exists"
    doLast {
        val agentPath = layout.buildDirectory.file("agent/opentelemetry-javaagent.jar").get().asFile
        if (agentPath.exists()) {
            println("Agent JAR is present at: ${agentPath.absolutePath}")
        } else {
            throw GradleException("Agent JAR not found. Run the 'copyAgent' task first.")
        }
    }
}

tasks.register("buildNativeImage") {
    dependsOn("checkAgentPath")
    group = "build"
    description = "Builds a GraalVM native image of the application"
    doLast {
        val jarFile = tasks.jar.get().archiveFile.get().asFile
        val outputFileName = "proxy-native"
        exec {
            commandLine(
                    "native-image",
                    "-jar", jarFile.absolutePath,
                    outputFileName
            )
        }
        println("Native image built successfully: $outputFileName")
    }
}

application {
    mainClass.set("io.jetproxy.MainProxy") // Replace with your main class
}
