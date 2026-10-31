package pananakop;

import javax.swing.SwingUtilities;

public class Game implements Runnable {

    private GameWindow gameWindow;
    private GamePanel gamePanel;
    private Thread gameThread;
    private final int FPS_SET = 120;

    public Game() {
        gamePanel = new GamePanel();
        gameWindow = new GameWindow(gamePanel);
        SwingUtilities.invokeLater(gamePanel::requestFocusInWindow);
        startGameLoop();
    }


    private void startGameLoop() {
        gameThread = new Thread(this);
        gameThread.start();
    }
    @Override 
    public void run() {

        // Game loop 
        double timePerFrame = 1000000000.0 / FPS_SET;
        long lastFrameTime = System.nanoTime();
        int frames = 0;
        long lastcheck = System.currentTimeMillis();
        long now = System.nanoTime();
        while(true) {

            now = System.nanoTime();
            if(System.nanoTime() - lastFrameTime >= timePerFrame) {
              
                gamePanel.repaint();
                lastFrameTime = now;
                frames++;
            }

            
        if (System.currentTimeMillis() - lastcheck >=  1000 ) { // Print every second
            
            lastcheck = System.currentTimeMillis();
            System.out.println("FPS: " + frames);
            frames = 0; // Reset frame count
        }
        
        }
    }
}
