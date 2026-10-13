package minigames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

// The arena where everything is drawn.
// ActionListener = receives the timer ticks, KeyListener = receives key presses
public class GamePanel extends JPanel implements ActionListener, KeyListener {

    public static final int ARENA_WIDTH = 1000;
    public static final int ARENA_HEIGHT = 700;

    private static final int BULLET_SPEED = 6;             // speed of a bullet flying straight
    private static final int DIAGONAL_BULLET_SPEED = 4;    // speed on each axis of a bullet flying diagonally
    private static final int BULLET_HALF_SIZE = 3;         // the bullet is 6 pixels wide (see Bullet.java)
    private static final int BULLET_START_DISTANCE = 20;   // how far from the tank center the bullet appears
    private static final int BARREL_LENGTH = 30;
    private static final int SHOOT_COOLDOWN_FRAMES = 40;   // frames to wait between two shots
    private static final int MAXIMUM_BOUNCES = 5;          // a bullet disappears after this many bounces

    private static final int PLAYER_ONE_START_X = 50;
    private static final int PLAYER_TWO_START_X = 920;
    private static final int TANKS_START_Y = 335;

    // Build phase settings
    private static final int GHOST_SPEED = 5;                      // how fast the shape moves while building
    private static final int OBJECTS_IN_FIRST_BUILD_PHASE = 2;     // shapes each player places at the start
    private static final int OBJECTS_IN_LATER_BUILD_PHASES = 1;    // shapes each player places after a lost life
    private static final int PLAYER_ONE_SHAPE_START_X = 200;
    private static final int PLAYER_TWO_SHAPE_START_X = 700;
    private static final int SHAPE_START_Y = 330;

    private Tank playerOneTank;
    private Tank playerTwoTank;
    private ArrayList<Bullet> bullets;
    private ArrayList<WallBlock> wallBlocks;

    private GameState gameState;
    private PlayerBuilder playerOneBuilder;
    private PlayerBuilder playerTwoBuilder;
    private Random random;

    private Timer gameTimer;
    private HashSet<Integer> pressedKeys;   // the codes of all keys that are held down right now
    private boolean leftShiftIsDown;
    private boolean rightShiftIsDown;
    private boolean playerOneShiftWasDown;  // was the key down in the previous frame? (to detect one press)
    private boolean playerTwoShiftWasDown;

    private boolean gameIsOver;
    private String winnerText;
    private Font livesFont;
    private Font gameOverFont;

    public GamePanel() {
        this.setPreferredSize(new Dimension(ARENA_WIDTH, ARENA_HEIGHT));
        this.setBackground(Color.WHITE);
        this.setFocusable(true);   // needed so the panel can receive key presses

        pressedKeys = new HashSet<Integer>();
        bullets = new ArrayList<Bullet>();
        wallBlocks = new ArrayList<WallBlock>();
        random = new Random();
        leftShiftIsDown = false;
        rightShiftIsDown = false;
        playerOneShiftWasDown = false;
        playerTwoShiftWasDown = false;
        gameIsOver = false;
        winnerText = "";
        livesFont = new Font("Arial", Font.BOLD, 18);
        gameOverFont = new Font("Arial", Font.BOLD, 60);
        this.addKeyListener(this);

        createTanks();
        createMainWalls();

        playerOneBuilder = new PlayerBuilder();
        playerTwoBuilder = new PlayerBuilder();
        startBuildPhase(OBJECTS_IN_FIRST_BUILD_PHASE);

        setUpTimer();
    }

    // Creates both tanks at their starting positions
    private void createTanks() {
        playerOneTank = new Tank(PLAYER_ONE_START_X, TANKS_START_Y, Direction.RIGHT, Color.GREEN);
        playerTwoTank = new Tank(PLAYER_TWO_START_X, TANKS_START_Y, Direction.LEFT, Color.RED);
    }

    // The timer calls actionPerformed() every 16 ms (about 60 times per second)
    private void setUpTimer() {
        gameTimer = new Timer(16, this);
        gameTimer.start();
    }

    // ---------- Directions ----------

    // Returns -1 if the direction points left, 1 if it points right, 0 if it points neither
    private int getDirectionX(Direction direction) {
        if (direction == Direction.LEFT || direction == Direction.UP_LEFT || direction == Direction.DOWN_LEFT) {
            return -1;
        }
        if (direction == Direction.RIGHT || direction == Direction.UP_RIGHT || direction == Direction.DOWN_RIGHT) {
            return 1;
        }
        return 0;
    }

    // Returns -1 if the direction points up, 1 if it points down, 0 if it points neither
    private int getDirectionY(Direction direction) {
        if (direction == Direction.UP || direction == Direction.UP_LEFT || direction == Direction.UP_RIGHT) {
            return -1;
        }
        if (direction == Direction.DOWN || direction == Direction.DOWN_LEFT || direction == Direction.DOWN_RIGHT) {
            return 1;
        }
        return 0;
    }

    // ---------- Walls ----------

    // Adds one wall made of 3 blocks next to each other, horizontal or vertical
    private void addWall(int startX, int startY, boolean isHorizontal, boolean isIndestructible) {
        for (int blockNumber = 0; blockNumber < 3; blockNumber++) {
            int blockX = startX;
            int blockY = startY;

            if (isHorizontal == true) {
                blockX = startX + blockNumber * WallBlock.DEFAULT_SIZE;
            } else {
                blockY = startY + blockNumber * WallBlock.DEFAULT_SIZE;
            }

            WallBlock newBlock = new WallBlock(blockX, blockY, isIndestructible);
            wallBlocks.add(newBlock);
        }
    }

    // The main walls behind the tanks, bullets cannot destroy these
    private void createMainWalls() {
        addWall(20, 310, false, true);    // behind player 1
        addWall(960, 310, false, true);   // behind player 2
    }

    // Returns true if the tank overlaps any wall block
    private boolean tankTouchesAnyWall(Tank tank) {
        Rectangle tankBounds = tank.getBounds();

        for (WallBlock currentBlock : wallBlocks) {
            if (tankBounds.intersects(currentBlock.getBounds())) {
                return true;
            }
        }

        return false;
    }

    // Returns the wall block that the bullet touches, or null if it touches none
    private WallBlock findWallBlockHitByBullet(Bullet bullet) {
        Rectangle bulletBounds = bullet.getBounds();

        for (WallBlock currentBlock : wallBlocks) {
            if (bulletBounds.intersects(currentBlock.getBounds())) {
                return currentBlock;
            }
        }

        return null;
    }

    // Takes one health from the block and removes it when the health is gone
    private void damageWallBlock(WallBlock hitBlock) {
        // The main walls are never damaged
        if (hitBlock.isIndestructible() == true) {
            return;
        }

        hitBlock.setHealth(hitBlock.getHealth() - 1);

        if (hitBlock.isDestroyed() == true) {
            wallBlocks.remove(hitBlock);
        }
    }

    // ---------- Build phase ----------

    // Starts a build phase: every player gets the given number of random shapes to place
    private void startBuildPhase(int objectsPerPlayer) {
        gameState = GameState.BUILDING;

        prepareBuilder(playerOneBuilder, objectsPerPlayer, PLAYER_ONE_SHAPE_START_X);
        prepareBuilder(playerTwoBuilder, objectsPerPlayer, PLAYER_TWO_SHAPE_START_X);
    }

    private void prepareBuilder(PlayerBuilder builder, int objectsToPlace, int startX) {
        builder.setObjectsLeftToPlace(objectsToPlace);
        builder.setCurrentShape(createRandomShape());
        builder.setPositionX(startX);
        builder.setPositionY(SHAPE_START_Y);
    }

    // Chooses one of the predefined shapes randomly
    private WallShape createRandomShape() {
        int randomNumber = random.nextInt(2);   // gives 0 or 1

        if (randomNumber == 0) {
            return WallShape.createHorizontalWall();
        } else {
            return WallShape.createVerticalWall();
        }
    }

    private void handleBuildPhase() {
        // Move the shapes with the same keys that move the tanks
        moveShape(playerOneBuilder,
                pressedKeys.contains(KeyEvent.VK_W),
                pressedKeys.contains(KeyEvent.VK_S),
                pressedKeys.contains(KeyEvent.VK_A),
                pressedKeys.contains(KeyEvent.VK_D));

        moveShape(playerTwoBuilder,
                pressedKeys.contains(KeyEvent.VK_UP),
                pressedKeys.contains(KeyEvent.VK_DOWN),
                pressedKeys.contains(KeyEvent.VK_LEFT),
                pressedKeys.contains(KeyEvent.VK_RIGHT));

        // The shoot keys place the shape. We react only in the frame when the key is pressed down.
        boolean playerOnePressedPlace = leftShiftIsDown == true && playerOneShiftWasDown == false;
        boolean playerTwoPressedPlace = rightShiftIsDown == true && playerTwoShiftWasDown == false;

        if (playerOnePressedPlace == true) {
            tryToPlaceShape(playerOneBuilder);
        }
        if (playerTwoPressedPlace == true) {
            tryToPlaceShape(playerTwoBuilder);
        }

        // When both players placed everything, the fight starts
        if (playerOneBuilder.isFinished() == true && playerTwoBuilder.isFinished() == true) {
            gameState = GameState.FIGHTING;
        }
    }

    // Moves the shape of one player, and keeps it inside the arena
    private void moveShape(PlayerBuilder builder, boolean goUp, boolean goDown, boolean goLeft, boolean goRight) {
        if (builder.isFinished() == true) {
            return;   // this player has nothing to move anymore
        }

        WallShape shape = builder.getCurrentShape();
        int newPositionX = builder.getPositionX();
        int newPositionY = builder.getPositionY();

        if (goUp == true) {
            newPositionY = newPositionY - GHOST_SPEED;
        }
        if (goDown == true) {
            newPositionY = newPositionY + GHOST_SPEED;
        }
        if (goLeft == true) {
            newPositionX = newPositionX - GHOST_SPEED;
        }
        if (goRight == true) {
            newPositionX = newPositionX + GHOST_SPEED;
        }

        // Keep the whole shape inside the arena
        if (newPositionX < 0) {
            newPositionX = 0;
        }
        if (newPositionY < 0) {
            newPositionY = 0;
        }
        if (newPositionX + shape.getWidth() > ARENA_WIDTH) {
            newPositionX = ARENA_WIDTH - shape.getWidth();
        }
        if (newPositionY + shape.getHeight() > ARENA_HEIGHT) {
            newPositionY = ARENA_HEIGHT - shape.getHeight();
        }

        builder.setPositionX(newPositionX);
        builder.setPositionY(newPositionY);
    }

    // Returns the rectangle of one block of a shape
    private Rectangle getBlockRectangle(WallShape shape, int blockIndex, int shapeX, int shapeY) {
        int blockX = shapeX + shape.getBlockColumn(blockIndex) * WallBlock.DEFAULT_SIZE;
        int blockY = shapeY + shape.getBlockRow(blockIndex) * WallBlock.DEFAULT_SIZE;

        return new Rectangle(blockX, blockY, WallBlock.DEFAULT_SIZE, WallBlock.DEFAULT_SIZE);
    }

    // A shape can be placed when none of its blocks overlaps a wall or a tank
    private boolean shapeCanBePlaced(WallShape shape, int shapeX, int shapeY) {
        for (int blockIndex = 0; blockIndex < shape.getBlockCount(); blockIndex++) {
            Rectangle newBlockRectangle = getBlockRectangle(shape, blockIndex, shapeX, shapeY);

            for (WallBlock existingBlock : wallBlocks) {
                if (newBlockRectangle.intersects(existingBlock.getBounds())) {
                    return false;
                }
            }

            if (newBlockRectangle.intersects(playerOneTank.getBounds())) {
                return false;
            }
            if (newBlockRectangle.intersects(playerTwoTank.getBounds())) {
                return false;
            }
        }

        return true;
    }

    private void tryToPlaceShape(PlayerBuilder builder) {
        if (builder.isFinished() == true) {
            return;
        }

        WallShape shape = builder.getCurrentShape();

        if (shapeCanBePlaced(shape, builder.getPositionX(), builder.getPositionY()) == false) {
            return;   // overlapping is not allowed, so nothing happens
        }

        // Turn every block of the shape into a real wall block
        for (int blockIndex = 0; blockIndex < shape.getBlockCount(); blockIndex++) {
            Rectangle blockRectangle = getBlockRectangle(shape, blockIndex, builder.getPositionX(), builder.getPositionY());
            WallBlock newBlock = new WallBlock(blockRectangle.x, blockRectangle.y, false);
            wallBlocks.add(newBlock);
        }

        builder.setObjectsLeftToPlace(builder.getObjectsLeftToPlace() - 1);

        // If the player has more to place, he gets a new random shape
        if (builder.isFinished() == false) {
            builder.setCurrentShape(createRandomShape());
        }
    }

    // ---------- Game loop ----------

    @Override
    public void actionPerformed(ActionEvent event) {
        if (gameState == GameState.BUILDING) {
            handleBuildPhase();
        } else {
            handleMovement();
            handleShooting();
            moveBullets();
            checkBulletHits();
            checkForGameOver();
        }

        // Remember the Shift keys for the next frame
        playerOneShiftWasDown = leftShiftIsDown;
        playerTwoShiftWasDown = rightShiftIsDown;

        repaint();   // asks Swing to draw the panel again
    }

    private void handleMovement() {
        boolean playerOneGoesUp = pressedKeys.contains(KeyEvent.VK_W);
        boolean playerOneGoesDown = pressedKeys.contains(KeyEvent.VK_S);
        boolean playerOneGoesLeft = pressedKeys.contains(KeyEvent.VK_A);
        boolean playerOneGoesRight = pressedKeys.contains(KeyEvent.VK_D);
        moveTank(playerOneTank, playerOneGoesUp, playerOneGoesDown, playerOneGoesLeft, playerOneGoesRight);

        boolean playerTwoGoesUp = pressedKeys.contains(KeyEvent.VK_UP);
        boolean playerTwoGoesDown = pressedKeys.contains(KeyEvent.VK_DOWN);
        boolean playerTwoGoesLeft = pressedKeys.contains(KeyEvent.VK_LEFT);
        boolean playerTwoGoesRight = pressedKeys.contains(KeyEvent.VK_RIGHT);
        moveTank(playerTwoTank, playerTwoGoesUp, playerTwoGoesDown, playerTwoGoesLeft, playerTwoGoesRight);
    }

    // Sets the direction the tank faces, based on the movement keys that are held down
    private void updateFacingDirection(Tank tank, boolean goUp, boolean goDown, boolean goLeft, boolean goRight) {
        // Two opposite keys at the same time cancel each other out
        boolean movesUp = goUp == true && goDown == false;
        boolean movesDown = goDown == true && goUp == false;
        boolean movesLeft = goLeft == true && goRight == false;
        boolean movesRight = goRight == true && goLeft == false;

        if (movesUp == true && movesLeft == true) {
            tank.setFacingDirection(Direction.UP_LEFT);
        } else if (movesUp == true && movesRight == true) {
            tank.setFacingDirection(Direction.UP_RIGHT);
        } else if (movesDown == true && movesLeft == true) {
            tank.setFacingDirection(Direction.DOWN_LEFT);
        } else if (movesDown == true && movesRight == true) {
            tank.setFacingDirection(Direction.DOWN_RIGHT);
        } else if (movesUp == true) {
            tank.setFacingDirection(Direction.UP);
        } else if (movesDown == true) {
            tank.setFacingDirection(Direction.DOWN);
        } else if (movesLeft == true) {
            tank.setFacingDirection(Direction.LEFT);
        } else if (movesRight == true) {
            tank.setFacingDirection(Direction.RIGHT);
        }
        // If no key is held, the tank keeps facing the same way
    }

    private void moveTank(Tank tank, boolean goUp, boolean goDown, boolean goLeft, boolean goRight) {
        // Remember the old position, so we can go back if the move is not allowed
        int oldPositionX = tank.getPositionX();
        int oldPositionY = tank.getPositionY();

        updateFacingDirection(tank, goUp, goDown, goLeft, goRight);

        if (goUp == true) {
            tank.setPositionY(tank.getPositionY() - tank.getSpeed());
        }
        if (goDown == true) {
            tank.setPositionY(tank.getPositionY() + tank.getSpeed());
        }
        if (goLeft == true) {
            tank.setPositionX(tank.getPositionX() - tank.getSpeed());
        }
        if (goRight == true) {
            tank.setPositionX(tank.getPositionX() + tank.getSpeed());
        }

        // Check if the new position is not allowed
        boolean moveIsNotAllowed = false;

        // Not allowed: the tank left the arena
        if (tank.getPositionX() < 0) {
            moveIsNotAllowed = true;
        }
        if (tank.getPositionY() < 0) {
            moveIsNotAllowed = true;
        }
        if (tank.getPositionX() + tank.getSize() > ARENA_WIDTH) {
            moveIsNotAllowed = true;
        }
        if (tank.getPositionY() + tank.getSize() > ARENA_HEIGHT) {
            moveIsNotAllowed = true;
        }

        // Not allowed: the tank touches a wall
        if (tankTouchesAnyWall(tank) == true) {
            moveIsNotAllowed = true;
        }

        // If the move is not allowed, put the tank back where it was
        if (moveIsNotAllowed == true) {
            tank.setPositionX(oldPositionX);
            tank.setPositionY(oldPositionY);
        }
    }

    // Player 1 shoots with Left Shift, player 2 shoots with Right Shift
    private void handleShooting() {
        tryToShoot(playerOneTank, leftShiftIsDown);
        tryToShoot(playerTwoTank, rightShiftIsDown);
    }

    private void tryToShoot(Tank tank, boolean shootKeyIsDown) {
        // Count down the waiting time between two shots
        if (tank.getShootCooldown() > 0) {
            tank.setShootCooldown(tank.getShootCooldown() - 1);
        }

        // Shoot only if the key is held and the waiting time is over
        if (shootKeyIsDown == true && tank.getShootCooldown() == 0) {
            createBullet(tank);
            tank.setShootCooldown(SHOOT_COOLDOWN_FRAMES);
        }
    }

    private void createBullet(Tank shooterTank) {
        int tankCenterX = shooterTank.getPositionX() + shooterTank.getSize() / 2;
        int tankCenterY = shooterTank.getPositionY() + shooterTank.getSize() / 2;

        // Which way does the tank face? Each value is -1, 0 or 1
        int directionX = getDirectionX(shooterTank.getFacingDirection());
        int directionY = getDirectionY(shooterTank.getFacingDirection());

        // A diagonal bullet uses a smaller speed on each axis
        int bulletSpeed = BULLET_SPEED;
        if (directionX != 0 && directionY != 0) {
            bulletSpeed = DIAGONAL_BULLET_SPEED;
        }

        int bulletSpeedX = directionX * bulletSpeed;
        int bulletSpeedY = directionY * bulletSpeed;

        // The bullet appears in front of the tank, so it does not hit its own tank
        int bulletStartX = tankCenterX - BULLET_HALF_SIZE + directionX * BULLET_START_DISTANCE;
        int bulletStartY = tankCenterY - BULLET_HALF_SIZE + directionY * BULLET_START_DISTANCE;

        Bullet newBullet = new Bullet(shooterTank, bulletStartX, bulletStartY, bulletSpeedX, bulletSpeedY);
        bullets.add(newBullet);
    }

    private void moveBullets() {
        // Go through the list from the end, so removing a bullet is safe
        for (int bulletIndex = bullets.size() - 1; bulletIndex >= 0; bulletIndex--) {
            Bullet currentBullet = bullets.get(bulletIndex);

            moveBulletOnXAxis(currentBullet);
            moveBulletOnYAxis(currentBullet);
            bounceBulletOffArenaEdges(currentBullet);

            // Remove bullets that bounced too many times
            if (currentBullet.getBounceCount() > MAXIMUM_BOUNCES) {
                bullets.remove(bulletIndex);
            }
        }
    }

    // Moves the bullet sideways. If it hits a wall, it goes back and turns around.
    private void moveBulletOnXAxis(Bullet bullet) {
        if (bullet.getSpeedX() == 0) {
            return;   // the bullet does not move sideways
        }

        bullet.setPositionX(bullet.getPositionX() + bullet.getSpeedX());

        WallBlock blockHit = findWallBlockHitByBullet(bullet);

        if (blockHit != null) {
            bullet.setPositionX(bullet.getPositionX() - bullet.getSpeedX());   // undo the move
            bullet.setSpeedX(-bullet.getSpeedX());                             // turn around
            bullet.setBounceCount(bullet.getBounceCount() + 1);
            damageWallBlock(blockHit);
        }
    }

    // Moves the bullet up or down. If it hits a wall, it goes back and turns around.
    private void moveBulletOnYAxis(Bullet bullet) {
        if (bullet.getSpeedY() == 0) {
            return;   // the bullet does not move up or down
        }

        bullet.setPositionY(bullet.getPositionY() + bullet.getSpeedY());

        WallBlock blockHit = findWallBlockHitByBullet(bullet);

        if (blockHit != null) {
            bullet.setPositionY(bullet.getPositionY() - bullet.getSpeedY());   // undo the move
            bullet.setSpeedY(-bullet.getSpeedY());                             // turn around
            bullet.setBounceCount(bullet.getBounceCount() + 1);
            damageWallBlock(blockHit);
        }
    }

    // If the bullet touches an edge of the arena, it turns around on that axis
    private void bounceBulletOffArenaEdges(Bullet bullet) {
        // Left edge
        if (bullet.getPositionX() < 0) {
            bullet.setPositionX(0);
            bullet.setSpeedX(-bullet.getSpeedX());
            bullet.setBounceCount(bullet.getBounceCount() + 1);
        }

        // Right edge
        if (bullet.getPositionX() + bullet.getSize() > ARENA_WIDTH) {
            bullet.setPositionX(ARENA_WIDTH - bullet.getSize());
            bullet.setSpeedX(-bullet.getSpeedX());
            bullet.setBounceCount(bullet.getBounceCount() + 1);
        }

        // Top edge
        if (bullet.getPositionY() < 0) {
            bullet.setPositionY(0);
            bullet.setSpeedY(-bullet.getSpeedY());
            bullet.setBounceCount(bullet.getBounceCount() + 1);
        }

        // Bottom edge
        if (bullet.getPositionY() + bullet.getSize() > ARENA_HEIGHT) {
            bullet.setPositionY(ARENA_HEIGHT - bullet.getSize());
            bullet.setSpeedY(-bullet.getSpeedY());
            bullet.setBounceCount(bullet.getBounceCount() + 1);
        }
    }

    // Checks if any bullet touches a tank. Any bullet can hurt any tank,
    // so a bounced bullet can also hit the tank that fired it.
    private void checkBulletHits() {
        for (int bulletIndex = bullets.size() - 1; bulletIndex >= 0; bulletIndex--) {
            Bullet currentBullet = bullets.get(bulletIndex);
            Rectangle bulletBounds = currentBullet.getBounds();

            if (bulletBounds.intersects(playerOneTank.getBounds())) {
                playerOneTank.setLives(playerOneTank.getLives() - 1);
                startNextRound();
                return;   // the bullet list was cleared, so we stop checking
            }

            if (bulletBounds.intersects(playerTwoTank.getBounds())) {
                playerTwoTank.setLives(playerTwoTank.getLives() - 1);
                startNextRound();
                return;
            }
        }
    }

    // Puts the tanks back to their starting positions, removes all bullets
    // and starts a new build phase. The walls stay as they are.
    private void startNextRound() {
        bullets.clear();

        playerOneTank.setPositionX(PLAYER_ONE_START_X);
        playerOneTank.setPositionY(TANKS_START_Y);
        playerOneTank.setFacingDirection(Direction.RIGHT);
        playerOneTank.setShootCooldown(0);

        playerTwoTank.setPositionX(PLAYER_TWO_START_X);
        playerTwoTank.setPositionY(TANKS_START_Y);
        playerTwoTank.setFacingDirection(Direction.LEFT);
        playerTwoTank.setShootCooldown(0);

        startBuildPhase(OBJECTS_IN_LATER_BUILD_PHASES);
    }

    // The game ends when one of the tanks has no lives left
    private void checkForGameOver() {
        boolean playerOneIsDead = playerOneTank.getLives() <= 0;
        boolean playerTwoIsDead = playerTwoTank.getLives() <= 0;

        if (playerOneIsDead == true) {
            winnerText = "Player 2 wins!";
            gameIsOver = true;
        }
        if (playerTwoIsDead == true) {
            winnerText = "Player 1 wins!";
            gameIsOver = true;
        }

        if (gameIsOver == true) {
            gameTimer.stop();   // the game loop stops, the last picture stays on the screen
        }
    }

    // ---------- Keyboard ----------

    @Override
    public void keyPressed(KeyEvent event) {
        int pressedKeyCode = event.getKeyCode();
        pressedKeys.add(pressedKeyCode);

        // Both Shift keys have the same key code, so we check which side was pressed
        if (pressedKeyCode == KeyEvent.VK_SHIFT) {
            if (event.getKeyLocation() == KeyEvent.KEY_LOCATION_LEFT) {
                leftShiftIsDown = true;
            }
            if (event.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
                rightShiftIsDown = true;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        int releasedKeyCode = event.getKeyCode();
        pressedKeys.remove(releasedKeyCode);

        if (releasedKeyCode == KeyEvent.VK_SHIFT) {
            if (event.getKeyLocation() == KeyEvent.KEY_LOCATION_LEFT) {
                leftShiftIsDown = false;
            }
            if (event.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
                rightShiftIsDown = false;
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent event) {
        // Not used, but KeyListener requires this method
    }

    // ---------- Drawing ----------

    // Swing calls this method every time the panel must be drawn
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);   // clears the panel first

        drawWalls(graphics);
        drawTank(graphics, playerOneTank);
        drawTank(graphics, playerTwoTank);
        drawBullets(graphics);
        drawLives(graphics);

        if (gameIsOver == false && gameState == GameState.BUILDING) {
            drawBuildPhase(graphics);
        }

        if (gameIsOver == true) {
            graphics.setFont(gameOverFont);
            graphics.setColor(Color.BLACK);
            drawCenteredText(graphics, winnerText, ARENA_HEIGHT / 2);
        }
    }

    // Draws a text in the middle of the screen, horizontally
    private void drawCenteredText(Graphics graphics, String text, int textY) {
        FontMetrics fontMetrics = graphics.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(text);
        int textX = (ARENA_WIDTH - textWidth) / 2;

        graphics.drawString(text, textX, textY);
    }

    private void drawWalls(Graphics graphics) {
        Color brickColor = new Color(139, 69, 19);   // brown

        for (WallBlock currentBlock : wallBlocks) {
            if (currentBlock.isIndestructible() == true) {
                graphics.setColor(Color.DARK_GRAY);
            } else {
                graphics.setColor(brickColor);
            }

            Rectangle blockBounds = currentBlock.getBounds();
            graphics.fillRect(blockBounds.x, blockBounds.y, blockBounds.width, blockBounds.height);
        }
    }

    private void drawBullets(Graphics graphics) {
        graphics.setColor(Color.BLACK);

        for (Bullet currentBullet : bullets) {
            graphics.fillOval(currentBullet.getPositionX(), currentBullet.getPositionY(),
                    currentBullet.getSize(), currentBullet.getSize());
        }
    }

    private void drawLives(Graphics graphics) {
        graphics.setFont(livesFont);

        graphics.setColor(Color.GREEN);
        graphics.drawString("Player 1 lives: " + playerOneTank.getLives(), 20, 30);

        graphics.setColor(Color.RED);
        graphics.drawString("Player 2 lives: " + playerTwoTank.getLives(), 800, 30);
    }

    // Draws the shapes that the players are moving and the build phase texts
    private void drawBuildPhase(Graphics graphics) {
        graphics.setFont(livesFont);
        graphics.setColor(Color.BLACK);
        drawCenteredText(graphics, "BUILD PHASE", 30);
        drawCenteredText(graphics, "Move: W A S D / arrow keys     Place: Left Shift / Right Shift", 55);

        graphics.setColor(Color.GREEN);
        graphics.drawString("Walls to place: " + playerOneBuilder.getObjectsLeftToPlace(), 20, 55);

        graphics.setColor(Color.RED);
        graphics.drawString("Walls to place: " + playerTwoBuilder.getObjectsLeftToPlace(), 800, 55);

        drawShape(graphics, playerOneBuilder, Color.GREEN);
        drawShape(graphics, playerTwoBuilder, Color.RED);
    }

    // The shape is drawn see-through: greenish if it can be placed here, reddish if it overlaps something
    private void drawShape(Graphics graphics, PlayerBuilder builder, Color playerColor) {
        if (builder.isFinished() == true) {
            return;   // nothing left to draw for this player
        }

        WallShape shape = builder.getCurrentShape();
        boolean canBePlaced = shapeCanBePlaced(shape, builder.getPositionX(), builder.getPositionY());

        Color fillColor;
        if (canBePlaced == true) {
            fillColor = new Color(0, 200, 0, 140);
        } else {
            fillColor = new Color(220, 0, 0, 140);
        }

        for (int blockIndex = 0; blockIndex < shape.getBlockCount(); blockIndex++) {
            Rectangle blockRectangle = getBlockRectangle(shape, blockIndex, builder.getPositionX(), builder.getPositionY());

            graphics.setColor(fillColor);
            graphics.fillRect(blockRectangle.x, blockRectangle.y, blockRectangle.width, blockRectangle.height);

            // The outline has the color of the player, so you can see which shape is yours
            graphics.setColor(playerColor);
            graphics.drawRect(blockRectangle.x, blockRectangle.y, blockRectangle.width, blockRectangle.height);
        }
    }

    private void drawTank(Graphics graphics, Tank tank) {
        // Draw the body
        graphics.setColor(tank.getBodyColor());
        graphics.fillRect(tank.getPositionX(), tank.getPositionY(), tank.getSize(), tank.getSize());

        // The barrel is a line from the center of the tank in the facing direction
        int centerX = tank.getPositionX() + tank.getSize() / 2;
        int centerY = tank.getPositionY() + tank.getSize() / 2;

        int directionX = getDirectionX(tank.getFacingDirection());
        int directionY = getDirectionY(tank.getFacingDirection());

        int barrelEndX = centerX + directionX * BARREL_LENGTH;
        int barrelEndY = centerY + directionY * BARREL_LENGTH;

        graphics.setColor(Color.BLACK);
        graphics.drawLine(centerX, centerY, barrelEndX, barrelEndY);
    }
}
