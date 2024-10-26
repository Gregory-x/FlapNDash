import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
/* 
// To do: - MUST: portals,
//        - animation to jump into mine when pressing on start and dying in lava or ether when dying/losing
//        - texture pipes
//        - portal effect?
*/       
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
    int pipes_added = 0;
    List<Rectangle> pipes = new ArrayList<>();
    private Rectangle portalPipe = null;
    Random random = new Random();
    int gravity = 1;
    int n;  
    final int JUMP_STRENGTH = -10;
    final int PIPE_SPACING = 400;
    final int PIPE_WIDTH = 80;
    final int PIPE_GAP = 200;
    int PIPE_SPEED = 4;
    private String readyText = "Gravity Flipped!";
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
        pipes.clear();
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

                // Draw portal first (before pipes)
                if (portalPipe != null) {
                    g2d.setColor(new Color(0, 255, 255, 128)); // Cyan with transparency
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

        if(score == n && score > 0)
        {
            System.out.println("Time to flip grav");
            gravityFlipped = !gravityFlipped;
            n += (int) (Math.random() * (10 - 5 + 1)) + 5;
            System.out.println(n);
            /* 
            BACKGROUND_SPEED = 2;
            PIPE_SPEED = 2; */
            portalPipe = null;
            gravity_flipped_stuff();
        }
        
        if (birdY > f.getHeight() || birdY < 0) {
            gameOver();
        }
    }
    
    private void gravity_flipped_stuff() {
        BACKGROUND_SPEED = 2;
        PIPE_SPEED = 2;
        
        // Reset text properties
        textAlpha = 1.0f;
        draw_Notification = true;
        readyText = "Gravity Flip!";
        
        // Create a timer for fading text
        Timer fadeTimer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (textAlpha > 0) {
                    textAlpha -= 0.05f;
                    if (textAlpha <= 0) {
                        textAlpha = 0;
                        draw_Notification = false;
                        ((Timer)e.getSource()).stop();
                    }
                }
                f.repaint();
            }
        });
        fadeTimer.start();
    
        // Reset speeds after delay
        new Timer(1500, event -> {
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
        
        // Check if this is the set of pipes where we should add the portal
        if (score == n - 2) {
            portalPipe = new Rectangle(
                pipeX,  
                f.getHeight() - height - PIPE_GAP,  
                PIPE_WIDTH,  
                PIPE_GAP  
            );
            System.out.println("Portal added at score: " + score + ", n: " + n);
        }
        
        pipes_added++;
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
        portalPipe = null;  // Reset portal
        pipes_added = 0;    // Reset pipe counter
        currentBackgroundIndex = 0;
        draw_Notification = false;  
        n = (int) (Math.random() * (8 - 5 + 1)) + 3;
        System.out.println(n);
        addPipe();
    }

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