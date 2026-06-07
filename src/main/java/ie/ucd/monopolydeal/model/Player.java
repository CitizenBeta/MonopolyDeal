package ie.ucd.monopolydeal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Player {
    public static final int MAX_CARDS_AT_HAND = 7;
    public static final int MAX_ACTIONS_PER_TURN = 3;

    private final String name;
    private final int number;
    private final List<Card> hand = new ArrayList<>();
    private final List<Card> bankCash = new ArrayList<>();
    // Uses one pre-created PropertySet per color so property operations can avoid repeated null creation checks.
    private final Map<PropertyColor, PropertySet> propertySets = new EnumMap<>(PropertyColor.class);

    public Player(String name, int number) {
        this.name = name;
        this.number = number;
        // Initializes every color bucket up front, including colors the player may not currently own.
        for (PropertyColor color : PropertyColor.values()) {
            propertySets.put(color, new PropertySet(color));
        }
    }

    public String getName() { return name; }
    public int getNumber() { return number; }
    public List<Card> getHand() { return Collections.unmodifiableList(hand); }
    // Compatibility alias for code/tests that still use the older hand naming.
    public List<Card> getCardsAtHand() { return Collections.unmodifiableList(hand); }
    public List<Card> getBankCash() { return Collections.unmodifiableList(bankCash); }
    // Compatibility alias for code/tests that still use the older bank naming.
    public List<Card> getCardsAtBank() { return Collections.unmodifiableList(bankCash); }
    public Map<PropertyColor, PropertySet> getPropertySets() { return Collections.unmodifiableMap(propertySets); }

    public void addCardToHand(Card card) {
        if (card != null) hand.add(card);
    }

    public void removeCardFromHand(Card card) {
        hand.remove(card);
    }

    public boolean removeCardFromBank(Card card) {
        return bankCash.remove(card);
    }

    public void addCardToBank(Card card) {
        if (card != null) {
            bankCash.add(card);
        }
    }

    public void restoreCards(List<Card> handCards, List<Card> bankCards) {
        hand.clear();
        if (handCards != null) {
            hand.addAll(handCards);
        }

        bankCash.clear();
        if (bankCards != null) {
            bankCash.addAll(bankCards);
        }
    }

    public boolean addProperty(PropertyCard card) {
        // Property cards must leave the hand before they become table assets.
        if (!hand.contains(card)) return false;
        PropertySet set = propertySets.get(card.getColor());
        if (!set.canAddProperty()) return false;
        if (!set.addProperty(card)) return false;
        hand.remove(card);
        return true;
    }

    public boolean addWildProperty(WildPropertyCard card, PropertyColor color) {
        // A wild card can only be placed onto one of its currently supported colors.
        if (!hand.contains(card)) return false;
        if (color == null || !card.getPossibleColors().contains(color)) return false;
        PropertySet set = propertySets.get(color);
        if (set == null) return false;
        if (!set.canAddProperty()) return false;
        if (!set.addProperty(card)) return false;
        hand.remove(card);
        return true;
    }

    public boolean moveExistingWild(WildPropertyCard card, PropertyColor newColor) {
        // Repositioning an existing wild card is ignored instead of failing loudly for invalid moves.
        if (newColor == null || !card.getPossibleColors().contains(newColor)) {
            return false;
        }

        PropertyColor currentColor = card.getCurrentColor();
        if (currentColor == newColor) {
            return false;
        }

        PropertySet targetSet = propertySets.get(newColor);
        if (targetSet == null || !targetSet.canAddProperty()) {
            return false;
        }

        if (currentColor == null || !propertySets.get(currentColor).getCards().contains(card)) {
            return false;
        }

        // Remove from the previous color bucket before assigning the new color.
        propertySets.get(currentColor).removeProperty(card);
        if (targetSet.addProperty(card)) {
            return true;
        }
        propertySets.get(currentColor).addProperty(card);
        return false;
    }

    public boolean receivePropertyCard(Card card, PropertyColor color) {
        return PlayerProperties.receivePropertyCard(propertySets, card, color);
    }

    public boolean removePropertyCard(Card card) {
        return PlayerProperties.removePropertyCard(propertySets, card);
    }

    public PropertyColor getPropertyColor(Card card) {
        return PlayerProperties.getPropertyColor(propertySets, card);
    }

    public List<Card> getStealableCards() {
        return PlayerProperties.getStealableCards(propertySets);
    }

    public boolean addHouse(PropertyColor color, ActionCard card) {
        if (!hand.contains(card)) {
            return false;
        }

        // PropertySet enforces whether this color is eligible for a house.
        if (!propertySets.get(color).addHouse(card)) {
            return false;
        }

        hand.remove(card);
        return true;
    }

    public boolean addHotel(PropertyColor color, ActionCard card) {
        if (!hand.contains(card)) {
            return false;
        }

        // PropertySet enforces whether a hotel can be added on top of the current set state.
        if (!propertySets.get(color).addHotel(card)) {
            return false;
        }

        hand.remove(card);
        return true;
    }

    public void removeHouse(PropertyColor color) {
        propertySets.get(color).removeHouse();
    }

    public void removeHotel(PropertyColor color) {
        propertySets.get(color).removeHotel();
    }

    public int countCompletedSets() {
        int total = 0;
        for (PropertySet set : propertySets.values()) {
            if (set.isFullSet()) total++;
        }
        return total;
    }

    public boolean hasWon() {
        return countCompletedSets() >= 3;
    }

    public List<PropertyColor> getFullSetColors() {
        List<PropertyColor> colors = new ArrayList<>();
        for (Map.Entry<PropertyColor, PropertySet> entry : propertySets.entrySet()) {
            if (entry.getValue().isFullSet()) colors.add(entry.getKey());
        }
        return colors;
    }

    public boolean transferFullSetTo(Player recipient, PropertyColor color) {
        return PlayerProperties.transferFullSetTo(propertySets, recipient, color);
    }

    public List<WildPropertyCard> getPlacedWildCards() {
        List<WildPropertyCard> result = new ArrayList<>();
        for (PropertySet set : propertySets.values()) {
            for (Card card : set.getCards()) {
                if (card instanceof WildPropertyCard wild) {
                    result.add(wild);
                }
            }
        }
        return result;
    }

    public int getCashValue() {
        int total = 0;
        for (Card card : bankCash) {
            total += card.getBankValue();
        }
        return total;
    }

    public int getBankTotalValue() {
        return getCashValue();
    }

    public int getTotalAssetValue() {
        int total = getCashValue();
        for (PropertySet set : propertySets.values()) {
            // Total assets include both properties and upgrades attached to those properties.
            for (Card card : set.getAllCards()) {
                total += card.getBankValue();
            }
        }
        return total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Player %d: %s (Cash: %dM, Total Assets: %dM)%n",
                number, name, getCashValue(), getTotalAssetValue()));
        sb.append("  Hand: ").append(hand.size()).append("/").append(MAX_CARDS_AT_HAND).append(" cards\n");
        sb.append("  Properties:\n");
        for (PropertySet set : propertySets.values()) {
            if (!set.getCards().isEmpty()) {
                sb.append("    ").append(set.summary()).append("\n");
            }
        }
        return sb.toString();
    }
}
