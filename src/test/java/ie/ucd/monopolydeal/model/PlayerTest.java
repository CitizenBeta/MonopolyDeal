package ie.ucd.monopolydeal.model;

import ie.ucd.monopolydeal.game.Deck;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void bankMoneyCardShouldMoveCardFromHandToBank() {
        Player player = new Player("Alice", 1);
        MoneyCard money = new MoneyCard("5M", 5);

        player.addCardToHand(money);

        assertTrue(player.bankMoneyCard(money));
        assertFalse(player.getCardsAtHand().contains(money));
        assertTrue(player.getCardsAtBank().contains(money));
        assertEquals(5, player.getCashValue());
    }

    @Test
    void addPropertyShouldMoveCardFromHandToCorrectPropertySet() {
        Player player = new Player("Alice", 1);
        PropertyCard property = new PropertyCard("Park Place", 4, PropertyColor.DARK_BLUE);

        player.addCardToHand(property);

        assertTrue(player.addProperty(property));
        assertFalse(player.getCardsAtHand().contains(property));
        assertTrue(player.getPropertySets().get(PropertyColor.DARK_BLUE).getCards().contains(property));
        assertEquals(PropertyColor.DARK_BLUE, player.getPropertyColor(property));
    }

    @Test
    void addWildPropertyShouldAssignChosenColor() {
        Player player = new Player("Alice", 1);
        WildPropertyCard wild = new WildPropertyCard(
                "Light Blue/Brown Wild",
                List.of(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN),
                1
        );

        player.addCardToHand(wild);

        assertTrue(player.addWildProperty(wild, PropertyColor.BROWN));
        assertEquals(PropertyColor.BROWN, wild.getCurrentColor());
        assertTrue(player.getPropertySets().get(PropertyColor.BROWN).getCards().contains(wild));
    }

    @Test
    void playerShouldWinAfterCompletingThreePropertySets() {
        Player player = new Player("Alice", 1);

        addAndPlayProperty(player, new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN));
        addAndPlayProperty(player, new PropertyCard("Baltic Avenue", 1, PropertyColor.BROWN));

        addAndPlayProperty(player, new PropertyCard("Park Place", 4, PropertyColor.DARK_BLUE));
        addAndPlayProperty(player, new PropertyCard("Boardwalk", 4, PropertyColor.DARK_BLUE));

        addAndPlayProperty(player, new PropertyCard("Electric Company", 2, PropertyColor.UTILITY));
        addAndPlayProperty(player, new PropertyCard("Water Works", 2, PropertyColor.UTILITY));

        assertEquals(3, player.countCompletedSets());
        assertTrue(player.hasWon());
    }

    @Test
    void discardExcessCardsShouldReduceHandToMaximumSize() {
        Player player = new Player("Alice", 1);
        Deck deck = new Deck();

        for (int i = 1; i <= 9; i++) {
            player.addCardToHand(new MoneyCard(i + "M", i));
        }

        List<Card> discarded = player.discardExcessCards(deck);

        assertEquals(Player.MAX_CARDS_AT_HAND, player.getCardsAtHand().size());
        assertEquals(2, discarded.size());
        assertEquals(2, deck.getDiscardPileCount());
    }

    private void addAndPlayProperty(Player player, PropertyCard property) {
        player.addCardToHand(property);
        assertTrue(player.addProperty(property));
    }
}
