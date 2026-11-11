plugins {
    id("net.neoforged.moddev") version "2.0.148"
    id("neoforge-mutex")
}

version = "${sc.properties["mod.version"]}+${sc.current.version}"
base.archivesName = "${sc.properties["mod.file"]}-neoforge"

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

repositories {
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {

}

neoForge {
    version = sc.properties["deps.neo_loader"]

    mods {
        register(sc.properties["mod.id"]) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            gameDirectory = rootProject.file("run/${sc.current.project}/client")
            client()
            // `-PquickPlay=localhost:25600` joins a server straight away, `-PmcUser=Name` picks the offline name
            providers.gradleProperty("quickPlay").orNull?.let { programArguments.addAll("--quickPlayMultiplayer", it) }
            providers.gradleProperty("mcUser").orNull?.let { programArguments.addAll("--username", it) }
            // `-PclientSize=1920x1080` sets the window size (screenshots, video capture)
            providers.gradleProperty("clientSize").orNull?.split("x")?.let { programArguments.addAll("--width", it[0], "--height", it[1]) }
            // `-PtestHooks` lets the test runner request screenshots through files (see TestHooks)
            if (providers.gradleProperty("testHooks").isPresent) jvmArguments.add("-Dfarmstead.testhooks=true")
        }

        register("server") {
            gameDirectory = rootProject.file("run/${sc.current.project}/server")
            server()
            programArguments.add("--nogui") // the Swing console window crashes under heavy RCON/log traffic
        }
    }
}

// Loader-specific code lives in `<package>/platform/<loader>/`; other loaders' packages are left out
sourceSets.main {
    java {
        exclude("**/platform/fabric/**")
        exclude("**/platform/forge/**")
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, value: String) {
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", sc.properties["mod.id"])
            register("name", sc.properties["mod.name"])
            register("homepage", sc.properties["mod.homepage"])
            register("sources", sc.properties["mod.sources"])
            register("issues", sc.properties["mod.issues"])
            register("version", sc.properties["mod.version"])
            register("minecraft", sc.properties["mod.mc_compat"])
            // NeoForge 26.x renamed the square mod-list image key and warns on the old one
            register("icon_key", if (sc.current.parsed >= "26.1") "iconFile" else "logoFile")
        }

        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava, "extraClient" to "") }

        exclude("fabric.mod.json", "*.ct", "*.classtweaker", "META-INF/mods.toml", "pack.mcmeta")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    // Includes the license file in the built mod
    withType<Jar> {
        val name = sc.properties["mod.id"]
        inputs.property("mod_id", name)
        from("../../LICENSE") { rename { "$it-$name" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", sc.properties["mod.version"])
        from(jar.flatMap { it.archiveFile }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${sc.properties["mod.version"]}"))
    }
}
