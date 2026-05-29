package ie.ucd.monopolydeal.model;

import java.util.List;

public interface CardFactory {

    MoneyCard createMoneyCard(String name, int amount);

    PropertyCard createPropertyCard(String name, int bankValue, PropertyColor color);

    ActionCard createActionCard(String name, int bankValue, ActionType actionType, List<PropertyColor> colors);

    WildPropertyCard createWildPropertyCard(String name, List<PropertyColor> colors, int bankValue);
}
