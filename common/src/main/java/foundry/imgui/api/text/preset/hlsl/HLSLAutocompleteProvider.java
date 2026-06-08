package foundry.imgui.api.text.preset.hlsl;

import foundry.imgui.api.text.autocomplete.AutocompleteItem;
import foundry.imgui.api.text.autocomplete.FunctionSignature;
import foundry.imgui.api.text.autocomplete.IAutocompleteProvider;
import foundry.imgui.api.text.editor.EditorCoordinates;
import foundry.imgui.api.text.editor.EditorGlyph;

import java.util.*;

public final class HLSLAutocompleteProvider implements IAutocompleteProvider {
    private static final List<FunctionSignature> SIGS = Arrays.asList(
            new FunctionSignature("abs", "float", "float x"),
            new FunctionSignature("acos", "float", "float x"),
            new FunctionSignature("all", "bool", "float4 x"),
            new FunctionSignature("any", "bool", "float4 x"),
            new FunctionSignature("asin", "float", "float x"),
            new FunctionSignature("atan", "float", "float x"),
            new FunctionSignature("atan2", "float", "float y, float x"),
            new FunctionSignature("ceil", "float", "float x"),
            new FunctionSignature("clamp", "float", "float x, float min, float max"),
            new FunctionSignature("clip", "void", "float x"),
            new FunctionSignature("cos", "float", "float x"),
            new FunctionSignature("cosh", "float", "float x"),
            new FunctionSignature("cross", "float3", "float3 x, float3 y"),
            new FunctionSignature("ddx", "float", "float x"),
            new FunctionSignature("ddx_coarse", "float", "float x"),
            new FunctionSignature("ddx_fine", "float", "float x"),
            new FunctionSignature("ddy", "float", "float x"),
            new FunctionSignature("ddy_coarse", "float", "float x"),
            new FunctionSignature("ddy_fine", "float", "float x"),
            new FunctionSignature("degrees", "float", "float x"),
            new FunctionSignature("determinant", "float", "float4x4 m"),
            new FunctionSignature("distance", "float", "float3 a, float3 b"),
            new FunctionSignature("dot", "float", "float4 x, float4 y"),
            new FunctionSignature("exp", "float", "float x"),
            new FunctionSignature("exp2", "float", "float x"),
            new FunctionSignature("faceforward", "float3", "float3 n, float3 i, float3 ng"),
            new FunctionSignature("floor", "float", "float x"),
            new FunctionSignature("fmod", "float", "float x, float y"),
            new FunctionSignature("frac", "float", "float x"),
            new FunctionSignature("frexp", "float", "float x, out float exp"),
            new FunctionSignature("fwidth", "float", "float x"),
            new FunctionSignature("isfinite", "bool", "float x"),
            new FunctionSignature("isinf", "bool", "float x"),
            new FunctionSignature("isnan", "bool", "float x"),
            new FunctionSignature("ldexp", "float", "float x, float exp"),
            new FunctionSignature("length", "float", "float3 x"),
            new FunctionSignature("lerp", "float", "float x, float y, float s"),
            new FunctionSignature("log", "float", "float x"),
            new FunctionSignature("log10", "float", "float x"),
            new FunctionSignature("log2", "float", "float x"),
            new FunctionSignature("max", "float", "float x, float y"),
            new FunctionSignature("min", "float", "float x, float y"),
            new FunctionSignature("modf", "float", "float x, out float ip"),
            new FunctionSignature("mul", "float4", "float4 x, float4x4 m"),
            new FunctionSignature("normalize", "float3", "float3 x"),
            new FunctionSignature("pow", "float", "float x, float y"),
            new FunctionSignature("radians", "float", "float x"),
            new FunctionSignature("rcp", "float", "float x"),
            new FunctionSignature("reflect", "float3", "float3 i, float3 n"),
            new FunctionSignature("refract", "float3", "float3 i, float3 n, float eta"),
            new FunctionSignature("reversebits", "uint", "uint x"),
            new FunctionSignature("round", "float", "float x"),
            new FunctionSignature("rsqrt", "float", "float x"),
            new FunctionSignature("saturate", "float", "float x"),
            new FunctionSignature("sign", "float", "float x"),
            new FunctionSignature("sin", "float", "float x"),
            new FunctionSignature("sincos", "void", "float x, out float s, out float c"),
            new FunctionSignature("sinh", "float", "float x"),
            new FunctionSignature("smoothstep", "float", "float min, float max, float x"),
            new FunctionSignature("sqrt", "float", "float x"),
            new FunctionSignature("step", "float", "float edge, float x"),
            new FunctionSignature("tan", "float", "float x"),
            new FunctionSignature("tanh", "float", "float x"),
            new FunctionSignature("transpose", "float4x4", "float4x4 m"),
            new FunctionSignature("trunc", "float", "float x"),
            new FunctionSignature("countbits", "uint", "uint x"),
            new FunctionSignature("firstbithigh", "int", "uint x"),
            new FunctionSignature("firstbitlow", "uint", "uint x"),
            new FunctionSignature("f16tof32", "float", "uint x"),
            new FunctionSignature("f32tof16", "uint", "float x"),
            new FunctionSignature("Sample", "float4", "SamplerState s, float2 location"),
            new FunctionSignature("SampleLevel", "float4", "SamplerState s, float2 location, float lod"),
            new FunctionSignature("SampleBias", "float4", "SamplerState s, float2 location, float bias"),
            new FunctionSignature("SampleGrad", "float4", "SamplerState s, float2 loc, float2 ddx, float2 ddy"),
            new FunctionSignature("SampleCmp", "float", "SamplerComparisonState s, float2 loc, float cmp"),
            new FunctionSignature("Gather", "float4", "SamplerState s, float2 location"),
            new FunctionSignature("Load", "float4", "int3 location"),
            new FunctionSignature("GetDimensions", "void", "out uint width, out uint height"),
            new FunctionSignature("InterlockedAdd", "void", "inout int dest, int value"),
            new FunctionSignature("InterlockedMax", "void", "inout int dest, int value"),
            new FunctionSignature("InterlockedMin", "void", "inout int dest, int value")
    );

    private final HLSLColorizer colorizer;

    public HLSLAutocompleteProvider(HLSLColorizer colorizer) {
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

        for (String kw : HLSLColorizer.KEYWORDS)
            if (kw.toLowerCase().startsWith(lower))
                out.add(item(kw, "keyword", "", HLSLColorizer.COLOR_KEYWORD));

        for (String t : HLSLColorizer.BUILT_IN_TYPES)
            if (t.toLowerCase().startsWith(lower))
                out.add(item(t, "type", "", HLSLColorizer.COLOR_BUILT_IN_TYPE));

        for (String fn : HLSLColorizer.BUILT_IN_FUNCTIONS)
            if (fn.toLowerCase().startsWith(lower))
                out.add(item(fn, "function", sigFor(fn), HLSLColorizer.COLOR_FUNCTION_CALL));

        for (String sem : HLSLColorizer.SEMANTICS)
            if (sem.toLowerCase().startsWith(lower))
                out.add(item(sem, "variable", "", HLSLColorizer.COLOR_SEMANTIC));

        for (String c : HLSLColorizer.BUILT_IN_CONSTANTS)
            if (c.toLowerCase().startsWith(lower))
                out.add(item(c, "constant", "", HLSLColorizer.COLOR_BUILT_IN_CONST));

        for (String fn : colorizer.userDefinedFunctions)
            if (fn.toLowerCase().startsWith(lower))
                out.add(item(fn, "function", "", HLSLColorizer.COLOR_FUNCTION_NAME));

        for (String tp : colorizer.userDefinedTypes)
            if (tp.toLowerCase().startsWith(lower))
                out.add(item(tp, "type", "", HLSLColorizer.COLOR_BUILT_IN_TYPE));

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