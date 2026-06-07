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
            // End-of-turn excess cards are recycled to the bottom of the draw pile.
            drawPile.add(0, card);
        }
    }

    public int getDrawPileNumber() {
        return drawPile.size();
    }

    public int getDiscardPileNumber() {
        return discardPile.size();
    }

    public int getTotalCardNumber() {
        return totalCardNumber;
    }

    // Snapshot support so a canceled multistep action can also roll back deck changes.
    List<Card> copyDrawPile() {
        return new ArrayList<>(drawPile);
    }

    List<Card> copyDiscardPile() {
        return new ArrayList<>(discardPile);
    }

    void restorePiles(List<Card> drawCards, List<Card> discardCards) {
        drawPile.clear();
        drawPile.addAll(drawCards);
        discardPile.clear();
        discardPile.addAll(discardCards);
    }

    private void refillFromDiscard() {
        if (discardPile.isEmpty()) {
            return;
        }
        // Out of cards: shuffle the whole discard pile to form the new draw pile.
        drawPile.addAll(discardPile);
        discardPile.clear();
        shuffle();
    }

}
