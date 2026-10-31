package inputs;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Set;

import pananakop.GamePanel;
public class KeyboardInputs implements KeyListener {

    private GamePanel gamePanel;
    private Set<Integer> movementKeys = new HashSet<>();
    private boolean shiftDown;

    public KeyboardInputs( GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }
    
            @Override
            public void keyTyped(KeyEvent e) {
              
            }

            @Override
            public void keyReleased(KeyEvent e) {
                movementKeys.remove(e.getKeyCode());
                if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                    shiftDown = false;
                }
                if (e.getKeyCode() == KeyEvent.VK_A && movementKeys.contains(KeyEvent.VK_D)) {
                    gamePanel.setFacingLeft(false);
                } else if (e.getKeyCode() == KeyEvent.VK_D && movementKeys.contains(KeyEvent.VK_A)) {
                    gamePanel.setFacingLeft(true);
                }
                updateMovementState();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                    shiftDown = true;
                    updateMovementState();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if (movementKeys.add(e.getKeyCode())) {
                        gamePanel.jump();
                    }
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_D) {
                    movementKeys.add(e.getKeyCode());
                    gamePanel.setFacingLeft(e.getKeyCode() == KeyEvent.VK_A);
                    updateMovementState();
                }
            }

    private void updateMovementState() {
        gamePanel.setMovementInput(
            movementKeys.contains(KeyEvent.VK_A),
            movementKeys.contains(KeyEvent.VK_D),
            shiftDown
        );
    }
}
