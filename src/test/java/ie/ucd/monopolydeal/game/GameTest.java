package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    // Setup creates players, deals cards and starts the first turn
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

    // Setup rejects invalid player counts
    @Test
    void setupShouldRequireTwoToFivePlayers() {
        Game game = new Game();

        assertThrows(IllegalArgumentException.class, () -> game.setup(List.of("Alice")));
        assertThrows(IllegalArgumentException.class,
                () -> game.setup(List.of("A", "B", "C", "D", "E", "F")));
    }

    // Money cards move from hand to bank when played
    @Test
    void playMoneyCardShouldMoveItToCurrentPlayerBank() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        MoneyCard money = new MoneyCard("5M", 5);
        current.addCardToHand(money);

        boolean played = game.playCard(money, new TestDecisionMaker());

        assertTrue(played);
        assertEquals(1, game.getActionsUsed());
        assertFalse(current.getCardsAtHand().contains(money));
        assertTrue(current.getCardsAtBank().contains(money));
        assertEquals(5, current.getCashValue());
    }

    // A turn cannot use more than three actions
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

        assertTrue(game.playCard(first, new TestDecisionMaker()));
        assertTrue(game.playCard(second, new TestDecisionMaker()));
        assertTrue(game.playCard(third, new TestDecisionMaker()));
        assertFalse(game.playCard(fourth, new TestDecisionMaker()));
        assertEquals(Player.MAX_ACTIONS_PER_TURN, game.getActionsUsed());
    }

    // Ending a turn advances the player and resets actions
    @Test
    void endTurnShouldMoveToNextPlayerAndResetActions() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player alice = game.getCurrPlayer();

        MoneyCard money = new MoneyCard("1M", 1);
        alice.addCardToHand(money);
        game.playCard(money, new TestDecisionMaker());

        assertEquals(1, game.getActionsUsed());

        boolean ended = game.endTurn(new TestDecisionMaker());

        assertTrue(ended);
        assertEquals("Bob", game.getCurrPlayer().getName());
        assertEquals(0, game.getActionsUsed());
        assertEquals(7, game.getCurrPlayer().getCardsAtHand().size());
    }

    // Empty hand draws five at the start of turn
    @Test
    void playerWithEmptyHandShouldDrawFiveAtStartOfTurn() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player bob = game.getPlayers().get(1);
        clearHand(bob);

        int drawPileBefore = game.getDrawPileNumber();

        assertTrue(game.endTurn(new TestDecisionMaker()));

        assertSame(bob, game.getCurrPlayer());
        assertEquals(5, bob.getCardsAtHand().size());
        assertEquals(drawPileBefore - 5, game.getDrawPileNumber());
    }

    // Player may end turn without playing cards
    @Test
    void playerMayEndTurnWithoutPlayingCards() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));

        assertTrue(game.endTurn(new TestDecisionMaker()));

        assertEquals("Bob", game.getCurrPlayer().getName());
        assertEquals(0, game.getActionsUsed());
    }

    // Pass Go draws two cards and costs one action
    @Test
    void passGoActionShouldDrawTwoCardsAndUseOneAction() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();

        ActionCard passGo = new ActionCard("Pass Go", 1, ActionType.PASS_GO);
        current.addCardToHand(passGo);

        int handSizeBefore = current.getCardsAtHand().size();

        boolean played = game.playCard(passGo, new TestDecisionMaker(UseMode.PLAY, false));

        assertTrue(played);
        assertEquals(1, game.getActionsUsed());
        assertEquals(handSizeBefore + 1, current.getCardsAtHand().size());
        assertFalse(current.getCardsAtHand().contains(passGo));
    }

    // Double Rent needs a standard rent card
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

        assertFalse(game.playCard(doubleRent, new TestDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getCardsAtHand().contains(doubleRent));
        assertTrue(current.getCardsAtHand().contains(anyRent));
    }

    // Zero-value wild cards cannot be used as payment
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

        assertFalse(game.playCard(debtCollector, new TestDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getCardsAtHand().contains(debtCollector));
        assertEquals(PropertyColor.BROWN, wild.getCurrentColor());
    }

    // Debt Collector transfers payment to the actor
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

        assertTrue(game.playCard(debtCollector, new TestDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getCardsAtBank().contains(five));
        assertFalse(target.getCardsAtBank().contains(five));
        assertFalse(current.getCardsAtHand().contains(debtCollector));
        assertEquals(1, game.getActionsUsed());
    }

    // Just Say No cancels an action against the target
    @Test
    void justSayNoShouldCancelDebtCollectorPayment() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);

        removeJustSayNoCards(current);
        removeJustSayNoCards(target);

        ActionCard debtCollector = new ActionCard("Debt Collector", 3, ActionType.DEBT_COLLECTOR);
        ActionCard justSayNo = new ActionCard("Just Say No!", 4, ActionType.JUST_SAY_NO);
        MoneyCard five = new MoneyCard("5M", 5);
        current.addCardToHand(debtCollector);
        target.addCardToHand(justSayNo);
        target.addCardToBank(five);

        assertTrue(game.playCard(debtCollector, new TestDecisionMaker(UseMode.PLAY, true)));
        assertFalse(current.getCardsAtBank().contains(five));
        assertTrue(target.getCardsAtBank().contains(five));
        assertFalse(target.getCardsAtHand().contains(justSayNo));
        assertEquals(1, game.getActionsUsed());
    }

    // Sly Deal steals one unprotected property
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

        assertTrue(game.playCard(slyDeal, new TestDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getPropertySets().get(PropertyColor.LIGHT_BLUE).getCards().contains(property));
        assertFalse(target.getPropertySets().get(PropertyColor.LIGHT_BLUE).getCards().contains(property));
        assertEquals(1, game.getActionsUsed());
    }

    // Forced Deal swaps one property from each player
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

        assertTrue(game.playCard(forcedDeal, new TestDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getPropertySets().get(PropertyColor.LIGHT_BLUE).getCards().contains(targetProperty));
        assertTrue(target.getPropertySets().get(PropertyColor.BROWN).getCards().contains(currentProperty));
        assertFalse(current.getPropertySets().get(PropertyColor.BROWN).getCards().contains(currentProperty));
        assertFalse(target.getPropertySets().get(PropertyColor.LIGHT_BLUE).getCards().contains(targetProperty));
    }

    // Deal Breaker moves a complete set
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

        assertTrue(game.playCard(dealBreaker, new TestDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getPropertySets().get(PropertyColor.BROWN).getCards().contains(first));
        assertTrue(current.getPropertySets().get(PropertyColor.BROWN).getCards().contains(second));
        assertTrue(target.getPropertySets().get(PropertyColor.BROWN).getCards().isEmpty());
    }

    // Standard rent collects from an available opponent
    @Test
    void rentCardShouldCollectRentFromAvailableOpponent() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);

        PropertyCard brown = new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN);
        ActionCard rent = new ActionCard(
                "Light Blue/Brown Rent",
                1,
                ActionType.RENT,
                List.of(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN)
        );
        MoneyCard one = new MoneyCard("1M", 1);

        current.addCardToHand(brown);
        assertTrue(current.addProperty(brown));
        current.addCardToHand(rent);
        target.addCardToBank(one);

        assertTrue(game.playCard(rent, new TestDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getCardsAtBank().contains(one));
        assertFalse(target.getCardsAtBank().contains(one));
        assertEquals(1, game.getActionsUsed());
    }

    // Any Rent collects from one chosen player
    @Test
    void anyRentShouldCollectRentFromOneChosenPlayer() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);

        PropertyCard green = new PropertyCard("Pacific Avenue", 4, PropertyColor.GREEN);
        ActionCard anyRent = new ActionCard("Any Rent", 3, ActionType.MULTI_RENT, PropertyColor.getColors());
        MoneyCard two = new MoneyCard("2M", 2);

        current.addCardToHand(green);
        assertTrue(current.addProperty(green));
        current.addCardToHand(anyRent);
        target.addCardToBank(two);

        assertTrue(game.playCard(anyRent, new TestDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getCardsAtBank().contains(two));
        assertTrue(target.getCardsAtBank().isEmpty());
        assertFalse(current.getCardsAtHand().contains(anyRent));
    }

    // House and Hotel increase rent on a complete set
    @Test
    void houseAndHotelShouldIncreaseRentOnCompletePropertySet() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();

        addPropertyToTable(current, new PropertyCard("Boardwalk", 4, PropertyColor.DARK_BLUE));
        addPropertyToTable(current, new PropertyCard("Park Place", 4, PropertyColor.DARK_BLUE));

        ActionCard house = new ActionCard("House", 3, ActionType.HOUSE);
        ActionCard hotel = new ActionCard("Hotel", 4, ActionType.HOTEL);
        current.addCardToHand(house);
        current.addCardToHand(hotel);

        assertTrue(game.playCard(house, new TestDecisionMaker(UseMode.PLAY, false)));
        assertTrue(game.playCard(hotel, new TestDecisionMaker(UseMode.PLAY, false)));

        PropertySet set = current.getPropertySets().get(PropertyColor.DARK_BLUE);
        assertEquals(1, set.getHouseCount());
        assertEquals(1, set.getHotelCount());
        assertEquals(15, set.calculateRent());
    }

    // End-turn discard must choose exact unique cards
    @Test
    void endTurnDiscardMustChooseExactUniqueCards() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        MoneyCard firstExtra = new MoneyCard("8M", 8);
        MoneyCard secondExtra = new MoneyCard("9M", 9);
        current.addCardToHand(firstExtra);
        current.addCardToHand(secondExtra);

        DecisionMaker invalidDiscard = new TestDecisionMaker() {
            @Override
            public List<Card> selectDiscards(Player player, List<Card> cards, int count) {
                return List.of(firstExtra, firstExtra);
            }
        };

        assertFalse(game.endTurn(invalidDiscard));
        assertSame(current, game.getCurrPlayer());
        assertTrue(current.getCardsAtHand().contains(firstExtra));
        assertTrue(current.getCardsAtHand().contains(secondExtra));
        assertTrue(game.getUsedCards().isEmpty());
    }

    // End turn discards down to seven cards
    @Test
    void endTurnShouldDiscardExtraCardsBeforeNextPlayerStarts() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();

        MoneyCard firstExtra = new MoneyCard("9M", 9);
        MoneyCard secondExtra = new MoneyCard("8M", 8);
        current.addCardToHand(firstExtra);
        current.addCardToHand(secondExtra);

        assertTrue(game.endTurn(new TestDecisionMaker()));
        assertEquals(Player.MAX_CARDS_AT_HAND, game.getPlayers().get(0).getCardsAtHand().size());
        assertEquals("Bob", game.getCurrPlayer().getName());
        assertEquals(CardHistory.CardAction.DISCARDED, game.getUsedCards().get(0).action());
        assertEquals(CardHistory.CardAction.DISCARDED, game.getUsedCards().get(1).action());
    }

    // End-turn discards go to draw pile bottom and history
    @Test
    void endTurnDiscardsExtrasToBottomOfDrawPileAndRecordsThem() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        MoneyCard firstExtra = new MoneyCard("8M", 8);
        MoneyCard secondExtra = new MoneyCard("9M", 9);
        current.addCardToHand(firstExtra);
        current.addCardToHand(secondExtra);

        int drawPileBefore = game.getDrawPileNumber();

        assertTrue(game.endTurn(new TestDecisionMaker()));

        assertEquals(drawPileBefore, game.getDrawPileNumber());
        assertEquals(Player.MAX_CARDS_AT_HAND, current.getCardsAtHand().size());
        assertEquals(CardHistory.CardAction.DISCARDED, game.getUsedCards().get(0).action());
        assertEquals(CardHistory.CardAction.DISCARDED, game.getUsedCards().get(1).action());
    }

    // Birthday collects from every available opponent
    @Test
    void birthdayShouldCollectFromMultipleAvailablePlayers() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob", "Cara"));
        Player current = game.getCurrPlayer();
        Player bob = game.getPlayers().get(1);
        Player cara = game.getPlayers().get(2);

        ActionCard birthday = new ActionCard("It's My Birthday!", 2, ActionType.TODAY_IS_MY_BIRTHDAY);
        MoneyCard one = new MoneyCard("1M", 1);
        MoneyCard two = new MoneyCard("2M", 2);
        current.addCardToHand(birthday);
        bob.addCardToBank(one);
        cara.addCardToBank(two);

        assertTrue(game.playCard(birthday, new TestDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getCardsAtBank().contains(one));
        assertTrue(current.getCardsAtBank().contains(two));
        assertTrue(bob.getCardsAtBank().isEmpty());
        assertTrue(cara.getCardsAtBank().isEmpty());
    }

    // Completing three sets ends the game
    @Test
    void completingThirdSetShouldEndGameWithWinner() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();

        addPropertyToTable(current, new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN));
        addPropertyToTable(current, new PropertyCard("Baltic Avenue", 1, PropertyColor.BROWN));
        addPropertyToTable(current, new PropertyCard("Boardwalk", 4, PropertyColor.DARK_BLUE));
        addPropertyToTable(current, new PropertyCard("Park Place", 4, PropertyColor.DARK_BLUE));
        addPropertyToTable(current, new PropertyCard("Electric Company", 2, PropertyColor.UTILITY));

        PropertyCard waterWorks = new PropertyCard("Water Works", 2, PropertyColor.UTILITY);
        current.addCardToHand(waterWorks);

        assertTrue(game.playCard(waterWorks, new TestDecisionMaker()));
        assertTrue(game.isOver());
        assertSame(current, game.getWinner());
    }

    // Another player can win from a transferred property
    @Test
    void otherPlayerCompletingThirdSetShouldEndGameWithWinner() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);

        addPropertyToTable(current, new PropertyCard("New York Avenue", 3, PropertyColor.ORANGE));
        addPropertyToTable(target, new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN));
        addPropertyToTable(target, new PropertyCard("Baltic Avenue", 1, PropertyColor.BROWN));
        addPropertyToTable(target, new PropertyCard("Boardwalk", 4, PropertyColor.DARK_BLUE));
        addPropertyToTable(target, new PropertyCard("Park Place", 4, PropertyColor.DARK_BLUE));
        addPropertyToTable(target, new PropertyCard("St. James Place", 2, PropertyColor.ORANGE));
        addPropertyToTable(target, new PropertyCard("Tennessee Avenue", 2, PropertyColor.ORANGE));
        addPropertyToTable(target, new PropertyCard("Connecticut Avenue", 1, PropertyColor.LIGHT_BLUE));

        ActionCard forcedDeal = new ActionCard("Forced Deal", 3, ActionType.FORCED_DEAL);
        current.addCardToHand(forcedDeal);

        assertTrue(game.playCard(forcedDeal, new TestDecisionMaker()));
        assertTrue(game.isOver());
        assertSame(target, game.getWinner());
    }

    // Moving a wild card can also complete the winning third set
    @Test
    void movingWildCardToThirdSetShouldEndGameWithWinner() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();

        addPropertyToTable(current, new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN));
        addPropertyToTable(current, new PropertyCard("Baltic Avenue", 1, PropertyColor.BROWN));
        addPropertyToTable(current, new PropertyCard("Boardwalk", 4, PropertyColor.DARK_BLUE));
        addPropertyToTable(current, new PropertyCard("Park Place", 4, PropertyColor.DARK_BLUE));
        addPropertyToTable(current, new PropertyCard("St. James Place", 2, PropertyColor.ORANGE));
        addPropertyToTable(current, new PropertyCard("Tennessee Avenue", 2, PropertyColor.ORANGE));

        WildPropertyCard wild = new WildPropertyCard(
                "Pink/Orange Wild",
                List.of(PropertyColor.PINK, PropertyColor.ORANGE),
                2
        );
        current.addCardToHand(wild);
        assertTrue(current.addWildProperty(wild, PropertyColor.PINK));

        assertFalse(game.isOver());

        assertTrue(game.moveWildCard(wild, PropertyColor.ORANGE));

        assertTrue(game.isOver());
        assertSame(current, game.getWinner());
    }

    // Cancelled payment rolls back the action
    @Test
    void cancelledPaymentShouldRollBackThePlayedAction() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);

        ActionCard debtCollector = new ActionCard("Debt Collector", 3, ActionType.DEBT_COLLECTOR);
        MoneyCard five = new MoneyCard("5M", 5);
        current.addCardToHand(debtCollector);
        target.addCardToBank(five);

        DecisionMaker cancelPayment = new TestDecisionMaker(UseMode.PLAY, false) {
            @Override
            public List<Card> selectPaymentCards(Player owner, List<Card> cards, int amount) {
                return List.of();
            }
        };

        assertFalse(game.playCard(debtCollector, cancelPayment));
        assertTrue(current.getCardsAtHand().contains(debtCollector));
        assertTrue(target.getCardsAtBank().contains(five));
        assertFalse(current.getCardsAtBank().contains(five));
        assertEquals(0, game.getActionsUsed());
    }

    // Payment gives no change for overpayment
    @Test
    void overpaymentShouldTransferFullCardValueWithoutChange() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);

        ActionCard debtCollector = new ActionCard("Debt Collector", 3, ActionType.DEBT_COLLECTOR);
        MoneyCard ten = new MoneyCard("10M", 10);
        current.addCardToHand(debtCollector);
        target.addCardToBank(ten);

        assertTrue(game.playCard(debtCollector, new TestDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getCardsAtBank().contains(ten));
        assertTrue(target.getCardsAtBank().isEmpty());
        assertEquals(10, current.getBankTotalValue());
    }

    // Failed property payment rolls back earlier transfers
    @Test
    void failedMidPaymentPropertyTransferShouldRollBackEarlierPaymentCards() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);

        ActionCard debtCollector = new ActionCard("Debt Collector", 3, ActionType.DEBT_COLLECTOR);
        MoneyCard one = new MoneyCard("1M", 1);
        WildPropertyCard wild = new WildPropertyCard(
                "Dark Blue/Green Wild",
                List.of(PropertyColor.DARK_BLUE, PropertyColor.GREEN),
                4
        );
        current.addCardToHand(debtCollector);
        target.addCardToBank(one);
        target.addCardToHand(wild);
        assertTrue(target.addWildProperty(wild, PropertyColor.DARK_BLUE));

        DecisionMaker cancelWildColor = new TestDecisionMaker(UseMode.PLAY, false) {
            @Override
            public PropertyColor selectColor(String prompt, List<PropertyColor> colors) {
                return null;
            }
        };

        assertFalse(game.playCard(debtCollector, cancelWildColor));
        assertTrue(current.getCardsAtHand().contains(debtCollector));
        assertFalse(current.getCardsAtBank().contains(one));
        assertTrue(target.getCardsAtBank().contains(one));
        assertTrue(target.getPropertySets().get(PropertyColor.DARK_BLUE).getCards().contains(wild));
        assertEquals(PropertyColor.DARK_BLUE, wild.getCurrentColor());
        assertEquals(0, game.getActionsUsed());
    }

    // Exposed collections are read-only
    @Test
    void exposedCollectionsShouldBeReadOnly() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        PropertySet brownSet = current.getPropertySets().get(PropertyColor.BROWN);

        assertThrows(UnsupportedOperationException.class, () -> game.getPlayers().clear());
        assertThrows(UnsupportedOperationException.class, () -> current.getCardsAtHand().clear());
        assertThrows(UnsupportedOperationException.class, () -> current.getCardsAtBank().add(new MoneyCard("1M", 1)));
        assertThrows(UnsupportedOperationException.class, () -> current.getPropertySets().clear());
        assertThrows(UnsupportedOperationException.class, () -> brownSet.getCards().add(new MoneyCard("1M", 1)));
    }

    // Check whether a hand card is a Just Say No card
    private static boolean isJustSayNo(Card card) {
        return card instanceof ActionCard actionCard && actionCard.getActionType() == ActionType.JUST_SAY_NO;
    }

    // Remove dealt Just Say No cards so the test controls cancellation
    private static void removeJustSayNoCards(Player player) {
        List<Card> cards = new ArrayList<>(player.getCardsAtHand());
        for (Card card : cards) {
            if (isJustSayNo(card)) {
                player.removeCardFromHand(card);
            }
        }
    }

    // Empty a dealt hand so each rule starts from a controlled state
    private static void clearHand(Player player) {
        for (Card card : new ArrayList<>(player.getCardsAtHand())) {
            player.removeCardFromHand(card);
        }
    }

    // Place a property directly onto a player's table
    private static void addPropertyToTable(Player player, PropertyCard property) {
        player.addCardToHand(property);
        assertTrue(player.addProperty(property));
    }
}
