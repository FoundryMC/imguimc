package foundry.imgui.fabric.mixin;

import com.mojang.blaze3d.platform.Window;
import foundry.imgui.impl.ImGuiMCImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "setDefaultErrorCallback", at = @At("TAIL"))
    public void init(final CallbackInfo ci) {
        ImGuiMCImpl.initHandler();

        if (ImGuiMCImpl.handler == null) {
            return;
        }

        //? if >=26.1 {
        /*final ResourceLocation id = ImGuiMCImpl.path("font_manager");
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(id, ImGuiMCImpl.handler.getFontManager());
        *///? } else if >= 1.21.9 {
        /*final ResourceLocation id = ImGuiMCImpl.path("font_manager");
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(id, ImGuiMCImpl.handler.getFontManager());
        *///? } else if >= 1.21.2 {
        /*final ResourceLocation id = ImGuiMCImpl.path("font_manager");
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return id;
            }

            @Override
            public java.util.concurrent.CompletableFuture<Void> reload(final PreparationBarrier preparationBarrier, final net.minecraft.server.packs.resources.ResourceManager resourceManager, final java.util.concurrent.Executor backgroundExecutor, final java.util.concurrent.Executor gameExecutor) {
                return ImGuiMCImpl.handler.getFontManager().reload(preparationBarrier, resourceManager, backgroundExecutor, gameExecutor);
            }

            @Override
            public String getName() {
                return id.toString();
            }
        });
        *///? } else {
        final ResourceLocation id = ImGuiMCImpl.path("font_manager");
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return id;
            }

            @Override
            public java.util.concurrent.CompletableFuture<Void> reload(final PreparationBarrier preparationBarrier, final net.minecraft.server.packs.resources.ResourceManager resourceManager, final net.minecraft.util.profiling.ProfilerFiller preparationsProfiler, final net.minecraft.util.profiling.ProfilerFiller reloadProfiler, final java.util.concurrent.Executor backgroundExecutor, final java.util.concurrent.Executor gameExecutor) {
                return ImGuiMCImpl.handler.getFontManager().reload(preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
            }

            @Override
            public String getName() {
                return id.toString();
            }
        });
        //? }
    }
}
