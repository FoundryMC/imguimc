package foundry.imgui.api.text.editor;

import java.util.ArrayList;
import java.util.List;

public final class EditorState {
    public static final int MAX_UNDO = 200;

    public final List<List<EditorGlyph>> lines;
    public final EditorCoordinates cursor;
    public final EditorCoordinates selStart;
    public final EditorCoordinates selEnd;

    public EditorState(List<List<EditorGlyph>> lines,
                       EditorCoordinates cursor,
                       EditorCoordinates selStart,
                       EditorCoordinates selEnd) {
        this.lines = deepCopyLines(lines);
        this.cursor = cursor.copy();
        this.selStart = selStart.copy();
        this.selEnd = selEnd.copy();
    }

    public static List<List<EditorGlyph>> deepCopyLines(List<List<EditorGlyph>> src) {
        List<List<EditorGlyph>> copy = new ArrayList<>(src.size());
        for (List<EditorGlyph> line : src) {
            List<EditorGlyph> lc = new ArrayList<>(line.size());
            for (EditorGlyph g : line) lc.add(g.copy());
            copy.add(lc);
        }
        return copy;
    }
}