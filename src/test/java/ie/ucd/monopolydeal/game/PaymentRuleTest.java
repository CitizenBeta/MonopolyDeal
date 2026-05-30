package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Tests payment rules that protect card ownership and legal payment sources
class PaymentRuleTest {

    // Payment cannot use cards still in a player's hand
    @Test
    void paymentCannotUseCardsFromHand() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);
        clearHand(current);
        clearHand(target);

        ActionCard debtCollector = new ActionCard("Debt Collector", 3, ActionType.DEBT_COLLECTOR);
        MoneyCard handMoney = new MoneyCard("5M", 5);
        current.addCardToHand(debtCollector);
        target.addCardToHand(handMoney);

        assertFalse(game.canPlayActionCard(debtCollector));
        assertFalse(game.playCard(debtCollector, new ScriptedDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getCardsAtHand().contains(debtCollector));
        assertTrue(target.getCardsAtHand().contains(handMoney));
        assertTrue(current.getCardsAtBank().isEmpty());
    }

    // Empty a dealt hand so hand cards cannot hide payment behavior
    private static void clearHand(Player player) {
        for (Card card : new ArrayList<>(player.getCardsAtHand())) {
            player.removeCardFromHand(card);
        }
    }
}
