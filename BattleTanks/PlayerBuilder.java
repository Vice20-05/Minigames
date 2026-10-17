package minigames;

/**
 * Holds the build-phase state for one player.
 *
 * The game panel owns movement, collision checks, and placement. This class
 * only stores the shape currently being positioned and how many shapes remain.
 */
public class PlayerBuilder {

    private int objectsLeftToPlace;
    private WallShape currentShape;
    private int positionX;
    private int positionY;

    public int getObjectsLeftToPlace() {
        return objectsLeftToPlace;
    }

    public void setObjectsLeftToPlace(int objectsLeftToPlace) {
        this.objectsLeftToPlace = objectsLeftToPlace;
    }

    public WallShape getCurrentShape() {
        return currentShape;
    }

    public void setCurrentShape(WallShape currentShape) {
        this.currentShape = currentShape;
    }

    public int getPositionX() {
        return positionX;
    }

    public void setPositionX(int positionX) {
        this.positionX = positionX;
    }

    public int getPositionY() {
        return positionY;
    }

    public void setPositionY(int positionY) {
        this.positionY = positionY;
    }

    public boolean isFinished() {
        return objectsLeftToPlace <= 0;
    }
}
