package ie.ucd.monopolydeal.model;

public class PropertyCard implements Card {
    private final String name;
    private final int bankValue;
    private final PropertyColor color;

    public PropertyCard(String name, int bankValue, PropertyColor color) {
        this.name = name;
        this.bankValue = bankValue;
        this.color = color;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getBankValue() {
        return bankValue;
    }

    @Override
    public String getDetail() {
        return name + " [Property, " + color.getName() + ", " + color.getRentDescription() + ", bank " + bankValue + "M]";
    }

    public PropertyColor getColor() {
        return color;
    }

    @Override
    public String toString() {
        return getDetail();
    }
}
