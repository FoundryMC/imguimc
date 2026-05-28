package foundry.imguitest;

import foundry.imgui.api.ImGuiMC;
import foundry.imgui.api.ImGuiMCEvents;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import net.fabricmc.api.ClientModInitializer;

public class ImGuiTestMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ImGuiMCEvents.INSTANCE.imGuiLoadPre(() -> {
            try (final ImGuiMC.ActiveContext ctx = ImGuiMC.withImGui()) {
                if (ctx != null) {
                    ctx.io().addConfigFlags(
                            ImGuiConfigFlags.NavEnableKeyboard |
                            ImGuiConfigFlags.NavEnableGamepad |
                            ImGuiConfigFlags.NavEnableSetMousePos |
                            ImGuiConfigFlags.DockingEnable |
                            ImGuiConfigFlags.ViewportsEnable);
                }
            }
        });
        ImGuiMCEvents.INSTANCE.preRenderImGuiEvent(ImGui::showDemoWindow);
    }
}
