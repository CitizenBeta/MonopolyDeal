package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    @Test
    void setupShouldCreatePlayersDealCardsAndStartFirstTurn() {
        Game game = new Game();

        game.setup(List.of("Alice", "Bob"));

        assertTrue(game.isStarted());
        assertFalse(game.isOver());
        assertEquals(2, game.getPlayers().size());
        assertEquals("Alice", game.getCurrPlayer().getName());
        assertEquals(7, game.getPlayers().get(0).getCardsAtHand().size());
        assertEquals(5, game.getPlayers().get(1).getCardsAtHand().size());
        assertEquals(1, game.getTurnCount());
    }

    @Test
    void playMoneyCardShouldMoveItToCurrentPlayerBank() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        MoneyCard money = new MoneyCard("5M", 5);
        current.addCardToHand(money);

        boolean played = game.playCard(money, new ScriptedDecisionMaker());

        assertTrue(played);
        assertEquals(1, game.getActionsUsed());
        assertFalse(current.getCardsAtHand().contains(money));
        assertTrue(current.getCardsAtBank().contains(money));
        assertEquals(5, current.getCashValue());
    }

    @Test
    void cannotPlayMoreThanThreeCardsInOneTurn() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();

        MoneyCard first = new MoneyCard("1M", 1);
        MoneyCard second = new MoneyCard("2M", 2);
        MoneyCard third = new MoneyCard("3M", 3);
        MoneyCard fourth = new MoneyCard("4M", 4);

        current.addCardToHand(first);
        current.addCardToHand(second);
        current.addCardToHand(third);
        current.addCardToHand(fourth);

        assertTrue(game.playCard(first, new ScriptedDecisionMaker()));
        assertTrue(game.playCard(second, new ScriptedDecisionMaker()));
        assertTrue(game.playCard(third, new ScriptedDecisionMaker()));
        assertFalse(game.playCard(fourth, new ScriptedDecisionMaker()));
        assertEquals(Player.MAX_ACTIONS_PER_TURN, game.getActionsUsed());
    }

    @Test
    void endTurnShouldMoveToNextPlayerAndResetActions() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player alice = game.getCurrPlayer();

        MoneyCard money = new MoneyCard("1M", 1);
        alice.addCardToHand(money);
        game.playCard(money, new ScriptedDecisionMaker());

        assertEquals(1, game.getActionsUsed());

        boolean ended = game.endTurn(new ScriptedDecisionMaker());

        assertTrue(ended);
        assertEquals("Bob", game.getCurrPlayer().getName());
        assertEquals(0, game.getActionsUsed());
        assertEquals(7, game.getCurrPlayer().getCardsAtHand().size());
    }

    @Test
    void passGoActionShouldDrawTwoCardsAndUseOneAction() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();

        ActionCard passGo = new ActionCard("Pass Go", 1, ActionType.PASS_GO);
        current.addCardToHand(passGo);

        int handSizeBefore = current.getCardsAtHand().size();

        boolean played = game.playCard(passGo, new ScriptedDecisionMaker(UseMode.PLAY, false));

        assertTrue(played);
        assertEquals(1, game.getActionsUsed());
        assertEquals(handSizeBefore + 1, current.getCardsAtHand().size());
        assertFalse(current.getCardsAtHand().contains(passGo));
    }
}
