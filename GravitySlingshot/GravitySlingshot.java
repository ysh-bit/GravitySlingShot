
import java.awt.*;
import java.util.List;
import javax.swing.*;

public class GravitySlingshot extends JFrame {

    private final GameEngine engine;
    private final SpaceCanvas canvas;

    private JProgressBar fuelBar;
    private JLabel scoreLabel;
    private JLabel statusLabel;
    private JComboBox<String> levelSelector;
    private JButton pauseBtn;

    public GravitySlingshot() {
        super("Gravity Slingshot — Orbital Physics Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 680);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        engine = new GameEngine();
        canvas = new SpaceCanvas(engine);

        setJMenuBar(buildMenuBar());
        add(buildTopHud(), BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(buildBottomStatus(), BorderLayout.SOUTH);

        engine.setUiSyncCallback(this::refreshHud);

        Thread gameThread = new Thread(engine, "Gravity-Physics-Thread");
        gameThread.start();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu missionMenu = new JMenu("Mission");
        JMenuItem restartItem = new JMenuItem("Restart & Refuel");
        restartItem.addActionListener(e -> {
            engine.getProbe().refillFuel();
            engine.loadLevel(engine.getCurrentLevel());
        });

        JMenuItem logsItem = new JMenuItem("View Telemetry Logs (I/O)");
        logsItem.addActionListener(e -> showTelemetryDialog());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        missionMenu.add(restartItem);
        missionMenu.add(logsItem);
        missionMenu.addSeparator();
        missionMenu.add(exitItem);

        JMenu physicsMenu = new JMenu("Physics");
        JCheckBoxMenuItem trajectoryToggle = new JCheckBoxMenuItem("Show Daemon Trajectory Preview", true);
        trajectoryToggle.addActionListener(e -> engine.setShowTrajectory(trajectoryToggle.isSelected()));
        physicsMenu.add(trajectoryToggle);

        menuBar.add(missionMenu);
        menuBar.add(physicsMenu);
        return menuBar;
    }

    private JPanel buildTopHud() {
        JPanel hud = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        hud.setBackground(new Color(22, 29, 44));

        JLabel fuelText = new JLabel("Fuel:");
        fuelText.setForeground(Color.WHITE);

        fuelBar = new JProgressBar(0, 100);
        fuelBar.setValue(100);
        fuelBar.setStringPainted(true);
        fuelBar.setPreferredSize(new Dimension(130, 20));
        fuelBar.setForeground(new Color(0, 210, 180));

        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(new Color(255, 215, 80));
        scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        String[] levels = {"Level 1: Single Orbit", "Level 2: Dual Slingshot", "Level 3: Binary Core"};
        levelSelector = new JComboBox<>(levels);
        levelSelector.addActionListener(e -> {
            int idx = levelSelector.getSelectedIndex() + 1;
            if (idx != engine.getCurrentLevel()) {
                engine.getProbe().refillFuel();
                engine.loadLevel(idx);
            }
        });

        JButton resetBtn = new JButton("Restart & Refuel");
        resetBtn.addActionListener(e -> {
            engine.getProbe().refillFuel();
            engine.loadLevel(engine.getCurrentLevel());
        });

        pauseBtn = new JButton("Pause");
        pauseBtn.addActionListener(e -> {
            boolean nextPause = !engine.isPaused();
            engine.setPaused(nextPause);
            pauseBtn.setText(nextPause ? "Resume" : "Pause");
        });

        hud.add(fuelText);
        hud.add(fuelBar);
        hud.add(scoreLabel);
        hud.add(levelSelector);
        hud.add(resetBtn);
        hud.add(pauseBtn);
        return hud;
    }

    private JPanel buildBottomStatus() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        bar.setBackground(new Color(18, 23, 36));
        statusLabel = new JLabel(engine.getStatusMessage());
        statusLabel.setForeground(new Color(180, 220, 255));
        bar.add(statusLabel);
        return bar;
    }

    private void refreshHud() {
        fuelBar.setValue(engine.getProbe().getFuel());
        scoreLabel.setText("Score: " + engine.getScore());
        statusLabel.setText(engine.getStatusMessage());
        if (levelSelector.getSelectedIndex() != engine.getCurrentLevel() - 1) {
            levelSelector.setSelectedIndex(engine.getCurrentLevel() - 1);
        }
        canvas.repaint();
    }

    private void showTelemetryDialog() {
        List<String> logs = TelemetryManager.loadRecentLogs();
        JTextArea area = new JTextArea(String.join("\n", logs), 14, 48);
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Saved Mission Telemetry (java.io)", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GravitySlingshot().setVisible(true));
    }
}
