package foundry.imgui.impl.renderer.v0;

//? if <1.21.6 {

import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.opengl.GL45C.GL_CLIP_ORIGIN;
import com.mojang.blaze3d.pipeline.RenderTarget;
import foundry.imgui.api.ImGuiSampler;
import foundry.imgui.api.ImGuiTextureProvider;
import foundry.imgui.impl.renderer.ImGuiRenderer;
import imgui.*;
import imgui.callback.ImPlatformFuncViewport;
import imgui.flag.ImGuiBackendFlags;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiViewportFlags;
import imgui.type.ImInt;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * This class is a straightforward port of the
 * <a href="https://raw.githubusercontent.com/ocornut/imgui/1ee252772ae9c0a971d06257bb5c89f628fa696a/backends/imgui_impl_opengl3.cpp">imgui_impl_opengl3.cpp</a>.
 * <p>
 * It does support a backup and restoring of the GL state in the same way the original Dear ImGui code does.
 * Some of the very specific OpenGL variables may be ignored here,
 * yet you can copy-paste this class in your codebase and modify the rendering routine in the way you'd like.
 */
@ApiStatus.Internal
public class ImGuiRendererGL33 implements ImGuiRenderer {

    /**
     * Data class to store implementation specific fields.
     * Same as {@code ImGui_ImplOpenGL3_Data}.
     */
    protected static class Data {
        protected int glVersion;
        protected boolean glProfileIsCompat;
        protected int glProfileMask;
        protected int fontTexture = 0;
        protected int shaderHandle = 0;
        protected int attribLocationTex = 0; // Uniforms location
        protected int attribLocationProjMtx = 0;
        protected int attribLocationVtxPos = 0; // Vertex attributes location
        protected int attribLocationVtxUV = 0;
        protected int attribLocationVtxColor = 0;
        protected int vboHandle = 0;
        protected int elementsHandle = 0;
        // protected int vertexBufferSize;
        // protected int indexBufferSize;
        // protected boolean hasPolygonMode;
        protected boolean hasClipOrigin;
    }

    /**
     * Internal class to store containers for frequently used arrays.
     * This class helps minimize the number of object allocations on the JVM side,
     * thereby improving performance and reducing garbage collection overhead.
     */
    private static final class Properties {
        private final ImVec4 clipRect = new ImVec4();
        private final float[] orthoProjMatrix = new float[4 * 4];
        private final int[] lastActiveTexture = new int[1];
        private final int[] lastProgram = new int[1];
        private final int[] lastTexture = new int[1];
        private final int[] lastSampler = new int[1];
        private final int[] lastArrayBuffer = new int[1];
        private final int[] lastVertexArrayObject = new int[1];
        private final int[] lastPolygonMode = new int[2];
        private final int[] lastViewport = new int[4];
        private final int[] lastScissorBox = new int[4];
        private final int[] lastBlendSrcRgb = new int[1];
        private final int[] lastBlendDstRgb = new int[1];
        private final int[] lastBlendSrcAlpha = new int[1];
        private final int[] lastBlendDstAlpha = new int[1];
        private final int[] lastBlendEquationRgb = new int[1];
        private final int[] lastBlendEquationAlpha = new int[1];
        private boolean lastEnableBlend = false;
        private boolean lastEnableCullFace = false;
        private boolean lastEnableDepthTest = false;
        private boolean lastEnableStencilTest = false;
        private boolean lastEnableScissorTest = false;
        private boolean lastEnablePrimitiveRestart = false;
    }

    protected Data data = null;
    private final Properties props = new Properties();

    protected Data newData() {
        return new Data();
    }

    @Override
    public void init() {
        this.data = this.newData();

        final ImGuiIO io = ImGui.getIO();
        io.setBackendRendererName("imgui-java_impl_opengl3");

        {
            int major = glGetInteger(GL_MAJOR_VERSION);
            int minor = glGetInteger(GL_MINOR_VERSION);
            if (major == 0 && minor == 0) {
                // Query GL_VERSION in desktop GL 2.x, the string will start with "<major>.<minor>"
                final String glVersion = glGetString(GL_VERSION);
                if (glVersion != null) {
                    final String[] glVersions = glVersion.split("\\.");
                    major = Integer.parseInt(glVersions[0]);
                    minor = Integer.parseInt(glVersions[1]);
                }
            }

            this.data.glVersion = major * 100 + minor * 10;
            this.data.hasClipOrigin = this.data.glVersion >= 450;

            this.data.glProfileMask = glGetInteger(GL_CONTEXT_PROFILE_MASK);
            this.data.glProfileIsCompat = (this.data.glProfileMask & GL_CONTEXT_COMPATIBILITY_PROFILE_BIT) != 0;
        }

        // We can honor the ImDrawCmd::VtxOffset field, allowing for large meshes.
        io.addBackendFlags(ImGuiBackendFlags.RendererHasVtxOffset);

        // We can create multi-viewports on the Renderer side (optional)
        io.addBackendFlags(ImGuiBackendFlags.RendererHasViewports);

        // Make an arbitrary GL call (we don't actually need the result)
        // IF YOU GET A CRASH HERE: it probably means the OpenGL function loader didn't do its job. Let us know!
        {
            final int[] currentTexture = new int[1];
            glGetIntegerv(GL_TEXTURE_BINDING_2D, currentTexture);
        }

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            this.initPlatformInterface();
        }
    }

    @Override
    public void free() {
        final ImGuiIO io = ImGui.getIO();

        this.shutdownPlatformInterface();
        this.destroyDeviceObjects();

        io.setBackendRendererName(null);
        io.removeBackendFlags(ImGuiBackendFlags.RendererHasVtxOffset | ImGuiBackendFlags.RendererHasViewports);
        this.data = null;
    }

    @Override
    public void newFrame() {
        if (this.data.shaderHandle == 0) {
            this.createDeviceObjects();
        }
        if (this.data.fontTexture == 0) {
            this.createFontsTexture();
        }
    }

    protected void setupRenderState(final ImDrawData drawData, final int fbWidth, final int fbHeight, final int gVertexArrayObject) {
        // Setup render state: alpha-blending enabled, no face culling, no depth testing, scissor enabled, polygon fill
        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_STENCIL_TEST);
        glEnable(GL_SCISSOR_TEST);

        glDisable(GL_PRIMITIVE_RESTART);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        // Support for GL 4.5 rarely used glClipControl(GL_UPPER_LEFT)
        boolean clipOriginLowerLeft = true;
        if (this.data.hasClipOrigin) {
            final int[] currentClipOrigin = new int[1];
            glGetIntegerv(GL_CLIP_ORIGIN, currentClipOrigin);
            if (currentClipOrigin[0] == GL_UPPER_LEFT) {
                clipOriginLowerLeft = false;
            }
        }

        // Setup viewport, orthographic projection matrix
        // Our visible imgui space lies from draw_data->DisplayPos (top left) to draw_data->DisplayPos+data_data->DisplaySize (bottom right).
        // DisplayPos is (0,0) for single viewport apps.
        glViewport(0, 0, fbWidth, fbHeight);
        final float L = drawData.getDisplayPosX();
        final float R = drawData.getDisplayPosX() + drawData.getDisplaySizeX();
        float T = drawData.getDisplayPosY();
        float B = drawData.getDisplayPosY() + drawData.getDisplaySizeY();

        // Swap top and bottom if origin is upper left
        if (this.data.hasClipOrigin && !clipOriginLowerLeft) {
            final float tmp = T;
            T = B;
            B = tmp;
        }

        this.props.orthoProjMatrix[0] = 2.0f / (R - L);
        this.props.orthoProjMatrix[5] = 2.0f / (T - B);
        this.props.orthoProjMatrix[10] = -1.0f;
        this.props.orthoProjMatrix[12] = (R + L) / (L - R);
        this.props.orthoProjMatrix[13] = (T + B) / (B - T);
        this.props.orthoProjMatrix[15] = 1.0f;

        glUseProgram(this.data.shaderHandle);
        glUniform1i(this.data.attribLocationTex, 0);
        glUniformMatrix4fv(this.data.attribLocationProjMtx, false, this.props.orthoProjMatrix);

        glBindSampler(0, 0);

        glBindVertexArray(gVertexArrayObject);

        // Bind vertex/index buffers and setup attributes for ImDrawVert
        glBindBuffer(GL_ARRAY_BUFFER, this.data.vboHandle);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.data.elementsHandle);
        glEnableVertexAttribArray(this.data.attribLocationVtxPos);
        glEnableVertexAttribArray(this.data.attribLocationVtxUV);
        glEnableVertexAttribArray(this.data.attribLocationVtxColor);
        glVertexAttribPointer(this.data.attribLocationVtxPos, 2, GL_FLOAT, false, ImDrawData.sizeOfImDrawVert(), 0);
        glVertexAttribPointer(this.data.attribLocationVtxUV, 2, GL_FLOAT, false, ImDrawData.sizeOfImDrawVert(), 8);
        glVertexAttribPointer(this.data.attribLocationVtxColor, 4, GL_UNSIGNED_BYTE, true, ImDrawData.sizeOfImDrawVert(), 16);
    }

    @Override
    public void renderDrawData(final ImDrawData drawData, final RenderTarget renderTarget) {
        // Avoid rendering when minimized, scale coordinates for retina displays (screen coordinates != framebuffer coordinates)
        final int fbWidth = (int) (drawData.getDisplaySizeX() * drawData.getFramebufferScaleX());
        final int fbHeight = (int) (drawData.getDisplaySizeY() * drawData.getFramebufferScaleY());
        if (fbWidth <= 0 || fbHeight <= 0) {
            return;
        }

        if (drawData.getCmdListsCount() <= 0) {
            return;
        }

        glGetIntegerv(GL_ACTIVE_TEXTURE, this.props.lastActiveTexture);
        glActiveTexture(GL_TEXTURE0);
        glGetIntegerv(GL_CURRENT_PROGRAM, this.props.lastProgram);
        glGetIntegerv(GL_TEXTURE_BINDING_2D, this.props.lastTexture);
        glGetIntegerv(GL_SAMPLER_BINDING, this.props.lastSampler);
        glGetIntegerv(GL_ARRAY_BUFFER_BINDING, this.props.lastArrayBuffer);
        glGetIntegerv(GL_VERTEX_ARRAY_BINDING, this.props.lastVertexArrayObject);
        glGetIntegerv(GL_POLYGON_MODE, this.props.lastPolygonMode);
        glGetIntegerv(GL_VIEWPORT, this.props.lastViewport);
        glGetIntegerv(GL_SCISSOR_BOX, this.props.lastScissorBox);
        glGetIntegerv(GL_BLEND_SRC_RGB, this.props.lastBlendSrcRgb);
        glGetIntegerv(GL_BLEND_DST_RGB, this.props.lastBlendDstRgb);
        glGetIntegerv(GL_BLEND_SRC_ALPHA, this.props.lastBlendSrcAlpha);
        glGetIntegerv(GL_BLEND_DST_ALPHA, this.props.lastBlendDstAlpha);
        glGetIntegerv(GL_BLEND_EQUATION_RGB, this.props.lastBlendEquationRgb);
        glGetIntegerv(GL_BLEND_EQUATION_ALPHA, this.props.lastBlendEquationAlpha);
        this.props.lastEnableBlend = glIsEnabled(GL_BLEND);
        this.props.lastEnableCullFace = glIsEnabled(GL_CULL_FACE);
        this.props.lastEnableDepthTest = glIsEnabled(GL_DEPTH_TEST);
        this.props.lastEnableStencilTest = glIsEnabled(GL_STENCIL_TEST);
        this.props.lastEnableScissorTest = glIsEnabled(GL_SCISSOR_TEST);
        this.props.lastEnablePrimitiveRestart = glIsEnabled(GL_PRIMITIVE_RESTART);

        // Setup desired GL state
        // Recreate the VAO every time (this is to easily allow multiple GL contexts to be rendered to. VAO are not shared among GL contexts)
        // The renderer would actually work without any VAO bound, but then our VertexAttrib calls would overwrite the default one currently bound.
        final int vertexArrayObject = glGenVertexArrays();
        this.setupRenderState(drawData, fbWidth, fbHeight, vertexArrayObject);

        // Will project scissor/clipping rectangles into framebuffer space
        final float clipOffX = drawData.getDisplayPosX(); // (0,0) unless using multi-viewports
        final float clipOffY = drawData.getDisplayPosY(); // (0,0) unless using multi-viewports
        final float clipScaleX = drawData.getFramebufferScaleX(); // (1,1) unless using retina display which are often (2,2)
        final float clipScaleY = drawData.getFramebufferScaleY(); // (1,1) unless using retina display which are often (2,2)

        // Render command lists
        for (int n = 0; n < drawData.getCmdListsCount(); n++) {
            // FIXME: this is a straightforward port from Dear ImGui and it doesn't work with multi-viewports.
            //        So we keep solution we used before.
            // Upload vertex/index buffers
            // final int vtxBufferSize = drawData.getCmdListVtxBufferSize(n) * ImDrawData.sizeOfImDrawVert();
            // final int idxBufferSize = drawData.getCmdListIdxBufferSize(n) * ImDrawData.sizeOfImDrawIdx();
            // if (data.vertexBufferSize < vtxBufferSize) {
            //     data.vertexBufferSize = vtxBufferSize;
            //     glBufferData(GL_ARRAY_BUFFER, data.vertexBufferSize, GL_STREAM_DRAW);
            // }
            // if (data.indexBufferSize < idxBufferSize) {
            //     data.indexBufferSize = idxBufferSize;
            //     glBufferData(GL_ELEMENT_ARRAY_BUFFER, data.indexBufferSize, GL_STREAM_DRAW);
            // }
            // glBufferSubData(GL_ARRAY_BUFFER, 0, drawData.getCmdListVtxBufferData(n));
            // glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, drawData.getCmdListIdxBufferData(n));

            glBufferData(GL_ARRAY_BUFFER, drawData.getCmdListVtxBufferData(n), GL_STREAM_DRAW);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, drawData.getCmdListIdxBufferData(n), GL_STREAM_DRAW);

            for (int cmdIdx = 0; cmdIdx < drawData.getCmdListCmdBufferSize(n); cmdIdx++) {
                // TODO:
                // if userCallback
                // else

                drawData.getCmdListCmdBufferClipRect(this.props.clipRect, n, cmdIdx);

                final float clipMinX = (this.props.clipRect.x - clipOffX) * clipScaleX;
                final float clipMinY = (this.props.clipRect.y - clipOffY) * clipScaleY;
                final float clipMaxX = (this.props.clipRect.z - clipOffX) * clipScaleX;
                final float clipMaxY = (this.props.clipRect.w - clipOffY) * clipScaleY;

                if (clipMaxX <= clipMinX || clipMaxY <= clipMinY) {
                    continue;
                }

                // Apply scissor/clipping rectangle (Y is inverted in OpenGL)
                glScissor((int) clipMinX, (int) (fbHeight - clipMaxY), (int) (clipMaxX - clipMinX), (int) (clipMaxY - clipMinY));

                // Bind texture, Draw
                final long textureId = drawData.getCmdListCmdBufferTextureId(n, cmdIdx);
                final int elemCount = drawData.getCmdListCmdBufferElemCount(n, cmdIdx);
                final int idxOffset = drawData.getCmdListCmdBufferIdxOffset(n, cmdIdx);
                final int vtxOffset = drawData.getCmdListCmdBufferVtxOffset(n, cmdIdx);
                final long indices = idxOffset * (long) ImDrawData.sizeOfImDrawIdx();
                final int type = ImDrawData.sizeOfImDrawIdx() == 2 ? GL_UNSIGNED_SHORT : GL_UNSIGNED_INT;

                glBindTexture(GL_TEXTURE_2D, (int) textureId);
                glDrawElementsBaseVertex(GL_TRIANGLES, elemCount, type, indices, vtxOffset);
            }
        }

        // Destroy the temporary VAO
        glDeleteVertexArrays(vertexArrayObject);

        // Restore modified GL state
        // This "glIsProgram()" check is required because if the program is "pending deletion" at the time of binding backup, it will have been deleted by now and will cause an OpenGL error. See #6220.
        if (this.props.lastProgram[0] == 0 || glIsProgram(this.props.lastProgram[0])) {
            glUseProgram(this.props.lastProgram[0]);
        }
        glBindTexture(GL_TEXTURE_2D, this.props.lastTexture[0]);
        glBindSampler(0, this.props.lastSampler[0]);
        glActiveTexture(this.props.lastActiveTexture[0]);
        glBindVertexArray(this.props.lastVertexArrayObject[0]);
        glBindBuffer(GL_ARRAY_BUFFER, this.props.lastArrayBuffer[0]);
        glBlendEquationSeparate(this.props.lastBlendEquationRgb[0], this.props.lastBlendEquationAlpha[0]);
        glBlendFuncSeparate(this.props.lastBlendSrcRgb[0], this.props.lastBlendDstRgb[0], this.props.lastBlendSrcAlpha[0], this.props.lastBlendDstAlpha[0]);
        if (this.props.lastEnableBlend) {
            glEnable(GL_BLEND);
        } else {
            glDisable(GL_BLEND);
        }
        if (this.props.lastEnableCullFace) {
            glEnable(GL_CULL_FACE);
        } else {
            glDisable(GL_CULL_FACE);
        }
        if (this.props.lastEnableDepthTest) {
            glEnable(GL_DEPTH_TEST);
        } else {
            glDisable(GL_DEPTH_TEST);
        }
        if (this.props.lastEnableStencilTest) {
            glEnable(GL_STENCIL_TEST);
        } else {
            glDisable(GL_STENCIL_TEST);
        }
        if (this.props.lastEnableScissorTest) {
            glEnable(GL_SCISSOR_TEST);
        } else {
            glDisable(GL_SCISSOR_TEST);
        }
        if (this.props.lastEnablePrimitiveRestart) {
            glEnable(GL_PRIMITIVE_RESTART);
        } else {
            glDisable(GL_PRIMITIVE_RESTART);
        }
        if (this.data.glProfileIsCompat) {
            glPolygonMode(GL_FRONT, this.props.lastPolygonMode[0]);
            glPolygonMode(GL_BACK, this.props.lastPolygonMode[1]);
        } else {
            glPolygonMode(GL_FRONT_AND_BACK, this.props.lastPolygonMode[0]);
        }
        glViewport(this.props.lastViewport[0], this.props.lastViewport[1], this.props.lastViewport[2], this.props.lastViewport[3]);
        glScissor(this.props.lastScissorBox[0], this.props.lastScissorBox[1], this.props.lastScissorBox[2], this.props.lastScissorBox[3]);
    }

    @Override
    public void discard() {
    }

    @Override
    public void recreateFontsTexture() {
        this.destroyFontsTexture();
        this.createFontsTexture();
    }

    @Override
    public long getImGuiId(final ImGuiTextureProvider texture, @Nullable final ImGuiSampler sampler) {
        return texture.imguimc$id();
    }

    public void createFontsTexture() {
        final ImFontAtlas fontAtlas = ImGui.getIO().getFonts();

        final ImInt width = new ImInt();
        final ImInt height = new ImInt();
        final ByteBuffer pixels = fontAtlas.getTexDataAsAlpha8(width, height);

        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer lastTexture = stack.mallocInt(1);
            glGetIntegerv(GL_TEXTURE_BINDING_2D, lastTexture);
            this.data.fontTexture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, this.data.fontTexture);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, stack.ints(GL_ONE, GL_ONE, GL_ONE, GL_RED));
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1); // Not on WebGL/ES
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0); // Not on WebGL/ES
            glPixelStorei(GL_UNPACK_SKIP_ROWS, 0); // Not on WebGL/ES
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0); // Not on WebGL/ES
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, width.get(), height.get(), 0, GL_RED, GL_UNSIGNED_BYTE, pixels);

            // Store our identifier
            fontAtlas.setTexID(this.data.fontTexture);
            fontAtlas.clearTexData();

            glBindTexture(GL_TEXTURE_2D, lastTexture.get(0));
        }
    }

    public void destroyFontsTexture() {
        final ImGuiIO io = ImGui.getIO();
        if (this.data.fontTexture != 0) {
            glDeleteTextures(this.data.fontTexture);
            io.getFonts().setTexID(0);
            this.data.fontTexture = 0;
        }
    }

    protected boolean checkShader(final int handle, final String desc) {
        final int[] status = new int[1];
        final int[] logLength = new int[1];
        glGetShaderiv(handle, GL_COMPILE_STATUS, status);
        glGetShaderiv(handle, GL_INFO_LOG_LENGTH, logLength);
        if (status[0] == GL_FALSE) {
            System.err.printf("%s: failed to compile: %s\n", this, desc);
        }
        if (logLength[0] > 1) {
            final String log = glGetShaderInfoLog(handle);
            System.err.println(log);
        }
        return status[0] == GL_TRUE;
    }

    private boolean checkProgram(final int handle) {
        final int status = glGetProgrami(handle, GL_LINK_STATUS);
        final int logLength = glGetProgrami(handle, GL_INFO_LOG_LENGTH);
        if (status != GL_TRUE) {
            System.err.printf("%s: failed to link: %s\n", this, "shader program");
        }
        if (logLength > 1) {
            System.err.println(glGetProgramInfoLog(handle, logLength));
        }
        return status == GL_TRUE;
    }

    private void createDeviceObjects() {
        // Backup GL state
        final int[] lastTexture = new int[1];
        final int[] lastArrayBuffer = new int[1];
        final int[] lastVertexArray = new int[1];
        glGetIntegerv(GL_TEXTURE_BINDING_2D, lastTexture);
        glGetIntegerv(GL_ARRAY_BUFFER_BINDING, lastArrayBuffer);
        glGetIntegerv(GL_VERTEX_ARRAY_BINDING, lastVertexArray);

        // Select shaders matching our GLSL versions
        final CharSequence vertexShader;
        final CharSequence fragmentShader;

        if (this.data.glVersion >= 410) {
            vertexShader = this.vertexShaderGlsl410Core();
            fragmentShader = this.fragmentShaderGlsl410Core();
        } else {
            vertexShader = this.vertexShaderGlsl130();
            fragmentShader = this.fragmentShaderGlsl130();
        }

        // Create shaders
        final int vertHandle = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertHandle, vertexShader);
        glCompileShader(vertHandle);
        this.checkShader(vertHandle, "vertex shader");

        final int fragHandle = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragHandle, fragmentShader);
        glCompileShader(fragHandle);
        this.checkShader(fragHandle, "fragment shader");

        // Link
        this.data.shaderHandle = glCreateProgram();
        glAttachShader(this.data.shaderHandle, vertHandle);
        glAttachShader(this.data.shaderHandle, fragHandle);
        glLinkProgram(this.data.shaderHandle);
        this.checkProgram(this.data.shaderHandle);

        glDetachShader(this.data.shaderHandle, vertHandle);
        glDetachShader(this.data.shaderHandle, fragHandle);
        glDeleteShader(vertHandle);
        glDeleteShader(fragHandle);

        this.data.attribLocationTex = glGetUniformLocation(this.data.shaderHandle, "Texture");
        this.data.attribLocationProjMtx = glGetUniformLocation(this.data.shaderHandle, "ProjMtx");
        this.data.attribLocationVtxPos = glGetAttribLocation(this.data.shaderHandle, "Position");
        this.data.attribLocationVtxUV = glGetAttribLocation(this.data.shaderHandle, "UV");
        this.data.attribLocationVtxColor = glGetAttribLocation(this.data.shaderHandle, "Color");

        // Create buffers
        this.data.vboHandle = glGenBuffers();
        this.data.elementsHandle = glGenBuffers();

        this.createFontsTexture();

        // Restore modified GL state
        glBindTexture(GL_TEXTURE_2D, lastTexture[0]);
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer[0]);
        glBindVertexArray(lastVertexArray[0]);

    }

    private void destroyDeviceObjects() {
        if (this.data.vboHandle != 0) {
            glDeleteBuffers(this.data.vboHandle);
            this.data.vboHandle = 0;
        }
        if (this.data.elementsHandle != 0) {
            glDeleteBuffers(this.data.elementsHandle);
            this.data.elementsHandle = 0;
        }
        if (this.data.shaderHandle != 0) {
            glDeleteProgram(this.data.shaderHandle);
            this.data.shaderHandle = 0;
        }
        this.destroyFontsTexture();
    }

    //--------------------------------------------------------------------------------------------------------
    // MULTI-VIEWPORT / PLATFORM INTERFACE SUPPORT
    // This is an _advanced_ and _optional_ feature, allowing the backend to create and handle multiple viewports simultaneously.
    // If you are new to dear imgui or creating a new binding for dear imgui, it is recommended that you completely ignore this section first..
    //--------------------------------------------------------------------------------------------------------

    private final class RendererRenderWindowFunction extends ImPlatformFuncViewport {
        @Override
        public void accept(final ImGuiViewport vp) {
            if (!vp.hasFlags(ImGuiViewportFlags.NoRendererClear)) {
                glClearColor(0, 0, 0, 0);
                glClear(GL_COLOR_BUFFER_BIT);
            }
            ImGuiRendererGL33.this.renderDrawData(vp.getDrawData(), Minecraft.getInstance().getMainRenderTarget());
        }
    }

    protected void initPlatformInterface() {
        ImGui.getPlatformIO().setRendererRenderWindow(new RendererRenderWindowFunction());
    }

    protected void shutdownPlatformInterface() {
        ImGui.destroyPlatformWindows();
    }

    protected String vertexShaderGlsl130() {
        return """
                #version 130
                uniform mat4 ProjMtx;
                in vec2 Position;
                in vec2 UV;
                in vec4 Color;
                out vec2 Frag_UV;
                out vec4 Frag_Color;
                void main()
                {
                    Frag_UV = UV;
                    Frag_Color = Color;
                    gl_Position = ProjMtx * vec4(Position.xy,0,1);
                }
                """;
    }

    protected String vertexShaderGlsl410Core() {
        return """
                #version 410 core
                layout (location = 0) in vec2 Position;
                layout (location = 1) in vec2 UV;
                layout (location = 2) in vec4 Color;
                uniform mat4 ProjMtx;
                out vec2 Frag_UV;
                out vec4 Frag_Color;
                void main()
                {
                    Frag_UV = UV;
                    Frag_Color = Color;
                    gl_Position = ProjMtx * vec4(Position.xy,0,1);
                }
                """;
    }

    protected String fragmentShaderGlsl130() {
        return """
                #version 130
                uniform sampler2D Texture;
                in vec2 Frag_UV;
                in vec4 Frag_Color;
                out vec4 Out_Color;
                void main()
                {
                    Out_Color = Frag_Color * texture(Texture, Frag_UV.st);
                }
                """;
    }

    protected String fragmentShaderGlsl410Core() {
        return """
                #version 410 core
                in vec2 Frag_UV;
                in vec4 Frag_Color;
                uniform sampler2D Texture;
                layout (location = 0) out vec4 Out_Color;
                void main()
                {
                    Out_Color = Frag_Color * texture(Texture, Frag_UV.st);
                }
                """;
    }
}
//?}