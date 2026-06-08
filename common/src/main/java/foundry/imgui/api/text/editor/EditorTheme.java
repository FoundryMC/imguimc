package foundry.imgui.api.text.editor;

// All colours are ImGui ABGR packed ints.
public final class EditorTheme {
    public final int backgroundColor;
    public final int currentLineColor;
    public final int selectionColor;
    public final int cursorColor;
    public final int cursorBlinkMs; // 0 = no blink
    public final float cursorWidth;
    public final int lineNumberColor;
    public final int gutterSeparatorColor;
    public final float gutterPaddingRight;
    public final float hScrollbarHeight;
    public final float lineSpacing;
    public final int tabSize;

    private EditorTheme(Builder b) {
        this.backgroundColor = b.backgroundColor;
        this.currentLineColor = b.currentLineColor;
        this.selectionColor = b.selectionColor;
        this.cursorColor = b.cursorColor;
        this.cursorBlinkMs = b.cursorBlinkMs;
        this.cursorWidth = b.cursorWidth;
        this.lineNumberColor = b.lineNumberColor;
        this.gutterSeparatorColor = b.gutterSeparatorColor;
        this.gutterPaddingRight = b.gutterPaddingRight;
        this.hScrollbarHeight = b.hScrollbarHeight;
        this.lineSpacing = b.lineSpacing;
        this.tabSize = b.tabSize;
    }

    public static Builder dark() {
        return new Builder()
                .withBackgroundColor(0xB2120D0A)
                .withCurrentLineColor(0x18FFFFFF)
                .withSelectionColor(0x804060C0)
                .withCursorColor(0xFFE0E0E0)
                .withCursorBlinkMs(1000)
                .withCursorWidth(2f)
                .withLineNumberColor(0xFF858585)
                .withGutterSeparatorColor(0xFF2A2A2A)
                .withGutterPaddingRight(10f)
                .withHScrollbarHeight(10f)
                .withLineSpacing(1.0f)
                .withTabSize(4);
    }

    public static Builder monokai() {
        return new Builder()
                .withBackgroundColor(0xE6272822)
                .withCurrentLineColor(0x20FFFFFF)
                .withSelectionColor(0x8049483E)
                .withCursorColor(0xFFF8F8F2)
                .withCursorBlinkMs(1000)
                .withCursorWidth(2f)
                .withLineNumberColor(0xFF75715E)
                .withGutterSeparatorColor(0xFF3E3D32)
                .withGutterPaddingRight(10f)
                .withHScrollbarHeight(10f)
                .withLineSpacing(1.0f)
                .withTabSize(4);
    }

    public static Builder light() {
        return new Builder()
                .withBackgroundColor(0xFFFAFAFA)
                .withCurrentLineColor(0x18000000)
                .withSelectionColor(0x18000000)
                .withCursorColor(0xFF1A1A1A)
                .withCursorBlinkMs(1000)
                .withCursorWidth(2f)
                .withLineNumberColor(0xFF999999)
                .withGutterSeparatorColor(0xFFDDDDDD)
                .withGutterPaddingRight(10f)
                .withHScrollbarHeight(10f)
                .withLineSpacing(1.0f)
                .withTabSize(4);
    }

    public static Builder dracula() {
        return new Builder()
                .withBackgroundColor(0xFF36272A)
                .withCurrentLineColor(0x2044475A)
                .withSelectionColor(0x8044475A)
                .withCursorColor(0xFFF8F8F2)
                .withCursorBlinkMs(1000)
                .withCursorWidth(2f)
                .withLineNumberColor(0xFF6272A4)
                .withGutterSeparatorColor(0xFF44475A)
                .withGutterPaddingRight(10f)
                .withHScrollbarHeight(10f)
                .withLineSpacing(1.0f)
                .withTabSize(4);
    }

    public static Builder nord() {
        return new Builder()
                .withBackgroundColor(0xFF40342E)
                .withCurrentLineColor(0x3052423B)
                .withSelectionColor(0x60D0C088)
                .withCursorColor(0xFFF4EFEC)
                .withCursorBlinkMs(1000)
                .withCursorWidth(2f)
                .withLineNumberColor(0xFF6A564C)
                .withGutterSeparatorColor(0xFF5E4C43)
                .withGutterPaddingRight(10f)
                .withHScrollbarHeight(10f)
                .withLineSpacing(1.0f)
                .withTabSize(4);
    }

    public static Builder solarizedDark() {
        return new Builder()
                .withBackgroundColor(0xFF362B00)
                .withCurrentLineColor(0x18FDF6E3)
                .withSelectionColor(0x60586E75)
                .withCursorColor(0xFF839496)
                .withCursorBlinkMs(1000)
                .withCursorWidth(2f)
                .withLineNumberColor(0xFF586E75)
                .withGutterSeparatorColor(0xFF073642)
                .withGutterPaddingRight(10f)
                .withHScrollbarHeight(10f)
                .withLineSpacing(1.0f)
                .withTabSize(4);
    }

    public static Builder solarizedLight() {
        return new Builder()
                .withBackgroundColor(0xFFE3F6FD)
                .withCurrentLineColor(0x18000000)
                .withSelectionColor(0x5093A1A1)
                .withCursorColor(0xFF657B83)
                .withCursorBlinkMs(1000)
                .withCursorWidth(2f)
                .withLineNumberColor(0xFF93A1A1)
                .withGutterSeparatorColor(0xFFEEE8D5)
                .withGutterPaddingRight(10f)
                .withHScrollbarHeight(10f)
                .withLineSpacing(1.0f)
                .withTabSize(4);
    }

    public static Builder oneDark() {
        return new Builder()
                .withBackgroundColor(0xFF342C28)
                .withCurrentLineColor(0x202C313C)
                .withSelectionColor(0x603E4450)
                .withCursorColor(0xFFABB2BF)
                .withCursorBlinkMs(1000)
                .withCursorWidth(2f)
                .withLineNumberColor(0xFF4B5263)
                .withGutterSeparatorColor(0xFF3B4048)
                .withGutterPaddingRight(10f)
                .withHScrollbarHeight(10f)
                .withLineSpacing(1.0f)
                .withTabSize(4);
    }

    public static Builder githubLight() {
        return new Builder()
                .withBackgroundColor(0xFFFFFFFF)
                .withCurrentLineColor(0x14000000)
                .withSelectionColor(0x500489E0)
                .withCursorColor(0xFF24292E)
                .withCursorBlinkMs(1000)
                .withCursorWidth(2f)
                .withLineNumberColor(0xFF959DA5)
                .withGutterSeparatorColor(0xFFE1E4E8)
                .withGutterPaddingRight(10f)
                .withHScrollbarHeight(10f)
                .withLineSpacing(1.0f)
                .withTabSize(4);
    }

    public EditorTheme withCursorColor(int c) { return toBuilder().withCursorColor(c).build(); }
    public EditorTheme withSelectionColor(int c) { return toBuilder().withSelectionColor(c).build(); }
    public EditorTheme withBackgroundColor(int c) { return toBuilder().withBackgroundColor(c).build(); }
    public EditorTheme withLineNumberColor(int c) { return toBuilder().withLineNumberColor(c).build(); }
    public EditorTheme withLineSpacing(float s) { return toBuilder().withLineSpacing(s).build(); }
    public EditorTheme withTabSize(int t) { return toBuilder().withTabSize(t).build(); }

    private Builder toBuilder() {
        Builder b = new Builder();
        b.backgroundColor = backgroundColor;
        b.currentLineColor = currentLineColor;
        b.selectionColor = selectionColor;
        b.cursorColor = cursorColor;
        b.cursorBlinkMs = cursorBlinkMs;
        b.cursorWidth = cursorWidth;
        b.lineNumberColor = lineNumberColor;
        b.gutterSeparatorColor = gutterSeparatorColor;
        b.gutterPaddingRight = gutterPaddingRight;
        b.hScrollbarHeight = hScrollbarHeight;
        b.lineSpacing = lineSpacing;
        b.tabSize = tabSize;
        return b;
    }

    public static final class Builder {
        private int backgroundColor = 0xB2120D0A;
        private int currentLineColor = 0x18FFFFFF;
        private int selectionColor = 0x804060C0;
        private int cursorColor = 0xFFE0E0E0;
        private int cursorBlinkMs = 1000;
        private float cursorWidth = 2f;
        private int lineNumberColor = 0xFF858585;
        private int gutterSeparatorColor = 0xFF2A2A2A;
        private float gutterPaddingRight = 10f;
        private float hScrollbarHeight = 10f;
        private float lineSpacing = 1.0f;
        private int tabSize = 4;

        public Builder withBackgroundColor(int c) { backgroundColor = c; return this; }
        public Builder withCurrentLineColor(int c) { currentLineColor = c; return this; }
        public Builder withSelectionColor(int c) { selectionColor = c; return this; }
        public Builder withCursorColor(int c) { cursorColor = c; return this; }
        public Builder withCursorBlinkMs(int ms) { cursorBlinkMs = ms; return this; }
        public Builder withCursorWidth(float w) { cursorWidth = w; return this; }
        public Builder withLineNumberColor(int c) { lineNumberColor = c; return this; }
        public Builder withGutterSeparatorColor(int c) { gutterSeparatorColor = c; return this; }
        public Builder withGutterPaddingRight(float p) { gutterPaddingRight = p; return this; }
        public Builder withHScrollbarHeight(float h) { hScrollbarHeight = h; return this; }
        public Builder withLineSpacing(float s) { lineSpacing = s; return this; }
        public Builder withTabSize(int t) { tabSize = t; return this; }

        public EditorTheme build() { return new EditorTheme(this); }
    }
}