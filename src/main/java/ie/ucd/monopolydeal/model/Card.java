package ie.ucd.monopolydeal.model;

public abstract class Card {
    private final String name;
    private final int bankValue;

    protected Card(String name, int bankValue) {
        this.name = name;
        this.bankValue = bankValue;
    }

    public String getName() {
        return name;
    }

    public int getBankValue() {
        return bankValue;
    }

    public abstract String getDetail();

    @Override
    public String toString() {
        return getDetail();
    }
}
