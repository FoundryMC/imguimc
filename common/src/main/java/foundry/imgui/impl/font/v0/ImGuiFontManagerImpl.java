package foundry.imgui.impl.font.v0;

//? if <1.21.4 {

import foundry.imgui.api.ImGuiMC;
import foundry.imgui.impl.ImGuiMCImpl;
import foundry.imgui.impl.font.ImGuiFontManager;
import foundry.imgui.impl.platform.ImGuiMCPlatform;
import imgui.ImFont;
import imgui.ImFontAtlas;
import imgui.ImFontConfig;
import imgui.ImGui;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NativeResource;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@ApiStatus.Internal
public class ImGuiFontManagerImpl implements ImGuiFontManager {

    private static final FileToIdConverter FONT_LISTER = new FileToIdConverter("imgui_font", ".ttf");
    private static final DecimalFormat FONT_FORMAT = new DecimalFormat("0.#");

    private Map<ResourceLocation, FontPackBuilder> fontBuilders;
    private Map<ResourceLocation, FontPack> fonts;
    private ImFont defaultFont;

    public ImGuiFontManagerImpl() {
        this.fontBuilders = Map.of();
        this.fonts = Map.of();
    }

    @Override
    public ImFont getFont(@Nullable final ResourceLocation name, final boolean bold, final boolean italic) {
        if (name == null) {
            return this.defaultFont;
        }

        final FontPack font = this.fonts.get(name);
        if (font == null) {
            return this.defaultFont;
        }

        if (italic ^ bold) {
            return italic ? font.italic : font.bold;
        } else {
            return italic ? font.boldItalic : font.regular;
        }
    }

    //? if >=1.21.2 {
    /*@NotNull
    @Override
    public CompletableFuture<Void> reload(final PreparationBarrier preparationBarrier, @NotNull final ResourceManager resourceManager, @NotNull final Executor backgroundExecutor, @NotNull final Executor gameExecutor) {
    *///? } else {
    @NotNull
    @Override
    public CompletableFuture<Void> reload(final PreparationBarrier preparationBarrier, @NotNull final ResourceManager resourceManager,
                                          @NotNull final net.minecraft.util.profiling.ProfilerFiller preparationsProfiler, @NotNull final net.minecraft.util.profiling.ProfilerFiller reloadProfiler,
                                          @NotNull final Executor backgroundExecutor, @NotNull final Executor gameExecutor) {
        //? }

        return CompletableFuture.supplyAsync(() -> {
            final Map<ResourceLocation, FontData> fontData = new HashMap<>();

            for (final Map.Entry<ResourceLocation, Resource> entry : FONT_LISTER.listMatchingResources(resourceManager).entrySet()) {
                final ResourceLocation id = FONT_LISTER.fileToId(entry.getKey());
                final Resource resource = entry.getValue();
                try (final InputStream stream = resource.open()) {
                    final short[] ranges = resource.metadata().getSection(ImGuiFontRangesSectionSerializer.INSTANCE)
                            .map(ImGuiFontRangesSectionSerializer.FontMetadata::ranges)
                            .orElse(ImGuiFontManager.DEFAULT_FONT_RANGES);
                    final float size = resource.metadata().getSection(ImGuiFontSizeSectionSerializer.INSTANCE)
                            .orElse(ImGuiFontManager.DEFAULT_FONT_SIZE);
                    fontData.put(id, new FontData(stream.readAllBytes(), ranges, size));
                } catch (final IOException e) {
                    ImGuiMCImpl.LOGGER.error("Failed to load ImGui font: {}", id, e);
                }
            }

            return fontData;
        }, backgroundExecutor).thenCompose(preparationBarrier::wait).thenAcceptAsync(fontData -> {
            final Map<ResourceLocation, FontPackBuilder> fontBuilders = new HashMap<>();

            this.fontBuilders.values().forEach(FontPackBuilder::free);
            this.fontBuilders = Map.of();
            for (final Map.Entry<ResourceLocation, FontData> entry : fontData.entrySet()) {
                final ResourceLocation id = entry.getKey();
                final String[] parts = id.getPath().split("-", 2);
                if (parts.length < 2) {
                    continue;
                }

                final ResourceLocation name = ResourceLocation.tryBuild(id.getNamespace(), parts[0]);
                if (name == null) {
                    ImGuiMCImpl.LOGGER.error("Invalid font name: {}:{}", id.getNamespace(), parts[0]);
                    continue;
                }

                final String type = parts[1];
                switch (type) {
                    case "regular" -> fontBuilders.computeIfAbsent(name, FontPackBuilder::new).main = entry.getValue();
                    case "italic" -> fontBuilders.computeIfAbsent(name, FontPackBuilder::new).italic = entry.getValue();
                    case "bold" -> fontBuilders.computeIfAbsent(name, FontPackBuilder::new).bold = entry.getValue();
                    case "bold_italic" -> fontBuilders.computeIfAbsent(name, FontPackBuilder::new).boldItalic = entry.getValue();
                    default -> ImGuiMCImpl.LOGGER.warn("Unknown font type {} for font: {}", type, name);
                }
            }

            ImGuiMCImpl.LOGGER.info("Loaded {} ImGui fonts", fontBuilders.size());

            this.fontBuilders = Map.copyOf(fontBuilders);
            ImGuiMCImpl.handler.updateFonts();
        }, gameExecutor);
    }

    @Override
    public @NotNull String getName() {
        return "font_manager";
    }

    @Override
    public void rebuildFonts(final ImFontAtlas atlas) {
        final Map<ResourceLocation, FontPack> fonts = new HashMap<>();

        atlas.clear();

        final float scale = ImGuiFontManager.getFontScale();
        for (final Map.Entry<ResourceLocation, FontPackBuilder> entry : this.fontBuilders.entrySet()) {
            ImGuiMCImpl.LOGGER.info("Built {}", entry.getKey());
            fonts.put(entry.getKey(), entry.getValue().build(scale));
        }

        this.fonts = Map.copyOf(fonts);
        this.defaultFont = this.getFont(ImGuiMC.FONT_DEFAULT, false, false);
        if (this.defaultFont == null) {
            ImGuiMCImpl.LOGGER.error("Failed to load default font: {}, using ImGui default", ImGuiMC.FONT_DEFAULT);
            this.defaultFont = atlas.addFontDefault();
        }

        ImGui.getIO().setFontDefault(this.defaultFont);

        ImGuiMCPlatform.INSTANCE.registerImGuiFonts(atlas, this.defaultFont, scale);
    }

    @Override
    public void free() {
        for (final ImGuiFontManagerImpl.FontPack fontPack : this.fonts.values()) {
            fontPack.free();
        }
        this.fonts = Map.of();
    }

    private record FontPack(ImFont regular,
                            ImFont italic,
                            ImFont bold,
                            ImFont boldItalic,
                            ImFontConfig[] configs) implements NativeResource {

        @Override
        public void free() {
            this.regular.destroy();
            if (this.italic != this.regular) {
                this.italic.destroy();
            }
            if (this.bold != this.regular) {
                this.bold.destroy();
            }
            if (this.boldItalic != this.regular) {
                this.boldItalic.destroy();
            }
            for (final ImFontConfig config : this.configs) {
                config.destroy();
            }
        }
    }

    private static class FontPackBuilder implements NativeResource {

        private final ResourceLocation name;
        private final List<ImFontConfig> configs;
        private FontData main;
        private FontData italic;
        private FontData bold;
        private FontData boldItalic;

        private FontPackBuilder(final ResourceLocation name) {
            this.name = name;
            this.configs = new ArrayList<>(4);
        }

        private ImFont loadOrDefault(@Nullable final FontData data, final String type, final float scale, final ImFont defaultFont) {
            if (data == null) {
                return defaultFont;
            } else {
                final ImFontConfig fontConfig = new ImFontConfig();
                this.configs.add(fontConfig);
                final float sizePixels = data.size * scale;
                fontConfig.setGlyphRanges(data.ranges);
                fontConfig.setName(this.name.getPath() + " " + type + " " + FONT_FORMAT.format(sizePixels) + " px");
                fontConfig.setPixelSnapH(true);
                return ImGui.getIO().getFonts().addFontFromMemoryTTF(data.bytes, sizePixels, fontConfig);
            }
        }

        public FontPack build(final float scale) {
            final ImFont main = Objects.requireNonNull(this.loadOrDefault(this.main, "regular", scale, null));
            final ImFont italic = this.loadOrDefault(this.italic, "italic", scale, main);
            final ImFont bold = this.loadOrDefault(this.bold, "bold", scale, main);
            final ImFont boldItalic = this.loadOrDefault(this.boldItalic, "bold_italic", scale, bold);
            return new FontPack(main, italic, bold, boldItalic, this.configs.toArray(ImFontConfig[]::new));
        }

        @Override
        public void free() {
            final Iterator<ImFontConfig> iterator = this.configs.iterator();
            while (iterator.hasNext()) {
                iterator.next().destroy();
                iterator.remove();
            }

            this.main = null;
            this.italic = null;
            this.bold = null;
            this.boldItalic = null;
        }
    }

    private record FontData(byte[] bytes, short[] ranges, float size) {
    }
}
//?}