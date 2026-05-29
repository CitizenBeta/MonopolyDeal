package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Builds the standard Monopoly Deal card list
final class DeckBuilder {
    private DeckBuilder() {
    }

    static void buildStandardDeck(List<Card> cards, CardFactory cardFactory) {
        // The card counts below define the standard Monopoly Deal deck used by the game
        addMoney(cards, cardFactory, "1M", 1, 6);
        addMoney(cards, cardFactory, "2M", 2, 5);
        addMoney(cards, cardFactory, "3M", 3, 3);
        addMoney(cards, cardFactory, "4M", 4, 3);
        addMoney(cards, cardFactory, "5M", 5, 2);
        addMoney(cards, cardFactory, "10M", 10, 1);

        addProperty(cards, cardFactory, "Mediterranean Avenue", PropertyColor.BROWN, 1, 1);
        addProperty(cards, cardFactory, "Baltic Avenue", PropertyColor.BROWN, 1, 1);
        addProperty(cards, cardFactory, "Oriental Avenue", PropertyColor.LIGHT_BLUE, 1, 1);
        addProperty(cards, cardFactory, "Vermont Avenue", PropertyColor.LIGHT_BLUE, 1, 1);
        addProperty(cards, cardFactory, "Connecticut Avenue", PropertyColor.LIGHT_BLUE, 1, 1);
        addProperty(cards, cardFactory, "St. Charles Place", PropertyColor.PINK, 2, 1);
        addProperty(cards, cardFactory, "States Avenue", PropertyColor.PINK, 2, 1);
        addProperty(cards, cardFactory, "Virginia Avenue", PropertyColor.PINK, 2, 1);
        addProperty(cards, cardFactory, "St. James Place", PropertyColor.ORANGE, 2, 1);
        addProperty(cards, cardFactory, "Tennessee Avenue", PropertyColor.ORANGE, 2, 1);
        addProperty(cards, cardFactory, "New York Avenue", PropertyColor.ORANGE, 2, 1);
        addProperty(cards, cardFactory, "Kentucky Avenue", PropertyColor.RED, 3, 1);
        addProperty(cards, cardFactory, "Indiana Avenue", PropertyColor.RED, 3, 1);
        addProperty(cards, cardFactory, "Illinois Avenue", PropertyColor.RED, 3, 1);
        addProperty(cards, cardFactory, "Atlantic Avenue", PropertyColor.YELLOW, 3, 1);
        addProperty(cards, cardFactory, "Ventnor Avenue", PropertyColor.YELLOW, 3, 1);
        addProperty(cards, cardFactory, "Marvin Gardens", PropertyColor.YELLOW, 3, 1);
        addProperty(cards, cardFactory, "Pacific Avenue", PropertyColor.GREEN, 4, 1);
        addProperty(cards, cardFactory, "North Carolina Avenue", PropertyColor.GREEN, 4, 1);
        addProperty(cards, cardFactory, "Pennsylvania Avenue", PropertyColor.GREEN, 4, 1);
        addProperty(cards, cardFactory, "Park Place", PropertyColor.DARK_BLUE, 4, 1);
        addProperty(cards, cardFactory, "Boardwalk", PropertyColor.DARK_BLUE, 4, 1);
        addProperty(cards, cardFactory, "Reading Railroad", PropertyColor.RAILROAD, 2, 1);
        addProperty(cards, cardFactory, "Pennsylvania Railroad", PropertyColor.RAILROAD, 2, 1);
        addProperty(cards, cardFactory, "B. & O. Railroad", PropertyColor.RAILROAD, 2, 1);
        addProperty(cards, cardFactory, "Short Line", PropertyColor.RAILROAD, 2, 1);
        addProperty(cards, cardFactory, "Electric Company", PropertyColor.UTILITY, 2, 1);
        addProperty(cards, cardFactory, "Water Works", PropertyColor.UTILITY, 2, 1);

        // Wild property cards list all colors they are allowed to represent
        addWild(cards, cardFactory, "Light Blue/Brown Wild", 1, Arrays.asList(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN), 1);
        addWild(cards, cardFactory, "Light Blue/Railroad Wild", 4, Arrays.asList(PropertyColor.LIGHT_BLUE, PropertyColor.RAILROAD), 1);
        addWild(cards, cardFactory, "Pink/Orange Wild", 2, Arrays.asList(PropertyColor.PINK, PropertyColor.ORANGE), 2);
        addWild(cards, cardFactory, "Red/Yellow Wild", 3, Arrays.asList(PropertyColor.RED, PropertyColor.YELLOW), 2);
        addWild(cards, cardFactory, "Dark Blue/Green Wild", 4, Arrays.asList(PropertyColor.DARK_BLUE, PropertyColor.GREEN), 1);
        addWild(cards, cardFactory, "Green/Railroad Wild", 4, Arrays.asList(PropertyColor.GREEN, PropertyColor.RAILROAD), 1);
        addWild(cards, cardFactory, "Railroad/Utility Wild", 2, Arrays.asList(PropertyColor.RAILROAD, PropertyColor.UTILITY), 1);
        addWild(cards, cardFactory, "10 Color Wild", 0, PropertyColor.getColors(), 2);

        // Action cards still have a bank value, because players may use them as money
        addAction(cards, cardFactory, "Pass Go", ActionType.PASS_GO, 1, 10, Collections.emptyList());
        addAction(cards, cardFactory, "Debt Collector", ActionType.DEBT_COLLECTOR, 3, 3, Collections.emptyList());
        addAction(cards, cardFactory, "It's My Birthday!", ActionType.TODAY_IS_MY_BIRTHDAY, 2, 3, Collections.emptyList());
        addAction(cards, cardFactory, "Sly Deal", ActionType.SLY_DEAL, 3, 3, Collections.emptyList());
        addAction(cards, cardFactory, "Forced Deal", ActionType.FORCED_DEAL, 3, 3, Collections.emptyList());
        addAction(cards, cardFactory, "Deal Breaker", ActionType.DEAL_BREAKER, 5, 2, Collections.emptyList());
        addAction(cards, cardFactory, "House", ActionType.HOUSE, 3, 3, Collections.emptyList());
        addAction(cards, cardFactory, "Hotel", ActionType.HOTEL, 4, 2, Collections.emptyList());
        addAction(cards, cardFactory, "Just Say No!", ActionType.JUST_SAY_NO, 4, 3, Collections.emptyList());
        addAction(cards, cardFactory, "Double The Rent!", ActionType.DOUBLE_RENT, 1, 2, Collections.emptyList());

        // Rent cards store their valid colors; multi-rent uses all property colors
        addAction(cards, cardFactory, "Light Blue/Brown Rent", ActionType.RENT, 1, 2, Arrays.asList(PropertyColor.LIGHT_BLUE, PropertyColor.BROWN));
        addAction(cards, cardFactory, "Pink/Orange Rent", ActionType.RENT, 1, 2, Arrays.asList(PropertyColor.PINK, PropertyColor.ORANGE));
        addAction(cards, cardFactory, "Red/Yellow Rent", ActionType.RENT, 1, 2, Arrays.asList(PropertyColor.RED, PropertyColor.YELLOW));
        addAction(cards, cardFactory, "Dark Blue/Green Rent", ActionType.RENT, 1, 2, Arrays.asList(PropertyColor.DARK_BLUE, PropertyColor.GREEN));
        addAction(cards, cardFactory, "Railroad/Utility Rent", ActionType.RENT, 1, 2, Arrays.asList(PropertyColor.RAILROAD, PropertyColor.UTILITY));
        addAction(cards, cardFactory, "Any Rent", ActionType.MULTI_RENT, 3, 3, PropertyColor.getColors());
    }

    private static void addMoney(List<Card> cards, CardFactory cardFactory, String name, int amount, int copies) {
        for (int i = 0; i < copies; i++) {
            cards.add(cardFactory.createMoneyCard(name, amount));
        }
    }

    private static void addProperty(List<Card> cards, CardFactory cardFactory,
                                    String name, PropertyColor color, int bank, int copies) {
        for (int i = 0; i < copies; i++) {
            cards.add(cardFactory.createPropertyCard(name, bank, color));
        }
    }

    private static void addAction(List<Card> cards, CardFactory cardFactory,
                                  String name, ActionType type, int bank, int copies, List<PropertyColor> colors) {
        for (int i = 0; i < copies; i++) {
            cards.add(cardFactory.createActionCard(name, bank, type, colors));
        }
    }

    private static void addWild(List<Card> cards, CardFactory cardFactory,
                                String name, int bank, List<PropertyColor> colors, int copies) {
        for (int i = 0; i < copies; i++) {
            cards.add(cardFactory.createWildPropertyCard(name, colors, bank));
        }
    }
}
