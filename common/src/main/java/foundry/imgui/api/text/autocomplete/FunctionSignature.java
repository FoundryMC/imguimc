package foundry.imgui.api.text.autocomplete;

public final class FunctionSignature {
    public final String name;
    public final String returnType;
    public final String[] params;

    public FunctionSignature(String name, String returnType, String... params) {
        this.name = name;
        this.returnType = returnType;
        this.params = params;
    }

    public String format() {
        return returnType + " " + name + "(" + String.join(", ", params) + ")";
    }
}