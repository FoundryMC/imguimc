package foundry.imgui.api.text.preset.msl;

import foundry.imgui.api.text.color.AbstractBaseColorizer;
import foundry.imgui.api.text.editor.EditorGlyph;

import java.util.*;
import java.util.regex.*;

public final class MSLColorizer extends AbstractBaseColorizer {
    // ABGR
    public static final int COLOR_DEFAULT = 0xFFC6B7A9;
    public static final int COLOR_KEYWORD = 0xFF8585FC;
    public static final int COLOR_BUILT_IN_TYPE = 0xFF71C0F6;
    public static final int COLOR_STRING = 0xFF74DBE6;
    public static final int COLOR_PREPROCESSOR = 0xFF7426F9;
    public static final int COLOR_NUMBER = 0xFF5DACA2;
    public static final int COLOR_FUNCTION_CALL = 0xFF40A885;
    public static final int COLOR_FUNCTION_NAME = 0xFFBA769A;
    public static final int COLOR_ATTRIBUTE = 0xFFE0C080; // [[attribute]] annotations
    public static final int COLOR_BUILT_IN_CONST = 0xFF86D9B9;
    public static final int COLOR_USER_IDENT = 0xFFC79565;
    public static final int COLOR_OPERATOR = 0xFFC6B7A9;
    public static final int COLOR_COMMENT = 0xFF888888;
    public static final int COLOR_COMMENT_MULTI = 0xFF557962;

    // C++14 keywords (subset relevant in MSL) plus Metal-specific keywords.
    public static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "if", "else", "for", "while", "do", "switch", "case", "default",
            "break", "continue", "return", "discard_fragment",
            "struct", "class", "enum", "union", "namespace", "typedef",
            "template", "typename", "inline", "static", "extern",
            "const", "constexpr", "volatile", "auto", "decltype",
            "nullptr", "this", "operator", "using", "static_assert",
            "sizeof", "alignas", "alignof", "noexcept",
            // Metal address-space qualifiers
            "device", "constant", "threadgroup", "threadgroup_imageblock",
            "thread", "object_data", "ray_data", "intersection_tag",
            // Metal function qualifiers
            "vertex", "fragment", "kernel",
            // Metal 3+ mesh shaders
            "mesh", "object",
            "visible"
    ));

    public static final Set<String> BUILT_IN_TYPES = new HashSet<>(Arrays.asList(
            "void", "bool", "char", "short", "int", "long",
            "uchar", "ushort", "uint", "ulong",
            "half", "float",
            "bfloat",
            "bool2", "bool3", "bool4",
            "char2", "char3", "char4",
            "uchar2", "uchar3", "uchar4",
            "short2", "short3", "short4",
            "ushort2", "ushort3", "ushort4",
            "int2", "int3", "int4",
            "uint2", "uint3", "uint4",
            "long2", "long3", "long4",
            "ulong2", "ulong3", "ulong4",
            "half2", "half3", "half4",
            "float2", "float3", "float4",
            "bfloat2", "bfloat3", "bfloat4",
            "half2x2", "half2x3", "half2x4",
            "half3x2", "half3x3", "half3x4",
            "half4x2", "half4x3", "half4x4",
            "float2x2", "float2x3", "float2x4",
            "float3x2", "float3x3", "float3x4",
            "float4x2", "float4x3", "float4x4",
            "texture1d", "texture1d_array",
            "texture2d", "texture2d_array", "texture2d_ms", "texture2d_ms_array",
            "texture3d", "texturecube", "texturecube_array",
            "depth2d", "depth2d_array", "depth2d_ms", "depth2d_ms_array",
            "depthcube", "depthcube_array",
            "sampler", "sampler_state",
            "atomic_int", "atomic_uint", "atomic_bool",
            "intersector", "instance_acceleration_structure", "primitive_acceleration_structure",
            "intersection_result", "ray",
            "imageblock", "r8unorm", "r8snorm", "r8uint", "r8sint",
            "r16unorm", "r16snorm", "r16uint", "r16sint", "r16float",
            "rg8unorm", "rg8snorm", "rgba8unorm", "rgba8snorm",
            "rgba8uint", "rgba8sint", "rgba16uint", "rgba16sint", "rgba16float",
            "rgba32uint", "rgba32sint", "rgba32float",
            "mesh_grid_properties"
    ));

    public static final Set<String> BUILT_IN_FUNCTIONS = new HashSet<>(Arrays.asList(
            "abs", "acos", "acosh", "asin", "asinh", "atan", "atan2", "atanh",
            "ceil", "clamp", "copysign", "cos", "cosh", "cospi",
            "cross", "degrees", "distance", "dot",
            "exp", "exp2", "exp10",
            "fabs", "fdim", "floor", "fma", "fmax", "fmin", "fmod",
            "fract", "frexp", "hypot", "ilogb", "ldexp", "lgamma",
            "log", "log2", "log10", "logb",
            "max", "min", "mix", "modf", "nan", "nextafter",
            "normalize", "powr", "pow", "precise",
            "radians", "reflect", "refract", "remainder", "remquo", "rint", "round", "rsqrt",
            "saturate", "sign", "sin", "sinh", "sincos", "sinpi",
            "smoothstep", "sqrt", "step", "tan", "tanh", "tanpi", "tgamma", "trunc",
            "half_cos", "half_exp", "half_exp2", "half_exp10",
            "half_log", "half_log2", "half_log10",
            "half_powr", "half_recip", "half_rsqrt", "half_sin", "half_sqrt", "half_tan",
            "native_cos", "native_exp", "native_exp2", "native_exp10",
            "native_log", "native_log2", "native_log10",
            "native_powr", "native_recip", "native_rsqrt", "native_sin", "native_sqrt", "native_tan",
            "length", "length_squared", "faceforward",
            "clz", "ctz", "popcount", "rotate",
            "add_sat", "hadd", "mad_hi", "mad_sat", "mad24", "mul24", "mul_hi", "rhadd", "sub_sat",
            "transpose",
            "dfdx", "dfdy", "fwidth",
            "atomic_fetch_add_explicit", "atomic_fetch_sub_explicit",
            "atomic_fetch_and_explicit", "atomic_fetch_or_explicit", "atomic_fetch_xor_explicit",
            "atomic_load_explicit", "atomic_store_explicit", "atomic_exchange_explicit",
            "atomic_compare_exchange_weak_explicit", "atomic_compare_exchange_strong_explicit",
            "threadgroup_barrier", "simdgroup_barrier",
            "simd_broadcast", "simd_shuffle", "simd_shuffle_down", "simd_shuffle_up",
            "simd_shuffle_xor", "simd_vote_all", "simd_vote_any", "simd_vote_and", "simd_vote_or",
            "simd_sum", "simd_product", "simd_min", "simd_max", "simd_prefix_inclusive_sum",
            "simd_prefix_exclusive_sum", "simd_prefix_inclusive_product",
            "simd_prefix_exclusive_product", "simd_active_threads_mask", "simd_is_first",
            "simd_is_helper_thread",
            "quad_broadcast", "quad_shuffle", "quad_shuffle_down", "quad_shuffle_up", "quad_shuffle_xor",
            "sample", "sample_compare", "gather", "gather_compare", "read", "write",
            "get_width", "get_height", "get_depth", "get_num_mip_levels", "get_array_size",
            "intersect",
            "select", "bitselect", "as_type", "convert"
    ));

    // Metal [[attribute]] annotation identifiers (inside [[ ]]).
    public static final Set<String> ATTRIBUTES = new HashSet<>(Arrays.asList(
            "stage_in", "buffer", "texture", "sampler", "color",
            "position", "point_size", "clip_distance", "cull_distance",
            "vertex_id", "instance_id", "base_instance", "base_vertex",
            "amplification_count", "dispatch_quadgroups_per_threadgroup",
            "dispatch_simdgroups_per_threadgroup", "dispatch_threads_per_threadgroup",
            "thread_index_in_quadgroup", "thread_index_in_simdgroup",
            "thread_index_in_threadgroup", "thread_position_in_grid",
            "thread_position_in_threadgroup", "threadgroup_position_in_grid",
            "threads_per_grid", "threads_per_simdgroup", "threads_per_threadgroup",
            "quadgroup_index_in_threadgroup", "simdgroup_index_in_threadgroup",
            "index_in_threadgroup", "flat", "center_perspective", "center_no_perspective",
            "centroid_perspective", "centroid_no_perspective", "sample_perspective",
            "sample_no_perspective", "primitive_id", "viewport_array_index",
            "render_target_array_index", "front_facing", "point_coord", "sample_id",
            "sample_mask", "depth_qualifier", "early_fragment_tests",
            "max_total_threads_per_threadgroup", "visible_function_table_size",
            "mesh", "payload", "object_data", "grid_origin", "grid_size",
            "raster_order_group", "function_constant", "index"
    ));

    public static final Set<String> BUILT_IN_CONSTANTS = new HashSet<>(Arrays.asList(
            "true", "false", "nullptr", "INFINITY", "NAN", "MAXHALF", "MINHALF"
    ));

    public final Set<String> userDefinedTypes = new HashSet<>();
    public final Set<String> userDefinedFunctions = new HashSet<>();

    private static final Pattern CODE_PATTERN = Pattern.compile(
            "(\"(?:[^\"\\\\]|\\\\.)*\")"          // group 1 - string (escape-aware)
                    + "|('(?:[^'\\\\]|\\\\.)*')"   // group 2 - char literal
                    + "|([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?=\\()" // group 3 - call before '('
                    + "|([a-zA-Z_][a-zA-Z0-9_]*)"             // group 4 - plain identifier
                    + "|(0[xX][0-9a-fA-F]+[uUlL]*)"           // group 5 - hex literal
                    + "|(\\d+\\.?\\d*[fFuUlLhH]?(?:[eE][+-]?\\d+)?)" // group 6 - decimal
                    + "|([+\\-*/%=<>!&|^~?:]+)"               // group 7 - operator
                    + "|([()\\[\\]{}.,;])"                     // group 8 - punctuation
                    + "|(\\s+)"                                // group 9 - whitespace
    );

    private static final Pattern ATTR_PATTERN = Pattern.compile("\\[\\[\\s*([a-zA-Z_][a-zA-Z0-9_]*)");

    @Override
    public int getDefaultColor() { return COLOR_DEFAULT; }

    @Override
    protected void analyzeDocument(List<List<EditorGlyph>> lines) {
        userDefinedTypes.clear();
        userDefinedFunctions.clear();
        Pattern structPat = Pattern.compile("(?:struct|class|enum)\\s+([a-zA-Z_][a-zA-Z0-9_]*)");
        Pattern funcPat = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");
        for (List<EditorGlyph> line : lines) {
            String t = glyphsToString(line).trim();
            if (t.isEmpty() || t.startsWith("//")) continue;
            Matcher m = structPat.matcher(t);
            if (m.find()) userDefinedTypes.add(m.group(1));
            m = funcPat.matcher(t);
            while (m.find()) {
                String fn = m.group(2);
                if (!BUILT_IN_FUNCTIONS.contains(fn) && !KEYWORDS.contains(fn))
                    userDefinedFunctions.add(fn);
            }
        }
    }

    @Override
    protected void colorizeLineImpl(List<EditorGlyph> line, int lineIdx, String text) {
        String trimmed = text.stripLeading();
        if (trimmed.startsWith("#")) {
            for (EditorGlyph g : line) g.color = COLOR_PREPROCESSOR;
            return;
        }

        int commentAt = findLineCommentStart(text);
        String codePart = commentAt >= 0 ? text.substring(0, commentAt) : text;

        colorizeAttributes(line, codePart);
        colorizeCode(line, codePart, 0);

        if (commentAt >= 0)
            for (int i = commentAt; i < line.size(); i++) line.get(i).color = COLOR_COMMENT;
    }

    private void colorizeAttributes(List<EditorGlyph> line, String code) {
        Matcher m = ATTR_PATTERN.matcher(code);
        while (m.find()) {
            int atStart = m.start();
            if (atStart + 1 < line.size()) {
                line.get(atStart).color = COLOR_ATTRIBUTE;
                line.get(atStart + 1).color = COLOR_ATTRIBUTE;
            }
            int nameStart = m.start(1), nameEnd = m.end(1);
            for (int i = nameStart; i < nameEnd && i < line.size(); i++)
                line.get(i).color = COLOR_ATTRIBUTE;
            int end = code.indexOf("]]", nameEnd);
            if (end >= 0) {
                for (int i = nameEnd; i < end + 2 && i < line.size(); i++)
                    line.get(i).color = COLOR_ATTRIBUTE;
            }
        }
    }

    private void colorizeCode(List<EditorGlyph> line, String code, int offset) {
        Matcher m = CODE_PATTERN.matcher(code);
        int idx = 0;
        while (m.find()) {
            while (idx < m.start() && offset + idx < line.size()) idx++;
            int color = resolveColor(m);
            for (int i = m.start(); i < m.end() && offset + i < line.size(); i++) {
                if (line.get(offset + i).color != COLOR_ATTRIBUTE)
                    line.get(offset + i).color = color;
            }
            idx = m.end();
        }
        while (idx < code.length() && offset + idx < line.size()) {
            if (line.get(offset + idx).color != COLOR_ATTRIBUTE)
                line.get(offset + idx).color = COLOR_DEFAULT;
            idx++;
        }
    }

    private int resolveColor(Matcher m) {
        if (m.group(1) != null) return COLOR_STRING;
        if (m.group(2) != null) return COLOR_STRING;

        if (m.group(3) != null) {
            String tok = m.group(3);
            return BUILT_IN_FUNCTIONS.contains(tok) ? COLOR_FUNCTION_CALL : COLOR_FUNCTION_NAME;
        }

        if (m.group(4) != null) {
            String tok = m.group(4);
            if (KEYWORDS.contains(tok)) return COLOR_KEYWORD;
            if (BUILT_IN_TYPES.contains(tok)) return COLOR_BUILT_IN_TYPE;
            if (BUILT_IN_CONSTANTS.contains(tok)) return COLOR_BUILT_IN_CONST;
            if (ATTRIBUTES.contains(tok)) return COLOR_ATTRIBUTE;
            if (userDefinedTypes.contains(tok)) return COLOR_BUILT_IN_TYPE;
            if (userDefinedFunctions.contains(tok)) return COLOR_FUNCTION_NAME;
            return COLOR_USER_IDENT;
        }

        if (m.group(5) != null) return COLOR_NUMBER;
        if (m.group(6) != null) return COLOR_NUMBER;
        if (m.group(7) != null) return COLOR_OPERATOR;
        if (m.group(8) != null) return COLOR_OPERATOR;
        return COLOR_DEFAULT;
    }
}