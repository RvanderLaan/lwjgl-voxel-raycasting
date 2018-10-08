package input;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFWCursorPosCallback;

public class CursorHandler extends GLFWCursorPosCallback {
    
    private static Vector2f cursorPos = new Vector2f();

    @Override
    public void invoke(long window, double xpos, double ypos) {
        cursorPos.x = (float) xpos;
        cursorPos.y = (float) ypos;
    }   
    
    public static Vector2f getCursorPos() {
        return cursorPos;
    }
}