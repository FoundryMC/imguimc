package foundry.imgui.api.text.editor;

public final class EditorCoordinates {
    public int line;
    public int column;

    public EditorCoordinates(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public EditorCoordinates copy() {
        return new EditorCoordinates(line, column);
    }

    public void set(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public void set(EditorCoordinates o) {
        this.line = o.line;
        this.column = o.column;
    }

    public boolean equals(EditorCoordinates o) {
        return line == o.line && column == o.column;
    }

    public boolean lessThan(EditorCoordinates o) {
        return line < o.line || (line == o.line && column < o.column);
    }

    public boolean greaterThan(EditorCoordinates o) {
        return line > o.line || (line == o.line && column > o.column);
    }

    @Override
    public String toString() {
        return "(" + line + "," + column + ")";
    }
}