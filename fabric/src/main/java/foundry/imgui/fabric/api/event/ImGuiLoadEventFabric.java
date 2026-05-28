package foundry.imgui.fabric.api.event;

import foundry.imgui.api.event.ImGuiLoadEvent;
import foundry.imgui.api.event.ImGuiLoadEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired after ImGui loads successfully.
 *
 * @since 1.1.0
 * @deprecated Use {@link ImGuiLoadEventsFabric#POST}
 */
@ApiStatus.ScheduledForRemoval(inVersion = "3.0.0")
@Deprecated
public interface ImGuiLoadEventFabric extends ImGuiLoadEvent {

    Event<ImGuiLoadEvent> EVENT = EventFactory.createArrayBacked(ImGuiLoadEvent.class, () -> {
    }, events -> () -> {
        for (ImGuiLoadEvent event : events) {
            event.afterImGuiLoad();
        }
    });
}
