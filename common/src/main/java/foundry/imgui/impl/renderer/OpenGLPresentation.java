package foundry.imgui.impl.renderer;

//? if <1.21.5 {

import static org.lwjgl.opengl.ARBDirectStateAccess.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.*;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

@ApiStatus.Internal
public enum OpenGLPresentation {

    LEGACY {
        @Override
        public void _presentToScreen(final int color0, final int width, final int height) {
            glDisable(GL_SCISSOR_TEST);
            glViewport(0, 0, width, height);
            glDepthMask(true);
            glColorMask(true, true, true, true);

            final int fbo = glGenFramebuffers();

            final int oldRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
            final int oldWrite = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER_BINDING, 0);
            glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, color0, 0);
            glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, oldRead);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER_BINDING, oldWrite);

            glDeleteFramebuffers(fbo);
        }
    },
    DSA {
        @Override
        public void _presentToScreen(final int color0, final int width, final int height) {
            glDisable(GL_SCISSOR_TEST);
            glViewport(0, 0, width, height);
            glDepthMask(true);
            glColorMask(true, true, true, true);

            final int fbo = glCreateFramebuffers();

            glNamedFramebufferTexture(fbo, GL_COLOR_ATTACHMENT0, color0, 0);
            glBlitNamedFramebuffer(fbo, 0, 0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);

            glDeleteFramebuffers(fbo);
        }
    };

    private static OpenGLPresentation presentation;

    OpenGLPresentation() {
    }

    public final void presentToScreen(final RenderTarget renderTarget) {
        final int color0 = renderTarget.getColorTextureId();
        final int width = renderTarget.width;
        final int height = renderTarget.height;
        this._presentToScreen(color0, width, height);
    }

    protected abstract void _presentToScreen(final int color0, final int width, final int height);

    public static OpenGLPresentation get() {
        if (presentation == null) {
            final GLCapabilities caps = GL.getCapabilities();
            if (caps.OpenGL45 || caps.GL_ARB_direct_state_access) {
                presentation = DSA;
            } else {
                presentation = LEGACY;
            }
        }
        return presentation;
    }
}
//? }
