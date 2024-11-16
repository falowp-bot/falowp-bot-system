val kotlinxCoroutinesVersion: String by project
val ktorVersion: String by project
val logbackVersion: String by project
val jacksonVersion: String by project
val jsoupVersion: String by project
val guavaVersion: String by project
val playwrightVersion: String by project
val animatedGifVersion: String by project
val jetbrainsAnnotationsVersion: String by project
val jvmVersion: String = "2.1.0-RC"

plugins {
    kotlin("jvm") version "2.1.0-RC"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("maven-publish")
    id("signing")
}

group = "com.blr19c.falowp"
version = "2.0.0-RC4"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    //kotlinx-coroutines
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$kotlinxCoroutinesVersion")

    // Ktor server
    api("io.ktor:ktor-server-core-jvm:$ktorVersion")
    api("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    api("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    api("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
    api("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    api("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    api("io.ktor:ktor-server-config-yaml-jvm:$ktorVersion")

    // Ktor client
    api("io.ktor:ktor-client-websockets-jvm:$ktorVersion")
    api("io.ktor:ktor-client-core-jvm:$ktorVersion")
    api("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    api("io.ktor:ktor-client-encoding-jvm:$ktorVersion")
    api("io.ktor:ktor-client-json-jvm:$ktorVersion")
    api("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
    api("io.ktor:ktor-client-serialization-jvm:$ktorVersion")
    api("io.ktor:ktor-client-logging-jvm:$ktorVersion")
    api("io.ktor:ktor-serialization-jackson:$ktorVersion")
    api("ch.qos.logback:logback-classic:$logbackVersion")

    //jackson
    api("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    //guava
    api("com.google.guava:guava:$guavaVersion")
    //html处理
    api("org.jsoup:jsoup:$jsoupVersion")

    //浏览器
    api("com.microsoft.playwright:playwright:$playwrightVersion")
    //gif处理
    api("com.madgag:animated-gif-lib:$animatedGifVersion") {
        exclude("junit", "junit")
        exclude("org.easytesting", "fest-assert-core")
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxCoroutinesVersion")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$kotlinxCoroutinesVersion")
        force("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
        force("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
        force("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
        force("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
        force("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
        force("com.google.guava:guava:$guavaVersion")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$jvmVersion")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$jvmVersion")
    }
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.getByName("javadoc"))
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

artifacts {
    add("archives", tasks.named<Jar>("javadocJar"))
    add("archives", tasks.named<Jar>("sourcesJar"))
}


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "falowp-bot-system"
            version = project.version.toString()

            artifact(tasks.getByName<Jar>("javadocJar")) {
                classifier = "javadoc"
            }
            artifact(tasks.getByName<Jar>("sourcesJar")) {
                classifier = "sources"
            }

            pom {
                name.set("${project.group}:falowp-bot-system")
                description.set("FalowpBot system infrastructure")
                packaging = "jar"
                url.set("https://github.com/falowp-bot")

                scm {
                    url.set("https://github.com/falowp-bot")
                    connection.set("https://github.com/falowp-bot")
                    developerConnection.set("https://github.com/falowp-bot")
                }

                licenses {
                    license {
                        name.set("Apache-2.0 license")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("falowp")
                        name.set("falowp")
                        organization {
                            name = "falowp"
                            url = "https://falowp.blr19c.com"
                        }
                        timezone.set("+8")
                        roles.add("owner")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("s01SonatypeUserName")?.toString() ?: System.getenv("MAVEN_USERNAME")
                password = project.findProperty("s01SonatypePassword")?.toString() ?: System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}

signing {
    if (System.getenv("GPG_PRIVATE_KEY") != null) {
        useInMemoryPgpKeys(System.getenv("GPG_PRIVATE_KEY"), System.getenv("GPG_PASSPHRASE"))
    }
    sign(publishing.publications)
}