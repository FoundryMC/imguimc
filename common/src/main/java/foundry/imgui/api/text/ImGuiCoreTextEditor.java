package foundry.imgui.api.text;

import foundry.imgui.api.text.autocomplete.IAutocompleteProvider;
import foundry.imgui.api.text.color.IEditorColorizer;
import foundry.imgui.api.text.editor.EditorAutocomplete;
import foundry.imgui.api.text.editor.EditorCoordinates;
import foundry.imgui.api.text.editor.EditorGlyph;
import foundry.imgui.api.text.editor.EditorState;
import foundry.imgui.api.text.editor.EditorTheme;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
// GHO__ST
public final class ImGuiCoreTextEditor {

    private final List<List<EditorGlyph>> lines = new ArrayList<>();

    private final EditorCoordinates cursor = new EditorCoordinates(0, 0);
    private final EditorCoordinates selStart = new EditorCoordinates(0, 0);
    private final EditorCoordinates selEnd = new EditorCoordinates(0, 0);
    private final EditorCoordinates dragAnchor = new EditorCoordinates(0, 0);
    private final EditorCoordinates selectionAnchor = new EditorCoordinates(0, 0);

    private int preferredColumn = 0;
    private boolean usePreferredColumn = false;
    private boolean readOnly = false;
    private boolean isDragging = false;

    private final List<EditorState> undoStack = new ArrayList<>();
    private final List<EditorState> redoStack = new ArrayList<>();

    private float textStart = 60f;
    private ImVec2 contentOrigin = new ImVec2(0, 0);
    private ImVec2 drawCursorPos = new ImVec2(0, 0);
    private boolean drawCursorPosReady = false;
    private float maxLineWidth = 0f;
    private long blinkEpoch = System.currentTimeMillis();
    private float editorScrollY = 0f;

    private final IEditorColorizer colorizer;
    private final EditorAutocomplete autocomplete;
    private final EditorTheme theme;

    private final List<Integer> pendingChars = new ArrayList<>();

    private long lastClickTime = 0;
    private int clickCount = 0;
    private final EditorCoordinates lastClickPos = new EditorCoordinates(-1, -1);

    public ImGuiCoreTextEditor(IEditorColorizer colorizer, IAutocompleteProvider provider, EditorTheme theme) {
        this.colorizer = colorizer != null ? colorizer : new NullColorizer();
        this.autocomplete = provider != null ? new EditorAutocomplete(provider) : null;
        this.theme = theme;
        lines.add(new ArrayList<>());
    }

    public void setText(String text) {
        lines.clear();
        lines.add(new ArrayList<>());
        int defColor = colorizer.getDefaultColor();
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                lines.add(new ArrayList<>());
            } else if (c != '\r') {
                lines.get(lines.size() - 1).add(new EditorGlyph(c, defColor));
            }
        }
        cursor.set(0, 0);
        clearSelection();
        colorizer.invalidateAll();
        undoStack.clear();
        redoStack.clear();
        maxLineWidth = 0f;
        if (autocomplete != null) autocomplete.hide();
    }

    public String getText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            for (EditorGlyph g : lines.get(i)) sb.append(g.ch);
            if (i < lines.size() - 1) sb.append('\n');
        }
        return sb.toString();
    }

    public int getTotalLines() { return lines.size(); }
    public void setReadOnly(boolean ro) { this.readOnly = ro; }
    public boolean isReadOnly() { return readOnly; }
    public IEditorColorizer getColorizer() { return colorizer; }
    public EditorTheme getTheme() { return theme; }

    public void resetBlink() { blinkEpoch = System.currentTimeMillis(); }

    // Feed typed characters from a GLFW charCallback or equivalent.
    public void onCharTyped(int codepoint) {
        pendingChars.add(codepoint);
    }

    public void render(String id, float width, float height, boolean isResizing) {
        float fontSize = ImGui.getFontSize();
        float charWidth = ImGui.getFont().calcTextSizeA(fontSize, Float.MAX_VALUE, -1, "M").x;
        float lineHeight = fontSize * theme.lineSpacing;

        boolean mouseOverAC = autocomplete != null
                && autocomplete.isMouseOver(charWidth, lineHeight, contentOrigin, cursor, textStart, editorScrollY, height);

        int flags = ImGuiWindowFlags.NoMove | ImGuiWindowFlags.HorizontalScrollbar;
        if (mouseOverAC) flags |= ImGuiWindowFlags.NoScrollWithMouse;

        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0, 0, 0, 0);
        ImGui.beginChild(id, width, height, false, flags);
        ImGui.popStyleColor();

        contentOrigin = ImGui.getWindowPos();

        boolean focused = ImGui.isWindowFocused();
        boolean hovered = ImGui.isWindowHovered(imgui.flag.ImGuiHoveredFlags.ChildWindows);

        if (autocomplete != null) {
            if (!focused) {
                autocomplete.hide();
            } else if (!hovered && ImGui.isMouseClicked(0)) {
                autocomplete.hide();
            }
        }

        boolean canInput = focused && !isResizing && isCursorToggled();
        if (canInput) handleKeyboard(charWidth, lineHeight);

        renderContent(charWidth, lineHeight, width, height, focused, hovered, mouseOverAC, isResizing, canInput);

        ImGui.endChild();

        // Autocomplete drawn outside the child window so it floats on top of everything.
        if (autocomplete != null && autocomplete.isVisible() && isCursorToggled()) {
            boolean clicked = autocomplete.render(charWidth, lineHeight, contentOrigin, cursor, textStart, editorScrollY, height, lines, this);
            if (clicked) {
                int lineIdx = cursor.line;
                if (lineIdx >= 0 && lineIdx < lines.size()) colorizer.colorizeLine(lines, lineIdx);
                autocomplete.hide();
            }
        }
    }

    private void renderContent(float charWidth, float lineHeight,
                               float width, float height,
                               boolean focused, boolean hovered,
                               boolean mouseOverAC, boolean isResizing,
                               boolean canInput) {
        ImDrawList dl = ImGui.getWindowDrawList();
        ImVec2 origin = ImGui.getCursorScreenPos();
        float scrollY = ImGui.getScrollY();
        float scrollX = ImGui.getScrollX();

        drawCursorPos = origin;
        drawCursorPosReady = true;
        editorScrollY = scrollY;

        int firstLine = Math.max(0, (int)(scrollY / lineHeight) - 1);
        int lastLine = Math.min(lines.size() - 1, firstLine + (int)(height / lineHeight) + 2);

        String maxNumStr = " " + lines.size() + " ";
        textStart = ImGui.getFont().calcTextSizeA(ImGui.getFontSize(), Float.MAX_VALUE, -1, maxNumStr).x
                + theme.gutterPaddingRight;

        dl.addRectFilled(contentOrigin.x, contentOrigin.y,
                contentOrigin.x + width,
                contentOrigin.y + height,
                theme.backgroundColor);

        dl.addLine(origin.x + textStart - 4, origin.y - scrollY,
                origin.x + textStart - 4,
                origin.y - scrollY + lines.size() * lineHeight,
                theme.gutterSeparatorColor, 1f);

        colorizer.colorizeVisibleLines(lines, firstLine, lastLine);

        float newMaxWidth = 0f;
        EditorCoordinates normStart = normSelStart();
        EditorCoordinates normEnd = normSelEnd();

        for (int li = firstLine; li <= lastLine; li++) {
            float lineY = origin.y + li * lineHeight;
            List<EditorGlyph> line = lines.get(li);

            if (li == cursor.line && !hasSelection()) {
                dl.addRectFilled(origin.x - scrollX, lineY,
                        origin.x - scrollX + Math.max(width, maxLineWidth + textStart + 20),
                        lineY + lineHeight, theme.currentLineColor);
            }

            if (hasSelection() && li >= normStart.line && li <= normEnd.line) {
                float sx = (li == normStart.line)
                        ? contentOrigin.x + textStart - scrollX + colX(li, normStart.column, charWidth)
                        : contentOrigin.x;
                float ex = (li == normEnd.line)
                        ? contentOrigin.x + textStart - scrollX + colX(li, normEnd.column, charWidth)
                        : contentOrigin.x + Math.max(width, maxLineWidth + textStart + 20);
                dl.addRectFilled(sx, lineY, ex, lineY + lineHeight, theme.selectionColor);
            }

            String numStr = String.valueOf(li + 1);
            float numW = ImGui.getFont().calcTextSizeA(ImGui.getFontSize(), Float.MAX_VALUE, -1, numStr).x;
            dl.addText(origin.x + textStart - numW - theme.gutterPaddingRight - 2,
                    lineY, theme.lineNumberColor, numStr);

            float x = 0;
            for (EditorGlyph g : line) {
                if (g.ch == '\t') {
                    x = nextTabStop(x, charWidth, theme.tabSize);
                } else {
                    float gx = origin.x + textStart + x;
                    if (gx + charWidth >= origin.x + textStart - scrollX
                            && gx <= origin.x + textStart + width + scrollX) {
                        dl.addText(gx, lineY, g.color, String.valueOf(g.ch));
                    }
                    x += charWidth;
                }
            }
            newMaxWidth = Math.max(newMaxWidth, x);

            if (li == cursor.line && focused && !readOnly) {
                long now = System.currentTimeMillis();
                boolean cursorVisible = theme.cursorBlinkMs == 0
                        || ((now - blinkEpoch) % theme.cursorBlinkMs) < (theme.cursorBlinkMs / 2);
                if (cursorVisible) {
                    float cx = origin.x + textStart + colX(li, cursor.column, charWidth);
                    dl.addRectFilled(cx, lineY, cx + theme.cursorWidth, lineY + lineHeight, theme.cursorColor);
                }
            }
        }

        maxLineWidth = Math.max(maxLineWidth, newMaxWidth);

        // +100 so the cursor never sits flush against the scroll edge
        ImGui.dummy(textStart + maxLineWidth + 100f, lines.size() * lineHeight);

        if (!isResizing && canInput) handleMouse(charWidth, lineHeight, hovered, mouseOverAC);
    }

    private void handleKeyboard(float charWidth, float lineHeight) {
        boolean ctrl = ImGui.getIO().getKeyCtrl();
        boolean shift = ImGui.getIO().getKeyShift();

        if (autocomplete != null && autocomplete.handleKeyboard(cursor, lines, this)) return;

        if (ImGui.isKeyPressed(ImGuiKey.LeftArrow)) { if (ctrl) wordLeft(shift); else moveLeft(shift); updateAutocomplete(); return; }
        if (ImGui.isKeyPressed(ImGuiKey.RightArrow)) { if (ctrl) wordRight(shift); else moveRight(shift); updateAutocomplete(); return; }
        if (ImGui.isKeyPressed(ImGuiKey.UpArrow)) { moveUp(shift); updateAutocomplete(); return; }
        if (ImGui.isKeyPressed(ImGuiKey.DownArrow)) { moveDown(shift); updateAutocomplete(); return; }
        if (ImGui.isKeyPressed(ImGuiKey.Home)) { if (ctrl) moveDocHome(shift); else moveLineHome(shift); updateAutocomplete(); return; }
        if (ImGui.isKeyPressed(ImGuiKey.End)) { if (ctrl) moveDocEnd(shift); else moveLineEnd(shift); updateAutocomplete(); return; }
        if (ImGui.isKeyPressed(ImGuiKey.PageUp)) { movePageUp(shift, lineHeight); updateAutocomplete(); return; }
        if (ImGui.isKeyPressed(ImGuiKey.PageDown)) { movePageDown(shift, lineHeight); updateAutocomplete(); return; }

        if (ctrl) {
            if (ImGui.isKeyPressed(ImGuiKey.A)) { selectAll(); return; }
            if (ImGui.isKeyPressed(ImGuiKey.C)) { copy(); return; }
            if (ImGui.isKeyPressed(ImGuiKey.X) && !readOnly) { cut(); return; }
            if (ImGui.isKeyPressed(ImGuiKey.V) && !readOnly) { paste(); return; }
            if (ImGui.isKeyPressed(ImGuiKey.Z) && !readOnly) { undo(); return; }
            if (ImGui.isKeyPressed(ImGuiKey.Y) && !readOnly) { redo(); return; }
        }

        if (readOnly) return;

        if (ImGui.isKeyPressed(ImGuiKey.Enter)) {
            pushUndo(); insertNewline(); resetBlink(); updateAutocomplete(); return;
        }
        if (ImGui.isKeyPressed(ImGuiKey.Tab)) {
            pushUndo();
            if (shift) unindentSelection(); else indentSelection();
            resetBlink(); return;
        }
        if (ImGui.isKeyPressed(ImGuiKey.Backspace)) {
            pushUndo();
            if (hasSelection()) deleteSelection(); else backspace();
            resetBlink(); updateAutocomplete(); return;
        }
        if (ImGui.isKeyPressed(ImGuiKey.Delete)) {
            pushUndo();
            if (hasSelection()) deleteSelection(); else deleteForward();
            resetBlink(); updateAutocomplete(); return;
        }

        // Drain externally fed codepoints first, then fall back to polling ImGui key state.
        // The polling path lets the editor work without a wired GLFW charCallback.
        if (!readOnly && !ctrl) {
            boolean inserted = false;
            if (!pendingChars.isEmpty()) {
                pushUndo();
                if (hasSelection()) deleteSelection();
                for (int cp : pendingChars) {
                    char c = (char) cp;
                    if (c != 0 && !Character.isISOControl(c)) { insertChar(c); inserted = true; }
                }
                pendingChars.clear();
                if (inserted) { resetBlink(); updateAutocomplete(); }
                return;
            }
            for (char c = 32; c < 127; c++) {
                if (ImGui.isKeyPressed(c, false)) {
                    char toInsert;
                    if (c >= 'a' && c <= 'z') toInsert = shift ? (char)(c - 32) : c;
                    else if (c >= 'A' && c <= 'Z') toInsert = shift ? c : (char)(c + 32);
                    else toInsert = shift ? shiftChar(c) : c;
                    pushUndo();
                    if (hasSelection()) deleteSelection();
                    insertChar(toInsert);
                    resetBlink();
                    updateAutocomplete();
                    break;
                }
            }
        } else {
            pendingChars.clear();
        }
    }

    private void handleMouse(float charWidth, float lineHeight, boolean hovered, boolean mouseOverAC) {
        if (!hovered || mouseOverAC) return;

        if (ImGui.isMouseClicked(0)) {
            long now = System.currentTimeMillis();
            EditorCoordinates click = screenToCoords(charWidth, lineHeight);

            boolean sameSpot = click.equals(lastClickPos);
            if (sameSpot && now - lastClickTime < 500) clickCount++;
            else clickCount = 1;
            lastClickTime = now;
            lastClickPos.set(click);

            if (clickCount == 3) {
                cursor.set(click.line, 0);
                setRawSelection(new EditorCoordinates(click.line, 0),
                        new EditorCoordinates(click.line, lineLen(click.line)));
                if (autocomplete != null) autocomplete.hide();
            } else if (clickCount == 2) {
                cursor.set(click);
                selectWord();
                if (autocomplete != null) autocomplete.hide();
            } else {
                cursor.set(click);
                clearSelection();
                dragAnchor.set(click);
                if (autocomplete != null) autocomplete.hide();
            }
            resetBlink();
            isDragging = true;
        }

        if (isDragging && ImGui.isMouseDragging(0) && ImGui.isMouseDown(0) && clickCount <= 1) {
            EditorCoordinates drag = screenToCoords(charWidth, lineHeight);
            cursor.set(drag);
            setRawSelection(dragAnchor, cursor);
            resetBlink();
            if (autocomplete != null && hasSelection()) autocomplete.hide();
        }

        if (ImGui.isMouseReleased(0)) isDragging = false;
    }

    public void scrollToCursor(float charWidth, float lineHeight, float viewW, float viewH) {
        float lineY = cursor.line * lineHeight;
        float scrollY = ImGui.getScrollY();
        if (lineY < scrollY) ImGui.setScrollY(lineY);
        if (lineY + lineHeight > scrollY + viewH) ImGui.setScrollY(lineY + lineHeight - viewH);

        float colX_ = textStart + colX(cursor.line, cursor.column, charWidth);
        float scrollX = ImGui.getScrollX();
        if (colX_ < scrollX + textStart + 10) ImGui.setScrollX(Math.max(0, colX_ - textStart - 10));
        if (colX_ + charWidth > scrollX + viewW) ImGui.setScrollX(colX_ + charWidth - viewW + 20);
    }

    private void insertChar(char c) {
        if (readOnly) return;
        int defColor = colorizer.getDefaultColor();
        lines.get(cursor.line).add(cursor.column++, new EditorGlyph(c, defColor));
        colorizer.markLineDirty(cursor.line);
    }

    private void insertNewline() {
        List<EditorGlyph> cur = lines.get(cursor.line);
        List<EditorGlyph> tail = new ArrayList<>(cur.subList(cursor.column, cur.size()));
        cur.subList(cursor.column, cur.size()).clear();

        int defColor = colorizer.getDefaultColor();

        int baseIndent = 0;
        while (baseIndent < cur.size() && (cur.get(baseIndent).ch == ' ' || cur.get(baseIndent).ch == '\t'))
            baseIndent++;

        List<EditorGlyph> indentGlyphs = new ArrayList<>(baseIndent);
        for (int i = 0; i < baseIndent; i++)
            indentGlyphs.add(new EditorGlyph(cur.get(i).ch, defColor));

        for (int i = indentGlyphs.size() - 1; i >= 0; i--)
            tail.add(0, indentGlyphs.get(i));

        lines.add(cursor.line + 1, tail);
        cursor.line++;
        cursor.column = indentGlyphs.size();
        colorizer.markLinesDirty(cursor.line - 1, cursor.line);
        maxLineWidth = 0;
    }

    private void backspace() {
        if (cursor.column > 0) {
            lines.get(cursor.line).remove(--cursor.column);
            colorizer.markLineDirty(cursor.line);
        } else if (cursor.line > 0) {
            List<EditorGlyph> prev = lines.get(cursor.line - 1);
            List<EditorGlyph> cur = lines.remove(cursor.line);
            cursor.line--;
            cursor.column = prev.size();
            prev.addAll(cur);
            colorizer.markLinesDirty(cursor.line, cursor.line);
            maxLineWidth = 0;
        }
    }

    private void deleteForward() {
        if (cursor.column < lineLen(cursor.line)) {
            lines.get(cursor.line).remove(cursor.column);
            colorizer.markLineDirty(cursor.line);
        } else if (cursor.line < lines.size() - 1) {
            List<EditorGlyph> next = lines.remove(cursor.line + 1);
            lines.get(cursor.line).addAll(next);
            colorizer.markLineDirty(cursor.line);
            maxLineWidth = 0;
        }
    }

    private void deleteSelection() {
        if (!hasSelection()) return;
        EditorCoordinates s = normSelStart();
        EditorCoordinates e = normSelEnd();

        if (s.line == e.line) {
            lines.get(s.line).subList(s.column, e.column).clear();
            colorizer.markLineDirty(s.line);
        } else {
            List<EditorGlyph> firstLine = lines.get(s.line);
            List<EditorGlyph> lastLine = lines.get(e.line);
            firstLine.subList(s.column, firstLine.size()).clear();
            firstLine.addAll(lastLine.subList(e.column, lastLine.size()));
            lines.subList(s.line + 1, e.line + 1).clear();
            colorizer.markLinesDirty(s.line, s.line);
            maxLineWidth = 0;
        }
        cursor.set(s);
        clearSelection();
    }

    private void copy() {
        if (!hasSelection()) return;
        ImGui.setClipboardText(selectedText());
    }

    private void cut() {
        if (!hasSelection()) return;
        copy();
        pushUndo();
        deleteSelection();
        resetBlink();
    }

    private void paste() {
        String clip = ImGui.getClipboardText();
        if (clip == null || clip.isEmpty()) return;
        pushUndo();
        if (hasSelection()) deleteSelection();
        int defColor = colorizer.getDefaultColor();

        // Normalize line endings then split — avoids triggering auto-indent on pasted content.
        String normalized = clip.replace("\r\n", "\n").replace('\r', '\n');
        String[] parts = normalized.split("\n", -1);

        for (int p = 0; p < parts.length; p++) {
            String seg = parts[p];
            List<EditorGlyph> curLine = lines.get(cursor.line);
            for (int i = 0; i < seg.length(); i++) {
                char c = seg.charAt(i);
                if (!Character.isISOControl(c) || c == '\t') {
                    curLine.add(cursor.column++, new EditorGlyph(c, defColor));
                }
            }
            if (p < parts.length - 1) {
                List<EditorGlyph> tail = new ArrayList<>(curLine.subList(cursor.column, curLine.size()));
                curLine.subList(cursor.column, curLine.size()).clear();
                lines.add(cursor.line + 1, tail);
                cursor.line++;
                cursor.column = 0;
            }
        }

        // Full invalidation so analyzeDocument picks up user-defined symbols in pasted code.
        colorizer.invalidateAll();
        resetBlink();
        maxLineWidth = 0;
        updateAutocomplete();
    }

    private void indentSelection() {
        if (!hasSelection()) {
            int defColor = colorizer.getDefaultColor();
            lines.get(cursor.line).add(cursor.column++, new EditorGlyph('\t', defColor));
            colorizer.markLineDirty(cursor.line);
            return;
        }
        EditorCoordinates s = normSelStart();
        EditorCoordinates e = normSelEnd();
        int defColor = colorizer.getDefaultColor();
        for (int li = s.line; li <= e.line; li++) {
            lines.get(li).add(0, new EditorGlyph('\t', defColor));
            colorizer.markLineDirty(li);
        }
    }

    private void unindentSelection() {
        EditorCoordinates s = normSelStart();
        EditorCoordinates e = normSelEnd();
        for (int li = s.line; li <= e.line; li++) {
            List<EditorGlyph> ln = lines.get(li);
            if (!ln.isEmpty()) {
                char first = ln.get(0).ch;
                if (first == '\t') {
                    ln.remove(0);
                } else {
                    int removed = 0;
                    while (!ln.isEmpty() && ln.get(0).ch == ' ' && removed < theme.tabSize) {
                        ln.remove(0); removed++;
                    }
                }
                colorizer.markLineDirty(li);
            }
        }
    }

    private void moveLeft(boolean select) {
        usePreferredColumn = false;
        if (!select && hasSelection()) { cursor.set(normSelStart()); clearSelection(); return; }
        EditorCoordinates old = cursor.copy();
        if (cursor.column > 0) cursor.column--;
        else if (cursor.line > 0) { cursor.line--; cursor.column = lineLen(cursor.line); }
        extendOrClear(old, select);
    }

    private void moveRight(boolean select) {
        usePreferredColumn = false;
        if (!select && hasSelection()) { cursor.set(normSelEnd()); clearSelection(); return; }
        EditorCoordinates old = cursor.copy();
        if (cursor.column < lineLen(cursor.line)) cursor.column++;
        else if (cursor.line < lines.size() - 1) { cursor.line++; cursor.column = 0; }
        extendOrClear(old, select);
    }

    private void moveUp(boolean select) {
        if (!usePreferredColumn) { preferredColumn = cursor.column; usePreferredColumn = true; }
        EditorCoordinates old = cursor.copy();
        if (cursor.line > 0) {
            cursor.line--;
            cursor.column = Math.min(preferredColumn, lineLen(cursor.line));
        }
        extendOrClear(old, select);
    }

    private void moveDown(boolean select) {
        if (!usePreferredColumn) { preferredColumn = cursor.column; usePreferredColumn = true; }
        EditorCoordinates old = cursor.copy();
        if (cursor.line < lines.size() - 1) {
            cursor.line++;
            cursor.column = Math.min(preferredColumn, lineLen(cursor.line));
        }
        extendOrClear(old, select);
    }

    private void wordLeft(boolean select) {
        usePreferredColumn = false;
        EditorCoordinates old = cursor.copy();
        if (cursor.column == 0 && cursor.line > 0) { cursor.line--; cursor.column = lineLen(cursor.line); }
        else {
            List<EditorGlyph> ln = lines.get(cursor.line);
            int c = cursor.column;
            while (c > 0 && !isWordChar(ln.get(c - 1).ch)) c--;
            while (c > 0 && isWordChar(ln.get(c - 1).ch)) c--;
            cursor.column = c;
        }
        extendOrClear(old, select);
    }

    private void wordRight(boolean select) {
        usePreferredColumn = false;
        EditorCoordinates old = cursor.copy();
        List<EditorGlyph> ln = lines.get(cursor.line);
        int c = cursor.column;
        int len = ln.size();
        if (c == len && cursor.line < lines.size() - 1) { cursor.line++; cursor.column = 0; }
        else {
            while (c < len && !isWordChar(ln.get(c).ch)) c++;
            while (c < len && isWordChar(ln.get(c).ch)) c++;
            cursor.column = c;
        }
        extendOrClear(old, select);
    }

    private void moveLineHome(boolean select) {
        usePreferredColumn = false;
        EditorCoordinates old = cursor.copy();
        // Smart home: first press jumps to first non-whitespace, second press goes to col 0.
        int firstNonWs = 0;
        List<EditorGlyph> ln = lines.get(cursor.line);
        while (firstNonWs < ln.size() && (ln.get(firstNonWs).ch == ' ' || ln.get(firstNonWs).ch == '\t'))
            firstNonWs++;
        cursor.column = (cursor.column != firstNonWs) ? firstNonWs : 0;
        extendOrClear(old, select);
    }

    private void moveLineEnd(boolean select) {
        usePreferredColumn = false;
        EditorCoordinates old = cursor.copy();
        cursor.column = lineLen(cursor.line);
        extendOrClear(old, select);
    }

    private void moveDocHome(boolean select) {
        usePreferredColumn = false;
        EditorCoordinates old = cursor.copy();
        cursor.set(0, 0);
        extendOrClear(old, select);
    }

    private void moveDocEnd(boolean select) {
        usePreferredColumn = false;
        EditorCoordinates old = cursor.copy();
        cursor.line = lines.size() - 1;
        cursor.column = lineLen(cursor.line);
        extendOrClear(old, select);
    }

    private void movePageUp(boolean select, float lineHeight) {
        int pageLines = Math.max(1, (int)(ImGui.getWindowHeight() / lineHeight) - 1);
        EditorCoordinates old = cursor.copy();
        cursor.line = Math.max(0, cursor.line - pageLines);
        cursor.column = Math.min(cursor.column, lineLen(cursor.line));
        extendOrClear(old, select);
    }

    private void movePageDown(boolean select, float lineHeight) {
        int pageLines = Math.max(1, (int)(ImGui.getWindowHeight() / lineHeight) - 1);
        EditorCoordinates old = cursor.copy();
        cursor.line = Math.min(lines.size() - 1, cursor.line + pageLines);
        cursor.column = Math.min(cursor.column, lineLen(cursor.line));
        extendOrClear(old, select);
    }

    private void selectAll() {
        selStart.set(0, 0);
        selEnd.set(lines.size() - 1, lineLen(lines.size() - 1));
        cursor.set(selEnd);
    }

    private void selectWord() {
        List<EditorGlyph> ln = lines.get(cursor.line);
        int c = cursor.column;
        int s = c, e = c;
        while (s > 0 && isWordChar(ln.get(s - 1).ch)) s--;
        while (e < ln.size() && isWordChar(ln.get(e).ch)) e++;
        selStart.set(cursor.line, s);
        selEnd.set(cursor.line, e);
        cursor.set(cursor.line, e);
    }

    private void extendOrClear(EditorCoordinates anchor, boolean select) {
        if (select) {
            if (!hasSelection()) selectionAnchor.set(anchor);
            setRawSelection(selectionAnchor, cursor);
        } else {
            clearSelection();
        }
    }

    private void setRawSelection(EditorCoordinates a, EditorCoordinates b) {
        if (a.lessThan(b) || a.equals(b)) { selStart.set(a); selEnd.set(b); }
        else { selStart.set(b); selEnd.set(a); }
    }

    private void clearSelection() {
        selStart.set(cursor);
        selEnd.set(cursor);
        dragAnchor.set(cursor);
        selectionAnchor.set(cursor);
    }

    private boolean hasSelection() { return !selStart.equals(selEnd); }
    private EditorCoordinates normSelStart() { return selStart.lessThan(selEnd) ? selStart : selEnd; }
    private EditorCoordinates normSelEnd() { return selStart.lessThan(selEnd) ? selEnd : selStart; }

    private String selectedText() {
        if (!hasSelection()) return "";
        EditorCoordinates s = normSelStart();
        EditorCoordinates e = normSelEnd();
        StringBuilder sb = new StringBuilder();
        for (int li = s.line; li <= e.line; li++) {
            List<EditorGlyph> ln = lines.get(li);
            int sc = (li == s.line) ? s.column : 0;
            int ec = (li == e.line) ? e.column : ln.size();
            for (int ci = sc; ci < ec; ci++) sb.append(ln.get(ci).ch);
            if (li < e.line) sb.append('\n');
        }
        return sb.toString();
    }

    public void pushUndo() {
        undoStack.add(new EditorState(lines, cursor, selStart, selEnd));
        if (undoStack.size() > EditorState.MAX_UNDO) undoStack.remove(0);
        redoStack.clear();
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        redoStack.add(new EditorState(lines, cursor, selStart, selEnd));
        applyState(undoStack.remove(undoStack.size() - 1));
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        undoStack.add(new EditorState(lines, cursor, selStart, selEnd));
        applyState(redoStack.remove(redoStack.size() - 1));
    }

    private void applyState(EditorState s) {
        lines.clear();
        lines.addAll(EditorState.deepCopyLines(s.lines));
        cursor.set(s.cursor);
        selStart.set(s.selStart);
        selEnd.set(s.selEnd);
        colorizer.invalidateAll();
        maxLineWidth = 0;
        resetBlink();
    }

    private float colX(int lineIdx, int col, float charWidth) {
        if (lineIdx >= lines.size()) return 0;
        List<EditorGlyph> ln = lines.get(lineIdx);
        float x = 0;
        for (int i = 0; i < col && i < ln.size(); i++) {
            if (ln.get(i).ch == '\t') x = nextTabStop(x, charWidth, theme.tabSize);
            else x += charWidth;
        }
        return x;
    }

    private static float nextTabStop(float x, float charWidth, int tabSize) {
        float tabW = charWidth * tabSize;
        return (float)((Math.floor(x / tabW) + 1) * tabW);
    }

    private EditorCoordinates screenToCoords(float charWidth, float lineHeight) {
        if (!drawCursorPosReady) return cursor.copy();

        ImVec2 mouse = ImGui.getMousePos();
        float sx = ImGui.getScrollX();
        float sy = ImGui.getScrollY();

        float relY = mouse.y - contentOrigin.y + sy;
        float relX = mouse.x - contentOrigin.x + sx - textStart;

        int li = (int) Math.floor(relY / lineHeight);
        li = Math.max(0, Math.min(lines.size() - 1, li));
        List<EditorGlyph> ln = lines.get(li);

        int col = 0;
        float x = 0;
        for (int i = 0; i < ln.size(); i++) {
            float glyphW = (ln.get(i).ch == '\t')
                    ? nextTabStop(x, charWidth, theme.tabSize) - x
                    : charWidth;
            if (relX < x + glyphW / 2f) break;
            x += glyphW;
            col = i + 1;
        }
        return new EditorCoordinates(li, col);
    }

    private void updateAutocomplete() {
        if (autocomplete != null) {
            if (hasSelection()) { autocomplete.hide(); return; }
            autocomplete.update(cursor, lines);
        }
    }

    private int lineLen(int li) {
        return (li >= 0 && li < lines.size()) ? lines.get(li).size() : 0;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static char shiftChar(char c) {
        switch (c) {
            case '0': return ')'; case '1': return '!'; case '2': return '@';
            case '3': return '#'; case '4': return '$'; case '5': return '%';
            case '6': return '^'; case '7': return '&'; case '8': return '*';
            case '9': return '('; case '-': return '_'; case '=': return '+';
            case '[': return '{'; case ']': return '}'; case '\\': return '|';
            case ';': return ':'; case '\'': return '"'; case ',': return '<';
            case '.': return '>'; case '/': return '?'; case '`': return '~';
            default: return c;
        }
    }

    // Fallback for plain-text mode (null colorizer).
    private static final class NullColorizer implements IEditorColorizer {
        private static final int DEF = 0xFFD4D4D4;
        @Override public void colorizeVisibleLines(List<List<EditorGlyph>> l, int f, int e) {}
        @Override public void markLineDirty(int i) {}
        @Override public void markLinesDirty(int s, int e) {}
        @Override public void invalidateAll() {}
        @Override public void colorizeLine(List<List<EditorGlyph>> l, int i) {}
        @Override public int getDefaultColor() { return DEF; }
    }

    public static boolean isCursorToggled() {
        Minecraft mc = Minecraft.getInstance();

        return mc != null && !mc.mouseHandler.isMouseGrabbed();
    }
}