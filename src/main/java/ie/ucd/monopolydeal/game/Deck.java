package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Deck {
    private List<Card> drawPile = new ArrayList<>();
    private List<Card> discardPile = new ArrayList<>();
    private int totalCardNumber;

    public Deck() {
        initialize();
    }

    private void addMoneyCard(String name, int bankValue, int number) {
        for (int i = 0; i < number; i++) {
            drawPile.add(new MoneyCard(name, bankValue));
        }
    }

    private void addActionCard(String name, int bankValue, ActionType actionType, int number) {
        for (int i = 0; i < number; i++) {
            drawPile.add(new ActionCard(name, bankValue, actionType));
        }
    }

    private void addActionCard(String name, int bankValue, ActionType actionType, int number, List<PropertyColor> colors) {
        for (int i = 0; i < number; i++) {
            drawPile.add(new ActionCard(name, bankValue, actionType, colors));
        }
    }

    private void addPropertyCard(String name, int bankValue, PropertyColor color, int number) {
        for (int i = 0; i < number; i++) {
            drawPile.add(new PropertyCard(name, bankValue, color));
        }
    }

    private void addWildPropertyCard(String name, int bankValue, List<PropertyColor> possibleColors, int number) {
        for (int i = 0; i < number; i++) {
            drawPile.add(new WildPropertyCard(name, possibleColors, bankValue));
        }
    }

    private void shuffle() {
        Collections.shuffle(drawPile);
    }

    private void initialize() {
        addMoneyCard("1M", 1, 6);
        addMoneyCard("2M", 2, 5);
        addMoneyCard("3M", 3, 3);
        addMoneyCard("4M", 4, 3);
        addMoneyCard("5M", 5, 2);
        addMoneyCard("10M", 10, 1);

        addPropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN, 1);
        addPropertyCard("Baltic Avenue", 1, PropertyColor.BROWN, 1);
        addPropertyCard("Oriental Avenue", 1, PropertyColor.LIGHT_BLUE, 1);
        addPropertyCard("Vermont Avenue", 1, PropertyColor.LIGHT_BLUE, 1);
        addPropertyCard("Connecticut Avenue", 1, PropertyColor.LIGHT_BLUE, 1);
        addPropertyCard("St. Charles Place", 2, PropertyColor.PINK, 1);
        addPropertyCard("States Avenue", 2, PropertyColor.PINK, 1);
        addPropertyCard("Virginia Avenue", 2, PropertyColor.PINK, 1);
        addPropertyCard("St. James Place", 2, PropertyColor.ORANGE, 1);
        addPropertyCard("Tennessee Avenue", 2, PropertyColor.ORANGE, 1);
        addPropertyCard("New York Avenue", 2, PropertyColor.ORANGE, 1);
        addPropertyCard("Kentucky Avenue", 3, PropertyColor.RED, 1);
        addPropertyCard("Indiana Avenue", 3, PropertyColor.RED, 1);
        addPropertyCard("Illinois Avenue", 3, PropertyColor.RED, 1);
        addPropertyCard("Atlantic Avenue", 3, PropertyColor.YELLOW, 1);
        addPropertyCard("Ventnor Avenue", 3, PropertyColor.YELLOW, 1);
        addPropertyCard("Marvin Gardens", 3, PropertyColor.YELLOW, 1);
        addPropertyCard("Pacific Avenue", 4, PropertyColor.GREEN, 1);
        addPropertyCard("North Carolina Avenue", 4, PropertyColor.GREEN, 1);
        addPropertyCard("Pennsylvania Avenue", 4, PropertyColor.GREEN, 1);
        addPropertyCard("Park Place", 4, PropertyColor.DARK_BLUE, 1);
        addPropertyCard("Boardwalk", 4, PropertyColor.DARK_BLUE, 1);
        addPropertyCard("Reading Railroad", 2, PropertyColor.RAILROAD, 1);
        addPropertyCard("Pennsylvania Railroad", 2, PropertyColor.RAILROAD, 1);
        addPropertyCard("B. & O. Railroad", 2, PropertyColor.RAILROAD, 1);
        addPropertyCard("Short Line", 2, PropertyColor.RAILROAD, 1);
        addPropertyCard("Electric Company", 2, PropertyColor.UTILITY, 1);
        addPropertyCard("Water Works", 2, PropertyColor.UTILITY, 1);

        addWildPropertyCard("Light Blue/Brown Wild", 1, Arrays.asList(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN), 1);
        addWildPropertyCard("Purple/Orange Wild", 2, Arrays.asList(PropertyColor.PINK, PropertyColor.ORANGE), 2);
        addWildPropertyCard("Red/Yellow Wild", 3, Arrays.asList(PropertyColor.RED, PropertyColor.YELLOW), 2);
        addWildPropertyCard("Dark Blue/Green Wild", 4, Arrays.asList(PropertyColor.DARK_BLUE, PropertyColor.GREEN), 1);
        addWildPropertyCard("Railroad/Green Wild", 4, Arrays.asList(PropertyColor.RAILROAD, PropertyColor.GREEN), 1);
        addWildPropertyCard("Light Blue/Railroad Wild", 4, Arrays.asList(PropertyColor.LIGHT_BLUE, PropertyColor.RAILROAD), 1);
        addWildPropertyCard("Utility/Railroad Wild", 2, Arrays.asList(PropertyColor.UTILITY, PropertyColor.RAILROAD), 1);
        addWildPropertyCard("10 Color Wild", 0, PropertyColor.getColors(), 2);

        addActionCard("Pass Go", 1, ActionType.PASS_GO, 10);
        addActionCard("Debt Collector", 3, ActionType.DEBT_COLLECTOR, 3);
        addActionCard("It's My Birthday!", 2, ActionType.TODAY_IS_MY_BIRTHDAY, 3);
        addActionCard("Sly Deal", 3, ActionType.SLY_DEAL, 3);
        addActionCard("Forced Deal", 3, ActionType.FORCED_DEAL, 3);
        addActionCard("Deal Breaker", 5, ActionType.DEAL_BREAKER, 2);
        addActionCard("House", 3, ActionType.HOUSE, 3);
        addActionCard("Hotel", 4, ActionType.HOTEL, 2);
        addActionCard("Just Say No!", 4, ActionType.JUST_SAY_NO, 3);
        addActionCard("Double The Rent!", 1, ActionType.DOUBLE_RENT, 2);

        addActionCard("Light Blue/Brown Rent", 1, ActionType.RENT, 2, Arrays.asList(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN));
        addActionCard("Purple/Orange Rent", 1, ActionType.RENT, 2, Arrays.asList(PropertyColor.PINK, PropertyColor.ORANGE));
        addActionCard("Red/Yellow Rent", 1, ActionType.RENT, 2, Arrays.asList(PropertyColor.RED, PropertyColor.YELLOW));
        addActionCard("Dark Blue/Green Rent", 1, ActionType.RENT, 2, Arrays.asList(PropertyColor.DARK_BLUE, PropertyColor.GREEN));
        addActionCard("Railroad/Utility Rent", 1, ActionType.RENT, 2, Arrays.asList(PropertyColor.RAILROAD, PropertyColor.UTILITY));
        addActionCard("Any Rent", 3, ActionType.MULTI_RENT, 3, PropertyColor.getColors());

        shuffle();
        totalCardNumber = drawPile.size();
    }

    public int getDrawPileNumber() {
        return drawPile.size();
    }

    public int getTotalCardNumber() {
        return totalCardNumber;
    }

    public int getDiscardPileNumber() {
        return discardPile.size();
    }

    public Card draw() {
        if (drawPile.isEmpty()) {
            if (!discardPile.isEmpty()) {
                refill();
                return drawPile.remove(drawPile.size() - 1);
            } else {
                return null;
            }
        } else {
            return drawPile.remove(drawPile.size() - 1);
        }
    }

    public void discard(Card card) {
        if (card != null) {
            discardPile.add(card);
        }
    }

    private void refill() {
        if (!discardPile.isEmpty()) {
            drawPile.addAll(discardPile);
            shuffle();
            discardPile.clear();
        }
    }
}
