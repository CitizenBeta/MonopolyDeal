package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;

import java.util.*;

public class Deck {
    private final List<Card> drawPile = new ArrayList<>();
    private final List<Card> discardPile = new ArrayList<>();
    // The random object can be injected so tests can use a predictable shuffle.
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


    private void addMoney(String name, int amount, int copies) {
        for (int i = 0; i < copies; i++) {
            drawPile.add(cardFactory.createMoneyCard(name, amount));
        }
    }

    private void addProperty(String name, PropertyColor color, int bank, int copies) {
        for (int i = 0; i < copies; i++) {
            drawPile.add(cardFactory.createPropertyCard(name, bank, color));
        }
    }

    private void addAction(String name, ActionType type, int bank, int amount, int copies, List<PropertyColor> colors) {
        for (int i = 0; i < copies; i++) {
            drawPile.add(cardFactory.createActionCard(name, bank, type, colors));
        }
    }

    private void addWild(String name, int bank, List<PropertyColor> colors, int copies) {
        for (int i = 0; i < copies; i++) {
            drawPile.add(cardFactory.createWildPropertyCard(name, colors, bank));
        }
    }


    private void initializeStandardDeck() {
        // The card counts below define the standard Monopoly Deal deck used by the game.
        addMoney("1M", 1, 6);
        addMoney("2M", 2, 5);
        addMoney("3M", 3, 3);
        addMoney("4M", 4, 3);
        addMoney("5M", 5, 2);
        addMoney("10M", 10, 1);

        addProperty("Mediterranean Avenue", PropertyColor.BROWN, 1, 1);
        addProperty("Baltic Avenue", PropertyColor.BROWN, 1, 1);
        addProperty("Oriental Avenue", PropertyColor.LIGHT_BLUE, 1, 1);
        addProperty("Vermont Avenue", PropertyColor.LIGHT_BLUE, 1, 1);
        addProperty("Connecticut Avenue", PropertyColor.LIGHT_BLUE, 1, 1);
        addProperty("St. Charles Place", PropertyColor.PINK, 2, 1);
        addProperty("States Avenue", PropertyColor.PINK, 2, 1);
        addProperty("Virginia Avenue", PropertyColor.PINK, 2, 1);
        addProperty("St. James Place", PropertyColor.ORANGE, 2, 1);
        addProperty("Tennessee Avenue", PropertyColor.ORANGE, 2, 1);
        addProperty("New York Avenue", PropertyColor.ORANGE, 2, 1);
        addProperty("Kentucky Avenue", PropertyColor.RED, 3, 1);
        addProperty("Indiana Avenue", PropertyColor.RED, 3, 1);
        addProperty("Illinois Avenue", PropertyColor.RED, 3, 1);
        addProperty("Atlantic Avenue", PropertyColor.YELLOW, 3, 1);
        addProperty("Ventnor Avenue", PropertyColor.YELLOW, 3, 1);
        addProperty("Marvin Gardens", PropertyColor.YELLOW, 3, 1);
        addProperty("Pacific Avenue", PropertyColor.GREEN, 4, 1);
        addProperty("North Carolina Avenue", PropertyColor.GREEN, 4, 1);
        addProperty("Pennsylvania Avenue", PropertyColor.GREEN, 4, 1);
        addProperty("Park Place", PropertyColor.DARK_BLUE, 4, 1);
        addProperty("Boardwalk", PropertyColor.DARK_BLUE, 4, 1);
        addProperty("Reading Railroad", PropertyColor.RAILROAD, 2, 1);
        addProperty("Pennsylvania Railroad", PropertyColor.RAILROAD, 2, 1);
        addProperty("B. & O. Railroad", PropertyColor.RAILROAD, 2, 1);
        addProperty("Short Line", PropertyColor.RAILROAD, 2, 1);
        addProperty("Electric Company", PropertyColor.UTILITY, 2, 1);
        addProperty("Water Works", PropertyColor.UTILITY, 2, 1);
        // Wild property cards list all colors they are allowed to represent.
        addWild("Light Blue/Brown Wild", 1, Arrays.asList(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN), 1);
        addWild("Light Blue/Railroad Wild", 4, Arrays.asList(PropertyColor.LIGHT_BLUE, PropertyColor.RAILROAD), 1);
        addWild("Pink/Orange Wild", 2, Arrays.asList(PropertyColor.PINK, PropertyColor.ORANGE), 2);
        addWild("Red/Yellow Wild", 3, Arrays.asList(PropertyColor.RED, PropertyColor.YELLOW), 2);
        addWild("Dark Blue/Green Wild", 4, Arrays.asList(PropertyColor.DARK_BLUE, PropertyColor.GREEN), 1);
        addWild("Railroad/Green Wild", 4, Arrays.asList(PropertyColor.RAILROAD, PropertyColor.GREEN), 1);
        addWild("Utility/Railroad Wild", 2, Arrays.asList(PropertyColor.UTILITY, PropertyColor.RAILROAD), 1);
        addWild("10 Color Wild", 0, PropertyColor.getColors(), 2);
        // Action cards still have a bank value, because players may use them as money.
        addAction("Pass Go", ActionType.PASS_GO, 1, 2, 10, Collections.emptyList());
        addAction("Debt Collector", ActionType.DEBT_COLLECTOR, 3, 5, 3, Collections.emptyList());
        addAction("It's My Birthday!", ActionType.TODAY_IS_MY_BIRTHDAY, 2, 2, 3, Collections.emptyList());
        addAction("Sly Deal", ActionType.SLY_DEAL, 3, 0, 3, Collections.emptyList());
        addAction("Forced Deal", ActionType.FORCED_DEAL, 3, 0, 3, Collections.emptyList());
        addAction("Deal Breaker", ActionType.DEAL_BREAKER, 5, 0, 2, Collections.emptyList());
        addAction("House", ActionType.HOUSE, 3, 0, 3, Collections.emptyList());
        addAction("Hotel", ActionType.HOTEL, 4, 0, 2, Collections.emptyList());
        addAction("Just Say No!", ActionType.JUST_SAY_NO, 4, 0, 3, Collections.emptyList());
        addAction("Double The Rent!", ActionType.DOUBLE_RENT, 1, 0, 2, Collections.emptyList());
        // Rent cards store their valid colors; multi-rent uses all property colors.
        addAction("Light Blue/Brown Rent", ActionType.RENT, 1, 0, 2, Arrays.asList(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN));
        addAction("Pink/Orange Rent", ActionType.RENT, 1, 0, 2, Arrays.asList(PropertyColor.PINK, PropertyColor.ORANGE));
        addAction("Red/Yellow Rent", ActionType.RENT, 1, 0, 2, Arrays.asList(PropertyColor.RED, PropertyColor.YELLOW));
        addAction("Dark Blue/Green Rent", ActionType.RENT, 1, 0, 2, Arrays.asList(PropertyColor.DARK_BLUE, PropertyColor.GREEN));
        addAction("Railroad/Utility Rent", ActionType.RENT, 1, 0, 2, Arrays.asList(PropertyColor.RAILROAD, PropertyColor.UTILITY));
        addAction("Any Rent", ActionType.MULTI_RENT, 3, 0, 3, PropertyColor.getColors());
    }

    public void reset() {
        // Rebuild from scratch so no cards from a previous game remain in either pile.
        drawPile.clear();
        discardPile.clear();
        initializeStandardDeck();
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
            // End-turn discards are recycled to the bottom instead of the discard pile.
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
        // Keep the latest discard visible, then shuffle the older discards back into the draw pile.
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
