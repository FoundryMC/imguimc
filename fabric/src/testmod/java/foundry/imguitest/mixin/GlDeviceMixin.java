package foundry.imguitest.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice")
public class GlDeviceMixin {

    @Shadow
    protected static boolean USE_GL_ARB_buffer_storage;

    @Shadow
    protected static boolean USE_GL_ARB_vertex_attrib_binding;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void classInit(final CallbackInfo ci) {
        USE_GL_ARB_buffer_storage = false;
        USE_GL_ARB_vertex_attrib_binding = false;
    }
}
