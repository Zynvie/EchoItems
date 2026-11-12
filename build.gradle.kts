import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    kotlin("jvm") version "2.4.20-RC"
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
    id("de.eldoria.plugin-yml.paper") version "0.9.0"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = "cat.emir"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("26.1.2.build.+")
    paperLibrary("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("me.clip:placeholderapi:2.12.3")
    paperLibrary("de.tr7zw:item-nbt-api:2.16.0")

    // for echolib
    implementation("cat.emir:EchoLib:1.1.5")
    paperLibrary("org.spongepowered:configurate-yaml:4.2.0")
    paperLibrary("org.spongepowered:configurate-extra-kotlin:4.2.0")
    paperLibrary("io.github.classgraph:classgraph:4.8.179")
    paperLibrary("com.h2database:h2:2.3.232")
    paperLibrary("com.zaxxer:HikariCP:7.0.2")
    paperLibrary("org.jetbrains.exposed:exposed-core:1.3.1")
    paperLibrary("org.jetbrains.exposed:exposed-jdbc:1.3.1")
}

tasks {
    runServer {
        minecraftVersion("26.1.2")
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
        downloadPlugins {
            modrinth("viaversion", "5.11.0")
            modrinth("placeholderapi", "2.12.3")
        }
    }

    jar.get().enabled = false

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        relocate("cat.emir.echolib", "cat.emir.echoitems.echolib")
        archiveClassifier = ""
    }
}

// Configuring paper-plugin.yml
paper {
    authors = listOf("EmirhanTr3")
    description = "The only item plugin you will ever need."
    website = "https://github.com/EmirhanTr3/EchoItems"
    main = "cat.emir.echoitems.EchoItems"
    loader = "cat.emir.echoitems.echolib.load.LibraryLoader"
    bootstrapper = "cat.emir.echoitems.EchoItemsBootstrap"
    apiVersion = "26.1.2"

    // Keep this on!
    generateLibrariesJson = true

    serverDependencies {
        register("PlaceholderAPI") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }
}

kotlin {
    jvmToolchain(25)
}