package foundry.imgui.impl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import foundry.imgui.api.ImGuiMC;
import foundry.imgui.impl.platform.ImGuiMCPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApiStatus.Internal
public final class ImGuiMCImpl {

    public static final Logger LOGGER = LoggerFactory.getLogger(ImGuiMC.MOD_ID);

    public static ImGuiHandler handler;

    public static void init() {
    }

    public static void initHandler() {
        try {
            handler = new ImGuiHandler(Minecraft.getInstance().getWindow());
            ImGuiMCPlatform.INSTANCE.afterImGuiLoad();
        } catch (final Throwable t) {
            LOGGER.error("Failed to load ImGui, disabling", t);
            handler = null;
        }
    }

    public static ResourceLocation path(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ImGuiMC.MOD_ID, path);
    }

    public static RenderTarget getMainRenderTarget() {
        //? if >= 26.2-snapshot-3 {
        /*return Minecraft.getInstance().gameRenderer.mainRenderTarget();
        *///? } else {
        return Minecraft.getInstance().getMainRenderTarget();
         //? }
    }

    public static long getWindowHandle(final Window window) {
        //? if >=1.21.9 {
        /*return window.handle();
        *///? } else {
        return window.getWindow();
         //? }
    }
}
