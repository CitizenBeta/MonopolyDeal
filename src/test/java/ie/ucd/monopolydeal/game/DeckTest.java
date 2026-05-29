package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.Card;
import ie.ucd.monopolydeal.model.WildPropertyCard;
import ie.ucd.monopolydeal.model.MoneyCard;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

    @Test
    void newDeckShouldContainStandardCards() {
        Deck deck = new Deck(new Random(1));

        assertEquals(106, deck.getTotalCardNumber());
        assertEquals(106, deck.getDrawPileNumber());
        assertEquals(0, deck.getDiscardPileNumber());
    }

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

    @Test
    void drawShouldReturnCardAndReduceDrawPile() {
        Deck deck = new Deck(new Random(1));
        int before = deck.getDrawPileNumber();

        Card drawn = deck.draw();

        assertNotNull(drawn);
        assertEquals(before - 1, deck.getDrawPileNumber());
    }

    @Test
    void discardShouldAddCardToDiscardPile() {
        Deck deck = new Deck(new Random(1));
        MoneyCard card = new MoneyCard("1M", 1);

        deck.discard(card);

        assertEquals(1, deck.getDiscardPileNumber());
    }

    @Test
    void resetShouldRestoreFullDeckAndClearDiscardPile() {
        Deck deck = new Deck(new Random(1));
        deck.draw();
        deck.discard(new MoneyCard("1M", 1));

        deck.reset();

        assertEquals(deck.getTotalCardNumber(), deck.getDrawPileNumber());
        assertEquals(0, deck.getDiscardPileNumber());
    }
}
