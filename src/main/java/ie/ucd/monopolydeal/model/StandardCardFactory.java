package ie.ucd.monopolydeal.model;

import java.util.List;

public class StandardCardFactory implements CardFactory {

    @Override
    public MoneyCard createMoneyCard(String name, int amount) {
        return new MoneyCard(name, amount);
    }

    @Override
    public PropertyCard createPropertyCard(String name, int bankValue, PropertyColor color) {
        return new PropertyCard(name, bankValue, color);
    }

    @Override
    public ActionCard createActionCard(String name, int bankValue, ActionType actionType, List<PropertyColor> colors) {
        return new ActionCard(name, bankValue, actionType, colors);
    }

    @Override
    public WildPropertyCard createWildPropertyCard(String name, List<PropertyColor> colors, int bankValue) {
        return new WildPropertyCard(name, colors, bankValue);
    }
}
