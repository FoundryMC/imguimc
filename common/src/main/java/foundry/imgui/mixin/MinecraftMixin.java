package foundry.imgui.mixin;

import foundry.imgui.impl.ImGuiMCImpl;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    //? if >= 1.21 {
    @Inject(method = "runTick", at = {
            @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = "ldc=mouse"),
            @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=mouse")
    })
            //? } else {
    /*@Inject(method = "runTick", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = "ldc=display"))
     *///? }
    public void beginFrame(final CallbackInfo ci) {
        if (ImGuiMCImpl.handler != null) {
            ImGuiMCImpl.handler.beginFrame();
        }
    }

    //? if >=26.1 {
    /*@Inject(method = "renderFrame", at=@At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = "ldc=present"))
     *///? } elif >=1.21.6 {
    /*@Inject(method = "runTick", at = {
            @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = "ldc=blit"),
            @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=blit")
    })
            *///? } else {
    @Inject(method = "runTick", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=updateDisplay"))
     //? }
    public void endFrame(final CallbackInfo ci) {
        if (ImGuiMCImpl.handler != null) {
            ImGuiMCImpl.handler.endFrame();
            //? if <26.2-pre-2 {
            ImGuiMCImpl.handler.swapBuffers();
            //? }
        }
    }

    //? if >=26.2-pre-2 {
    /*@Inject(method = "renderFrame", at=@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;submit()V", shift = At.Shift.AFTER))
    public void swapBuffers(final CallbackInfo ci) {
        if (ImGuiMCImpl.handler != null) {
            ImGuiMCImpl.handler.swapBuffers();
        }
    }
    *///? }
}
