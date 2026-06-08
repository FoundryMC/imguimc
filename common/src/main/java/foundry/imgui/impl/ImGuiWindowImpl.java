package foundry.imgui.impl;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import imgui.*;
import imgui.callback.*;
import imgui.flag.*;
import imgui.lwjgl3.glfw.ImGuiImplGlfwNative;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.*;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.Supplier;

/**
 * This class is a straightforward port of the
 * <a href="https://raw.githubusercontent.com/ocornut/imgui/32f4c234a8edd9a85b32a91c9e29afac15c50028/backends/imgui_impl_glfw.cpp">imgui_impl_glfw.cpp</a>.
 * <p>
 * It supports clipboard, gamepad, mouse and keyboard in the same way the original Dear ImGui code does. You can copy-paste this class in your codebase and
 * modify the rendering routine in the way you'd like.
 */
@ApiStatus.Internal
public class ImGuiWindowImpl {

    protected static final String OS = System.getProperty("os.name", "generic").toLowerCase();
    protected static final boolean IS_WINDOWS = OS.contains("win");
    protected static final boolean IS_APPLE = OS.contains("mac") || OS.contains("darwin");
    protected static final boolean IS_LINUX = OS.contains("linux");

    /**
     * Data class to store implementation specific fields.
     * Same as {@code ImGui_ImplGlfw_Data}.
     */
    protected static class Data {
        protected long window = -1;
        //? if >= 26.1 {
        /*protected com.mojang.blaze3d.systems.GpuBackend backend = null;
         *///? }
        protected GlfwClientApi clientApi = GlfwClientApi.UNKNOWN;
        protected double time = 0.0;
        protected long mouseWindow = -1;
        protected long[] mouseCursors = new long[ImGuiMouseCursor.COUNT];
        protected long lastMouseCursor = -1;
        protected boolean mouseIgnoreButtonUpWaitForFocusLoss = false;
        protected boolean mouseIgnoreButtonUp = false;
        protected ImVec2 lastValidMousePos = new ImVec2();
        protected long[] keyOwnerWindows = new long[GLFW_KEY_LAST];
        protected boolean isWayland = false;
        protected boolean installedCallbacks = false;
        protected boolean callbacksChainForAllWindows = false;
        protected boolean viewportEnable = false;

        // Chain GLFW callbacks: our callbacks will call the user's previously installed callbacks, if any.
        protected GLFWWindowFocusCallback prevUserCallbackWindowFocus = null;
        protected GLFWCursorPosCallback prevUserCallbackCursorPos = null;
        protected GLFWCursorEnterCallback prevUserCallbackCursorEnter = null;
        protected GLFWMouseButtonCallback prevUserCallbackMousebutton = null;
        protected GLFWScrollCallback prevUserCallbackScroll = null;
        protected GLFWKeyCallback prevUserCallbackKey = null;
        protected GLFWCharCallback prevUserCallbackChar = null;
        protected GLFWMonitorCallback prevUserCallbackMonitor = null;

        // This field is required to use GLFW with touch screens on Windows.
        // For compatibility reasons it was added here as a comment. But we don't use somewhere in the binding implementation.
        // protected long prevWndProc;
    }

    /**
     * Internal class to store containers for frequently used arrays.
     * This class helps minimize the number of object allocations on the JVM side,
     * thereby improving performance and reducing garbage collection overhead.
     */
    private static final class Properties {
        private final int[] windowW = new int[1];
        private final int[] windowH = new int[1];
        private final int[] windowX = new int[1];
        private final int[] windowY = new int[1];
        private int displayW;
        private int displayH;

        // For mouse tracking
        private final ImVec2 mousePosPrev = new ImVec2();
        private final double[] mouseX = new double[1];
        private final double[] mouseY = new double[1];

        // Scratch ImVec2 outputs for getWindowSizeAndFramebufferScale
        private final ImVec2 tmpDisplaySize = new ImVec2();
        private final ImVec2 tmpFbScale = new ImVec2();

        // Monitor properties
        private final int[] monitorX = new int[1];
        private final int[] monitorY = new int[1];
        private final int[] monitorWorkAreaX = new int[1];
        private final int[] monitorWorkAreaY = new int[1];
        private final int[] monitorWorkAreaWidth = new int[1];
        private final int[] monitorWorkAreaHeight = new int[1];
        private final float[] monitorContentScaleX = new float[1];
        private final float[] monitorContentScaleY = new float[1];


        // For char translation
        private final String charNames = "`-=[]\\,;'./";
        private final int[] charKeys = {
                GLFW_KEY_GRAVE_ACCENT, GLFW_KEY_MINUS, GLFW_KEY_EQUAL, GLFW_KEY_LEFT_BRACKET,
                GLFW_KEY_RIGHT_BRACKET, GLFW_KEY_BACKSLASH, GLFW_KEY_COMMA, GLFW_KEY_SEMICOLON,
                GLFW_KEY_APOSTROPHE, GLFW_KEY_PERIOD, GLFW_KEY_SLASH
        };
    }

    protected Data data = null;
    private final Properties props = new Properties();

    // We gather version tests as define in order to easily see which features are version-dependent.
    // In C++: GLFW_HAS_X11/GLFW_HAS_WAYLAND derive from compile-time _GLFW_X11/_GLFW_WAYLAND macros.
    // In Java: detect Wayland at runtime via glfwGetPlatform() (see isWayland helper), since LWJGL does not expose GLFW's compile-time flags.
    protected static final int glfwVersionCombined = GLFW_VERSION_MAJOR * 1000 + GLFW_VERSION_MINOR * 100 + GLFW_VERSION_REVISION;
    protected static final boolean glfwHasWindowTopmost = glfwVersionCombined >= 3200; // 3.2+ GLFW_FLOATING
    protected static final boolean glfwHasWindowHovered = glfwVersionCombined >= 3300; // 3.3+ GLFW_HOVERED
    protected static final boolean glfwHasWindowAlpha = glfwVersionCombined >= 3300; // 3.3+ glfwSetWindowOpacity
    protected static final boolean glfwHasPerMonitorDpi = glfwVersionCombined >= 3300; // 3.3+ glfwGetMonitorContentScale
    protected static final boolean glfwHasFocusWindow = glfwVersionCombined >= 3200; // 3.2+ glfwFocusWindow
    protected static final boolean glfwHasFocusOnShow = glfwVersionCombined >= 3300; // 3.3+ GLFW_FOCUS_ON_SHOW
    protected static final boolean glfwHasMonitorWorkArea = glfwVersionCombined >= 3300; // 3.3+ glfwGetMonitorWorkarea
    protected static final boolean glfwHasOsxWindowPosFix = glfwVersionCombined >= 3301; // 3.3.1+ Fixed: Resizing window repositions it on MacOS #1553
    protected static final boolean glfwHasNewCursors = glfwVersionCombined >= 3400; // 3.4+ GLFW_RESIZE_ALL_CURSOR, GLFW_RESIZE_NESW_CURSOR, GLFW_RESIZE_NWSE_CURSOR, GLFW_NOT_ALLOWED_CURSOR
    protected static final boolean glfwHasMousePassthrough = glfwVersionCombined >= 3400; // 3.4+ GLFW_MOUSE_PASSTHROUGH
    protected static final boolean glfwHasGamepadApi = glfwVersionCombined >= 3300; // 3.3+ glfwGetGamepadState() new api
    protected static final boolean glfwHasGetKeyName = glfwVersionCombined >= 3200; // 3.2+ glfwGetKeyName()
    protected static final boolean glfwHasGetError = glfwVersionCombined >= 3300; // 3.3+ glfwGetError()
    protected static final boolean glfwHasGetPlatform = glfwVersionCombined >= 3400; // 3.4+ glfwGetPlatform()

    private final ImGuiHandler bridge;

    public ImGuiWindowImpl(final ImGuiHandler bridge) {
        this.bridge = bridge;
    }

    protected ImStrSupplier getClipboardTextFn() {
        return new ImStrSupplier() {
            @Override
            public String get() {
                if (ImGuiWindowImpl.this.bridge.getWindow() == ImGuiWindowImpl.this.data.window) {
                    return Minecraft.getInstance().keyboardHandler.getClipboard();
                }

                final String clipboardString = glfwGetClipboardString(ImGuiWindowImpl.this.data.window);
                return clipboardString != null ? clipboardString : "";
            }
        };
    }

    protected ImStrConsumer setClipboardTextFn() {
        return new ImStrConsumer() {
            @Override
            public void accept(final String text) {
                if (ImGuiWindowImpl.this.bridge.getWindow() == ImGuiWindowImpl.this.data.window) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(text);
                } else {
                    glfwSetClipboardString(ImGuiWindowImpl.this.data.window, text);
                }
            }
        };
    }

    protected int glfwKeyToImGuiKey(final int glfwKey) {
        return switch (glfwKey) {
            case GLFW_KEY_TAB -> ImGuiKey.Tab;
            case GLFW_KEY_LEFT -> ImGuiKey.LeftArrow;
            case GLFW_KEY_RIGHT -> ImGuiKey.RightArrow;
            case GLFW_KEY_UP -> ImGuiKey.UpArrow;
            case GLFW_KEY_DOWN -> ImGuiKey.DownArrow;
            case GLFW_KEY_PAGE_UP -> ImGuiKey.PageUp;
            case GLFW_KEY_PAGE_DOWN -> ImGuiKey.PageDown;
            case GLFW_KEY_HOME -> ImGuiKey.Home;
            case GLFW_KEY_END -> ImGuiKey.End;
            case GLFW_KEY_INSERT -> ImGuiKey.Insert;
            case GLFW_KEY_DELETE -> ImGuiKey.Delete;
            case GLFW_KEY_BACKSPACE -> ImGuiKey.Backspace;
            case GLFW_KEY_SPACE -> ImGuiKey.Space;
            case GLFW_KEY_ENTER -> ImGuiKey.Enter;
            case GLFW_KEY_ESCAPE -> ImGuiKey.Escape;
            case GLFW_KEY_APOSTROPHE -> ImGuiKey.Apostrophe;
            case GLFW_KEY_COMMA -> ImGuiKey.Comma;
            case GLFW_KEY_MINUS -> ImGuiKey.Minus;
            case GLFW_KEY_PERIOD -> ImGuiKey.Period;
            case GLFW_KEY_SLASH -> ImGuiKey.Slash;
            case GLFW_KEY_SEMICOLON -> ImGuiKey.Semicolon;
            case GLFW_KEY_EQUAL -> ImGuiKey.Equal;
            case GLFW_KEY_LEFT_BRACKET -> ImGuiKey.LeftBracket;
            case GLFW_KEY_BACKSLASH -> ImGuiKey.Backslash;
            case GLFW_KEY_WORLD_1, GLFW_KEY_WORLD_2 -> ImGuiKey.Oem102;
            case GLFW_KEY_RIGHT_BRACKET -> ImGuiKey.RightBracket;
            case GLFW_KEY_GRAVE_ACCENT -> ImGuiKey.GraveAccent;
            case GLFW_KEY_CAPS_LOCK -> ImGuiKey.CapsLock;
            case GLFW_KEY_SCROLL_LOCK -> ImGuiKey.ScrollLock;
            case GLFW_KEY_NUM_LOCK -> ImGuiKey.NumLock;
            case GLFW_KEY_PRINT_SCREEN -> ImGuiKey.PrintScreen;
            case GLFW_KEY_PAUSE -> ImGuiKey.Pause;
            case GLFW_KEY_KP_0 -> ImGuiKey.Keypad0;
            case GLFW_KEY_KP_1 -> ImGuiKey.Keypad1;
            case GLFW_KEY_KP_2 -> ImGuiKey.Keypad2;
            case GLFW_KEY_KP_3 -> ImGuiKey.Keypad3;
            case GLFW_KEY_KP_4 -> ImGuiKey.Keypad4;
            case GLFW_KEY_KP_5 -> ImGuiKey.Keypad5;
            case GLFW_KEY_KP_6 -> ImGuiKey.Keypad6;
            case GLFW_KEY_KP_7 -> ImGuiKey.Keypad7;
            case GLFW_KEY_KP_8 -> ImGuiKey.Keypad8;
            case GLFW_KEY_KP_9 -> ImGuiKey.Keypad9;
            case GLFW_KEY_KP_DECIMAL -> ImGuiKey.KeypadDecimal;
            case GLFW_KEY_KP_DIVIDE -> ImGuiKey.KeypadDivide;
            case GLFW_KEY_KP_MULTIPLY -> ImGuiKey.KeypadMultiply;
            case GLFW_KEY_KP_SUBTRACT -> ImGuiKey.KeypadSubtract;
            case GLFW_KEY_KP_ADD -> ImGuiKey.KeypadAdd;
            case GLFW_KEY_KP_ENTER -> ImGuiKey.KeypadEnter;
            case GLFW_KEY_KP_EQUAL -> ImGuiKey.KeypadEqual;
            case GLFW_KEY_LEFT_SHIFT -> ImGuiKey.LeftShift;
            case GLFW_KEY_LEFT_CONTROL -> ImGuiKey.LeftCtrl;
            case GLFW_KEY_LEFT_ALT -> ImGuiKey.LeftAlt;
            case GLFW_KEY_LEFT_SUPER -> ImGuiKey.LeftSuper;
            case GLFW_KEY_RIGHT_SHIFT -> ImGuiKey.RightShift;
            case GLFW_KEY_RIGHT_CONTROL -> ImGuiKey.RightCtrl;
            case GLFW_KEY_RIGHT_ALT -> ImGuiKey.RightAlt;
            case GLFW_KEY_RIGHT_SUPER -> ImGuiKey.RightSuper;
            case GLFW_KEY_MENU -> ImGuiKey.Menu;
            case GLFW_KEY_0 -> ImGuiKey._0;
            case GLFW_KEY_1 -> ImGuiKey._1;
            case GLFW_KEY_2 -> ImGuiKey._2;
            case GLFW_KEY_3 -> ImGuiKey._3;
            case GLFW_KEY_4 -> ImGuiKey._4;
            case GLFW_KEY_5 -> ImGuiKey._5;
            case GLFW_KEY_6 -> ImGuiKey._6;
            case GLFW_KEY_7 -> ImGuiKey._7;
            case GLFW_KEY_8 -> ImGuiKey._8;
            case GLFW_KEY_9 -> ImGuiKey._9;
            case GLFW_KEY_A -> ImGuiKey.A;
            case GLFW_KEY_B -> ImGuiKey.B;
            case GLFW_KEY_C -> ImGuiKey.C;
            case GLFW_KEY_D -> ImGuiKey.D;
            case GLFW_KEY_E -> ImGuiKey.E;
            case GLFW_KEY_F -> ImGuiKey.F;
            case GLFW_KEY_G -> ImGuiKey.G;
            case GLFW_KEY_H -> ImGuiKey.H;
            case GLFW_KEY_I -> ImGuiKey.I;
            case GLFW_KEY_J -> ImGuiKey.J;
            case GLFW_KEY_K -> ImGuiKey.K;
            case GLFW_KEY_L -> ImGuiKey.L;
            case GLFW_KEY_M -> ImGuiKey.M;
            case GLFW_KEY_N -> ImGuiKey.N;
            case GLFW_KEY_O -> ImGuiKey.O;
            case GLFW_KEY_P -> ImGuiKey.P;
            case GLFW_KEY_Q -> ImGuiKey.Q;
            case GLFW_KEY_R -> ImGuiKey.R;
            case GLFW_KEY_S -> ImGuiKey.S;
            case GLFW_KEY_T -> ImGuiKey.T;
            case GLFW_KEY_U -> ImGuiKey.U;
            case GLFW_KEY_V -> ImGuiKey.V;
            case GLFW_KEY_W -> ImGuiKey.W;
            case GLFW_KEY_X -> ImGuiKey.X;
            case GLFW_KEY_Y -> ImGuiKey.Y;
            case GLFW_KEY_Z -> ImGuiKey.Z;
            case GLFW_KEY_F1 -> ImGuiKey.F1;
            case GLFW_KEY_F2 -> ImGuiKey.F2;
            case GLFW_KEY_F3 -> ImGuiKey.F3;
            case GLFW_KEY_F4 -> ImGuiKey.F4;
            case GLFW_KEY_F5 -> ImGuiKey.F5;
            case GLFW_KEY_F6 -> ImGuiKey.F6;
            case GLFW_KEY_F7 -> ImGuiKey.F7;
            case GLFW_KEY_F8 -> ImGuiKey.F8;
            case GLFW_KEY_F9 -> ImGuiKey.F9;
            case GLFW_KEY_F10 -> ImGuiKey.F10;
            case GLFW_KEY_F11 -> ImGuiKey.F11;
            case GLFW_KEY_F12 -> ImGuiKey.F12;
            case GLFW_KEY_F13 -> ImGuiKey.F13;
            case GLFW_KEY_F14 -> ImGuiKey.F14;
            case GLFW_KEY_F15 -> ImGuiKey.F15;
            case GLFW_KEY_F16 -> ImGuiKey.F16;
            case GLFW_KEY_F17 -> ImGuiKey.F17;
            case GLFW_KEY_F18 -> ImGuiKey.F18;
            case GLFW_KEY_F19 -> ImGuiKey.F19;
            case GLFW_KEY_F20 -> ImGuiKey.F20;
            case GLFW_KEY_F21 -> ImGuiKey.F21;
            case GLFW_KEY_F22 -> ImGuiKey.F22;
            case GLFW_KEY_F23 -> ImGuiKey.F23;
            case GLFW_KEY_F24 -> ImGuiKey.F24;
            default -> ImGuiKey.None;
        };
    }

    // X11 does not include current pressed/released modifier key in 'mods' flags submitted by GLFW
    // See https://github.com/ocornut/imgui/issues/6034 and https://github.com/glfw/glfw/issues/1630
    protected void updateKeyModifiers(final long window) {
        final ImGuiIO io = ImGui.getIO();
        io.addKeyEvent(ImGuiKey.ImGuiMod_Ctrl, (glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) || (glfwGetKey(window, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS));
        io.addKeyEvent(ImGuiKey.ImGuiMod_Shift, (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) || (glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS));
        io.addKeyEvent(ImGuiKey.ImGuiMod_Alt, (glfwGetKey(window, GLFW_KEY_LEFT_ALT) == GLFW_PRESS) || (glfwGetKey(window, GLFW_KEY_RIGHT_ALT) == GLFW_PRESS));
        io.addKeyEvent(ImGuiKey.ImGuiMod_Super, (glfwGetKey(window, GLFW_KEY_LEFT_SUPER) == GLFW_PRESS) || (glfwGetKey(window, GLFW_KEY_RIGHT_SUPER) == GLFW_PRESS));
    }

    protected boolean shouldChainCallback(final long window) {
        return this.data.callbacksChainForAllWindows || window == this.data.window;
    }

    public void mouseButtonCallback(final long window, final int button, final int action, final int mods) {
        if (this.data.prevUserCallbackMousebutton != null && this.shouldChainCallback(window)) {
            this.data.prevUserCallbackMousebutton.invoke(window, button, action, mods);
        }

        try {
            this.bridge.start();

            // Workaround for Linux: ignore mouse up events which are following an focus loss following a viewport creation
            if (this.data.mouseIgnoreButtonUp && action == GLFW_RELEASE) {
                return;
            }

            this.updateKeyModifiers(window);

            final ImGuiIO io = ImGui.getIO();
            if (button >= 0 && button < ImGuiMouseButton.COUNT) {
                io.addMouseButtonEvent(button, action == GLFW_PRESS);
            }
        } finally {
            this.bridge.stop();
        }
    }

    public void scrollCallback(final long window, final double xOffset, final double yOffset) {
        if (this.data.prevUserCallbackScroll != null && this.shouldChainCallback(window)) {
            this.data.prevUserCallbackScroll.invoke(window, xOffset, yOffset);
        }

        try {
            this.bridge.start();
            final ImGuiIO io = ImGui.getIO();
            io.addMouseWheelEvent((float) xOffset, (float) yOffset);
        } finally {
            this.bridge.stop();
        }
    }

    protected int translateUntranslatedKey(final int key, final int scancode) {
        if (!glfwHasGetKeyName) {
            return key;
        }

        // GLFW 3.1+ attempts to "untranslate" keys, which goes the opposite of what every other framework does, making using lettered shortcuts difficult.
        // (It had reasons to do so: namely GLFW is/was more likely to be used for WASD-type game controls rather than lettered shortcuts, but IHMO the 3.1 change could have been done differently)
        // See https://github.com/glfw/glfw/issues/1502 for details.
        // Adding a workaround to undo this (so our keys are translated->untranslated->translated, likely a lossy process).
        // This won't cover edge cases but this is at least going to cover common cases.
        if (key >= GLFW_KEY_KP_0 && key <= GLFW_KEY_KP_EQUAL) {
            return key;
        }

        int resultKey = key;
        final GLFWErrorCallback prevErrorCallback = glfwSetErrorCallback(null);
        final String keyName = glfwGetKeyName(key, scancode);
        glfwSetErrorCallback(prevErrorCallback);
        this.eatErrors();
        // C++ checks key_name[0] != 0 && key_name[1] == 0 (NUL-terminated single char). In Java strings have no
        // trailing NUL, so the equivalent is "exactly one character".
        if (keyName != null && keyName.length() == 1) {
            if (keyName.charAt(0) >= '0' && keyName.charAt(0) <= '9') {
                resultKey = GLFW_KEY_0 + (keyName.charAt(0) - '0');
            } else if (keyName.charAt(0) >= 'A' && keyName.charAt(0) <= 'Z') {
                resultKey = GLFW_KEY_A + (keyName.charAt(0) - 'A');
            } else if (keyName.charAt(0) >= 'a' && keyName.charAt(0) <= 'z') {
                resultKey = GLFW_KEY_A + (keyName.charAt(0) - 'a');
            } else {
                final int index = this.props.charNames.indexOf(keyName.charAt(0));
                if (index != -1) {
                    resultKey = this.props.charKeys[index];
                }
            }
        }

        return resultKey;
    }

    protected void eatErrors() {
        if (glfwHasGetError) { // Eat errors (see #5908)
            try (final MemoryStack stack = MemoryStack.stackPush()) {
                glfwGetError(stack.mallocPointer(1));
            }
        }
    }

    public void keyCallback(final long window, final int keycode, final int scancode, final int action, final int mods) {
        if (this.data.prevUserCallbackKey != null && this.shouldChainCallback(window)) {
            this.data.prevUserCallbackKey.invoke(window, keycode, scancode, action, mods);
        }

        if (action != GLFW_PRESS && action != GLFW_RELEASE) {
            return;
        }

        try {
            this.bridge.start();
            this.updateKeyModifiers(window);

            if (keycode >= 0 && keycode < this.data.keyOwnerWindows.length) {
                this.data.keyOwnerWindows[keycode] = (action == GLFW_PRESS) ? window : -1;
            }

            final int key = this.translateUntranslatedKey(keycode, scancode);

            final ImGuiIO io = ImGui.getIO();
            final int imguiKey = this.glfwKeyToImGuiKey(key);
            io.addKeyEvent(imguiKey, (action == GLFW_PRESS));
            io.setKeyEventNativeData(imguiKey, key, scancode); // To support legacy indexing (<1.87 user code)
        } finally {
            this.bridge.stop();
        }
    }

    public void windowFocusCallback(final long window, final boolean focused) {
        if (this.data.prevUserCallbackWindowFocus != null && this.shouldChainCallback(window)) {
            this.data.prevUserCallbackWindowFocus.invoke(window, focused);
        }

        try {
            this.bridge.start();
            // Workaround for Linux: when losing focus with mouseIgnoreButtonUpWaitForFocusLoss set, we will temporarily ignore subsequent Mouse Up events
            this.data.mouseIgnoreButtonUp = this.data.mouseIgnoreButtonUpWaitForFocusLoss && !focused;
            this.data.mouseIgnoreButtonUpWaitForFocusLoss = false;

            ImGui.getIO().addFocusEvent(focused);
        } finally {
            this.bridge.stop();
        }
    }

    public void cursorPosCallback(final long window, final double x, final double y) {
        if (this.data.prevUserCallbackCursorPos != null && this.shouldChainCallback(window)) {
            this.data.prevUserCallbackCursorPos.invoke(window, x, y);
        }

        try {
            this.bridge.start();
            float posX = (float) x;
            float posY = (float) y;

            final ImGuiIO io = ImGui.getIO();

            if (io.hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
                glfwGetWindowPos(window, this.props.windowX, this.props.windowY);
                posX += this.props.windowX[0];
                posY += this.props.windowY[0];
            }

            io.addMousePosEvent(posX, posY);
            this.data.lastValidMousePos.set(posX, posY);
        } finally {
            this.bridge.stop();
        }
    }

    // Workaround: X11 seems to send spurious Leave/Enter events which would make us lose our position,
    // so we back it up and restore on Leave/Enter (see https://github.com/ocornut/imgui/issues/4984)
    public void cursorEnterCallback(final long window, final boolean entered) {
        if (this.data.prevUserCallbackCursorEnter != null && this.shouldChainCallback(window)) {
            this.data.prevUserCallbackCursorEnter.invoke(window, entered);
        }

        try {
            this.bridge.start();
            final ImGuiIO io = ImGui.getIO();

            if (entered) {
                this.data.mouseWindow = window;
                io.addMousePosEvent(this.data.lastValidMousePos.x, this.data.lastValidMousePos.y);
            } else if (this.data.mouseWindow == window) {
                io.getMousePos(this.data.lastValidMousePos);
                this.data.mouseWindow = -1;
                io.addMousePosEvent(-Float.MAX_VALUE, -Float.MAX_VALUE);
            }
        } finally {
            this.bridge.stop();
        }
    }

    public void charCallback(final long window, final int c) {
        if (this.data.prevUserCallbackChar != null && this.shouldChainCallback(window)) {
            this.data.prevUserCallbackChar.invoke(window, c);
        }

        try {
            this.bridge.start();
            ImGui.getIO().addInputCharacter(c);
        } finally {
            this.bridge.stop();
        }
    }

    public void installCallbacks(final long window) {
        this.data.prevUserCallbackWindowFocus = glfwSetWindowFocusCallback(window, this::windowFocusCallback);
        this.data.prevUserCallbackCursorEnter = glfwSetCursorEnterCallback(window, this::cursorEnterCallback);
        this.data.prevUserCallbackCursorPos = glfwSetCursorPosCallback(window, this::cursorPosCallback);
        this.data.prevUserCallbackMousebutton = glfwSetMouseButtonCallback(window, this::mouseButtonCallback);
        this.data.prevUserCallbackScroll = glfwSetScrollCallback(window, this::scrollCallback);
        this.data.prevUserCallbackKey = glfwSetKeyCallback(window, this::keyCallback);
        this.data.prevUserCallbackChar = glfwSetCharCallback(window, this::charCallback);
        this.data.installedCallbacks = true;
    }

    protected void freeCallback(final Callback cb) {
        if (cb != null) {
            cb.free();
        }
    }

    public void restoreCallbacks(final long window) {
        this.freeCallback(glfwSetWindowFocusCallback(window, this.data.prevUserCallbackWindowFocus));
        this.freeCallback(glfwSetCursorEnterCallback(window, this.data.prevUserCallbackCursorEnter));
        this.freeCallback(glfwSetCursorPosCallback(window, this.data.prevUserCallbackCursorPos));
        this.freeCallback(glfwSetMouseButtonCallback(window, this.data.prevUserCallbackMousebutton));
        this.freeCallback(glfwSetScrollCallback(window, this.data.prevUserCallbackScroll));
        this.freeCallback(glfwSetKeyCallback(window, this.data.prevUserCallbackKey));
        this.freeCallback(glfwSetCharCallback(window, this.data.prevUserCallbackChar));
        this.data.installedCallbacks = false;
        this.data.prevUserCallbackWindowFocus = null;
        this.data.prevUserCallbackCursorEnter = null;
        this.data.prevUserCallbackCursorPos = null;
        this.data.prevUserCallbackMousebutton = null;
        this.data.prevUserCallbackScroll = null;
        this.data.prevUserCallbackKey = null;
        this.data.prevUserCallbackChar = null;
    }

    /**
     * Set to 'true' to enable chaining installed callbacks for all windows (including secondary viewports created by backends or by user.
     * This is 'false' by default meaning we only chain callbacks for the main viewport.
     * We cannot set this to 'true' by default because user callbacks code may be not testing the 'window' parameter of their callback.
     * If you set this to 'true' your user callback code will need to make sure you are testing the 'window' parameter.
     */
    public void setCallbacksChainForAllWindows(final boolean chainForAllWindows) {
        this.data.callbacksChainForAllWindows = chainForAllWindows;
    }

    protected Data newData() {
        return new Data();
    }

    public GlfwClientApi getClientApi() {
        return this.data.clientApi;
    }

    public boolean initForOpenGL(final Window window, final boolean installCallbacks) {
        return this.initImpl(window, installCallbacks, GlfwClientApi.OPENGL);
    }

    public boolean initForVulkan(final Window window, final boolean installCallbacks) {
        return this.initImpl(window, installCallbacks, GlfwClientApi.VULKAN);
    }

    public boolean initForOther(final Window window, final boolean installCallbacks) {
        return this.initImpl(window, installCallbacks, GlfwClientApi.OTHER);
    }

    private boolean initImpl(final Window window, final boolean installCallbacks, final GlfwClientApi clientApi) {
        final ImGuiIO io = ImGui.getIO();

        this.data = this.newData();
        this.data.window = ImGuiMCImpl.getWindowHandle(window);
        //? if >= 26.1 {
        /*this.data.backend = window.backend();
         *///? }
        this.data.time = 0.0;
        this.data.isWayland = isWayland();

        // Compute runtime GLFW version so the backend name matches the actually-loaded library.
        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer verMajor = stack.mallocInt(1);
            final IntBuffer verMinor = stack.mallocInt(1);
            final IntBuffer verRev = stack.mallocInt(1);
            glfwGetVersion(verMajor, verMinor, verRev);
            final int versionCombined = verMajor.get(0) * 1000 + verMinor.get(0) * 100 + verRev.get(0);
            io.setBackendPlatformName("imgui_impl_glfw (" + versionCombined + ")");
        }

        io.addBackendFlags(ImGuiBackendFlags.HasMouseCursors | ImGuiBackendFlags.HasSetMousePos);
        // In C++ (1.92+): viewports are gated purely on platform support (disabled on Wayland).
        // In Java: ImGuiConfigFlags.ViewportsEnable is also required from the user — this is set by the
        //          application and is intentionally preserved as a divergence from upstream.
        if (!this.data.isWayland) {
            io.addBackendFlags(ImGuiBackendFlags.PlatformHasViewports);
        }
        if (glfwHasMousePassthrough || (glfwHasWindowHovered && IS_WINDOWS)) {
            io.addBackendFlags(ImGuiBackendFlags.HasMouseHoveredViewport);
        }

        io.setGetClipboardTextFn(this.getClipboardTextFn());
        io.setSetClipboardTextFn(this.setClipboardTextFn());
        // TODO Add OpenInShellFn
        // TODO add SetImeDataFn

        // Create mouse cursors
        // (By design, on X11 cursors are user configurable and some cursors may be missing. When a cursor doesn't exist,
        // GLFW will emit an error which will often be printed by the app, so we temporarily disable error reporting.
        // Missing cursors will return NULL and our _UpdateMouseCursor() function will use the Arrow cursor instead.)
        final GLFWErrorCallback prevErrorCallback = glfwSetErrorCallback(null);
        this.data.mouseCursors[ImGuiMouseCursor.Arrow] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        this.data.mouseCursors[ImGuiMouseCursor.TextInput] = glfwCreateStandardCursor(GLFW_IBEAM_CURSOR);
        this.data.mouseCursors[ImGuiMouseCursor.ResizeNS] = glfwCreateStandardCursor(GLFW_VRESIZE_CURSOR);
        this.data.mouseCursors[ImGuiMouseCursor.ResizeEW] = glfwCreateStandardCursor(GLFW_HRESIZE_CURSOR);
        this.data.mouseCursors[ImGuiMouseCursor.Hand] = glfwCreateStandardCursor(GLFW_HAND_CURSOR);
        if (glfwHasNewCursors) {
            this.data.mouseCursors[ImGuiMouseCursor.ResizeAll] = glfwCreateStandardCursor(GLFW_RESIZE_ALL_CURSOR);
            this.data.mouseCursors[ImGuiMouseCursor.ResizeNESW] = glfwCreateStandardCursor(GLFW_RESIZE_NESW_CURSOR);
            this.data.mouseCursors[ImGuiMouseCursor.ResizeNWSE] = glfwCreateStandardCursor(GLFW_RESIZE_NWSE_CURSOR);
            this.data.mouseCursors[ImGuiMouseCursor.NotAllowed] = glfwCreateStandardCursor(GLFW_NOT_ALLOWED_CURSOR);
        } else {
            this.data.mouseCursors[ImGuiMouseCursor.ResizeAll] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
            this.data.mouseCursors[ImGuiMouseCursor.ResizeNESW] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
            this.data.mouseCursors[ImGuiMouseCursor.ResizeNWSE] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
            this.data.mouseCursors[ImGuiMouseCursor.NotAllowed] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        }
        glfwSetErrorCallback(prevErrorCallback);
        this.eatErrors();

        // Chain GLFW callbacks: our callbacks will call the user's previously installed callbacks, if any.
        if (installCallbacks) {
            this.installCallbacks(this.data.window);
        }

        // Update monitors the first time (note: monitor callback are broken in GLFW 3.2 and earlier, see github.com/glfw/glfw/issues/784)
        this.updateMonitors();

        // Our mouse update function expect PlatformHandle to be filled for the main viewport
        final ImGuiViewport mainViewport = ImGui.getMainViewport();
        mainViewport.setPlatformHandle(this.data.window);
        setRawPlatformHandle(mainViewport, this.data.window);

        // In C++: on Windows a WndProc hook is registered (SetPropA "IMGUI_BACKEND_DATA" + SetWindowLongPtrW)
        //         to surface MouseSourceEvent (touch/pen) and to handle WM_NCHITTEST for NoInputs.
        // In Java: this needs a JNI/JNA layer (see follow-up in review.md). Without it, only the passthrough
        //          branches of multi-viewport handling are correct.
        this.data.clientApi = clientApi;
        return true;
    }

    private static void setRawPlatformHandle(final ImGuiViewport viewport, final long window) {
        if (IS_WINDOWS) {
            viewport.setPlatformHandleRaw(GLFWNativeWin32.glfwGetWin32Window(window));
        }
        if (IS_APPLE) {
            viewport.setPlatformHandleRaw(GLFWNativeCocoa.glfwGetCocoaWindow(window));
        }
        if (IS_LINUX) {
            if (isWayland()) {
                viewport.setPlatformHandleRaw(GLFWNativeWayland.glfwGetWaylandWindow(window));
            } else {
                viewport.setPlatformHandleRaw(GLFWNativeX11.glfwGetX11Window(window));
            }
        }
    }

    private static boolean isWayland() {
        // In C++ a third fallback parses glfwGetVersionString() and checks glfwGetX11Display() when GLFW < 3.4 is
        // compiled without glfwGetPlatform(). LWJGL 3.4.x always exposes glfwGetPlatform, so we omit that path; if
        // we ever downgrade to a 3.3.x LWJGL, Wayland would silently report as non-Wayland here.
        if (glfwHasGetPlatform) {
            return glfwGetPlatform() == GLFW_PLATFORM_WAYLAND;
        }
        return false;
    }

    public void shutdown() {
        final ImGuiIO io = ImGui.getIO();

        this.shutdownPlatformInterface();

        if (this.data.installedCallbacks) {
            this.restoreCallbacks(this.data.window);
        }

        for (int cursorN = 0; cursorN < ImGuiMouseCursor.COUNT; cursorN++) {
            // Skip unpopulated slots (e.g. Wait/Progress, added in imgui 1.91; GLFW has no
            // corresponding standard cursor). Upstream C code is tolerant of NULL here but
            // LWJGL's glfwDestroyCursor asserts non-null.
            if (this.data.mouseCursors[cursorN] != 0) {
                glfwDestroyCursor(this.data.mouseCursors[cursorN]);
            }
        }

        // In C++: the Windows WndProc hook installed during init is unhooked here (SetPropA + SetWindowLongPtrW).
        // In Java: the hook is never installed (see follow-up), so there is nothing to unhook.

        io.setBackendPlatformName(null);
        io.removeBackendFlags(ImGuiBackendFlags.HasMouseCursors | ImGuiBackendFlags.HasSetMousePos | ImGuiBackendFlags.HasGamepad
                | ImGuiBackendFlags.PlatformHasViewports | ImGuiBackendFlags.HasMouseHoveredViewport);
        // In C++: ImGui::GetPlatformIO().ClearPlatformHandlers() resets the Platform_* handlers.
        // In Java: the method is not exposed on ImGuiPlatformIO — see follow-up in review.md.
        this.data = null;
    }

    protected void updateMouseData() {
        final ImGuiIO io = ImGui.getIO();
        final ImGuiPlatformIO platformIO = ImGui.getPlatformIO();

        int mouseViewportId = 0;
        io.getMousePos(this.props.mousePosPrev);

        for (int n = 0; n < platformIO.getViewportsSize(); n++) {
            final ImGuiViewport viewport = platformIO.getViewports(n);
            final long window = viewport.getPlatformHandle();
            final boolean isWindowFocused = glfwGetWindowAttrib(window, GLFW_FOCUSED) != 0;

            if (isWindowFocused) {
                // (Optional) Set OS mouse position from Dear ImGui if requested (rarely used, only when ImGuiConfigFlags_NavEnableSetMousePos is enabled by user)
                // When multi-viewports are enabled, all Dear ImGui positions are same as OS positions.
                if (io.getWantSetMousePos()) {
                    glfwSetCursorPos(window, this.props.mousePosPrev.x - viewport.getPosX(), this.props.mousePosPrev.y - viewport.getPosY());
                }

                // (Optional) Fallback to provide mouse position when focused (ImGui_ImplGlfw_CursorPosCallback already provides this when hovered or captured)
                if (this.data.mouseWindow == -1) {
                    glfwGetCursorPos(window, this.props.mouseX, this.props.mouseY);
                    double mouseX = this.props.mouseX[0];
                    double mouseY = this.props.mouseY[0];
                    if (io.hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
                        // Single viewport mode: mouse position in client window coordinates (io.MousePos is (0,0) when the mouse is on the upper-left corner of the app window)
                        // Multi-viewport mode: mouse position in OS absolute coordinates (io.MousePos is (0,0) when the mouse is on the upper-left of the primary monitor)
                        glfwGetWindowPos(window, this.props.windowX, this.props.windowY);
                        mouseX += this.props.windowX[0];
                        mouseY += this.props.windowY[0];
                    }
                    this.data.lastValidMousePos.set((float) mouseX, (float) mouseY);
                    io.addMousePosEvent((float) mouseX, (float) mouseY);
                }
            }

            // (Optional) When using multiple viewports: call io.AddMouseViewportEvent() with the viewport the OS mouse cursor is hovering.
            // If ImGuiBackendFlags_HasMouseHoveredViewport is not set by the backend, Dear imGui will ignore this field and infer the information using its flawed heuristic.
            // - [X] GLFW >= 3.3 backend ON WINDOWS ONLY does correctly ignore viewports with the _NoInputs flag.
            // - [!] GLFW <= 3.2 backend CANNOT correctly ignore viewports with the _NoInputs flag, and CANNOT reported Hovered Viewport because of mouse capture.
            //       Some backend are not able to handle that correctly. If a backend report an hovered viewport that has the _NoInputs flag (e.g. when dragging a window
            //       for docking, the viewport has the _NoInputs flag in order to allow us to find the viewport under), then Dear ImGui is forced to ignore the value reported
            //       by the backend, and use its flawed heuristic to guess the viewport behind.
            // - [X] GLFW backend correctly reports this regardless of another viewport behind focused and dragged from (we need this to find a useful drag and drop target).
            // FIXME: This is currently only correct on Win32. See what we do below with the WM_NCHITTEST, missing an equivalent for other systems.
            // See https://github.com/glfw/glfw/issues/1236 if you want to help in making this a GLFW feature.

            if (glfwHasMousePassthrough) {
                final boolean windowNoInput = viewport.hasFlags(ImGuiViewportFlags.NoInputs);
                glfwSetWindowAttrib(window, GLFW_MOUSE_PASSTHROUGH, windowNoInput ? GLFW_TRUE : GLFW_FALSE);
            }
            if (glfwHasMousePassthrough || glfwHasWindowHovered) {
                if (glfwGetWindowAttrib(window, GLFW_HOVERED) != 0) {
                    mouseViewportId = viewport.getID();
                }
            }
            // else
            // We cannot use bd->MouseWindow maintained from CursorEnter/Leave callbacks, because it is locked to the window capturing mouse.
        }

        if (io.hasBackendFlags(ImGuiBackendFlags.HasMouseHoveredViewport)) {
            io.addMouseViewportEvent(mouseViewportId);
        }
    }

    protected void updateMouseCursor() {
        final ImGuiIO io = ImGui.getIO();

        if (io.hasConfigFlags(ImGuiConfigFlags.NoMouseCursorChange) || glfwGetInputMode(this.data.window, GLFW_CURSOR) == GLFW_CURSOR_DISABLED) {
            this.data.lastMouseCursor = -1; // Invalidate so that if user changes underlying cursor we will update it next time we can.
            return;
        }

        final int imguiCursor = ImGui.getMouseCursor();
        final ImGuiPlatformIO platformIO = ImGui.getPlatformIO();

        for (int n = 0; n < platformIO.getViewportsSize(); n++) {
            final long windowPtr = platformIO.getViewports(n).getPlatformHandle();

            if (imguiCursor == ImGuiMouseCursor.None || io.getMouseDrawCursor()) {
                if (this.data.lastMouseCursor != -1) {
                    // Hide OS mouse cursor if imgui is drawing it or if it wants no cursor
                    glfwSetInputMode(windowPtr, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
                    this.data.lastMouseCursor = -1;
                }
            } else {
                // Show OS mouse cursor
                // FIXME-PLATFORM: Unfocused windows seems to fail changing the mouse cursor with GLFW 3.2, but 3.3 works here.
                final long cursor = this.data.mouseCursors[imguiCursor] != 0 ? this.data.mouseCursors[imguiCursor] : this.data.mouseCursors[ImGuiMouseCursor.Arrow];
                if (this.data.lastMouseCursor != cursor) {
                    glfwSetCursor(windowPtr, cursor);
                    this.data.lastMouseCursor = cursor;
                }
                glfwSetInputMode(windowPtr, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            }
        }
    }

    @FunctionalInterface
    private interface MapButton {
        void run(int keyNo, int buttonNo, int _unused);
    }

    @FunctionalInterface
    private interface MapAnalog {
        void run(int keyNo, int axisNo, int _unused, float v0, float v1);
    }

    @SuppressWarnings("ManualMinMaxCalculation")
    private float saturate(final float v) {
        return v < 0.0f ? 0.0f : v > 1.0f ? 1.0f : v;
    }

    protected void updateGamepads() {
        final ImGuiIO io = ImGui.getIO();

        if (!io.hasConfigFlags(ImGuiConfigFlags.NavEnableGamepad)) {
            return;
        }

        io.removeBackendFlags(ImGuiBackendFlags.HasGamepad);

        final MapButton mapButton;
        final MapAnalog mapAnalog;
        final GLFWGamepadState gamepad = glfwHasGamepadApi ? GLFWGamepadState.malloc() : null;

        try (gamepad) {
            if (glfwHasGamepadApi) {
                if (!glfwGetGamepadState(GLFW_JOYSTICK_1, gamepad)) {
                    return;
                }
                mapButton = (keyNo, buttonNo, _unused) -> io.addKeyEvent(keyNo, gamepad.buttons(buttonNo) != 0);
                mapAnalog = (keyNo, axisNo, _unused, v0, v1) -> {
                    float v = gamepad.axes(axisNo);
                    v = (v - v0) / (v1 - v0);
                    io.addKeyAnalogEvent(keyNo, v > 0.10f, this.saturate(v));
                };
            } else {
                final FloatBuffer axes = glfwGetJoystickAxes(GLFW_JOYSTICK_1);
                final ByteBuffer buttons = glfwGetJoystickButtons(GLFW_JOYSTICK_1);
                if (axes == null || axes.limit() == 0 || buttons == null || buttons.limit() == 0) {
                    return;
                }
                mapButton = (keyNo, buttonNo, _unused) -> io.addKeyEvent(keyNo, (buttons.limit() > buttonNo && buttons.get(buttonNo) == GLFW_PRESS));
                mapAnalog = (keyNo, axisNo, _unused, v0, v1) -> {
                    float v = (axes.limit() > axisNo) ? axes.get(axisNo) : v0;
                    v = (v - v0) / (v1 - v0);
                    io.addKeyAnalogEvent(keyNo, v > 0.10f, this.saturate(v));
                };
            }

            io.addBackendFlags(ImGuiBackendFlags.HasGamepad);
            mapButton.run(ImGuiKey.GamepadStart, GLFW_GAMEPAD_BUTTON_START, 7);
            mapButton.run(ImGuiKey.GamepadBack, GLFW_GAMEPAD_BUTTON_BACK, 6);
            mapButton.run(ImGuiKey.GamepadFaceLeft, GLFW_GAMEPAD_BUTTON_X, 2);     // Xbox X, PS Square
            mapButton.run(ImGuiKey.GamepadFaceRight, GLFW_GAMEPAD_BUTTON_B, 1);     // Xbox B, PS Circle
            mapButton.run(ImGuiKey.GamepadFaceUp, GLFW_GAMEPAD_BUTTON_Y, 3);     // Xbox Y, PS Triangle
            mapButton.run(ImGuiKey.GamepadFaceDown, GLFW_GAMEPAD_BUTTON_A, 0);     // Xbox A, PS Cross
            mapButton.run(ImGuiKey.GamepadDpadLeft, GLFW_GAMEPAD_BUTTON_DPAD_LEFT, 13);
            mapButton.run(ImGuiKey.GamepadDpadRight, GLFW_GAMEPAD_BUTTON_DPAD_RIGHT, 11);
            mapButton.run(ImGuiKey.GamepadDpadUp, GLFW_GAMEPAD_BUTTON_DPAD_UP, 10);
            mapButton.run(ImGuiKey.GamepadDpadDown, GLFW_GAMEPAD_BUTTON_DPAD_DOWN, 12);
            mapButton.run(ImGuiKey.GamepadL1, GLFW_GAMEPAD_BUTTON_LEFT_BUMPER, 4);
            mapButton.run(ImGuiKey.GamepadR1, GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER, 5);
            mapAnalog.run(ImGuiKey.GamepadL2, GLFW_GAMEPAD_AXIS_LEFT_TRIGGER, 4, -0.75f, +1.0f);
            mapAnalog.run(ImGuiKey.GamepadR2, GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER, 5, -0.75f, +1.0f);
            mapButton.run(ImGuiKey.GamepadL3, GLFW_GAMEPAD_BUTTON_LEFT_THUMB, 8);
            mapButton.run(ImGuiKey.GamepadR3, GLFW_GAMEPAD_BUTTON_RIGHT_THUMB, 9);
            mapAnalog.run(ImGuiKey.GamepadLStickLeft, GLFW_GAMEPAD_AXIS_LEFT_X, 0, -0.25f, -1.0f);
            mapAnalog.run(ImGuiKey.GamepadLStickRight, GLFW_GAMEPAD_AXIS_LEFT_X, 0, +0.25f, +1.0f);
            mapAnalog.run(ImGuiKey.GamepadLStickUp, GLFW_GAMEPAD_AXIS_LEFT_Y, 1, -0.25f, -1.0f);
            mapAnalog.run(ImGuiKey.GamepadLStickDown, GLFW_GAMEPAD_AXIS_LEFT_Y, 1, +0.25f, +1.0f);
            mapAnalog.run(ImGuiKey.GamepadRStickLeft, GLFW_GAMEPAD_AXIS_RIGHT_X, 2, -0.25f, -1.0f);
            mapAnalog.run(ImGuiKey.GamepadRStickRight, GLFW_GAMEPAD_AXIS_RIGHT_X, 2, +0.25f, +1.0f);
            mapAnalog.run(ImGuiKey.GamepadRStickUp, GLFW_GAMEPAD_AXIS_RIGHT_Y, 3, -0.25f, -1.0f);
            mapAnalog.run(ImGuiKey.GamepadRStickDown, GLFW_GAMEPAD_AXIS_RIGHT_Y, 3, +0.25f, +1.0f);
        }
    }

    protected void updateMonitors() {
        final ImGuiPlatformIO platformIO = ImGui.getPlatformIO();

        final PointerBuffer monitors = glfwGetMonitors();
        if (monitors == null) {
            return;
        }

        boolean updatedMonitors = false;
        for (int n = 0; n < monitors.limit(); n++) {
            final long monitor = monitors.get(n);

            final GLFWVidMode vidMode = glfwGetVideoMode(monitor);
            if (vidMode == null) {
                continue; // Failed to get Video mode (e.g. Emscripten does not support this function)
            }
            if (vidMode.width() <= 0 || vidMode.height() <= 0) {
                continue; // Failed to query suitable monitor info (#9195)
            }

            glfwGetMonitorPos(monitor, this.props.monitorX, this.props.monitorY);

            final float mainPosX = this.props.monitorX[0];
            final float mainPosY = this.props.monitorY[0];
            final float mainSizeX = vidMode.width();
            final float mainSizeY = vidMode.height();

            float workPosX = mainPosX;
            float workPosY = mainPosY;
            float workSizeX = mainSizeX;
            float workSizeY = mainSizeY;

            // Workaround a small GLFW issue reporting zero on monitor changes: https://github.com/glfw/glfw/pull/1761
            if (glfwHasMonitorWorkArea) {
                glfwGetMonitorWorkarea(monitor, this.props.monitorWorkAreaX, this.props.monitorWorkAreaY, this.props.monitorWorkAreaWidth, this.props.monitorWorkAreaHeight);
                if (this.props.monitorWorkAreaWidth[0] > 0 && this.props.monitorWorkAreaHeight[0] > 0) {
                    workPosX = this.props.monitorWorkAreaX[0];
                    workPosY = this.props.monitorWorkAreaY[0];
                    workSizeX = this.props.monitorWorkAreaWidth[0];
                    workSizeY = this.props.monitorWorkAreaHeight[0];
                }
            }

            // Warning: the validity of monitor DPI information on Windows depends on the application DPI awareness settings,
            // which generally needs to be set in the manifest or at runtime.
            final float dpiScale = getContentScaleForMonitor(monitor);
            if (dpiScale == 0.0f) {
                continue; // Some accessibility applications are declaring virtual monitors with a DPI of 0 (#7902)
            }

            // Preserve existing monitor list until a valid one is added.
            // Happens on macOS sleeping (#5683) and seemingly occasionally on Windows (#9195)
            if (!updatedMonitors) {
                platformIO.resizeMonitors(0);
                updatedMonitors = true;
            }

            platformIO.pushMonitors(monitor, mainPosX, mainPosY, mainSizeX, mainSizeY, workPosX, workPosY, workSizeX, workSizeY, dpiScale);
        }
    }

    public static float getContentScaleForMonitor(final long monitor) {
        if (IS_APPLE) {
            return 1.0f;
        }
        if (glfwHasGetPlatform && glfwGetPlatform() == GLFW_PLATFORM_WAYLAND) {
            return 1.0f;
        }
        if (glfwHasPerMonitorDpi) {
            try (final MemoryStack stack = MemoryStack.stackPush()) {
                final FloatBuffer xScale = stack.mallocFloat(1);
                final FloatBuffer yScale = stack.mallocFloat(1);
                glfwGetMonitorContentScale(monitor, xScale, yScale);
                return Math.max(xScale.get(0), yScale.get(0));
            }
        }
        return 1.0f;
    }

    private void getWindowSizeAndFramebufferScale(final RenderTarget renderTarget, final long window, final ImVec2 outSize, final ImVec2 outFbScale) {
        glfwGetWindowSize(window, this.props.windowW, this.props.windowH);

        //? if >=1.21.6 {
        /*final com.mojang.blaze3d.textures.GpuTextureView view = renderTarget.getColorTextureView();
        this.props.displayW = view.getWidth(0);
        this.props.displayH = view.getHeight(0);
        *///? } else {
        this.props.displayW = renderTarget.width;
        this.props.displayH = renderTarget.height;
        //? }

        float fbScaleX = (this.props.windowW[0] > 0) ? (float) this.props.displayW / (float) this.props.windowW[0] : 1.0f;
        float fbScaleY = (this.props.windowH[0] > 0) ? (float) this.props.displayH / (float) this.props.windowH[0] : 1.0f;
        // In C++ (1.92+): #if GLFW_HAS_WAYLAND && !bd->IsWayland forces fb_scale to (1, 1). GLFW_HAS_WAYLAND is a
        //                 compile-time guard that is only ever true on Linux GLFW builds, so in practice this overrides
        //                 the ratio on Linux non-Wayland sessions only. The companion change is in imgui_impl_opengl3:
        //                 the renderer no longer multiplies glViewport by DisplayFramebufferScale, it uses DisplaySize
        //                 directly, so reporting (1, 1) does not shrink the output.
        // In Java: ImGuiImplGl3.java has been resynced to mirror the new renderer, so we restore the override here.
        //          Translate the compile-time GLFW_HAS_WAYLAND guard to the runtime check IS_LINUX && !data.isWayland.
        if (IS_LINUX && !this.data.isWayland) {
            fbScaleX = 1.0f;
            fbScaleY = 1.0f;
        }
        if (outSize != null) {
            outSize.set(this.props.windowW[0], this.props.windowH[0]);
        }
        if (outFbScale != null) {
            outFbScale.set(fbScaleX, fbScaleY);
        }
    }

    private void updateViewports() {
        final ImGuiIO io = ImGui.getIO();

        final boolean viewportsRequested = io.hasBackendFlags(ImGuiBackendFlags.PlatformHasViewports) && io.hasConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        if (viewportsRequested != this.data.viewportEnable) {
            this.data.viewportEnable = viewportsRequested;
            if (viewportsRequested) {
                this.initPlatformInterface();
            } else {
                this.shutdownPlatformInterface();
            }
        }
    }

    public void newFrame(final RenderTarget mainRenderTarget) {
        final ImGuiIO io = ImGui.getIO();

        this.updateViewports();

        // Setup main viewport size (every frame to accommodate for window resizing)
        this.getWindowSizeAndFramebufferScale(mainRenderTarget, this.data.window, this.props.tmpDisplaySize, this.props.tmpFbScale);
        io.setDisplaySize(this.props.tmpDisplaySize.x, this.props.tmpDisplaySize.y);
        io.setDisplayFramebufferScale(this.props.tmpFbScale.x, this.props.tmpFbScale.y);
        this.updateMonitors();

        // Setup time step
        // (Accept glfwGetTime() not returning a monotonically increasing value. Seems to happens on disconnecting peripherals and probably on VMs and Emscripten, see #6491, #6189, #6114, #3644)
        double currentTime = glfwGetTime();
        if (currentTime <= this.data.time) {
            currentTime = this.data.time + 0.00001f;
        }
        io.setDeltaTime(this.data.time > 0.0 ? (float) (currentTime - this.data.time) : 1.0f / 60.0f);
        this.data.time = currentTime;

        this.data.mouseIgnoreButtonUp = false;
        this.updateMouseData();
        this.updateMouseCursor();

        // Update game controllers (if enabled and available)
        this.updateGamepads();
    }

    public boolean isCorrectSize(final int width, final int height) {
        return this.props.displayW == width && this.props.displayH == height;
    }

    @SuppressWarnings("unchecked")
    public static <T extends RenderViewportData> @Nullable T getRenderData(final ImGuiViewport vp, final Supplier<T> factory) {
        if (vp.getPlatformUserData() == null) {
            return null;
        }

        final ViewportData data = (ViewportData) vp.getPlatformUserData();
        if (data.viewportData == null) {
            data.viewportData = factory.get();
        }
        return (T) data.viewportData;
    }

    //? if >=26.2-pre-2 {
    /*public static @Nullable com.mojang.blaze3d.systems.GpuSurface getSurface(final ImGuiViewport vp) {
        if (vp.getPlatformUserData() == null) {
            return null;
        }

        return ((ViewportData) vp.getPlatformUserData()).windowSurface;
    }
    *///? }

    public interface RenderViewportData extends NativeResource {

        RenderTarget getRenderTarget();
    }

    //--------------------------------------------------------------------------------------------------------
    // MULTI-VIEWPORT / PLATFORM INTERFACE SUPPORT
    // This is an _advanced_ and _optional_ feature, allowing the backend to create and handle multiple viewports simultaneously.
    // If you are new to dear imgui or creating a new binding for dear imgui, it is recommended that you completely ignore this section first..
    //--------------------------------------------------------------------------------------------------------

    private static final class ViewportData {
        long window = -1;
        RenderViewportData viewportData;
        //? if >=26.2-pre-2 {
        /*com.mojang.blaze3d.systems.GpuSurface windowSurface;
        boolean windowSurfaceNeedsReconfiguring = true;
        boolean surfaceIsInvalid = true;
        *///? }
        boolean windowOwned = false;
        long ignoreWindowPosEventFrame = -1;
        long ignoreWindowSizeEventFrame = -1;
    }

    private void windowCloseCallback(final ImGuiViewport vp) {
        if (vp.isValidPtr()) {
            vp.setPlatformRequestClose(true);
        }
    }

    // GLFW may dispatch window pos/size events after calling glfwSetWindowPos()/glfwSetWindowSize().
    // However: depending on the platform the callback may be invoked at different time:
    // - on Windows it appears to be called within the glfwSetWindowPos()/glfwSetWindowSize() call
    // - on Linux it is queued and invoked during glfwPollEvents()
    // Because the event doesn't always fire on glfwSetWindowXXX() we use a frame counter tag to only
    // ignore recent glfwSetWindowXXX() calls.
    private void windowPosCallback(final ImGuiViewport vp, final int xPos, final int yPos) {
        if (vp.isNotValidPtr()) {
            return;
        }

        final ViewportData vd = (ViewportData) vp.getPlatformUserData();
        if (vd != null) {
            final boolean ignoreEvent = (this.bridge.getFrame() <= vd.ignoreWindowPosEventFrame + 1);
            if (ignoreEvent) {
                return;
            }
        }

        vp.setPlatformRequestMove(true);
    }

    private void windowSizeCallback(final ImGuiViewport vp, final int width, final int height) {
        if (vp.isNotValidPtr()) {
            return;
        }

        final ViewportData vd = (ViewportData) vp.getPlatformUserData();
        if (vd != null) {
            final boolean ignoreEvent = (this.bridge.getFrame() <= vd.ignoreWindowSizeEventFrame + 1);
            if (ignoreEvent) {
                return;
            }
        }

        vp.setPlatformRequestResize(true);
    }

    private static void setViewportHints(final int flags) {
        // GLFW 3.2 unfortunately always set focus on glfwCreateWindow() if GLFW_VISIBLE is set, regardless of GLFW_FOCUSED
        // With GLFW 3.3, the hint GLFW_FOCUS_ON_SHOW fixes this problem
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_FOCUSED, GLFW_FALSE);
        if (glfwHasFocusOnShow) {
            glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE);
        }
        glfwWindowHint(GLFW_DECORATED, (flags & ImGuiViewportFlags.NoDecoration) != 0 ? GLFW_FALSE : GLFW_TRUE);
        if (glfwHasWindowTopmost) {
            glfwWindowHint(GLFW_FLOATING, (flags & ImGuiViewportFlags.TopMost) != 0 ? GLFW_TRUE : GLFW_FALSE);
        }
    }

    //? if >= 26.1 {
    /*private static long createGlfwWindow(
            final int width,
            final int height,
            final long share,
            final com.mojang.blaze3d.systems.GpuBackend backend,
            final int flags) throws com.mojang.blaze3d.systems.BackendCreationException {
        final com.mojang.blaze3d.GLFWErrorCapture glfwErrors = new com.mojang.blaze3d.GLFWErrorCapture();

        final long windowHandle;
        try (final com.mojang.blaze3d.GLFWErrorScope unused = new com.mojang.blaze3d.GLFWErrorScope(glfwErrors)) {
            backend.setWindowHints();
            setViewportHints(flags);

            windowHandle = glfwCreateWindow(width, height, "No Title Yet", MemoryUtil.NULL, share);
            if (windowHandle == 0L) {
                backend.handleWindowCreationErrors(glfwErrors.firstError());
            }
        }

        for (final com.mojang.blaze3d.GLFWErrorCapture.Error error : glfwErrors) {
            ImGuiMCImpl.LOGGER.error("GLFW error collected during GL backend initialization: {}", error);
        }

        return windowHandle;
    }
    *///? }

    private final class CreateWindowFunction extends ImPlatformFuncViewport {
        @Override
        public void accept(final ImGuiViewport vp) {
            final ViewportData vd = new ViewportData();
            vp.setPlatformUserData(vd);

            // Workaround for Linux: ignore mouse up events corresponding to losing focus of the previously focused window (#7733, #3158, #7922)
            if (IS_LINUX) {
                ImGuiWindowImpl.this.data.mouseIgnoreButtonUpWaitForFocusLoss = true;
            }

            final long shareWindow = (ImGuiWindowImpl.this.data.clientApi == GlfwClientApi.OPENGL) ? ImGuiWindowImpl.this.data.window : NULL;
            vd.windowOwned = true;

            //? if >=26.1 {
            /*try {
                vd.window = createGlfwWindow(
                        (int) vp.getSizeX(),
                        (int) vp.getSizeY(),
                        shareWindow,
                        ImGuiWindowImpl.this.data.backend,
                        vp.getFlags());
            } catch (final com.mojang.blaze3d.systems.BackendCreationException throwable) {
                final net.minecraft.CrashReport crashReport = net.minecraft.CrashReport.forThrowable(throwable, "Creating ImGui Viewport");
                throw new net.minecraft.ReportedException(crashReport);
            }
            *///? } else {
            setViewportHints(vp.getFlags());
            vd.window = glfwCreateWindow(
                    (int) vp.getSizeX(),
                    (int) vp.getSizeY(),
                    "No Title Yet",
                    MemoryUtil.NULL,
                    shareWindow);
            //? }

            //? if >=26.2-pre-2 {
            /*vd.windowSurface = com.mojang.blaze3d.systems.RenderSystem.getDevice().createSurface(vd.window);
             *///? }

            vp.setPlatformHandle(vd.window);
            ImGuiWindowImpl.setRawPlatformHandle(vp, vd.window);

            glfwSetWindowPos(vd.window, (int) vp.getPosX(), (int) vp.getPosY());

            // Install GLFW callbacks for secondary viewports
            glfwSetWindowFocusCallback(vd.window, ImGuiWindowImpl.this::windowFocusCallback);
            glfwSetCursorEnterCallback(vd.window, ImGuiWindowImpl.this::cursorEnterCallback);
            glfwSetCursorPosCallback(vd.window, ImGuiWindowImpl.this::cursorPosCallback);
            glfwSetMouseButtonCallback(vd.window, ImGuiWindowImpl.this::mouseButtonCallback);
            glfwSetScrollCallback(vd.window, ImGuiWindowImpl.this::scrollCallback);
            glfwSetKeyCallback(vd.window, ImGuiWindowImpl.this::keyCallback);
            glfwSetCharCallback(vd.window, ImGuiWindowImpl.this::charCallback);
            glfwSetWindowCloseCallback(vd.window, window -> ImGuiWindowImpl.this.windowCloseCallback(vp));
            glfwSetWindowPosCallback(vd.window, (window, xpos, ypos) -> ImGuiWindowImpl.this.windowPosCallback(vp, xpos, ypos));
            glfwSetWindowSizeCallback(vd.window, (window, width, height) -> ImGuiWindowImpl.this.windowSizeCallback(vp, width, height));
        }
    }

    private final class DestroyWindowFunction extends ImPlatformFuncViewport {
        @Override
        public void accept(final ImGuiViewport vp) {
            final ViewportData vd = (ViewportData) vp.getPlatformUserData();

            if (vd != null) {
                if (vd.windowOwned) {
                    // In C++ (Win32, !GLFW_HAS_MOUSE_PASSTHROUGH && GLFW_HAS_WINDOW_HOVERED): RemovePropA(hwnd, "IMGUI_VIEWPORT").
                    // In Java: no WndProc hook means there is no prop to remove — see follow-up in review.md.
                    if (!glfwHasMousePassthrough && glfwHasWindowHovered && IS_WINDOWS) {
                        // intentionally empty — see comment above.
                    }

                    // Release any keys that were pressed in the window being destroyed and are still held down,
                    // because we will not receive any release events after window is destroyed.
                    for (int i = 0; i < ImGuiWindowImpl.this.data.keyOwnerWindows.length; i++) {
                        if (ImGuiWindowImpl.this.data.keyOwnerWindows[i] == vd.window) {
                            ImGuiWindowImpl.this.keyCallback(vd.window, i, 0, GLFW_RELEASE, 0); // Later params are only used for main viewport, on which this function is never called.
                        }
                    }

                    Callbacks.glfwFreeCallbacks(vd.window);
                    glfwDestroyWindow(vd.window);

                    vd.window = -1;
                }

                if (vd.viewportData != null) {
                    vd.viewportData.free();
                    vd.viewportData = null;
                }

                //? if >=26.2-pre-2 {
                /*if (vd.windowSurface != null) {
                    vd.windowSurface.close();
                    vd.windowSurface = null;
                }
                *///? }
            }

            vp.setPlatformHandle(-1);
            vp.setPlatformHandleRaw(-1);
            vp.setPlatformUserData(null);
        }
    }

    private static final class ShowWindowFunction extends ImPlatformFuncViewport {
        @Override
        public void accept(final ImGuiViewport vp) {
            final ViewportData vd = (ViewportData) vp.getPlatformUserData();
            if (vd == null) {
                return;
            }

            if (IS_WINDOWS && vp.hasFlags(ImGuiViewportFlags.NoTaskBarIcon)) {
                ImGuiImplGlfwNative.win32hideFromTaskBar(vp.getPlatformHandleRaw());
            }

            // In C++ (Win32): SetPropA + SetWindowLongPtrW(hwnd, GWLP_WNDPROC, ImGui_ImplGlfw_WndProc) — for MouseSourceEvent (touch/pen) and WM_NCHITTEST/HTTRANSPARENT.
            // In Java: WndProc hook is not implemented (requires JNI/JNA), see follow-up in review.md.
            // In C++ (GLFW < 3.3 fallback for NoFocusOnAppearing): workaround uses ::ShowWindow(hwnd, SW_SHOWNA).
            // In Java: LWJGL 3.x requires GLFW >= 3.3, this branch does not apply.
            glfwShowWindow(vd.window);
        }
    }

    private static final class GetWindowPosFunction extends ImPlatformFuncViewportSuppImVec2 {
        private final int[] posX = new int[1];
        private final int[] posY = new int[1];

        @Override
        public void get(final ImGuiViewport vp, final ImVec2 dst) {
            final ViewportData vd = (ViewportData) vp.getPlatformUserData();
            if (vd == null) {
                return;
            }
            this.posX[0] = 0;
            this.posY[0] = 0;
            glfwGetWindowPos(vd.window, this.posX, this.posY);
            dst.set(this.posX[0], this.posY[0]);
        }
    }

    private static final class SetWindowPosFunction extends ImPlatformFuncViewportImVec2 {
        private final ImGuiHandler bridge;

        private SetWindowPosFunction(final ImGuiHandler bridge) {
            this.bridge = bridge;
        }

        @Override
        public void accept(final ImGuiViewport vp, final ImVec2 value) {
            final ViewportData vd = (ViewportData) vp.getPlatformUserData();
            if (vd == null) {
                return;
            }
            vd.ignoreWindowPosEventFrame = this.bridge.getFrame();
            glfwSetWindowPos(vd.window, (int) value.x, (int) value.y);
        }
    }

    private static final class GetWindowSizeFunction extends ImPlatformFuncViewportSuppImVec2 {
        private final int[] width = new int[1];
        private final int[] height = new int[1];

        @Override
        public void get(final ImGuiViewport vp, final ImVec2 dst) {
            final ViewportData vd = (ViewportData) vp.getPlatformUserData();
            if (vd == null) {
                return;
            }
            this.width[0] = 0;
            this.height[0] = 0;
            glfwGetWindowSize(vd.window, this.width, this.height);
            dst.x = this.width[0];
            dst.y = this.height[0];
        }
    }

    private static final class SetWindowSizeFunction extends ImPlatformFuncViewportImVec2 {
        private final ImGuiHandler bridge;
        private final int[] x = new int[1];
        private final int[] y = new int[1];
        private final int[] width = new int[1];
        private final int[] height = new int[1];

        private SetWindowSizeFunction(final ImGuiHandler bridge) {
            this.bridge = bridge;
        }

        @Override
        public void accept(final ImGuiViewport vp, final ImVec2 value) {
            final ViewportData vd = (ViewportData) vp.getPlatformUserData();
            if (vd == null) {
                return;
            }
            if (IS_APPLE && !glfwHasOsxWindowPosFix) {
                // Native OS windows are positioned from the bottom-left corner on macOS, whereas on other platforms they are
                // positioned from the upper-left corner. GLFW makes an effort to convert macOS style coordinates, however it
                // doesn't handle it when changing size. We are manually moving the window in order for changes of size to be based
                // on the upper-left corner.
                this.x[0] = 0;
                this.y[0] = 0;
                this.width[0] = 0;
                this.height[0] = 0;
                glfwGetWindowPos(vd.window, this.x, this.y);
                glfwGetWindowSize(vd.window, this.width, this.height);
                glfwSetWindowPos(vd.window, this.x[0], this.y[0] - this.height[0] + (int) value.y);
            }
            vd.ignoreWindowSizeEventFrame = this.bridge.getFrame();
            glfwSetWindowSize(vd.window, (int) value.x, (int) value.y);
        }
    }

    private static final class SetWindowTitleFunction extends ImPlatformFuncViewportString {
        @Override
        public void accept(final ImGuiViewport vp, final String value) {
            final ViewportData vd = (ViewportData) vp.getPlatformUserData();
            if (vd != null) {
                glfwSetWindowTitle(vd.window, value);
            }
        }
    }

    private static final class SetWindowFocusFunction extends ImPlatformFuncViewport {
        @Override
        public void accept(final ImGuiViewport vp) {
            if (glfwHasFocusWindow) {
                final ViewportData vd = (ViewportData) vp.getPlatformUserData();
                if (vd != null) {
                    glfwFocusWindow(vd.window);
                }
            }
        }
    }

    private static final class GetWindowFocusFunction extends ImPlatformFuncViewportSuppBoolean {
        @Override
        public boolean get(final ImGuiViewport vp) {
            final ViewportData data = (ViewportData) vp.getPlatformUserData();
            return glfwGetWindowAttrib(data.window, GLFW_FOCUSED) != 0;
        }
    }

    private static final class GetWindowMinimizedFunction extends ImPlatformFuncViewportSuppBoolean {
        @Override
        public boolean get(final ImGuiViewport vp) {
            final ViewportData vd = (ViewportData) vp.getPlatformUserData();
            if (vd != null) {
                return glfwGetWindowAttrib(vd.window, GLFW_ICONIFIED) != GLFW_FALSE;
            }
            return false;
        }
    }

    private static final class SetWindowAlphaFunction extends ImPlatformFuncViewportFloat {
        @Override
        public void accept(final ImGuiViewport vp, final float value) {
            if (glfwHasWindowAlpha) {
                final ViewportData vd = (ViewportData) vp.getPlatformUserData();
                if (vd != null) {
                    glfwSetWindowOpacity(vd.window, value);
                }
            }
        }
    }

    private static final class SwapBuffersFunction extends ImPlatformFuncViewport {
        private final Data data;

        private SwapBuffersFunction(final Data data) {
            this.data = data;
        }

        @Override
        public void accept(final ImGuiViewport vp) {
            final ViewportData vd = (ViewportData) vp.getPlatformUserData();
            if (vd == null) {
                return;
            }

            final RenderViewportData viewportData = vd.viewportData;
            if (viewportData == null) {
                return;
            }

            //? if >= 26.2-pre-2 {
            /*if (vd.windowSurface.isAcquired()) {
                return;
            }

            if (vd.windowSurfaceNeedsReconfiguring || vd.surfaceIsInvalid || vd.windowSurface.isSuboptimal()) {
                final int width;
                final int height;
                try (final MemoryStack stack = MemoryStack.stackPush()) {
                    final IntBuffer w = stack.mallocInt(1);
                    final IntBuffer h = stack.mallocInt(1);
                    GLFW.glfwGetFramebufferSize(vd.window, w, h);
                    width = w.get(0);
                    height = h.get(0);
                }
                if (width != 0 || height != 0) {
                    final com.mojang.blaze3d.systems.GpuSurface.PresentMode presentMode =
                            com.mojang.blaze3d.systems.GpuSurface.PresentMode.getSupportedVsyncMode(vd.windowSurface.supportedPresentModes(), true);
                    final com.mojang.blaze3d.systems.GpuSurface.Configuration config = new com.mojang.blaze3d.systems.GpuSurface.Configuration(width, height, presentMode);

                    try {
                        vd.windowSurface.configure(config);
                        vd.windowSurfaceNeedsReconfiguring = false;
                        vd.surfaceIsInvalid = false;
                    } catch (final com.mojang.blaze3d.systems.SurfaceException exception) {
                        ImGuiMCImpl.LOGGER.warn("Couldn't configure surface to {}: {}", config, exception);
                        vd.surfaceIsInvalid = true;
                    }
                }
            }

            if (!vd.surfaceIsInvalid && !vp.hasFlags(ImGuiViewportFlags.IsMinimized)) {
                try {
                    vd.windowSurface.acquireNextTexture();
                } catch (final com.mojang.blaze3d.systems.SurfaceException ex) {
                    ImGuiMCImpl.LOGGER.warn("Couldn't acquire next surface texture with config {}: {}", vd.windowSurface.currentConfiguration(), ex);
                    vd.surfaceIsInvalid = true;
                    vd.windowSurfaceNeedsReconfiguring = true;
                }
            }

            if (vd.windowSurface.isAcquired()) {
                final long oldContext;
                if (this.data.clientApi == GlfwClientApi.OPENGL) {
                    oldContext = glfwGetCurrentContext();
                    glfwMakeContextCurrent(vd.window);
                } else {
                    oldContext = 0;
                }

                final com.mojang.blaze3d.textures.GpuTextureView colorTexture = viewportData.getRenderTarget().getColorTextureView();
                if (colorTexture == null) {
                    throw new IllegalStateException("Can't blit to screen, color texture doesn't exist yet");
                }

                vd.windowSurface.blitFromTexture(com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder(), colorTexture);

                if (this.data.clientApi == GlfwClientApi.OPENGL) {
                    glfwMakeContextCurrent(oldContext);
                }
            }
            *///? } else {
            final long oldContext;
            if (this.data.clientApi == GlfwClientApi.OPENGL) {
                oldContext = glfwGetCurrentContext();
                glfwMakeContextCurrent(vd.window);
            } else {
                oldContext = 0;
            }

            //? if >=1.21.5 {
            /*viewportData.getRenderTarget().blitToScreen();
            *///? } else {
            foundry.imgui.impl.renderer.OpenGLPresentation.get().presentToScreen(viewportData.getRenderTarget());
            //? }

            if (this.data.clientApi == GlfwClientApi.OPENGL) {
                glfwMakeContextCurrent(oldContext);
            }
            //? }
        }
    }

    protected void initPlatformInterface() {
        // In C++: when GLFW_HAS_VULKAN is set, Platform_CreateVkSurface is registered — a helper for the Vulkan renderer.
        // In Java: the Vulkan branch is intentionally omitted (no Vulkan binding in this project).
        // In C++ (1.92+): Platform_GetWindowFramebufferScale is installed on ImGuiPlatformIO for per-viewport DPI.
        // In Java: ImGuiPlatformIO does not expose the corresponding setter — see follow-up in review.md.
        final ImGuiPlatformIO platformIO = ImGui.getPlatformIO();

        // Register platform interface (will be coupled with a renderer interface)
        platformIO.setPlatformCreateWindow(new CreateWindowFunction());
        platformIO.setPlatformDestroyWindow(new DestroyWindowFunction());
        platformIO.setPlatformShowWindow(new ShowWindowFunction());
        platformIO.setPlatformGetWindowPos(new GetWindowPosFunction());
        platformIO.setPlatformSetWindowPos(new SetWindowPosFunction(this.bridge));
        platformIO.setPlatformGetWindowSize(new GetWindowSizeFunction());
        platformIO.setPlatformSetWindowSize(new SetWindowSizeFunction(this.bridge));
        platformIO.setPlatformSetWindowTitle(new SetWindowTitleFunction());
        platformIO.setPlatformSetWindowFocus(new SetWindowFocusFunction());
        platformIO.setPlatformGetWindowFocus(new GetWindowFocusFunction());
        platformIO.setPlatformGetWindowMinimized(new GetWindowMinimizedFunction());
        platformIO.setPlatformSetWindowAlpha(new SetWindowAlphaFunction());
        platformIO.setPlatformSwapBuffers(new SwapBuffersFunction(this.data));

        // Register main window handle (which is owned by the main application, not by us)
        // This is mostly for simplicity and consistency, so that our code (e.g. mouse handling etc.) can use same logic for main and secondary viewports.
        final ImGuiViewport mainViewport = ImGui.getMainViewport();
        final ViewportData vd = new ViewportData();
        vd.window = this.data.window;
        vd.windowOwned = false;
        mainViewport.setPlatformUserData(vd);
        mainViewport.setPlatformHandle(this.data.window);
    }

    protected void shutdownPlatformInterface() {
        ImGui.destroyPlatformWindows();
    }

    // Mirrors C++ enum GlfwClientApi. UNKNOWN matches C++'s GlfwClientApi_Unknown ("anything else").
    public enum GlfwClientApi {
        UNKNOWN,
        OPENGL,
        VULKAN,
        OTHER
    }
}