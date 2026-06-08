package foundry.imgui.api.event;

import foundry.imgui.api.ImGuiMC;

/**
 * Fired before and after ImGui loads successfully.
 *
 * @since 2.0.0
 */
public final class ImGuiLoadEvents {

    private ImGuiLoadEvents() {
    }

    /**
     * Fired right before ImGui handlers are initialized.
     */
    public interface Pre {

        /**
         * Called right before ImGui handlers are initialized. This gives the opportunity to set platform flags before windows are set up.
         * <br>
         * The context is already current, so there's no need to call {@link ImGuiMC#withImGui}
         */
        void imGuiLoadPre();
    }

    /**
     * Fired right after ImGui handlers are initialized.
     */
    public interface Post {

        /**
         * Called after ImGui loads successfully.
         * <br>
         * The context is already current, so there's no need to call {@link ImGuiMC#withImGui}
         */
        void imGuiLoadPost();
    }
}
