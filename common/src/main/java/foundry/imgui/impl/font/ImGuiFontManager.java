package foundry.imgui.impl.font;

import static org.lwjgl.glfw.GLFW.*;
import foundry.imgui.impl.ImGuiWindowImpl;
import imgui.ImFont;
import imgui.ImFontAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import java.nio.FloatBuffer;

@ApiStatus.Internal
public interface ImGuiFontManager extends PreparableReloadListener, NativeResource {

    short[] DEFAULT_FONT_RANGES = new short[]{0x0020, 0x00FF, 0};
    float DEFAULT_FONT_SIZE = 20.0F;

    ImFont getFont(@Nullable ResourceLocation name, boolean bold, boolean italic);

    void rebuildFonts(ImFontAtlas atlas);

    static float getFontScale() {
        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final FloatBuffer xscale = stack.mallocFloat(1);
            final FloatBuffer yscale = stack.mallocFloat(1);
            final long monitor = glfwGetPrimaryMonitor();
            glfwGetMonitorContentScale(monitor, xscale, yscale);
            return Math.max(ImGuiWindowImpl.getContentScaleForMonitor(monitor), Math.max(xscale.get(0), yscale.get(0)));
        }
    }
}
