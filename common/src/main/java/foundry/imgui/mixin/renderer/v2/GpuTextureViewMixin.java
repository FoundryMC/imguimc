package foundry.imgui.mixin.renderer.v2;

//? if >=1.21.6 {

/*import com.mojang.blaze3d.opengl.GlTextureView;
import foundry.imgui.api.ImGuiTextureProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = {
        GlTextureView.class,
        //? if >=26.2 {
        /^com.mojang.blaze3d.vulkan.VulkanGpuTextureView.class
        ^///? }
}, remap = false)
public class GpuTextureViewMixin implements ImGuiTextureProvider {

    @Unique
    private long imguimc$id;

    @Override
    public long imguimc$id() {
        return this.imguimc$id;
    }

    @Override
    public void imguimc$setId(final long id) {
        this.imguimc$id = id;
    }
}

*///?}