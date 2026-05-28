package foundry.imgui.api.event;

import org.jetbrains.annotations.ApiStatus;

/**
 * Fired after ImGui loads successfully.
 *
 * @since 1.1.0
 * @deprecated Use {@link ImGuiLoadEvents.Post}
 */
@ApiStatus.ScheduledForRemoval(inVersion = "2.0.0")
@Deprecated
public interface ImGuiLoadEvent {

    void afterImGuiLoad();
}
