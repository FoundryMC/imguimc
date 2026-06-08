package foundry.imgui.api;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.imgui.api.event.RegisterImGuiFontsEvent;
import foundry.imgui.impl.ActiveContextImpl;
import foundry.imgui.impl.ImGuiMCImpl;
import foundry.imgui.impl.font.ImGuiFontManager;
import imgui.*;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * @since 1.0.0
 */
public interface ImGuiMC {

    ResourceLocation FONT_JETBRAINS_MONO = ImGuiMCImpl.path("jetbrains_mono");
    ResourceLocation FONT_DEFAULT = FONT_JETBRAINS_MONO;

    String MOD_ID = "imguimc";

    /**
     * Sets up the ImGui context for running code.
     *
     * @return The current context or <code>null</code> if there is no active context
     */
    static @Nullable ActiveContext withImGui() {
        if (!RenderSystem.isOnRenderThread()) {
            ImGuiMCImpl.LOGGER.error("Called Veil#withImGui() on another thread");
            return null;
        }

        if (ImGuiMCImpl.handler != null) {
            ImGuiMCImpl.handler.start();
            return ActiveContextImpl.INSTANCE;
        }

        return null;
    }

    /**
     * Schedules the font atlas to be rebuilt at the start of the next frame.
     * <br>
     * {@link RegisterImGuiFontsEvent} will be fired just before the texture is created.
     */
    static void rebuildFonts() {
        if (ImGuiMCImpl.handler != null) {
            ImGuiMCImpl.handler.updateFonts();
        }
    }

    /**
     * Fetches a data-driven font by name.
     *
     * @param name   The name of the font
     * @param bold   Whether to request a bold version
     * @param italic Whether to request an italic version
     * @return The font to use
     * @see ImGuiFontManager
     */
    static ImFont getFont(@Nullable final ResourceLocation name, final boolean bold, final boolean italic) {
        if (ImGuiMCImpl.handler == null) {
            throw new IllegalStateException("ImGui is not loaded");
        }
        return ImGuiMCImpl.handler.getFontManager().getFont(name, bold, italic);
    }

    /**
     * Fetches the default font.
     *
     * @param bold   Whether to request a bold version
     * @param italic Whether to request an italic version
     * @return The font to use
     * @see ImGuiFontManager
     */
    static ImFont getFont(final boolean bold, final boolean italic) {
        return getFont(null, bold, italic);
    }

    /**
     * @return <code>true</code> if ImGui is currently loaded and able to be used
     */
    static boolean isImguiLoaded() {
        return ImGuiMCImpl.handler != null;
    }

    /**
     * Converts the specified abstract texture into a texture provider. The texture <strong>must</strong> be a 2D texture.
     *
     * @return The {@link ImGuiTextureProvider} for texture calls
     * @since 1.1.0
     */
    @Contract(value = "null->null", pure = true)
    static ImGuiTextureProvider getTexture(final AbstractTexture texture) {
        return (ImGuiTextureProvider) texture;
    }

    /**
     * Converts the specified render target color buffer into a texture provider.
     *
     * @return The {@link ImGuiTextureProvider} for texture calls
     * @since 2.0.0
     */
    @Contract(value = "null->fail", pure = true)
    static ImGuiTextureProvider getColorTexture(final RenderTarget renderTarget) {
        //? if >=1.21.6 {
        /*return getTexture(renderTarget.getColorTextureView());
        *///? } else if >=1.21.5 {
        /*return new foundry.imgui.impl.renderer.v0.RawOpenGlTextureProvider(((com.mojang.blaze3d.opengl.GlTexture) renderTarget.getColorTexture()).glId());
        *///? } else {
        return new foundry.imgui.impl.renderer.v0.RawOpenGlTextureProvider(renderTarget.getColorTextureId());
        //? }
    }

    /**
     * Converts the specified render target depth buffer into a texture provider.
     *
     * @return The {@link ImGuiTextureProvider} for texture calls or <code>null</code> if the render target doesn't have a depth buffer
     * @since 2.0.0
     */
    @Contract(value = "null->fail", pure = true)
    static @Nullable ImGuiTextureProvider getDepthTexture(final RenderTarget renderTarget) {
        if (!renderTarget.useDepth) {
            return null;
        }
        //? if >=1.21.6 {
        /*return getTexture(renderTarget.getDepthTextureView());
        *///? } else if >=1.21.5 {
        /*return new foundry.imgui.impl.renderer.v0.RawOpenGlTextureProvider(((com.mojang.blaze3d.opengl.GlTexture) renderTarget.getDepthTexture()).glId());
        *///? } else {
        return new foundry.imgui.impl.renderer.v0.RawOpenGlTextureProvider(renderTarget.getDepthTextureId());
        //? }
    }

    //? if >= 1.21.6 {

    /*/^*
     * Converts the specified gpu texture into a texture provider. The texture <strong>must</strong> be a 2D texture.
     *
     * @return The {@link ImGuiTextureProvider} for texture calls
     * @since 1.1.0
     ^/
    @Contract(value = "null->null", pure = true)
    static ImGuiTextureProvider getTexture(final com.mojang.blaze3d.textures.GpuTextureView texture) {
        return (ImGuiTextureProvider) texture;
    }
    *///? }

    //? if >= 1.21.11 {
    /*/^*
     * Converts the specified gpu sampler into a texture provider.
     *
     * @return The {@link ImGuiSampler} for texture calls
     * @since 1.1.0
     ^/
    @Contract(value = "null->null", pure = true)
    static ImGuiSampler getSampler(final com.mojang.blaze3d.textures.GpuSampler sampler) {
        return (ImGuiSampler) sampler;
    }
    *///? }

    /**
     * Fully renders Minecraft text into ImGui.
     *
     * @param text The text to render
     * @since 2.0.0
     */
    static void component(final FormattedText text) {
        component(text, Float.POSITIVE_INFINITY);
    }

    /**
     * Fully renders wrapped Minecraft text into ImGui.
     *
     * @param text      The text to render
     * @param wrapWidth The width to wrap to
     * @since 2.0.0
     */
    static void component(final FormattedText text, final float wrapWidth) {
        ImGuiMCImpl.IM_GUI_CHAR_SINK.setup();
        for (final FormattedCharSequence part : Language.getInstance().getVisualOrder(ImGuiMCImpl.IM_GUI_SPLITTER.splitLines(text, (int) wrapWidth, Style.EMPTY))) {
            part.accept(ImGuiMCImpl.IM_GUI_CHAR_SINK);
            ImGuiMCImpl.IM_GUI_CHAR_SINK.finish();
            ImGui.newLine();
        }
        ImGuiMCImpl.IM_GUI_CHAR_SINK.reset();
    }

    /**
     * Retrieves the ImGui font to use for the specified Minecraft style.
     *
     * @param style The style to get the font for
     * @return The ImFont to use
     * @since 2.0.0
     */
    static ImFont getStyleFont(final Style style) {
        //? if >=1.21.9 {
        /*final ResourceLocation fontName;
        if (net.minecraft.network.chat.FontDescription.DEFAULT.equals(style.getFont())) {
            fontName = FONT_JETBRAINS_MONO;
        } else if (style.getFont() instanceof net.minecraft.network.chat.FontDescription.Resource(final ResourceLocation id)) {
            fontName = id;
        } else {
            fontName = FONT_JETBRAINS_MONO;
        }
        return getFont(fontName, style.isBold(), style.isItalic());
        *///? } else {
        return getFont(Style.DEFAULT_FONT.equals(style.getFont()) ? null : style.getFont(), style.isBold(), style.isItalic());
        //? }
    }

    /**
     * Retrieves the ARGB color for the specified ImGui style color.
     *
     * @param color The ImGui color index
     * @return The ARGB ImGui color
     * @since 2.0.0
     */
    static int getColor(final int color) {
        final ImVec4 colors = ImGui.getStyle().getColors()[color];
        return (int) (colors.w * 255) << 24 | (int) (colors.x * 255) << 16 | (int) (colors.y * 255) << 8 | (int) (colors.z * 255);
    }

    /**
     * @return A string splitter for ImGui fonts
     * @since 2.0.0
     */
    static StringSplitter getStringSplitter() {
        return ImGuiMCImpl.IM_GUI_SPLITTER;
    }

    static void image(
            final ImGuiTextureProvider userTexture,
            final ImVec2 size) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.image(imGuiId, size);
    }

    static void image(
            final ImGuiTextureProvider userTexture,
            final float sizeX,
            final float sizeY) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.image(imGuiId, sizeX, sizeY);
    }

    static void image(
            final ImGuiTextureProvider userTexture,
            final ImVec2 size,
            final ImVec2 uv0) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.image(imGuiId, size, uv0);
    }

    static void image(
            final ImGuiTextureProvider userTexture,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.image(imGuiId, sizeX, sizeY, uv0X, uv0Y);
    }

    static void image(
            final ImGuiTextureProvider userTexture,
            final ImVec2 size,
            final ImVec2 uv0,
            final ImVec2 uv1) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.image(imGuiId, size, uv0, uv1);
    }

    static void image(
            final ImGuiTextureProvider userTexture,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y,
            final float uv1X,
            final float uv1Y) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.image(imGuiId, sizeX, sizeY, uv0X, uv0Y, uv1X, uv1Y);
    }

    static void image(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final ImVec2 size) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.imageWithBg(imGuiId, size);
    }

    static void image(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final float sizeX,
            final float sizeY) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.image(imGuiId, sizeX, sizeY);
    }

    static void image(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final ImVec2 size,
            final ImVec2 uv0) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.image(imGuiId, size, uv0);
    }

    static void image(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.image(imGuiId, sizeX, sizeY, uv0X, uv0Y);
    }

    static void image(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final ImVec2 size,
            final ImVec2 uv0,
            final ImVec2 uv1) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.image(imGuiId, size, uv0, uv1);
    }

    static void image(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y,
            final float uv1X,
            final float uv1Y) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.image(imGuiId, sizeX, sizeY, uv0X, uv0Y, uv1X, uv1Y);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            final ImVec2 size) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.imageWithBg(imGuiId, size);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            final float sizeX,
            final float sizeY) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.imageWithBg(imGuiId, sizeX, sizeY);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            final ImVec2 size,
            final ImVec2 uv0) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.imageWithBg(imGuiId, size, uv0);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.imageWithBg(imGuiId, sizeX, sizeY, uv0X, uv0Y);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            final ImVec2 size,
            final ImVec2 uv0,
            final ImVec2 uv1) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.imageWithBg(imGuiId, size, uv0, uv1);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y,
            final float uv1X,
            final float uv1Y) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.imageWithBg(imGuiId, sizeX, sizeY, uv0X, uv0Y, uv1X, uv1Y);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            final ImVec2 size,
            final ImVec2 uv0,
            final ImVec2 uv1,
            final ImVec4 tintCol) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.imageWithBg(imGuiId, size, uv0, uv1, tintCol);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y,
            final float uv1X,
            final float uv1Y,
            final float tintColX,
            final float tintColY,
            final float tintColZ,
            final float tintColW) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.imageWithBg(imGuiId, sizeX, sizeY, uv0X, uv0Y, uv1X, uv1Y, tintColX, tintColY, tintColZ, tintColW);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            final ImVec2 size,
            final ImVec2 uv0,
            final ImVec2 uv1,
            final ImVec4 tintCol,
            final ImVec4 borderCol) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.imageWithBg(imGuiId, size, uv0, uv1, tintCol, borderCol);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y,
            final float uv1X,
            final float uv1Y,
            final float tintColX,
            final float tintColY,
            final float tintColZ,
            final float tintColW,
            final float borderColX,
            final float borderColY,
            final float borderColZ,
            final float borderColW) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        ImGui.imageWithBg(imGuiId, sizeX, sizeY, uv0X, uv0Y, uv1X, uv1Y, tintColX, tintColY, tintColZ, tintColW, borderColX, borderColY, borderColZ, borderColW);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final ImVec2 size) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.imageWithBg(imGuiId, size);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final float sizeX,
            final float sizeY) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.imageWithBg(imGuiId, sizeX, sizeY);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final ImVec2 size,
            final ImVec2 uv0) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.imageWithBg(imGuiId, size, uv0);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.imageWithBg(imGuiId, sizeX, sizeY, uv0X, uv0Y);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final ImVec2 size,
            final ImVec2 uv0,
            final ImVec2 uv1) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.imageWithBg(imGuiId, size, uv0, uv1);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y,
            final float uv1X,
            final float uv1Y) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.imageWithBg(imGuiId, sizeX, sizeY, uv0X, uv0Y, uv1X, uv1Y);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final ImVec2 size,
            final ImVec2 uv0,
            final ImVec2 uv1,
            final ImVec4 tintCol) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.imageWithBg(imGuiId, size, uv0, uv1, tintCol);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y,
            final float uv1X,
            final float uv1Y,
            final float tintColX,
            final float tintColY,
            final float tintColZ,
            final float tintColW) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.imageWithBg(imGuiId, sizeX, sizeY, uv0X, uv0Y, uv1X, uv1Y, tintColX, tintColY, tintColZ, tintColW);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final ImVec2 size,
            final ImVec2 uv0,
            final ImVec2 uv1,
            final ImVec4 tintCol,
            final ImVec4 borderCol) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.imageWithBg(imGuiId, size, uv0, uv1, tintCol, borderCol);
    }

    static void imageWithBg(
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y,
            final float uv1X,
            final float uv1Y,
            final float tintColX,
            final float tintColY,
            final float tintColZ,
            final float tintColW,
            final float borderColX,
            final float borderColY,
            final float borderColZ,
            final float borderColW) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        ImGui.imageWithBg(imGuiId, sizeX, sizeY, uv0X, uv0Y, uv1X, uv1Y, tintColX, tintColY, tintColZ, tintColW, borderColX, borderColY, borderColZ, borderColW);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            final ImVec2 size) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        return ImGui.imageButton(strId, imGuiId, size);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            final float sizeX,
            final float sizeY) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        return ImGui.imageButton(strId, imGuiId, sizeX, sizeY);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            final ImVec2 size,
            final ImVec2 uv0) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        return ImGui.imageButton(strId, imGuiId, size, uv0);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        return ImGui.imageButton(strId, imGuiId, sizeX, sizeY, uv0X, uv0Y);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            final ImVec2 size,
            final ImVec2 uv0,
            final ImVec2 uv1) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        return ImGui.imageButton(strId, imGuiId, size, uv0, uv1);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y,
            final float uv1X,
            final float uv1Y) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        return ImGui.imageButton(strId, imGuiId, sizeX, sizeY, uv0X, uv0Y, uv1X, uv1Y);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            final ImVec2 size,
            final ImVec2 uv0,
            final ImVec2 uv1,
            final ImVec4 bgCol) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        return ImGui.imageButton(strId, imGuiId, size, uv0, uv1, bgCol);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y,
            final float uv1X,
            final float uv1Y,
            final float bgColX,
            final float bgColY,
            final float bgColZ,
            final float bgColW) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        return ImGui.imageButton(strId, imGuiId, sizeX, sizeY, uv0X, uv0Y, uv1X, uv1Y, bgColX, bgColY, bgColZ, bgColW);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            final ImVec2 size,
            final ImVec2 uv0,
            final ImVec2 uv1,
            final ImVec4 bgCol,
            final ImVec4 tintCol) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        return ImGui.imageButton(strId, imGuiId, size, uv0, uv1, bgCol, tintCol);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y,
            final float uv1X,
            final float uv1Y,
            final float bgColX,
            final float bgColY,
            final float bgColZ,
            final float bgColW,
            final float tintColX,
            final float tintColY,
            final float tintColZ,
            final float tintColW) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, null);
        return ImGui.imageButton(strId, imGuiId, sizeX, sizeY, uv0X, uv0Y, uv1X, uv1Y, bgColX, bgColY, bgColZ, bgColW, tintColX, tintColY, tintColZ, tintColW);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final ImVec2 size) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        return ImGui.imageButton(strId, imGuiId, size);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final float sizeX,
            final float sizeY) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        return ImGui.imageButton(strId, imGuiId, sizeX, sizeY);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final ImVec2 size,
            final ImVec2 uv0) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        return ImGui.imageButton(strId, imGuiId, size, uv0);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        return ImGui.imageButton(strId, imGuiId, sizeX, sizeY, uv0X, uv0Y);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final ImVec2 size,
            final ImVec2 uv0,
            final ImVec2 uv1) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        return ImGui.imageButton(strId, imGuiId, size, uv0, uv1);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y,
            final float uv1X,
            final float uv1Y) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        return ImGui.imageButton(strId, imGuiId, sizeX, sizeY, uv0X, uv0Y, uv1X, uv1Y);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final ImVec2 size,
            final ImVec2 uv0,
            final ImVec2 uv1,
            final ImVec4 bgCol) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        return ImGui.imageButton(strId, imGuiId, size, uv0, uv1, bgCol);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y,
            final float uv1X,
            final float uv1Y,
            final float bgColX,
            final float bgColY,
            final float bgColZ,
            final float bgColW) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        return ImGui.imageButton(strId, imGuiId, sizeX, sizeY, uv0X, uv0Y, uv1X, uv1Y, bgColX, bgColY, bgColZ, bgColW);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final ImVec2 size,
            final ImVec2 uv0,
            final ImVec2 uv1,
            final ImVec4 bgCol,
            final ImVec4 tintCol) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        return ImGui.imageButton(strId, imGuiId, size, uv0, uv1, bgCol, tintCol);
    }

    static boolean imageButton(
            final String strId,
            final ImGuiTextureProvider userTexture,
            @Nullable final ImGuiSampler sampler,
            final float sizeX,
            final float sizeY,
            final float uv0X,
            final float uv0Y,
            final float uv1X,
            final float uv1Y,
            final float bgColX,
            final float bgColY,
            final float bgColZ,
            final float bgColW,
            final float tintColX,
            final float tintColY,
            final float tintColZ,
            final float tintColW) {
        final long imGuiId = ImGuiMCImpl.handler.getRenderer().getImGuiId(userTexture, sampler);
        return ImGui.imageButton(strId, imGuiId, sizeX, sizeY, uv0X, uv0Y, uv1X, uv1Y, bgColX, bgColY, bgColZ, bgColW, tintColX, tintColY, tintColZ, tintColW);
    }

    interface ActiveContext extends AutoCloseable {

        ImGuiIO io();

        @Override
        void close();
    }
}
