package core;

import input.KeyboardHandler;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.opengl.GL41C.*;

public class RenderController {
    private Shader computeShader;
    private LookupMode lookupMode = LookupMode.OCTREE;

    enum LookupMode {
        OCTREE(1),
        TEXTURE_DIRECT(2),
        TEXTURE_LOOKUP(3);

        private int value;
        LookupMode(int value) {
            this.value = value;
        }
    }

    public RenderController(Shader computeShader, LookupMode lookupMode) {
        this.computeShader = computeShader;
        glProgramUniform1i(
                computeShader.getProgramId(),
                computeShader.getUniformId("lookupMode"),
                lookupMode.value);
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
    }
}
