package foundry.imgui.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.imgui.api.ImGuiMC;
import imgui.ImGui;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = {"onPress", "onButton"}, at = @At("HEAD"), cancellable = true)
    public void mouseButtonCallback(final CallbackInfo ci) {
        try (final ImGuiMC.ActiveContext ctx = ImGuiMC.withImGui()) {
            if (ctx != null && ctx.io().getWantCaptureMouse()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    public void scrollCallback(final CallbackInfo ci) {
        try (final ImGuiMC.ActiveContext ctx = ImGuiMC.withImGui()) {
            if (ctx != null && ctx.io().getWantCaptureMouse()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "grabMouse", at = @At("HEAD"))
    public void grabMouse(final CallbackInfo ci) {
        try (final ImGuiMC.ActiveContext ctx = ImGuiMC.withImGui()) {
            if (ctx != null) {
                ImGui.setWindowFocus(null);
            }
        }
    }

    // REI calls these on another thread for some reason, so it's possible other mods may do the same thing

    @Inject(method = {"xpos", "ypos"}, at = @At("HEAD"), cancellable = true)
    public void cancelMouse(final CallbackInfoReturnable<Double> cir) {
        if (!RenderSystem.isOnRenderThread()) {
            return;
        }

        try (final ImGuiMC.ActiveContext ctx = ImGuiMC.withImGui()) {
            if (ctx != null && ctx.io().getWantCaptureMouse()) {
                cir.setReturnValue(Double.MIN_VALUE);
            }
        }
    }
}
