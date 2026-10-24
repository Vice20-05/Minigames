package battletanks;

import battletanks.WallBlock;
import java.util.ArrayList;
import java.util.Random;

// Builds the walls of the arena. Knows nothing about drawing or tanks.
public class MapBuilder {

    private static final int NUMBER_OF_MAPS = 5;

    private final Random random;

    public MapBuilder() {
        random = new Random();
    }

    // Puts the base walls and one random maze into the list.
    // Returns the number of the map that was chosen (1 to 5).
    public int buildRandomMap(ArrayList<WallBlock> wallBlocks) {
        int mapNumber = random.nextInt(NUMBER_OF_MAPS) + 1;

        addMainWalls(wallBlocks);

        if (mapNumber == 1) {
            buildClassicMaze(wallBlocks);
        } else if (mapNumber == 2) {
            buildCrossMaze(wallBlocks);
        } else if (mapNumber == 3) {
            buildCorridorMaze(wallBlocks);
        } else if (mapNumber == 4) {
            buildFortressMaze(wallBlocks);
        } else {
            buildZigzagMaze(wallBlocks);
        }

        return mapNumber;
    }

    private void addWallLine(ArrayList<WallBlock> wallBlocks,
            int startX, int startY, int blockCount,
            boolean isHorizontal, boolean isIndestructible) {

        for (int blockNumber = 0; blockNumber < blockCount; blockNumber++) {
            int blockX = startX;
            int blockY = startY;

            if (isHorizontal) {
                blockX = startX + blockNumber * WallBlock.DEFAULT_SIZE;
            } else {
                blockY = startY + blockNumber * WallBlock.DEFAULT_SIZE;
            }

            wallBlocks.add(
                    new WallBlock(blockX, blockY, isIndestructible)
            );
        }
    }

    // The walls behind the tanks, they are the same on every map.
    private void addMainWalls(ArrayList<WallBlock> wallBlocks) {
        addWallLine(wallBlocks, 20, 310, 3, false, true);
        addWallLine(wallBlocks, 960, 310, 3, false, true);
    }

    // Map 1: the original maze.
    private void buildClassicMaze(ArrayList<WallBlock> wallBlocks) {
        addWallLine(wallBlocks, 340, 180, 16, true, true);
        addWallLine(wallBlocks, 340, 500, 16, true, true);

        addWallLine(wallBlocks, 340, 200, 6, false, true);
        addWallLine(wallBlocks, 340, 380, 6, false, true);

        addWallLine(wallBlocks, 640, 200, 6, false, true);
        addWallLine(wallBlocks, 640, 380, 6, false, true);

        addWallLine(wallBlocks, 490, 280, 7, false, false);

        addWallLine(wallBlocks, 400, 240, 3, false, false);
        addWallLine(wallBlocks, 400, 400, 3, false, false);
        addWallLine(wallBlocks, 580, 240, 3, false, false);
        addWallLine(wallBlocks, 580, 400, 3, false, false);
    }

    // Map 2: a steel cross with a breakable center and steel corners.
    private void buildCrossMaze(ArrayList<WallBlock> wallBlocks) {
        addWallLine(wallBlocks, 490, 140, 5, false, true);
        addWallLine(wallBlocks, 490, 460, 5, false, true);

        addWallLine(wallBlocks, 440, 320, 6, true, false);
        addWallLine(wallBlocks, 440, 360, 6, true, false);

        addWallLine(wallBlocks, 300, 100, 4, true, true);
        addWallLine(wallBlocks, 620, 100, 4, true, true);
        addWallLine(wallBlocks, 300, 580, 4, true, true);
        addWallLine(wallBlocks, 620, 580, 4, true, true);
    }

    // Map 3: long steel corridors with breakable walls in the gaps.
    private void buildCorridorMaze(ArrayList<WallBlock> wallBlocks) {
        addWallLine(wallBlocks, 240, 200, 10, true, true);
        addWallLine(wallBlocks, 560, 200, 10, true, true);
        addWallLine(wallBlocks, 240, 480, 10, true, true);
        addWallLine(wallBlocks, 560, 480, 10, true, true);

        addWallLine(wallBlocks, 480, 160, 4, false, false);
        addWallLine(wallBlocks, 480, 440, 4, false, false);

        addWallLine(wallBlocks, 480, 320, 3, false, true);
    }

    // Map 4: each player has a small fortress in front of the base.
    private void buildFortressMaze(ArrayList<WallBlock> wallBlocks) {
        addWallLine(wallBlocks, 200, 260, 5, true, true);
        addWallLine(wallBlocks, 200, 420, 5, true, true);
        addWallLine(wallBlocks, 300, 280, 6, false, false);

        addWallLine(wallBlocks, 700, 260, 5, true, true);
        addWallLine(wallBlocks, 700, 420, 5, true, true);
        addWallLine(wallBlocks, 680, 280, 6, false, false);

        addWallLine(wallBlocks, 490, 80, 4, false, true);
        addWallLine(wallBlocks, 490, 540, 4, false, true);
    }

    // Map 5: zigzag of steel and breakable walls.
    private void buildZigzagMaze(ArrayList<WallBlock> wallBlocks) {
        addWallLine(wallBlocks, 300, 100, 8, false, true);
        addWallLine(wallBlocks, 300, 440, 8, false, true);
        addWallLine(wallBlocks, 680, 100, 8, false, true);
        addWallLine(wallBlocks, 680, 440, 8, false, true);

        addWallLine(wallBlocks, 420, 260, 8, false, false);
        addWallLine(wallBlocks, 560, 260, 8, false, false);

        addWallLine(wallBlocks, 490, 330, 2, false, true);
    }
}