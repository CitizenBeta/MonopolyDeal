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
    void setupShouldRequireTwoToFivePlayers() {
        Game game = new Game();

        assertThrows(IllegalArgumentException.class, () -> game.setup(List.of("Alice")));
        assertThrows(IllegalArgumentException.class,
                () -> game.setup(List.of("A", "B", "C", "D", "E", "F")));
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

    @Test
    void doubleRentShouldRequireStandardRentCard() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);

        PropertyCard property = new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN);
        current.addCardToHand(property);
        assertTrue(current.addProperty(property));
        target.addCardToBank(new MoneyCard("5M", 5));

        ActionCard doubleRent = new ActionCard("Double The Rent!", 1, ActionType.DOUBLE_RENT);
        ActionCard anyRent = new ActionCard("Any Rent", 3, ActionType.MULTI_RENT, PropertyColor.getColors());
        current.addCardToHand(doubleRent);
        current.addCardToHand(anyRent);

        assertFalse(game.playCard(doubleRent, new ScriptedDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getCardsAtHand().contains(doubleRent));
        assertTrue(current.getCardsAtHand().contains(anyRent));
    }

    @Test
    void zeroValueWildCardShouldNotBePayment() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);

        WildPropertyCard wild = new WildPropertyCard("10 Color Wild", PropertyColor.getColors(), 0);
        target.addCardToHand(wild);
        assertTrue(target.addWildProperty(wild, PropertyColor.BROWN));

        ActionCard debtCollector = new ActionCard("Debt Collector", 3, ActionType.DEBT_COLLECTOR);
        current.addCardToHand(debtCollector);

        assertFalse(game.playCard(debtCollector, new ScriptedDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getCardsAtHand().contains(debtCollector));
        assertEquals(PropertyColor.BROWN, wild.getCurrentColor());
    }

    @Test
    void debtCollectorShouldTransferPaymentToCurrentPlayer() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);

        ActionCard debtCollector = new ActionCard("Debt Collector", 3, ActionType.DEBT_COLLECTOR);
        MoneyCard five = new MoneyCard("5M", 5);
        current.addCardToHand(debtCollector);
        target.addCardToBank(five);

        assertTrue(game.playCard(debtCollector, new ScriptedDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getCardsAtBank().contains(five));
        assertFalse(target.getCardsAtBank().contains(five));
        assertFalse(current.getCardsAtHand().contains(debtCollector));
        assertEquals(1, game.getActionsUsed());
    }

    @Test
    void justSayNoShouldCancelDebtCollectorPayment() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);

        current.getCardsAtHand().removeIf(GameTest::isJustSayNo);
        target.getCardsAtHand().removeIf(GameTest::isJustSayNo);

        ActionCard debtCollector = new ActionCard("Debt Collector", 3, ActionType.DEBT_COLLECTOR);
        ActionCard justSayNo = new ActionCard("Just Say No!", 4, ActionType.JUST_SAY_NO);
        MoneyCard five = new MoneyCard("5M", 5);
        current.addCardToHand(debtCollector);
        target.addCardToHand(justSayNo);
        target.addCardToBank(five);

        assertTrue(game.playCard(debtCollector, new ScriptedDecisionMaker(UseMode.PLAY, true)));
        assertFalse(current.getCardsAtBank().contains(five));
        assertTrue(target.getCardsAtBank().contains(five));
        assertFalse(target.getCardsAtHand().contains(justSayNo));
        assertEquals(1, game.getActionsUsed());
    }

    @Test
    void slyDealShouldStealOnePropertyOutsideFullSet() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);

        ActionCard slyDeal = new ActionCard("Sly Deal", 3, ActionType.SLY_DEAL);
        PropertyCard property = new PropertyCard("Oriental Avenue", 1, PropertyColor.LIGHT_BLUE);
        current.addCardToHand(slyDeal);
        target.addCardToHand(property);
        assertTrue(target.addProperty(property));

        assertTrue(game.playCard(slyDeal, new ScriptedDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getPropertySets().get(PropertyColor.LIGHT_BLUE).getCards().contains(property));
        assertFalse(target.getPropertySets().get(PropertyColor.LIGHT_BLUE).getCards().contains(property));
        assertEquals(1, game.getActionsUsed());
    }

    @Test
    void forcedDealShouldSwapTwoProperties() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);

        ActionCard forcedDeal = new ActionCard("Forced Deal", 3, ActionType.FORCED_DEAL);
        PropertyCard currentProperty = new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN);
        PropertyCard targetProperty = new PropertyCard("Oriental Avenue", 1, PropertyColor.LIGHT_BLUE);
        current.addCardToHand(forcedDeal);
        current.addCardToHand(currentProperty);
        target.addCardToHand(targetProperty);
        assertTrue(current.addProperty(currentProperty));
        assertTrue(target.addProperty(targetProperty));

        assertTrue(game.playCard(forcedDeal, new ScriptedDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getPropertySets().get(PropertyColor.LIGHT_BLUE).getCards().contains(targetProperty));
        assertTrue(target.getPropertySets().get(PropertyColor.BROWN).getCards().contains(currentProperty));
        assertFalse(current.getPropertySets().get(PropertyColor.BROWN).getCards().contains(currentProperty));
        assertFalse(target.getPropertySets().get(PropertyColor.LIGHT_BLUE).getCards().contains(targetProperty));
    }

    @Test
    void dealBreakerShouldMoveACompleteSet() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);

        ActionCard dealBreaker = new ActionCard("Deal Breaker", 5, ActionType.DEAL_BREAKER);
        PropertyCard first = new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN);
        PropertyCard second = new PropertyCard("Baltic Avenue", 1, PropertyColor.BROWN);
        current.addCardToHand(dealBreaker);
        target.addCardToHand(first);
        target.addCardToHand(second);
        assertTrue(target.addProperty(first));
        assertTrue(target.addProperty(second));

        assertTrue(game.playCard(dealBreaker, new ScriptedDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getPropertySets().get(PropertyColor.BROWN).getCards().contains(first));
        assertTrue(current.getPropertySets().get(PropertyColor.BROWN).getCards().contains(second));
        assertTrue(target.getPropertySets().get(PropertyColor.BROWN).getCards().isEmpty());
    }

    private static boolean isJustSayNo(Card card) {
        return card instanceof ActionCard actionCard && actionCard.getActionType() == ActionType.JUST_SAY_NO;
    }
}
