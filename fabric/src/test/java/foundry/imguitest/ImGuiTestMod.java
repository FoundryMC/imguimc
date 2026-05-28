package foundry.imguitest;

import foundry.imgui.api.ImGuiMC;
import foundry.imgui.api.ImGuiMCEvents;
import imgui.ImGui;
import net.fabricmc.api.ClientModInitializer;

public class ImGuiTestMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ImGuiMCEvents.INSTANCE.preRenderImGuiEvent(ImGui::showDemoWindow);
    }
}
