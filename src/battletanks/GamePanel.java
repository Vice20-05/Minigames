package battletanks;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// The arena where everything is drawn.
public class GamePanel extends JPanel implements Updatable, KeyListener {

    public static final int ARENA_WIDTH = 1000;
    public static final int ARENA_HEIGHT = 700;

    private static final int FRAME_DELAY_MILLISECONDS = 16;

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

    private static final String GAME_NAME = "Tank Battle";

    // The two buttons on the game over screen (they are centered in the arena).
    private static final int BUTTON_WIDTH = 260;
    private static final int BUTTON_HEIGHT = 60;
    private static final int BUTTON_X = (ARENA_WIDTH - BUTTON_WIDTH) / 2;
    private static final int REPLAY_BUTTON_Y = 400;
    private static final int TITLE_BUTTON_Y = 480;

    // Both threads (game loop and Swing) use the game data,
    // so every access to it is done inside synchronized (gameLock).
    private final Object gameLock = new Object();

    private Tank playerOneTank;
    private Tank playerTwoTank;

    private ArrayList<Bullet> bullets;
    private ArrayList<WallBlock> wallBlocks;

    private GameState gameState;

    private PlayerBuilder playerOneBuilder;
    private PlayerBuilder playerTwoBuilder;

    private Random random;

    private GameImages gameImages;
    private FallbackPainter fallbackPainter;
    private MapBuilder mapBuilder;

    private GameLoopTask gameLoopTask;
    private Thread gameLoopThread;

    private HashSet<Integer> pressedKeys;

    private boolean leftShiftIsDown;
    private boolean rightShiftIsDown;

    private boolean playerOnePlaceWasDown;   // was the place key (Tab) down in the previous frame?
    private boolean playerTwoPlaceWasDown;   // was the place key (Enter) down in the previous frame?

    private boolean gameIsOver;
    private String winnerText;
    private int winnerPlayerNumber;   // 1 or 2, 0 while the game is running

    // Game over steps: 1. ask the names and save  2. show the two buttons
    private boolean nameStepIsStarted;
    private boolean nameStepIsFinished;

    private ScreenNavigator screenNavigator;
    private ScoreRepository scoreRepository;

    private Font livesFont;
    private Font gameOverFont;

    public GamePanel(ScreenNavigator screenNavigator,
            ScoreRepository scoreRepository) {

        this.screenNavigator = screenNavigator;
        this.scoreRepository = scoreRepository;

        setPreferredSize(new Dimension(ARENA_WIDTH, ARENA_HEIGHT));
        setBackground(Color.WHITE);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);   // needed, otherwise Swing takes the Tab key for itself

        pressedKeys = new HashSet<Integer>();
        bullets = new ArrayList<Bullet>();
        wallBlocks = new ArrayList<WallBlock>();
        random = new Random();

        gameImages = new GameImages();
        fallbackPainter = new FallbackPainter();
        mapBuilder = new MapBuilder();

        gameIsOver = false;
        winnerText = "";
        winnerPlayerNumber = 0;
        nameStepIsStarted = false;
        nameStepIsFinished = false;

        livesFont = new Font("Consolas", Font.BOLD, 16);
        gameOverFont = new Font("Impact", Font.PLAIN, 70);

        addKeyListener(this);

        // Clicking the replay button starts a new game,
        // clicking the title button goes back to the title screen.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleGameOverClick(e.getX(), e.getY());
            }
        });

        playerOneBuilder = new PlayerBuilder();
        playerTwoBuilder = new PlayerBuilder();

        resetGame();
        startImageLoaderThread();
    }

    // Threads

    // THREAD 2: loads the pictures while the game already runs.
    private void startImageLoaderThread() {
        Thread imageLoaderThread = new Thread(
                new ImageLoaderTask(gameImages, this),
                "Image-Loader"
        );

        imageLoaderThread.setDaemon(true);
        imageLoaderThread.start();
    }

    // THREAD 1: runs the game loop. Started when the panel is shown...
    @Override
    public void addNotify() {
        super.addNotify();
        startGameLoopThread();
    }

    // ...and stopped when the panel is removed from the window.
    @Override
    public void removeNotify() {
        stopGameLoopThread();
        super.removeNotify();
    }

    private void startGameLoopThread() {
        stopGameLoopThread();

        gameLoopTask = new GameLoopTask(this, FRAME_DELAY_MILLISECONDS);
        gameLoopThread = new Thread(gameLoopTask, "Game-Loop");
        gameLoopThread.setDaemon(true);   // does not keep the program alive after the window is closed
        gameLoopThread.start();
    }

    private void stopGameLoopThread() {
        if (gameLoopTask != null) {
            gameLoopTask.stop();
        }

        gameLoopTask = null;
        gameLoopThread = null;
    }

    //Game initialization

    private void resetGame() {
        synchronized (gameLock) {
            gameIsOver = false;
            winnerText = "";
            winnerPlayerNumber = 0;
            nameStepIsStarted = false;
            nameStepIsFinished = false;

            pressedKeys.clear();
            bullets.clear();
            wallBlocks.clear();

            leftShiftIsDown = false;
            rightShiftIsDown = false;
            playerOnePlaceWasDown = false;
            playerTwoPlaceWasDown = false;

            createTanks();

            // Random map (one of 5) and a random background.
            mapBuilder.buildRandomMap(wallBlocks);
            gameImages.pickRandomBackground();

            startBuildPhase(OBJECTS_IN_FIRST_BUILD_PHASE);
        }

        repaint();
    }

    private void replayGame() {
        resetGame();
        requestFocusInWindow();
    }

    // Game over screen

    // The buttons only work after the names were asked and saved.
    private void handleGameOverClick(int clickX, int clickY) {
        boolean buttonsAreVisible;

        synchronized (gameLock) {
            buttonsAreVisible = gameIsOver && nameStepIsFinished;
        }

        if (!buttonsAreVisible) {
            return;
        }

        boolean clickIsInsideButtonColumn = clickX >= BUTTON_X
                && clickX <= BUTTON_X + BUTTON_WIDTH;

        boolean clickIsOnReplayButton = clickIsInsideButtonColumn
                && clickY >= REPLAY_BUTTON_Y
                && clickY <= REPLAY_BUTTON_Y + BUTTON_HEIGHT;

        boolean clickIsOnTitleButton = clickIsInsideButtonColumn
                && clickY >= TITLE_BUTTON_Y
                && clickY <= TITLE_BUTTON_Y + BUTTON_HEIGHT;

        if (clickIsOnReplayButton) {
            replayGame();
        } else if (clickIsOnTitleButton) {
            screenNavigator.showTitleScreen();
        }
    }

    // The dialogs must open on the Swing thread, not on the game loop thread.
    private void requestNameDialog() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                askForNamesAndSaveResult();
            }
        });
    }

    private void askForNamesAndSaveResult() {
        String playerOneName = askForPlayerName("Player 1 name:", "Player 1");
        String playerTwoName = askForPlayerName("Player 2 name:", "Player 2");

        int winnerNumber;

        synchronized (gameLock) {
            winnerNumber = winnerPlayerNumber;
        }

        String winnerName;
        String loserName;

        if (winnerNumber == 1) {
            winnerName = playerOneName;
            loserName = playerTwoName;
        } else {
            winnerName = playerTwoName;
            loserName = playerOneName;
        }

        saveResultToDatabase(winnerName, loserName);

        // Now the two buttons can appear.
        synchronized (gameLock) {
            nameStepIsFinished = true;
        }

        repaint();
        requestFocusInWindow();
    }

    // Asks one name. Cancel or an empty answer gives the default name.
    private String askForPlayerName(String question, String defaultName) {
        String enteredName = JOptionPane.showInputDialog(
                this,
                question,
                winnerText,
                JOptionPane.PLAIN_MESSAGE
        );

        if (enteredName == null) {
            return defaultName;
        }

        String trimmedName = enteredName.trim();

        if (trimmedName.isEmpty()) {
            return defaultName;
        }

        return trimmedName;
    }

    // If the database fails, the player sees a message and the game still goes on.
    private void saveResultToDatabase(String winnerName, String loserName) {
        if (scoreRepository == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "No database is connected, the result was not saved.",
                    "Save error",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            scoreRepository.saveGameResult(GAME_NAME, winnerName, loserName);
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not save the result:\n" + exception.getMessage(),
                    "Save error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
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

    // Called by the game loop thread once per frame.
    @Override
    public void update() {
        boolean mustAskForNames = false;

        synchronized (gameLock) {
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

            // The game just ended: the names must be asked exactly once.
            if (gameIsOver && !nameStepIsStarted) {
                nameStepIsStarted = true;
                mustAskForNames = true;
            }
        }

        repaint();

        // Called outside the lock, so the dialog cannot block the painting.
        if (mustAskForNames) {
            requestNameDialog();
        }
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
            winnerPlayerNumber = 2;
            gameIsOver = true;
        } else if (playerTwoIsDead) {
            winnerText = "Player 1 wins!";
            winnerPlayerNumber = 1;
            gameIsOver = true;
        }

        if (gameIsOver) {
            pressedKeys.clear();
        }
    }

    //Keyboard

    @Override
    public void keyPressed(KeyEvent event) {
        int pressedKeyCode = event.getKeyCode();

        synchronized (gameLock) {
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
    }

    @Override
    public void keyReleased(KeyEvent event) {
        int releasedKeyCode = event.getKeyCode();

        synchronized (gameLock) {
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
    }

    @Override
    public void keyTyped(KeyEvent event) {
        // Required by KeyListener.
    }

    //Drawing

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        // The game loop thread must not change the lists while we draw them.
        synchronized (gameLock) {
            drawBackground(graphics);

            // Draw the arena, tanks, bullets and lives.
            drawWalls(graphics);
            drawTank(graphics, playerOneTank,
                    gameImages.getPlayerOneTankImage(), Color.GREEN);
            drawTank(graphics, playerTwoTank,
                    gameImages.getPlayerTwoTankImage(), Color.RED);
            drawBullets(graphics);
            drawLives(graphics);

            if (!gameIsOver && gameState == GameState.BUILDING) {
                drawBuildPhase(graphics);
            }

            if (gameIsOver) {
                drawGameOver(graphics);
            }
        }
    }

    // Background texture, or a plain color if no background could be loaded.
    private void drawBackground(Graphics graphics) {
        Image backgroundImage = gameImages.getCurrentBackgroundImage();

        if (backgroundImage != null) {
            graphics.drawImage(
                    backgroundImage,
                    0, 0,
                    ARENA_WIDTH, ARENA_HEIGHT,
                    this
            );
        } else {
            graphics.setColor(new Color(225, 220, 200));
            graphics.fillRect(0, 0, ARENA_WIDTH, ARENA_HEIGHT);
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

        // The buttons appear only after the names were asked and saved.
        if (!nameStepIsFinished) {
            return;
        }

        graphics.setFont(new Font("Arial", Font.BOLD, 28));

        drawButton(graphics, "REPLAY", REPLAY_BUTTON_Y, Color.GREEN);
        drawButton(graphics, "TITLE SCREEN", TITLE_BUTTON_Y, Color.ORANGE);
    }

    private void drawButton(Graphics graphics,
            String buttonText, int buttonY, Color buttonColor) {

        graphics.setColor(buttonColor);
        graphics.fillRect(BUTTON_X, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);

        graphics.setColor(Color.BLACK);
        graphics.drawRect(BUTTON_X, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);

        // The text is centered because the buttons are centered in the arena.
        drawCenteredText(graphics, buttonText, buttonY + 40);
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
                imageToDraw = gameImages.getSteelWallImage();
            } else {
                imageToDraw = gameImages.getBrickWallImage();
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
                fallbackPainter.drawWall(
                        graphics,
                        blockBounds.x,
                        blockBounds.y,
                        blockBounds.width,
                        blockBounds.height,
                        currentBlock.isIndestructible()
                );
            }
        }
    }

    private void drawBullets(Graphics graphics) {
        Image bulletImage = gameImages.getBulletImage();

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
                fallbackPainter.drawBullet(
                        graphics,
                        currentBullet.getPositionX(),
                        currentBullet.getPositionY(),
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

        Image heartImage = gameImages.getHeartImage();

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
                fallbackPainter.drawHeart(
                        graphics,
                        heartX,
                        startY,
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
            Tank tank, Image tankImage, Color fallbackColor) {

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
            // The picture is missing, so the tank is drawn with Swing shapes.
            fallbackPainter.drawTank(
                    graphics2D,
                    tank.getPositionX(),
                    tank.getPositionY(),
                    tank.getSize(),
                    fallbackColor
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