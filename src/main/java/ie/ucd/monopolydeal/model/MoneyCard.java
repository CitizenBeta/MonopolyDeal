package ie.ucd.monopolydeal.model;

public class MoneyCard extends Card {
    public MoneyCard(String name, int bankValue) {
        super(name, bankValue);
    }

    @Override
    public String getDetail() {
        return getName() + " [Money, " + getBankValue() + "M]";
    }
}
