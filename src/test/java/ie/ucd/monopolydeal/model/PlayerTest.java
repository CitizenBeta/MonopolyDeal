package ie.ucd.monopolydeal.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Tests player-owned hand, bank and property behavior
class PlayerTest {

    // Property cards move from hand to the matching set
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

    // Wild cards store the color chosen by the player
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

    // A placed wild card can move between its allowed sets
    @Test
    void ownerCanMovePlacedWildCardBetweenAllowedSets() {
        Player player = new Player("Alice", 1);
        WildPropertyCard wild = new WildPropertyCard(
                "Light Blue/Brown Wild",
                List.of(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN),
                1
        );
        player.addCardToHand(wild);
        assertTrue(player.addWildProperty(wild, PropertyColor.BROWN));

        player.moveExistingWild(wild, PropertyColor.LIGHT_BLUE);

        assertEquals(PropertyColor.LIGHT_BLUE, wild.getCurrentColor());
        assertTrue(player.getPropertySets().get(PropertyColor.BROWN).getCards().isEmpty());
        assertTrue(player.getPropertySets().get(PropertyColor.LIGHT_BLUE).getCards().contains(wild));
    }

    // Receiving a property rejects the wrong fixed color
    @Test
    void receivePropertyCardShouldRejectWrongFixedColor() {
        Player player = new Player("Alice", 1);
        PropertyCard property = new PropertyCard("Boardwalk", 4, PropertyColor.DARK_BLUE);

        assertFalse(player.receivePropertyCard(property, PropertyColor.BROWN));

        assertTrue(player.getPropertySets().get(PropertyColor.BROWN).getCards().isEmpty());
        assertTrue(player.getPropertySets().get(PropertyColor.DARK_BLUE).getCards().isEmpty());
    }

    // Extra same-color properties can be stacked onto an already-full set
    @Test
    void addPropertyShouldAllowStackingBeyondFullSet() {
        Player player = new Player("Alice", 1);
        addAndPlayProperty(player, new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN));
        addAndPlayProperty(player, new PropertyCard("Baltic Avenue", 1, PropertyColor.BROWN));

        PropertyCard extra = new PropertyCard("Extra Brown", 1, PropertyColor.BROWN);
        player.addCardToHand(extra);

        assertTrue(player.addProperty(extra));
        assertEquals(3, player.getPropertySets().get(PropertyColor.BROWN).getCards().size());
        assertEquals(1, player.countCompletedSets());
    }

    // A stolen full set merges into a receiver that already owns that color (overflow allowed)
    @Test
    void transferFullSetShouldMergeIntoReceiverThatOwnsThatColor() {
        Player source = new Player("Alice", 1);
        Player receiver = new Player("Bob", 2);
        addAndPlayProperty(source, new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN));
        addAndPlayProperty(source, new PropertyCard("Baltic Avenue", 1, PropertyColor.BROWN));
        addAndPlayProperty(receiver, new PropertyCard("Extra Brown", 1, PropertyColor.BROWN));

        assertTrue(source.transferFullSetTo(receiver, PropertyColor.BROWN));

        assertTrue(source.getPropertySets().get(PropertyColor.BROWN).getCards().isEmpty());
        assertEquals(3, receiver.getPropertySets().get(PropertyColor.BROWN).getCards().size());
    }

    // Three completed sets make the player win
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

    // Place a property directly onto a player's table
    private void addAndPlayProperty(Player player, PropertyCard property) {
        player.addCardToHand(property);
        assertTrue(player.addProperty(property));
    }
}
