plugins {
    java
    id("com.vanniktech.maven.publish") version "0.37.0"
}

group = "io.github.jack5505"
version = "0.1.0"

java {
    // Библиотека должна работать на Java 21+.
    // Собираем на установленном JDK (26), но компилируем байткод под 21.
    // sources/javadoc jars создаёт плагин com.vanniktech.maven.publish сам.
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

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.github.jack5505", "checks5505", "0.1.0")

    pom {
        name = "checks5505"
        description = "Annotation-based cross-field business rule validation for Java 21+"
        url = "https://github.com/jack5505/checks5505"

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }

        developers {
            developer {
                id = "jack5505"
                name = "jack5505"
            }
        }

        scm {
            url = "https://github.com/jack5505/checks5505"
            connection = "scm:git:git://github.com/jack5505/checks5505.git"
            developerConnection = "scm:git:ssh://github.com/jack5505/checks5505.git"
        }
    }
}
