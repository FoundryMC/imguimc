package foundry.imgui.mixin.renderer.v2;

//? if >=1.21.6 {

/*import com.mojang.blaze3d.textures.GpuTextureView;
import foundry.imgui.api.ImGuiMC;
import foundry.imgui.api.ImGuiTextureProvider;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractTexture.class)
public abstract class AbstractTextureMixin implements ImGuiTextureProvider {

    @Shadow
    public abstract GpuTextureView getTextureView();

    @Override
    public long imguimc$id() {
        return ImGuiMC.getTexture(this.getTextureView()).imguimc$id();
    }

    @Override
    public void imguimc$setId(final long id) {
        ImGuiMC.getTexture(this.getTextureView()).imguimc$setId(id);
    }
}

*///?}