package ie.ucd.monopolydeal.model;

import java.util.ArrayList;
import java.util.List;

public class PropertySet {
    private final PropertyColor color;
    private final int requiredCount;
    private final List<Card> cards;

    public PropertySet(PropertyColor color, int requiredCount) {
        this.color = color;
        this.requiredCount = requiredCount;
        this.cards = new ArrayList<>();
    }

    public boolean addCard(Card card) {
        if (card == null || cards.size() >= requiredCount) return false;
        if (card instanceof PropertyCard) {
            if (((PropertyCard) card).getColor() != color) return false;
        } else if (card instanceof WildPropertyCard) {
            WildPropertyCard wild = (WildPropertyCard) card;
            if (!wild.getPossibleColors().contains(color)) return false;
        } else {
            return false;
        }
        return cards.add(card);
    }

    public boolean removeCard(Card card) {
        return cards.remove(card);
    }

    public boolean isComplete() {
        return cards.size() == requiredCount;
    }

    public PropertyColor getColor() {
        return color;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    public int getCurrentCount() {
        return cards.size();
    }

    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    public int calculateRent() {
        if (!isComplete()) return 0;
        switch (color) {
            case BROWN: return 1;
            case LIGHT_BLUE: return 2;
            case PINK: return 4;
            case ORANGE: return 6;
            case RED: return 8;
            case YELLOW: return 10;
            case GREEN: return 12;
            case DARK_BLUE: return 16;
            case RAILROAD: return 4 * cards.size();
            case UTILITY: return cards.size() == 2 ? 10 : 4;
            default: return 0;
        }
    }
}