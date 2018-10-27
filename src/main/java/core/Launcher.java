package core;

import input.CursorHandler;
import input.KeyboardHandler;
import input.MouseButtonHandler;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL33.glBindSampler;
import static org.lwjgl.opengl.GL33.glGenSamplers;
import static org.lwjgl.opengl.GL33.glSamplerParameteri;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL42.glBindImageTexture;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_WORK_GROUP_SIZE;
import static org.lwjgl.opengl.GL43.glDispatchCompute;
import static org.lwjgl.system.MathUtil.mathRoundPoT;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress;

public class Launcher {

    /** The GLFW window handle. */
    private long window;
    private int width = 800;
    private int height = 600;
    private int fps = 0;
    /**  Whether we need to recreate our ray tracer framebuffer. */
    private boolean resetFramebuffer = true;

    /** The OpenGL texture acting as our framebuffer for the ray tracer. */
    private int tex;
    /** A VAO simply holding a VBO for rendering a simple quad. */
    private int vao;
    /** The shader program handle of the compute shader. */
    private int computeProgram;
    /** The shader program handle of a fullscreen quad shader. */
    private int quadProgram;
    /** A sampler object to sample the framebuffer texture when finally presenting it
     * on the screen. */
    private int sampler;

    private Shader computeShader;

    /** The texture containing the voxelized data structure */
    private int voxelTexture;

    /** The location of the 'eye' uniform declared in the compute shader holding the
     * world-space eye position. */
//    private int eyeUniform;
    /** The location of the rayNN uniforms. These will be explained later. */
//    private int ray00Uniform, ray10Uniform, ray01Uniform, ray11Uniform;

    /** The binding point in the compute shader of the framebuffer image (level 0 of
     * the {@link #tex} texture). */
    private int framebufferImageBinding;
    /** Value of the work group size in X dimension declared in the compute shader. */
    private int workGroupSizeX;
    /** Value of the work group size in Y dimension declared in the compute shader. */
    private int workGroupSizeY;

    private Matrix4f projMatrix = new Matrix4f();
    private Matrix4f viewMatrix = new Matrix4f();
    private Matrix4f invViewProjMatrix = new Matrix4f();
    private Vector3f tmpVector = new Vector3f();

    private Camera camera;
    private Controller controller;
    private RenderController renderController;

    /*
     * All the GLFW callbacks we use to detect certain events, such as keyboard and
     * mouse events or window resize events.
     */
    private GLFWErrorCallback errCallback;
    private KeyboardHandler keyCallback;
    private GLFWFramebufferSizeCallback fbCallback;
    private CursorHandler cpCallback;
    private MouseButtonHandler mbCallback;

    /*
     * LWJGL's OpenGL debug callback object, which will get notified by OpenGL about
     * certain events, such as OpenGL errors, warnings or merely information.
     */
    private Callback debugProc;

    public Launcher() {
    }

    /** Do everything necessary once at the start of the application. */
    private void init() throws IOException {
        /*
         * Set a GLFW error callback to be notified about any error messages GLFW
         * generates.
         */
        glfwSetErrorCallback(errCallback = GLFWErrorCallback.createPrint(System.err));
        /*
         * Initialize GLFW itself.
         */
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        /*
         * And set some OpenGL context attributes, such as that we are using OpenGL 4.3.
         * This is the minimum core version such so that can use compute shaders.
         */
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // <- make the window visible explicitly later
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        /*
         * Now, create the window.
         */
        window = glfwCreateWindow(width, height, "SVO Ray tracing", NULL, NULL);
        if (window == NULL)
            throw new AssertionError("Failed to create the GLFW window");

        /* And set some GLFW callbacks to get notified about events. */

        glfwSetKeyCallback(window, keyCallback = new KeyboardHandler());

        /*
         * We need to get notified when the GLFW window framebuffer size changed (i.e.
         * by resizing the window), in order to recreate our own ray tracer framebuffer
         * texture.
         */
        glfwSetFramebufferSizeCallback(window, fbCallback = new GLFWFramebufferSizeCallback() {
            public void invoke(long window, int width, int height) {
                if (width > 0 && height > 0 && (Launcher.this.width != width || Launcher.this.height != height)) {
                    Launcher.this.width = width;
                    Launcher.this.height = height;
                    Launcher.this.resetFramebuffer = true;
                }
            }
        });

        glfwSetCursorPosCallback(window, cpCallback = new CursorHandler());

        glfwSetMouseButtonCallback(window, mbCallback = new MouseButtonHandler());

        /*
         * Center the created GLFW window on the screen.
         */
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // ctrl f vsync v-sync
        glfwShowWindow(window);

        /*
         * Account for HiDPI screens where window size != framebuffer pixel size.
         */
        try (MemoryStack frame = MemoryStack.stackPush()) {
            IntBuffer framebufferSize = frame.mallocInt(2);
            nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
            width = framebufferSize.get(0);
            height = framebufferSize.get(1);
        }

        GL.createCapabilities();
        debugProc = GLUtil.setupDebugMessageCallback();

        camera = new Camera(new Vector3f(0.5f, 0.5f, 0.05f));

        /* Create all needed GL resources */
        createFramebufferTexture();
        createSampler();
        quadFullScreenVao();
        createComputeProgram();
        initComputeProgram();
        createQuadProgram();
        initQuadProgram();
        SVO svo = createVoxelTexture();
        setStaticUniforms(svo);

        controller = new Controller(camera, svo);
        renderController = new RenderController(computeShader, RenderController.LookupMode.OCTREE);
    }

    private void setStaticUniforms(SVO svo) {
        float texSize = svo.getMaxTextureSize();

        glUseProgram(computeProgram);

        // Set number of indirection grids
        glUniform1f(computeShader.getUniformId("invNumberOfIndGrids"), 2f / texSize);

        // Set voxel texture size
        glUniform3f(
                computeShader.getUniformId("textureSize"),
                texSize,
                1 / texSize,
                1 / (2f * texSize)
        );

        glUseProgram(0);
    }

    private SVO createVoxelTexture() {
        SVO svo = new SVO(6, 100);
        int textureSize = svo.getMaxTextureSize();
        svo.generateDemoScene();
        svo.generateSVO();
//        System.out.println("textureSize + \", \" + invNumberOfIndGrids = " + textureSize + ", " + invNumberOfIndGrids);
        voxelTexture = SVO.uploadTexture(textureSize, svo.getTextureData());
        return svo;
    }

    private void update(float dt) {
        if (KeyboardHandler.isKeyPressed(GLFW_KEY_ESCAPE)) {
            glfwSetWindowShouldClose(window, true);
        }

        controller.update(dt);
        renderController.update(dt);

        // Reset raw input controllers
        KeyboardHandler.update();
        MouseButtonHandler.update();

        String camPosText = camera.getPosition().toString(new DecimalFormat("0.0"));
        glfwSetWindowTitle(window, "SVO Ray tracing - " + fps + " FPS - CamPos: " + camPosText);
    }

    /** Create a VAO with a full-screen quad VBO.
     */
    private void quadFullScreenVao() {
        /*
         * Really simple. Just a VAO with a VBO to render a full-screen quad as two
         * triangles.
         */
        this.vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        ByteBuffer bb = BufferUtils.createByteBuffer(4 * 2 * 6);
        FloatBuffer fv = bb.asFloatBuffer();
        fv.put(-1.0f).put(-1.0f);
        fv.put(1.0f).put(-1.0f);
        fv.put(1.0f).put(1.0f);
        fv.put(1.0f).put(1.0f);
        fv.put(-1.0f).put(1.0f);
        fv.put(-1.0f).put(-1.0f);
        glBufferData(GL_ARRAY_BUFFER, bb, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    /** Create the full-screen quad shader.
     */
    private void createQuadProgram() throws IOException {
        /*
         * Create program and shader objects for our full-screen quad rendering.
         */
        int program = glCreateProgram();
        int vshader = ResourceUtils.createShader("quad.vs.glsl", GL_VERTEX_SHADER, "330");
        int fshader = ResourceUtils.createShader("quad.fs.glsl", GL_FRAGMENT_SHADER, "330");
        glAttachShader(program, vshader);
        glAttachShader(program, fshader);
        glBindAttribLocation(program, 0, "vertex");
        glBindFragDataLocation(program, 0, "color");
        glLinkProgram(program);
        int linked = glGetProgrami(program, GL_LINK_STATUS);
        String programLog = glGetProgramInfoLog(program);
        if (programLog.trim().length() > 0) {
            System.err.println(programLog);
        }
        if (linked == 0) {
            throw new AssertionError("Could not link program");
        }
        this.quadProgram = program;
    }

    /** Create the tracing compute shader program. */
    private void createComputeProgram() throws IOException {
        /*
         * Create our GLSL compute shader. It does not look any different to creating a
         * program with vertex/fragment shaders. The only thing that changes is the
         * shader type, now being GL_COMPUTE_SHADER.
         */
        int program = glCreateProgram();
        int cshader = ResourceUtils.createShader("raytracing.glsl",
                GL_COMPUTE_SHADER);
        glAttachShader(program, cshader);
        glLinkProgram(program);
        int linked = glGetProgrami(program, GL_LINK_STATUS);
        String programLog = glGetProgramInfoLog(program);
        if (programLog.trim().length() > 0) {
            System.err.println(programLog);
        }
        if (linked == 0) {
            throw new AssertionError("Could not link program");
        }
        this.computeProgram = program;
        this.computeShader = new Shader(computeProgram);
    }

    /** Initialize the full-screen-quad program. This just binds the program briefly
     * to obtain the uniform locations. */
    private void initQuadProgram() {
        glUseProgram(quadProgram);
        int texUniform = glGetUniformLocation(quadProgram, "tex");
        glUniform1i(texUniform, 0);
        glUseProgram(0);
    }

    /** Initialize the compute shader. This just binds the program briefly to obtain
     * the uniform locations, the declared work group size values and the image
     * binding point of the framebuffer image. */
    private void initComputeProgram() {
        glUseProgram(computeProgram);
        IntBuffer workGroupSize = BufferUtils.createIntBuffer(3);
        glGetProgramiv(computeProgram, GL_COMPUTE_WORK_GROUP_SIZE, workGroupSize);
        workGroupSizeX = workGroupSize.get(0);
        workGroupSizeY = workGroupSize.get(1);

        /* Query the "image binding point" of the image uniform */
        IntBuffer params = BufferUtils.createIntBuffer(1);
        int loc = glGetUniformLocation(computeProgram, "framebufferImage");
        glGetUniformiv(computeProgram, loc, params);
        framebufferImageBinding = params.get(0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_3D, voxelTexture);
        glUseProgram(0);
    }

    /** Create the texture that will serve as our framebuffer that the compute shader
     * will write/render to.
     */
    private void createFramebufferTexture() {
        this.tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        /*
         * glTexStorage2D only allocates space for the texture, but does not initialize
         * it with any values. This is fine, because we use the texture solely as output
         * texture in the compute shader.
         */
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA32F, width, height);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /** Create the sampler to sample the framebuffer texture within the fullscreen
     * quad shader. We use NEAREST filtering since one texel on the framebuffer
     * texture corresponds exactly to one pixel on the GLFW window framebuffer.
     */
    private void createSampler() {
        this.sampler = glGenSamplers();
        glSamplerParameteri(this.sampler, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glSamplerParameteri(this.sampler, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }

    /** Recreate the framebuffer when the window size changes.
     */
    private void resizeFramebufferTexture() {
        glDeleteTextures(tex);
        createFramebufferTexture();
    }

    /** Compute one frame by tracing the scene using our compute shader.
     * The resulting pixels will be written to the framebuffer texture {@link #tex}.
     */
    private void trace() {
        glUseProgram(computeProgram);

        /* Rotate camera about Y axis. */
//        cameraPosition.set((float) sin(-currRotationAboutY) * 3.0f, 2.0f, (float) cos(-currRotationAboutY) * 3.0f).normalize();
        Vector3f lookat = camera.getRotation().transform(new Vector3f(0, 0, 1)).add(camera.getPosition());
        viewMatrix.setLookAt(camera.getPosition(), lookat, Camera.UP);

        /*
         * If the framebuffer size has changed, because the GLFW window was resized, we
         * need to reset the camera's projection matrix and recreate our framebuffer
         * texture.
         */
        if (resetFramebuffer) {
            projMatrix.setPerspective((float) Math.toRadians(60.0f), (float) width / height, 1f, 2f);
            resizeFramebufferTexture();
            resetFramebuffer = false;
        }
        /*
         * Invert the view-projection matrix to unproject NDC-space coordinates to
         * world-space vectors. See next few statements.
         */
        projMatrix.invertPerspectiveView(viewMatrix, invViewProjMatrix);

        /*
         * Compute and set the view frustum corner rays in the shader for the shader to
         * compute the direction from the eye through a framebuffer's pixel center for a
         * given shader work item.
         */
        glUniform3f(computeShader.getUniformId("eye"),
                camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
        invViewProjMatrix.transformProject(tmpVector.set(-1, -1, 0)).sub(camera.getPosition());
        glUniform3f(computeShader.getUniformId("ray00"),
                tmpVector.x, tmpVector.y, tmpVector.z);
        invViewProjMatrix.transformProject(tmpVector.set(-1, 1, 0)).sub(camera.getPosition());
        glUniform3f(computeShader.getUniformId("ray01"),
                tmpVector.x, tmpVector.y, tmpVector.z);
        invViewProjMatrix.transformProject(tmpVector.set(1, -1, 0)).sub(camera.getPosition());
        glUniform3f(computeShader.getUniformId("ray10"),
                tmpVector.x, tmpVector.y, tmpVector.z);
        invViewProjMatrix.transformProject(tmpVector.set(1, 1, 0)).sub(camera.getPosition());
        glUniform3f(computeShader.getUniformId("ray11"),
                tmpVector.x, tmpVector.y, tmpVector.z);


        // Set voxel texture location (TEXTURE0)
        glUniform1i(computeShader.getUniformId("voxelTexture"), 0);

        /*
         * Bind level 0 of framebuffer texture as writable image in the shader. This
         * tells OpenGL that any writes to the image defined in our shader is going to
         * go to the first level of the texture 'tex'.
         */
        glBindImageTexture(framebufferImageBinding, tex, 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);


        /*
         * Compute appropriate global work size dimensions. Because OpenGL only allows
         * to invoke a compute shader with a power-of-two global work size in each
         * dimension, we need to compute a size that is both a power-of-two and that
         * covers our complete framebuffer. We use LWJGL's built-in method
         * mathRoundPoT() for this.
         */
        int worksizeX = mathRoundPoT(width);
        int worksizeY = mathRoundPoT(height);

        /* Invoke the compute shader. */
        glDispatchCompute(worksizeX / workGroupSizeX, worksizeY / workGroupSizeY, 1);
        /*
         * Synchronize all writes to the framebuffer image before we let OpenGL source
         * texels from it afterwards when rendering the final image with the full-screen
         * quad.
         */
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

        /* Reset bindings. */
        glBindImageTexture(framebufferImageBinding, 0, 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);
        glUseProgram(0);
    }

    /** Present the final image on the default framebuffer of the GLFW window. */
    private void present() {
        /*
         * Draw the rendered image on the screen using a textured full-screen quad.
         */
        glUseProgram(quadProgram);
        glBindVertexArray(vao);
        glBindTexture(GL_TEXTURE_2D, tex);
        glBindSampler(0, this.sampler);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindSampler(0, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(0);
        glUseProgram(0);
    }

    private void loop() {

        long nanoStart = System.nanoTime();
        float dt = 1 / 60f;
        int fpsCounter = 0;
        long fpsNanoStart = System.nanoTime();

        // Our render loop is really simple...
        while (!glfwWindowShouldClose(window)) {
            // ...we just poll for GLFW window events (as usual).
            glfwPollEvents();

            update(dt); // todo: measure delta time

            // Tell OpenGL about any possibly modified viewport size.
            glViewport(0, 0, width, height);
            // Call the compute shader to trace the scene and produce an image in our
            // framebuffer texture.
            trace();
            // Finally we blit/render the framebuffer texture to the default window
            // framebuffer of the GLFW window.
            present();
            // Tell the GLFW window to swap buffers so that our rendered framebuffer texture
            // becomes visible.
            glfwSwapBuffers(window);

            long nanoNow = System.nanoTime();
            dt = (nanoNow - nanoStart) / 1e9f;

            nanoStart = System.nanoTime();

            if (nanoNow - fpsNanoStart > 1e9) {
                fpsNanoStart = nanoNow;
                fps = fpsCounter;
                fpsCounter = 0;
            }
            fpsCounter++;
        }
    }

    private void run() throws Exception {
        try {
            init();
            loop();
            if (debugProc != null)
                debugProc.free();
            errCallback.free();
            keyCallback.free();
            fbCallback.free();
            cpCallback.free();
            mbCallback.free();
            glfwDestroyWindow(window);
        } finally {
            glfwTerminate();
        }
    }

    public static void main(String[] args) throws Exception {
        new Launcher().run();
    }
}
