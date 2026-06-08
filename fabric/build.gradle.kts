import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.internal.fingerprint.classpath.impl.ClasspathFingerprintingStrategy.compileClasspath
import org.gradle.internal.fingerprint.classpath.impl.ClasspathFingerprintingStrategy.runtimeClasspath

plugins {
    id("net.fabricmc.fabric-loom")
    id("multiloader-loader")
}

sourceSets {
    val main by getting

    val testmod by creating {
        compileClasspath += main.compileClasspath
        runtimeClasspath += main.runtimeClasspath
    }
}

loom {
    accessWidenerPath.set(project(":fabric").file("src/testmod/resources/testmod.classtweaker"))
    fabricModJsonPath = project(":fabric").file("src/main/resources/fabric.mod.json")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs {
        named("client") {
            client()
            source(sourceSets.named("testmod").get())

            programArgs("--vulkanValidation", "--renderDebugLabels")
//            environmentVariable("ENABLE_VULKAN_RENDERDOC_CAPTURE", "1")
//            environmentVariable("LD_PRELOAD", "librenderdoc.so")
        }

        remove(runConfigs["server"])

        all {
            configName = "Fabric ${environment.capitalized()}"
            ideConfigGenerated(true)
            vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
            runDir = "../../run" // Shares the run directory between versions
        }
    }

    val modid = project.property("mod.id") as String
    mods {
        // Registering your main mod is standard
        register(modid) {
            sourceSet(sourceSets.main.get())
        }
        // Register your test mod here
        register("$modid-testmod") {
            sourceSet(sourceSets.named("testmod").get())
        }
    }
}

tasks.named<ProcessResources>("processTestmodResources") {
    dependsOn(project(":common:${findProperty("deps.common")}").tasks.named("stonecutterGenerate"))
    dependsOn(configurations.named("commonResources"))
    from(configurations.named("commonResources"))

    val expandProps = mapOf(
        "version" to version,
        "group" to findProperty("mod.group"),
        "minecraft" to findProperty("mod.mc_dep"),
        "name" to findProperty("mod.name"),
        "author" to findProperty("mod.author"),
        "id" to findProperty("mod.id"),
        "license" to findProperty("mod.license"),
        "description" to findProperty("mod.description"),
        "neoforge" to (findProperty("deps.neoforge") ?: "missing"),
        "neoforge_loader" to (findProperty("deps.neoforge_loader") ?: "missing"),
        "fapi" to (findProperty("deps.fabric_api") ?: "missing"),
        "java" to project.extensions.getByType<JavaPluginExtension>().sourceCompatibility.toString()
    )

    filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "META-INF/neoforge.mods.toml", "*.mixins.json")) {
        expand(expandProps)
    }

    // 4. Track inputs cleanly for cache verification
    inputs.properties(expandProps)
}

tasks.register<Jar>("testmodJar") {
    from(sourceSets["testmod"].output)
    archiveClassifier.set("testmod")
}

dependencies {
    /**
     * Fetches only the required Fabric API modules to not waste time downloading all of them for each version.
     * @see <a href="https://github.com/FabricMC/fabric">List of Fabric API modules</a>
     */
    fun fapi(vararg modules: String) {
        for (it in modules) {
            implementation(fabricApi.module(it, project.property("deps.fabric_api") as String))
            include(fabricApi.module(it, project.property("deps.fabric_api") as String))
        }
    }

    minecraft("com.mojang:minecraft:${project.property("deps.minecraft")}")
    implementation("net.fabricmc:fabric-loader:${project.property("deps.fabric_loader")}")

    fapi("fabric-api-base", "fabric-resource-loader-v1")

    api("io.github.spair:imgui-java-binding:${project.property("deps.imgui")}")
    include("io.github.spair:imgui-java-binding:${project.property("deps.imgui")}")

    runtimeOnly("io.github.spair:imgui-java-natives-linux:${project.property("deps.imgui")}")
    runtimeOnly("io.github.spair:imgui-java-natives-macos:${project.property("deps.imgui")}")
    runtimeOnly("io.github.spair:imgui-java-natives-windows:${project.property("deps.imgui")}")
    include("io.github.spair:imgui-java-natives-linux:${project.property("deps.imgui")}")
    include("io.github.spair:imgui-java-natives-macos:${project.property("deps.imgui")}")
    include("io.github.spair:imgui-java-natives-windows:${project.property("deps.imgui")}")
}

tasks {
    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}
