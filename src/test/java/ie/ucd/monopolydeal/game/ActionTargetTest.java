package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Tests action-card target search helpers
class ActionTargetTest {

    // Rent colors only include colors that the player owns and can charge rent from
    @Test
    void rentColorsShouldOnlyIncludePlayableOwnedColors() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player player = game.getCurrPlayer();
        ActionTargets targets = new ActionTargets(game, new Payment(game));
        ActionCard rent = new ActionCard(
                "Light Blue/Brown Rent",
                1,
                ActionType.RENT,
                List.of(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN)
        );
        ActionCard anyRent = new ActionCard("Any Rent", 3, ActionType.MULTI_RENT, PropertyColor.getColors());
        addPropertyToTable(player, new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN));

        assertEquals(List.of(PropertyColor.BROWN), targets.getRentColors(player, rent));
        assertEquals(List.of(PropertyColor.BROWN), targets.getRentColors(player, anyRent));
    }

    // Build targets skip Railroad and Utility even when those sets are complete
    @Test
    void buildableColorsShouldSkipRailroadAndUtility() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player player = game.getCurrPlayer();
        ActionTargets targets = new ActionTargets(game, new Payment(game));
        addPropertyToTable(player, new PropertyCard("Park Place", 4, PropertyColor.DARK_BLUE));
        addPropertyToTable(player, new PropertyCard("Boardwalk", 4, PropertyColor.DARK_BLUE));
        addPropertyToTable(player, new PropertyCard("Reading Railroad", 2, PropertyColor.RAILROAD));
        addPropertyToTable(player, new PropertyCard("Pennsylvania Railroad", 2, PropertyColor.RAILROAD));
        addPropertyToTable(player, new PropertyCard("B. & O. Railroad", 2, PropertyColor.RAILROAD));
        addPropertyToTable(player, new PropertyCard("Short Line", 2, PropertyColor.RAILROAD));
        addPropertyToTable(player, new PropertyCard("Electric Company", 2, PropertyColor.UTILITY));
        addPropertyToTable(player, new PropertyCard("Water Works", 2, PropertyColor.UTILITY));

        assertEquals(List.of(PropertyColor.DARK_BLUE), targets.buildableColors(player, true));
    }

    // Forced Deal targets need both players to have a transferable property
    @Test
    void forcedDealTargetsShouldRequireBothPlayersToHaveStealableCards() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob", "Cara"));
        Player alice = game.getCurrPlayer();
        Player bob = game.getPlayers().get(1);
        Player cara = game.getPlayers().get(2);
        ActionTargets targets = new ActionTargets(game, new Payment(game));
        addPropertyToTable(alice, new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN));
        addPropertyToTable(bob, new PropertyCard("Oriental Avenue", 1, PropertyColor.LIGHT_BLUE));

        List<Player> targetsForForcedDeal = targets.playersForForcedDeal(alice);

        assertEquals(List.of(bob), targetsForForcedDeal);
        assertFalse(targetsForForcedDeal.contains(cara));
    }

    // Place a property directly onto a player's table
    private static void addPropertyToTable(Player player, PropertyCard property) {
        player.addCardToHand(property);
        assertTrue(player.addProperty(property));
    }
}
