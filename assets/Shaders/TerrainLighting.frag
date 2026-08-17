#import "Common/ShaderLib/GLSLCompat.glsllib"

#ifdef DIFFUSEMAP
    uniform sampler2D m_DiffuseMap;
#endif

#ifdef DISCARD_ALPHA
    uniform float m_AlphaDiscardThreshold;
#endif

#ifdef USE_FOG
    uniform vec4 m_FogColor;
    #ifdef FOG_LINEAR
        uniform vec2 m_LinearFog;
    #endif
    #ifdef FOG_EXP
        uniform float m_ExpFog;
    #endif
    #ifdef FOG_EXPSQ
        uniform float m_ExpSqFog;
    #endif
#endif

in vec2 texCoord;
in vec4 vertColor;
in vec3 wvNormal;
in float fog_z;

void main() {
    vec4 diffuseColor = vec4(1.0);

    #ifdef DIFFUSEMAP
        diffuseColor = texture2D(m_DiffuseMap, texCoord);
    #endif

    #ifdef DISCARD_ALPHA
        if (diffuseColor.a <= m_AlphaDiscardThreshold) {
            discard;
        }
    #endif

    // Multiply by vertex color (baked terrain lighting from torches/lava)
    // and owner tint (applied in vertex shader)
    diffuseColor *= vertColor;

    // Fog
    #ifdef USE_FOG
        float fogFactor;
        #ifdef FOG_LINEAR
            fogFactor = (m_LinearFog.y - fog_z) / (m_LinearFog.y - m_LinearFog.x);
        #endif
        #ifdef FOG_EXP
            fogFactor = exp(-m_ExpFog * fog_z);
        #endif
        #ifdef FOG_EXPSQ
            fogFactor = exp(-m_ExpSqFog * fog_z * fog_z);
        #endif
        fogFactor = clamp(fogFactor, 0.0, 1.0);
        diffuseColor = mix(m_FogColor, diffuseColor, fogFactor);
    #endif

    gl_FragColor = diffuseColor;
}
