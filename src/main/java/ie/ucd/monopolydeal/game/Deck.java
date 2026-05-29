package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Deck {
    private final List<Card> drawPile = new ArrayList<>();
    private final List<Card> discardPile = new ArrayList<>();
    // The random object can be injected so tests can use a predictable shuffle
    private final Random random;
    private int totalCardNumber;
    private final CardFactory cardFactory;

    public Deck() {
        this(new Random(), new StandardCardFactory());
    }

    public Deck(Random random) {
        this(random, new StandardCardFactory());
    }

    public Deck(Random random, CardFactory cardFactory) {
        this.random = random;
        this.cardFactory = cardFactory;
        reset();
    }

    public void reset() {
        // Rebuild from scratch so no cards from a previous game remain in either pile
        drawPile.clear();
        discardPile.clear();
        DeckBuilder.buildStandardDeck(drawPile, cardFactory);
        totalCardNumber = drawPile.size();
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(drawPile, random);
    }

    public Card draw() {
        if (drawPile.isEmpty()) {
            refillFromDiscard();
            // Refill lazily only when a draw is attempted and the pile is empty
        }
        if (drawPile.isEmpty()) {
            return null;
        }
        return drawPile.remove(drawPile.size() - 1);
    }

    public void discard(Card card) {
        if (card != null) {
            discardPile.add(card);
        }
    }

    public void putAtDrawPileBottom(Card card) {
        if (card != null) {
            // End-turn discards are recycled to the bottom instead of the discard pile
            drawPile.add(0, card);
        }
    }

    public int getDrawPileCount() {
        return drawPile.size();
    }

    public int getDrawPileNumber() {
        return drawPile.size();
    }

    public int getDiscardPileCount() {
        return discardPile.size();
    }

    public int getDiscardPileNumber() {
        return discardPile.size();
    }

    public int getTotalCardCount() {
        return totalCardNumber;
    }

    public int getTotalCardNumber() {
        return totalCardNumber;
    }

    private void refillFromDiscard() {
        if (discardPile.isEmpty()) {
            return;
        }
        // Keep the latest discard visible, then shuffle the older discards back into the draw pile
        Card lastDiscard = discardPile.remove(discardPile.size() - 1);
        drawPile.addAll(discardPile);
        discardPile.clear();
        discardPile.add(lastDiscard);
        shuffle();
    }

}
