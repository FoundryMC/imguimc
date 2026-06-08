package foundry.imgui.api.text.color;

import foundry.imgui.api.text.editor.EditorGlyph;

import java.util.List;

public interface IEditorColorizer {
    // Only called for the visible range each frame; implementations should cache.
    void colorizeVisibleLines(List<List<EditorGlyph>> lines, int firstVisibleLine, int lastVisibleLine);

    void markLineDirty(int lineIdx);
    void markLinesDirty(int startLine, int endLine);

    // Nuke all cached state after setText.
    void invalidateAll();

    // Immediate single-line recolor used after autocomplete inserts text.
    void colorizeLine(List<List<EditorGlyph>> lines, int lineIdx);

    int getDefaultColor();
}