package ie.ucd.monopolydeal.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CardModelTest {

    @Test
    void moneyCardShouldReturnBasicInformation() {
        MoneyCard card = new MoneyCard("5M", 5);

        assertEquals("5M", card.getName());
        assertEquals(5, card.getBankValue());
        assertTrue(card.getDetail().contains("Money"));
        assertTrue(card.toString().contains("5M"));
    }

    @Test
    void actionCardShouldStoreActionTypeAndColors() {
        ActionCard card = new ActionCard(
                "Red/Yellow Rent",
                1,
                ActionType.RENT,
                List.of(PropertyColor.RED, PropertyColor.YELLOW)
        );

        assertEquals("Red/Yellow Rent", card.getName());
        assertEquals(1, card.getBankValue());
        assertEquals(ActionType.RENT, card.getActionType());
        assertEquals(List.of(PropertyColor.RED, PropertyColor.YELLOW), card.getColors());
        assertTrue(card.getDetail().contains("Action"));
    }

    @Test
    void wildPropertyCardShouldAcceptOnlyAllowedColors() {
        WildPropertyCard card = new WildPropertyCard(
                "Light Blue/Brown Wild",
                List.of(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN),
                1
        );

        assertNull(card.getCurrentColor());

        card.setCurrentColor(PropertyColor.BROWN);
        assertEquals(PropertyColor.BROWN, card.getCurrentColor());

        assertThrows(IllegalArgumentException.class,
                () -> card.setCurrentColor(PropertyColor.RED));
    }
}
