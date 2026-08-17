#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;
uniform mat4 g_WorldMatrix;
uniform mat3 g_NormalMatrix;

#ifdef OWNER_TINT
    uniform vec4 m_OwnerTint;
#endif

in vec3 inPosition;
in vec3 inNormal;
in vec2 inTexCoord;

#ifdef VERTEX_COLOR
    in vec4 inColor;
#endif

out vec2 texCoord;
out vec4 vertColor;
out vec3 wvNormal;
out float fog_z;

void main() {
    vec4 modelSpacePos = vec4(inPosition, 1.0);
    gl_Position = g_WorldViewProjectionMatrix * modelSpacePos;

    texCoord = inTexCoord;

    #ifdef VERTEX_COLOR
        vertColor = inColor;
    #else
        vertColor = vec4(1.0);
    #endif

    #ifdef OWNER_TINT
        vertColor *= m_OwnerTint;
    #endif

    wvNormal = normalize(g_NormalMatrix * inNormal);

    // Fog depth
    vec4 worldPos = g_WorldMatrix * modelSpacePos;
    fog_z = length((g_WorldViewProjectionMatrix * modelSpacePos).xyz);
}
