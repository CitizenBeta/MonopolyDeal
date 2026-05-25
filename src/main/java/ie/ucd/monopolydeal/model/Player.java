package ie.ucd.monopolydeal.model;

import ie.ucd.monopolydeal.game.Deck;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Player {
    public static final int MAX_HAND_SIZE = 7;
    public static final int MAX_CARDS_AT_HAND = MAX_HAND_SIZE;
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
    public List<Card> getHand() { return hand; }
    // Compatibility alias for code/tests that still use the older hand naming.
    public List<Card> getCardsAtHand() { return hand; }
    public List<Card> getBankCash() { return bankCash; }
    // Compatibility alias for code/tests that still use the older bank naming.
    public List<Card> getCardsAtBank() { return bankCash; }
    public Map<PropertyColor, PropertySet> getPropertySets() { return propertySets; }

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

    public boolean bankMoneyCard(MoneyCard card) {
        // Banking is only valid for a money card currently held by this player.
        if (hand.contains(card)) {
            hand.remove(card);
            bankCash.add(card);
            return true;
        }
        return false;
    }

    public void discardFromHand(Card card, Deck deck) {
        // The deck discard pile is updated only after ownership is removed from the player.
        if (hand.remove(card)) {
            deck.discard(card);
        }
    }

    public boolean isHandFull() {
        return hand.size() >= MAX_HAND_SIZE;
    }

    public List<Card> discardExcessCards(Deck deck) {
        List<Card> discarded = new ArrayList<>();
        // Trims from the end of the hand, preserving the earlier card order.
        while (hand.size() > MAX_HAND_SIZE) {
            Card toDiscard = hand.remove(hand.size() - 1);
            deck.discard(toDiscard);
            discarded.add(toDiscard);
        }
        return discarded;
    }

    public boolean addProperty(PropertyCard card) {
        // Property cards must leave the hand before they become table assets.
        if (!hand.contains(card)) return false;
        PropertySet set = propertySets.get(card.getColor());
        if (!set.canAddProperty()) return false;
        hand.remove(card);
        set.addProperty(card);
        return true;
    }

    public boolean addWildProperty(WildPropertyCard card, PropertyColor color) {
        // A wild card can only be placed onto one of its currently supported colors.
        if (!hand.contains(card)) return false;
        if (color == null || !card.getPossibleColors().contains(color)) return false;
        PropertySet set = propertySets.get(color);
        if (set == null) return false;
        if (!set.canAddProperty()) return false;
        card.setCurrentColor(color);
        hand.remove(card);
        set.addProperty(card);
        return true;
    }

    public void moveExistingWild(WildPropertyCard card, PropertyColor newColor) {
        // Repositioning an existing wild card is ignored instead of failing loudly for invalid moves.
        if (newColor == null || !card.getPossibleColors().contains(newColor)) {
            return;
        }

        PropertyColor currentColor = card.getCurrentColor();
        if (currentColor == newColor) {
            return;
        }

        PropertySet targetSet = propertySets.get(newColor);
        if (targetSet == null || !targetSet.canAddProperty()) {
            return;
        }

        // Remove from the previous color bucket before assigning the new color.
        if (currentColor != null) {
            propertySets.get(currentColor).removeProperty(card);
        }

        card.setCurrentColor(newColor);
        targetSet.addProperty(card);
    }

    public boolean receivePropertyCard(Card card, PropertyColor color) {
        PropertySet set = propertySets.get(color);
        if (set == null || !set.canAddProperty()) {
            return false;
        }

        // Incoming wild cards must be configured to the recipient set's color.
        if (card instanceof WildPropertyCard wild) {
            if (!wild.getPossibleColors().contains(color)) {
                return false;
            }
            wild.setCurrentColor(color);
        }

        // Fixed-color property cards cannot be received into a different color set.
        if (card instanceof PropertyCard propertyCard && propertyCard.getColor() != color) {
            return false;
        }

        set.addProperty(card);
        return true;
    }

    public boolean removePropertyCard(Card card) {
        // Upgrades are searched together with property cards because both live under PropertySet.
        for (PropertySet set : propertySets.values()) {
            if (set.getCards().contains(card)) {
                set.removeProperty(card);
                return true;
            }
            if (set.removeUpgradeCard(card)) {
                return true;
            }
        }
        return false;
    }

    public PropertyColor getPropertyColor(Card card) {
        // Checks all cards in a set so upgrades and wild cards can be resolved through the same lookup.
        for (Map.Entry<PropertyColor, PropertySet> entry : propertySets.entrySet()) {
            if (entry.getValue().getAllCards().contains(card)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public List<Card> getStealableCards() {
        List<Card> stealable = new ArrayList<>();
        for (PropertySet set : propertySets.values()) {
            // Completed sets are protected from stealable-card effects.
            if (!set.isFullSet()) {
                stealable.addAll(set.getCards());
            }
        }
        return stealable;
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
        PropertySet sourceSet = propertySets.get(color);
        PropertySet targetSet = recipient.getPropertySets().get(color);
        if (sourceSet == null || targetSet == null || !sourceSet.isFullSet()) {
            return false;
        }

        // Prevents merging into a recipient set that would exceed the Monopoly color size.
        if (targetSet.getCards().size() + sourceSet.getCards().size() > color.getSize()) {
            return false;
        }

        sourceSet.transferUpgradesTo(targetSet);

        // Copies first because the source set is mutated while the transfer loop runs.
        List<Card> cardsToMove = new ArrayList<>(sourceSet.getCards());
        for (Card card : cardsToMove) {
            removePropertyCard(card);
            recipient.receivePropertyCard(card, color);
        }
        return true;
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

    @Deprecated
    public int getBankTotal() {
        return getTotalAssetValue();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Player %d: %s (Cash: %dM, Total Assets: %dM)%n",
                number, name, getCashValue(), getTotalAssetValue()));
        sb.append("  Hand: ").append(hand.size()).append("/").append(MAX_HAND_SIZE).append(" cards\n");
        sb.append("  Properties:\n");
        for (PropertySet set : propertySets.values()) {
            if (!set.getCards().isEmpty()) {
                sb.append("    ").append(set.summary()).append("\n");
            }
        }
        return sb.toString();
    }
}
