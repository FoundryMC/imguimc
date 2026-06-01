package foundry.imgui.mixin.viewport_capture;

import foundry.imgui.impl.ImGuiMCImpl;
import foundry.imgui.impl.MainImGuiViewport;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    //? if >=26.2-pre-2 {
    /*@org.spongepowered.asm.mixin.injection.ModifyArg(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/GpuSurface;blitFromTexture(Lcom/mojang/blaze3d/systems/CommandEncoder;Lcom/mojang/blaze3d/textures/GpuTextureView;)V"), index = 1)
    public com.mojang.blaze3d.textures.GpuTextureView imguimc$changeBlitTexture(final com.mojang.blaze3d.textures.GpuTextureView textureView) {
        if (ImGuiMCImpl.handler != null) {
            final MainImGuiViewport mainViewport = ImGuiMCImpl.handler.getMainViewport();
            if (mainViewport.isCaptured()) {
                return mainViewport.getRenderTarget().getColorTextureView();
            }
        }
        return textureView;
    }
    *///? } else if >=26.1 {
    /*@com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V"))
    public void cancelBlit(final com.mojang.blaze3d.pipeline.RenderTarget instance, final com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        if (ImGuiMCImpl.handler != null) {
            final MainImGuiViewport mainViewport = ImGuiMCImpl.handler.getMainViewport();
            if (mainViewport.isCaptured()) {
                original.call(mainViewport.getRenderTarget());
                return;
            }
        }
        original.call(instance);
    }
    *///? } else if >=1.21.5 {
    /*@com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V"))
    public void cancelBlit(final com.mojang.blaze3d.pipeline.RenderTarget instance, final com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        if (ImGuiMCImpl.handler != null) {
            final MainImGuiViewport mainViewport = ImGuiMCImpl.handler.getMainViewport();
            if (mainViewport.isCaptured()) {
                original.call(mainViewport.getRenderTarget());
                return;
            }
        }
        original.call(instance);
    }
    *///? } else {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(II)V"))
    public void cancelBlit(final com.mojang.blaze3d.pipeline.RenderTarget instance, final int width, final int height, final com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        if (ImGuiMCImpl.handler != null) {
            final MainImGuiViewport mainViewport = ImGuiMCImpl.handler.getMainViewport();
            if (mainViewport.isCaptured()) {
                final com.mojang.blaze3d.pipeline.RenderTarget renderTarget = mainViewport.getRenderTarget();
                original.call(renderTarget, renderTarget.width, renderTarget.height);
                return;
            }
        }
        original.call(instance, width, height);
    }
    //? }
}
