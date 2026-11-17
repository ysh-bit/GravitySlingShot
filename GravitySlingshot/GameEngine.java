import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

public class GameEngine implements Runnable {
    private final List<Planet> planets = new ArrayList<>();
    private final List<Vector2D> probeTrail = new ArrayList<>();
    private final List<Vector2D> predictedPath = new ArrayList<>();

    private Probe probe;
    private Wormhole wormhole;
    private Vector2D aimVector = null;

    private volatile boolean running = true;
    private volatile boolean paused = false;
    private volatile boolean showTrajectory = true;

    private int currentLevel = 1;
    private int score = 0;
    private String statusMessage = "Drag backward from the probe to aim & release to launch!";
    private Runnable uiSyncCallback;

    public GameEngine() {
        loadLevel(1);
        startTrajectoryDaemon();
    }

    public void setUiSyncCallback(Runnable callback) {
        this.uiSyncCallback = callback;
    }

    public synchronized void loadLevel(int level) {
        this.currentLevel = level;
        planets.clear();
        probeTrail.clear();
        predictedPath.clear();
        aimVector = null;

        switch (level) {
            case 1:
                probe = new Probe(140, 320);
                wormhole = new Wormhole(820, 320, 28);
                planets.add(new Planet("Kepler-22b", 480, 230, 46, 850, new Color(45, 195, 175)));
                break;
            case 2:
                probe = new Probe(120, 480);
                wormhole = new Wormhole(840, 150, 26);
                planets.add(new Planet("Terra-Prime", 360, 260, 38, 650, new Color(70, 170, 95)));
                planets.add(new Planet("Aether-Giant", 620, 410, 52, 1100, new Color(65, 130, 235)));
                break;
            case 3:
            default:
                probe = new Probe(110, 310);
                wormhole = new Wormhole(850, 310, 25);
                planets.add(new Planet("Binary-Alpha", 380, 170, 42, 920, new Color(235, 120, 60)));
                planets.add(new Planet("Binary-Beta", 380, 450, 42, 920, new Color(75, 190, 235)));
                planets.add(new Planet("Singularity-Core", 640, 310, 32, 1250, new Color(210, 65, 120)));
                break;
        }
        statusMessage = "Level " + level + " Ready — Drag from probe to slingshot!";
        notifyUi();
    }

    private void startTrajectoryDaemon() {
        Thread predictorThread = new Thread(() -> {
            while (running) {
                try {
                    computeTrajectoryPreview();
                    Thread.sleep(35);
                } catch (InterruptedException ignored) {}
            }
        }, "Trajectory-Predictor-Daemon");
        predictorThread.setDaemon(true);
        predictorThread.setPriority(Thread.MIN_PRIORITY);
        predictorThread.start();
    }

    private synchronized void computeTrajectoryPreview() {
        predictedPath.clear();
        if (!showTrajectory || aimVector == null || probe.isLaunched()) return;

        Probe ghost = new Probe(probe.getPosition().x, probe.getPosition().y);
        try {
            ghost.launch(aimVector);
        } catch (FuelDepletedException e) {
            return;
        }

        double dt = 0.18;
        for (int step = 0; step < 110; step++) {
            Vector2D netAccel = new Vector2D(0, 0);
            for (Planet p : planets) {
                netAccel = netAccel.add(p.calculatePullOn(ghost));
            }
            ghost.applyAcceleration(netAccel, dt);
            if (step % 3 == 0) {
                predictedPath.add(new Vector2D(ghost.getPosition()));
            }

            for (Planet p : planets) {
                if (ghost.getPosition().subtract(p.getPosition()).magnitude() <= p.getRadius()) {
                    return;
                }
            }
        }
    }

    @Override
    public void run() {
        final double dt = 0.18;
        int tick = 0;
        while (running) {
            if (!paused) {
                updatePhysics(dt, tick++);
                notifyUi();
            }
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized void updatePhysics(double dt, int tick) {
        wormhole.updatePulse();
        if (!probe.isLaunched()) return;

        try {
            Vector2D netAccel = new Vector2D(0, 0);
            for (Planet p : planets) {
                netAccel = netAccel.add(p.calculatePullOn(probe));
                double dist = probe.getPosition().subtract(p.getPosition()).magnitude();
                if (dist <= p.getRadius() + probe.getRadius()) {
                    throw new BlackHoleCollisionException("Probe crashed into " + p.getName() + "!");
                }
            }

            probe.applyAcceleration(netAccel, dt);
            if (tick % 2 == 0) {
                probeTrail.add(new Vector2D(probe.getPosition()));
            }

            double goalDist = probe.getPosition().subtract(wormhole.getPosition()).magnitude();
            if (goalDist <= wormhole.getRadius()) {
                int levelBonus = 250 + (probe.getFuel() * 5);
                score += levelBonus;
                statusMessage = "WORMHOLE REACHED! +" + levelBonus + " pts!";
                TelemetryManager.saveRecord(new TelemetryManager.MissionRecord(currentLevel, score, "SUCCESS"));
                int next = (currentLevel % 3) + 1;
                loadLevel(next);
                return;
            }

            Vector2D pos = probe.getPosition();
            if (pos.x < -80 || pos.x > 1060 || pos.y < -80 || pos.y > 700) {
                statusMessage = "Probe drifted into deep space! Resetting probe...";
                TelemetryManager.saveRecord(new TelemetryManager.MissionRecord(currentLevel, score, "LOST_IN_SPACE"));
                probe.reset();
                probeTrail.clear();
            }

        } catch (BlackHoleCollisionException ex) {
            statusMessage = ex.getMessage() + " Press Retry or drag again.";
            TelemetryManager.saveRecord(new TelemetryManager.MissionRecord(currentLevel, score, "CRASHED"));
            probe.reset();
            probeTrail.clear();
        }
    }

    public synchronized void fireProbe(Vector2D launchVel) {
        if (probe.isLaunched()) return;
        try {
            probeTrail.clear();
            probe.launch(launchVel);
            aimVector = null;
            statusMessage = "Probe in flight! Gravitational slingshot active...";
        } catch (FuelDepletedException e) {
            statusMessage = e.getMessage() + " Click 'Restart Level' to refuel.";
        }
        notifyUi();
    }

    public synchronized void setAimVector(Vector2D aim) { this.aimVector = aim; }
    public synchronized Vector2D getAimVector() { return aimVector; }
    public synchronized Probe getProbe() { return probe; }
    public synchronized Wormhole getWormhole() { return wormhole; }
    public synchronized List<Planet> getPlanets() { return new ArrayList<>(planets); }
    public synchronized List<Vector2D> getProbeTrail() { return new ArrayList<>(probeTrail); }
    public synchronized List<Vector2D> getPredictedPath() { return new ArrayList<>(predictedPath); }

    public void setPaused(boolean paused) { this.paused = paused; notifyUi(); }
    public boolean isPaused() { return paused; }
    public void setShowTrajectory(boolean show) { this.showTrajectory = show; }
    public int getScore() { return score; }
    public int getCurrentLevel() { return currentLevel; }
    public String getStatusMessage() { return statusMessage; }

    private void notifyUi() {
        if (uiSyncCallback != null) {
            SwingUtilities.invokeLater(uiSyncCallback);
        }
    }
}
