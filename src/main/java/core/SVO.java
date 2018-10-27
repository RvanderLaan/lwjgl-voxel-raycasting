package core;

import geometry.Geometry;
import lombok.Getter;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import geometry.*;

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
        this.indirectionPool = new ArrayList<>(8 * maxDepth * maxDepth);
    }

    public void generateDemoScene() {
        geometries.add(new Sphere(new Vector3f(worldSize / 2f), worldSize / 3f, new Vector3f(0.5f, 0.5f, 0.5f)));
//        geometries.add(new Line(new Vector3f(worldSize / 4f, worldSize/ 4f, worldSize /3f), new Vector3f(worldSize / 3f)));
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
    protected Vector3f getNormalizedTextureIndex(int indirectionGridIndex) {
        Vector3i i = getTextureIndex(indirectionGridIndex);
        return new Vector3f(i).div((float) getMaxTextureSize());
    }

    protected Vector3i getTextureIndex(int indirectionGridIndex, Vector3i target) {
        int textureSize = getMaxTextureSize();
        int halfTextureSize = textureSize / 2;
        int x = (indirectionGridIndex % halfTextureSize) * 2;
        int y = ((indirectionGridIndex / halfTextureSize) % halfTextureSize) * 2;
        int z = ((indirectionGridIndex / (halfTextureSize * halfTextureSize))) * 2;
        target.set(x, y, z);
//        System.out.println(target.toString());
        return target;
    }
    protected Vector3i getTextureIndex(int indirectionGridIndex) {
        return getTextureIndex(indirectionGridIndex, new Vector3i());
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

        // Todo: Put actual texture index in Index Nodes afterwards,
        // so that the texture size can be adjusted to the amount of indir nodes

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
                    Vector3f textureIndex = getNormalizedTextureIndex(newIGIndex);
                    ig.setNode(i, Cell.createIndex(textureIndex));
                }
            } else {
                // If at max depth, possibly add a data node
                if (intersection != null) {
                    // Create a data node with the color of the geometry
                    Vector3f color = new Vector3f(); // intersection.getColor()
                    color.set(childBoxStart).div(worldSize);
//                    color.set((float) Math.random(), (float) Math.random(), (float) Math.random());
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
        int n = (int) Math.pow(2, maxDepth);
        int logN = (int) Math.log(n);
        int textureSize = n + logN;
        textureSize /= 2; // Todo: Assumption that 1/2 of the space is empty, may break stuff

        // Max texture size of 256 since index nodes can only point to 2^8=255 values along each axis
        return Math.min(
                nextPowerOfTwo(textureSize),
                128);
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
        System.out.println("Max depth: " + maxDepth + ", IndirectionPool size: " + indirectionPool.size());

        int textureSize = getMaxTextureSize();
        System.out.println("Texture Size: " + textureSize + "^3 = " + (int) Math.pow(textureSize, 3));

        // In a worst case scenario, all octree nodes are used, so the tree is subdivided to maxDepth everywhere
//        int size = getMaxTextureSize();
        ByteBuffer textureData = BufferUtils.createByteBuffer(textureSize * textureSize * textureSize * 4); // 4 bytes since r g b a

        Vector3i index = new Vector3i();
        for (int i = 0; i < indirectionPool.size(); i++) {
            getTextureIndex(i, index);

            // Insert pool cells in a cube in texture memory
            indirectionPool.get(i).get(textureSize, index.x, index.y, index.z, textureData);
        }

        int bytesUsed = IndirectionGrid.getTextureIndex(textureSize, index.x, index.y, index.z, 7);
        System.out.println("Cells used: " + (bytesUsed / 4) / 8 + "/" + ((textureData.limit() / 4) / 8) + " (" + Math.round(100 * bytesUsed / (float) textureData.limit()) + "%)");
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
