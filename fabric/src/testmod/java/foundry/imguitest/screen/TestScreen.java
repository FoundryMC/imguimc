package foundry.imguitest.screen;

import foundry.imgui.api.ImGuiMC;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.type.ImBoolean;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TestScreen extends Screen {

    private final ImBoolean showDemoWindow;

    public TestScreen() {
        super(Component.literal("Test Screen"));
        this.showDemoWindow = new ImBoolean(false);
    }

    private void render(final ImGuiIO io) {
        if (ImGui.begin("Hello, World!")) {
            ImGui.setWindowSize(800, 600);
            ImGui.checkbox("Show Demo Window", this.showDemoWindow);
        }
        ImGui.end();

        if (this.showDemoWindow.get()) {
            ImGui.showDemoWindow(this.showDemoWindow);
        }
    }

    @Override
    public void extractRenderState(@NotNull final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        try (final ImGuiMC.ActiveContext ctx = ImGuiMC.withImGui()) {
            if (ctx != null) {
                this.render(ctx.io());
            }
        }
    }
}
