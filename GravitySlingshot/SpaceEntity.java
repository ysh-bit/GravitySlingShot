import java.awt.*;
import java.awt.geom.Ellipse2D;

interface Renderable {
    void render(Graphics2D g2d);
}

interface Gravitational {
    Vector2D calculatePullOn(Probe probe);
}

class FuelDepletedException extends Exception {
    public FuelDepletedException(String message) {
        super(message);
    }
}

class BlackHoleCollisionException extends Exception {
    public BlackHoleCollisionException(String message) {
        super(message);
    }
}

public abstract class SpaceEntity implements Renderable {
    protected Vector2D position;
    protected double radius;
    protected Color primaryColor;
    protected String name;

    public SpaceEntity(String name, double x, double y, double radius, Color color) {
        this.name = name;
        this.position = new Vector2D(x, y);
        this.radius = radius;
        this.primaryColor = color;
    }

    public Vector2D getPosition() { return position; }
    public double getRadius() { return radius; }
    public String getName() { return name; }
}

class Planet extends SpaceEntity implements Gravitational {
    public static final double G_CONSTANT = 1800.0;
    private final double mass;
    private final GravityWell well;

    public class GravityWell {
        private final double influenceRadius;

        public GravityWell(double influenceRadius) {
            this.influenceRadius = influenceRadius;
        }

        public void drawFieldRings(Graphics2D g2d) {
            float[] dash = {4f, 6f};
            g2d.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
            for (int i = 1; i <= 3; i++) {
                double r = radius + (influenceRadius - radius) * (i / 3.0);
                int alpha = 90 - (i * 22);
                g2d.setColor(new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), Math.max(15, alpha)));
                g2d.draw(new Ellipse2D.Double(position.x - r, position.y - r, r * 2, r * 2));
            }
            g2d.setStroke(new BasicStroke(1f));
        }
    }

    public Planet(String name, double x, double y, double radius, double mass, Color color) {
        super(name, x, y, radius, color);
        this.mass = mass;
        this.well = new GravityWell(radius * 4.2);
    }

    @Override
    public Vector2D calculatePullOn(Probe probe) {
        Vector2D direction = this.position.subtract(probe.getPosition());
        double distance = direction.magnitude();
        if (distance < this.radius) distance = this.radius;
        double forceMag = (G_CONSTANT * this.mass) / (distance * distance);
        return direction.normalize().scale(forceMag);
    }

    @Override
    public void render(Graphics2D g2d) {
        well.drawFieldRings(g2d);
        GradientPaint gp = new GradientPaint(
            (float)(position.x - radius), (float)(position.y - radius), primaryColor.brighter(),
            (float)(position.x + radius), (float)(position.y + radius), primaryColor.darker().darker()
        );
        g2d.setPaint(gp);
        g2d.fill(new Ellipse2D.Double(position.x - radius, position.y - radius, radius * 2, radius * 2));

        g2d.setColor(new Color(220, 230, 240, 180));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2d.drawString(name, (int)(position.x - radius), (int)(position.y + radius + 14));
    }
}

class Probe extends SpaceEntity {
    private Vector2D velocity;
    private final Vector2D spawnPoint;
    private boolean launched;
    private int fuel;

    public Probe(double x, double y) {
        super("Voyager-X", x, y, 8.0, new Color(0, 230, 255));
        this.spawnPoint = new Vector2D(x, y);
        this.velocity = new Vector2D(0, 0);
        this.launched = false;
        this.fuel = 100;
    }

    public void launch(Vector2D launchVelocity) throws FuelDepletedException {
        int fuelCost = (int) Math.min(100, launchVelocity.magnitude() * 8.5);
        if (fuel < fuelCost || fuel <= 0) {
            throw new FuelDepletedException("Insufficient thruster fuel for launch vector!");
        }
        this.fuel -= fuelCost;
        this.velocity = launchVelocity;
        this.launched = true;
    }

    public void applyAcceleration(Vector2D accel, double dt) {
        if (!launched) return;
        this.velocity = this.velocity.add(accel.scale(dt));
        this.position = this.position.add(this.velocity.scale(dt));
    }

    public void reset() {
        this.position = new Vector2D(spawnPoint);
        this.velocity = new Vector2D(0, 0);
        this.launched = false;
    }

    public void refillFuel() { this.fuel = 100; }
    public boolean isLaunched() { return launched; }
    public int getFuel() { return fuel; }

    @Override
    public void render(Graphics2D g2d) {
        g2d.setColor(primaryColor);
        g2d.fill(new Ellipse2D.Double(position.x - radius, position.y - radius, radius * 2, radius * 2));
        g2d.setColor(Color.WHITE);
        g2d.draw(new Ellipse2D.Double(position.x - radius - 2, position.y - radius - 2, (radius + 2) * 2, (radius + 2) * 2));
    }
}

class Wormhole extends SpaceEntity {
    private double pulseAngle = 0;

    public Wormhole(double x, double y, double radius) {
        super("Target Wormhole", x, y, radius, new Color(175, 80, 255));
    }

    public void updatePulse() {
        pulseAngle += 0.08;
    }

    @Override
    public void render(Graphics2D g2d) {
        for (int i = 3; i >= 1; i--) {
            double r = radius * (0.5 + i * 0.25) + Math.sin(pulseAngle + i) * 3.0;
            g2d.setColor(new Color(175, 80, 255, 65 * i));
            g2d.fill(new Ellipse2D.Double(position.x - r, position.y - r, r * 2, r * 2));
        }
        g2d.setColor(new Color(15, 10, 30));
        g2d.fill(new Ellipse2D.Double(position.x - radius * 0.55, position.y - radius * 0.55, radius * 1.1, radius * 1.1));
    }
}
