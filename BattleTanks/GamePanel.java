package minigames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

// The arena where everything is drawn.
public class GamePanel extends JPanel implements ActionListener, KeyListener {

    public static final int ARENA_WIDTH = 1000;
    public static final int ARENA_HEIGHT = 700;

    private static final int BULLET_SPEED = 6;
    private static final int DIAGONAL_BULLET_SPEED = 4;
    private static final int BULLET_HALF_SIZE = 3;
    private static final int BULLET_START_DISTANCE = 20;
    private static final int SHOOT_COOLDOWN_FRAMES = 40;
    private static final int MAXIMUM_BOUNCES = 5;

    private static final int PLAYER_ONE_START_X = 50;
    private static final int PLAYER_TWO_START_X = 920;
    private static final int TANKS_START_Y = 335;

    private static final int GHOST_SPEED = 5;
    private static final int OBJECTS_IN_FIRST_BUILD_PHASE = 2;
    private static final int PLAYER_ONE_SHAPE_START_X = 200;
    private static final int PLAYER_TWO_SHAPE_START_X = 700;
    private static final int SHAPE_START_Y = 330;

    private static final int HEART_SIZE = 24;
    private static final int SPACE_BETWEEN_HEARTS = 5;

    private Tank playerOneTank;
    private Tank playerTwoTank;

    private ArrayList<Bullet> bullets;
    private ArrayList<WallBlock> wallBlocks;

    private GameState gameState;

    private PlayerBuilder playerOneBuilder;
    private PlayerBuilder playerTwoBuilder;

    private Random random;
    private Timer gameTimer;

    private HashSet<Integer> pressedKeys;

    private boolean leftShiftIsDown;
    private boolean rightShiftIsDown;

    private boolean playerOneShiftWasDown;
    private boolean playerTwoShiftWasDown;
        private boolean playerOnePlaceWasDown;   // was the place key (Tab) down in the previous frame?
    private boolean playerTwoPlaceWasDown;   // was the place key (Enter) down in the previous frame?

    private boolean gameIsOver;
    private String winnerText;

    private Font livesFont;
    private Font gameOverFont;

    private Image backgroundImage;
    private Image playerOneTankImage;
    private Image playerTwoTankImage;
    private Image brickWallImage;
    private Image steelWallImage;
    private Image bulletImage;
    private Image heartImage;

    public GamePanel() {
        setPreferredSize(new Dimension(ARENA_WIDTH, ARENA_HEIGHT));
        setBackground(Color.WHITE);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);   // needed, otherwise Swing takes the Tab key for itself

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

        livesFont = new Font("Consolas", Font.BOLD, 16);
        gameOverFont = new Font("Impact", Font.PLAIN, 70);

        addKeyListener(this);

        // Clicking the replay button starts a new game.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (gameIsOver
                        && e.getX() >= 400 && e.getX() <= 600
                        && e.getY() >= 400 && e.getY() <= 460) {
                    replayGame();
                }
            }
        });

        loadImages();

        playerOneBuilder = new PlayerBuilder();
        playerTwoBuilder = new PlayerBuilder();

        resetGame();
        setUpTimer();
    }

    //Pictures

    // Loads the PNG pictures from src/main/resources/images.
    private void loadImages() {
        backgroundImage = loadImage("/images/bg.png");
        playerOneTankImage = loadImage("/images/tank_blue.png");
        playerTwoTankImage = loadImage("/images/tank_bigRed.png");
        brickWallImage = loadImage("/images/barrelGreen_side.png");
        steelWallImage = loadImage("/images/barrelRust_side.png");
        bulletImage = loadImage("/images/bulletDark1_outline.png");
        heartImage = loadImage("/images/explosion2.png");
    }

    private Image loadImage(String imagePath) {
        java.net.URL imageURL = getClass().getResource(imagePath);

        if (imageURL == null) {
            System.out.println("Image not found: " + imagePath);
            return null;
        }

        return new ImageIcon(imageURL).getImage();
    }

    //Game initialization

    private void resetGame() {

        createTanks();
        gameIsOver = false;
        winnerText = "";

        pressedKeys.clear();
        bullets.clear();
        wallBlocks.clear();

        leftShiftIsDown = false;
        rightShiftIsDown = false;
        playerOneShiftWasDown = false;
        playerTwoShiftWasDown = false;
        playerOnePlaceWasDown = false;
        playerTwoPlaceWasDown = false;

        createTanks();
        
        createMainWalls();
        createMaze();

        startBuildPhase(OBJECTS_IN_FIRST_BUILD_PHASE);

        repaint();
    }

    private void replayGame() {
        resetGame();

        if (gameTimer != null) {
            gameTimer.start();
        }

        requestFocusInWindow();
        repaint();
    }

    private void createTanks() {
        playerOneTank = new Tank(
                PLAYER_ONE_START_X,
                TANKS_START_Y,
                Direction.RIGHT,
                Color.GREEN
        );

        playerTwoTank = new Tank(
                PLAYER_TWO_START_X,
                TANKS_START_Y,
                Direction.LEFT,
                Color.RED
        );
    }

    private void setUpTimer() {
        gameTimer = new Timer(16, this);
        gameTimer.start();
    }

    // ---------- Directions ----------

    private int getDirectionX(Direction direction) {
        if (direction == Direction.LEFT
                || direction == Direction.UP_LEFT
                || direction == Direction.DOWN_LEFT) {
            return -1;
        }

        if (direction == Direction.RIGHT
                || direction == Direction.UP_RIGHT
                || direction == Direction.DOWN_RIGHT) {
            return 1;
        }

        return 0;
    }

    private int getDirectionY(Direction direction) {
        if (direction == Direction.UP
                || direction == Direction.UP_LEFT
                || direction == Direction.UP_RIGHT) {
            return -1;
        }

        if (direction == Direction.DOWN
                || direction == Direction.DOWN_LEFT
                || direction == Direction.DOWN_RIGHT) {
            return 1;
        }

        return 0;
    }

    //Walls 

    private void addWall(int startX, int startY,
            boolean isHorizontal, boolean isIndestructible) {

        for (int blockNumber = 0; blockNumber < 3; blockNumber++) {
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

    private void addWallLine(int startX, int startY, int blockCount,
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

    private void createMainWalls() {
        addWall(20, 310, false, true);
        addWall(960, 310, false, true);
    }

    // Creates the outer frame and the destructible walls in the maze.
    private void createMaze() {
        addWallLine(340, 180, 16, true, true);
        addWallLine(340, 500, 16, true, true);

        addWallLine(340, 200, 6, false, true);
        addWallLine(340, 380, 6, false, true);

        addWallLine(640, 200, 6, false, true);
        addWallLine(640, 380, 6, false, true);

        addWallLine(490, 280, 7, false, false);

        addWallLine(400, 240, 3, false, false);
        addWallLine(400, 400, 3, false, false);
        addWallLine(580, 240, 3, false, false);
        addWallLine(580, 400, 3, false, false);
    }

    private boolean tankTouchesAnyWall(Tank tank) {
        Rectangle tankBounds = tank.getBounds();

        for (WallBlock currentBlock : wallBlocks) {
            if (tankBounds.intersects(currentBlock.getBounds())) {
                return true;
            }
        }

        return false;
    }

    private WallBlock findWallBlockHitByBullet(Bullet bullet) {
        Rectangle bulletBounds = bullet.getBounds();

        for (WallBlock currentBlock : wallBlocks) {
            if (bulletBounds.intersects(currentBlock.getBounds())) {
                return currentBlock;
            }
        }

        return null;
    }

    private void damageWallBlock(WallBlock hitBlock) {
        if (hitBlock.isIndestructible()) {
            return;
        }

        hitBlock.setHealth(hitBlock.getHealth() - 1);

        if (hitBlock.isDestroyed()) {
            wallBlocks.remove(hitBlock);
        }
    }

    //  Build phase 
    private void startBuildPhase(int objectsPerPlayer) {
        gameState = GameState.BUILDING;

        prepareBuilder(
                playerOneBuilder,
                objectsPerPlayer,
                PLAYER_ONE_SHAPE_START_X
        );

        prepareBuilder(
                playerTwoBuilder,
                objectsPerPlayer,
                PLAYER_TWO_SHAPE_START_X
        );
    }

    private void prepareBuilder(PlayerBuilder builder,
            int objectsToPlace, int startX) {

        builder.setObjectsLeftToPlace(objectsToPlace);
        builder.setCurrentShape(createRandomShape());
        builder.setPositionX(startX);
        builder.setPositionY(SHAPE_START_Y);
    }

    private WallShape createRandomShape() {
        int randomNumber = random.nextInt(2);

        if (randomNumber == 0) {
            return WallShape.createHorizontalWall();
        }

        return WallShape.createVerticalWall();
    }

        private void handleBuildPhase() {
        moveShape(
                playerOneBuilder,
                pressedKeys.contains(KeyEvent.VK_W),
                pressedKeys.contains(KeyEvent.VK_S),
                pressedKeys.contains(KeyEvent.VK_A),
                pressedKeys.contains(KeyEvent.VK_D)
        );

        moveShape(
                playerTwoBuilder,
                pressedKeys.contains(KeyEvent.VK_UP),
                pressedKeys.contains(KeyEvent.VK_DOWN),
                pressedKeys.contains(KeyEvent.VK_LEFT),
                pressedKeys.contains(KeyEvent.VK_RIGHT)
        );

        // Player 1 places with Tab, player 2 places with Enter.
        // We react only in the frame when the key is pressed down.
        boolean playerOnePlaceIsDown = pressedKeys.contains(KeyEvent.VK_TAB);
        boolean playerTwoPlaceIsDown = pressedKeys.contains(KeyEvent.VK_ENTER);

        boolean playerOnePressedPlace =
                playerOnePlaceIsDown && !playerOnePlaceWasDown;

        boolean playerTwoPressedPlace =
                playerTwoPlaceIsDown && !playerTwoPlaceWasDown;

        // Remember the keys for the next frame
        playerOnePlaceWasDown = playerOnePlaceIsDown;
        playerTwoPlaceWasDown = playerTwoPlaceIsDown;

        if (playerOnePressedPlace) {
            tryToPlaceShape(playerOneBuilder);
        }

        if (playerTwoPressedPlace) {
            tryToPlaceShape(playerTwoBuilder);
        }

        if (playerOneBuilder.isFinished()
                && playerTwoBuilder.isFinished()) {
            gameState = GameState.FIGHTING;
        }
    }

    private void moveShape(PlayerBuilder builder,
            boolean goUp, boolean goDown,
            boolean goLeft, boolean goRight) {

        if (builder.isFinished()) {
            return;
        }

        WallShape shape = builder.getCurrentShape();

        int newPositionX = builder.getPositionX();
        int newPositionY = builder.getPositionY();

        if (goUp) {
            newPositionY -= GHOST_SPEED;
        }

        if (goDown) {
            newPositionY += GHOST_SPEED;
        }

        if (goLeft) {
            newPositionX -= GHOST_SPEED;
        }

        if (goRight) {
            newPositionX += GHOST_SPEED;
        }

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

    private Rectangle getBlockRectangle(WallShape shape,
            int blockIndex, int shapeX, int shapeY) {

        int blockX = shapeX
                + shape.getBlockColumn(blockIndex) * WallBlock.DEFAULT_SIZE;

        int blockY = shapeY
                + shape.getBlockRow(blockIndex) * WallBlock.DEFAULT_SIZE;

        return new Rectangle(
                blockX,
                blockY,
                WallBlock.DEFAULT_SIZE,
                WallBlock.DEFAULT_SIZE
        );
    }

    private boolean shapeCanBePlaced(WallShape shape,
            int shapeX, int shapeY) {

        for (int blockIndex = 0;
                blockIndex < shape.getBlockCount();
                blockIndex++) {

            Rectangle newBlockRectangle = getBlockRectangle(
                    shape, blockIndex, shapeX, shapeY
            );

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
        if (builder.isFinished()) {
            return;
        }

        WallShape shape = builder.getCurrentShape();

        if (!shapeCanBePlaced(
                shape,
                builder.getPositionX(),
                builder.getPositionY())) {
            return;
        }

        for (int blockIndex = 0;
                blockIndex < shape.getBlockCount();
                blockIndex++) {

            Rectangle blockRectangle = getBlockRectangle(
                    shape,
                    blockIndex,
                    builder.getPositionX(),
                    builder.getPositionY()
            );

            wallBlocks.add(
                    new WallBlock(
                            blockRectangle.x,
                            blockRectangle.y,
                            false
                    )
            );
        }

        builder.setObjectsLeftToPlace(
                builder.getObjectsLeftToPlace() - 1
        );

        if (!builder.isFinished()) {
            builder.setCurrentShape(createRandomShape());
        }
    }

    //  Game loop 

    @Override
    public void actionPerformed(ActionEvent event) {
        if (gameIsOver) {
            return;
        }

        if (gameState == GameState.BUILDING) {
            handleBuildPhase();
        } else {
            handleMovement();
            handleShooting();
            moveBullets();
            checkBulletHits();
            checkForGameOver();
        }

        playerOneShiftWasDown = leftShiftIsDown;
        playerTwoShiftWasDown = rightShiftIsDown;

        repaint();
    }

    private void handleMovement() {
        moveTank(
                playerOneTank,
                pressedKeys.contains(KeyEvent.VK_W),
                pressedKeys.contains(KeyEvent.VK_S),
                pressedKeys.contains(KeyEvent.VK_A),
                pressedKeys.contains(KeyEvent.VK_D)
        );

        moveTank(
                playerTwoTank,
                pressedKeys.contains(KeyEvent.VK_UP),
                pressedKeys.contains(KeyEvent.VK_DOWN),
                pressedKeys.contains(KeyEvent.VK_LEFT),
                pressedKeys.contains(KeyEvent.VK_RIGHT)
        );
    }

    private void updateFacingDirection(Tank tank,
            boolean goUp, boolean goDown,
            boolean goLeft, boolean goRight) {

        boolean movesUp = goUp && !goDown;
        boolean movesDown = goDown && !goUp;
        boolean movesLeft = goLeft && !goRight;
        boolean movesRight = goRight && !goLeft;

        if (movesUp && movesLeft) {
            tank.setFacingDirection(Direction.UP_LEFT);
        } else if (movesUp && movesRight) {
            tank.setFacingDirection(Direction.UP_RIGHT);
        } else if (movesDown && movesLeft) {
            tank.setFacingDirection(Direction.DOWN_LEFT);
        } else if (movesDown && movesRight) {
            tank.setFacingDirection(Direction.DOWN_RIGHT);
        } else if (movesUp) {
            tank.setFacingDirection(Direction.UP);
        } else if (movesDown) {
            tank.setFacingDirection(Direction.DOWN);
        } else if (movesLeft) {
            tank.setFacingDirection(Direction.LEFT);
        } else if (movesRight) {
            tank.setFacingDirection(Direction.RIGHT);
        }
    }

    private void moveTank(Tank tank,
            boolean goUp, boolean goDown,
            boolean goLeft, boolean goRight) {

        int oldPositionX = tank.getPositionX();
        int oldPositionY = tank.getPositionY();

        updateFacingDirection(tank, goUp, goDown, goLeft, goRight);

        if (goUp) {
            tank.setPositionY(tank.getPositionY() - tank.getSpeed());
        }

        if (goDown) {
            tank.setPositionY(tank.getPositionY() + tank.getSpeed());
        }

        if (goLeft) {
            tank.setPositionX(tank.getPositionX() - tank.getSpeed());
        }

        if (goRight) {
            tank.setPositionX(tank.getPositionX() + tank.getSpeed());
        }

        boolean moveIsNotAllowed = false;

        if (tank.getPositionX() < 0
                || tank.getPositionY() < 0
                || tank.getPositionX() + tank.getSize() > ARENA_WIDTH
                || tank.getPositionY() + tank.getSize() > ARENA_HEIGHT) {
            moveIsNotAllowed = true;
        }

        if (tankTouchesAnyWall(tank)) {
            moveIsNotAllowed = true;
        }

        if (moveIsNotAllowed) {
            tank.setPositionX(oldPositionX);
            tank.setPositionY(oldPositionY);
        }
    }

    //  Shooting 

    private void handleShooting() {
        tryToShoot(playerOneTank, leftShiftIsDown);
        tryToShoot(playerTwoTank, rightShiftIsDown);
    }

    private void tryToShoot(Tank tank, boolean shootKeyIsDown) {
        if (tank.getShootCooldown() > 0) {
            tank.setShootCooldown(tank.getShootCooldown() - 1);
        }

        if (shootKeyIsDown && tank.getShootCooldown() == 0) {
            createBullet(tank);
            tank.setShootCooldown(SHOOT_COOLDOWN_FRAMES);
        }
    }

    private void createBullet(Tank shooterTank) {
        int tankCenterX =
                shooterTank.getPositionX() + shooterTank.getSize() / 2;

        int tankCenterY =
                shooterTank.getPositionY() + shooterTank.getSize() / 2;

        int directionX = getDirectionX(shooterTank.getFacingDirection());
        int directionY = getDirectionY(shooterTank.getFacingDirection());

        int bulletSpeed = BULLET_SPEED;

        if (directionX != 0 && directionY != 0) {
            bulletSpeed = DIAGONAL_BULLET_SPEED;
        }

        int bulletSpeedX = directionX * bulletSpeed;
        int bulletSpeedY = directionY * bulletSpeed;

        int bulletStartX = tankCenterX
                - BULLET_HALF_SIZE
                + directionX * BULLET_START_DISTANCE;

        int bulletStartY = tankCenterY
                - BULLET_HALF_SIZE
                + directionY * BULLET_START_DISTANCE;

        bullets.add(
                new Bullet(
                        shooterTank,
                        bulletStartX,
                        bulletStartY,
                        bulletSpeedX,
                        bulletSpeedY
                )
        );
    }

    private void moveBullets() {
        for (int bulletIndex = bullets.size() - 1;
                bulletIndex >= 0;
                bulletIndex--) {

            Bullet currentBullet = bullets.get(bulletIndex);

            moveBulletOnXAxis(currentBullet);
            moveBulletOnYAxis(currentBullet);
            bounceBulletOffArenaEdges(currentBullet);

            if (currentBullet.getBounceCount() > MAXIMUM_BOUNCES) {
                bullets.remove(bulletIndex);
            }
        }
    }

    private void moveBulletOnXAxis(Bullet bullet) {
        if (bullet.getSpeedX() == 0) {
            return;
        }

        bullet.setPositionX(
                bullet.getPositionX() + bullet.getSpeedX()
        );

        WallBlock blockHit = findWallBlockHitByBullet(bullet);

        if (blockHit != null) {
            bullet.setPositionX(
                    bullet.getPositionX() - bullet.getSpeedX()
            );

            bullet.setSpeedX(-bullet.getSpeedX());
            bullet.setBounceCount(bullet.getBounceCount() + 1);

            damageWallBlock(blockHit);
        }
    }

    private void moveBulletOnYAxis(Bullet bullet) {
        if (bullet.getSpeedY() == 0) {
            return;
        }

        bullet.setPositionY(
                bullet.getPositionY() + bullet.getSpeedY()
        );

        WallBlock blockHit = findWallBlockHitByBullet(bullet);

        if (blockHit != null) {
            bullet.setPositionY(
                    bullet.getPositionY() - bullet.getSpeedY()
            );

            bullet.setSpeedY(-bullet.getSpeedY());
            bullet.setBounceCount(bullet.getBounceCount() + 1);

            damageWallBlock(blockHit);
        }
    }

    private void bounceBulletOffArenaEdges(Bullet bullet) {
        if (bullet.getPositionX() < 0) {
            bullet.setPositionX(0);
            bullet.setSpeedX(-bullet.getSpeedX());
            bullet.setBounceCount(bullet.getBounceCount() + 1);
        }

        if (bullet.getPositionX() + bullet.getSize() > ARENA_WIDTH) {
            bullet.setPositionX(ARENA_WIDTH - bullet.getSize());
            bullet.setSpeedX(-bullet.getSpeedX());
            bullet.setBounceCount(bullet.getBounceCount() + 1);
        }

        if (bullet.getPositionY() < 0) {
            bullet.setPositionY(0);
            bullet.setSpeedY(-bullet.getSpeedY());
            bullet.setBounceCount(bullet.getBounceCount() + 1);
        }

        if (bullet.getPositionY() + bullet.getSize() > ARENA_HEIGHT) {
            bullet.setPositionY(ARENA_HEIGHT - bullet.getSize());
            bullet.setSpeedY(-bullet.getSpeedY());
            bullet.setBounceCount(bullet.getBounceCount() + 1);
        }
    }

    // Hits and game over 

    private void checkBulletHits() {
        for (int bulletIndex = bullets.size() - 1;
                bulletIndex >= 0;
                bulletIndex--) {

            Bullet currentBullet = bullets.get(bulletIndex);
            Rectangle bulletBounds = currentBullet.getBounds();

            if (bulletBounds.intersects(playerOneTank.getBounds())) {
                playerOneTank.setLives(playerOneTank.getLives() - 1);
                bullets.remove(bulletIndex);
            } else if (bulletBounds.intersects(playerTwoTank.getBounds())) {
                playerTwoTank.setLives(playerTwoTank.getLives() - 1);
                bullets.remove(bulletIndex);
            }
        }
    }

    private void checkForGameOver() {
        boolean playerOneIsDead = playerOneTank.getLives() <= 0;
        boolean playerTwoIsDead = playerTwoTank.getLives() <= 0;

        if (playerOneIsDead) {
            winnerText = "Player 2 wins!";
            gameIsOver = true;
        } else if (playerTwoIsDead) {
            winnerText = "Player 1 wins!";
            gameIsOver = true;
        }

        if (gameIsOver) {
            gameTimer.stop();
            pressedKeys.clear();
        }
    }

    //Keyboard

    @Override
    public void keyPressed(KeyEvent event) {
        int pressedKeyCode = event.getKeyCode();
        pressedKeys.add(pressedKeyCode);

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
        // Required by KeyListener.
    }

    //Drawing

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        // Background texture.
        if (backgroundImage != null) {
            graphics.drawImage(
                    backgroundImage,
                    0, 0,
                    ARENA_WIDTH, ARENA_HEIGHT,
                    this
            );
        }

        // Draw the arena, tanks, bullets and lives.
        drawWalls(graphics);
        drawTank(graphics, playerOneTank, playerOneTankImage);
        drawTank(graphics, playerTwoTank, playerTwoTankImage);
        drawBullets(graphics);
        drawLives(graphics);

        if (!gameIsOver && gameState == GameState.BUILDING) {
            drawBuildPhase(graphics);
        }

        if (gameIsOver) {
            drawGameOver(graphics);
        }
    }

    private void drawGameOver(Graphics graphics) {
        graphics.setFont(gameOverFont);
        graphics.setColor(Color.BLACK);

        drawCenteredText(
                graphics,
                winnerText,
                ARENA_HEIGHT / 2
        );

        // Replay button.
        graphics.setFont(new Font("Arial", Font.BOLD, 28));
        graphics.setColor(Color.GREEN);
        graphics.fillRect(400, 400, 200, 60);

        graphics.setColor(Color.BLACK);
        graphics.drawRect(400, 400, 200, 60);
        graphics.drawString("REPLAY", 435, 438);
    }

    private void drawCenteredText(Graphics graphics,
            String text, int textY) {

        FontMetrics fontMetrics = graphics.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(text);
        int textX = (ARENA_WIDTH - textWidth) / 2;

        graphics.drawString(text, textX, textY);
    }

    private void drawWalls(Graphics graphics) {
        for (WallBlock currentBlock : wallBlocks) {
            Rectangle blockBounds = currentBlock.getBounds();

            Image imageToDraw;

            if (currentBlock.isIndestructible()) {
                imageToDraw = steelWallImage;
            } else {
                imageToDraw = brickWallImage;
            }

            if (imageToDraw != null) {
                graphics.drawImage(
                        imageToDraw,
                        blockBounds.x,
                        blockBounds.y,
                        blockBounds.width,
                        blockBounds.height,
                        this
                );
            } else {
                // Simple fallback if a wall texture is missing.
                graphics.setColor(
                        currentBlock.isIndestructible()
                                ? Color.GRAY : new Color(150, 90, 40)
                );

                graphics.fillRect(
                        blockBounds.x,
                        blockBounds.y,
                        blockBounds.width,
                        blockBounds.height
                );
            }
        }
    }

    private void drawBullets(Graphics graphics) {
        for (Bullet currentBullet : bullets) {
            if (bulletImage != null) {
                graphics.drawImage(
                        bulletImage,
                        currentBullet.getPositionX(),
                        currentBullet.getPositionY(),
                        currentBullet.getSize(),
                        currentBullet.getSize(),
                        this
                );
            } else {
                graphics.setColor(Color.BLACK);

                graphics.fillOval(
                        currentBullet.getPositionX(),
                        currentBullet.getPositionY(),
                        currentBullet.getSize(),
                        currentBullet.getSize()
                );
            }
        }
    }

    private void drawLives(Graphics graphics) {
        drawHearts(graphics, playerOneTank.getLives(), 20, 8);
        drawHearts(graphics, playerTwoTank.getLives(), 800, 8);
    }

    private void drawHearts(Graphics graphics,
            int numberOfLives, int startX, int startY) {

        for (int heartNumber = 0;
                heartNumber < numberOfLives;
                heartNumber++) {

            int heartX = startX
                    + heartNumber * (HEART_SIZE + SPACE_BETWEEN_HEARTS);

            if (heartImage != null) {
                graphics.drawImage(
                        heartImage,
                        heartX,
                        startY,
                        HEART_SIZE,
                        HEART_SIZE,
                        this
                );
            } else {
                graphics.setColor(Color.RED);
                graphics.fillOval(
                        heartX,
                        startY,
                        HEART_SIZE,
                        HEART_SIZE
                );
            }
        }
    }

    private void drawBuildPhase(Graphics graphics) {
        graphics.setFont(livesFont);
        graphics.setColor(Color.BLACK);

        drawCenteredText(graphics, "BUILD PHASE", 30);

        drawCenteredText(
                graphics,
                "Move: W A S D / arrow keys     Place: Tab / Enter",
                55
        );

        graphics.setColor(Color.GREEN);
        graphics.drawString(
                "Walls to place: "
                        + playerOneBuilder.getObjectsLeftToPlace(),
                20, 55
        );

        graphics.setColor(Color.RED);
        graphics.drawString(
                "Walls to place: "
                        + playerTwoBuilder.getObjectsLeftToPlace(),
                800, 55
        );

        drawShape(graphics, playerOneBuilder, Color.BLUE);
        drawShape(graphics, playerTwoBuilder, Color.RED);
    }

    private void drawShape(Graphics graphics,
            PlayerBuilder builder, Color playerColor) {

        if (builder.isFinished()) {
            return;
        }

        WallShape shape = builder.getCurrentShape();

        boolean canBePlaced = shapeCanBePlaced(
                shape,
                builder.getPositionX(),
                builder.getPositionY()
        );

        Color fillColor;

        if (canBePlaced) {
            fillColor = new Color(0, 200, 0, 140);
        } else {
            fillColor = new Color(220, 0, 0, 140);
        }

        for (int blockIndex = 0;
                blockIndex < shape.getBlockCount();
                blockIndex++) {

            Rectangle blockRectangle = getBlockRectangle(
                    shape,
                    blockIndex,
                    builder.getPositionX(),
                    builder.getPositionY()
            );

            graphics.setColor(fillColor);

            graphics.fillRect(
                    blockRectangle.x,
                    blockRectangle.y,
                    blockRectangle.width,
                    blockRectangle.height
            );

            graphics.setColor(playerColor);

            graphics.drawRect(
                    blockRectangle.x,
                    blockRectangle.y,
                    blockRectangle.width,
                    blockRectangle.height
            );
        }
    }

    private void drawTank(Graphics graphics,
            Tank tank, Image tankImage) {

        Graphics2D graphics2D = (Graphics2D) graphics.create();

        int centerX = tank.getPositionX() + tank.getSize() / 2;
        int centerY = tank.getPositionY() + tank.getSize() / 2;

        double angleInDegrees = getRotationAngle(
                tank.getFacingDirection()
        );

        graphics2D.rotate(
                Math.toRadians(angleInDegrees),
                centerX,
                centerY
        );

        if (tankImage != null) {
            graphics2D.drawImage(
                    tankImage,
                    tank.getPositionX(),
                    tank.getPositionY(),
                    tank.getSize(),
                    tank.getSize(),
                    this
            );
        } else {
            graphics2D.setColor(tank.getPositionX() < ARENA_WIDTH / 2
                    ? Color.GREEN : Color.RED);

            graphics2D.fillRect(
                    tank.getPositionX(),
                    tank.getPositionY(),
                    tank.getSize(),
                    tank.getSize()
            );
        }

        graphics2D.dispose();
    }

    private double getRotationAngle(Direction direction) {
        if (direction == Direction.UP_RIGHT) {
            return 45;
        }

        if (direction == Direction.RIGHT) {
            return 90;
        }

        if (direction == Direction.DOWN_RIGHT) {
            return 135;
        }

        if (direction == Direction.DOWN) {
            return 180;
        }

        if (direction == Direction.DOWN_LEFT) {
            return 225;
        }

        if (direction == Direction.LEFT) {
            return 270;
        }

        if (direction == Direction.UP_LEFT) {
            return 315;
        }

        return 0;
    }
}

