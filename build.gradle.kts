plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.nhayfield"
version = "1.0.1"

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.1")
    type.set("IC")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }

    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("")
    }

    signPlugin {
        certificateChainFile.set(file("chain.crt"))
        privateKeyFile.set(file("private.key"))
        password.set("")
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
