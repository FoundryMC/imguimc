package foundry.imgui.mixin.renderer.v2.viewport;

//? if >=1.21.6 {

/*import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.*;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public class GlCommandEncoderMixin {

    @Unique
    private long imguimc$context;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void init(final CallbackInfo ci) {
        this.imguimc$context = glfwGetCurrentContext();
    }

    @Inject(method = "presentTexture", at = @At("HEAD"), cancellable = true)
    public void bindFramebufferTextures(final GpuTextureView gpuTextureView, final CallbackInfo ci) {
        final long context = glfwGetCurrentContext();
        if (context == this.imguimc$context) {
            return;
        }

        ci.cancel();

        final int color0 = ((GlTexture) gpuTextureView.texture()).glId();
        final int width = gpuTextureView.getWidth(0);
        final int height = gpuTextureView.getHeight(0);

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
}
*///? }
