package ie.ucd.monopolydeal.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Player {
    public static final int MAX_CARDS_AT_HAND = 7;
    public static final int MAX_ACTIONS_PER_TURN = 3;

    private final String name;
    private final int number;
    private final List<Card> cardsAtHand = new ArrayList<>();
    private final List<Card> cardsAtBank = new ArrayList<>();
    private final List<PropertySet> propertySets = new ArrayList<>();

    public Player(String name, int number) {
        this.name = name;
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public int getNumber() {
        return number;
    }

    public List<Card> getCardsAtHand() {
        return cardsAtHand;
    }

    public List<Card> getCardsAtBank() {
        return cardsAtBank;
    }

    public void addCardToHand(Card card) {
        if (card == null) return;
        cardsAtHand.add(card);
    }

    public void addCardToBank(Card card) {
        if (card == null) return;
        cardsAtHand.remove(card);
        if (card instanceof PropertyCard || card instanceof WildPropertyCard) {
            PropertyColor targetColor = getTargetColorForCard(card);
            PropertySet set = findCompatibleSet(targetColor);
            if (set != null && set.addCard(card)) {
                cardsAtBank.add(card);
                return;
            }
            int required = getRequiredCountForColor(targetColor);
            PropertySet newSet = new PropertySet(targetColor, required);
            if (newSet.addCard(card)) {
                propertySets.add(newSet);
                cardsAtBank.add(card);
            }
        } else {
            cardsAtBank.add(card);
        }
    }

    public boolean removeCardFromHand(Card card) {
        if (card == null) return false;
        return cardsAtHand.remove(card);
    }

    public boolean removeCardFromBank(Card card) {
        if (card == null) return false;
        boolean removed = cardsAtBank.remove(card);
        if (removed) {
            for (PropertySet ps : propertySets) {
                if (ps.getCards().contains(card)) {
                    ps.removeCard(card);
                    if (ps.getCurrentCount() == 0) {
                        propertySets.remove(ps);
                    }
                    break;
                }
            }
        }
        return removed;
    }

    public int getBankTotalValue() {
        int total = 0;
        for (Card card : cardsAtBank) {
            total += card.getBankValue();
        }
        return total;
    }

    public boolean isHandFull() {
        return cardsAtHand.size() > MAX_CARDS_AT_HAND;
    }

    public List<PropertySet> getCompleteSets() {
        return propertySets.stream().filter(PropertySet::isComplete).collect(Collectors.toList());
    }

    public List<PropertySet> getAllPropertySets() {
        return new ArrayList<>(propertySets);
    }

    public int getTotalRent() {
        return propertySets.stream().mapToInt(PropertySet::calculateRent).sum();
    }

    private PropertyColor getTargetColorForCard(Card card) {
        if (card instanceof PropertyCard) {
            return ((PropertyCard) card).getColor();
        } else if (card instanceof WildPropertyCard) {
            return ((WildPropertyCard) card).getPossibleColors().get(0);
        }
        return null;
    }

    private PropertySet findCompatibleSet(PropertyColor color) {
        for (PropertySet ps : propertySets) {
            if (ps.getColor() == color && !ps.isComplete()) {
                return ps;
            }
        }
        return null;
    }

    private int getRequiredCountForColor(PropertyColor color) {
        switch (color) {
            case BROWN: return 2;
            case LIGHT_BLUE: return 3;
            case PINK: return 3;
            case ORANGE: return 3;
            case RED: return 3;
            case YELLOW: return 3;
            case GREEN: return 3;
            case DARK_BLUE: return 2;
            case RAILROAD: return 4;
            case UTILITY: return 2;
            default: return 0;
        }
    }
}