package foundry.imgui.mixin.renderer.v0;

//? if <1.21.5 {

import foundry.imgui.api.ImGuiTextureProvider;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractTexture.class)
public abstract class AbstractTextureMixin implements ImGuiTextureProvider {

    @Shadow
    public abstract int getId();

    @Override
    public long imguimc$id() {
        return this.getId();
    }

    @Override
    public void imguimc$setId(final long id) {
        throw new UnsupportedOperationException();
    }
}

//?}