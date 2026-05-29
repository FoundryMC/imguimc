package foundry.imguitest.mixin;

import static org.lwjgl.opengl.GL11C.GL_READ_BUFFER;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL15C.glGetBufferSubData;
import static org.lwjgl.opengl.GL30C.GL_MAP_READ_BIT;
import static org.lwjgl.opengl.GL30C.GL_MAP_WRITE_BIT;
import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlStateManager;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import java.nio.ByteBuffer;

@Mixin(targets = "com.mojang.blaze3d.opengl.BufferStorage$Mutable")
public class MutableBufferStorageMixin {

    /**
     * @author Ocelot
     * @reason Don't use mapped buffers
     */
    @Overwrite
    public GlBuffer.GlMappedView mapBuffer(final DirectStateAccess directStateAccess, final GlBuffer buffer, final long offset, final long length, final int access) {
        GlStateManager.clearGlErrors();
        final ByteBuffer byteBuffer = MemoryUtil.memAlloc((int) length);
        if ((access & GL_MAP_READ_BIT) != 0) {
            glBindBuffer(GL_READ_BUFFER, buffer.handle);
            glGetBufferSubData(GL_READ_BUFFER, offset, byteBuffer);
        }
        return new GlBuffer.GlMappedView(() -> {
            if ((access & GL_MAP_WRITE_BIT) != 0) {
                byteBuffer.rewind();
                directStateAccess.bufferSubData(buffer.handle, offset, byteBuffer, buffer.usage());
            }
            MemoryUtil.memFree(byteBuffer);
        }, buffer, byteBuffer);
    }
}
