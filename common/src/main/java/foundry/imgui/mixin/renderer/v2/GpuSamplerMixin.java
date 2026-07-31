package foundry.imgui.mixin.renderer.v2;

//? if >=1.21.11 {

/*import com.mojang.blaze3d.opengl.GlSampler;
import foundry.imgui.api.ImGuiSampler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = {
        GlSampler.class,
        //? if >=26.2 {
        /^com.mojang.blaze3d.vulkan.VulkanGpuSampler.class
        ^///? }
}, remap = false)
public class GpuSamplerMixin implements ImGuiSampler {
}

*///?}
