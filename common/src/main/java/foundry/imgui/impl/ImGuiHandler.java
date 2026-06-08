package foundry.imgui.impl;

import static org.lwjgl.glfw.GLFW.*;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.imgui.impl.font.ImGuiFontManager;
import foundry.imgui.impl.platform.ImGuiMCPlatform;
import foundry.imgui.impl.renderer.ImGuiRenderer;
import imgui.ImGui;
import imgui.ImGuiPlatformIO;
import imgui.ImGuiViewport;
import imgui.extension.implot.ImPlot;
import imgui.extension.implot.ImPlotContext;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiViewportFlags;
import imgui.internal.ImGuiContext;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import java.util.concurrent.atomic.AtomicBoolean;

@ApiStatus.Internal
public class ImGuiHandler {

    private final Window mainWindow;
    private final ImGuiWindowImpl windowImpl;
    private final ImGuiRenderer rendererImpl;
    private final ImGuiFontManager fontManager;
    private final ImGuiContext imGuiContext;
    private final ImPlotContext imPlotContext;
    private final AtomicBoolean active;
    private final AtomicBoolean fontsDirty;
    private long frame;

    public ImGuiHandler(final Window mainWindow) {
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
            //? if >=1.21.6 {
            /*//? if >=26.2-pre-2 {
            /^final String backendName = RenderSystem.getDevice().getDeviceInfo().backendName().toLowerCase(java.util.Locale.ROOT);
            ^///? } else {
            final String backendName = RenderSystem.getDevice().getBackendName().toLowerCase(java.util.Locale.ROOT);
             //? }
            if (backendName.contains("vulkan")) {
                this.windowImpl.initForVulkan(mainWindow, true);
            } else if (backendName.contains("opengl")) {
                this.windowImpl.initForOpenGL(mainWindow, true);
            } else {
                this.windowImpl.initForOther(mainWindow, true);
            }
            *///? } else {
            this.windowImpl.initForOpenGL(mainWindow, true);
            //? }
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

            this.frame++;
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
            ImGui.getStyle().setFontScaleMain((float) Math.max(ImGuiFontManager.getFontScale(), Minecraft.getInstance().getWindow().getGuiScale() / 2.0));
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
            this.active.set(false);
            ImGuiMCPlatform.INSTANCE.drawImGuiPost();

            ImGui.render();
            final RenderTarget renderTarget = ImGuiMCImpl.getMainRenderTarget();
            this.rendererImpl.renderDrawData(ImGui.getDrawData(), renderTarget);
            this.rendererImpl.renderPlatformWindows(renderTarget);
        } finally {
            this.stop();
        }
    }

    public void swapBuffers() {
        try {
            this.start();
            if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
                final ImGuiPlatformIO platformIO = ImGui.getPlatformIO();
                final int viewportsSize = platformIO.getViewportsSize();
                for (int i = 1; i < viewportsSize; i++) {
                    final ImGuiViewport viewport = platformIO.getViewports(i);
                    if (viewport.hasFlags(ImGuiViewportFlags.IsMinimized)) {
                        continue;
                    }

                    this.swapBuffers(viewport);
                }
            }
        } finally {
            this.stop();
        }
    }

    private void swapBuffers(final ImGuiViewport viewport) {
        if (viewport.isNotValidPtr()) {
            return;
        }

        //? if >=26.2-pre-2 {
        /*final com.mojang.blaze3d.systems.GpuSurface windowSurface = ImGuiWindowImpl.getSurface(viewport);
        if (windowSurface == null || !windowSurface.isAcquired()) {
            return;
        }
        *///? }

        final ImGuiWindowImpl.GlfwClientApi clientApi = this.windowImpl.getClientApi();
        final long window = viewport.getPlatformHandle();

        //? if >= 26.2-pre-2 {
        /*final long oldContext;
        if (clientApi == ImGuiWindowImpl.GlfwClientApi.OPENGL) {
            oldContext = glfwGetCurrentContext();
            glfwMakeContextCurrent(window);
        } else {
            oldContext = 0;
        }

        windowSurface.present();

        if (clientApi == ImGuiWindowImpl.GlfwClientApi.OPENGL) {
            glfwMakeContextCurrent(oldContext);
        }
        *///? } else {
        if (clientApi == ImGuiWindowImpl.GlfwClientApi.OPENGL) {
            final long oldContext = glfwGetCurrentContext();
            glfwMakeContextCurrent(window);
            glfwSwapBuffers(window);
            glfwMakeContextCurrent(oldContext);
        }
        //? }
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
        return ImGuiMCImpl.getWindowHandle(this.mainWindow);
    }

    public long getFrame() {
        return this.frame;
    }
}
