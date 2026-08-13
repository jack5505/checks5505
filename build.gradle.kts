plugins {
    java
}

group = "io.github.jack5505"
version = "0.1.0-SNAPSHOT"

java {
    // Библиотека должна работать на Java 21+.
    // Собираем на установленном JDK (26), но компилируем байткод под 21.
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
