package foundry.imgui.mixin;

import foundry.imgui.api.ImGuiMC;
import foundry.imgui.impl.ImGuiMCImpl;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    public void keyCallback(final CallbackInfo ci) {
        try (final ImGuiMC.ActiveContext ctx = ImGuiMC.withImGui()) {
            if (ctx != null && ctx.io().getWantCaptureKeyboard() && ImGuiMCImpl.handler.getMainViewport().isCaptureKeyboard()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    public void charCallback(final CallbackInfo ci) {
        try (final ImGuiMC.ActiveContext ctx = ImGuiMC.withImGui()) {
            if (ctx != null && ctx.io().getWantCaptureKeyboard() && ImGuiMCImpl.handler.getMainViewport().isCaptureKeyboard()) {
                ci.cancel();
            }
        }
    }
}
