package foundry.imgui.impl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.imgui.impl.font.ImGuiFontManager;
import foundry.imgui.impl.platform.ImGuiMCPlatform;
import foundry.imgui.impl.renderer.ImGuiRenderer;
import imgui.ImGui;
import imgui.extension.implot.ImPlot;
import imgui.extension.implot.ImPlotContext;
import imgui.flag.ImGuiConfigFlags;
import imgui.internal.ImGuiContext;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;

@ApiStatus.Internal
public class ImGuiHandler {

    private final long mainWindow;
    private final ImGuiWindowImpl windowImpl;
    private final ImGuiRenderer rendererImpl;
    private final ImGuiFontManager fontManager;
    private final ImGuiContext imGuiContext;
    private final ImPlotContext imPlotContext;
    private final AtomicBoolean active;
    private final AtomicBoolean fontsDirty;

    public ImGuiHandler(final long mainWindow) {
        this.mainWindow = mainWindow;
        this.windowImpl = new ImGuiWindowImpl(this);
        this.rendererImpl = ImGuiMCPlatform.INSTANCE.createRenderer();
        this.fontManager = ImGuiMCPlatform.INSTANCE.createFontManager();

        ImGuiStateStack.push();
        ImGuiContext imGuiContext = null;
        ImPlotContext imPlotContext = null;
        try {
            imGuiContext = ImGui.createContext();
            imPlotContext = ImPlot.createContext();
            this.active = new AtomicBoolean();
            this.fontsDirty = new AtomicBoolean();
            ImGuiMCPlatform.INSTANCE.imGuiLoadPre();
            this.rendererImpl.init();
            this.windowImpl.init(mainWindow, true);
            ImGuiMCPlatform.INSTANCE.imGuiLoadPost();

            // TODO style sheet init event
//            VeilImGuiStylesheet.initStyles();
        } catch (final Throwable t) {
            // Make sure nothing leaks when an error occurs
            this.windowImpl.shutdown();
            this.rendererImpl.free();
            if (imGuiContext != null) {
                ImGui.destroyContext(imGuiContext);
            }
            if (imPlotContext != null) {
                ImPlot.destroyContext(imPlotContext);
            }
            throw t;
        } finally {
            ImGuiStateStack.forcePop();
        }
        this.imGuiContext = imGuiContext;
        this.imPlotContext = imPlotContext;
    }

    public void start() {
        // These callbacks MUST be called from the main thread
        RenderSystem.assertOnRenderThread();

        ImGuiStateStack.push();
        ImGui.setCurrentContext(this.imGuiContext);
        ImPlot.setCurrentContext(this.imPlotContext);

        // Sanity check
        if (ImGui.getCurrentContext().isNotValidPtr()) {
            throw new IllegalStateException("ImGui Context is not valid");
        }
    }

    public void stop() {
        RenderSystem.assertOnRenderThread();
        ImGuiStateStack.pop();
    }

    public void beginFrame() {
        try {
            this.start();

            if (this.active.get()) {
                ImGuiMCImpl.LOGGER.error("ImGui failed to render previous frame, disposing");
                ImGui.endFrame();
            }
            this.active.set(true);
            if (this.fontsDirty.getAndSet(false)) {
                this.fontManager.rebuildFonts(ImGui.getIO().getFonts());
                this.rendererImpl.recreateFontsTexture();
            }
            this.rendererImpl.newFrame();
            this.windowImpl.newFrame(ImGuiMCImpl.getMainRenderTarget());
            ImGui.newFrame();

            ImGuiMCPlatform.INSTANCE.drawImGuiPre();
//            AdvancedFboImGuiAreaImpl.begin();
//            VeilRenderSystem.renderer().getEditorManager().render();
        } finally {
            this.stop();
        }
    }

    public void endFrame() {
        try {
            if (!this.active.get()) {
                ImGuiMCImpl.LOGGER.error("ImGui state de-synced");
                return;
            }

            this.start();

            ImGuiMCPlatform.INSTANCE.drawImGuiPost();

            this.active.set(false);

            final RenderTarget renderTarget = ImGuiMCImpl.getMainRenderTarget();

            //? if >=1.21.6 {
            /*final com.mojang.blaze3d.textures.GpuTextureView view = renderTarget.getColorTextureView();
            final int framebufferWidth = view.getWidth(0);
            final int framebufferHeight = view.getHeight(0);
            *///? } else {
            final int framebufferWidth = renderTarget.width;
            final int framebufferHeight = renderTarget.height;
            //? }

            ImGui.render();
            if (this.windowImpl.isCorrectSize(framebufferWidth, framebufferHeight)) {
                this.rendererImpl.renderDrawData(ImGui.getDrawData(), renderTarget);
            } else {
                this.rendererImpl.discard();
            }

            if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
                final long backupWindowPtr = glfwGetCurrentContext();
                ImGui.updatePlatformWindows();
                ImGui.renderPlatformWindowsDefault();
                glfwMakeContextCurrent(backupWindowPtr);
            }
        } finally {
            ImGuiStateStack.forcePop();
        }
    }

    public void updateFonts() {
        this.fontsDirty.set(true);
    }

    public ImGuiRenderer getRenderer() {
        return this.rendererImpl;
    }

    public ImGuiFontManager getFontManager() {
        return this.fontManager;
    }

    public long getWindow() {
        return this.mainWindow;
    }
}
