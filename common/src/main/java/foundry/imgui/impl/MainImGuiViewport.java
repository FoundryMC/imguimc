package foundry.imgui.impl;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import foundry.imgui.api.ImGuiMC;
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiStyleVar;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.system.MemoryStack;
import java.nio.IntBuffer;

/**
 * Handles the state changes between using the main Minecraft framebuffer and a custom one for the main window.
 */
@ApiStatus.Internal
public class MainImGuiViewport {

    private static final Vector4f CLEAR_COLOR = new Vector4f(0, 0, 0, 1);

    private final Vector2i oldSize;
    private final Vector2i size;
    private final Vector2f cursorOrigin;
    private final Vector2i mainWindowSize;
    private ImGuiViewport mainViewport;
    private RenderTarget replacementRenderTarget;
    private boolean mainViewportVisible;
    private boolean captured;
    private boolean captureMouse;
    private boolean captureKeyboard;

    public MainImGuiViewport() {
        this.oldSize = new Vector2i();
        this.size = new Vector2i();
        this.cursorOrigin = new Vector2f();
        this.mainWindowSize = new Vector2i();
    }

    /**
     * @return The render target to draw all ImGui windows to for the primary window
     */
    public RenderTarget getRenderTarget() {
        return this.captured ? this.replacementRenderTarget : ImGuiMCImpl.getMainRenderTarget();
    }

    public boolean isCaptured() {
        return this.captured;
    }

    public void update(final RenderTarget renderTarget, final Window window, final boolean capturing) {
        this.mainViewport = null;

        if (this.captured || capturing) {
            if (!this.captured) {
                this.oldSize.set(-1, -1);
                ImGui.setNextWindowContentSize(renderTarget.width, renderTarget.height);
            }

            try (final MemoryStack stack = MemoryStack.stackPush()) {
                final IntBuffer w = stack.mallocInt(1);
                final IntBuffer h = stack.mallocInt(1);
                glfwGetFramebufferSize(ImGuiMCImpl.getWindowHandle(window), w, h);
                this.mainWindowSize.set(w.get(0), h.get(0));
            }

            if (ImGui.begin("Viewport")) {
                this.mainViewportVisible = true;

                ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
                final float width = ImGui.getContentRegionAvailX();
                final float height = ImGui.getContentRegionAvailY();
                ImGuiMC.image(ImGuiMC.getColorTexture(renderTarget), width, height, 0, 1, 1, 0);
                ImGui.popStyleVar();

                this.cursorOrigin.set(ImGui.getItemRectMinX(), ImGui.getItemRectMinY());

                if (ImGui.isItemHovered()) {
                    this.captureMouse = false;
                }
                if (ImGui.isWindowFocused()) {
                    this.captureKeyboard = false;
                }

                this.size.set((int) width, (int) height);
                this.mainViewport = ImGui.getWindowViewport();
            } else {
                this.cursorOrigin.set(0, 0);
                this.mainViewportVisible = false;
                this.size.set(this.mainWindowSize);
            }
            ImGui.end();

            if (this.replacementRenderTarget == null) {
                this.replacementRenderTarget = ImGuiMCImpl.createRenderTarget(this.mainWindowSize.x, this.mainWindowSize.y, false);
            } else if (this.replacementRenderTarget.width != this.mainWindowSize.x || this.replacementRenderTarget.height != this.mainWindowSize.y) {
                ImGuiMCImpl.resizeRenderTarget(this.replacementRenderTarget, this.mainWindowSize.x, this.mainWindowSize.y);
            }

            //? if >=26.2-pre-2 {
            /*com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.replacementRenderTarget.getColorTexture(), CLEAR_COLOR);
             *///? } else if >=1.21.5 {
            /*com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.replacementRenderTarget.getColorTexture(), 0xFF000000);
             *///? } else if >=1.21.2 {
            /*this.replacementRenderTarget.clear();
             *///? } else {
            this.replacementRenderTarget.clear(Minecraft.ON_OSX);
            //? }
        } else {
            this.mainViewportVisible = true;
            if (this.replacementRenderTarget != null) {
                this.replacementRenderTarget.destroyBuffers();
                this.replacementRenderTarget = null;
            }
        }
    }

    public void setCaptured(final boolean captured) {
        this.captured = captured;
    }

    public void updateCapturedViewport() {
        this.captureMouse = true;
        this.captureKeyboard = true;
        if (!this.captured) {
            return;
        }

        if (!this.oldSize.equals(this.size)) {
            //? if >=26.1 {
            /*Minecraft.getInstance().resizeGui();
             *///? } else {
            Minecraft.getInstance().resizeDisplay();
            //? }
            this.oldSize.set(this.size);
        }
    }

    public Vector2ic size() {
        return this.size;
    }

    public Vector2fc cursorPos() {
        return this.cursorOrigin;
    }

    public boolean isCaptureMouse() {
        return !this.captureMouse;
    }

    public boolean isCaptureKeyboard() {
        return !this.captureKeyboard;
    }

    /**
     * @return The viewport of the main Minecraft viewport or <code>null</code> to not send events to Minecraft
     */
    public @Nullable ImGuiViewport imGuiViewport() {
        return this.mainViewport;
    }
}
