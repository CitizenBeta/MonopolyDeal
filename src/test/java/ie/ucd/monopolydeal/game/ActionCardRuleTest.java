package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Tests action-card rules that need full game flow
class ActionCardRuleTest {

    // Pass Go can be played more than once in the same turn
    @Test
    void passGoCanBePlayedMoreThanOncePerTurn() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        clearHand(current);

        ActionCard first = new ActionCard("Pass Go", 1, ActionType.PASS_GO);
        ActionCard second = new ActionCard("Pass Go", 1, ActionType.PASS_GO);
        current.addCardToHand(first);
        current.addCardToHand(second);

        assertTrue(game.playCard(first, new TestDecisionMaker(UseMode.PLAY, false)));
        assertTrue(game.playCard(second, new TestDecisionMaker(UseMode.PLAY, false)));

        assertEquals(2, game.getActionsUsed());
        assertEquals(4, current.getCardsAtHand().size());
        assertFalse(current.getCardsAtHand().contains(first));
        assertFalse(current.getCardsAtHand().contains(second));
    }

    // Action cards can be banked instead of resolving their effect
    @Test
    void actionCardCanBeBankedAsMoneyInsteadOfPlayed() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        clearHand(current);

        ActionCard passGo = new ActionCard("Pass Go", 1, ActionType.PASS_GO);
        current.addCardToHand(passGo);
        int drawPileBefore = game.getDrawPileNumber();

        assertTrue(game.playCard(passGo, new TestDecisionMaker(UseMode.BANK, false)));

        assertEquals(1, game.getActionsUsed());
        assertEquals(drawPileBefore, game.getDrawPileNumber());
        assertTrue(current.getCardsAtBank().contains(passGo));
        assertFalse(current.getCardsAtHand().contains(passGo));
    }

    // Standard rent charges every opponent who can pay
    @Test
    void standardRentShouldChargeEveryOpponentWithPaymentOptions() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob", "Cara"));
        Player current = game.getCurrPlayer();
        Player bob = game.getPlayers().get(1);
        Player cara = game.getPlayers().get(2);
        clearHand(current);

        PropertyCard brown = new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN);
        ActionCard rent = new ActionCard(
                "Light Blue/Brown Rent",
                1,
                ActionType.RENT,
                List.of(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN)
        );
        MoneyCard bobMoney = new MoneyCard("1M", 1);
        MoneyCard caraMoney = new MoneyCard("2M", 2);
        addPropertyToTable(current, brown);
        current.addCardToHand(rent);
        bob.addCardToBank(bobMoney);
        cara.addCardToBank(caraMoney);

        assertTrue(game.playCard(rent, new TestDecisionMaker(UseMode.PLAY, false)));

        assertTrue(current.getCardsAtBank().contains(bobMoney));
        assertTrue(current.getCardsAtBank().contains(caraMoney));
        assertTrue(bob.getCardsAtBank().isEmpty());
        assertTrue(cara.getCardsAtBank().isEmpty());
    }

    // Any Rent charges only the selected opponent
    @Test
    void anyRentShouldChargeOnlyOneChosenOpponent() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob", "Cara"));
        Player current = game.getCurrPlayer();
        Player bob = game.getPlayers().get(1);
        Player cara = game.getPlayers().get(2);
        clearHand(current);

        PropertyCard green = new PropertyCard("Pacific Avenue", 4, PropertyColor.GREEN);
        ActionCard anyRent = new ActionCard("Any Rent", 3, ActionType.MULTI_RENT, PropertyColor.getColors());
        MoneyCard bobMoney = new MoneyCard("2M", 2);
        MoneyCard caraMoney = new MoneyCard("3M", 3);
        addPropertyToTable(current, green);
        current.addCardToHand(anyRent);
        bob.addCardToBank(bobMoney);
        cara.addCardToBank(caraMoney);

        assertTrue(game.playCard(anyRent, new TestDecisionMaker(UseMode.PLAY, false)));

        assertTrue(current.getCardsAtBank().contains(bobMoney));
        assertFalse(current.getCardsAtBank().contains(caraMoney));
        assertTrue(bob.getCardsAtBank().isEmpty());
        assertTrue(cara.getCardsAtBank().contains(caraMoney));
    }

    // Double Rent consumes a matching rent card and uses two actions
    @Test
    void doubleRentShouldConsumeRentCardAndUseTwoActions() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);
        clearHand(current);

        PropertyCard brown = new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN);
        ActionCard doubleRent = new ActionCard("Double The Rent!", 1, ActionType.DOUBLE_RENT);
        ActionCard rent = new ActionCard(
                "Light Blue/Brown Rent",
                1,
                ActionType.RENT,
                List.of(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN)
        );
        MoneyCard payment = new MoneyCard("2M", 2);
        addPropertyToTable(current, brown);
        current.addCardToHand(doubleRent);
        current.addCardToHand(rent);
        target.addCardToBank(payment);

        assertTrue(game.playCard(doubleRent, new TestDecisionMaker(UseMode.PLAY, false)));

        assertEquals(2, game.getActionsUsed());
        assertTrue(current.getCardsAtBank().contains(payment));
        assertFalse(current.getCardsAtHand().contains(doubleRent));
        assertFalse(current.getCardsAtHand().contains(rent));
    }

    // Sly Deal cannot steal from a completed set
    @Test
    void slyDealCannotStealFromAFullSet() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);
        clearHand(current);

        ActionCard slyDeal = new ActionCard("Sly Deal", 3, ActionType.SLY_DEAL);
        addPropertyToTable(target, new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN));
        addPropertyToTable(target, new PropertyCard("Baltic Avenue", 1, PropertyColor.BROWN));
        current.addCardToHand(slyDeal);

        assertFalse(game.canPlayActionCard(slyDeal));
        assertFalse(game.playCard(slyDeal, new TestDecisionMaker(UseMode.PLAY, false)));
        assertEquals(2, target.getPropertySets().get(PropertyColor.BROWN).getCards().size());
        assertTrue(current.getCardsAtHand().contains(slyDeal));
    }

    // Forced Deal cannot swap a property out of a completed set
    @Test
    void forcedDealCannotSwapAPropertyFromAFullSet() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);
        clearHand(current);

        ActionCard forcedDeal = new ActionCard("Forced Deal", 3, ActionType.FORCED_DEAL);
        addPropertyToTable(current, new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN));
        addPropertyToTable(current, new PropertyCard("Baltic Avenue", 1, PropertyColor.BROWN));
        addPropertyToTable(target, new PropertyCard("Oriental Avenue", 1, PropertyColor.LIGHT_BLUE));
        current.addCardToHand(forcedDeal);

        assertFalse(game.canPlayActionCard(forcedDeal));
        assertFalse(game.playCard(forcedDeal, new TestDecisionMaker(UseMode.PLAY, false)));
        assertEquals(2, current.getPropertySets().get(PropertyColor.BROWN).getCards().size());
        assertEquals(1, target.getPropertySets().get(PropertyColor.LIGHT_BLUE).getCards().size());
    }

    // Deal Breaker moves buildings with the completed set
    @Test
    void dealBreakerShouldMoveBuildingsWithACompleteSet() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        Player target = game.getPlayers().get(1);
        clearHand(current);

        ActionCard dealBreaker = new ActionCard("Deal Breaker", 5, ActionType.DEAL_BREAKER);
        ActionCard house = new ActionCard("House", 3, ActionType.HOUSE);
        ActionCard hotel = new ActionCard("Hotel", 4, ActionType.HOTEL);
        addPropertyToTable(target, new PropertyCard("Park Place", 4, PropertyColor.DARK_BLUE));
        addPropertyToTable(target, new PropertyCard("Boardwalk", 4, PropertyColor.DARK_BLUE));
        target.addCardToHand(house);
        target.addCardToHand(hotel);
        assertTrue(target.addHouse(PropertyColor.DARK_BLUE, house));
        assertTrue(target.addHotel(PropertyColor.DARK_BLUE, hotel));
        current.addCardToHand(dealBreaker);

        assertTrue(game.playCard(dealBreaker, new TestDecisionMaker(UseMode.PLAY, false)));

        PropertySet currentSet = current.getPropertySets().get(PropertyColor.DARK_BLUE);
        PropertySet targetSet = target.getPropertySets().get(PropertyColor.DARK_BLUE);
        assertEquals(2, currentSet.getCards().size());
        assertEquals(1, currentSet.getHouseCount());
        assertEquals(1, currentSet.getHotelCount());
        assertTrue(targetSet.getAllCards().isEmpty());
    }

    // House and Hotel must follow full-set and upgrade-order rules
    @Test
    void houseAndHotelMustFollowBuildingRulesInGameFlow() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player current = game.getCurrPlayer();
        clearHand(current);

        ActionCard house = new ActionCard("House", 3, ActionType.HOUSE);
        ActionCard hotel = new ActionCard("Hotel", 4, ActionType.HOTEL);
        ActionCard secondHotel = new ActionCard("Hotel", 4, ActionType.HOTEL);
        addPropertyToTable(current, new PropertyCard("Park Place", 4, PropertyColor.DARK_BLUE));
        current.addCardToHand(house);

        assertFalse(game.playCard(house, new TestDecisionMaker(UseMode.PLAY, false)));

        addPropertyToTable(current, new PropertyCard("Boardwalk", 4, PropertyColor.DARK_BLUE));
        assertTrue(game.playCard(house, new TestDecisionMaker(UseMode.PLAY, false)));
        current.addCardToHand(hotel);
        assertTrue(game.playCard(hotel, new TestDecisionMaker(UseMode.PLAY, false)));
        current.addCardToHand(secondHotel);
        assertFalse(game.playCard(secondHotel, new TestDecisionMaker(UseMode.PLAY, false)));

        PropertySet set = current.getPropertySets().get(PropertyColor.DARK_BLUE);
        assertEquals(1, set.getHouseCount());
        assertEquals(1, set.getHotelCount());
        assertTrue(current.getCardsAtHand().contains(secondHotel));
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
