plugins {
    java
    signing
    id("com.vanniktech.maven.publish") version "0.37.0"
}

group = "io.github.jack5505"
version = "0.2.0"

java {
    // Библиотека должна работать на Java 21+.
    // Собираем на установленном JDK (26), но компилируем байткод под 21.
    // sources/javadoc jars создаёт плагин com.vanniktech.maven.publish сам.
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}

// JMH benchmarks: manual source set, no third-party Gradle plugin.
// Run: JAVA_HOME=/opt/homebrew/opt/openjdk ./gradlew jmh
// Override JMH parameters: ./gradlew jmh -PjmhArgs="-f 2 -i 10"
val jmh = sourceSets.create("jmh")

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    add("jmhImplementation", sourceSets.main.get().output)
    add("jmhImplementation", "org.openjdk.jmh:jmh-core:1.37")
    add("jmhAnnotationProcessor", "org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("jmh") {
    group = "benchmark"
    description = "Runs JMH benchmarks (override with -PjmhArgs)"
    dependsOn(tasks.named("jmhClasses"))
    classpath = jmh.runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")
    if (project.hasProperty("jmhArgs")) {
        args((project.property("jmhArgs") as String).split(Regex("\\s+")))
    } else {
        args("-f", "1", "-wi", "3", "-i", "5", "-w", "1s", "-r", "2s")
    }
}

// GPG-подпись. Локально ключ берётся из ~/.gradle/gradle.properties
// (signing.secretKeyRingFile). В CI этого файла нет — ключ приходит как
// GitHub Secret (signingInMemoryKey) и подписывает «из памяти».
signing {
    val key = providers.gradleProperty("signingInMemoryKey")
    if (key.isPresent) {
        useInMemoryPgpKeys(key.get(), providers.gradleProperty("signingInMemoryKeyPassword").getOrElse(""))
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.github.jack5505", "checks5505", "0.2.0")

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
