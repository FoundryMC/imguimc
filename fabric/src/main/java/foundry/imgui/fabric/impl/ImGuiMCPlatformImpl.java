package foundry.imgui.fabric.impl;

import foundry.imgui.fabric.api.event.ImGuiLoadEventsFabric;
import foundry.imgui.fabric.api.event.RegisterImGuiFontsEventFabric;
import foundry.imgui.fabric.api.event.RenderImGuiEventsFabric;
import foundry.imgui.fabric.api.event.ImGuiLoadEventFabric;
import foundry.imgui.impl.platform.ImGuiMCPlatform;
import imgui.ImFont;
import imgui.ImFontAtlas;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ImGuiMCPlatformImpl implements ImGuiMCPlatform {

    @Override
    public void registerImGuiFonts(final ImFontAtlas atlas, final ImFont defaultFont, final float fontScale) {
        RegisterImGuiFontsEventFabric.EVENT.invoker().registerImGuiFonts(atlas, defaultFont, fontScale);
    }

    @Override
    public void drawImGuiPre() {
        RenderImGuiEventsFabric.PRE.invoker().drawImGuiPre();
    }

    @Override
    public void drawImGuiPost() {
        RenderImGuiEventsFabric.POST.invoker().drawImGuiPost();
    }

    @Override
    public void afterImGuiLoad() {
        ImGuiLoadEventFabric.EVENT.invoker().afterImGuiLoad();
    }

    @Override
    public void imGuiLoadPre() {
        ImGuiLoadEventsFabric.PRE.invoker().imGuiLoadPre();
    }

    @Override
    public void imGuiLoadPost() {
        ImGuiLoadEventsFabric.POST.invoker().imGuiLoadPost();
    }
}
