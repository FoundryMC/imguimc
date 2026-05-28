package foundry.imgui.api;

import foundry.imgui.api.event.ImGuiLoadEvent;
import foundry.imgui.api.event.ImGuiLoadEvents;
import foundry.imgui.api.event.RegisterImGuiFontsEvent;
import foundry.imgui.api.event.RenderImGuiEvents;
import org.jetbrains.annotations.ApiStatus;

import java.util.ServiceLoader;

/**
 * Manages platform-specific implementations of event subscriptions.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public interface ImGuiMCEvents {

    ImGuiMCEvents INSTANCE = ServiceLoader.load(ImGuiMCEvents.class).findFirst().orElseThrow(() -> new RuntimeException("Failed to find platform event provider"));

    /**
     * @since 1.1.0
     * @deprecated Use {@link #imGuiLoadPre(ImGuiLoadEvents.Pre)}
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "2.0.0")
    @Deprecated
    void onImGuiLoad(ImGuiLoadEvent event);

    /**
     * @since 1.3.0
     */
    void imGuiLoadPre(ImGuiLoadEvents.Pre event);

    /**
     * @since 1.3.0
     */
    void imGuiLoadPost(ImGuiLoadEvents.Post event);

    void onRegisterImGuiFonts(RegisterImGuiFontsEvent event);

    /**
     * @deprecated Use {@link #preRenderImGuiEvent(RenderImGuiEvents.Pre)}
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "2.0.0")
    @Deprecated
    default void preRenderImGuiEvents(RenderImGuiEvents.Pre event) {
        this.preRenderImGuiEvent(event);
    }

    /**
     * @deprecated Use {@link #postRenderImGuiEvent(RenderImGuiEvents.Post)}
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "2.0.0")
    @Deprecated
    default void postRenderImGuiEvents(RenderImGuiEvents.Post event) {
        this.postRenderImGuiEvent(event);
    }

    /**
     * @since 1.2.0
     */
    void preRenderImGuiEvent(RenderImGuiEvents.Pre event);

    /**
     * @since 1.2.0
     */
    void postRenderImGuiEvent(RenderImGuiEvents.Post event);

}
