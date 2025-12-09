package src.Station;

import src.Item.*;

public class Chef {
    private String id;
    private String name;
    private Item inventory;
    private Position position;
    private src.Game.Direction direction;
    private boolean isBusy;

    public Chef(String id, String name, Position position) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.direction = src.Game.Direction.DOWN; // Default direction
        this.isBusy = false;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public src.Game.Direction getDirection() {
        return direction;
    }

    public void setDirection(src.Game.Direction direction) {
        this.direction = direction;
    }

    public boolean isBusy() {
        return isBusy;
    }

    public void setBusy(boolean busy) {
        isBusy = busy;
    }

    public Item getInventory() {
        return inventory;
    }

    public void setInventory(Item inventory) {
        this.inventory = inventory;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Chef{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
