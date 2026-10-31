package pananakop;
import javax.swing.JPanel;
import javax.swing.Timer;
import inputs.KeyboardInputs;
import inputs.MouseInputs;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


public class GamePanel extends JPanel {

    private static final int PLAYER_SIZE = 180;
    private static final int FRAME_CANVAS_SIZE = 800;
    private static final int FRAME_CHARACTER_HEIGHT = 720;
    private static final int ATTACK_STATE_DURATION = 3000;
    private static final int GROUND_LEVEL = 690;
    private MouseInputs mouseInputs;
    private float xDelta = 100;
    private float yDelta = GROUND_LEVEL - PLAYER_SIZE;
    private float verticalSpeed;
    private boolean grounded = true;
    private boolean movingLeft;
    private boolean movingRight;
    private boolean sprinting;
    private BufferedImage[] idleAnimation;
    private BufferedImage[] attackIdleAnimation;
    private BufferedImage[] walkAnimation;
    private BufferedImage[] runAnimation;
    private BufferedImage[] attackAnimation;
    private int currentFrame;
    private boolean walking;
    private boolean running;
    private boolean attackPlaying;
    private boolean combatIdle;
    private boolean facingLeft;
    private Timer animationTimer;
    private Timer attackStateTimer;
    private Timer physicsTimer;


    public GamePanel() {
        mouseInputs = new MouseInputs(this);
        setBackground(new Color(62, 62, 62));

        idleAnimation = normalizeFrames(loadAnimation(loadImage("idle.png"), 4));
        walkAnimation = normalizeFrames(loadAnimation(loadImage("walk.png"), 5));
        runAnimation = normalizeFrames(loadAnimation(loadImage("running.png"), 11));
        BufferedImage[] attackFrames = normalizeFrames(
            loadGridAnimation(loadImage("attack-actions.png"), 6, 2)
        );
        attackIdleAnimation = Arrays.copyOfRange(attackFrames, 0, 4);
        attackAnimation = Arrays.copyOfRange(attackFrames, 6, 12);

        animationTimer = new Timer(220, e -> {
            if (attackPlaying) {
                currentFrame++;
                if (currentFrame >= getAnimationFrameCount()) {
                    attackPlaying = false;
                    currentFrame = 0;
                    updateAnimationSpeed();
                }
            } else {
                currentFrame = (currentFrame + 1) % getAnimationFrameCount();
            }
            repaint();
        });
        attackStateTimer = new Timer(ATTACK_STATE_DURATION, e -> {
            combatIdle = false;
            if (!attackPlaying) {
                currentFrame = 0;
                updateAnimationSpeed();
            }
            repaint();
        });
        attackStateTimer.setRepeats(false);
        animationTimer.start();
        physicsTimer = new Timer(16, e -> updatePlayer());
        physicsTimer.start();
        
        setPanelSize();
        setFocusable(true);
        addKeyListener(new KeyboardInputs(this));
        addMouseListener(mouseInputs);
        addMouseMotionListener(mouseInputs);
    }

    private int getAnimationFrameCount() {
        if (attackPlaying) {
            return attackAnimation.length;
        }
        if (running) {
            return runAnimation.length;
        }
        if (walking) {
            return walkAnimation.length;
        }
        return combatIdle ? attackIdleAnimation.length : idleAnimation.length;
    }

    private BufferedImage[] getCurrentAnimation() {
        if (attackPlaying) {
            return attackAnimation;
        }
        if (running) {
            return runAnimation;
        }
        if (walking) {
            return walkAnimation;
        }
        return combatIdle ? attackIdleAnimation : idleAnimation;
    }

    private BufferedImage[] loadAnimation(BufferedImage sheet, int frameCount) {
        BufferedImage[] frames = new BufferedImage[frameCount];
        for (int i = 0; i < frameCount; i++) {
            int left = i * sheet.getWidth() / frameCount;
            int right = (i + 1) * sheet.getWidth() / frameCount;
            frames[i] = sheet.getSubimage(left, 0, right - left, sheet.getHeight());
        }
        return frames;
    }

    private BufferedImage[] loadGridAnimation(BufferedImage sheet, int columns, int rows) {
        BufferedImage[] frames = new BufferedImage[columns * rows];
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int left = column * sheet.getWidth() / columns;
                int right = (column + 1) * sheet.getWidth() / columns;
                int top = row * sheet.getHeight() / rows;
                int bottom = (row + 1) * sheet.getHeight() / rows;
                frames[row * columns + column] = sheet.getSubimage(
                    left, top, right - left, bottom - top
                );
            }
        }
        return frames;
    }

    private BufferedImage[] normalizeFrames(BufferedImage[] frames) {
        BufferedImage[] normalized = new BufferedImage[frames.length];
        int availableHeight = FRAME_CHARACTER_HEIGHT;

        for (int i = 0; i < frames.length; i++) {
            BufferedImage sourceFrame = frames[i];
            BufferedImage frame = new BufferedImage(
                sourceFrame.getWidth(),
                sourceFrame.getHeight(),
                BufferedImage.TYPE_INT_ARGB_PRE
            );
            int left = frame.getWidth();
            int top = frame.getHeight();
            int right = -1;
            int bottom = -1;

            for (int y = 0; y < frame.getHeight(); y++) {
                for (int x = 0; x < frame.getWidth(); x++) {
                    int pixel = sourceFrame.getRGB(x, y);
                    int alpha = pixel >>> 24;
                    if (alpha > 0) {
                        frame.setRGB(x, y, pixel);
                    }
                    if (alpha > 16) {
                        left = Math.min(left, x);
                        top = Math.min(top, y);
                        right = Math.max(right, x);
                        bottom = Math.max(bottom, y);
                    }
                }
            }

            BufferedImage canvas = new BufferedImage(
                FRAME_CANVAS_SIZE,
                FRAME_CANVAS_SIZE,
                BufferedImage.TYPE_INT_ARGB
            );
            if (right >= left && bottom >= top) {
                int contentWidth = right - left + 1;
                int contentHeight = bottom - top + 1;
                double scale = (double) availableHeight / contentHeight;
                int width = (int) Math.round(contentWidth * scale);
                int x = (FRAME_CANVAS_SIZE - width) / 2;
                int y = FRAME_CANVAS_SIZE - availableHeight;

                Graphics2D frameGraphics = canvas.createGraphics();
                frameGraphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
                );
                frameGraphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
                );
                frameGraphics.drawImage(
                    frame, x, y, x + width, y + availableHeight,
                    left, top, right + 1, bottom + 1, null
                );
                frameGraphics.dispose();
            }
            normalized[i] = canvas;
        }
        return normalized;
    }

    private BufferedImage loadImage(String fileName) {
        try (InputStream stream = getClass().getResourceAsStream("/" + fileName)) {
            BufferedImage image = stream == null ? null : ImageIO.read(stream);
            if (image == null) {
                image = ImageIO.read(new File("resourses", fileName));
            }
            if (image == null) {
                throw new IOException("Unsupported or invalid image: " + fileName);
            }
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load player image: " + fileName, e);
        }
    }

    public void setMovementState(boolean walking, boolean running) {
        boolean isMoving = walking || running;
        if (this.walking != isMoving || this.running != running) {
            this.walking = isMoving;
            this.running = running;
            if (!attackPlaying && !combatIdle) {
                currentFrame = 0;
                updateAnimationSpeed();
            }
            repaint();
        }
    }

    private void updateAnimationSpeed() {
        int frameDelay = attackPlaying ? 90 : running ? 85 : walking ? 120 : combatIdle ? 160 : 220;
        animationTimer.setDelay(frameDelay);
        animationTimer.setInitialDelay(frameDelay);
        animationTimer.restart();
    }

    public void startAttack() {
        if (!attackPlaying) {
            attackPlaying = true;
            combatIdle = true;
            currentFrame = 0;
            updateAnimationSpeed();
            attackStateTimer.restart();
            repaint();
        }
    }

    public void setFacingLeft(boolean facingLeft) {
        this.facingLeft = facingLeft;
        repaint();
    }

    public void setMovementInput(boolean movingLeft, boolean movingRight, boolean sprinting) {
        this.movingLeft = movingLeft;
        this.movingRight = movingRight;
        this.sprinting = sprinting;
        setMovementState(movingLeft || movingRight, sprinting && (movingLeft || movingRight));
    }

    public void jump() {
        if (grounded) {
            verticalSpeed = -12;
            grounded = false;
        }
    }

    private void updatePlayer() {
        int horizontalSpeed = sprinting ? 5 : 3;
        if (movingLeft != movingRight) {
            xDelta += movingLeft ? -horizontalSpeed : horizontalSpeed;
        }
        xDelta = Math.max(0, Math.min(getWidth() - PLAYER_SIZE, xDelta));

        verticalSpeed = Math.min(verticalSpeed + 0.55f, 14);
        yDelta += verticalSpeed;
        grounded = false;

        if (yDelta + PLAYER_SIZE >= GROUND_LEVEL) {
            yDelta = GROUND_LEVEL - PLAYER_SIZE;
            verticalSpeed = 0;
            grounded = true;
        }
        repaint();
    }

    public void setPanelSize() {
        Dimension size = new Dimension(1280, 800);
        setMinimumSize(size);
        setPreferredSize(size);
        setMaximumSize(size);
    }

    public void changexDelta(int value) {
        this.xDelta += value;
        repaint(); // Request a repaint to update the panel
    }

    public void changeyDelta(int value) {
        this.yDelta += value;
        repaint(); // Request a repaint to update the panel
    }
    public void serRectPosition(int x, int y) {
        this.xDelta = x;
        this.yDelta = y;
        verticalSpeed = 0;
        grounded = false;
        repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics = (Graphics2D) g.create();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setRenderingHint(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY
        );
        drawPlayer(graphics);
        graphics.setColor(new Color(24, 39, 27, 210));
        graphics.fillRoundRect(18, 18, 355, 34, 12, 12);
        graphics.setColor(new Color(255, 242, 207));
        graphics.drawString("A / D  Move     W / SPACE  Jump     SHIFT  Run     Click  Attack", 30, 40);
        graphics.dispose();
    }

    private void drawPlayer(Graphics2D graphics) {
        BufferedImage frame = getCurrentAnimation()[currentFrame];
        int x = (int) xDelta;
        int y = (int) yDelta;

        if (facingLeft) {
            graphics.drawImage(
                frame,
                x + PLAYER_SIZE,
                y,
                x,
                y + PLAYER_SIZE,
                0,
                0,
                frame.getWidth(),
                frame.getHeight(),
                null
            );
        } else {
            graphics.drawImage(frame, x, y, PLAYER_SIZE, PLAYER_SIZE, null);
        }
    }

    private void drawPlayerShadow(Graphics2D graphics) {
        graphics.setColor(new Color(34, 47, 28, 90));
        graphics.fillOval(35, 162, 112, 13);
    }

    private void drawLeg(Graphics2D graphics, int hipX, int hipY, double swing, int leg, Color skin) {
        int kneeX = hipX + (leg == 0 ? -8 : 9) + (int) swing;
        int kneeY = 133 + (int) Math.abs(swing) / 3;
        int ankleX = kneeX + (leg == 0 ? -3 : 5);
        int ankleY = 158;

        Path2D legShape = new Path2D.Double();
        legShape.moveTo(hipX, hipY);
        legShape.quadTo(kneeX - 2, kneeY - 5, kneeX, kneeY);
        legShape.lineTo(ankleX, ankleY);
        graphics.setColor(new Color(63, 48, 39));
        graphics.setStroke(new BasicStroke(17, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(legShape);
        graphics.setColor(skin);
        graphics.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(legShape);

        graphics.setColor(new Color(84, 55, 37));
        graphics.fillRoundRect(ankleX - 8, ankleY - 17, 16, 22, 6, 6);
        graphics.setColor(new Color(177, 127, 69));
        for (int wrap = 0; wrap < 3; wrap++) {
            int wrapY = ankleY - 14 + wrap * 5;
            graphics.drawLine(ankleX - 7, wrapY, ankleX + 7, wrapY - 2);
        }
        graphics.setColor(new Color(81, 52, 36));
        graphics.fillRoundRect(ankleX - 12, ankleY + 1, 25, 9, 7, 7);
        graphics.setColor(new Color(201, 151, 87));
        graphics.drawLine(ankleX - 7, ankleY + 4, ankleX + 8, ankleY + 4);
    }

    private void drawBody(Graphics2D graphics) {
        Path2D torso = new Path2D.Double();
        torso.moveTo(70, 61);
        torso.quadTo(89, 52, 108, 63);
        torso.lineTo(113, 101);
        torso.quadTo(93, 111, 73, 100);
        torso.closePath();
        graphics.setColor(new Color(52, 43, 36));
        graphics.fill(torso);
        graphics.setColor(new Color(158, 91, 55));
        graphics.fill(torso);

        graphics.setColor(new Color(116, 55, 40));
        Path2D shirt = new Path2D.Double();
        shirt.moveTo(74, 62);
        shirt.lineTo(87, 58);
        shirt.lineTo(101, 63);
        shirt.lineTo(110, 97);
        shirt.lineTo(77, 99);
        shirt.closePath();
        graphics.fill(shirt);

        Path2D sash = new Path2D.Double();
        sash.moveTo(78, 59);
        sash.quadTo(89, 63, 96, 79);
        sash.lineTo(109, 99);
        sash.lineTo(99, 103);
        sash.lineTo(86, 84);
        sash.quadTo(79, 71, 72, 67);
        sash.closePath();
        graphics.setColor(new Color(192, 52, 43));
        graphics.fill(sash);
        graphics.setColor(new Color(238, 184, 88));
        graphics.setStroke(new BasicStroke(1.5f));
        graphics.draw(sash);
        graphics.drawLine(83, 68, 101, 96);
        graphics.drawLine(87, 66, 105, 94);

        graphics.setColor(new Color(204, 157, 77));
        graphics.fillRoundRect(74, 96, 39, 10, 4, 4);
        graphics.setColor(new Color(93, 54, 35));
        graphics.fillRoundRect(88, 96, 11, 10, 3, 3);
        graphics.setColor(new Color(234, 195, 106));
        graphics.drawRoundRect(89, 97, 9, 8, 3, 3);

        graphics.setColor(new Color(104, 57, 40));
        graphics.fillRoundRect(79, 105, 30, 12, 4, 4);
        graphics.setColor(new Color(202, 157, 78));
        for (int tassel = 0; tassel < 4; tassel++) {
            graphics.drawLine(82 + tassel * 7, 115, 80 + tassel * 7, 124);
        }
    }

    private void drawArm(Graphics2D graphics, int shoulderX, int shoulderY, double swing, Color skin) {
        int elbowX = 71 + (int) swing;
        int elbowY = 91;
        int handX = 87 + (int) swing;
        int handY = 100;
        graphics.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(new Color(65, 45, 35));
        graphics.drawLine(shoulderX, shoulderY, elbowX, elbowY);
        graphics.drawLine(elbowX, elbowY, handX, handY);
        graphics.setColor(skin);
        graphics.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(shoulderX, shoulderY, elbowX, elbowY);
        graphics.drawLine(elbowX, elbowY, handX, handY);
        graphics.setColor(new Color(101, 57, 39));
        graphics.setStroke(new BasicStroke(1.5f));
        graphics.drawLine(shoulderX - 2, shoulderY + 2, shoulderX + 3, shoulderY + 8);
        graphics.drawLine(shoulderX + 3, shoulderY + 8, shoulderX - 1, shoulderY + 13);
        graphics.drawLine(shoulderX + 5, shoulderY + 4, shoulderX + 1, shoulderY + 10);
        graphics.setColor(new Color(91, 57, 38));
        graphics.fillRoundRect(elbowX - 7, elbowY - 6, 15, 11, 4, 4);
        graphics.setColor(new Color(192, 144, 73));
        graphics.drawLine(elbowX - 5, elbowY - 2, elbowX + 5, elbowY - 4);
    }

    private void drawHead(Graphics2D graphics) {
        graphics.setColor(new Color(68, 43, 34));
        Path2D ponytail = new Path2D.Double();
        ponytail.moveTo(84, 31);
        ponytail.curveTo(69, 13, 61, 29, 55, 42);
        ponytail.curveTo(48, 54, 59, 62, 67, 69);
        ponytail.curveTo(64, 54, 77, 48, 85, 43);
        ponytail.closePath();
        graphics.fill(ponytail);

        Path2D hair = new Path2D.Double();
        hair.moveTo(73, 42);
        hair.curveTo(69, 26, 81, 15, 94, 18);
        hair.curveTo(111, 18, 117, 31, 108, 45);
        hair.lineTo(104, 59);
        hair.lineTo(97, 48);
        hair.lineTo(78, 55);
        hair.closePath();
        graphics.fill(hair);

        graphics.setColor(new Color(166, 96, 58));
        graphics.fillOval(79, 24, 29, 34);
        graphics.setColor(new Color(196, 124, 75));
        graphics.fillOval(98, 36, 12, 15);
        graphics.setColor(new Color(74, 45, 35));
        Path2D hairTop = new Path2D.Double();
        hairTop.moveTo(76, 39);
        hairTop.curveTo(74, 20, 91, 14, 104, 22);
        hairTop.curveTo(113, 27, 113, 34, 108, 40);
        hairTop.curveTo(99, 34, 91, 34, 84, 42);
        hairTop.closePath();
        graphics.fill(hairTop);
        graphics.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(84, 28, 97, 21);
        graphics.setColor(new Color(168, 48, 39));
        graphics.fillOval(77, 27, 8, 7);
        graphics.setColor(new Color(226, 179, 87));
        graphics.drawOval(77, 27, 8, 7);
        graphics.setColor(new Color(224, 179, 91));
        graphics.fillOval(98, 39, 3, 3);
        graphics.setColor(new Color(49, 39, 33));
        graphics.drawLine(105, 44, 109, 45);
        graphics.setColor(new Color(232, 186, 98));
        graphics.drawLine(84, 57, 96, 59);
    }

    private void drawShield(Graphics2D graphics) {
        Polygon shield = new Polygon(
            new int[] { 117, 143, 157, 154, 145, 130, 117 },
            new int[] { 61, 58, 76, 112, 130, 119, 91 },
            7
        );
        graphics.setColor(new Color(54, 43, 35));
        graphics.fillPolygon(shield);
        graphics.setColor(new Color(137, 83, 45));
        Polygon innerShield = new Polygon(
            new int[] { 121, 142, 152, 149, 142, 132, 121 },
            new int[] { 65, 63, 78, 108, 124, 114, 91 },
            7
        );
        graphics.fillPolygon(innerShield);
        graphics.setColor(new Color(210, 166, 91));
        graphics.setStroke(new BasicStroke(2));
        graphics.drawPolygon(innerShield);
        graphics.drawLine(136, 69, 137, 116);
        graphics.drawLine(125, 84, 148, 101);
        graphics.drawLine(147, 80, 126, 104);
        graphics.setColor(new Color(229, 193, 113));
        graphics.fillOval(132, 88, 9, 12);
    }

    private void drawSword(Graphics2D graphics, double attackProgress) {
        double angle = 0.9;
        if (attackPlaying) {
            double[] attackAngles = { 2.45, 2.1, 1.35, 0.55, -0.25, -0.85 };
            int frame = Math.min((int) (attackProgress * attackAngles.length), attackAngles.length - 1);
            angle = attackAngles[frame];
        } else if (walking || running) {
            angle += Math.sin(currentFrame * Math.PI * 2 / Math.max(1, getAnimationFrameCount())) * 0.12;
        }

        AffineTransform original = graphics.getTransform();
        graphics.translate(88, 99);
        graphics.rotate(angle);
        graphics.setColor(new Color(83, 53, 38));
        graphics.fillRoundRect(-3, -4, 13, 8, 4, 4);
        graphics.setColor(new Color(221, 175, 93));
        graphics.fillRect(7, -7, 4, 14);
        Polygon blade = new Polygon(
            new int[] { 10, 59, 67, 59, 10 },
            new int[] { -3, -4, 0, 4, 3 },
            5
        );
        graphics.setColor(new Color(205, 211, 199));
        graphics.fillPolygon(blade);
        graphics.setColor(new Color(246, 239, 209));
        graphics.setStroke(new BasicStroke(1));
        graphics.drawLine(13, -2, 60, -3);
        graphics.setTransform(original);
    }

}
