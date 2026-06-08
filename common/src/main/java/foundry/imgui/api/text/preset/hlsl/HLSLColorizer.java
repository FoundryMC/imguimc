package foundry.imgui.api.text.preset.hlsl;

import foundry.imgui.api.text.color.AbstractBaseColorizer;
import foundry.imgui.api.text.editor.EditorGlyph;

import java.util.*;
import java.util.regex.*;

public final class HLSLColorizer extends AbstractBaseColorizer {
    // ABGR
    public static final int COLOR_DEFAULT = 0xFFC6B7A9;
    public static final int COLOR_KEYWORD = 0xFF8585FC;
    public static final int COLOR_BUILT_IN_TYPE = 0xFF71C0F6;
    public static final int COLOR_STRING = 0xFF74DBE6;
    public static final int COLOR_PREPROCESSOR = 0xFF7426F9;
    public static final int COLOR_NUMBER = 0xFF5DACA2;
    public static final int COLOR_FUNCTION_CALL = 0xFF40A885;
    public static final int COLOR_FUNCTION_NAME = 0xFFBA769A;
    public static final int COLOR_BUILT_IN_VAR = 0xFFCBE7A3;
    public static final int COLOR_BUILT_IN_CONST = 0xFF86D9B9;
    public static final int COLOR_SEMANTIC = 0xFFE0C080;
    public static final int COLOR_USER_IDENT = 0xFFC79565;
    public static final int COLOR_OPERATOR = 0xFFC6B7A9;
    public static final int COLOR_COMMENT = 0xFF888888;
    public static final int COLOR_COMMENT_MULTI = 0xFF557962;

    public static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "if", "else", "for", "while", "do", "switch", "case", "default",
            "break", "continue", "return", "discard",
            "in", "out", "inout", "uniform", "static", "extern", "volatile",
            "const", "inline", "nointerpolation", "linear", "centroid",
            "noperspective", "sample", "globallycoherent", "groupshared",
            "precise", "snorm", "unorm", "row_major", "column_major",
            "struct", "cbuffer", "tbuffer", "interface", "class", "namespace",
            "typedef", "export", "import",
            "numthreads"
    ));

    public static final Set<String> BUILT_IN_TYPES = new HashSet<>(Arrays.asList(
            "void", "bool", "int", "uint", "float", "double", "half",
            "min16float", "min10float", "min16int", "min12int", "min16uint",
            "bool1", "bool2", "bool3", "bool4",
            "int1", "int2", "int3", "int4",
            "uint1", "uint2", "uint3", "uint4",
            "half1", "half2", "half3", "half4",
            "float1", "float2", "float3", "float4",
            "double1", "double2", "double3", "double4",
            "float1x1", "float1x2", "float1x3", "float1x4",
            "float2x1", "float2x2", "float2x3", "float2x4",
            "float3x1", "float3x2", "float3x3", "float3x4",
            "float4x1", "float4x2", "float4x3", "float4x4",
            "int2x2", "int3x3", "int4x4",
            "double2x2", "double3x3", "double4x4",
            "vector", "matrix",
            "Texture1D", "Texture1DArray", "Texture2D", "Texture2DArray",
            "Texture2DMS", "Texture2DMSArray", "Texture3D", "TextureCube", "TextureCubeArray",
            "RWTexture1D", "RWTexture1DArray", "RWTexture2D", "RWTexture2DArray", "RWTexture3D",
            "Buffer", "RWBuffer", "ByteAddressBuffer", "RWByteAddressBuffer",
            "StructuredBuffer", "RWStructuredBuffer", "AppendStructuredBuffer", "ConsumeStructuredBuffer",
            "Sampler", "SamplerState", "SamplerComparisonState",
            "InputPatch", "OutputPatch", "PointStream", "LineStream", "TriangleStream",
            "RaytracingAccelerationStructure", "RayQuery"
    ));

    public static final Set<String> BUILT_IN_FUNCTIONS = new HashSet<>(Arrays.asList(
            "abs", "acos", "all", "any", "asin", "atan", "atan2",
            "ceil", "clamp", "cos", "cosh", "cross",
            "ddx", "ddx_coarse", "ddx_fine", "ddy", "ddy_coarse", "ddy_fine",
            "degrees", "determinant", "distance", "dot",
            "exp", "exp2", "faceforward", "floor", "fmod", "frac",
            "frexp", "fwidth", "isfinite", "isinf", "isnan",
            "ldexp", "length", "lerp", "log", "log10", "log2",
            "max", "min", "modf", "mul", "noise", "normalize",
            "pow", "radians", "rcp", "reflect", "refract",
            "reversebits", "round", "rsqrt", "saturate", "sign",
            "sin", "sincos", "sinh", "smoothstep", "sqrt", "step",
            "tan", "tanh", "transpose", "trunc",
            "Sample", "SampleBias", "SampleCmp", "SampleCmpLevelZero",
            "SampleGrad", "SampleLevel", "Load", "Gather", "GatherRed",
            "GatherGreen", "GatherBlue", "GatherAlpha", "GetDimensions",
            "CalculateLevelOfDetail", "CalculateLevelOfDetailUnclamped",
            "InterlockedAdd", "InterlockedAnd", "InterlockedCompareExchange",
            "InterlockedExchange", "InterlockedMax", "InterlockedMin", "InterlockedOr", "InterlockedXor",
            "clip", "countbits", "firstbithigh", "firstbitlow",
            "D3DCOLORtoUBYTE4", "f16tof32", "f32tof16",
            "AllMemoryBarrier", "AllMemoryBarrierWithGroupSync",
            "DeviceMemoryBarrier", "DeviceMemoryBarrierWithGroupSync",
            "GroupMemoryBarrier", "GroupMemoryBarrierWithGroupSync",
            "WaveActiveAllEqual", "WaveActiveAnyTrue", "WaveActiveAllTrue",
            "WaveActiveBallot", "WaveActiveCountBits", "WaveActiveMax",
            "WaveActiveMin", "WaveActiveProduct", "WaveActiveSum",
            "WavePrefixCountBits", "WavePrefixProduct", "WavePrefixSum",
            "WaveIsFirstLane", "WaveGetLaneCount", "WaveGetLaneIndex",
            "WaveReadLaneAt", "WaveReadLaneFirst",
            "TraceRay", "ReportHit", "IgnoreHit", "AcceptHitAndEndSearch",
            "CallShader", "DispatchRaysIndex", "DispatchRaysDimensions",
            "WorldRayOrigin", "WorldRayDirection", "RayTMin", "RayTCurrent",
            "RayFlags", "InstanceIndex", "InstanceID", "GeometryIndex",
            "PrimitiveIndex", "ObjectRayOrigin", "ObjectRayDirection",
            "ObjectToWorld3x4", "ObjectToWorld4x3", "WorldToObject3x4", "WorldToObject4x3"
    ));

    public static final Set<String> SEMANTICS = new HashSet<>(Arrays.asList(
            "SV_Position", "SV_Target", "SV_Target0", "SV_Target1", "SV_Target2",
            "SV_Target3", "SV_Target4", "SV_Target5", "SV_Target6", "SV_Target7",
            "SV_Depth", "SV_DepthGreaterEqual", "SV_DepthLessEqual",
            "SV_Coverage", "SV_InnerCoverage", "SV_StencilRef",
            "SV_VertexID", "SV_InstanceID", "SV_PrimitiveID", "SV_GSInstanceID",
            "SV_OutputControlPointID", "SV_DomainLocation", "SV_TessFactor",
            "SV_InsideTessFactor", "SV_IsFrontFace", "SV_SampleIndex",
            "SV_ClipDistance", "SV_CullDistance", "SV_RenderTargetArrayIndex",
            "SV_ViewportArrayIndex", "SV_DispatchThreadID", "SV_GroupID",
            "SV_GroupIndex", "SV_GroupThreadID",
            "POSITION", "NORMAL", "TEXCOORD", "TEXCOORD0", "TEXCOORD1", "TEXCOORD2",
            "TEXCOORD3", "COLOR", "COLOR0", "COLOR1", "TANGENT", "BINORMAL",
            "BLENDWEIGHT", "BLENDINDICES", "PSIZE", "FOG", "TESSFACTOR"
    ));

    public static final Set<String> BUILT_IN_CONSTANTS = new HashSet<>(Arrays.asList(
            "true", "false", "NULL"
    ));

    public final Set<String> userDefinedTypes = new HashSet<>();
    public final Set<String> userDefinedFunctions = new HashSet<>();

    private static final Pattern CODE_PATTERN = Pattern.compile(
            "(\"[^\"]*\")"
                    + "|([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?=\\()"
                    + "|([a-zA-Z_][a-zA-Z0-9_]*)"
                    + "|(\\d+\\.?\\d*[fFuUlLhH]?(?:[eE][+-]?\\d+)?)"
                    + "|(0[xX][0-9a-fA-F]+[uU]?)"
                    + "|([+\\-*/%=<>!&|^~?:]+)"
                    + "|([()\\[\\]{}.,;])"
                    + "|(\\s+)"
    );

    @Override
    public int getDefaultColor() { return COLOR_DEFAULT; }

    @Override
    protected void analyzeDocument(List<List<EditorGlyph>> lines) {
        userDefinedTypes.clear();
        userDefinedFunctions.clear();
        Pattern structPat = Pattern.compile("(?:struct|cbuffer|tbuffer)\\s+([a-zA-Z_][a-zA-Z0-9_]*)");
        Pattern funcPat = Pattern.compile(
                "(?:void|float\\d*|int\\d*|uint\\d*|bool\\d*|double\\d*|half\\d*|" +
                        "float\\dx\\d|[a-zA-Z_][a-zA-Z0-9_]*)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");
        for (List<EditorGlyph> line : lines) {
            String t = glyphsToString(line).trim();
            if (t.isEmpty() || t.startsWith("//")) continue;
            Matcher m = structPat.matcher(t);
            if (m.find()) userDefinedTypes.add(m.group(1));
            m = funcPat.matcher(t);
            if (m.find()) {
                String fn = m.group(1);
                if (!BUILT_IN_FUNCTIONS.contains(fn)) userDefinedFunctions.add(fn);
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
        colorizeCode(line, codePart, 0);

        if (commentAt >= 0)
            for (int i = commentAt; i < line.size(); i++) line.get(i).color = COLOR_COMMENT;
    }

    private void colorizeCode(List<EditorGlyph> line, String code, int offset) {
        Matcher m = CODE_PATTERN.matcher(code);
        int idx = 0;
        while (m.find()) {
            while (idx < m.start() && offset + idx < line.size())
                line.get(offset + idx++).color = COLOR_DEFAULT;
            int color = resolveColor(m);
            for (int i = m.start(); i < m.end() && offset + i < line.size(); i++)
                line.get(offset + i).color = color;
            idx = m.end();
        }
        while (idx < code.length() && offset + idx < line.size())
            line.get(offset + idx++).color = COLOR_DEFAULT;
    }

    private int resolveColor(Matcher m) {
        if (m.group(1) != null) return COLOR_STRING;

        if (m.group(2) != null) {
            String tok = m.group(2);
            return BUILT_IN_FUNCTIONS.contains(tok) ? COLOR_FUNCTION_CALL : COLOR_FUNCTION_NAME;
        }

        if (m.group(3) != null) {
            String tok = m.group(3);
            if (KEYWORDS.contains(tok)) return COLOR_KEYWORD;
            if (BUILT_IN_TYPES.contains(tok)) return COLOR_BUILT_IN_TYPE;
            if (SEMANTICS.contains(tok)) return COLOR_SEMANTIC;
            if (BUILT_IN_CONSTANTS.contains(tok)) return COLOR_BUILT_IN_CONST;
            if (userDefinedTypes.contains(tok)) return COLOR_BUILT_IN_TYPE;
            if (userDefinedFunctions.contains(tok)) return COLOR_FUNCTION_NAME;
            if (tok.startsWith("SV_") || tok.startsWith("sv_")) return COLOR_SEMANTIC;
            return COLOR_USER_IDENT;
        }

        if (m.group(4) != null) return COLOR_NUMBER;
        if (m.group(5) != null) return COLOR_NUMBER;
        if (m.group(6) != null) return COLOR_OPERATOR;
        if (m.group(7) != null) return COLOR_OPERATOR;
        return COLOR_DEFAULT;
    }
}