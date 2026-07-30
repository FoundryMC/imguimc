package foundry.imgui.impl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import imgui.ImGuiViewport;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NativeResource;
import java.util.function.Supplier;

@ApiStatus.Internal
public interface ImGuiWindowHandler {

    boolean init(Window window, boolean installCallbacks, ClientApi clientApi);

    void swapBuffers(ImGuiViewport viewport);

    void shutdown();

    float getContentScaleForMonitor(long monitor);

    void newFrame(RenderTarget mainRenderTarget);

    boolean isCorrectSize(int width, int height);

    @Contract("null,_->null;!null,!null->!null")
    <T extends RenderViewportData> @Nullable T getRenderData(ImGuiViewport vp, Supplier<T> factory);

    interface RenderViewportData extends NativeResource {

        RenderTarget getRenderTarget();
    }

    // Mirrors C++ enum GlfwClientApi. UNKNOWN matches C++'s GlfwClientApi_Unknown ("anything else").
    enum ClientApi {
        UNKNOWN,
        OPENGL,
        VULKAN,
        OTHER
    }
}