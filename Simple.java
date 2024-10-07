import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;

public class Simple {

    JFrame f;
    BufferedImage backgroundImg, characterImg;

    public Simple() {
        // Create instance of JFrame
        f = new JFrame();

        // Load the background and character images
        try {
            backgroundImg = ImageIO.read(new File("image/background.png")); // Replace with your background image path
            characterImg = ImageIO.read(new File("image/Flappy-Bird.png"));   // Replace with your character image path
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create custom JPanel to handle the background and character drawing
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                
                // Draw background image
                if (backgroundImg != null) {
                    g.drawImage(backgroundImg, 0, 0, getWidth(), getHeight(), this);
                }

                // Draw character image
                if (characterImg != null) {
                    // Position the character in the center of the frame, adjust x and y as needed
                    int characterX = (getWidth() - characterImg.getWidth()) / 2;
                    int characterY = (getHeight() - characterImg.getHeight()) / 2;
                    g.drawImage(characterImg, characterX, characterY, this);
                }
            }
        };

        panel.setLayout(null); // Set layout to null for custom positioning

        // Create button
        JButton b = new JButton("Start");
        b.setBounds(130, 100, 100, 40); // Position the button
        panel.add(b); // Add button to panel

        // Add the panel to the frame
        f.add(panel);

        // Frame settings
        f.setSize(700, 900);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }

    public static void main(String[] args) {
        new Simple();
    }
}
