package src.Game;

import src.Station.*;
import src.Item.*;
import src.Order.Order;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Map;

public class GameWindow extends JFrame {

    private GameController controller;
    private GamePanel canvas;
    private static final int TILE_SIZE = 64;
    private static final int UI_HEIGHT = 100;

    public GameWindow() {
        controller = new GameController();
        
        int mapWidth = controller.getWidth() * TILE_SIZE;
        int mapHeight = controller.getHeight() * TILE_SIZE;
        
        setTitle("Sushi Overcooked GUI (Swing)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        canvas = new GamePanel();
        canvas.setPreferredSize(new Dimension(mapWidth, mapHeight + UI_HEIGHT));
        canvas.setFocusable(true);
        canvas.setFocusTraversalKeysEnabled(false);
        canvas.addKeyListener(new InputHandler());
        
        add(canvas);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        
        // Game Loop (approx 60 FPS)
        Timer timer = new Timer(16, new ActionListener() {
            private long lastTime = System.nanoTime();

            @Override
            public void actionPerformed(ActionEvent e) {
                long now = System.nanoTime();
                double deltaTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                
                controller.tick(deltaTime);
                canvas.repaint();
            }
        });
        timer.start();
    }

    private class InputHandler extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W: controller.handleInput(Direction.UP); break;
                case KeyEvent.VK_S: controller.handleInput(Direction.DOWN); break;
                case KeyEvent.VK_A: controller.handleInput(Direction.LEFT); break;
                case KeyEvent.VK_D: controller.handleInput(Direction.RIGHT); break;
                case KeyEvent.VK_E: controller.handleInteraction(); break;
                case KeyEvent.VK_TAB: controller.switchChef(); break;
            }
        }
    }

    private class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            // Render UI
            renderUI(g2d);
            
            // Map Offset
            g2d.translate(0, UI_HEIGHT);
            
            // Render Map
            renderMap(g2d);
            
            g2d.translate(0, -UI_HEIGHT); // Reset
        }

        private void renderUI(Graphics2D g2) {
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(0, 0, getWidth(), UI_HEIGHT);
            
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.drawString("Orders:", 10, 25);
            
            int x = 80;
            for (Order order : controller.getOrderManager().getActiveOrders()) {
                g2.setColor(new Color(255, 255, 224)); // Light Yellow
                g2.fillRect(x, 5, 100, 90);
                g2.setColor(Color.ORANGE);
                g2.drawRect(x, 5, 100, 90);
                
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.PLAIN, 12));
                g2.drawString(order.getRecipe().getName(), x + 5, 25);
                g2.drawString("Time: " + (int)order.getTimeLeft(), x + 5, 45);
                g2.drawString("Rew: " + order.getReward(), x + 5, 65);
                
                x += 110;
            }
        }

        private void renderMap(Graphics2D g2) {
            // Background / Grid
            for (int x = 0; x < controller.getWidth(); x++) {
                for (int y = 0; y < controller.getHeight(); y++) {
                    g2.setColor(Color.LIGHT_GRAY);
                    g2.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    g2.setColor(Color.WHITE);
                    g2.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
            
            // Stations
            for (Map.Entry<Position, Station> entry : controller.getStationMap().entrySet()) {
                Position pos = entry.getKey();
                Station station = entry.getValue();
                drawStation(g2, pos.getX(), pos.getY(), station);
            }
            
            // Chefs
            for (Chef chef : controller.getChefs()) {
                drawChef(g2, chef);
            }
        }

        private void drawStation(Graphics2D g2, int x, int y, Station station) {
            int screenX = x * TILE_SIZE;
            int screenY = y * TILE_SIZE;
            
            if (station instanceof IngredientStorage) g2.setColor(new Color(139, 69, 19)); // Brown
            else if (station instanceof CuttingStation) g2.setColor(new Color(244, 164, 96)); // SandyBrown
            else if (station instanceof CookingStation) g2.setColor(new Color(178, 34, 34)); // FireBrick
            else if (station instanceof AssemblyStation) g2.setColor(new Color(222, 184, 135)); // BurlyWood
            else if (station instanceof PlateStorage) g2.setColor(new Color(255, 255, 240)); // Ivory
            else if (station instanceof ServingCounter) g2.setColor(Color.GREEN.darker());
            else if (station instanceof TrashStation) g2.setColor(Color.BLACK);
            else g2.setColor(Color.GRAY);
            
            g2.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
            g2.setColor(Color.BLACK);
            g2.drawRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
            
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            // Centering text roughly
            g2.drawString(String.valueOf(station.getSymbol()), screenX + TILE_SIZE/2 - 5, screenY + TILE_SIZE/2 + 5);
            
            // Draw Item on Top
            Item item = null;
            if (station instanceof Workstation) {
                item = ((Workstation) station).peekTopItem();
            } else if (station instanceof IngredientStorage) {
                item = ((IngredientStorage) station).getItemOnTop();
            } else if (station instanceof PlateStorage) {
                 if (!((PlateStorage) station).isEmpty()) item = ((PlateStorage) station).peekPlate();
            }
            
            if (item != null) {
                drawItem(g2, screenX + TILE_SIZE/4, screenY + TILE_SIZE/4, item);
            }
            
            // Bars
             if (station instanceof CuttingStation) {
                CuttingStation cs = (CuttingStation) station;
                if (cs.isCutting()) {
                    double progress = 1.0 - (double)cs.getRemainingTime() / CuttingStation.CUTTING_TIME;
                    drawProgressBar(g2, screenX, screenY, progress, Color.BLUE);
                }
            } else if (station instanceof CookingStation) {
                CookingStation cs = (CookingStation) station;
                if (cs.isCooking()) {
                     double progress = (double)cs.getRemainingTime() / CookingStation.COOKING_TIME;
                     drawProgressBar(g2, screenX, screenY, Math.min(progress, 1.0), Color.RED);
                }
            }
        }
        
        private void drawProgressBar(Graphics2D g2, int x, int y, double progress, Color color) {
            g2.setColor(Color.GRAY);
            g2.fillRect(x + 5, y + 5, TILE_SIZE - 10, 5);
            g2.setColor(color);
            g2.fillRect(x + 5, y + 5, (int)((TILE_SIZE - 10) * progress), 5);
        }

        private void drawChef(Graphics2D g2, Chef chef) {
            int x = chef.getPosition().getX() * TILE_SIZE;
            int y = chef.getPosition().getY() * TILE_SIZE;
            
            if (chef == controller.getActiveChef()) {
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(3));
                g2.drawRect(x, y, TILE_SIZE, TILE_SIZE);
                g2.setStroke(new BasicStroke(1));
            }
            
            g2.setColor(new Color(100, 149, 237)); // CornflowerBlue
            g2.fillOval(x + 5, y + 5, TILE_SIZE - 10, TILE_SIZE - 10);
            
            Item inventory = chef.getInventory();
            if (inventory != null) {
                drawItem(g2, x + TILE_SIZE/3, y + TILE_SIZE/3, inventory);
            }
        }

        private void drawItem(Graphics2D g2, int x, int y, Item item) {
             if (item instanceof Plate) {
                g2.setColor(Color.WHITE);
                g2.fillOval(x, y, TILE_SIZE/2, TILE_SIZE/2);
                g2.setColor(Color.BLACK);
                g2.drawOval(x, y, TILE_SIZE/2, TILE_SIZE/2);
                
                if (!((Plate) item).getContents().isEmpty()) {
                     g2.setColor(Color.ORANGE);
                     g2.fillOval(x+5, y+5, TILE_SIZE/4, TILE_SIZE/4);
                }
                
            } else if (item instanceof Ingredient) {
                String name = ((Ingredient) item).getName();
                Color c = Color.GREEN; 
                if (name.contains("Rice")) c = new Color(245, 245, 245);
                if (name.contains("Fish")) c = new Color(250, 128, 114);
                if (name.contains("Nori")) c = Color.BLACK;
                if (name.contains("Cucumber")) c = Color.GREEN;
                if (name.contains("Shrimp")) c = Color.PINK;
                
                g2.setColor(c);
                g2.fillRect(x, y, (int)(TILE_SIZE/2.5), (int)(TILE_SIZE/2.5));
                
                IngredientState state = ((Ingredient) item).getState();
                if (state == IngredientState.COOKED) {
                     g2.setColor(Color.RED);
                     g2.drawRect(x, y, (int)(TILE_SIZE/2.5), (int)(TILE_SIZE/2.5));
                } else if (state == IngredientState.CHOPPED) {
                     g2.setColor(Color.BLUE);
                     g2.drawRect(x, y, (int)(TILE_SIZE/2.5), (int)(TILE_SIZE/2.5));
                }
            } else if (item instanceof KitchenUtensils) {
                g2.setColor(Color.GRAY);
                g2.fillRect(x, y, TILE_SIZE/2, TILE_SIZE/2);
                g2.setColor(Color.BLACK);
                g2.drawString("U", x+5, y+15);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Launching GameWindow...");
        try {
            SwingUtilities.invokeLater(() -> {
                try {
                    new GameWindow();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
