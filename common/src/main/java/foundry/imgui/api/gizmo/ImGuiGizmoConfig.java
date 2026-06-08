package foundry.imgui.api.gizmo;

import imgui.extension.imguizmo.flag.Mode;
import imgui.extension.imguizmo.flag.Operation;

public final class ImGuiGizmoConfig {

    public final int operation;
    public final int mode;
    public final boolean snapTranslation;
    public final float snapTranslationStep;
    public final boolean snapRotation;
    public final float snapRotationDeg;
    public final boolean snapScale;
    public final float snapScaleStep;
    public final float[] localBounds;
    public final float[] boundsSnap;
    public final float gridSize;
    public final boolean showGrid;
    public final boolean showViewCube;
    public final float viewCubeArmLength;
    public final boolean orthographic;
    public final boolean lockTransform;
    public final boolean lockView;
    public final boolean lightMode;

    private ImGuiGizmoConfig(Builder b) {
        operation = b.operation;
        mode = b.mode;
        snapTranslation = b.snapTranslation;
        snapTranslationStep = b.snapTranslationStep;
        snapRotation = b.snapRotation;
        snapRotationDeg = b.snapRotationDeg;
        snapScale = b.snapScale;
        snapScaleStep = b.snapScaleStep;
        localBounds = b.localBounds;
        boundsSnap = b.boundsSnap;
        gridSize = b.gridSize;
        showGrid = b.showGrid;
        showViewCube = b.showViewCube;
        viewCubeArmLength = b.viewCubeArmLength;
        orthographic = b.orthographic;
        lockTransform = b.lockTransform;
        lockView = b.lockView;
        lightMode = b.lightMode;
    }

    public static Builder universal() {
        return new Builder()
                .withOperation(Operation.UNIVERSAL)
                .withMode(Mode.WORLD)
                .withGridSize(1f)
                .withShowGrid(true)
                .withShowViewCube(true);
    }

    public static Builder translate() {
        return new Builder()
                .withOperation(Operation.TRANSLATE)
                .withMode(Mode.LOCAL)
                .withGridSize(1f)
                .withShowGrid(true)
                .withShowViewCube(true);
    }

    public static Builder rotate() {
        return new Builder()
                .withOperation(Operation.ROTATE)
                .withMode(Mode.LOCAL)
                .withGridSize(1f)
                .withShowGrid(false)
                .withShowViewCube(true);
    }

    public static Builder scale() {
        return new Builder()
                .withOperation(Operation.SCALEU)
                .withMode(Mode.LOCAL)
                .withGridSize(1f)
                .withShowGrid(true)
                .withShowViewCube(true);
    }

    public static Builder bounds() {
        return new Builder()
                .withOperation(Operation.BOUNDS)
                .withMode(Mode.LOCAL)
                .withLocalBounds(new float[]{-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f})
                .withGridSize(1f)
                .withShowGrid(true)
                .withShowViewCube(true);
    }

    public static Builder light() {
        return new Builder()
                .withOperation(Operation.ROTATE)
                .withMode(Mode.WORLD)
                .withGridSize(1f)
                .withShowGrid(false)
                .withShowViewCube(true)
                .withLightMode(true);
    }

    public ImGuiGizmoConfig withOperation(int op) { return toBuilder().withOperation(op).build(); }
    public ImGuiGizmoConfig withMode(int m) { return toBuilder().withMode(m).build(); }
    public ImGuiGizmoConfig withGridSize(float s) { return toBuilder().withGridSize(s).build(); }
    public ImGuiGizmoConfig withShowGrid(boolean v) { return toBuilder().withShowGrid(v).build(); }

    public Builder toBuilder() {
        Builder b = new Builder();
        b.operation = operation;
        b.mode = mode;
        b.snapTranslation = snapTranslation;
        b.snapTranslationStep = snapTranslationStep;
        b.snapRotation = snapRotation;
        b.snapRotationDeg = snapRotationDeg;
        b.snapScale = snapScale;
        b.snapScaleStep = snapScaleStep;
        b.localBounds = localBounds != null ? localBounds.clone() : null;
        b.boundsSnap = boundsSnap != null ? boundsSnap.clone() : null;
        b.gridSize = gridSize;
        b.showGrid = showGrid;
        b.showViewCube = showViewCube;
        b.viewCubeArmLength = viewCubeArmLength;
        b.orthographic = orthographic;
        b.lockTransform = lockTransform;
        b.lockView = lockView;
        b.lightMode = lightMode;
        return b;
    }

    public static final class Builder {
        private int operation = Operation.UNIVERSAL;
        private int mode = Mode.WORLD;
        private boolean snapTranslation = false;
        private float snapTranslationStep = 0.25f;
        private boolean snapRotation = false;
        private float snapRotationDeg = 15f;
        private boolean snapScale = false;
        private float snapScaleStep = 0.1f;
        private float[] localBounds = null;
        private float[] boundsSnap = null;
        private float gridSize = 1f;
        private boolean showGrid = true;
        private boolean showViewCube = true;
        private float viewCubeArmLength = 8f;
        private boolean orthographic = false;
        private boolean lockTransform = false;
        private boolean lockView = false;
        private boolean lightMode = false;

        public Builder withOperation(int op) { operation = op; return this; }
        public Builder withMode(int m) { mode = m; return this; }
        public Builder withSnapTranslation(boolean on, float step) { snapTranslation = on; snapTranslationStep = step; return this; }
        public Builder withSnapRotation(boolean on, float deg) { snapRotation = on; snapRotationDeg = deg; return this; }
        public Builder withSnapScale(boolean on, float step) { snapScale = on; snapScaleStep = step; return this; }
        public Builder withLocalBounds(float[] bounds) { localBounds = bounds; return this; }
        public Builder withBoundsSnap(float[] snap) { boundsSnap = snap; return this; }
        public Builder withGridSize(float s) { gridSize = s; return this; }
        public Builder withShowGrid(boolean v) { showGrid = v; return this; }
        public Builder withShowViewCube(boolean v) { showViewCube = v; return this; }
        public Builder withViewCubeArmLength(float l) { viewCubeArmLength = l; return this; }
        public Builder withOrthographic(boolean v) { orthographic = v; return this; }
        public Builder withLockTransform(boolean v) { lockTransform = v; return this; }
        public Builder withLockView(boolean v) { lockView = v; return this; }
        public Builder withLightMode(boolean v) { lightMode = v; return this; }

        public ImGuiGizmoConfig build() { return new ImGuiGizmoConfig(this); }
    }
}
