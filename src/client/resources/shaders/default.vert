#version 330 core

// Input vertex data (per-vertex)
layout (location=0) in vec3 position;
layout (location=1) in vec2 baseTexCoord; // Base UVs from the mesh VBO
layout (location=2) in vec3 normal;

// Input instance data (per-instance)
// A mat4 is passed as four vec4 attributes
layout (location=3) in vec4 instanceModelMatrixCol0;
layout (location=4) in vec4 instanceModelMatrixCol1;
layout (location=5) in vec4 instanceModelMatrixCol2;
layout (location=6) in vec4 instanceModelMatrixCol3;
layout (location=7) in vec2 instanceUvOffset; // (u0, v0) for this instance's texture region
layout (location=8) in vec2 instanceUvScale;  // (widthUV, heightUV) for this instance's texture region

// Uniforms
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

// Outputs sent to the fragment shader
out vec2 outTexCoord;
out vec3 outNormal;

void main()
{
    mat4 instanceModelMatrix = mat4(
        instanceModelMatrixCol0,
        instanceModelMatrixCol1,
        instanceModelMatrixCol2,
        instanceModelMatrixCol3
    );

    // Calculate the final vertex position in clip space
    gl_Position = projectionMatrix * viewMatrix * instanceModelMatrix * vec4(position, 1.0);

    // Pass other attributes to fragment shader
    outTexCoord = instanceUvOffset + baseTexCoord * instanceUvScale;
    outNormal = mat3(transpose(inverse(instanceModelMatrix))) * normal;
}