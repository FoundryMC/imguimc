package foundry.imgui.api.text.color;

import foundry.imgui.api.text.editor.EditorGlyph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractBaseColorizer implements IEditorColorizer {
    private final Map<Integer, CacheEntry> cache = new HashMap<>();
    private int version = 0;
    protected boolean needsFullAnalysis = true;

    @Override
    public void invalidateAll() {
        cache.clear();
        needsFullAnalysis = true;
        version++;
    }

    @Override
    public void markLineDirty(int idx) {
        cache.remove(idx);
    }

    @Override
    public void markLinesDirty(int start, int end) {
        for (int i = start; i <= end; i++) cache.remove(i);
    }

    @Override
    public void colorizeVisibleLines(List<List<EditorGlyph>> lines, int first, int last) {
        if (needsFullAnalysis) {
            analyzeDocument(lines);
            needsFullAnalysis = false;
        }
        for (int i = first; i <= last && i < lines.size(); i++) {
            colorizeLineIfNeeded(lines, i);
        }
    }

    @Override
    public void colorizeLine(List<List<EditorGlyph>> lines, int idx) {
        if (idx < 0 || idx >= lines.size()) return;
        cache.remove(idx);
        if (needsFullAnalysis) {
            analyzeDocument(lines);
            needsFullAnalysis = false;
        }
        colorizeLineIfNeeded(lines, idx);
    }

    private void colorizeLineIfNeeded(List<List<EditorGlyph>> lines, int idx) {
        List<EditorGlyph> line = lines.get(idx);
        String text = glyphsToString(line);
        CacheEntry entry = cache.get(idx);
        if (entry != null && entry.text.equals(text) && entry.version == version) return;
        colorizeLineImpl(line, idx, text);
        cache.put(idx, new CacheEntry(text, version));
    }

    protected abstract void colorizeLineImpl(List<EditorGlyph> line, int lineIdx, String text);

    // Scan the whole document for user-defined types/functions. Runs once per invalidation.
    protected abstract void analyzeDocument(List<List<EditorGlyph>> lines);

    protected static String glyphsToString(List<EditorGlyph> glyphs) {
        StringBuilder sb = new StringBuilder(glyphs.size());
        for (EditorGlyph g : glyphs) sb.append(g.ch);
        return sb.toString();
    }

    // Returns the index of the first "//" not inside a string, or -1.
    protected static int findLineCommentStart(String text) {
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) inString = !inString;
            if (!inString && c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '/') return i;
        }
        return -1;
    }

    public void colorize(List<List<EditorGlyph>> lines) {
        invalidateAll();
        analyzeDocument(lines);
        needsFullAnalysis = false;
        for (int i = 0; i < lines.size(); i++) colorizeLineIfNeeded(lines, i);
    }

    private static final class CacheEntry {
        final String text;
        final int version;
        CacheEntry(String text, int version) { this.text = text; this.version = version; }
    }
}