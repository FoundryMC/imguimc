package foundry.imgui.api.text.preset.glsl;

import foundry.imgui.api.text.autocomplete.AutocompleteItem;
import foundry.imgui.api.text.autocomplete.FunctionSignature;
import foundry.imgui.api.text.autocomplete.IAutocompleteProvider;
import foundry.imgui.api.text.editor.EditorCoordinates;
import foundry.imgui.api.text.editor.EditorGlyph;

import java.util.*;

public final class GLSLAutocompleteProvider implements IAutocompleteProvider {
    private static final List<FunctionSignature> SIGS = Arrays.asList(
            new FunctionSignature("sin", "float", "float angle"),
            new FunctionSignature("cos", "float", "float angle"),
            new FunctionSignature("tan", "float", "float angle"),
            new FunctionSignature("pow", "float", "float x, float y"),
            new FunctionSignature("sqrt", "float", "float x"),
            new FunctionSignature("abs", "float", "float x"),
            new FunctionSignature("min", "float", "float x, float y"),
            new FunctionSignature("max", "float", "float x, float y"),
            new FunctionSignature("clamp", "float", "float x, float minVal, float maxVal"),
            new FunctionSignature("mix", "float", "float x, float y, float a"),
            new FunctionSignature("step", "float", "float edge, float x"),
            new FunctionSignature("smoothstep", "float", "float edge0, float edge1, float x"),
            new FunctionSignature("length", "float", "vec2 x"),
            new FunctionSignature("distance", "float", "vec2 p0, vec2 p1"),
            new FunctionSignature("dot", "float", "vec2 x, vec2 y"),
            new FunctionSignature("cross", "vec3", "vec3 x, vec3 y"),
            new FunctionSignature("normalize", "vec2", "vec2 x"),
            new FunctionSignature("reflect", "vec2", "vec2 I, vec2 N"),
            new FunctionSignature("refract", "vec2", "vec2 I, vec2 N, float eta"),
            new FunctionSignature("texture", "vec4", "sampler2D sampler, vec2 coord"),
            new FunctionSignature("texture2D", "vec4", "sampler2D sampler, vec2 coord"),
            new FunctionSignature("dFdx", "float", "float p"),
            new FunctionSignature("dFdy", "float", "float p"),
            new FunctionSignature("fwidth", "float", "float p"),
            new FunctionSignature("floor", "float", "float x"),
            new FunctionSignature("ceil", "float", "float x"),
            new FunctionSignature("fract", "float", "float x"),
            new FunctionSignature("mod", "float", "float x, float y"),
            new FunctionSignature("exp", "float", "float x"),
            new FunctionSignature("log", "float", "float x"),
            new FunctionSignature("exp2", "float", "float x"),
            new FunctionSignature("log2", "float", "float x"),
            new FunctionSignature("inversesqrt", "float", "float x")
    );

    private final GLSLColorizer colorizer;

    public GLSLAutocompleteProvider(GLSLColorizer colorizer) {
        this.colorizer = colorizer;
    }

    @Override public int minPrefixLength() { return 2; }
    @Override public boolean appendParens() { return true; }

    @Override
    public boolean shouldSuppress(String prefix, List<List<EditorGlyph>> lines, EditorCoordinates cursor) {
        if (cursor.line < lines.size()) {
            List<EditorGlyph> line = lines.get(cursor.line);
            int wordStart = cursor.column - prefix.length();
            if (wordStart > 0 && line.get(wordStart - 1).ch == '.') return true;
        }
        return false;
    }

    @Override
    public List<AutocompleteItem> getCandidates(String prefix, List<List<EditorGlyph>> lines, EditorCoordinates cursor) {
        String lower = prefix.toLowerCase();
        List<AutocompleteItem> out = new ArrayList<>();

        for (String kw : GLSLColorizer.KEYWORDS)
            if (kw.toLowerCase().startsWith(lower))
                out.add(item(kw, "keyword", "", GLSLColorizer.COLOR_KEYWORD));

        for (String t : GLSLColorizer.BUILT_IN_TYPES)
            if (t.toLowerCase().startsWith(lower))
                out.add(item(t, "type", "", GLSLColorizer.COLOR_BUILT_IN_TYPE));

        for (String q : GLSLColorizer.QUALIFIERS)
            if (q.toLowerCase().startsWith(lower))
                out.add(item(q, "qualifier", "", GLSLColorizer.COLOR_KEYWORD));

        for (String fn : GLSLColorizer.BUILT_IN_FUNCTIONS)
            if (fn.toLowerCase().startsWith(lower))
                out.add(item(fn, "function", sigFor(fn), GLSLColorizer.COLOR_FUNCTION_CALL));

        for (String v : GLSLColorizer.BUILT_IN_VARIABLES)
            if (v.toLowerCase().startsWith(lower))
                out.add(item(v, "variable", "", GLSLColorizer.COLOR_BUILT_IN_VAR));

        for (String c : GLSLColorizer.BUILT_IN_CONSTANTS)
            if (c.toLowerCase().startsWith(lower))
                out.add(item(c, "constant", "", GLSLColorizer.COLOR_BUILT_IN_CONST));

        for (String fn : colorizer.userDefinedFunctions)
            if (fn.toLowerCase().startsWith(lower))
                out.add(item(fn, "function", "", GLSLColorizer.COLOR_FUNCTION_NAME));

        for (String tp : colorizer.userDefinedTypes)
            if (tp.toLowerCase().startsWith(lower))
                out.add(item(tp, "type", "", GLSLColorizer.COLOR_BUILT_IN_TYPE));

        return out;
    }

    private static String sigFor(String name) {
        for (FunctionSignature s : SIGS) if (s.name.equals(name)) return s.format();
        return "";
    }

    private static AutocompleteItem item(String text, String type, String sig, int color) {
        return new AutocompleteItem(text, type, sig, color);
    }
}