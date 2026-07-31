package foundry.imgui.impl.font;

import foundry.imgui.impl.ImGuiMCImpl;
import imgui.ImFont;
import imgui.ImFontAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NativeResource;

@ApiStatus.Internal
public interface ImGuiFontManager extends PreparableReloadListener, NativeResource {

    short[] DEFAULT_FONT_RANGES = new short[]{0x0020, 0x00FF, 0};
    float DEFAULT_FONT_SIZE = 20.0F;

    ImFont getFont(@Nullable ResourceLocation name, boolean bold, boolean italic);

    void rebuildFonts(ImFontAtlas atlas);

    static float getFontScale() {
        return ImGuiMCImpl.getPrimaryMonitorScale();
    }
}
