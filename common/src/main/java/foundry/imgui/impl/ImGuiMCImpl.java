package foundry.imgui.impl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import foundry.imgui.api.ImGuiMC;
import foundry.imgui.impl.platform.ImGuiMCPlatform;
import imgui.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApiStatus.Internal
public final class ImGuiMCImpl {

    public static final Logger LOGGER = LoggerFactory.getLogger(ImGuiMC.MOD_ID);

    public static ImGuiHandler handler;

    public static final ImGuiCharSink IM_GUI_CHAR_SINK = new ImGuiCharSink();
    public static final StringSplitter IM_GUI_SPLITTER = new StringSplitter((charId, style) -> {
        // TODO in future versions get the font data from the built font
        try (final ImGuiMC.ActiveContext context = ImGuiMC.withImGui()) {
            if (context == null) {
                return 0.0F;
            }

            ImGui.pushFont(ImGuiMC.getStyleFont(style), 0.0F);
            float width = ImGui.calcTextSizeX(Character.toString(charId));
            ImGui.popFont();
            return width;
        }
    });

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
        //? if >= 26.2-pre-2 {
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

    public static TextureTarget createRenderTarget(final int width, final int height, final boolean depth) {
        //? if >=26.2-pre-2 {
        /*return new TextureTarget("ImGui", width, height, depth, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM);
         *///? } else if >=1.21.5 {
        /*return new TextureTarget("ImGui", width, height, depth);
         *///? } else if >=1.21.2 {
        /*final TextureTarget renderTarget = new TextureTarget(width, height, depth);
          renderTarget.setClearColor(0,0,0,1);
          renderTarget.clear();
          return renderTarget;
         *///? } else {
        final TextureTarget renderTarget = new TextureTarget(width, height, depth, net.minecraft.client.Minecraft.ON_OSX);
        renderTarget.setClearColor(0, 0, 0, 1);
        renderTarget.clear(net.minecraft.client.Minecraft.ON_OSX);
        return renderTarget;
        //? }
    }

    public static void resizeRenderTarget(final RenderTarget renderTarget, final int width, final int height) {
        //? if >=1.21.2 {
        /*renderTarget.resize(width, height);
         *///? } else {
        renderTarget.resize(width, height, net.minecraft.client.Minecraft.ON_OSX);
        //? }
    }
}
