import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Рисование пером");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 700);

            DrawingPanel drawingPanel = new DrawingPanel();

            JMenuBar menuBar = new JMenuBar();

            // Меню "Цвет"
            JMenu colorMenu = new JMenu("Цвет");
            JMenuItem rgbColorPickerItem = new JMenuItem("Выбрать цвет (RGB)...");
            rgbColorPickerItem.addActionListener(e -> drawingPanel.showRGBColorPicker(frame));
            colorMenu.add(rgbColorPickerItem);

            // Меню "Толщина"
            JMenu thicknessMenu = new JMenu("Толщина");
            JSlider thicknessSlider = new JSlider(1, 100, 5);
            thicknessSlider.setPaintTicks(true);
            thicknessSlider.setPaintLabels(true);
            thicknessSlider.setMajorTickSpacing(25);
            thicknessSlider.setMinorTickSpacing(5);

            JPanel thicknessPanel = new JPanel(new BorderLayout());
            thicknessPanel.add(thicknessSlider, BorderLayout.CENTER);
            JTextField thicknessField = new JTextField("5", 3);
            thicknessPanel.add(thicknessField, BorderLayout.EAST);

            thicknessSlider.addChangeListener(e -> {
                int value = thicknessSlider.getValue();
                drawingPanel.setPenThickness(value);
                thicknessField.setText(String.valueOf(value));
            });

            thicknessField.addActionListener(e -> {
                try {
                    int value = Integer.parseInt(thicknessField.getText());
                    if (value >= 1 && value <= 100) {
                        thicknessSlider.setValue(value);
                        drawingPanel.setPenThickness(value);
                    }
                } catch (NumberFormatException ex) {
                    thicknessField.setText(String.valueOf(thicknessSlider.getValue()));
                }
            });

            thicknessMenu.add(thicknessPanel);

            // Меню "Функции"
            JMenu functionsMenu = new JMenu("Функции");
            JMenuItem clearScreenItem = new JMenuItem("Очистить экран");
            clearScreenItem.addActionListener(e -> drawingPanel.clearScreen());
            functionsMenu.add(clearScreenItem);

            JMenuItem undoItem = new JMenuItem("Удалить последнее действие");
            undoItem.addActionListener(e -> drawingPanel.undo());
            functionsMenu.add(undoItem);

            JMenuItem saveItem = new JMenuItem("Сохранить рисунок");
            saveItem.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new FileNameExtensionFilter("Изображения PNG", "png"));
                if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    try {
                        drawingPanel.saveImage(file.getAbsolutePath() + ".png");
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame, "Ошибка сохранения файла: " + ex.getMessage());
                    }
                }
            });
            functionsMenu.add(saveItem);

            JMenuItem loadItem = new JMenuItem("Загрузить рисунок");
            loadItem.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new FileNameExtensionFilter("Изображения PNG", "png"));
                if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    try {
                        drawingPanel.loadImage(file.getAbsolutePath());
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame, "Ошибка загрузки файла: " + ex.getMessage());
                    }
                }
            });
            functionsMenu.add(loadItem);

            menuBar.add(colorMenu);
            menuBar.add(thicknessMenu);
            menuBar.add(functionsMenu);

            frame.setJMenuBar(menuBar);
            frame.add(drawingPanel, BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }
}

class DrawingPanel extends JPanel {
    private Color penColor = Color.BLACK;
    private int penThickness = 5;
    private Point lastPoint = null;
    private BufferedImage canvas;
    private Graphics2D g2d;
    private ArrayList<BufferedImage> undoHistory = new ArrayList<>();

    public DrawingPanel() {
        setBackground(Color.WHITE);
        canvas = new BufferedImage(900, 700, BufferedImage.TYPE_INT_ARGB);
        g2d = canvas.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastPoint = e.getPoint();
                saveUndoState();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                g2d.setColor(penColor);
                g2d.setStroke(new BasicStroke(penThickness));
                Point currentPoint = e.getPoint();
                if (lastPoint != null) {
                    g2d.drawLine(lastPoint.x, lastPoint.y, currentPoint.x, currentPoint.y);
                }
                lastPoint = currentPoint;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(canvas, 0, 0, null);
    }

    public void setPenThickness(int thickness) {
        this.penThickness = thickness;
    }

    public void clearScreen() {
        saveUndoState();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        repaint();
    }

    public void undo() {
        if (!undoHistory.isEmpty()) {
            canvas = undoHistory.remove(undoHistory.size() - 1);
            g2d = canvas.createGraphics();
            repaint();
        }
    }

    public void saveImage(String path) throws IOException {
        ImageIO.write(canvas, "png", new File(path));
    }

    public void loadImage(String path) throws IOException {
        canvas = ImageIO.read(new File(path));
        g2d = canvas.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        repaint();
    }

    public void showRGBColorPicker(Component parent) {
        JPanel rgbPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        rgbPanel.add(new JLabel("Red:"));
        JSpinner redSpinner = new JSpinner(new SpinnerNumberModel(penColor.getRed(), 0, 255, 1));
        rgbPanel.add(redSpinner);

        rgbPanel.add(new JLabel("Green:"));
        JSpinner greenSpinner = new JSpinner(new SpinnerNumberModel(penColor.getGreen(), 0, 255, 1));
        rgbPanel.add(greenSpinner);

        rgbPanel.add(new JLabel("Blue:"));
        JSpinner blueSpinner = new JSpinner(new SpinnerNumberModel(penColor.getBlue(), 0, 255, 1));
        rgbPanel.add(blueSpinner);

        int result = JOptionPane.showConfirmDialog(parent, rgbPanel, "Выберите цвет (RGB)", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int red = (int) redSpinner.getValue();
            int green = (int) greenSpinner.getValue();
            int blue = (int) blueSpinner.getValue();
            penColor = new Color(red, green, blue);
        }
    }

    private void saveUndoState() {
        BufferedImage copy = new BufferedImage(canvas.getWidth(), canvas.getHeight(), canvas.getType());
        Graphics g = copy.getGraphics();
        g.drawImage(canvas, 0, 0, null);
        g.dispose();
        undoHistory.add(copy);
    }
}
