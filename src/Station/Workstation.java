package src.Station;

import src.Item.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public abstract class Workstation extends Station {
    private final int capacity;             
    private final int processTime;          
    private boolean isProcessing;           
    private final List<Item> itemsOnTop;    

    public Workstation(String id, Position position, char symbol, int capacity, int processTime) {
        super(id, position, symbol);
        if(capacity <= 0){
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        this.capacity = capacity;
        this.processTime = Math.max(processTime, 0);
        this.isProcessing = false;
        this.itemsOnTop = new ArrayList<>();
    }

    public int getCapacity() {
        return capacity;
    }

    public int getProcessTime() {
        return processTime;
    }

    public List<Item> getItemsOnTop() {
        return Collections.unmodifiableList(itemsOnTop);
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public boolean isFull() {
        return itemsOnTop.size() >= capacity;
    }

    public boolean hasItems() {
        return !itemsOnTop.isEmpty();
    }

    public Item peekTopItem() {
        if (itemsOnTop.isEmpty()) {
            return null;
        }
        return itemsOnTop.get(itemsOnTop.size() - 1);
    }

    public Item removeTopItem() {
        if (itemsOnTop.isEmpty()) {
            return null;
        }
        return itemsOnTop.remove(itemsOnTop.size() - 1);
    }

    public boolean canAccept(Item item) {
        return item != null && !isFull();
    }

    public boolean addItem(Item item) {
        if (!canAccept(item)) {
            return false;
        }
        itemsOnTop.add(item);
        return true;
    }

    
    public void startProcess() {
        // Implementasi spesifik di subclass
    }

    public void finishProcess() {
        // Implementasi spesifik di subclass
    }

    @Override
    public void interact(Chef chef) {
        if(chef == null) {
            return;
        }

        Item inHand = chef.getInventory();
        Item onTop = peekTopItem();

        // --- SHARED PLATING LOGIC (MOVED FROM SUBCLASSES) ---

        // CASE 1: Chef has Clean Plate, Station has Ingredient (not inside Utensil)
        // Note: Using !isFull() or similar checks might be needed, but usually we merge logic.
        if(inHand instanceof Plate plateInHand && plateInHand.isClean() && onTop instanceof Preparable preparable && !(onTop instanceof KitchenUtensils)){
            try{
                plateInHand.addIngredient(preparable);
                removeTopItem(); // Remove the ingredient from station
                addItem(plateInHand); // Put the plate down? Or keep in hand?
                // Logic in subclasses was: removeTopItem, addItem(plateInHand), setInventory(null).
                // This implies "Scoop up ingredient with plate AND place plate on station".
                // Wait, if station is full (1 item), removeTop makes it 0. addItem makes it 1.
                // This swaps the Ingredient on station with the Plate (now containing ingredient).
                chef.setInventory(null);
            } catch (RuntimeException e){
                // If fail, do nothing
            }
            return;
        }

        // CASE 2: Chef has Clean Plate, Station has Utensil (Pot/Pan)
        if(inHand instanceof Plate plateInHand2 && plateInHand2.isClean() && onTop instanceof KitchenUtensils utensilOnTable){
            try{
                // Pour from Utensil to Plate
                // Note: We must handle ConcurrentModification if we iterate directly?
                // clone contents or iterate carefully. 
                // Using new ArrayList to avoid CME if utensil.getContents() is modified during iteration (though usually we clear after).
                List<Preparable> toTransfer = new ArrayList<>(utensilOnTable.getContents());
                boolean transferred = false;
                for(Preparable p : toTransfer){
                    try {
                        plateInHand2.addIngredient(p);
                        transferred = true;
                    } catch (Exception e) {
                        // ignore if one item fails?
                    }
                }
                if (transferred) {
                    utensilOnTable.getContents().clear(); // Assume all transferred or bulk clear?
                    // The original code did: for(...) add; then clear().
                    // This assumes all items fit.
                }
            } catch (RuntimeException e){}
            return;
        }

        // CASE 3: Chef has Utensil (Pot/Pan), Station has Clean Plate
        if(inHand instanceof KitchenUtensils utensilInHand && onTop instanceof Plate plateOnTable && plateOnTable.isClean()){
           try{
                List<Preparable> toTransfer = new ArrayList<>(utensilInHand.getContents());
                boolean transferred = false;
                for(Preparable p : toTransfer){
                    try {
                        plateOnTable.addIngredient(p);
                        transferred = true;
                    } catch (Exception e) {}
                }
                if (transferred) {
                     utensilInHand.getContents().clear();
                }
            } catch (RuntimeException e){}
            return;
        }

        // CASE 4: Chef has Ingredient (Preparable), Station has Clean Plate
        if(inHand instanceof Preparable preparable && onTop instanceof Plate plateOnTable && plateOnTable.isClean()){
            // REMOVED TRY-CATCH TO LET EXCEPTIONS PROPAGATE TO GAMECONTROLLER
            plateOnTable.addIngredient(preparable);
            chef.setInventory(null);
            return;
        }

        // --- DEFAULT PICK / PLACE LOGIC ---

        // Jika tangan kosong, ambil item dari workstation
        if(inHand == null && hasItems()) {
            Item taken = removeTopItem();
            chef.setInventory(taken);
            return;
        }

        // Jika tangan ada isinya, letakkan ke workstation jika meja tidak penuh
        if(inHand != null && !isFull()) {
            if(addItem(inHand)){
                chef.setInventory(null);
            }
        }
    }
}    