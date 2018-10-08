package input;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

import java.util.LinkedList;
import java.util.Queue;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class MouseButtonHandler extends GLFWMouseButtonCallback {
    
    // Only true once when key is released
    public static boolean[] release = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST+1];
    // Only true once when key is pushed
    public static boolean[] push = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST+1];
    // True whenever a key is pressed down
    public static boolean[] down = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST+1];
    
    // To 'unpress' keys
    private static Queue<Integer> liftRelease  = new LinkedList<Integer>(); 
    private static Queue<Integer> liftPush     = new LinkedList<Integer>();

    @Override
    public void invoke(long window, int button, int action, int mods) {
        if (button < 0)
            return;
        
        if (action == GLFW_PRESS) {
            push[button] = true;
            liftPush.add(button);
        } else if (action == GLFW_RELEASE) {
            release[button] = true;
            liftRelease.add(button);
        }

        down[button] = action != GLFW_RELEASE;
    }
    
    public static void update() {
        for (int i = 0; i < liftRelease.size(); i++) 
            release[liftRelease.poll()] = false;
        for (int i = 0; i < liftPush.size(); i++) 
            push[liftPush.poll()] = false;
    }
    
    /**  Only true once when key is released */
    public static boolean isButtonReleased(int keycode) {
        return release[keycode];
    }
    /**  Only true once when key is pushed */
    public static boolean isButtonPressed(int keycode) {
        return push[keycode];
    }
    /** True whenever a key is pressed down */
    public static boolean isButtonDown(int keycode) {
        return down[keycode];
    }

}
