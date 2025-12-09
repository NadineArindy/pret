package src.Game;

import src.Station.*;
import src.Item.*;
import src.Order.OrderManager;
import src.Recipe.Recipe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class GameController {
    private List<Chef> chefs;
    private int activeChefIndex;
    private Map<Position, Station> stationMap;
    private OrderManager orderManager;
    private KitchenLoop kitchenLoop;
    private int width = 11;
    private int height = 8;
    private long lastTime;

    public GameController() {
        this.chefs = new ArrayList<>();
        this.stationMap = new HashMap<>();
        this.orderManager = new OrderManager();
        this.kitchenLoop = new KitchenLoop(new PlateStorage("PS-1", new Position(0,0), 'P', 10)); // Temporary init
        
        initializeGame();
    }

    private void initializeGame() {
        // Initialize Map
        // Top Row: Ingredient Storages
        addStation(new IngredientStorage("IS-Rice", new Position(1, 0), 'R', Rice.class, true));
        addStation(new IngredientStorage("IS-Fish", new Position(2, 0), 'F', Fish.class, true));
        addStation(new IngredientStorage("IS-Nori", new Position(3, 0), 'N', Nori.class, true));
        addStation(new IngredientStorage("IS-Cucumber", new Position(4, 0), 'C', Cucumber.class, true));
        addStation(new IngredientStorage("IS-Shrimp", new Position(5, 0), 'S', Shrimp.class, true));

        // Bottom Row: Stations
        PlateStorage ps = new PlateStorage("PS-1", new Position(1, 7), 'P', 20);
        // Fill plate storage with some plates
        for(int i=0; i<5; i++) ps.addPlate(new Plate("Plate"));
        addStation(ps);
        this.kitchenLoop = new KitchenLoop(ps); // Re-init with correct storage

        addStation(new ServingCounter("SC-1", new Position(3, 7), 'V', orderManager, kitchenLoop));
        addStation(new TrashStation("TS-1", new Position(5, 7), 'X'));
        
        // Left/Right: Workstations
        addStation(new CuttingStation("CS-1", new Position(0, 2), 'K', 1, 3000));
        addStation(new CuttingStation("CS-2", new Position(0, 3), 'K', 1, 3000));
        
        CookingStation ck1 = new CookingStation("CK-1", new Position(10, 2), 'O', 1, 12000);
        ck1.addItem(new BoilingPot("Pot"));
        addStation(ck1);
        
        CookingStation ck2 = new CookingStation("CK-2", new Position(10, 3), 'O', 1, 12000);
        ck2.addItem(new FryingPan("Pan"));
        addStation(ck2);

        addStation(new AssemblyStation("AS-1", new Position(5, 4), 'A', 1, 0));


        // Initialize Chefs
        chefs.add(new Chef("Chef-1", "Chef A", new Position(5, 5)));
        chefs.add(new Chef("Chef-2", "Chef B", new Position(6, 5)));
        activeChefIndex = 0;

        // Initialize Recipes (Simple setup for demo)
        setupRecipes();
        
        // Initial Order
        orderManager.spawnRandomOrder();
    }

    private void addStation(Station station) {
        stationMap.put(station.getPosition(), station);
    }

    private void setupRecipes() {
        // Define simple recipes similar to Main.java
        List<Ingredient> sushiIngredients = new ArrayList<>();
        Rice rice = new Rice("Nasi"); rice.setState(IngredientState.COOKED);
        Fish fish = new Fish("Ikan"); fish.setState(IngredientState.RAW); // Sashimi?
        Nori nori = new Nori("Nori");
        
        sushiIngredients.add(rice);
        sushiIngredients.add(fish);
        sushiIngredients.add(nori);
        
        try {
            Recipe sushi = new Recipe("Sushi", sushiIngredients);
            List<Recipe> recipes = new ArrayList<>();
            recipes.add(sushi);
            orderManager.setAvailableRecipes(recipes);
        } catch (src.Exception.InvalidDataException e) {
            e.printStackTrace();
        }
    }

    public void tick(double deltaTimeInSeconds) {
        // Update Stations
        for (Station s : stationMap.values()) {
            if (s instanceof CuttingStation) ((CuttingStation) s).update((int)(deltaTimeInSeconds * 1000));
            if (s instanceof CookingStation) ((CookingStation) s).update((int)(deltaTimeInSeconds * 1000));
        }
        
        // Update KitchenLoop
        kitchenLoop.update((int)(deltaTimeInSeconds * 1000));

        // Check Orders (simplified, usually done in OrderManager update)
        orderManager.purgeExpired();
        
        // Randomly spawn orders
        if (Math.random() < 0.005) { // Low chance per tick
            orderManager.spawnRandomOrder();
        }
    }

    public void handleInput(Direction dir) {
        Chef activeChef = getActiveChef();
        if (activeChef.isBusy()) return;

        activeChef.setDirection(dir);
        
        int dx = 0, dy = 0;
        switch (dir) {
            case UP: dy = -1; break;
            case DOWN: dy = 1; break;
            case LEFT: dx = -1; break;
            case RIGHT: dx = 1; break;
        }

        Position newPos = new Position(activeChef.getPosition().getX() + dx, activeChef.getPosition().getY() + dy);
        
        if (isValidMove(newPos)) {
            activeChef.setPosition(newPos);
        }
    }

    public void handleInteraction() {
        Chef activeChef = getActiveChef();
        if (activeChef.isBusy()) return;

        Position targetPos = getFacingPosition(activeChef);
        Station station = stationMap.get(targetPos);
        
        if (station != null) {
            station.interact(activeChef);
        }
    }

    public void switchChef() {
        activeChefIndex = (activeChefIndex + 1) % chefs.size();
    }

    private boolean isValidMove(Position pos) {
        // Check bounds
        if (pos.getX() < 0 || pos.getX() >= width || pos.getY() < 0 || pos.getY() >= height) return false;
        
        // Check collisions with stations
        if (stationMap.containsKey(pos)) return false;
        
        // Check collisions with other chefs
        for (Chef c : chefs) {
            if (c.getPosition().equals(pos)) return false;
        }
        
        return true;
    }

    private Position getFacingPosition(Chef chef) {
        int x = chef.getPosition().getX();
        int y = chef.getPosition().getY();
        switch (chef.getDirection()) {
            case UP: y--; break;
            case DOWN: y++; break;
            case LEFT: x--; break;
            case RIGHT: x++; break;
        }
        return new Position(x, y);
    }

    public Chef getActiveChef() {
        return chefs.get(activeChefIndex);
    }

    public List<Chef> getChefs() {
        return chefs;
    }

    public Map<Position, Station> getStationMap() {
        return stationMap;
    }

    public OrderManager getOrderManager() {
        return orderManager;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
