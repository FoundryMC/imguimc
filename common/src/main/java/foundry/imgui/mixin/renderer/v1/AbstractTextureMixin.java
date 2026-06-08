package foundry.imgui.mixin.renderer.v1;

//? if =1.21.5 {

/*import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTexture;
import foundry.imgui.api.ImGuiTextureProvider;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractTexture.class)
public abstract class AbstractTextureMixin implements ImGuiTextureProvider {

    @Shadow
    public abstract GpuTexture getTexture();

    @Override
    public long imguimc$id() {
        return ((GlTexture) this.getTexture()).glId();
    }

    @Override
    public void imguimc$setId(final long id) {
        throw new UnsupportedOperationException();
    }
}

*///?}