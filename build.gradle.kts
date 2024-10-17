plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Jetty dependencies
    implementation("org.eclipse.jetty:jetty-server:11.0.14")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.14")

    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")

    // Jetty Proxy module for ProxyServlet
    implementation("org.eclipse.jetty:jetty-proxy:11.0.14")

    // SnakeYAML for parsing YAML files
    implementation("org.yaml:snakeyaml:2.0")

    // SLF4J API
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    implementation("org.brotli:dec:0.1.2")

    implementation("redis.clients:jedis:5.2.0")
    implementation("com.timgroup:java-statsd-client:3.1.0")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.4.0") // Use the latest version
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0") // Use the latest version

}

tasks.test {
    useJUnitPlatform()
}