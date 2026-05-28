package foundry.imgui.neoforge.impl;

import foundry.imgui.api.ImGuiMCEvents;
import foundry.imgui.api.event.ImGuiLoadEvent;
import foundry.imgui.api.event.ImGuiLoadEvents;
import foundry.imgui.api.event.RegisterImGuiFontsEvent;
import foundry.imgui.api.event.RenderImGuiEvents;
import foundry.imgui.neoforge.api.event.ImGuiLoadEventNeoforge;
import foundry.imgui.neoforge.api.event.ImGuiLoadEventsNeoforge;
import foundry.imgui.neoforge.api.event.RegisterImGuiFontsEventNeoforge;
import foundry.imgui.neoforge.api.event.RenderImGuiEventsNeoforge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public class ImGuiMCEventsImpl implements ImGuiMCEvents {

    private static IEventBus modBus() {
        final IEventBus modBus = ModLoadingContext.get().getActiveContainer().getEventBus();
        return Objects.requireNonNull(modBus, "Call this in your mod constructor");
    }

    @Override
    public void onImGuiLoad(final ImGuiLoadEvent event) {
        modBus().<ImGuiLoadEventNeoforge>addListener(forgeEvent -> event.afterImGuiLoad());
    }

    @Override
    public void imGuiLoadPre(final ImGuiLoadEvents.Pre event) {
        modBus().<ImGuiLoadEventsNeoforge.Pre>addListener(forgeEvent -> event.imGuiLoadPre());
    }

    @Override
    public void imGuiLoadPost(final ImGuiLoadEvents.Post event) {
        modBus().<ImGuiLoadEventsNeoforge.Post>addListener(forgeEvent -> event.imGuiLoadPost());
    }

    @Override
    public void onRegisterImGuiFonts(final RegisterImGuiFontsEvent event) {
        NeoForge.EVENT_BUS.<RegisterImGuiFontsEventNeoforge>addListener(forgeEvent -> event.registerImGuiFonts(forgeEvent.getAtlas(), forgeEvent.getDefaultFont(), forgeEvent.getFontScale()));
    }

    @Override
    public void preRenderImGuiEvent(final RenderImGuiEvents.Pre event) {
        NeoForge.EVENT_BUS.<RenderImGuiEventsNeoforge.Pre>addListener(forgeEvent -> event.drawImGuiPre());
    }

    @Override
    public void postRenderImGuiEvent(final RenderImGuiEvents.Post event) {
        NeoForge.EVENT_BUS.<RenderImGuiEventsNeoforge.Post>addListener(forgeEvent -> event.drawImGuiPost());
    }
}
