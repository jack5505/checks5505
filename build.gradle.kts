import java.net.URI

plugins {
    java
    `maven-publish`
}

group = "io.github.jack5505"
version = "0.1.0"

java {
    // Библиотека должна работать на Java 21+.
    // Собираем на установленном JDK (26), но компилируем байткод под 21.
    withSourcesJar()
    withJavadocJar()
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

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
                    connection = "scm:git:git://github.com/jack5505/checks5505.git"
                    developerConnection = "scm:git:ssh://github.com/jack5505/checks5505.git"
                    url = "https://github.com/jack5505/checks5505"
                }
            }
        }
    }

    repositories {
        maven {
            name = "central"
            url = URI.create(
                if (version.toString().endsWith("SNAPSHOT")) {
                    "https://central.sonatype.com/repository/maven-snapshots/"
                } else {
                    "https://central.sonatype.com/api/v1/publisher/upload"
                }
            )
            credentials {
                username = System.getenv("SONATYPE_USERNAME")
                    ?: project.findProperty("sonatypeUsername") as String?
                    ?: ""
                password = System.getenv("SONATYPE_PASSWORD")
                    ?: project.findProperty("sonatypePassword") as String?
                    ?: ""
            }
        }
    }
}
