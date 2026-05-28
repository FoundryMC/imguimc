package foundry.imgui.neoforge.api.event;

import foundry.imgui.api.ImGuiMC;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired before and after ImGui loads successfully.
 *
 * @since 1.3.0
 */
public abstract class ImGuiLoadEventsNeoforge extends Event implements IModBusEvent {

    ImGuiLoadEventsNeoforge() {
    }

    /**
     * Fired right before ImGui handlers are initialized.
     * <br>
     * The context is already current, so there's no need to call {@link ImGuiMC#withImGui}
     */
    public static final class Pre extends ImGuiLoadEventsNeoforge {

        @ApiStatus.Internal
        public Pre() {
        }
    }

    /**
     * Fired right after ImGui handlers are initialized.
     * <br>
     * The context is already current, so there's no need to call {@link ImGuiMC#withImGui}
     */
    public static final class Post extends ImGuiLoadEventsNeoforge {

        @ApiStatus.Internal
        public Post() {
        }
    }
}
