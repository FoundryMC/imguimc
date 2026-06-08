package foundry.imgui.api.text.editor;

import foundry.imgui.api.text.ImGuiCoreTextEditor;
import foundry.imgui.api.text.autocomplete.AutocompleteItem;
import foundry.imgui.api.text.autocomplete.IAutocompleteProvider;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiKey;

import java.util.ArrayList;
import java.util.List;

// Autocomplete popup that floats on the foreground draw list.
// Hovering it never steals keyboard focus from the editor.
public final class EditorAutocomplete {
    private static final int MAX_VISIBLE = 8;
    private static final float POPUP_WIDTH = 360f;
    private static final float ITEM_HEIGHT = 22f;
    private static final float ICON_COL_W = 26f;

    // ABGR
    private static final int COL_BG = 0xF0191919;
    private static final int COL_BORDER = 0xFF3A3A3A;
    private static final int COL_SELECTED = 0xFF2A5A8A;
    private static final int COL_HOVERED = 0xFF244C74;
    private static final int COL_TEXT = 0xFFD4D4D4;
    private static final int COL_TYPE = 0xFF707070;
    private static final int COL_SCROLLBG = 0x40FFFFFF;
    private static final int COL_SCROLLFG = 0x80FFFFFF;

    private boolean showPopup = false;
    private final List<AutocompleteItem> items = new ArrayList<>();
    private int selectedIdx = 0;
    private int scrollOffset = 0;
    private String prefix = "";

    private float editorPanelTop = 0f;
    private float editorPanelBottom = Float.MAX_VALUE;

    private final IAutocompleteProvider provider;

    public EditorAutocomplete(IAutocompleteProvider provider) {
        this.provider = provider;
    }

    public boolean isVisible() {
        return showPopup && !items.isEmpty();
    }

    public void hide() {
        showPopup = false;
        items.clear();
        selectedIdx = 0;
        scrollOffset = 0;
        prefix = "";
    }

    public void update(EditorCoordinates cursor, List<List<EditorGlyph>> lines) {
        String word = currentWord(cursor, lines);
        if (word.length() < provider.minPrefixLength()) { hide(); return; }
        if (provider.shouldSuppress(word, lines, cursor)) { hide(); return; }

        List<AutocompleteItem> candidates = provider.getCandidates(word, lines, cursor);
        if (candidates.isEmpty()) { hide(); return; }

        prefix = word;
        items.clear();
        items.addAll(candidates);
        selectedIdx = 0;
        scrollOffset = 0;
        showPopup = true;
    }

    // Returns true if a key was consumed and should not be forwarded to the editor.
    public boolean handleKeyboard(EditorCoordinates cursor,
                                  List<List<EditorGlyph>> lines,
                                  ImGuiCoreTextEditor editor) {
        if (!showPopup || items.isEmpty()) return false;

        if (ImGui.isKeyPressed(ImGuiKey.DownArrow)) {
            selectedIdx = Math.min(selectedIdx + 1, items.size() - 1);
            clampScroll();
            return true;
        }
        if (ImGui.isKeyPressed(ImGuiKey.UpArrow)) {
            selectedIdx = Math.max(selectedIdx - 1, 0);
            clampScroll();
            return true;
        }
        if (ImGui.isKeyPressed(ImGuiKey.Tab) || ImGui.isKeyPressed(ImGuiKey.Enter)) {
            accept(cursor, lines, editor);
            return true;
        }
        if (ImGui.isKeyPressed(ImGuiKey.Escape)) {
            hide();
            return true;
        }
        return false;
    }

    // Returns true when the mouse is inside the popup (suppresses editor scroll passthrough).
    public boolean isMouseOver(float charWidth, float lineHeight,
                               ImVec2 editorPos, EditorCoordinates cursor,
                               float textStart, float scrollY, float panelHeight) {
        if (!showPopup || items.isEmpty()) return false;
        editorPanelTop = editorPos.y;
        editorPanelBottom = editorPos.y + panelHeight;
        float[] rect = popupRect(charWidth, lineHeight, editorPos, cursor, textStart, scrollY);
        ImVec2 mp = ImGui.getMousePos();
        return mp.x >= rect[0] && mp.x <= rect[2] && mp.y >= rect[1] && mp.y <= rect[3];
    }

    // Returns true if an item was accepted by click.
    public boolean render(float charWidth, float lineHeight,
                          ImVec2 editorPos, EditorCoordinates cursor,
                          float textStart, float scrollY, float panelHeight,
                          List<List<EditorGlyph>> lines,
                          ImGuiCoreTextEditor editor) {
        if (!showPopup || items.isEmpty()) return false;

        editorPanelTop = editorPos.y;
        editorPanelBottom = editorPos.y + panelHeight;

        float cursorScreenY = editorPos.y - scrollY + cursor.line * lineHeight;
        if (cursorScreenY + lineHeight < editorPanelTop || cursorScreenY > editorPanelBottom) return false;

        float[] rect = popupRect(charWidth, lineHeight, editorPos, cursor, textStart, scrollY);
        float x = rect[0], y = rect[1], x2 = rect[2], y2 = rect[3];
        float totalHeight = y2 - y;

        ImDrawList dl = ImGui.getForegroundDrawList();
        dl.addRectFilled(x, y, x2, y2, COL_BG, 5f);
        dl.addRect(x, y, x2, y2, COL_BORDER, 5f, 0, 1.5f);

        ImVec2 mp = ImGui.getMousePos();
        boolean mouseIn = mp.x >= x && mp.x <= x2 && mp.y >= y && mp.y <= y2;

        if (mouseIn && items.size() > MAX_VISIBLE) {
            float wheel = ImGui.getIO().getMouseWheel();
            if (wheel != 0) {
                scrollOffset -= (int) wheel;
                scrollOffset = clamp(scrollOffset, 0, items.size() - MAX_VISIBLE);
            }
        }
        clampScroll();

        boolean accepted = false;
        float fontSize = ImGui.getFontSize();
        int visCount = Math.min(items.size(), MAX_VISIBLE);

        for (int i = 0; i < visCount; i++) {
            int idx = scrollOffset + i;
            if (idx >= items.size()) break;

            AutocompleteItem item = items.get(idx);
            float iy = y + 2 + i * ITEM_HEIGHT;
            boolean hovering = mouseIn && mp.y >= iy && mp.y < iy + ITEM_HEIGHT;

            if (hovering) {
                selectedIdx = idx;
                dl.addRectFilled(x + 2, iy, x2 - 2, iy + ITEM_HEIGHT, COL_HOVERED, 3f);
                if (ImGui.isMouseClicked(0)) {
                    accept(cursor, lines, editor);
                    accepted = true;
                }
            } else if (idx == selectedIdx) {
                dl.addRectFilled(x + 2, iy, x2 - 2, iy + ITEM_HEIGHT, COL_SELECTED, 3f);
            }

            float tx = x + 8;
            float ty = iy + (ITEM_HEIGHT - fontSize) * 0.5f;

            dl.addText(tx, ty, item.color, typeIcon(item.type));
            tx += ICON_COL_W;
            dl.addText(tx, ty, COL_TEXT, item.text);

            // Pull tag left when scrollbar is visible to avoid overlap.
            String tag = item.type;
            float tagWidth = ImGui.getFont().calcTextSizeA(fontSize * 0.85f, Float.MAX_VALUE, -1, tag).x;
            float tagRightPad = items.size() > MAX_VISIBLE ? 18f : 8f;
            dl.addText(x2 - tagWidth - tagRightPad, ty, COL_TYPE, tag);
        }

        if (items.size() > MAX_VISIBLE) {
            float sbX = x2 - 6;
            float sbH = totalHeight - 4;
            float thumbH = (float) MAX_VISIBLE / items.size() * sbH;
            float maxOff = items.size() - MAX_VISIBLE;
            float thumbY = y + 2 + (scrollOffset / maxOff) * (sbH - thumbH);
            dl.addRectFilled(sbX, y + 2, sbX + 4, y + totalHeight - 2, COL_SCROLLBG, 2f);
            dl.addRectFilled(sbX, thumbY, sbX + 4, thumbY + thumbH, COL_SCROLLFG, 2f);
        }

        return accepted;
    }

    private void accept(EditorCoordinates cursor, List<List<EditorGlyph>> lines, ImGuiCoreTextEditor editor) {
        if (items.isEmpty() || selectedIdx >= items.size()) { hide(); return; }

        editor.pushUndo();

        AutocompleteItem sel = items.get(selectedIdx);
        List<EditorGlyph> line = lines.get(cursor.line);

        for (int i = 0; i < prefix.length() && cursor.column > 0; i++) {
            line.remove(cursor.column - 1);
            cursor.column--;
        }

        String text = sel.text;
        boolean addParens = provider.appendParens() && sel.type.equals("function");
        if (addParens) text += "()";

        int defColor = editor.getColorizer().getDefaultColor();
        for (char c : text.toCharArray()) {
            line.add(cursor.column++, new EditorGlyph(c, defColor));
        }

        if (addParens) cursor.column--; // land inside the parens

        editor.resetBlink();
        hide();
    }

    private float[] popupRect(float charWidth, float lineHeight,
                              ImVec2 editorPos, EditorCoordinates cursor,
                              float textStart, float scrollY) {
        float cx = editorPos.x + textStart + cursor.column * charWidth;
        float cy = editorPos.y - scrollY + cursor.line * lineHeight;

        int vis = Math.min(items.size(), MAX_VISIBLE);
        float h = vis * ITEM_HEIGHT + 4;
        float w = POPUP_WIDTH;

        ImVec2 ds = ImGui.getIO().getDisplaySize();

        float x = cx;
        float yBelow = cy + lineHeight;
        float yAbove = cy - h;

        if (x + w > ds.x - 4) x = ds.x - w - 4;
        if (x < 4) x = 4;

        float y;
        float spaceBelow = editorPanelBottom - yBelow;
        float spaceAbove = yAbove - editorPanelTop;

        if (spaceBelow >= h) y = yBelow;
        else if (spaceAbove >= h) y = yAbove;
        else if (spaceBelow >= spaceAbove) y = yBelow;
        else y = yAbove;

        if (y + h > ds.y - 4) y = ds.y - h - 4;
        if (y < 4) y = 4;

        return new float[]{x, y, x + w, y + h};
    }

    private void clampScroll() {
        if (selectedIdx < scrollOffset)
            scrollOffset = selectedIdx;
        else if (selectedIdx >= scrollOffset + MAX_VISIBLE)
            scrollOffset = selectedIdx - MAX_VISIBLE + 1;
        scrollOffset = clamp(scrollOffset, 0, Math.max(0, items.size() - MAX_VISIBLE));
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static String typeIcon(String type) {
        switch (type) {
            case "keyword":   return "K";
            case "type":      return "T";
            case "function":  return "F";
            case "variable":  return "V";
            case "constant":  return "C";
            case "qualifier": return "Q";
            default:          return ".";
        }
    }

    static String currentWord(EditorCoordinates cursor, List<List<EditorGlyph>> lines) {
        if (cursor.line >= lines.size()) return "";
        List<EditorGlyph> line = lines.get(cursor.line);
        int end = cursor.column;
        int start = end;
        while (start > 0) {
            char c = line.get(start - 1).ch;
            if (!Character.isLetterOrDigit(c) && c != '_') break;
            start--;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end && i < line.size(); i++) sb.append(line.get(i).ch);
        return sb.toString();
    }
}