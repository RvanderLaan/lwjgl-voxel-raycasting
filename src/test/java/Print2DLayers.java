import core.Cell;
import core.SVO;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Print2DLayers {
    public static void main(String[] args) {
        SVO svo = new SVO(2, 100);
        svo.generateDemoScene();
        svo.generateSVO();
        print(svo);
    }
    public static void print(SVO svo) {
//        core.IndirectionGrid root = svo.getIndirectionPool().get(0);
//
//        int r = svo.getMaxDepth();
//        String[][][] volume = new String[r][r][r];
        // Print layers along the X axis
        StringBuffer stringBuffer = new StringBuffer();
        svo.getIndirectionPool().forEach((indirectionGrid -> {
            for (Cell cell : indirectionGrid.getChildren())
                stringBuffer.append(cell.getNodeType().toString() + "\t");
            stringBuffer.append("\n");
        }));

        stringBuffer.append("\n");

        ByteBuffer textureData = svo.getTextureData();
        while (textureData.hasRemaining()) {
            for (int i = 0; i < 4; i++) {
                byte b = textureData.get();
//                String bits = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
                stringBuffer.append(((int) b + 256) % 256 + "   \t");
            }
            stringBuffer.append("\n");
        }

        Path path = Paths.get("./src/test/resources/octree-bytes.txt");
        //Use try-with-resource to get auto-closeable writer instance
        try (BufferedWriter writer = Files.newBufferedWriter(path))
        {
            writer.write(stringBuffer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    protected void lookup(core.SVO svo, Vector3f uwvInput) {
//        core.Cell cell = core.Cell.createIndex(0);
//        Vector3f uwv = uwvInput;
//        Vector3f lookup = new Vector3f();
//
//        // P is the vector from the first node of an IG to the lookup vector
//
//        for (int i = 0; i < svo.getMaxDepth(); i++) {
//            if (cell.getNodeType() == core.Cell.NodeType.INDEX) {
//                lookup.set(uwv + )
//                cell =
//            }
//            // Lookup in current node
//            Vector3f lookup = (uwv.add())
//        }
//        for (core.Cell cell : ig.getChildren()) {
//            if (cell.getNodeType() == core.Cell.NodeType.DATA) {
//
//            } else if (cell.getNodeType() == core.Cell.NodeType.INDEX) {
//                insert(svo, svo.getIndirectionPool().get(cell.getIndex()), depth + 1, volume);
//            }
//        }
//    }
}
