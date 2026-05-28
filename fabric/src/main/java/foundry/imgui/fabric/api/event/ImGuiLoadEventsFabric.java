package foundry.imgui.fabric.api.event;

import foundry.imgui.api.event.ImGuiLoadEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired before and after ImGui loads successfully.
 *
 * @since 1.3.0
 */
public final class ImGuiLoadEventsFabric {

    public static final Event<ImGuiLoadEvents.Pre> PRE = EventFactory.createArrayBacked(ImGuiLoadEvents.Pre.class, () -> {
    }, events -> () -> {
        for (ImGuiLoadEvents.Pre event : events) {
            event.imGuiLoadPre();
        }
    });
    public static final Event<ImGuiLoadEvents.Post> POST = EventFactory.createArrayBacked(ImGuiLoadEvents.Post.class, () -> {
    }, events -> () -> {
        for (ImGuiLoadEvents.Post event : events) {
            event.imGuiLoadPost();
        }
    });
}
