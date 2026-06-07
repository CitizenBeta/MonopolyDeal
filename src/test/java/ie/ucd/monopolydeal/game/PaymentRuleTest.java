package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Tests payment rules that protect card ownership and legal payment sources
class PaymentRuleTest {

    // Chosen-player payment moves enough selected bank cards to the receiver
    @Test
    void collectFromChosenPlayerShouldMoveSelectedBankCards() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player receiver = game.getCurrPlayer();
        Player payer = game.getPlayers().get(1);
        Payment payment = new Payment(game);
        MoneyCard one = new MoneyCard("1M", 1);
        MoneyCard two = new MoneyCard("2M", 2);
        payer.addCardToBank(one);
        payer.addCardToBank(two);

        assertTrue(payment.collectFromChosenPlayer(receiver, 3, new TestDecisionMaker(), "Choose payer"));

        assertTrue(receiver.getCardsAtBank().contains(one));
        assertTrue(receiver.getCardsAtBank().contains(two));
        assertTrue(payer.getCardsAtBank().isEmpty());
    }

    // Payment from every player skips opponents with no valid payment cards
    @Test
    void collectFromEveryAvailablePlayerShouldSkipPlayersWithoutPaymentOptions() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob", "Cara"));
        Player receiver = game.getCurrPlayer();
        Player bob = game.getPlayers().get(1);
        Player cara = game.getPlayers().get(2);
        Payment payment = new Payment(game);
        MoneyCard paymentCard = new MoneyCard("2M", 2);
        cara.addCardToBank(paymentCard);

        assertTrue(payment.collectFromEveryAvailablePlayer(receiver, 2, new TestDecisionMaker()));

        assertTrue(bob.getCardsAtBank().isEmpty());
        assertTrue(cara.getCardsAtBank().isEmpty());
        assertTrue(receiver.getCardsAtBank().contains(paymentCard));
    }

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
        assertFalse(game.playCard(debtCollector, new TestDecisionMaker(UseMode.PLAY, false)));
        assertTrue(current.getCardsAtHand().contains(debtCollector));
        assertTrue(target.getCardsAtHand().contains(handMoney));
        assertTrue(current.getCardsAtBank().isEmpty());
    }

    // Property payment can move a wild card into a valid receiver color
    @Test
    void transferPropertyCardShouldMoveWildCardToReceiverColor() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player receiver = game.getCurrPlayer();
        Player payer = game.getPlayers().get(1);
        Payment payment = new Payment(game);
        WildPropertyCard wild = new WildPropertyCard(
                "Light Blue/Brown Wild",
                List.of(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN),
                1
        );
        payer.addCardToHand(wild);
        assertTrue(payer.addWildProperty(wild, PropertyColor.BROWN));

        assertTrue(payment.transferPropertyCard(payer, receiver, wild, new TestDecisionMaker()));

        assertEquals(PropertyColor.LIGHT_BLUE, wild.getCurrentColor());
        assertTrue(payer.getPropertySets().get(PropertyColor.BROWN).getCards().isEmpty());
        assertTrue(receiver.getPropertySets().get(PropertyColor.LIGHT_BLUE).getCards().contains(wild));
    }

    // Swapping properties moves both cards in one successful operation
    @Test
    void swapPropertiesShouldMoveBothCards() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player alice = game.getCurrPlayer();
        Player bob = game.getPlayers().get(1);
        Payment payment = new Payment(game);
        PropertyCard brown = new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN);
        PropertyCard blue = new PropertyCard("Oriental Avenue", 1, PropertyColor.LIGHT_BLUE);
        addPropertyToTable(alice, brown);
        addPropertyToTable(bob, blue);

        assertTrue(payment.swapProperties(alice, brown, bob, blue, new TestDecisionMaker()));

        assertTrue(alice.getPropertySets().get(PropertyColor.LIGHT_BLUE).getCards().contains(blue));
        assertTrue(bob.getPropertySets().get(PropertyColor.BROWN).getCards().contains(brown));
        assertTrue(alice.getPropertySets().get(PropertyColor.BROWN).getCards().isEmpty());
        assertTrue(bob.getPropertySets().get(PropertyColor.LIGHT_BLUE).getCards().isEmpty());
    }

    // A House used as payment is banked as money by the receiver
    @Test
    void paymentWithHouseShouldBankItForReceiver() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player receiver = game.getCurrPlayer();
        Player payer = game.getPlayers().get(1);
        Payment payment = new Payment(game);
        ActionCard house = buildFullSetWithHouse(payer);

        assertTrue(payment.transferPropertyCard(payer, receiver, house, new TestDecisionMaker()));

        assertTrue(receiver.getCardsAtBank().contains(house));
        assertEquals(3, receiver.getCashValue());
        assertNull(payer.getPropertySets().get(PropertyColor.DARK_BLUE).getHouseCard());
    }

    // A Hotel used as payment is banked, but a House cannot be paid while a Hotel still sits on the set
    @Test
    void paymentWithHotelShouldBankItAndProtectHouseUnderHotel() {
        Game game = new Game();
        game.setup(List.of("Alice", "Bob"));
        Player receiver = game.getCurrPlayer();
        Player payer = game.getPlayers().get(1);
        Payment payment = new Payment(game);
        ActionCard house = buildFullSetWithHouse(payer);
        ActionCard hotel = new ActionCard("Hotel", 4, ActionType.HOTEL);
        payer.addCardToHand(hotel);
        assertTrue(payer.addHotel(PropertyColor.DARK_BLUE, hotel));

        // Paying the House alone would orphan the Hotel, so it is refused.
        assertFalse(payment.transferPropertyCard(payer, receiver, house, new TestDecisionMaker()));
        assertSame(house, payer.getPropertySets().get(PropertyColor.DARK_BLUE).getHouseCard());

        // Paying the Hotel banks it for the receiver and leaves the House in place.
        assertTrue(payment.transferPropertyCard(payer, receiver, hotel, new TestDecisionMaker()));
        assertTrue(receiver.getCardsAtBank().contains(hotel));
        assertEquals(4, receiver.getCashValue());
        assertSame(house, payer.getPropertySets().get(PropertyColor.DARK_BLUE).getHouseCard());
    }

    // Give a player a full Dark Blue set topped with a House
    private static ActionCard buildFullSetWithHouse(Player player) {
        addPropertyToTable(player, new PropertyCard("Park Place", 4, PropertyColor.DARK_BLUE));
        addPropertyToTable(player, new PropertyCard("Boardwalk", 4, PropertyColor.DARK_BLUE));
        ActionCard house = new ActionCard("House", 3, ActionType.HOUSE);
        player.addCardToHand(house);
        assertTrue(player.addHouse(PropertyColor.DARK_BLUE, house));
        return house;
    }

    // Empty a dealt hand so hand cards cannot hide payment behavior
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
