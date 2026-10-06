plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // EterLib : compilé depuis GitHub
    maven("https://jitpack.io")
    // Repli : EterLib publié sur cette machine (`gradlew publishToMavenLocal` dans EterLib), pour tester avant de pousser
    mavenLocal()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")

    // Socle commun : base, Redis, langue, joueurs, téléportation (plugin EterLib installé sur le serveur)
    compileOnly("com.github.Eternom:EterLib:1.3.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        // Déclarée comme entrée : sinon le cache de Gradle réutilise un plugin.yml avec l'ancienne version
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
