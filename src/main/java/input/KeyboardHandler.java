package input;

import org.lwjgl.glfw.GLFWKeyCallback;

import java.util.LinkedList;
import java.util.Queue;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class KeyboardHandler extends GLFWKeyCallback {
    // Only true once when key is released
    public static boolean[] release = new boolean[65536];
    // Only true once when key is pushed
    public static boolean[] push = new boolean[65536];
    // True whenever a key is pressed down
    public static boolean[] down = new boolean[65536];
    
    // To 'unpress' keys
    private static Queue<Integer> liftRelease  = new LinkedList<Integer>(); 
    private static Queue<Integer> liftPush     = new LinkedList<Integer>(); 
    
    @Override
    public void invoke(long window, int key, int scancode, int action, int mods) {
        if (key < 0)
            return;
        
        if (action == GLFW_PRESS) {
            push[key] = true;
            liftPush.add(key);
        } else if (action == GLFW_RELEASE) {
            release[key] = true;
            liftRelease.add(key);
        }

        down[key] = action != GLFW_RELEASE;
    }
    
    public static void update() {
        for (int i = 0; i < liftRelease.size(); i++) 
            release[liftRelease.poll()] = false;
        for (int i = 0; i < liftPush.size(); i++) 
            push[liftPush.poll()] = false;
    }

    /**  Only true once when key is released */
    public static boolean isKeyReleased(int keycode) {
        return release[keycode];
    }
    /**  Only true once when key is pushed */
    public static boolean isKeyPressed(int keycode) {
        return push[keycode];
    }
    /** True whenever a key is pressed down */
    public static boolean isKeyDown(int keycode) {
        return down[keycode];
    }
}
