plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.140" apply false
    id("net.fabricmc.fabric-loom") version "1.16-SNAPSHOT" apply false
    id("me.modmuss50.mod-publish-plugin") version "1.0.+" apply false
}

stonecutter active "1.21.1"

// Make newer versions published last
if (project.parent != null && !project.parent!!.name.equals("common", ignoreCase = true)) {
    stonecutter tasks {
        order("publishModrinth")
        order("publishCurseforge")
        order("publishGithub")
    }
}

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    var platform = node.project.parent!!.name.split("-")[0]

    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    swaps["platform"] = "\"${platform}\";"
    constants["release"] = property("mod.id") != "template"
    if (platform.equals("fabric", ignoreCase = true)) {
        dependencies["fapi"] = node.project.property("deps.fabric_api") as String
    }
    if (platform.equals("neoforge", ignoreCase = true)) {
        dependencies["neoforge"] = node.project.property("deps.neoforge") as String
    }

    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
        string(current.parsed >= "26.3") {
            replace("com.mojang.blaze3d.textures", "com.mojang.renderpearl.api.textures")
            replace("com.mojang.blaze3d.buffers.GpuBuffer", "com.mojang.renderpearl.api.buffers.GpuBuffer")
            replace("com.mojang.blaze3d.buffers.GpuBufferSlice", "com.mojang.renderpearl.api.buffers.GpuBufferSlice")
            replace("com.mojang.blaze3d.shaders.ShaderType", "com.mojang.renderpearl.api.pipeline.ShaderType")
            replace("com.mojang.blaze3d.shaders.UniformType", "com.mojang.renderpearl.api.pipeline.UniformType")
            replace("com.mojang.blaze3d.pipeline.BlendFunction", "com.mojang.renderpearl.api.pipeline.BlendFunction")
            replace("com.mojang.blaze3d.pipeline.RenderPipeline", "com.mojang.renderpearl.api.pipeline.RenderPipeline")
            replace("com.mojang.blaze3d.pipeline.BindGroupLayout", "com.mojang.renderpearl.api.pipeline.BindGroupLayout")
            replace("com.mojang.blaze3d.pipeline.ColorTargetState", "com.mojang.renderpearl.api.pipeline.ColorTargetState")
            replace("com.mojang.blaze3d.pipeline.DepthStencilState", "com.mojang.renderpearl.api.pipeline.DepthStencilState")
            replace("com.mojang.blaze3d.systems.CommandEncoder", "com.mojang.renderpearl.api.commands.CommandEncoder")
            replace("com.mojang.blaze3d.systems.RenderPass", "com.mojang.renderpearl.api.commands.RenderPass")
            replace("com.mojang.blaze3d.systems.GpuDevice", "com.mojang.renderpearl.api.device.GpuDevice")
            replace("com.mojang.blaze3d.vertex", "com.mojang.renderpearl.api.vertex")
            replace("com.mojang.blaze3d.GpuFormat", "com.mojang.renderpearl.api.GpuFormat")
            replace("com.mojang.blaze3d.PrimitiveTopology", "com.mojang.renderpearl.api.pipeline.PrimitiveTopology")
            replace("com.mojang.blaze3d.IndexType", "com.mojang.renderpearl.api.pipeline.IndexType")
            replace("com.mojang.blaze3d.opengl", "com.mojang.renderpearl.backend.opengl")
            replace("com.mojang.blaze3d.vulkan", "com.mojang.renderpearl.backend.vulkan")
        }
    }
}
