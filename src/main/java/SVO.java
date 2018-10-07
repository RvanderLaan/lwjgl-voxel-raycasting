import lombok.Getter;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Implementation based on https://developer.nvidia.com/gpugems/GPUGems2/gpugems2_chapter37.html
 */
public class SVO {

    @Getter
    private ArrayList<IndirectionGrid> indirectionPool;
    @Getter
    private int maxDepth;
    @Getter
    private int worldSize;

    @Getter
    private ArrayList<Geometry> geometries;

    /**
     * Creates a Sparse Voxel Octree
     * @param maxDepth
     * @param worldSize
     */
    public SVO(int maxDepth, int worldSize) {
        this.maxDepth = maxDepth;
        this.worldSize = worldSize;
        this.geometries = new ArrayList<>();
        this.indirectionPool = new ArrayList<>(1024);
    }

    public void generateDemoScene() {
        geometries.add(new Sphere(new Vector3f(worldSize / 2f), worldSize / 3f, new Vector3f(1, 0, 1)));
    }

    public void generateSVO() {
        createNode(0, new Vector3f(0));
    }

    private static Vector3f[] childBoxOffsets = {
            new Vector3f(0, 0, 0),
            new Vector3f(1, 0, 0),
            new Vector3f(0, 1, 0),
            new Vector3f(1, 1, 0),
            new Vector3f(0, 0, 1),
            new Vector3f(1, 0, 1),
            new Vector3f(0, 1, 1),
            new Vector3f(1, 1, 1),
    };

    /**
     * Recursively creates nodes in the SVO from the specified geometries
     * @param depth
     * @param boxStart
     */
    protected void createNode(int depth, Vector3f boxStart) {
        IndirectionGrid ig = new IndirectionGrid();
        indirectionPool.add(ig);
        int maxTextureSize = getMaxTextureSize();

        // The size of a child box is worldSize / 2^D, e.g. 1 -> 0.5 -> 0.25 -> 0.125 -> ...
        float childBoxSize = worldSize / (float) Math.pow(2, depth + 1);

        // Loop over all 8 sub-nodes
        for (int i = 0; i < 8; i++) {
            Vector3f childBoxStart = childBoxOffsets[i].mul(childBoxSize, new Vector3f()).add(boxStart);
            Vector3f childBoxEnd   = new Vector3f(childBoxSize).add(childBoxStart);

            // Find intersection of this child node with any geometry
            Geometry intersection = geometries.stream()
                    .filter((g -> g.intersects(childBoxStart, childBoxEnd)))
                    .findFirst()
                    .orElse(null);

            // Todo: Should it be + 1 or not?
            if (depth + 1 != maxDepth) {
                // If not at max depth, check whether the child node should be subdivided
                if (intersection != null) {
                    int listIndex = indirectionPool.size();
                    Vector3f textureIndex = new Vector3f(
                            listIndex % maxTextureSize,
                            (listIndex / 2) % maxTextureSize,
                            (listIndex / 4) % maxTextureSize
                    ).mul(1 / (float) maxTextureSize);
                    // Create a link from the child node to the next indirection grid
                    ig.setNode(i, Cell.createIndex(textureIndex));
                    // Subdivide the child node: Add a new intersection grid
                    createNode(depth + 1, childBoxStart);
                }
            } else {
                // If at max depth, possibly add a data node
                if (intersection != null) {
                    // Create a data node with the color of the geometry
                    ig.setNode(i, Cell.createData(intersection.getColor()));
                }
            }
        }

    }

    public int getMaxTextureSize() {
        int minTextureSize = (int) Math.ceil(Math.pow(indirectionPool.size() * 8 * 4, 1/3f));
        return nextPowerOfTwo(minTextureSize);
    }

    private static int nextPowerOfTwo(int value) {
        int highestOneBit = Integer.highestOneBit(value);
        if (value == highestOneBit) {
            return value;
        }
        return highestOneBit << 1;
    }

    public ByteBuffer getTextureData() {
//        System.out.println("Generating texture...");
        System.out.println("max depth: " + maxDepth + ", " + "indirectionPool size: " + indirectionPool.size());

        int textureSize = getMaxTextureSize();
        System.out.println("textureSize: " + textureSize + "^3 = " + (int) Math.pow(textureSize, 3));

        // In a worst case scenario, all octree nodes are used, so the tree is subdivided to maxDepth everywhere
//        int size = getMaxTextureSize();
        ByteBuffer textureData = BufferUtils.createByteBuffer(textureSize * textureSize * textureSize); // 4 bytes since r g b a

        for (IndirectionGrid ig : indirectionPool) {
            ig.get(textureData);
        }
        textureData.flip();
        return textureData;
    }

    public static int uploadTexture(int size, ByteBuffer textureData) {
        int texID = GL11.glGenTextures();

        GL11.glEnable(GL12.GL_TEXTURE_3D);
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, texID);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_WRAP_R, GL11.GL_REPEAT);
        GL12.glTexImage3D(GL12.GL_TEXTURE_3D, 0, GL11.GL_RGBA, size, size, size, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, textureData);

        return texID;
    }
}
