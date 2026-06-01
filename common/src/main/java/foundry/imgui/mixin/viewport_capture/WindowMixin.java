package foundry.imgui.mixin.viewport_capture;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.platform.Window;
import foundry.imgui.impl.ImGuiMCImpl;
import net.minecraft.client.Minecraft;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {

    @Unique
    private boolean imguimc$isCaptured() {
        if (ImGuiMCImpl.handler != null && ImGuiMCImpl.handler.getMainViewport().isCaptured()) {
            return (Object) this == Minecraft.getInstance().getWindow();
        }
        return false;
    }

    @Inject(method = "calculateScale", at = @At("HEAD"))
    public void calculateScale(final CallbackInfoReturnable<Integer> cir, final @Share("size") LocalRef<Vector2ic> sizeRef) {
        if (this.imguimc$isCaptured()) {
            sizeRef.set(ImGuiMCImpl.handler.getMainViewport().size());
        }
    }

    @Inject(method = "setGuiScale", at = @At("HEAD"))
    public void setGuiScale(final CallbackInfo ci, final @Share("size") LocalRef<Vector2ic> sizeRef) {
        if (this.imguimc$isCaptured()) {
            sizeRef.set(ImGuiMCImpl.handler.getMainViewport().size());
        }
    }

    @ModifyExpressionValue(method = {"calculateScale", "setGuiScale"}, at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/platform/Window;framebufferWidth:I", opcode = 180))
    public int setGuiScaleWidth(final int original, final @Share("size") LocalRef<Vector2ic> sizeRef) {
        final Vector2ic size = sizeRef.get();
        return size != null ? size.x() : original;
    }

    @ModifyExpressionValue(method = {"calculateScale", "setGuiScale"}, at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/platform/Window;framebufferHeight:I", opcode = 180))
    public int setGuiScaleHeight(final int original, final @Share("size") LocalRef<Vector2ic> sizeRef) {
        final Vector2ic size = sizeRef.get();
        return size != null ? size.y() : original;
    }

    @ModifyReturnValue(method = "getWidth", at = @At("RETURN"))
    public int getWidth(final int original) {
        if (this.imguimc$isCaptured()) {
            return ImGuiMCImpl.handler.getMainViewport().size().x();
        }
        return original;
    }

    @ModifyReturnValue(method = "getHeight", at = @At("RETURN"))
    public int getHeight(final int original) {
        if (this.imguimc$isCaptured()) {
            return ImGuiMCImpl.handler.getMainViewport().size().y();
        }
        return original;
    }

    @ModifyReturnValue(method = "getScreenWidth", at = @At("RETURN"))
    public int getScreenWidth(final int original) {
        if (this.imguimc$isCaptured()) {
            return ImGuiMCImpl.handler.getMainViewport().size().x();
        }
        return original;
    }

    @ModifyReturnValue(method = "getScreenHeight", at = @At("RETURN"))
    public int getScreenHeight(final int original) {
        if (this.imguimc$isCaptured()) {
            return ImGuiMCImpl.handler.getMainViewport().size().y();
        }
        return original;
    }
}
