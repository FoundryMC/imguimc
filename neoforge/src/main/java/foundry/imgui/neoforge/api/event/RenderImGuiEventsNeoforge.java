package foundry.imgui.neoforge.api.event;

import foundry.imgui.api.ImGuiMC;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus;

/**
 * Events fired when the ImGui frame begins and ends.
 *
 * @since 1.0.0
 */
public abstract class RenderImGuiEventsNeoforge extends Event {

    RenderImGuiEventsNeoforge() {
    }

    /**
     * Called right after the frame starts.
     * <br>
     * The context is already current, so there's no need to call {@link ImGuiMC#withImGui}
     */
    public static final class Pre extends RenderImGuiEventsNeoforge {

        @ApiStatus.Internal
        public Pre() {
        }
    }

    /**
     * Called right before ending the frame.
     * <br>
     * The context is already current, so there's no need to call {@link ImGuiMC#withImGui}
     */
    public static final class Post extends RenderImGuiEventsNeoforge {

        @ApiStatus.Internal
        public Post() {
        }
    }
}
