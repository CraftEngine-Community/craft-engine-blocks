import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import org.gradle.api.internal.DynamicObjectAware

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.1"
    id("de.eldoria.plugin-yml.bukkit") version "0.7.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://repo.momirealms.net/snapshots/")
}

dependencies {
    implementation(project(":legacy"))
    compileOnly("io.papermc.paper:paper-api:${rootProject.findProperty("paper_version")}-R0.1-SNAPSHOT")
    compileOnly("net.momirealms:craft-engine-core:${rootProject.findProperty("craftengine_version")}")
    compileOnly("net.momirealms:craft-engine-bukkit:${rootProject.findProperty("craftengine_version")}")
    compileOnly("net.momirealms:craft-engine-bukkit-proxy:${rootProject.findProperty("craftengine_version")}")
    compileOnly("net.momirealms:craft-engine-adventure:${rootProject.findProperty("craftengine_version")}")
    compileOnly("net.momirealms:craft-engine-nms-helper:${rootProject.findProperty("nms_helper_version")}")
    compileOnly("net.momirealms:sparrow-nbt:${rootProject.findProperty("sparrow_nbt_version")}")
    compileOnly("net.momirealms:sparrow-nbt-adventure:${rootProject.findProperty("sparrow_nbt_version")}")
    compileOnly("net.momirealms:sparrow-nbt-codec:${rootProject.findProperty("sparrow_nbt_version")}")
    compileOnly("net.momirealms:sparrow-nbt-legacy-codec:${rootProject.findProperty("sparrow_nbt_version")}")
    compileOnly("it.unimi.dsi:fastutil:${rootProject.findProperty("fastutil_version")}")
    compileOnly("com.google.code.gson:gson:${rootProject.findProperty("gson_version")}")
    compileOnly("net.bytebuddy:byte-buddy:${rootProject.findProperty("byte_buddy_version")}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
    dependsOn(tasks.clean)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching(arrayListOf("craft-engine-blocks.properties")) {
        expand((rootProject as DynamicObjectAware).asDynamicObject.properties)
    }
}

bukkit {
    main = "cn.gtemc.craftengine.CraftEngineBlocks"
    version = rootProject.findProperty("project_version") as String
    name = "CraftEngineBlocks"
    apiVersion = "1.20"
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
    author = "CraftEngine Community"
    website = "https://github.com/CraftEngine-Community"
    depend = listOf("CraftEngine")
    foliaSupported = true
}

artifacts {
    archives(tasks.shadowJar)
}

tasks {
    shadowJar {
        archiveFileName = "${rootProject.name}-${rootProject.findProject("project_version")}.jar"
        destinationDirectory.set(file("$rootDir/target"))
        relocate("net.bytebuddy", "cn.gtemc.craftengine.libraries.bytebuddy")
        relocate("net.momirealms.sparrow.nbt", "net.momirealms.craftengine.libraries.nbt")
    }
}