package foundry.imgui.neoforge.api.event;

import foundry.imgui.api.ImGuiMC;
import imgui.ImFont;
import imgui.ImFontAtlas;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired when the font atlas is rebuilt before the next ImGui frame.
 *
 * @see ImGuiMC#rebuildFonts()
 * @since 1.0.0
 */
public final class RegisterImGuiFontsEventNeoforge extends Event implements IModBusEvent {

    private final ImFontAtlas atlas;
    private final ImFont defaultFont;
    private final float fontScale;

    @ApiStatus.Internal
    public RegisterImGuiFontsEventNeoforge(final ImFontAtlas atlas, final ImFont defaultFont, final float fontScale) {
        this.atlas = atlas;
        this.defaultFont = defaultFont;
        this.fontScale = fontScale;
    }

    public ImFontAtlas getAtlas() {
        return this.atlas;
    }

    public ImFont getDefaultFont() {
        return this.defaultFont;
    }

    public float getFontScale() {
        return this.fontScale;
    }
}
