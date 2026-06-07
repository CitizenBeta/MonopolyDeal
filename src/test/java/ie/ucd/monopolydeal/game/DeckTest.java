package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.Card;
import ie.ucd.monopolydeal.model.ActionCard;
import ie.ucd.monopolydeal.model.ActionType;
import ie.ucd.monopolydeal.model.MoneyCard;
import ie.ucd.monopolydeal.model.PropertyCard;
import ie.ucd.monopolydeal.model.PropertyColor;
import ie.ucd.monopolydeal.model.WildPropertyCard;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

// Tests official deck composition and pile behavior
class DeckTest {

    // New deck starts with all playable cards in the draw pile
    @Test
    void newDeckShouldContainStandardCards() {
        Deck deck = new Deck(new Random(1));

        assertEquals(106, deck.getTotalCardNumber());
        assertEquals(106, deck.getDrawPileNumber());
        assertEquals(0, deck.getDiscardPileNumber());
    }

    // Standard deck matches the official playable card counts
    @Test
    void standardDeckShouldMatchOfficialPlayableCardCounts() {
        List<Card> cards = drawAll(new Deck(new Random(1)));

        assertEquals(106, cards.size());
        assertMoneyCounts(cards);
        assertPropertyCounts(cards);
        assertWildCounts(cards);
        assertActionCounts(cards);
    }

    // Wild cards use the official names and no old aliases
    @Test
    void deckShouldUseOfficialWildcardNames() {
        Deck deck = new Deck(new Random(1));
        Map<String, Integer> wildCounts = new HashMap<>();

        Card card = deck.draw();
        while (card != null) {
            if (card instanceof WildPropertyCard) {
                wildCounts.merge(card.getName(), 1, Integer::sum);
            }
            card = deck.draw();
        }

        assertEquals(11, wildCounts.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(1, wildCounts.get("Green/Railroad Wild"));
        assertEquals(1, wildCounts.get("Railroad/Utility Wild"));
        assertFalse(wildCounts.containsKey("Railroad/Green Wild"));
        assertFalse(wildCounts.containsKey("Utility/Railroad Wild"));
    }

    // Drawing removes one card from the draw pile
    @Test
    void drawShouldReturnCardAndReduceDrawPile() {
        Deck deck = new Deck(new Random(1));
        int before = deck.getDrawPileNumber();

        Card drawn = deck.draw();

        assertNotNull(drawn);
        assertEquals(before - 1, deck.getDrawPileNumber());
    }

    // Discarding adds one card to the discard pile
    @Test
    void discardShouldAddCardToDiscardPile() {
        Deck deck = new Deck(new Random(1));
        MoneyCard card = new MoneyCard("1M", 1);

        deck.discard(card);

        assertEquals(1, deck.getDiscardPileNumber());
    }

    // An empty draw pile is rebuilt from the entire discard pile, with nothing held back
    @Test
    void drawShouldReshuffleEntireDiscardPileWhenDrawPileEmpties() {
        Deck deck = new Deck(new Random(1));
        List<Card> all = drawAll(deck);

        assertEquals(106, all.size());
        assertEquals(0, deck.getDrawPileNumber());

        for (Card card : all) {
            deck.discard(card);
        }
        assertEquals(106, deck.getDiscardPileNumber());

        Card refilled = deck.draw();

        assertNotNull(refilled);
        assertEquals(105, deck.getDrawPileNumber());
        assertEquals(0, deck.getDiscardPileNumber());
    }

    // Reset restores a complete draw pile and clears discards
    @Test
    void resetShouldRestoreFullDeckAndClearDiscardPile() {
        Deck deck = new Deck(new Random(1));
        deck.draw();
        deck.discard(new MoneyCard("1M", 1));

        deck.reset();

        assertEquals(deck.getTotalCardNumber(), deck.getDrawPileNumber());
        assertEquals(0, deck.getDiscardPileNumber());
    }

    // Draw every card so composition checks can inspect the whole deck
    private static List<Card> drawAll(Deck deck) {
        List<Card> cards = new ArrayList<>();
        Card card = deck.draw();
        while (card != null) {
            cards.add(card);
            card = deck.draw();
        }
        return cards;
    }

    // Check official money-card counts
    private static void assertMoneyCounts(List<Card> cards) {
        Map<String, Integer> moneyCounts = new HashMap<>();
        for (Card card : cards) {
            if (card instanceof MoneyCard) {
                moneyCounts.merge(card.getName(), 1, Integer::sum);
            }
        }

        assertEquals(20, moneyCounts.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(6, moneyCounts.get("1M"));
        assertEquals(5, moneyCounts.get("2M"));
        assertEquals(3, moneyCounts.get("3M"));
        assertEquals(3, moneyCounts.get("4M"));
        assertEquals(2, moneyCounts.get("5M"));
        assertEquals(1, moneyCounts.get("10M"));
    }

    // Check official property-card counts by color
    private static void assertPropertyCounts(List<Card> cards) {
        Map<PropertyColor, Integer> propertyCounts = new EnumMap<>(PropertyColor.class);
        for (Card card : cards) {
            if (card instanceof PropertyCard propertyCard) {
                propertyCounts.merge(propertyCard.getColor(), 1, Integer::sum);
            }
        }

        assertEquals(28, propertyCounts.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(2, propertyCounts.get(PropertyColor.BROWN));
        assertEquals(3, propertyCounts.get(PropertyColor.LIGHT_BLUE));
        assertEquals(3, propertyCounts.get(PropertyColor.PINK));
        assertEquals(3, propertyCounts.get(PropertyColor.ORANGE));
        assertEquals(3, propertyCounts.get(PropertyColor.RED));
        assertEquals(3, propertyCounts.get(PropertyColor.YELLOW));
        assertEquals(3, propertyCounts.get(PropertyColor.GREEN));
        assertEquals(2, propertyCounts.get(PropertyColor.DARK_BLUE));
        assertEquals(4, propertyCounts.get(PropertyColor.RAILROAD));
        assertEquals(2, propertyCounts.get(PropertyColor.UTILITY));
    }

    // Check official wild-card counts by card name
    private static void assertWildCounts(List<Card> cards) {
        Map<String, Integer> wildCounts = new HashMap<>();
        for (Card card : cards) {
            if (card instanceof WildPropertyCard) {
                wildCounts.merge(card.getName(), 1, Integer::sum);
            }
        }

        assertEquals(11, wildCounts.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(1, wildCounts.get("Light Blue/Brown Wild"));
        assertEquals(1, wildCounts.get("Light Blue/Railroad Wild"));
        assertEquals(2, wildCounts.get("Pink/Orange Wild"));
        assertEquals(2, wildCounts.get("Red/Yellow Wild"));
        assertEquals(1, wildCounts.get("Dark Blue/Green Wild"));
        assertEquals(1, wildCounts.get("Green/Railroad Wild"));
        assertEquals(1, wildCounts.get("Railroad/Utility Wild"));
        assertEquals(2, wildCounts.get("10 Color Wild"));
    }

    // Check official action and rent-card counts by action type
    private static void assertActionCounts(List<Card> cards) {
        Map<ActionType, Integer> actionCounts = new EnumMap<>(ActionType.class);
        for (Card card : cards) {
            if (card instanceof ActionCard actionCard) {
                actionCounts.merge(actionCard.getActionType(), 1, Integer::sum);
            }
        }

        assertEquals(47, actionCounts.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(10, actionCounts.get(ActionType.PASS_GO));
        assertEquals(3, actionCounts.get(ActionType.DEBT_COLLECTOR));
        assertEquals(3, actionCounts.get(ActionType.TODAY_IS_MY_BIRTHDAY));
        assertEquals(3, actionCounts.get(ActionType.SLY_DEAL));
        assertEquals(3, actionCounts.get(ActionType.FORCED_DEAL));
        assertEquals(2, actionCounts.get(ActionType.DEAL_BREAKER));
        assertEquals(3, actionCounts.get(ActionType.HOUSE));
        assertEquals(2, actionCounts.get(ActionType.HOTEL));
        assertEquals(3, actionCounts.get(ActionType.JUST_SAY_NO));
        assertEquals(2, actionCounts.get(ActionType.DOUBLE_RENT));
        assertEquals(10, actionCounts.get(ActionType.RENT));
        assertEquals(3, actionCounts.get(ActionType.MULTI_RENT));
    }
}
