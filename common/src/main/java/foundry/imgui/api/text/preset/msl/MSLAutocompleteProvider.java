package foundry.imgui.api.text.preset.msl;

import foundry.imgui.api.text.autocomplete.AutocompleteItem;
import foundry.imgui.api.text.autocomplete.FunctionSignature;
import foundry.imgui.api.text.autocomplete.IAutocompleteProvider;
import foundry.imgui.api.text.editor.EditorCoordinates;
import foundry.imgui.api.text.editor.EditorGlyph;

import java.util.*;

public final class MSLAutocompleteProvider implements IAutocompleteProvider {
    private static final List<FunctionSignature> SIGS = Arrays.asList(
            new FunctionSignature("abs", "T", "T x"),
            new FunctionSignature("acos", "float", "float x"),
            new FunctionSignature("asin", "float", "float x"),
            new FunctionSignature("atan", "float", "float x"),
            new FunctionSignature("atan2", "float", "float y, float x"),
            new FunctionSignature("ceil", "float", "float x"),
            new FunctionSignature("clamp", "T", "T x, T minval, T maxval"),
            new FunctionSignature("cos", "float", "float x"),
            new FunctionSignature("cospi", "float", "float x"),
            new FunctionSignature("cross", "float3", "float3 x, float3 y"),
            new FunctionSignature("degrees", "float", "float x"),
            new FunctionSignature("distance", "float", "float3 x, float3 y"),
            new FunctionSignature("dot", "float", "float3 x, float3 y"),
            new FunctionSignature("exp", "float", "float x"),
            new FunctionSignature("exp2", "float", "float x"),
            new FunctionSignature("fabs", "float", "float x"),
            new FunctionSignature("floor", "float", "float x"),
            new FunctionSignature("fma", "float", "float a, float b, float c"),
            new FunctionSignature("fmax", "float", "float x, float y"),
            new FunctionSignature("fmin", "float", "float x, float y"),
            new FunctionSignature("fmod", "float", "float x, float y"),
            new FunctionSignature("fract", "float", "float x"),
            new FunctionSignature("frexp", "float", "float x, thread int &exp"),
            new FunctionSignature("isinf", "bool", "float x"),
            new FunctionSignature("isnan", "bool", "float x"),
            new FunctionSignature("isfinite", "bool", "float x"),
            new FunctionSignature("isnormal", "bool", "float x"),
            new FunctionSignature("ldexp", "float", "float x, int n"),
            new FunctionSignature("length", "float", "float3 x"),
            new FunctionSignature("length_squared", "float", "float3 x"),
            new FunctionSignature("log", "float", "float x"),
            new FunctionSignature("log2", "float", "float x"),
            new FunctionSignature("log10", "float", "float x"),
            new FunctionSignature("max", "T", "T x, T y"),
            new FunctionSignature("min", "T", "T x, T y"),
            new FunctionSignature("mix", "T", "T x, T y, T a"),
            new FunctionSignature("modf", "float", "float x, thread float &intpart"),
            new FunctionSignature("normalize", "float3", "float3 x"),
            new FunctionSignature("pow", "float", "float x, float y"),
            new FunctionSignature("powr", "float", "float x, float y"),
            new FunctionSignature("radians", "float", "float x"),
            new FunctionSignature("reflect", "float3", "float3 i, float3 n"),
            new FunctionSignature("refract", "float3", "float3 i, float3 n, float eta"),
            new FunctionSignature("rint", "float", "float x"),
            new FunctionSignature("round", "float", "float x"),
            new FunctionSignature("rsqrt", "float", "float x"),
            new FunctionSignature("saturate", "T", "T x"),
            new FunctionSignature("select", "T", "T a, T b, bool c"),
            new FunctionSignature("sign", "float", "float x"),
            new FunctionSignature("sin", "float", "float x"),
            new FunctionSignature("sincos", "float", "float x, thread float &cosval"),
            new FunctionSignature("sinpi", "float", "float x"),
            new FunctionSignature("smoothstep", "T", "T edge0, T edge1, T x"),
            new FunctionSignature("sqrt", "float", "float x"),
            new FunctionSignature("step", "T", "T edge, T x"),
            new FunctionSignature("tan", "float", "float x"),
            new FunctionSignature("tanpi", "float", "float x"),
            new FunctionSignature("transpose", "float4x4", "float4x4 m"),
            new FunctionSignature("trunc", "float", "float x"),
            new FunctionSignature("dfdx", "float", "float x"),
            new FunctionSignature("dfdy", "float", "float x"),
            new FunctionSignature("fwidth", "float", "float x"),
            new FunctionSignature("sample", "float4", "sampler s, float2 coord"),
            new FunctionSignature("sample_compare", "float", "sampler s, float2 coord, float cmp"),
            new FunctionSignature("gather", "float4", "sampler s, float2 coord"),
            new FunctionSignature("gather_compare", "float4", "sampler s, float2 coord, float cmp"),
            new FunctionSignature("read", "float4", "uint2 coord"),
            new FunctionSignature("write", "void", "float4 color, uint2 coord"),
            new FunctionSignature("get_width", "uint", "uint lod = 0"),
            new FunctionSignature("get_height", "uint", "uint lod = 0"),
            new FunctionSignature("get_depth", "uint", "uint lod = 0"),
            new FunctionSignature("as_type", "T", "U x"),
            new FunctionSignature("simd_broadcast", "T", "T value, ushort lane"),
            new FunctionSignature("simd_sum", "T", "T value"),
            new FunctionSignature("simd_min", "T", "T value"),
            new FunctionSignature("simd_max", "T", "T value"),
            new FunctionSignature("simd_shuffle", "T", "T value, ushort lane"),
            new FunctionSignature("simd_shuffle_down", "T", "T value, ushort delta"),
            new FunctionSignature("simd_shuffle_up", "T", "T value, ushort delta"),
            new FunctionSignature("simd_vote_all", "bool", "bool value"),
            new FunctionSignature("simd_vote_any", "bool", "bool value"),
            new FunctionSignature("simd_is_first", "bool", ""),
            new FunctionSignature("threadgroup_barrier", "void", "mem_flags flags"),
            new FunctionSignature("simdgroup_barrier", "void", "mem_flags flags")
    );

    private final MSLColorizer colorizer;

    public MSLAutocompleteProvider(MSLColorizer colorizer) {
        this.colorizer = colorizer;
    }

    @Override public int minPrefixLength() { return 2; }
    @Override public boolean appendParens() { return true; }

    @Override
    public boolean shouldSuppress(String prefix, List<List<EditorGlyph>> lines, EditorCoordinates cursor) {
        if (cursor.line < lines.size()) {
            List<EditorGlyph> line = lines.get(cursor.line);
            int wordStart = cursor.column - prefix.length();
            if (wordStart > 0) {
                char prev = line.get(wordStart - 1).ch;
                if (prev == '.' || prev == '>') return true;
            }
        }
        return false;
    }

    @Override
    public List<AutocompleteItem> getCandidates(String prefix, List<List<EditorGlyph>> lines, EditorCoordinates cursor) {
        String lower = prefix.toLowerCase();
        List<AutocompleteItem> out = new ArrayList<>();

        for (String kw : MSLColorizer.KEYWORDS)
            if (kw.toLowerCase().startsWith(lower))
                out.add(item(kw, "keyword", "", MSLColorizer.COLOR_KEYWORD));

        for (String t : MSLColorizer.BUILT_IN_TYPES)
            if (t.toLowerCase().startsWith(lower))
                out.add(item(t, "type", "", MSLColorizer.COLOR_BUILT_IN_TYPE));

        for (String fn : MSLColorizer.BUILT_IN_FUNCTIONS)
            if (fn.toLowerCase().startsWith(lower))
                out.add(item(fn, "function", sigFor(fn), MSLColorizer.COLOR_FUNCTION_CALL));

        for (String attr : MSLColorizer.ATTRIBUTES)
            if (attr.toLowerCase().startsWith(lower))
                out.add(item(attr, "variable", "", MSLColorizer.COLOR_ATTRIBUTE));

        for (String c : MSLColorizer.BUILT_IN_CONSTANTS)
            if (c.toLowerCase().startsWith(lower))
                out.add(item(c, "constant", "", MSLColorizer.COLOR_BUILT_IN_CONST));

        for (String fn : colorizer.userDefinedFunctions)
            if (fn.toLowerCase().startsWith(lower))
                out.add(item(fn, "function", "", MSLColorizer.COLOR_FUNCTION_NAME));

        for (String tp : colorizer.userDefinedTypes)
            if (tp.toLowerCase().startsWith(lower))
                out.add(item(tp, "type", "", MSLColorizer.COLOR_BUILT_IN_TYPE));

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