package foundry.imgui.api.text.preset;

import foundry.imgui.api.text.ImGuiCoreTextEditor;
import foundry.imgui.api.text.autocomplete.IAutocompleteProvider;
import foundry.imgui.api.text.color.IEditorColorizer;
import foundry.imgui.api.text.editor.EditorTheme;
import foundry.imgui.api.text.preset.glsl.GLSLAutocompleteProvider;
import foundry.imgui.api.text.preset.glsl.GLSLColorizer;
import foundry.imgui.api.text.preset.hlsl.HLSLAutocompleteProvider;
import foundry.imgui.api.text.preset.hlsl.HLSLColorizer;
import foundry.imgui.api.text.preset.msl.MSLAutocompleteProvider;
import foundry.imgui.api.text.preset.msl.MSLColorizer;

public final class ShaderTextEditor {
    public enum Language { GLSL, HLSL, MSL }

    private final IEditorColorizer colorizer;
    private final IAutocompleteProvider provider;
    private final ImGuiCoreTextEditor core;
    private final Language language;

    public ShaderTextEditor(Language language) {
        this(language, EditorTheme.dark().build());
    }

    public ShaderTextEditor(Language language, EditorTheme theme) {
        this.language = language;

        switch (language) {
            case HLSL: {
                HLSLColorizer c = new HLSLColorizer();
                colorizer = c;
                provider = new HLSLAutocompleteProvider(c);
                break;
            }
            case MSL: {
                MSLColorizer c = new MSLColorizer();
                colorizer = c;
                provider = new MSLAutocompleteProvider(c);
                break;
            }
            case GLSL:
            default: {
                GLSLColorizer c = new GLSLColorizer();
                colorizer = c;
                provider = new GLSLAutocompleteProvider(c);
                break;
            }
        }

        core = new ImGuiCoreTextEditor(colorizer, provider, theme);
    }

    public void render(String id, float width, float height, boolean isResizing) {
        core.render(id, width, height, isResizing);
    }

    public void setText(String text) { core.setText(text); }
    public String getText() { return core.getText(); }
    public int getTotalLines() { return core.getTotalLines(); }
    public void setReadOnly(boolean ro) { core.setReadOnly(ro); }
    public boolean isReadOnly() { return core.isReadOnly(); }

    public Language getLanguage() { return language; }
    public IEditorColorizer getColorizer() { return colorizer; }
    public ImGuiCoreTextEditor getCore() { return core; }
}