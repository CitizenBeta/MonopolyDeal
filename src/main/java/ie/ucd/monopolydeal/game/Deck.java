package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.ActionCard;
import ie.ucd.monopolydeal.model.ActionType;
import ie.ucd.monopolydeal.model.Card;
import ie.ucd.monopolydeal.model.Color;
import ie.ucd.monopolydeal.model.MoneyCard;
import ie.ucd.monopolydeal.model.PropertyCard;
import ie.ucd.monopolydeal.model.WildPropertyCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Deck {
    private final List<Card> drawPile = new ArrayList<>();
    private final List<Card> discardPile = new ArrayList<>();
    private final Random random;

    public Deck() {
        this(new Random());
    }

    public Deck(Random random) {
        this.random = random;
        reset();
    }

    private void addMoney(String name, int amount, int copies) {
        for (int i = 0; i < copies; i++) {
            drawPile.add(new MoneyCard(name, amount));
        }
    }

    private void addProperty(String name, Color color, int bank, int copies) {
        for (int i = 0; i < copies; i++) {
            drawPile.add(new PropertyCard(name, color, bank));
        }
    }

    private void addAction(String name, ActionType type, int bank, int amount, int copies, List<Color> colors) {
        for (int i = 0; i < copies; i++) {
            drawPile.add(new ActionCard(name, type, bank, amount, colors));
        }
    }

    private void addWild(String name, int bank, List<Color> colors, int copies) {
        for (int i = 0; i < copies; i++) {
            drawPile.add(new WildPropertyCard(name, colors, bank));
        }
    }

    private void initializeStandardDeck() {
        addMoney("1M", 1, 6);
        addMoney("2M", 2, 5);
        addMoney("3M", 3, 3);
        addMoney("4M", 4, 3);
        addMoney("5M", 5, 2);
        addMoney("10M", 10, 1);

        addProperty("Mediterranean Avenue", Color.BROWN, 1, 1);
        addProperty("Baltic Avenue", Color.BROWN, 1, 1);
        addProperty("Oriental Avenue", Color.LIGHT_BLUE, 1, 1);
        addProperty("Vermont Avenue", Color.LIGHT_BLUE, 1, 1);
        addProperty("Connecticut Avenue", Color.LIGHT_BLUE, 1, 1);
        addProperty("St. Charles Place", Color.PINK, 2, 1);
        addProperty("States Avenue", Color.PINK, 2, 1);
        addProperty("Virginia Avenue", Color.PINK, 2, 1);
        addProperty("St. James Place", Color.ORANGE, 2, 1);
        addProperty("Tennessee Avenue", Color.ORANGE, 2, 1);
        addProperty("New York Avenue", Color.ORANGE, 2, 1);
        addProperty("Kentucky Avenue", Color.RED, 3, 1);
        addProperty("Indiana Avenue", Color.RED, 3, 1);
        addProperty("Illinois Avenue", Color.RED, 3, 1);
        addProperty("Atlantic Avenue", Color.YELLOW, 3, 1);
        addProperty("Ventnor Avenue", Color.YELLOW, 3, 1);
        addProperty("Marvin Gardens", Color.YELLOW, 3, 1);
        addProperty("Pacific Avenue", Color.GREEN, 4, 1);
        addProperty("North Carolina Avenue", Color.GREEN, 4, 1);
        addProperty("Pennsylvania Avenue", Color.GREEN, 4, 1);
        addProperty("Park Place", Color.DARK_BLUE, 4, 1);
        addProperty("Boardwalk", Color.DARK_BLUE, 4, 1);
        addProperty("Reading Railroad", Color.RAILROAD, 2, 1);
        addProperty("Pennsylvania Railroad", Color.RAILROAD, 2, 1);
        addProperty("B&O Railroad", Color.RAILROAD, 2, 1);
        addProperty("Short Line", Color.RAILROAD, 2, 1);
        addProperty("Electric Company", Color.UTILITY, 2, 1);
        addProperty("Water Works", Color.UTILITY, 2, 1);

        addWild("Brown/Light Blue Wild", 1, Arrays.asList(Color.BROWN, Color.LIGHT_BLUE), 1);
        addWild("Pink/Orange Wild", 2, Arrays.asList(Color.PINK, Color.ORANGE), 2);
        addWild("Red/Yellow Wild", 3, Arrays.asList(Color.RED, Color.YELLOW), 2);
        addWild("Green/Dark Blue Wild", 4, Arrays.asList(Color.GREEN, Color.DARK_BLUE), 2);
        addWild("Railroad/Green Wild", 4, Arrays.asList(Color.RAILROAD, Color.GREEN), 1);
        addWild("Railroad/Dark Blue Wild", 4, Arrays.asList(Color.RAILROAD, Color.DARK_BLUE), 1);
        addWild("Railroad/Utility Wild", 2, Arrays.asList(Color.RAILROAD, Color.UTILITY), 1);
        addWild("Any Color Wild", 0, Color.getColors(), 2);

        addAction("Pass Go", ActionType.PASS_GO, 1, 2, 10, Collections.emptyList());
        addAction("Debt Collector", ActionType.DEBT_COLLECTOR, 3, 5, 3, Collections.emptyList());
        addAction("It's My Birthday", ActionType.BIRTHDAY, 2, 2, 3, Collections.emptyList());
        addAction("Sly Deal", ActionType.SLY_DEAL, 3, 0, 3, Collections.emptyList());
        addAction("Forced Deal", ActionType.FORCED_DEAL, 3, 0, 3, Collections.emptyList());
        addAction("Deal Breaker", ActionType.DEAL_BREAKER, 5, 0, 2, Collections.emptyList());
        addAction("House", ActionType.HOUSE, 3, 0, 3, Collections.emptyList());
        addAction("Hotel", ActionType.HOTEL, 4, 0, 2, Collections.emptyList());
        addAction("Just Say No", ActionType.JUST_SAY_NO, 4, 0, 3, Collections.emptyList());
        addAction("Double The Rent", ActionType.DOUBLE_RENT, 1, 0, 2, Collections.emptyList());

        addAction("Brown/Light Blue Rent", ActionType.RENT, 1, 0, 2, Arrays.asList(Color.BROWN, Color.LIGHT_BLUE));
        addAction("Pink/Orange Rent", ActionType.RENT, 1, 0, 2, Arrays.asList(Color.PINK, Color.ORANGE));
        addAction("Red/Yellow Rent", ActionType.RENT, 1, 0, 2, Arrays.asList(Color.RED, Color.YELLOW));
        addAction("Green/Dark Blue Rent", ActionType.RENT, 1, 0, 2, Arrays.asList(Color.GREEN, Color.DARK_BLUE));
        addAction("Railroad/Utility Rent", ActionType.RENT, 1, 0, 2, Arrays.asList(Color.RAILROAD, Color.UTILITY));
        addAction("Wild Rent", ActionType.MULTI_RENT, 3, 0, 3, Color.getColors());
    }

    public void reset() {
        drawPile.clear();
        discardPile.clear();
        initializeStandardDeck();
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(drawPile, random);
    }

    public Card draw() {
        if (drawPile.isEmpty()) {
            refillFromDiscard();
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

    public int getDrawPileCount() {
        return drawPile.size();
    }

    public int getDiscardPileCount() {
        return discardPile.size();
    }

    public int getTotalCardCount() {
        return drawPile.size() + discardPile.size();
    }

    private void refillFromDiscard() {
        if (discardPile.isEmpty()) {
            return;
        }
        Card lastDiscard = discardPile.remove(discardPile.size() - 1);
        drawPile.addAll(discardPile);
        discardPile.clear();
        discardPile.add(lastDiscard);
        shuffle();
    }

    public void printDeckSummary() {
        System.out.printf("Draw pile: %d cards, Discard pile: %d cards%n",
                drawPile.size(), discardPile.size());
    }
}