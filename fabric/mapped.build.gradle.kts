import org.gradle.internal.extensions.stdlib.capitalized

plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("multiloader-loader")
}

sourceSets {
    val main by getting

    val testmod by creating {
        compileClasspath += main.compileClasspath
        runtimeClasspath += main.runtimeClasspath
    }

    named("test") {
        compileClasspath += testmod.compileClasspath
        runtimeClasspath += testmod.runtimeClasspath
    }
}

loom {
    fabricModJsonPath = project(":fabric").file("src/main/resources/fabric.mod.json")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs {
        named("client") {
            client()
            source(sourceSets.getByName("testmod"))
        }

        remove(runConfigs["server"])

        all {
            configName = "Fabric ${environment.capitalized()}"
            ideConfigGenerated(true)
            vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
            runDir = "../../run" // Shares the run directory between versions
        }
    }

    mods {
        register("imguimc-testmod") {
            sourceSet(sourceSets.getByName("testmod"))
        }
    }
}

repositories {
    maven("https://maven.parchmentmc.org") {
        name = "ParchmentMC"
    }
}

dependencies {
    /**
     * Fetches only the required Fabric API modules to not waste time downloading all of them for each version.
     * @see <a href="https://github.com/FabricMC/fabric">List of Fabric API modules</a>
     */
    fun fapi(vararg modules: String) {
        for (it in modules) {
            modImplementation(fabricApi.module(it, property("deps.fabric_api") as String))
            include(fabricApi.module(it, property("deps.fabric_api") as String))
        }
    }

    minecraft("com.mojang:minecraft:${sc.current.version}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${property("parchment.minecraft")}:${property("parchment.version")}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")

    fapi("fabric-api-base", "fabric-resource-loader-v0")

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
        from(remapJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}
