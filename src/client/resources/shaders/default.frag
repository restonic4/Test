#version 330 core

// Inputs from the vertex shader
in vec2 outTexCoord;
in vec3 outNormal;

// Uniforms
uniform vec3 tintColor;
uniform sampler2D textureSampler;

// Output color for the fragment
out vec4 FragColor;

void main()
{
    vec4 texColor = texture(textureSampler, outTexCoord);

    vec3 finalRGB = texColor.rgb * tintColor;
    float finalAlpha = texColor.a;

    FragColor = vec4(finalRGB, finalAlpha);
    //FragColor = vec4(1.0, finalRGB.g, 1.0, 1.0);

    // Example simple lighting (requires normals):
    // vec3 lightDir = normalize(vec3(1, 1, 1));
    // float diff = max(dot(normalize(outNormal), lightDir), 0.2); // Ambient term 0.2
    // FragColor = vec4(tintColor * diff, 1.0);
}
