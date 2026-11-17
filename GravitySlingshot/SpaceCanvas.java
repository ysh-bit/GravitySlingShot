import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;

public class SpaceCanvas extends JPanel {
    private final GameEngine engine;
    private final int[][] stars = new int[110][3];
    private Point dragStart = null;
    private Point dragCurrent = null;

    public SpaceCanvas(GameEngine engine) {
        this.engine = engine;
        setBackground(new Color(11, 15, 26));
        generateStarfield();
        setupMouseControls();
    }

    private void generateStarfield() {
        Random rand = new Random(42);
        for (int i = 0; i < stars.length; i++) {
            stars[i][0] = rand.nextInt(1000);
            stars[i][1] = rand.nextInt(650);
            stars[i][2] = 1 + rand.nextInt(2);
        }
    }

    private void setupMouseControls() {

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Probe probe = engine.getProbe();
                if (probe.isLaunched()) return;
                double dist = new Vector2D(e.getX(), e.getY()).subtract(probe.getPosition()).magnitude();
                if (dist <= 45) {
                    dragStart = new Point((int) probe.getPosition().x, (int) probe.getPosition().y);
                    dragCurrent = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragStart != null && dragCurrent != null) {
                    Vector2D pull = new Vector2D(dragStart.x - dragCurrent.x, dragStart.y - dragCurrent.y);
                    if (pull.magnitude() > 10) {
                        Vector2D cappedVel = pull.scale(0.11);
                        if (cappedVel.magnitude() > 22) {
                            cappedVel = cappedVel.normalize().scale(22);
                        }
                        engine.fireProbe(cappedVel);
                    }
                }
                dragStart = null;
                dragCurrent = null;
                engine.setAimVector(null);
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    dragCurrent = e.getPoint();
                    Vector2D pull = new Vector2D(dragStart.x - dragCurrent.x, dragStart.y - dragCurrent.y);
                    Vector2D cappedVel = pull.scale(0.11);
                    if (cappedVel.magnitude() > 22) {
                        cappedVel = cappedVel.normalize().scale(22);
                    }
                    engine.setAimVector(cappedVel);
                    repaint();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int[] s : stars) {
            g2d.setColor(new Color(200, 215, 245, 110));
            g2d.fillOval(s[0], s[1], s[2], s[2]);
        }

        engine.getWormhole().render(g2d);
        for (Planet p : engine.getPlanets()) {
            p.render(g2d);
        }

        List<Vector2D> predicted = engine.getPredictedPath();
        for (int i = 0; i < predicted.size(); i++) {
            Vector2D pt = predicted.get(i);
            int alpha = Math.max(30, 220 - (i * 5));
            g2d.setColor(new Color(255, 255, 255, alpha));
            g2d.fill(new Ellipse2D.Double(pt.x - 2, pt.y - 2, 4, 4));
        }

        List<Vector2D> trail = engine.getProbeTrail();
        g2d.setColor(new Color(0, 230, 255, 140));
        for (int i = 1; i < trail.size(); i++) {
            Vector2D p1 = trail.get(i - 1);
            Vector2D p2 = trail.get(i);
            g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
        }

        if (dragStart != null && dragCurrent != null) {
            g2d.setColor(new Color(255, 195, 0));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawLine(dragStart.x, dragStart.y, dragCurrent.x, dragCurrent.y);
            g2d.setStroke(new BasicStroke(1f));
        }

        engine.getProbe().render(g2d);

        if (engine.isPaused()) {
            g2d.setColor(new Color(0, 0, 0, 140));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 28));
            g2d.drawString("SIMULATION PAUSED", getWidth() / 2 - 145, getHeight() / 2);
        }
    }
}
