package foundry.imguitest;

import foundry.imgui.api.ImGuiMC;
import foundry.imgui.api.ImGuiMCEvents;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ImGuiTestMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ImGuiMCEvents.INSTANCE.imGuiLoadPre(() -> {
            ImGui.getIO().addConfigFlags(
                    ImGuiConfigFlags.NavEnableKeyboard |
                            ImGuiConfigFlags.NavEnableGamepad |
                            ImGuiConfigFlags.NavEnableSetMousePos |
                            ImGuiConfigFlags.DockingEnable |
                            ImGuiConfigFlags.ViewportsEnable);
        });
        ImGuiMCEvents.INSTANCE.preRenderImGuiEvent(() -> {
            ImGui.showDemoWindow();
            if (ImGui.begin("Test Dock")) {
                ImGui.dockSpace(1);
            }
            ImGui.end();

            final Player player = Minecraft.getInstance().player;
            if (player != null) {
                if (ImGui.begin("Test Component")) {
                    ImGuiMC.component(Minecraft.getInstance().player.getDisplayName());
                    ImGuiMC.component(Component.keybind("key.forward").withStyle(ChatFormatting.BOLD, ChatFormatting.RED, ChatFormatting.ITALIC));
                }
                ImGui.end();
            }

            ImGui.showMetricsWindow();
        });
    }
}
