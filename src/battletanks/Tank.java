package battletanks;

import java.awt.Color;
import java.awt.Rectangle;

public class Tank {

    private int positionX;
    private int positionY;
    private int size;
    private int speed;
    private Direction facingDirection;
    private int lives;
    private int shootCooldown;    // frames to wait before the tank may shoot again
    private Color bodyColor;

    public Tank(int startX, int startY, Direction startDirection, Color bodyColor) {
        this.positionX = startX;
        this.positionY = startY;
        this.facingDirection = startDirection;
        this.bodyColor = bodyColor;
        this.size = 30;
        this.speed = 2;
        this.lives = 3;
        this.shootCooldown = 0;
    }

    // Returns the rectangle of the tank, used later for collisions
    public Rectangle getBounds() {
        return new Rectangle(positionX, positionY, size, size);
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

    public int getSize() {
        return size;
    }

    public int getSpeed() {
        return speed;
    }

    public Direction getFacingDirection() {
        return facingDirection;
    }

    public void setFacingDirection(Direction facingDirection) {
        this.facingDirection = facingDirection;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public int getShootCooldown() {
        return shootCooldown;
    }

    public void setShootCooldown(int shootCooldown) {
        this.shootCooldown = shootCooldown;
    }

    public Color getBodyColor() {
        return bodyColor;
    }
}
