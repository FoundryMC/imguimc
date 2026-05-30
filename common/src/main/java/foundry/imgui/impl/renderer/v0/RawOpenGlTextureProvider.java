package foundry.imgui.impl.renderer.v0;

//? if <1.21.6 {

import foundry.imgui.api.ImGuiTextureProvider;

public record RawOpenGlTextureProvider(int id) implements ImGuiTextureProvider {

    @Override
    public long imguimc$id() {
        return this.id;
    }
}
//? }
