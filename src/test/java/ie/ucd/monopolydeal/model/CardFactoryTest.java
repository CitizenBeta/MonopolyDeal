package ie.ucd.monopolydeal.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Tests the standard card factory
class CardFactoryTest {

    // Factory creates money cards with the requested value
    @Test
    void createMoneyCardShouldPreserveNameAndValue() {
        StandardCardFactory factory = new StandardCardFactory();

        MoneyCard card = factory.createMoneyCard("5M", 5);

        assertEquals("5M", card.getName());
        assertEquals(5, card.getBankValue());
    }

    // Factory creates fixed-color property cards
    @Test
    void createPropertyCardShouldPreserveColor() {
        StandardCardFactory factory = new StandardCardFactory();

        PropertyCard card = factory.createPropertyCard("Boardwalk", 4, PropertyColor.DARK_BLUE);

        assertEquals("Boardwalk", card.getName());
        assertEquals(4, card.getBankValue());
        assertEquals(PropertyColor.DARK_BLUE, card.getColor());
    }

    // Factory creates action cards with their action type and colors
    @Test
    void createActionCardShouldPreserveTypeAndColors() {
        StandardCardFactory factory = new StandardCardFactory();
        List<PropertyColor> colors = List.of(PropertyColor.RED, PropertyColor.YELLOW);

        ActionCard card = factory.createActionCard("Red/Yellow Rent", 1, ActionType.RENT, colors);

        assertEquals(ActionType.RENT, card.getActionType());
        assertEquals(colors, card.getColors());
    }

    // Factory creates wild cards with their allowed colors
    @Test
    void createWildPropertyCardShouldPreserveAllowedColors() {
        StandardCardFactory factory = new StandardCardFactory();
        List<PropertyColor> colors = List.of(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN);

        WildPropertyCard card = factory.createWildPropertyCard("Light Blue/Brown Wild", colors, 1);

        assertEquals(colors, card.getPossibleColors());
        assertEquals(1, card.getBankValue());
        assertNull(card.getCurrentColor());
    }
}
