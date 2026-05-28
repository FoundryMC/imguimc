package foundry.imgui.neoforge.impl;

import foundry.imgui.impl.platform.ImGuiMCPlatform;
import foundry.imgui.neoforge.api.event.ImGuiLoadEventNeoforge;
import foundry.imgui.neoforge.api.event.ImGuiLoadEventsNeoforge;
import foundry.imgui.neoforge.api.event.RegisterImGuiFontsEventNeoforge;
import foundry.imgui.neoforge.api.event.RenderImGuiEventsNeoforge;
import imgui.ImFont;
import imgui.ImFontAtlas;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ImGuiMCPlatformImpl implements ImGuiMCPlatform {

    @Override
    public void registerImGuiFonts(final ImFontAtlas atlas, final ImFont defaultFont, final float fontScale) {
        ModLoader.postEvent(new RegisterImGuiFontsEventNeoforge(atlas, defaultFont, fontScale));
    }

    @Override
    public void drawImGuiPre() {
        NeoForge.EVENT_BUS.post(new RenderImGuiEventsNeoforge.Pre());
    }

    @Override
    public void drawImGuiPost() {
        NeoForge.EVENT_BUS.post(new RenderImGuiEventsNeoforge.Post());
    }

    @Override
    public void afterImGuiLoad() {
        ModLoader.postEvent(new ImGuiLoadEventNeoforge());
    }

    @Override
    public void imGuiLoadPre() {
        ModLoader.postEvent(new ImGuiLoadEventsNeoforge.Pre());
    }

    @Override
    public void imGuiLoadPost() {
        ModLoader.postEvent(new ImGuiLoadEventsNeoforge.Post());
    }
}
