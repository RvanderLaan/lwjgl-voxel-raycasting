package core;

import lombok.Getter;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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
        geometries.add(new Sphere(new Vector3f(worldSize / 2f), worldSize / 2f, new Vector3f(0.5f, 0.5f, 0.5f)));
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
     * Every indirection grid is located in texture memory at a 2x2x2 cube, starting at 0, 0, 0
     * @param indirectionGridIndex
     * @return
     */
    protected Vector3f getTextureIndex(int indirectionGridIndex) {
        int textureSize = getMaxTextureSize();
        int halfTextureSize = textureSize / 2;
        int i = indirectionGridIndex;
        int x = (i % halfTextureSize) * 2;
        int y = ((i / halfTextureSize) % halfTextureSize) * 2;
        int z = ((i / (halfTextureSize * halfTextureSize))) * 2;
        return new Vector3f(x, y, z).div((float) textureSize);
    }

    /**
     * Recursively creates nodes in the core.SVO from the specified geometries.
     *
     * Algorithm: For each cell of this grid, check if in intersects with any geometry.
     * If it does not, that cell can be marked as empty.
     * If it does, two things can happen:
     *      If at max depth, this cell becomes a data cell ,representing the color of the geometry
     *      Else, the cell becomes a link to a new indirection grid that is located in this cell
     *
     * @param depth
     * @param boxStart
     */
    protected int createNode(int depth, Vector3f boxStart) {
        int currentIGIndex = indirectionPool.size();
        IndirectionGrid ig = new IndirectionGrid();
        indirectionPool.add(ig);

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

            if (depth + 1 != maxDepth) {
                // If not at max depth, check whether the child node should be subdivided
                if (intersection != null) {
                    // Subdivide the child node: Add a new intersection grid
                    int newIGIndex = createNode(depth + 1, childBoxStart);

                    // Create a link from the child node to the next indirection grid
                    // The texture index is the IG at current pool index + 1, which is the size()
                    Vector3f textureIndex = getTextureIndex(newIGIndex);
                    ig.setNode(i, Cell.createIndex(textureIndex));
                }
            } else {
                // If at max depth, possibly add a data node
                if (intersection != null) {
                    // Create a data node with the color of the geometry
                    Vector3f color = new Vector3f(intersection.getColor());
//                    color.add(
//                            new Vector3f((float) Math.random(), (float) Math.random(), (float) Math.random())
//                                    .sub(new Vector3f(0.5f))
//                                    .mul(0.5f));
                    color.set(childBoxStart).div(worldSize);
                    ig.setNode(i, Cell.createData(color));
                }
            }
        }
        return currentIGIndex;
    }

    public int getMaxTextureSize() {
        // At the start it is not known how many IGs there are, so worst case scenario:
        // every cell contains data
        // This means:
        // Every depth step, 2 new cells per dimension
        // for every data cell, log(N) index cells
        int n = (int) Math.pow(2, maxDepth - 1);
        int logN = (int) Math.log(n);
        int textureSize = n + logN;
        return nextPowerOfTwo(textureSize);
    }

    private static int nextPowerOfTwo(int value) {
        int highestOneBit = Integer.highestOneBit(value);
        if (value == highestOneBit) {
            return value;
        }
        return highestOneBit << 1;
    }

    public ByteBuffer getTextureData() {
        // Todo: Each cell should be placed in memory as they are in space: 2x2x2 cube, instead of
        // 8 values in a row as is happening now

//        System.out.println("Generating texture...");
        System.out.println("max depth: " + maxDepth + ", " + "indirectionPool size: " + indirectionPool.size());

        int textureSize = getMaxTextureSize();
        System.out.println("textureSize: " + textureSize + "^3 = " + (int) Math.pow(textureSize, 3));

        // In a worst case scenario, all octree nodes are used, so the tree is subdivided to maxDepth everywhere
//        int size = getMaxTextureSize();
        ByteBuffer textureData = BufferUtils.createByteBuffer(textureSize * textureSize * textureSize * 4); // 4 bytes since r g b a

        // Half texture size since each cell is 2x2x2, so in each dimension it takes up 2 spaces
        int halfTextureSize = textureSize / 2;
        int x = 0, y = 0, z = 0;
        for (int i = 0; i < indirectionPool.size(); i++) {
            x = (i % halfTextureSize) * 2;
            y = ((i / halfTextureSize) % halfTextureSize) * 2;
            z = ((i / (halfTextureSize * halfTextureSize))) * 2;

//            System.out.println("Indir Grid " + i + " loc: " + x + ", " + y + ", " + z + " -> " + getTextureIndex(i).toString(new DecimalFormat("0.00")));

            // Insert pool cells in a cube in texture memory
            indirectionPool.get(i).get(textureSize, x, y, z, textureData);
        }

        int bytesLeft = textureData.limit() - IndirectionGrid.getTextureIndex(textureSize, x, y, z, 7);
        System.out.println("Bytes left: " + bytesLeft + "(=" + (bytesLeft / 4) / 8 + " left over IRs out of " + ((textureData.limit() / 4) / 8) + ")");
//        textureData.flip();
        return textureData;
    }

    public static int uploadTexture(int size, ByteBuffer textureData) {
        int texID = GL11.glGenTextures();

        GL11.glEnable(GL12.GL_TEXTURE_3D);
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, texID);
//        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
//        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_WRAP_R, GL11.GL_REPEAT);
        GL12.glTexImage3D(GL12.GL_TEXTURE_3D, 0, GL11.GL_RGBA, size, size, size, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, textureData);

        return texID;
    }
}
