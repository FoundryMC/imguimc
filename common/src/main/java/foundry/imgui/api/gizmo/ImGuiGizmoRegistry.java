package foundry.imgui.api.gizmo;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ImGuiGizmoRegistry {

    private static ImGuiGizmoRegistry INSTANCE;

    private final Map<String, ImGuiGizmoScreen> screens = new LinkedHashMap<>();

    private ImGuiGizmoRegistry() {}

    public static ImGuiGizmoRegistry getInstance() {
        if (INSTANCE == null) INSTANCE = new ImGuiGizmoRegistry();
        return INSTANCE;
    }

    public ImGuiGizmoRegistry register(String name, ImGuiGizmoScreen screen) {
        screens.put(name, screen);
        return this;
    }

    public ImGuiGizmoScreen getOrCreate(String name) {
        return screens.computeIfAbsent(name, k ->
                new ImGuiGizmoScreen(k)
                        .setConfig(ImGuiGizmoConfig.universal().build())
                        .setTheme(ImGuiGizmoTheme.dark().build()));
    }

    public ImGuiGizmoScreen get(String name) { return screens.get(name); }
    public void remove(String name) { screens.remove(name); }
    public Collection<ImGuiGizmoScreen> all() { return Collections.unmodifiableCollection(screens.values()); }
    public void clear() { screens.clear(); }
}
