package foundry.imgui.impl.platform;

import foundry.imgui.api.ImGuiMC;
import foundry.imgui.api.event.ImGuiLoadEvent;
import foundry.imgui.api.event.ImGuiLoadEvents;
import foundry.imgui.api.event.RegisterImGuiFontsEvent;
import foundry.imgui.api.event.RenderImGuiEvents;
import foundry.imgui.impl.font.ImGuiFontManager;
import foundry.imgui.impl.renderer.ImGuiRenderer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.util.ServiceLoader;

@ApiStatus.Internal
public interface ImGuiMCPlatform extends ImGuiLoadEvent, ImGuiLoadEvents.Pre, ImGuiLoadEvents.Post, RegisterImGuiFontsEvent, RenderImGuiEvents.Pre, RenderImGuiEvents.Post {

    ImGuiMCPlatform INSTANCE = ServiceLoader.load(ImGuiMCPlatform.class, ImGuiMC.class.getClassLoader())
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Failed to find platform provider"));

    @Contract(pure = true)
    default ImGuiRenderer createRenderer() {
        //? if < 1.21.6 {
        return new foundry.imgui.impl.renderer.v0.ImGuiRendererGL33();
        //? } else {
        /*return new foundry.imgui.impl.renderer.v1.ImGuiRenderImplRenderSystem();
        *///? }
    }

    @Contract(pure = true)
    default ImGuiFontManager createFontManager() {
        //? if < 1.21.4 {
        return new foundry.imgui.impl.font.v0.ImGuiFontManagerImpl();
        //? } else {
        /*return new foundry.imgui.impl.font.v1.ImGuiFontManagerImpl();
        *///? }
    }
}
