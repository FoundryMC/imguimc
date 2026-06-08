package foundry.imgui.api.event;

import foundry.imgui.api.ImGuiMC;

/**
 * Events fired when the ImGui frame begins and ends.
 *
 * @since 1.0.0
 */
public final class RenderImGuiEvents {

    private RenderImGuiEvents() {
    }

    /**
     * Called right after the frame starts.
     */
    public interface Pre {

        /**
         * Draws ImGui elements first.
         * <br>
         * The context is already current, so there's no need to call {@link ImGuiMC#withImGui}
         */
        void drawImGuiPre();
    }

    /**
     * Called right before ending the frame.
     */
    public interface Post {

        /**
         * Draws ImGui elements last.
         * <br>
         * The context is already current, so there's no need to call {@link ImGuiMC#withImGui}
         */
        void drawImGuiPost();
    }
}
