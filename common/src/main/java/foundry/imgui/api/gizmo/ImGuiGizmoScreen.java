package foundry.imgui.api.gizmo;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.imguizmo.flag.Operation;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import org.joml.Matrix4f;
// GHO__ST
public final class ImGuiGizmoScreen {

    private final String id;

    private ImGuiGizmoConfig config;
    private ImGuiGizmoTheme theme;
    private ImGuiGizmoCamera camera;
    private ImGuiGizmoMiniBar miniBar;

    private float width = 0f;
    private float height = 0f;

    private int textureId = -1;
    private boolean flipTextureY = true;

    private final ImGuiGizmoMatrices matrices = new ImGuiGizmoMatrices();
    private final Matrix4f afterView = new Matrix4f();

    private boolean wasUsedLastFrame = false;

    // -1 means fall through to config value
    private int runtimeOperation = -1;
    private float runtimeGridSize = -1f;

    private float modelScale = 1f;

    public ImGuiGizmoScreen(String id) {
        this.id = id;
        config = ImGuiGizmoConfig.universal().build();
        theme = ImGuiGizmoTheme.dark().build();
        camera = new ImGuiGizmoCamera();
    }

    public ImGuiGizmoScreen setConfig(ImGuiGizmoConfig cfg) { config = cfg; return this; }
    public ImGuiGizmoScreen setTheme(ImGuiGizmoTheme t) { theme = t; return this; }
    public ImGuiGizmoScreen setCamera(ImGuiGizmoCamera cam) { camera = cam; return this; }
    public ImGuiGizmoScreen setMiniBar(ImGuiGizmoMiniBar bar) { miniBar = bar; return this; }
    public ImGuiGizmoScreen setSize(float w, float h) { width = w; height = h; return this; }
    public ImGuiGizmoScreen setTextureId(int id) { textureId = id; return this; }
    public ImGuiGizmoScreen setFlipTextureY(boolean flip) { flipTextureY = flip; return this; }
    public ImGuiGizmoScreen setRuntimeOperation(int op) { runtimeOperation = op; return this; }
    public ImGuiGizmoScreen setRuntimeGridSize(float s) { runtimeGridSize = s; return this; }
    public ImGuiGizmoScreen setModelScale(float scale) { modelScale = Math.max(0.0001f, scale); return this; }

    public ImGuiGizmoScreen setModelMatrix(Matrix4f m) {
        ImGuiGizmoMatrices.fromMatrix4f(m, matrices.modelRaw());
        matrices.sync();
        return this;
    }

    public void render() {
        ImVec2 avail = ImGui.getContentRegionAvail();
        float w = Math.max(1f, width > 0f ? width : avail.x);
        float h = Math.max(1f, height > 0f ? height : avail.y);

        ImGuiGizmoTheme.pushColor(ImGuiCol.ChildBg, theme.backgroundArgb);
        ImGuiGizmoTheme.pushColor(ImGuiCol.Border, theme.borderArgb);
        ImGui.pushStyleVar(ImGuiStyleVar.ChildRounding, theme.borderRounding);
        ImGui.pushStyleVar(ImGuiStyleVar.ChildBorderSize, theme.borderThickness);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f);

        boolean childOk = ImGui.beginChild("##ImguiGizmoScreen_" + id, w, h, true,
                ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);

        ImGui.popStyleVar(3);
        ImGui.popStyleColor(2);

        if (!childOk) { ImGui.endChild(); return; }

        ImVec2 winPos = ImGui.getWindowPos();
        ImVec2 winSize = ImGui.getWindowSize();
        float wx = winPos.x, wy = winPos.y;
        float ww = winSize.x, wh = winSize.y;

        matrices.updateFromCamera(camera, ww, wh);

        boolean isOrtho = config.orthographic || camera.isOrthographic();
        ImGuiGizmoMatrices.Context ctx = matrices.beginContext(isOrtho, wx, wy, ww, wh);

        float gs = runtimeGridSize > 0f ? runtimeGridSize : config.gridSize;
        if (config.showGrid) {
            ctx.drawGrid(gs);
        }

        if (textureId > 0) {
            float v0 = flipTextureY ? 1f : 0f;
            float v1 = flipTextureY ? 0f : 1f;
            ImGui.setCursorPos(0f, 0f);
            ImGui.image(textureId, ww, wh, 0f, v0, 1f, v1);
            ctx.reanchor();
        }

        int operation = resolveOperation();
        if (!config.lockTransform && operation >= 0) {
            float[] snap = buildSnapArray(operation);
            wasUsedLastFrame = ctx.drawManipulator(operation, config.mode, snap, config.localBounds, config.boundsSnap);
        } else {
            wasUsedLastFrame = false;
        }

        camera.handleInput(config.lockView);

        if (!config.lockView) {
            boolean cubeChanged = ctx.drawViewCubeTopRight(theme.viewCubeSize, config.viewCubeArmLength, theme.viewCubeBackground);
            if (cubeChanged) {
                ImGuiGizmoMatrices.toMatrix4f(matrices.viewRaw(), afterView);
                camera.syncFromViewMatrix(afterView);
            }
        }

        if (miniBar != null) {
            miniBar.render(this, wx, wy, ww, wh);
        }

        matrices.sync();
        applyModelScale();
        ImGui.endChild();
    }

    // priority: mini-bar > runtimeOperation > config
    private int resolveOperation() {
        if (miniBar != null && miniBar.getSelectedOperation() >= 0) return miniBar.getSelectedOperation();
        return runtimeOperation >= 0 ? runtimeOperation : config.operation;
    }

    private float[] buildSnapArray(int operation) {
        if ((operation & Operation.TRANSLATE) != 0 && config.snapTranslation)
            return new float[]{config.snapTranslationStep, config.snapTranslationStep, config.snapTranslationStep};
        if ((operation & Operation.ROTATE) != 0 && config.snapRotation)
            return new float[]{config.snapRotationDeg, config.snapRotationDeg, config.snapRotationDeg};
        if (((operation & Operation.SCALE) != 0 || (operation & Operation.SCALEU) != 0) && config.snapScale)
            return new float[]{config.snapScaleStep, config.snapScaleStep, config.snapScaleStep};
        return null;
    }

    // Re-normalises the rotation columns and reapplies modelScale after the gizmo may have modified them.
    private void applyModelScale() {
        if (modelScale == 1f) return;
        float[] raw = matrices.modelRaw();
        float sx = colLen(raw, 0);
        float sy = colLen(raw, 4);
        float sz = colLen(raw, 8);
        if (sx < 0.0001f || sy < 0.0001f || sz < 0.0001f) return;
        float rx = modelScale / sx;
        float ry = modelScale / sy;
        float rz = modelScale / sz;
        raw[0] *= rx; raw[1] *= rx; raw[2] *= rx;
        raw[4] *= ry; raw[5] *= ry; raw[6] *= ry;
        raw[8] *= rz; raw[9] *= rz; raw[10] *= rz;
        matrices.sync();
    }

    private static float colLen(float[] m, int off) {
        float a = m[off], b = m[off + 1], c = m[off + 2];
        return (float) Math.sqrt(a * a + b * b + c * c);
    }

    public ImGuiGizmoMatrices getMatrices() { return matrices; }
    public boolean wasImguiGizmoUsed() { return wasUsedLastFrame; }
    public ImGuiGizmoCamera getCamera() { return camera; }
    public ImGuiGizmoConfig getConfig() { return config; }
    public ImGuiGizmoTheme getTheme() { return theme; }
    public ImGuiGizmoMiniBar getMiniBar() { return miniBar; }
    public String getId() { return id; }
    public float getModelScale() { return modelScale; }
}
