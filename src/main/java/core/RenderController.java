package core;

import input.KeyboardHandler;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import static org.lwjgl.opengl.GL41C.*;

public class RenderController {
    private Shader computeShader;
    private LookupMode lookupMode = LookupMode.OCTREE;

    private int voxelTexture, voxelTextureDirect;
    private boolean isVoxelTextureActive = true;

    enum LookupMode {
        OCTREE(1),
        TEXTURE_DIRECT(2),
        TEXTURE_LOOKUP(3);

        private int value;
        LookupMode(int value) {
            this.value = value;
        }
    }

    public RenderController(Shader computeShader, LookupMode lookupMode, int voxTex, int voxTexDirect) {
        this.computeShader = computeShader;
        glProgramUniform1i(
                computeShader.getProgramId(),
                computeShader.getUniformId("lookupMode"),
                lookupMode.value);

        this.voxelTexture = voxTex;
        this.voxelTextureDirect = voxTexDirect;
    }

    public void update(float dt) {
        LookupMode newLookupMode = lookupMode;
        if (KeyboardHandler.isKeyPressed(GLFW.GLFW_KEY_1))
            newLookupMode = LookupMode.OCTREE;
        else if (KeyboardHandler.isKeyPressed(GLFW.GLFW_KEY_2))
            newLookupMode = LookupMode.TEXTURE_DIRECT;
        else if (KeyboardHandler.isKeyPressed(GLFW.GLFW_KEY_3))
            newLookupMode = LookupMode.TEXTURE_LOOKUP;

        if (lookupMode != newLookupMode) {
            lookupMode = newLookupMode;
            glProgramUniform1i(
                    computeShader.getProgramId(),
                    computeShader.getUniformId("lookupMode"),
                    lookupMode.value);
        }

        if (KeyboardHandler.isKeyPressed(GLFW.GLFW_KEY_T)) {
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, isVoxelTextureActive ? voxelTextureDirect : voxelTexture);
            isVoxelTextureActive = !isVoxelTextureActive;
        }
    }
}
