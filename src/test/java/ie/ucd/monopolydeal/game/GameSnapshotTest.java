package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Tests rollback snapshots for multistep actions
class GameSnapshotTest {

    // Restore resets game counters, card history, player cards and wild colors
    @Test
    void restoreShouldResetMutableGameAndPlayerState() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player player = game.getCurrPlayer();
        clearHand(player);
        MoneyCard originalBank = new MoneyCard("1M", 1);
        WildPropertyCard wild = new WildPropertyCard(
                "Light Blue/Brown Wild",
                List.of(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN),
                1
        );
        player.addCardToBank(originalBank);
        player.addCardToHand(wild);
        assertTrue(player.addWildProperty(wild, PropertyColor.BROWN));
        game.addUsedCard(player, originalBank, CardHistory.CardAction.PLAYED);

        GameSnapshot snapshot = new GameSnapshot(
                0,
                game.getActionsUsed(),
                game.getTurnCount(),
                game.isStarted(),
                game.isOver(),
                game.getUsedCards(),
                game.getPlayers(),
                game.getDeck()
        );

        MoneyCard extraBank = new MoneyCard("5M", 5);
        MoneyCard extraHistory = new MoneyCard("2M", 2);
        player.addCardToBank(extraBank);
        player.moveExistingWild(wild, PropertyColor.LIGHT_BLUE);
        game.addActionUsed();
        game.addUsedCard(player, extraHistory, CardHistory.CardAction.DISCARDED);

        snapshot.restore(game);

        assertEquals(0, game.getActionsUsed());
        assertEquals(1, game.getUsedCards().size());
        assertSame(originalBank, game.getUsedCards().get(0).card());
        assertTrue(player.getCardsAtBank().contains(originalBank));
        assertFalse(player.getCardsAtBank().contains(extraBank));
        assertEquals(PropertyColor.BROWN, wild.getCurrentColor());
        assertTrue(player.getPropertySets().get(PropertyColor.BROWN).getCards().contains(wild));
        assertTrue(player.getPropertySets().get(PropertyColor.LIGHT_BLUE).getCards().isEmpty());
    }

    // Empty a dealt hand so the snapshot starts from a predictable player state
    private static void clearHand(Player player) {
        for (Card card : new ArrayList<>(player.getCardsAtHand())) {
            player.removeCardFromHand(card);
        }
    }
}
