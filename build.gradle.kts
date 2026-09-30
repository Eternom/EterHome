plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")

    // Vault : fourni par le plugin Vault installé sur le serveur
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit")
    }

    // Téléchargées au démarrage par Paper via la section `libraries` du plugin.yml
    compileOnly("com.zaxxer:HikariCP:7.0.2")
    compileOnly("redis.clients:jedis:6.2.0")

    // Pilotes JDBC (le type utilisé est choisi dans config.yml)
    compileOnly("com.mysql:mysql-connector-j:9.4.0")
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.5.6")
    compileOnly("org.postgresql:postgresql:42.7.8")
    compileOnly("org.xerial:sqlite-jdbc:3.50.3.0")
    compileOnly("com.h2database:h2:2.3.232")
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
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
