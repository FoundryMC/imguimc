package foundry.imgui.api.gizmo;

import imgui.ImGui;
import imgui.extension.imguizmo.ImGuizmo;
import org.joml.Matrix4f;

public final class ImGuiGizmoCamera {

    private float yaw = 45f;
    private float pitch = 30f;
    private float distance = 5f;
    private float minDistance = 0.1f;
    private float maxDistance = 500f;

    private float fovDeg = 45f;
    private float nearPlane = 0.1f;
    private float farPlane = 500f;
    private boolean orthographic = false;
    private float orthoScale = 5f;

    private boolean isDraggingOrbit = false;
    private float lastMouseX = 0f;
    private float lastMouseY = 0f;

    private float orbitSensitivity = 0.5f;
    private float zoomSensitivity = 0.5f;

    private boolean locked = false;

    public void handleInput(boolean viewLocked) {
        if (viewLocked || locked || ImGuizmo.isOver() || ImGuizmo.isUsing() || !ImGui.isWindowHovered()) {
            isDraggingOrbit = false;
            return;
        }

        float wheel = ImGui.getIO().getMouseWheel();
        if (wheel != 0f) {
            if (orthographic) {
                orthoScale = Math.max(0.1f, orthoScale - wheel * zoomSensitivity);
            } else {
                distance = Math.max(minDistance, Math.min(maxDistance, distance - wheel * zoomSensitivity));
            }
        }

        float mx = ImGui.getIO().getMousePosX();
        float my = ImGui.getIO().getMousePosY();

        if (ImGui.isMouseDown(1)) {
            if (!isDraggingOrbit) {
                isDraggingOrbit = true;
                lastMouseX = mx;
                lastMouseY = my;
            } else {
                yaw += (mx - lastMouseX) * orbitSensitivity;
                pitch = Math.max(-89f, Math.min(89f, pitch - (my - lastMouseY) * orbitSensitivity));
                lastMouseX = mx;
                lastMouseY = my;
            }
        } else {
            isDraggingOrbit = false;
        }
    }

    public void handleInput() { handleInput(false); }

    public Matrix4f buildViewMatrix(Matrix4f dst) {
        return dst.identity()
                .translate(0f, 0f, -distance)
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw));
    }

    public Matrix4f buildProjectionMatrix(Matrix4f dst, float viewW, float viewH) {
        float aspect = (viewH > 0f) ? viewW / viewH : 1f;
        if (orthographic) {
            float halfH = orthoScale;
            float halfW = halfH * aspect;
            return dst.identity().ortho(-halfW, halfW, -halfH, halfH, nearPlane, farPlane);
        }
        return dst.identity().perspective((float) Math.toRadians(fovDeg), aspect, nearPlane, farPlane);
    }

    public void syncFromViewMatrix(Matrix4f view) {
        float newPitch = (float) Math.toDegrees(Math.asin(Math.max(-1f, Math.min(1f, view.m12()))));
        float newYaw = (float) Math.toDegrees(Math.atan2(-view.m02(), view.m00()));
        float newDist = -view.m23();

        if (Float.isFinite(newPitch) && Float.isFinite(newYaw)) {
            pitch = Math.max(-89f, Math.min(89f, newPitch));
            yaw = newYaw;
        }
        if (newDist > minDistance && newDist < maxDistance) {
            distance = newDist;
        }
    }

    public void reset() {
        yaw = 45f;
        pitch = 30f;
        distance = 5f;
        isDraggingOrbit = false;
    }

    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getDistance() { return distance; }
    public boolean isOrthographic() { return orthographic; }
    public boolean isLocked() { return locked; }

    public ImGuiGizmoCamera setYaw(float v) { yaw = v; return this; }
    public ImGuiGizmoCamera setPitch(float v) { pitch = Math.max(-89f, Math.min(89f, v)); return this; }
    public ImGuiGizmoCamera setDistance(float v) { distance = Math.max(minDistance, Math.min(maxDistance, v)); return this; }
    public ImGuiGizmoCamera setFovDeg(float v) { fovDeg = v; return this; }
    public ImGuiGizmoCamera setNearFar(float n, float f) { nearPlane = n; farPlane = f; return this; }
    public ImGuiGizmoCamera setOrthographic(boolean v) { orthographic = v; return this; }
    public ImGuiGizmoCamera setOrthoScale(float v) { orthoScale = v; return this; }
    public ImGuiGizmoCamera setDistanceLimits(float mn, float mx) { minDistance = mn; maxDistance = mx; return this; }
    public ImGuiGizmoCamera setOrbitSensitivity(float v) { orbitSensitivity = v; return this; }
    public ImGuiGizmoCamera setZoomSensitivity(float v) { zoomSensitivity = v; return this; }
    public ImGuiGizmoCamera setLocked(boolean v) { locked = v; return this; }
}
