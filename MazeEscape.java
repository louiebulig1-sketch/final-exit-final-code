import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Timer;

public class MazeEscape extends JFrame {
    
    // Game configuration
    private static final int ROWS = 15;
    private static final int COLS = 19;
    private static final int CELL_SIZE = 28;
    private static final double PLAYER_SPEED = 4.0;
    private static final double ENEMY_BASE_SPEED = 0.5;
    private static final double ENEMY_SPEED_INCREMENT = 0.1;
    private static final int POWER_FREEZE_MS = 8000;
    private static final int NORMAL_ORB_SCORE = 10;
    private static final int POWER_ORB_SCORE = 50;
    private static final int ESCAPE_BONUS = 200;
    private static final int[] LEVEL_TIMES = {120, 90, 60};
    private static final int FLASH_DISTANCE = 4;
    private static final int INITIAL_LIVES = 3;
    
    // Game state
    private int level = 1;
    private int score = 0;
    private int lives = INITIAL_LIVES;
    private int[][] maze;
    private Player player;
    private Enemy enemy;
    private int orbsLeft = 0;
    private double timer;
    private boolean levelRunning = false;
    private long lastTickTime = 0;
    private int[] realExit;
    private boolean enemyNear = false;
    private boolean gameOver = false;
    private int combo = 0;
    private long lastOrbTime = 0;
    private boolean gameStarted = false;
    
    // UI elements
    private GamePanel gamePanel;
    private JLabel scoreLabel;
    private JLabel levelLabel;
    private JLabel timerLabel;
    private JLabel orbsLabel;
    private JLabel livesLabel;
    private JLabel comboLabel;
    private JProgressBar timeBar;
    private JButton restartBtn;
    private JButton nextBtn;
    private JLabel messageLabel;
    private JPanel startScreen;
    private JPanel gameContainer;
    
    // Input handling
    private Set<Integer> keysPressed = new HashSet<>();
    private int lastDirection = -1;
    
    // Heart labels for lives display
    private JLabel[] heartLabels = new JLabel[INITIAL_LIVES];
    
    // Arrow buttons
    private JPanel arrowPanel;
    private ArrowButton upBtn, downBtn, leftBtn, rightBtn;
    
    public MazeEscape() {
        setTitle("MAZE ESCAPE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(20, 20, 40));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create HUD panel with high contrast
        JPanel hudPanel = new JPanel(new GridLayout(7, 1, 5, 8));
        hudPanel.setBackground(new Color(30, 30, 50));
        hudPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        hudPanel.setPreferredSize(new Dimension(250, 0));
        
        // Stats section
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        statsPanel.setBackground(new Color(30, 30, 50));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Score
        JPanel scorePanel = new JPanel(new BorderLayout());
        scorePanel.setBackground(new Color(30, 30, 50));
        JLabel scoreTitle = new JLabel("SCORE:");
        scoreTitle.setFont(new Font("Arial", Font.BOLD, 16));
        scoreTitle.setForeground(Color.CYAN);
        scoreLabel = new JLabel("0");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 20));
        scoreLabel.setForeground(Color.WHITE);
        scorePanel.add(scoreTitle, BorderLayout.WEST);
        scorePanel.add(scoreLabel, BorderLayout.EAST);
        
        // Level
        JPanel levelPanel = new JPanel(new BorderLayout());
        levelPanel.setBackground(new Color(30, 30, 50));
        JLabel levelTitle = new JLabel("LEVEL:");
        levelTitle.setFont(new Font("Arial", Font.BOLD, 16));
        levelTitle.setForeground(Color.CYAN);
        levelLabel = new JLabel("1");
        levelLabel.setFont(new Font("Arial", Font.BOLD, 20));
        levelLabel.setForeground(Color.WHITE);
        levelPanel.add(levelTitle, BorderLayout.WEST);
        levelPanel.add(levelLabel, BorderLayout.EAST);
        
        // Time
        JPanel timePanel = new JPanel(new BorderLayout());
        timePanel.setBackground(new Color(30, 30, 50));
        JLabel timeTitle = new JLabel("TIME:");
        timeTitle.setFont(new Font("Arial", Font.BOLD, 16));
        timeTitle.setForeground(Color.CYAN);
        timerLabel = new JLabel("--");
        timerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        timerLabel.setForeground(Color.WHITE);
        timePanel.add(timeTitle, BorderLayout.WEST);
        timePanel.add(timerLabel, BorderLayout.EAST);
        
        // Time progress bar
        timeBar = new JProgressBar(0, 100);
        timeBar.setValue(100);
        timeBar.setForeground(new Color(0, 255, 0));
        timeBar.setBackground(new Color(60, 60, 80));
        timeBar.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        
        statsPanel.add(scorePanel);
        statsPanel.add(levelPanel);
        statsPanel.add(timePanel);
        statsPanel.add(timeBar);
        
        // Lives and orbs
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        infoPanel.setBackground(new Color(30, 30, 50));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // Lives panel with individual heart labels
        JPanel livesPanel = new JPanel(new BorderLayout());
        livesPanel.setBackground(new Color(30, 30, 50));
        JLabel livesTitle = new JLabel("LIVES:");
        livesTitle.setFont(new Font("Arial", Font.BOLD, 14));
        livesTitle.setForeground(Color.YELLOW);
        livesPanel.add(livesTitle, BorderLayout.WEST);
        
        JPanel heartsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        heartsPanel.setBackground(new Color(30, 30, 50));
        
        // Initialize heart labels
        for (int i = 0; i < INITIAL_LIVES; i++) {
            heartLabels[i] = new JLabel("♥");
            heartLabels[i].setFont(new Font("Arial", Font.BOLD, 18));
            heartLabels[i].setForeground(Color.RED);
            heartsPanel.add(heartLabels[i]);
        }
        
        livesPanel.add(heartsPanel, BorderLayout.CENTER);
        
        // Orbs
        JPanel orbsPanel = new JPanel(new BorderLayout());
        orbsPanel.setBackground(new Color(30, 30, 50));
        JLabel orbsTitle = new JLabel("ORBS:");
        orbsTitle.setFont(new Font("Arial", Font.BOLD, 14));
        orbsTitle.setForeground(Color.YELLOW);
        orbsLabel = new JLabel("0");
        orbsLabel.setFont(new Font("Arial", Font.BOLD, 18));
        orbsLabel.setForeground(Color.WHITE);
        orbsPanel.add(orbsTitle, BorderLayout.WEST);
        orbsPanel.add(orbsLabel, BorderLayout.EAST);
        
        infoPanel.add(livesPanel);
        infoPanel.add(orbsPanel);
        
        // Combo
        comboLabel = new JLabel("COMBO: x1");
        comboLabel.setFont(new Font("Arial", Font.BOLD, 16));
        comboLabel.setForeground(Color.YELLOW);
        comboLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Create arrow panel
        arrowPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        arrowPanel.setBackground(new Color(30, 30, 50));
        arrowPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        arrowPanel.setPreferredSize(new Dimension(180, 180));
        
        // Create arrow buttons
        upBtn = new ArrowButton("↑", KeyEvent.VK_UP);
        downBtn = new ArrowButton("↓", KeyEvent.VK_DOWN);
        leftBtn = new ArrowButton("←", KeyEvent.VK_LEFT);
        rightBtn = new ArrowButton("→", KeyEvent.VK_RIGHT);
        
        // Arrange buttons in grid
        arrowPanel.add(new JLabel()); // Top-left empty
        arrowPanel.add(upBtn);
        arrowPanel.add(new JLabel()); // Top-right empty
        arrowPanel.add(leftBtn);
        arrowPanel.add(new JLabel()); // Center empty
        arrowPanel.add(rightBtn);
        arrowPanel.add(new JLabel()); // Bottom-left empty
        arrowPanel.add(downBtn);
        arrowPanel.add(new JLabel()); // Bottom-right empty
        
        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        buttonPanel.setBackground(new Color(30, 30, 50));
        restartBtn = new JButton("Restart");
        restartBtn.setFont(new Font("Arial", Font.BOLD, 12));
        restartBtn.setBackground(new Color(100, 100, 200));
        restartBtn.setForeground(Color.WHITE);
        nextBtn = new JButton("Next");
        nextBtn.setFont(new Font("Arial", Font.BOLD, 12));
        nextBtn.setBackground(new Color(100, 100, 200));
        nextBtn.setForeground(Color.WHITE);
        buttonPanel.add(restartBtn);
        buttonPanel.add(nextBtn);
        
        // Message
        messageLabel = new JLabel("");
        messageLabel.setFont(new Font("Arial", Font.BOLD, 14));
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setForeground(Color.YELLOW);
        
        // Arrange HUD
        hudPanel.add(statsPanel);
        hudPanel.add(infoPanel);
        hudPanel.add(comboLabel);
        hudPanel.add(arrowPanel);
        hudPanel.add(buttonPanel);
        hudPanel.add(new JLabel()); // Spacer
        hudPanel.add(messageLabel);
        
        // Create game container
        gameContainer = new JPanel(new BorderLayout());
        gameContainer.setBackground(new Color(20, 20, 40));
        
        // Title
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(20, 20, 40));
        titlePanel.setPreferredSize(new Dimension(0, 35));
        
        JLabel gameTitle = new JLabel("MAZE ESCAPE", SwingConstants.CENTER);
        gameTitle.setFont(new Font("Arial", Font.BOLD, 20));
        gameTitle.setForeground(Color.WHITE);
        
        titlePanel.add(gameTitle, BorderLayout.CENTER);
        gameContainer.add(titlePanel, BorderLayout.NORTH);
        
        // Create start screen
        startScreen = new JPanel(new BorderLayout());
        startScreen.setBackground(new Color(20, 20, 40));
        startScreen.setPreferredSize(new Dimension(COLS * CELL_SIZE, ROWS * CELL_SIZE));
        
        JLabel titleLabel = new JLabel("MAZE ESCAPE", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(Color.CYAN);
        
        JLabel startLabel = new JLabel("Press SPACE or Click to Start", SwingConstants.CENTER);
        startLabel.setFont(new Font("Arial", Font.BOLD, 18));
        startLabel.setForeground(Color.YELLOW);
        
        JPanel startButtonPanel = new JPanel(new GridLayout(1, 1));
        JButton startButton = new JButton("START GAME");
        startButton.setFont(new Font("Arial", Font.BOLD, 16));
        startButton.setBackground(new Color(0, 150, 255));
        startButton.setForeground(Color.WHITE);
        startButton.addActionListener(e -> startGame());
        
        startButtonPanel.add(startButton);
        
        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 20, 20));
        centerPanel.setOpaque(false);
        centerPanel.add(titleLabel);
        centerPanel.add(startLabel);
        centerPanel.add(startButtonPanel);
        
        startScreen.add(centerPanel, BorderLayout.CENTER);
        gameContainer.add(startScreen, BorderLayout.CENTER);
        
        // Create game panel
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(COLS * CELL_SIZE, ROWS * CELL_SIZE));
        
        // Add components to main panel
        mainPanel.add(gameContainer, BorderLayout.CENTER);
        mainPanel.add(hudPanel, BorderLayout.EAST);
        
        // Add to frame
        add(mainPanel);
        
        // Focus handling
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        
        // Event handlers
        restartBtn.addActionListener(e -> restartLevel());
        nextBtn.addActionListener(e -> {
            if (level < 3) {
                nextLevel();
            } else {
                showMessage("Congratulations! You completed all levels!");
                newGame();
            }
        });
        
        // Keyboard input
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!gameStarted && e.getKeyCode() == KeyEvent.VK_SPACE) {
                    startGame();
                    return;
                }
                
                keysPressed.add(e.getKeyCode());
                
                // Track last direction for Pac-Man style movement
                int currentDirection = -1;
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
                    currentDirection = 0;
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
                    currentDirection = 1;
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A) {
                    currentDirection = 2;
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D) {
                    currentDirection = 3;
                }
                
                if (currentDirection != -1) {
                    lastDirection = currentDirection;
                }
                
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    togglePause();
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                keysPressed.remove(e.getKeyCode());
            }
        });
        
        // Mouse click for start screen
        startScreen.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!gameStarted) {
                    startGame();
                }
            }
        });
        
        // Initialize game
        setupLevel(level);
        
        // Start game loop
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (gameStarted) {
                    update();
                    gamePanel.repaint();
                }
            }
        }, 0, 16);
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        requestFocusInWindow();
    }
    
    // Arrow button class - FIXED
    private class ArrowButton extends JButton {
        private int keyCode;
        private boolean isPressed = false;
        
        public ArrowButton(String text, int keyCode) {
            super(text);
            this.keyCode = keyCode;
            setFont(new Font("Arial", Font.BOLD, 18));
            setBackground(new Color(100, 100, 200));
            setForeground(Color.WHITE);
            setFocusable(false);
            setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
            setPreferredSize(new Dimension(50, 50));
            
            // Mouse press handler
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    isPressed = true;
                    keysPressed.add(keyCode);
                    
                    // Set last direction for Pac-Man style movement
                    switch (keyCode) {
                        case KeyEvent.VK_UP:
                            lastDirection = 0;
                            break;
                        case KeyEvent.VK_DOWN:
                            lastDirection = 1;
                            break;
                        case KeyEvent.VK_LEFT:
                            lastDirection = 2;
                            break;
                        case KeyEvent.VK_RIGHT:
                            lastDirection = 3;
                            break;
                    }
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    isPressed = false;
                    keysPressed.remove(keyCode);
                }
            });
        }
        
        public boolean isPressed() {
            return isPressed;
        }
    }
    
    private void startGame() {
        gameStarted = true;
        levelRunning = true;
        gameContainer.remove(startScreen);
        gameContainer.add(gamePanel, BorderLayout.CENTER);
        gameContainer.revalidate();
        gameContainer.repaint();
        showMessage("Level " + level + " - Collect all orbs!");
    }
    
    private void setupLevel(int level) {
        this.level = level;
        this.timer = LEVEL_TIMES[Math.min(level - 1, LEVEL_TIMES.length - 1)];
        this.levelRunning = false;
        this.lastTickTime = System.currentTimeMillis();
        this.combo = 0;
        this.lastOrbTime = 0;
        
        // Generate maze
        maze = generateMaze(level);
        
        // Find player start position (center)
        int playerStartRow = ROWS / 2;
        int playerStartCol = COLS / 2;
        while (maze[playerStartRow][playerStartCol] != 0) {
            playerStartRow++;
            playerStartCol++;
            if (playerStartRow >= ROWS) {
                playerStartRow = ROWS / 2 - 1;
            }
            if (playerStartCol >= COLS) {
                playerStartCol = COLS / 2 - 1;
            }
        }
        
        // Find enemy start position (top-left)
        int enemyStartRow = 1;
        int enemyStartCol = 1;
        while (maze[enemyStartRow][enemyStartCol] != 0) {
            enemyStartCol++;
            if (enemyStartCol >= COLS - 1) {
                enemyStartCol = 1;
                enemyStartRow++;
            }
        }
        
        // Initialize player
        player = new Player(playerStartCol * CELL_SIZE, playerStartRow * CELL_SIZE);
        
        // Initialize enemy with proper speed
        double enemySpeed = ENEMY_BASE_SPEED + (level - 1) * ENEMY_SPEED_INCREMENT;
        enemy = new Enemy(enemyStartCol * CELL_SIZE, enemyStartRow * CELL_SIZE, enemySpeed);
        
        // Count orbs
        orbsLeft = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (maze[r][c] == 2 || maze[r][c] == 3) {
                    orbsLeft++;
                }
            }
        }
        
        // Set real exit
        setRealExit();
        
        // Update HUD
        updateHUD();
    }
    
    private int[][] generateMaze(int level) {
        int[][] maze = new int[ROWS][COLS];
        
        // Fill with walls
        for (int r = 0; r < ROWS; r++) {
            Arrays.fill(maze[r], 1);
        }
        
        // Create border
        for (int r = 0; r < ROWS; r++) {
            maze[r][0] = maze[r][COLS - 1] = 0;
        }
        for (int c = 0; c < COLS; c++) {
            maze[0][c] = maze[ROWS - 1][c] = 0;
        }
        
        // Create maze pattern - much more open
        Random rand = new Random();
        
        // Create main paths
        for (int r = 2; r < ROWS - 2; r += 2) {
            for (int c = 2; c < COLS - 2; c += 2) {
                maze[r][c] = 0;
                
                // Create horizontal paths - higher chance
                if (c < COLS - 3 && rand.nextDouble() > 0.1) {
                    maze[r][c + 1] = 0;
                }
                
                // Create vertical paths - higher chance
                if (r < ROWS - 3 && rand.nextDouble() > 0.1) {
                    maze[r + 1][c] = 0;
                }
            }
        }
        
        // Create additional paths - much more open
        for (int r = 1; r < ROWS - 1; r++) {
            for (int c = 1; c < COLS - 1; c++) {
                if (rand.nextDouble() > 0.4) { // Lower threshold for more paths
                    maze[r][c] = 0;
                }
            }
        }
        
        // Ensure spawn areas are clear
        int centerR = ROWS / 2;
        int centerC = COLS / 2;
        for (int r = centerR - 2; r <= centerR + 2; r++) {
            for (int c = centerC - 2; c <= centerC + 2; c++) {
                if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                    maze[r][c] = 0;
                }
            }
        }
        
        // Add level-specific obstacles - fewer obstacles
        int obstaclesToAdd = level * 2; // Reduced from 3
        for (int i = 0; i < obstaclesToAdd; i++) {
            int r = 2 + rand.nextInt(ROWS - 4);
            int c = 2 + rand.nextInt(COLS - 4);
            if (rand.nextDouble() > 0.6 && !isNearSpawn(r, c)) { // Higher threshold
                maze[r][c] = 1;
            }
        }
        
        // Place orbs
        int orbsToPlace = Math.max(25, (ROWS * COLS) / 6); // More orbs
        int powerOrbsToPlace = Math.max(3, orbsToPlace / 10); // More power orbs
        
        // Place normal orbs
        for (int i = 0; i < orbsToPlace; i++) {
            int attempts = 0;
            while (attempts < 100) {
                int r = 1 + rand.nextInt(ROWS - 2);
                int c = 1 + rand.nextInt(COLS - 2);
                if (maze[r][c] == 0 && rand.nextDouble() > 0.5) { // Lower threshold
                    maze[r][c] = 2;
                    break;
                }
                attempts++;
            }
        }
        
        // Place power orbs
        for (int i = 0; i < powerOrbsToPlace; i++) {
            int attempts = 0;
            while (attempts < 100) {
                int r = 1 + rand.nextInt(ROWS - 2);
                int c = 1 + rand.nextInt(COLS - 2);
                if (maze[r][c] == 0 && rand.nextDouble() > 0.8) {
                    maze[r][c] = 3;
                    break;
                }
                attempts++;
            }
        }
        
        // Place exits
        java.util.List<int[]> exitCandidates = new ArrayList<>();
        for (int c = 2; c < COLS - 2; c += 3) {
            if (maze[1][c] == 0) exitCandidates.add(new int[]{1, c});
            if (maze[ROWS - 2][c] == 0) exitCandidates.add(new int[]{ROWS - 2, c});
        }
        for (int r = 2; r < ROWS - 2; r += 3) {
            if (maze[r][1] == 0) exitCandidates.add(new int[]{r, 1});
            if (maze[r][COLS - 2] == 0) exitCandidates.add(new int[]{r, COLS - 2});
        }
        
        Collections.shuffle(exitCandidates);
        int numDoors = level == 2 ? Math.min(3, exitCandidates.size()) : 1;
        
        for (int i = 0; i < numDoors && i < exitCandidates.size(); i++) {
            int[] pos = exitCandidates.get(i);
            maze[pos[0]][pos[1]] = 4;
        }
        
        return maze;
    }
    
    private boolean isNearSpawn(int r, int c) {
        int centerR = ROWS / 2;
        int centerC = COLS / 2;
        return Math.abs(r - centerR) <= 2 && Math.abs(c - centerC) <= 2;
    }
    
    private void setRealExit() {
        java.util.List<int[]> exits = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (maze[r][c] == 4) {
                    exits.add(new int[]{r, c});
                }
            }
        }
        
        if (!exits.isEmpty()) {
            Random rand = new Random();
            realExit = exits.get(rand.nextInt(exits.size()));
        }
    }
    
    private void update() {
        if (!levelRunning || gameOver) return;
        
        long now = System.currentTimeMillis();
        double deltaTime = (now - lastTickTime) / 1000.0;
        lastTickTime = now;
        
        // Update timer
        timer -= deltaTime;
        if (timer <= 0) {
            timer = 0;
            loseLife("Time's up!");
        }
        
        // Update player - FIXED to stop when keys are released
        updatePlayer(deltaTime);
        
        // Update enemy
        updateEnemy(deltaTime);
        
        // Check collisions
        checkCollisions();
        
        // Update HUD
        updateHUD();
        
        // Update combo
        if (now - lastOrbTime > 2000) {
            combo = 0;
            updateHUD();
        }
    }
    
    // FIXED Player movement to stop when keys are released
    private void updatePlayer(double deltaTime) {
        double dx = 0, dy = 0;
        
        // Only move if keys are currently pressed
        if (keysPressed.contains(KeyEvent.VK_UP) || keysPressed.contains(KeyEvent.VK_W)) {
            dy = -PLAYER_SPEED;
            lastDirection = 0; // UP
        } else if (keysPressed.contains(KeyEvent.VK_DOWN) || keysPressed.contains(KeyEvent.VK_S)) {
            dy = PLAYER_SPEED;
            lastDirection = 1; // DOWN
        } else if (keysPressed.contains(KeyEvent.VK_LEFT) || keysPressed.contains(KeyEvent.VK_A)) {
            dx = -PLAYER_SPEED;
            lastDirection = 2; // LEFT
        } else if (keysPressed.contains(KeyEvent.VK_RIGHT) || keysPressed.contains(KeyEvent.VK_D)) {
            dx = PLAYER_SPEED;
            lastDirection = 3; // RIGHT
        }
        // If no movement keys are pressed, dx and dy remain 0 (player stops)
        
        // Calculate new position
        double newX = player.x + dx * deltaTime * 60; // Scale for 60 FPS
        double newY = player.y + dy * deltaTime * 60;
        
        // Check collision with walls - Pac-Man style
        int newCol = (int) (newX / CELL_SIZE);
        int newRow = (int) (newY / CELL_SIZE);
        
        // Check bounds
        if (newRow < 0) newRow = 0;
        if (newRow >= ROWS) newRow = ROWS - 1;
        if (newCol < 0) newCol = 0;
        if (newCol >= COLS) newCol = COLS - 1;
        
        // Check if the new position is walkable
        if (maze[newRow][newCol] != 1) {
            player.x = newX;
            player.y = newY;
            player.col = newCol;
            player.row = newRow;
        } else {
            // Try to slide along walls
            // Try horizontal movement only
            int testCol = (int) (newX / CELL_SIZE);
            if (testCol >= 0 && testCol < COLS && maze[player.row][testCol] != 1) {
                player.x = newX;
                player.col = testCol;
            }
            // Try vertical movement only
            int testRow = (int) (newY / CELL_SIZE);
            if (testRow >= 0 && testRow < ROWS && maze[testRow][player.col] != 1) {
                player.y = newY;
                player.row = testRow;
            }
        }
    }
    
    private void updateEnemy(double deltaTime) {
        // Check if enemy is frozen
        if (enemy.isFrozen()) return;
        
        // Update enemy movement timer
        enemy.updateMovementTimer(deltaTime);
        
        // Only move if enough time has accumulated based on speed
        if (enemy.shouldMove()) {
            // Simple pathfinding - move towards player
            int playerRow = player.row;
            int playerCol = player.col;
            int enemyRow = enemy.row;
            int enemyCol = enemy.col;
            
            // Calculate direction
            int dr = 0, dc = 0;
            if (playerRow < enemyRow) dr = -1;
            else if (playerRow > enemyRow) dr = 1;
            
            if (playerCol < enemyCol) dc = -1;
            else if (playerCol > enemyCol) dc = 1;
            
            // Try to move
            int newRow = enemyRow + dr;
            int newCol = enemyCol + dc;
            
            if (newRow >= 0 && newRow < ROWS && newCol >= 0 && newCol < COLS && 
                maze[newRow][newCol] != 1) {
                enemy.move(newRow, newCol);
            } else {
                // Try alternative direction
                if (dr != 0) {
                    newRow = enemyRow + dr;
                    newCol = enemyCol;
                    if (newRow >= 0 && newRow < ROWS && newCol >= 0 && newCol < COLS && 
                        maze[newRow][newCol] != 1) {
                        enemy.move(newRow, newCol);
                    }
                }
                
                if (dc != 0 && !enemy.hasMoved()) {
                    newRow = enemyRow;
                    newCol = enemyCol + dc;
                    if (newRow >= 0 && newRow < ROWS && newCol >= 0 && newCol < COLS && 
                        maze[newRow][newCol] != 1) {
                        enemy.move(newRow, newCol);
                    }
                }
            }
            
            // Check proximity to player
            int distance = Math.abs(enemy.row - player.row) + Math.abs(enemy.col - player.col);
            enemyNear = distance <= FLASH_DISTANCE;
        }
    }
    
    private void checkCollisions() {
        // Check orb collection
        switch (maze[player.row][player.col]) {
            case 2:
                maze[player.row][player.col] = 0;
                orbsLeft--;
                combo++;
                int baseScore = NORMAL_ORB_SCORE;
                int comboBonus = (combo - 1) * 5;
                score += baseScore + comboBonus;
                lastOrbTime = System.currentTimeMillis();
                playBeep();
                if (orbsLeft == 0) {
                    showMessage("All orbs collected! Find the exit!");
                }
                break;
            case 3:
                maze[player.row][player.col] = 0;
                orbsLeft--;
                score += POWER_ORB_SCORE;
                enemy.freeze(POWER_FREEZE_MS);
                combo = 0;
                playChime();
                showMessage("Enemy frozen for " + (POWER_FREEZE_MS/1000) + " seconds!");
                break;
            case 4:
                // Check if it's the real exit
                if (orbsLeft <= 0 && player.row == realExit[0] && player.col == realExit[1]) {
                    score += ESCAPE_BONUS;
                    if (level < 3) {
                        showMessage("Level " + level + " completed! +" + ESCAPE_BONUS + " points!");
                        nextLevel();
                    } else {
                        showMessage("Congratulations! You completed all levels!");
                        gameOver = true;
                    }
                } else if (orbsLeft > 0) {
                    showMessage("Collect all orbs first!");
                }
                break;
            default:
                break;
        }
        
        // Check enemy collision
        if (enemy.row == player.row && enemy.col == player.col) {
            loseLife("Caught by enemy!");
        }
    }
    
    private void loseLife(String reason) {
        lives--;
        combo = 0;
        
        if (lives <= 0) {
            gameOver = true;
            showMessage("Game Over! Final Score: " + score);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    newGame();
                }
            }, 3000);
        } else {
            showMessage(reason + " Lives remaining: " + lives);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    restartLevel();
                }
            }, 1000);
        }
    }
    
    private void nextLevel() {
        levelRunning = false;
        level++;
        if (level > 3) level = 1;
        
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                setupLevel(level);
                levelRunning = true;
            }
        }, 1000);
    }
    
    private void restartLevel() {
        setupLevel(level);
        levelRunning = true;
    }
    
    private void newGame() {
        level = 1;
        score = 0;
        lives = INITIAL_LIVES;
        gameOver = false;
        setupLevel(level);
        gameStarted = false;
        
        // Show start screen again
        gameContainer.remove(gamePanel);
        gameContainer.add(startScreen, BorderLayout.CENTER);
        gameContainer.revalidate();
        gameContainer.repaint();
    }
    
    private void togglePause() {
        if (!gameStarted) return;
        
        levelRunning = !levelRunning;
        showMessage(levelRunning ? "Game resumed!" : "Game paused! Press SPACE to continue.");
    }
    
    private void showMessage(String message) {
        messageLabel.setText(message);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (messageLabel.getText().equals(message)) {
                    messageLabel.setText("");
                }
            }
        }, 3000);
    }
    
    private void updateHUD() {
        scoreLabel.setText(String.valueOf(score));
        levelLabel.setText(String.valueOf(level));
        timerLabel.setText((int)timer + "s");
        orbsLabel.setText(String.valueOf(orbsLeft));
        
        // Update lives display with individual heart labels
        for (int i = 0; i < INITIAL_LIVES; i++) {
            if (i < lives) {
                heartLabels[i].setText("♥");
                heartLabels[i].setForeground(Color.RED);
            } else {
                heartLabels[i].setText("♡");
                heartLabels[i].setForeground(new Color(100, 100, 100)); // Gray for empty
            }
        }
        
        String comboText = combo > 1 ? "COMBO: x" + combo : "COMBO: x1";
        comboLabel.setText(comboText);
        
        // Update time bar
        double total = LEVEL_TIMES[Math.min(level - 1, LEVEL_TIMES.length - 1)];
        double percent = Math.max(0, Math.min(1, timer / total));
        timeBar.setValue((int)(percent * 100));
        
        // Change time bar color based on time left
        if (percent > 0.5) {
            timeBar.setForeground(new Color(0, 255, 0));
        } else if (percent > 0.25) {
            timeBar.setForeground(new Color(255, 255, 0));
        } else {
            timeBar.setForeground(new Color(255, 0, 0));
        }
    }
    
    private void playBeep() {
        System.out.println("\007");
    }
    
    private void playChime() {
        System.out.println("\007");
    }
    
    // Player class with Pac-Man style movement
    private class Player {
        double x, y;
        int row, col;
        
        Player(double x, double y) {
            this.x = x;
            this.y = y;
            this.row = (int) (y / CELL_SIZE);
            this.col = (int) (x / CELL_SIZE);
        }
    }
    
    // Enemy class - FIXED to use proper speed-based movement
    private class Enemy {
        double x, y;
        int row, col;
        long freezeUntil;
        boolean hasMovedThisFrame;
        double movementTimer;
        double speed; // moves per second
        
        Enemy(double x, double y, double speed) {
            this.x = x;
            this.y = y;
            this.row = (int) (y / CELL_SIZE);
            this.col = (int) (x / CELL_SIZE);
            this.freezeUntil = 0;
            this.hasMovedThisFrame = false;
            this.movementTimer = 0;
            this.speed = speed;
        }
        
        void move(int newRow, int newCol) {
            this.row = newRow;
            this.col = newCol;
            this.x = newCol * CELL_SIZE;
            this.y = newRow * CELL_SIZE;
            this.hasMovedThisFrame = true;
        }
        
        boolean isFrozen() {
            return System.currentTimeMillis() < freezeUntil;
        }
        
        void freeze(long duration) {
            freezeUntil = System.currentTimeMillis() + duration;
        }
        
        boolean hasMoved() {
            boolean moved = hasMovedThisFrame;
            this.hasMovedThisFrame = false;
            return moved;
        }
        
        void updateMovementTimer(double deltaTime) {
            movementTimer += deltaTime;
        }
        
        boolean shouldMove() {
            // Calculate time needed for one move based on speed
            // Higher speed = shorter time between moves
            double moveInterval = 1.0 / speed;
            
            if (movementTimer >= moveInterval) {
                movementTimer = 0; // Reset timer
                return true;
            }
            return false;
        }
    }
    
    // Game panel for rendering with Pac-Man style graphics
    private class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            // Clear canvas
            g.setColor(new Color(20, 20, 40));
            g.fillRect(0, 0, getWidth(), getHeight());
            
            // Draw maze
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    int x = c * CELL_SIZE;
                    int y = r * CELL_SIZE;
                    
                    switch (maze[r][c]) {
                        case 1 -> {
                            // Wall
                            g.setColor(new Color(0, 50, 150));
                            g.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                            g.setColor(new Color(0, 100, 255));
                            g.drawRect(x, y, CELL_SIZE, CELL_SIZE);
                        }
                        case 2 -> // Normal orb
                            drawOrb(g, x + CELL_SIZE/2, y + CELL_SIZE/2, Color.YELLOW);
                        case 3 -> // Power orb
                            drawOrb(g, x + CELL_SIZE/2, y + CELL_SIZE/2, Color.PINK);
                        case 4 -> {
                            // Exit
                            boolean isReal = player.row == r && player.col == c &&
                                    orbsLeft <= 0 && r == realExit[0] && c == realExit[1];
                            g.setColor(isReal ? Color.GREEN : Color.CYAN);
                            g.fillRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);
                            g.setColor(isReal ? Color.DARK_GRAY : Color.BLUE);
                            g.drawRect(x + 3, y + 3, CELL_SIZE - 6, CELL_SIZE - 6);
                        }
                        default -> {
                        }
                    }
                }
            }
            
            // Draw player as Pac-Man style circle
            drawPacman(g, player.x, player.y, lastDirection);
            
            // Draw enemy
            if (enemy.isFrozen()) {
                g.setColor(new Color(150, 150, 255));
            } else if (enemyNear) {
                g.setColor(Color.RED);
            } else {
                g.setColor(Color.ORANGE);
            }
            g.fillOval((int)(enemy.x + 3), (int)(enemy.y + 3), CELL_SIZE - 6, CELL_SIZE - 6);
            
            // Draw enemy eyes
            if (!enemy.isFrozen()) {
                g.setColor(Color.BLACK);
                g.fillOval((int)(enemy.x + 7), (int)(enemy.y + 7), 3, 3);
                g.fillOval((int)(enemy.x + 14), (int)(enemy.y + 7), 3, 3);
            }
            
            // Draw danger overlay if enemy is near
            if (enemyNear) {
                g.setColor(new Color(255, 0, 0, 50));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            
            // Draw combo indicator
            if (combo > 1) {
                g.setColor(Color.YELLOW);
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.drawString("COMBO x" + combo, 5, 20);
            }
        }
        
        private void drawOrb(Graphics g, int x, int y, Color color) {
            // Glow effect
            g.setColor(color);
            g.fillOval(x - 8, y - 8, 16, 16);
            
            // Core
            g.setColor(color.brighter());
            g.fillOval(x - 4, y - 4, 8, 8);
            
            // Inner highlight
            g.setColor(Color.WHITE);
            g.fillOval(x - 2, y - 2, 3, 3);
        }
        
        private void drawPacman(Graphics g, double x, double y, int direction) {
            // Draw Pac-Man body
            g.setColor(Color.CYAN);
            
            // Calculate mouth angle based on direction
            int startAngle = 0;
            int arcAngle = 45;
            
            switch (direction) {
                case 0: // UP
                    startAngle = 90;
                    break;
                case 1: // DOWN
                    startAngle = 270;
                    break;
                case 2: // LEFT
                    startAngle = 180;
                    break;
                case 3: // RIGHT
                    startAngle = 0;
                    break;
                default: // Default to right
                    startAngle = 0;
                    break;
            }
            
            // Draw Pac-Man as a pie slice
            g.fillArc((int)(x + 3), (int)(y + 3), CELL_SIZE - 6, CELL_SIZE - 6, 
                     startAngle, arcAngle * 2);
            
            // Draw Pac-Man glow effect
            g.setColor(new Color(0, 230, 255, 100));
            g.fillOval((int)(x + 1), (int)(y + 1), CELL_SIZE - 2, CELL_SIZE - 2);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MazeEscape());
    }
}