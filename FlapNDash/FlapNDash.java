import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;
/* 
// To do: - MUST: reset settings to default, gravity flip text improve
//        - animation of falling/jumping into the cave or just falling; animation of floating around in the void?
//        - portal effect?
//        - difficulty menu?
*/       
public class FlapNDash {
    // Swing
    JFrame f;
    JButton newGameButton;
    JButton exitGameButton;
    JLabel scoreLabel;
    JButton startButton;

    List<BufferedImage> backgroundImages; // arr of bg images
    List<Rectangle> pipes = new ArrayList<>();
    BufferedImage characterImg, startBackgroundImage, endBackgroundImage;
    Color specialPipeColor = Color.RED;  // Color for the pipe where gravity flips
    Random random = new Random();
    Rectangle portalPipe = null;
    float textAlpha = 1.0f; // Opacity value for fading

    boolean gameRunning = false, start_screen = true, end_screen = false, gravityFlipped = false; // state of the game
    int score = 0, birdY = 0, birdVelocity = 0, backgroundX1 = 0, backgroundX2, n, pipes_added = 0, currentBackgroundIndex = 0;
    int gravity = 1, PIPE_SPEED = 4, BACKGROUND_SPEED = 4;
    final int JUMP_STRENGTH = -10, PIPE_SPACING = 400, PIPE_WIDTH = 80, PIPE_GAP = 200;

    // constructor
    public FlapNDash() {
        // main init
        f = new JFrame("Flap n Dash");
        backgroundImages = new ArrayList<>();
        try {
            backgroundImages.add(ImageIO.read(new File("image/w_cave1.jpg"))); // 0
            backgroundImages.add(ImageIO.read(new File("image/w_cave2.jpg")));
            backgroundImages.add(ImageIO.read(new File("image/w_cave1_flipped.jpg")));
            backgroundImages.add(ImageIO.read(new File("image/w_cave2_flipped.jpg"))); // 3
            characterImg = ImageIO.read(new File("image/stefan.png"));
            startBackgroundImage = ImageIO.read(new File("image/realistic_cave.jpg"));
            endBackgroundImage = ImageIO.read(new File("image/void.jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if (backgroundImages.isEmpty() || backgroundImages.contains(null)) {
            System.out.println("One or more background images failed to load.");
        }

        backgroundX2 = backgroundImages.get(0).getWidth();
       
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // Draw the appropriate background based on game state
                if (start_screen && startBackgroundImage != null) {
                    g.drawImage(startBackgroundImage, 0, 0, getWidth(), getHeight(), null);
                } else if (end_screen && endBackgroundImage != null) {
                    g.drawImage(endBackgroundImage, 0, 0, getWidth(), getHeight(), null);
                } else if (gameRunning) {
                    if(gravityFlipped)
                        currentBackgroundIndex=2;
                    else
                        currentBackgroundIndex=0;
                    BufferedImage currentBackground = backgroundImages.get(currentBackgroundIndex);
                    if (currentBackground != null) {
                    // Draw the two background images
                        g.drawImage(currentBackground, backgroundX1, 0, null);
                        g.drawImage(currentBackground, backgroundX2, 0, null);
                    }
                }

                // Draw character only if the game is running
                if (gameRunning && characterImg != null) {
                    int newCharacterWidth = 80;  
                    int newCharacterHeight = 80; 
                    int characterX = 100;
            
                    Image scaledImage = characterImg.getScaledInstance(
                        newCharacterWidth, newCharacterHeight, Image.SCALE_SMOOTH);
                    g.drawImage(scaledImage, characterX, birdY, this);
                }

                // Draw portal first (before the pipes are being drawn)
                if (portalPipe != null) {
                    if(!gravityFlipped)
                        g2d.setColor(new Color(128, 0, 128, 128));
                    else g2d.setColor(new Color(255, 0, 0, 128));
                    g2d.fillRect(portalPipe.x, portalPipe.y, portalPipe.width, portalPipe.height);
    
                    // Add portal effect
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2.0f));
                    g2d.drawRect(portalPipe.x, portalPipe.y, portalPipe.width, portalPipe.height);
    
                    // Add swirl effect
                    for(int i = 0; i < portalPipe.height; i += 10) {
                        g2d.drawLine(portalPipe.x, portalPipe.y + i, 
                        portalPipe.x + portalPipe.width, portalPipe.y + i);
                    }
                }

                // Then draw the pipes
                for (Rectangle pipe : pipes) {
                    g2d.setColor(gravityFlipped ? specialPipeColor : Color.LIGHT_GRAY);
                    g2d.fillRect(pipe.x, pipe.y, pipe.width, pipe.height);
                }   
            }
        };

        panel.setLayout(null);

        // Swing labels + buttons 
        scoreLabel = new JLabel("Score: " + score);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 30));
        scoreLabel.setBounds(10, 10, 200, 50);
        scoreLabel.setForeground(new Color(173, 216, 230));
        panel.add(scoreLabel);

        startButton = new JButton("Start");
        startButton.setFont(new Font("Arial", Font.BOLD, 35));
        startButton.setBounds(240, 300, 160, 64);
        panel.add(startButton);

        newGameButton = new JButton("New Game");
        newGameButton.setFont(new Font("Arial", Font.BOLD, 35));
        newGameButton.setBounds(205, 300, 230, 64);
        newGameButton.setVisible(false);  // Initially hidden
        panel.add(newGameButton);

        exitGameButton = new JButton("Exit");
        exitGameButton.setFont(new Font("Arial", Font.BOLD, 35));
        exitGameButton.setBounds(240, 380, 160, 64);
        exitGameButton.setVisible(true);  // Initially shown
        panel.add(exitGameButton);

        // times to run game loop which updates 30 times per second so 30fps
        Timer gameLoop = new Timer(30, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameRunning) {
                    updateGame();
                    panel.repaint();
                }
            }
        });
        
        // start button event click
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gameRunning = true;
                startButton.setVisible(false);
                newGameButton.setVisible(false);
                exitGameButton.setVisible(false);

                resetGame();
                panel.repaint();
                start_screen = false;
                gameLoop.start();
                birdY = (f.getHeight() - 50) / 2;

                addPipe();
            }
        });

        // new game button event
        newGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gameRunning = true;
                end_screen = false;

                resetGame();
                panel.repaint(); 

                newGameButton.setVisible(false);
                exitGameButton.setVisible(false);
            }
        });

        // on exit pressed
        exitGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                gameRunning = false;
                exitGameButton.setVisible(false);
                System.exit(0);
            }
        });
        
        // space pressed
        f.add(panel);
        f.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE && gameRunning) {
                    if(!gravityFlipped){
                        jump();
                    } else {
                        jump_backwards();
                    }
                        
                }
            }
        });

        // settings for the frame, so for the window that we render on
        f.setSize(700, 900);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
        f.setResizable(false);
        f.setFocusable(true);
        
        // centering it
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - f.getWidth()) / 2;
        int y = (screenSize.height - f.getHeight()) / 2;
        f.setLocation(x, y);
    }

    private void updateGame() {
        // Move backgrounds to the left
        backgroundX1 -= BACKGROUND_SPEED;
        backgroundX2 -= BACKGROUND_SPEED;
    
        BufferedImage currentBackground = backgroundImages.get(currentBackgroundIndex);
        int backgroundWidth = currentBackground.getWidth();
        
        // Reset background positions when they go off-screen
        if (backgroundX1 + backgroundWidth <= 0) {
            backgroundX1 = backgroundX2 + backgroundWidth; 
        }
    
        if (backgroundX2 + backgroundWidth <= 0) {
            backgroundX2 = backgroundX1 + backgroundWidth; 
        }
        
        // Update portal position
        if (portalPipe != null) {
            portalPipe.x -= PIPE_SPEED;
            if (portalPipe.x + portalPipe.width < 0) {
            portalPipe = null;
            }
        }

        if (gravityFlipped) {
            birdVelocity -= gravity;  // Gravity is reversed (pulling bird upwards)
        } else {
            birdVelocity += gravity;  // Normal gravity (pulling bird downwards)
        }
        birdY += birdVelocity;
    
        Rectangle bird = new Rectangle(100, birdY, 50, 50);
        boolean scoredThisFrame = false;

        // Update and check pipes
        for (int i = 0; i < pipes.size(); i += 2) { // Increment by 2 since pipes are in pairs
            Rectangle pipe = pipes.get(i);
            pipe.x -= PIPE_SPEED;
            pipes.get(i + 1).x -= PIPE_SPEED; // Move the matching top pipe

            // Score when passing through pipes (check only once per pipe pair)
            if (!scoredThisFrame && pipe.x + pipe.width <= 100 && pipe.x + pipe.width > 100 - PIPE_SPEED) {
                score++;
                scoredThisFrame = true;
                scoreLabel.setText("Score: " + score);
            }

            // Remove pipes that are off screen
            if (pipe.x + pipe.width < 0) {
                pipes.remove(i + 1); // Remove top pipe
                pipes.remove(i);     // Remove bottom pipe
                i -= 2; // Adjust index since we removed two pipes
                continue;
            }

            // Collision detection
            if (bird.intersects(pipe) || bird.intersects(pipes.get(i + 1))) {
                gameOver();
                return;
            }
        }

        // Add new pipes based on spacing, not scoring
        if (pipes.isEmpty() || pipes.get(pipes.size() - 2).x < f.getWidth() - PIPE_SPACING) {
            addPipe();
        }

        // changes gravity based on the score
        if(score == n && score > 0) {
            gravityFlipped = !gravityFlipped;
            n += (int) (Math.random() * (11)) + 5; // generates a number between 0 and 10 + 5 shift so between 5 and 15
            System.out.println(String.format("Next gravity flip at: %d", n));
            portalPipe = null;
            gravity_flipped_stuff();
        }

        // bird touches the top or bottom of the screen
        if (birdY > f.getHeight() || birdY < 0) {
            gameOver();
        }
    }
    
    private void gravity_flipped_stuff() {
        BACKGROUND_SPEED = 2;
        PIPE_SPEED = 2;
    
        // Reset speeds after delay
        new Timer(1000, event -> {
            BACKGROUND_SPEED = 4;
            PIPE_SPEED = 4;
            ((Timer)event.getSource()).stop();
        }).start();
    }

    private void jump() {
        birdVelocity = JUMP_STRENGTH;
    }

    private void jump_backwards(){
        birdVelocity = -JUMP_STRENGTH;
    }

    // handles action with pipes, and also with portal (adds/removes them)
    private void addPipe() {
        int height = 50 + random.nextInt(400);
        int pipeX = f.getWidth();
        
        if (!pipes.isEmpty()) {
            Rectangle lastPipe = pipes.get(pipes.size() - 2); // Get last bottom pipe
            pipeX = lastPipe.x + PIPE_SPACING;
        }
    
        // Create regular pipes
        Rectangle bottomPipe = new Rectangle(pipeX, f.getHeight() - height, PIPE_WIDTH, height);
        Rectangle topPipe = new Rectangle(pipeX, 0, PIPE_WIDTH, f.getHeight() - height - PIPE_GAP);
        pipes.add(bottomPipe);
        pipes.add(topPipe);
        
        // Check if this is the set of pipes where we should add the portal; we use n-2 because the current pipe would be n
        if (score == n - 2) {
            portalPipe = new Rectangle(
                pipeX,  
                f.getHeight() - height - PIPE_GAP,  
                PIPE_WIDTH,  
                PIPE_GAP  
            );
        }
        pipes_added++;
    }

    // resets the game, everything to initial values
    private void resetGame() {
        pipes.clear();
        score = 0;
        scoreLabel.setText("Score: " + score);
        birdY = (f.getHeight() - 50) / 2; 
        birdVelocity = 0; 
        gravityFlipped = false;  // Reset gravity state
        portalPipe = null;  // Reset portal
        pipes_added = 0;
        currentBackgroundIndex = 0; // Reset background
        n = (int) (Math.random() * (11)) + 5; // generates a number between 0 and 10 + 5 shift so between 5 and 15
        System.out.println(String.format("Next gravity flip at: %d", n));
        addPipe();
    }

    // stops the gameloop and displays the game over screen
    private void gameOver() {
        scoreLabel.setText("Score: " + score);
        gameRunning = false;
        pipes.clear();
        end_screen = true;
        newGameButton.setVisible(true);
        exitGameButton.setVisible(true);
    }

    public static void main(String[] args) {
        new FlapNDash();
    }
}