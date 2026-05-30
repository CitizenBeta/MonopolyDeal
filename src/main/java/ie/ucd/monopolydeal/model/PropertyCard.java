package ie.ucd.monopolydeal.model;

import java.util.Objects;

public class PropertyCard extends Card {
    private final PropertyColor color;

    public PropertyCard(String name, int bankValue, PropertyColor color) {
        super(name, bankValue);
        this.color = Objects.requireNonNull(color, "color");
    }

    @Override
    public String getDetail() {
        return getName() + " [Property, " + color.getName() + ", " + color.getRentDescription()
                + ", bank " + getBankValue() + "M]";
    }

    public PropertyColor getColor() {
        return color;
    }
}
