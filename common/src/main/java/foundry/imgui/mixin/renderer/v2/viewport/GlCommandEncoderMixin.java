package foundry.imgui.mixin.renderer.v2.viewport;

//? if >=1.21.6 {

/*import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.*;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.opengl.GlTexture;
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
        //? if >=26.3 {
        /^this.imguimc$context = org.lwjgl.sdl.SDLVideo.SDL_GL_GetCurrentContext();
        ^///? } else {
        this.imguimc$context = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
         //? }
    }

    //? if >=26.2 {
    /^@Inject(method = "presentTexture", at = @At("HEAD"), cancellable = true)
    public void presentTexture(final GpuTextureView gpuTextureView, final int swapchainWidth, final int swapchainHeight, final CallbackInfo ci) {
    ^///? } else {
    @Inject(method = "presentTexture", at = @At("HEAD"), cancellable = true)
    public void presentTexture(final GpuTextureView gpuTextureView, final CallbackInfo ci) {
    //? }
        //? if >=26.3 {
        /^final long context = org.lwjgl.sdl.SDLVideo.SDL_GL_GetCurrentContext();
        ^///? } else {
        final long context = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
         //? }
        if (context == this.imguimc$context) {
            return;
        }

        ci.cancel();

        final int color0 = ((GlTexture) gpuTextureView.texture()).glId();
        final int width = gpuTextureView.getWidth(0);
        final int height = gpuTextureView.getHeight(0);

        //? if >=26.2 {
        /^final int destY = Math.max(0, swapchainHeight - height);
        final int copyWidth = Math.min(swapchainWidth, width);
        final int copyHeight = Math.min(swapchainHeight, height);
        ^///? } else {
        final int destY = 0;
        final int copyWidth = width;
        final int copyHeight = height;
        //? }

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
        glBlitFramebuffer(0, 0, copyWidth, copyHeight, 0, destY, copyWidth, destY + copyHeight, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, oldRead);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER_BINDING, oldWrite);

        glDeleteFramebuffers(fbo);
    }
}
*///? }
