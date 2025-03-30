#version 330 core

// Inputs from the vertex shader
// in vec2 outTexCoord;
// in vec3 outNormal; // If needed

// Uniforms
uniform vec3 objectColor; // Example uniform for a solid color

// Output color for the fragment
out vec4 FragColor;

// uniform sampler2D texture_sampler; // If using textures

void main()
{
    // Simple: Output a fixed color set by uniform
    FragColor = vec4(objectColor, 1.0);

    // Example using texture:
    // FragColor = texture(texture_sampler, outTexCoord);

    // Example simple lighting (requires normals):
    // vec3 lightDir = normalize(vec3(1, 1, 1));
    // float diff = max(dot(normalize(outNormal), lightDir), 0.2); // Ambient term 0.2
    // FragColor = vec4(objectColor * diff, 1.0);
}
