package input;

import org.lwjgl.glfw.GLFWScrollCallback;

public class ScrollHandler extends GLFWScrollCallback {
    
    public static double[] offset = new double[2];
    

    @Override
    public void invoke(long window, double xoffset, double yoffset) {
        offset[0] = xoffset;
        offset[1] = yoffset;
    }
    
    public static void update() {
        offset[0] = 0;
        offset[1] = 0;
    }
}
