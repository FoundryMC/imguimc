package foundry.imgui.impl;

import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

import java.util.List;
import java.util.Set;

@ApiStatus.Internal
public abstract class ImGuiMCMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(final String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        //? if >=1.21.11 {
        /*return List.of(
                "renderer.v2.AbstractTextureMixin",
                "renderer.v2.GpuSamplerMixin",
                "renderer.v2.GpuTextureViewMixin",
                "renderer.v2.viewport.GlCommandEncoderMixin");
        *///? } else if >= 1.21.6 {
        /*return List.of(
                "renderer.v2.AbstractTextureMixin",
                "renderer.v2.GpuTextureViewMixin",
                "renderer.v2.viewport.GlCommandEncoderMixin");
         *///? } else if >= 1.21.5 {
        /*return List.of("renderer.v1.AbstractTextureMixin");
         *///? } else {
        return List.of("renderer.v0.AbstractTextureMixin");
         //? }
    }
}
