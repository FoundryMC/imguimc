//? if >=26.3 {
/*package foundry.imgui.mixin.renderer.v2;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.platform.SDLEventHandler;
import com.mojang.blaze3d.platform.Window;
import foundry.imgui.impl.ImGuiMCImpl;
import foundry.imgui.impl.ImGuiWindowHandlerSDL3;
import org.lwjgl.sdl.SDL_Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SDLEventHandler.class)
public class SDLEventHandlerMixin {

    @Inject(method = {"handleMouseMotionEvent", "handleMouseWheelEvent", "handleMouseButtonEvent", "handleTextInputEvent", "handleKeyEvent",}, at = @At("HEAD"))
    public void handleEvent(final SDL_Event event, final CallbackInfo ci) {
        if (ImGuiMCImpl.handler == null) {
            return;
        }

        ((ImGuiWindowHandlerSDL3) ImGuiMCImpl.handler.getWindowHandler()).processEvent(event);
    }

    @WrapWithCondition(method = "pollEvents", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;handleEvent(Lorg/lwjgl/sdl/SDL_Event;)V"))
    public boolean handleWindowEvent(final Window instance, final SDL_Event event) {
        if (ImGuiMCImpl.handler == null) {
            return true;
        }

        ((ImGuiWindowHandlerSDL3) ImGuiMCImpl.handler.getWindowHandler()).processEvent(event);
        return true;
    }
}
*///? }
