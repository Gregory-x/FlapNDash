import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;

// To do: - background image fix, change background images when gravity flips
//        - make pipes read when gravity flips and flip gravity after the bird passes through the pipes, also score system similarly
//        - draw portal looking thingy between the pipes when gravity flips
//        - animation to jump into mine when pressing on start and dying in lava or ether when dying/losing
//        - make gravity flip when actually going through the 2 types
//        - texture pipes
//        - portal between pipes, effect?
//        - less gravity?
//        When I start the game I need that the current background is removed and I use the specific one for the game, also when I press on new game the last rendered backgrounds should be cleared and only use mine
public class FlapNDash {

    JFrame f; // frame
    List<BufferedImage> backgroundImages; // arr of bg images
    BufferedImage characterImg, pipeImg, startBackgroundImage, endBackgroundImage;
    boolean gameRunning = false; // state of the game
    boolean start_screen = true;
    boolean end_screen = false;
    JLabel scoreLabel;
    int score = 0;
    int birdY = 0, birdVelocity = 0;
    int backgroundX1 = 0, backgroundX2;
    int BACKGROUND_SPEED = 4;
    int currentBackgroundIndex = 0;
    List<Rectangle> pipes = new ArrayList<>();
    Random random = new Random();
    int gravity = 1;
    int n;  
    final int JUMP_STRENGTH = -10;
    final int PIPE_SPACING = 400;
    final int PIPE_WIDTH = 80;
    final int PIPE_GAP = 200;
    int PIPE_SPEED = 4;
    private String readyText = "Ready?";
    private float textAlpha = 1.0f; // Opacity value for fading
    private boolean isFading = false;
    private boolean draw_Notification = false;

    int pipeCounter = 0;  // Counter for passed pipes
    boolean gravityFlipped = false;  // Track if gravity is flipped
    Color specialPipeColor = Color.RED;  // Color for the pipe where gravity flips

    JButton newGameButton;
    JButton exitGameButton;

    // object constructor
    public FlapNDash() {
        // main init
        f = new JFrame("Flap n Dash");
        //n = (int) (Math.random() * (8 - 5 + 1)) + 3;
        //System.out.println(n);
        backgroundImages = new ArrayList<>();
        try {
            backgroundImages.add(ImageIO.read(new File("image/w_cave1.jpg"))); // 0
            backgroundImages.add(ImageIO.read(new File("image/w_cave2.jpg")));
            backgroundImages.add(ImageIO.read(new File("image/w_cave1_flipped.jpg")));
            backgroundImages.add(ImageIO.read(new File("image/w_cave2_flipped.jpg"))); // 3

            //backgroundImages.add(ImageIO.read(new File("image/background2.png")));
            characterImg = ImageIO.read(new File("image/stefan.png"));
            //pipeImg = ImageIO.read(new File("image/pipe.png"));
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
                if (draw_Notification)
                {
                    FontMetrics fm = g.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(readyText)) / 2; // Center the text
                    int y = getHeight() / 2;
                    g.drawString(readyText, x, y);
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

                // Render pipes
                for (int i = 0; i < pipes.size(); i++) {
                    Rectangle pipe = pipes.get(i);

                    // Check if the pipe is the special one where gravity flips
                    if (gravityFlipped) {
                        g.setColor(specialPipeColor);  // Special color for gravity-flip pipe
                    } else {
                        g.setColor(Color.LIGHT_GRAY);  // Normal pipe color
                    }
                    g.fillRect(pipe.x, pipe.y, pipe.width, pipe.height);
                }
            }
        };

        panel.setLayout(null);

        scoreLabel = new JLabel("Score: " + score);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 30));
        scoreLabel.setBounds(10, 10, 200, 50);
        scoreLabel.setForeground(new Color(173, 216, 230));
        panel.add(scoreLabel);

        JButton startButton = new JButton("Start");
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
                //panel.repaint();
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

        f.setSize(700, 900);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
        f.setResizable(false);
        f.setFocusable(true);

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
            
        if (gravityFlipped) {
            birdVelocity -= gravity;  // Gravity is reversed (pulling bird upwards)
        } else {
            birdVelocity += gravity;  // Normal gravity (pulling bird downwards)
        }
        birdY += birdVelocity;
    
        Rectangle bird = new Rectangle(100, birdY, 50, 50);
        

        for (int i = 0; i < pipes.size(); i++) {
            Rectangle pipe = pipes.get(i);
            pipe.x -= PIPE_SPEED;

            if (pipe.x + pipe.width < 0) {
                pipes.remove(i);
                i--;
            }

            if (bird.intersects(pipe)) {
                gameOver();
            }
        }

        // Add new pipes every time the previous pipe goes off-screen
        if (pipes.isEmpty() || pipes.get(pipes.size() - 1).x < f.getWidth() - PIPE_SPACING) {
            score ++;
            scoreLabel.setText("Score: " + score);
            addPipe();
        }
        if(score == n && score > 0)
        {
            System.out.println("Time to flip grav");
            gravityFlipped = !gravityFlipped;
            n += (int) (Math.random() * (10 - 5 + 1)) + 5;
            System.out.println(n);
            /* 
            BACKGROUND_SPEED = 2;
            PIPE_SPEED = 2; */
            gravity_flipped_stuff();
        }
    
        if (birdY > f.getHeight() || birdY < 0) {
            gameOver();
        }
    }
    
    private void gravity_flipped_stuff(){
        // Simulate score update for testing
            BACKGROUND_SPEED = 2;
            PIPE_SPEED = 2;

            // Start fading effect
            isFading = true;
            // Create a timer for fading text
            Timer fadeTimer = new Timer(500, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isFading) {
                        draw_Notification = true;
                        textAlpha -= 0.1f; // Decrease opacity
                        if (textAlpha <= 0) {
                            textAlpha = 0; // Cap the opacity at 0
                            ((Timer) e.getSource()).stop(); // Stop fading timer
                            isFading = false; // Mark fading as complete
                            draw_Notification = false;
                        }
                        f.repaint(); // Repaint to update text visibility
                    }
                }
            });
            fadeTimer.start();

            // Timer to reset speeds after 1.5 seconds
            new Timer(500, event -> {
                BACKGROUND_SPEED = 4; // Reset to original speed
                PIPE_SPEED = 4; // Reset to original speed
            }).start();
        
    }
    private void jump() {
        birdVelocity = JUMP_STRENGTH;
    }

    private void jump_backwards(){
        birdVelocity = -JUMP_STRENGTH;
    }

    private void addPipe() {
        int height = 50 + random.nextInt(400);
        int pipeX = f.getWidth();
        
        if (!pipes.isEmpty()) {
            Rectangle lastPipe = pipes.get(pipes.size() - 1);
            pipeX = lastPipe.x + PIPE_SPACING;
        }

        // Create pipes with normal color
        pipes.add(new Rectangle(pipeX, f.getHeight() - height, PIPE_WIDTH, height));
        pipes.add(new Rectangle(pipeX, 0, PIPE_WIDTH, f.getHeight() - height - PIPE_GAP));
    }

    private void resetGame() {
        pipes.clear();
        n = 5 + (int)(Math.random() * 11);
        score = 0;
        scoreLabel.setText("Score: " + score);
        birdY = (f.getHeight() - 50) / 2; 
        birdVelocity = 0; 
        gravityFlipped = false;  // Reset gravity state
        pipeCounter = 0;  // Reset pipe counter
        currentBackgroundIndex = 0;
        draw_Notification = false;  
        n = (int) (Math.random() * (8 - 5 + 1)) + 3;
        System.out.println(n);
        addPipe();
    }

    private void gameOver() {
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
