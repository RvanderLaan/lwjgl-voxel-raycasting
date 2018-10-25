package core;

import lombok.Getter;

import org.lwjgl.opengl.*;

import java.util.HashMap;

public class Shader {

    @Getter
    private int programId;
    private HashMap<String, Integer> uniforms;

    public Shader(int programId) {
        this.programId = programId;
        uniforms = new HashMap<>();
    }

    public void use() {
        GL20.glUseProgram(programId);
    }

    public static void unuse() {
        GL20.glUseProgram(0);
    }

    public int getUniformId(String id) {
        if (!uniforms.containsKey(id)) {
            int uni = GL20.glGetUniformLocation(programId, id);
            uniforms.put(id, uni);
            return uni;
        }
        return uniforms.get(id);
    }

    public int getUniformBlockIndex(String id) {
        if (!uniforms.containsKey(id)) {
            int uni = GL31.glGetUniformBlockIndex(programId, id);
            uniforms.put(id, uni);
            return uni;
        }
        return uniforms.get(id);
    }
}
