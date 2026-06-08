package foundry.imgui.api.gizmo;

import imgui.extension.imguizmo.ImGuizmo;
import org.joml.Matrix4f;

public final class ImGuiGizmoMatrices {

    private final float[] model = new float[16];
    private final float[] view = new float[16];
    private final float[] proj = new float[16];

    private final Matrix4f modelMat = new Matrix4f();
    private final Matrix4f viewMat = new Matrix4f();
    private final Matrix4f projMat = new Matrix4f();

    ImGuiGizmoMatrices() {
        identity(model);
        identity(view);
        identity(proj);
        syncJoml();
    }

    float[] modelRaw() { return model; }
    float[] viewRaw() { return view; }
    float[] projRaw() { return proj; }

    void updateFromCamera(ImGuiGizmoCamera camera, float viewW, float viewH) {
        Matrix4f tmp = new Matrix4f();
        fromMatrix4f(camera.buildViewMatrix(tmp), view);
        fromMatrix4f(camera.buildProjectionMatrix(tmp, viewW, viewH), proj);
    }

    public Context beginContext(boolean orthographic, float wx, float wy, float ww, float wh) {
        ImGuizmo.beginFrame();
        ImGuizmo.setOrthographic(orthographic);
        ImGuizmo.setDrawList();
        ImGuizmo.setRect(wx, wy, ww, wh);
        ImGuizmo.enable(true);
        return new Context(this, wx, wy, ww, wh);
    }

    void sync() { syncJoml(); }

    public Matrix4f getModelMatrix() { return modelMat; }
    public Matrix4f getViewMatrix() { return viewMat; }
    public Matrix4f getProjMatrix() { return projMat; }
    public float[] getModelRaw() { return model; }
    public float[] getViewRaw() { return view; }
    public float[] getProjRaw() { return proj; }

    static void identity(float[] m) {
        for (int i = 0; i < 16; i++) m[i] = 0f;
        m[0] = m[5] = m[10] = m[15] = 1f;
    }

    static void fromMatrix4f(Matrix4f src, float[] dst) {
        dst[0] = src.m00(); dst[1] = src.m01(); dst[2] = src.m02(); dst[3] = src.m03();
        dst[4] = src.m10(); dst[5] = src.m11(); dst[6] = src.m12(); dst[7] = src.m13();
        dst[8] = src.m20(); dst[9] = src.m21(); dst[10] = src.m22(); dst[11] = src.m23();
        dst[12] = src.m30(); dst[13] = src.m31(); dst[14] = src.m32(); dst[15] = src.m33();
    }

    static Matrix4f toMatrix4f(float[] src, Matrix4f dst) {
        return dst.set(
                src[0], src[1], src[2], src[3],
                src[4], src[5], src[6], src[7],
                src[8], src[9], src[10], src[11],
                src[12], src[13], src[14], src[15]);
    }

    private void syncJoml() {
        toMatrix4f(model, modelMat);
        toMatrix4f(view, viewMat);
        toMatrix4f(proj, projMat);
    }

    public static final class Context {

        private final ImGuiGizmoMatrices matrices;
        public final float wx, wy, ww, wh;

        Context(ImGuiGizmoMatrices matrices, float wx, float wy, float ww, float wh) {
            this.matrices = matrices;
            this.wx = wx;
            this.wy = wy;
            this.ww = ww;
            this.wh = wh;
        }

        public void drawGrid(float cellSize, float gridCount) {
            float[] identity = new float[16];
            ImGuiGizmoMatrices.identity(identity);
            ImGuizmo.drawGrid(matrices.view, matrices.proj, identity, cellSize * gridCount);
        }

        public void drawGrid(float cellSize) { drawGrid(cellSize, 10f); }

        public void reanchor() {
            ImGuizmo.setDrawList();
            ImGuizmo.setRect(wx, wy, ww, wh);
        }

        public boolean drawManipulator(int operation, int mode, float[] snap, float[] localBounds, float[] boundsSnap) {
            if (localBounds != null) {
                ImGuizmo.manipulate(matrices.view, matrices.proj, operation, mode, matrices.model, null, snap, localBounds, boundsSnap);
            } else if (snap != null) {
                ImGuizmo.manipulate(matrices.view, matrices.proj, operation, mode, matrices.model, null, snap);
            } else {
                ImGuizmo.manipulate(matrices.view, matrices.proj, operation, mode, matrices.model);
            }
            return ImGuizmo.isUsing();
        }

        public boolean drawManipulator(int operation, int mode) {
            return drawManipulator(operation, mode, null, null, null);
        }

        public boolean drawViewCube(float armLength, float cubeX, float cubeY, float cubeW, float cubeH, int bgColor) {
            float[] before = matrices.view.clone();
            ImGuizmo.viewManipulate(matrices.view, armLength, cubeX, cubeY, cubeW, cubeH, bgColor);
            for (int i = 0; i < 16; i++) {
                if (Math.abs(before[i] - matrices.view[i]) > 1e-5f) return true;
            }
            return false;
        }

        public boolean drawViewCubeTopRight(float size, float armLen, int bgColor) {
            return drawViewCube(armLen, wx + ww - size, wy, size, size, bgColor);
        }

        public boolean isUsing() { return ImGuizmo.isUsing(); }
        public boolean isOver() { return ImGuizmo.isOver(); }
    }
}
