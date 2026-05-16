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
    private final Map<PropertyColor, PropertySet> propertySets = new EnumMap<>(PropertyColor.class);

    public Player(String name, int number) {
        this.name = name;
        this.number = number;
        for (PropertyColor color : PropertyColor.values()) {
            propertySets.put(color, new PropertySet(color));
        }
    }

    public String getName() { return name; }
    public int getNumber() { return number; }
    public List<Card> getHand() { return hand; }
    public List<Card> getCardsAtHand() { return hand; }
    public List<Card> getBankCash() { return bankCash; }
    public List<Card> getCardsAtBank() { return bankCash; }
    public Map<PropertyColor, PropertySet> getPropertySets() { return propertySets; }

    public void addCardToHand(Card card) {
        if (card != null) hand.add(card);
    }

    public void removeCardFromHand(Card card) {
        hand.remove(card);
    }

    public void addCardToBank(Card card) {
        if (card != null) {
            bankCash.add(card);
        }
    }

    public boolean bankMoneyCard(MoneyCard card) {
        if (hand.contains(card)) {
            hand.remove(card);
            bankCash.add(card);
            return true;
        }
        return false;
    }

    public void discardFromHand(Card card, Deck deck) {
        if (hand.remove(card)) {
            deck.discard(card);
        }
    }

    public boolean isHandFull() {
        return hand.size() >= MAX_HAND_SIZE;
    }

    public List<Card> discardExcessCards(Deck deck) {
        List<Card> discarded = new ArrayList<>();
        while (hand.size() > MAX_HAND_SIZE) {
            Card toDiscard = hand.remove(hand.size() - 1);
            deck.discard(toDiscard);
            discarded.add(toDiscard);
        }
        return discarded;
    }

    public boolean addProperty(PropertyCard card) {
        if (!hand.contains(card)) return false;
        PropertySet set = propertySets.get(card.getColor());
        if (!set.canAddProperty()) return false;
        hand.remove(card);
        set.addProperty(card);
        return true;
    }

    public boolean addWildProperty(WildPropertyCard card, PropertyColor color) {
        if (!hand.contains(card)) return false;
        PropertySet set = propertySets.get(color);
        if (!set.canAddProperty()) return false;
        card.setCurrentColor(color);
        hand.remove(card);
        set.addProperty(card);
        return true;
    }

    public void moveExistingWild(WildPropertyCard card, PropertyColor newColor) {
        PropertyColor currentColor = card.getCurrentColor();
        if (currentColor != null) {
            propertySets.get(currentColor).removeProperty(card);
        }
        if (propertySets.get(newColor).canAddProperty()) {
            card.setCurrentColor(newColor);
            propertySets.get(newColor).addProperty(card);
        } else {
            if (currentColor != null) {
                propertySets.get(currentColor).addProperty(card);
            }
        }
    }

    public void receivePropertyCard(Card card, PropertyColor color) {
        propertySets.get(color).addProperty(card);
        if (card instanceof WildPropertyCard wild) {
            wild.setCurrentColor(color);
        }
    }

    public void removePropertyCard(Card card) {
        for (PropertySet set : propertySets.values()) {
            if (set.getCards().contains(card)) {
                set.removeProperty(card);
                return;
            }
        }
    }

    public List<Card> getStealableCards() {
        List<Card> stealable = new ArrayList<>();
        for (PropertySet set : propertySets.values()) {
            if (!set.isFullSet()) {
                stealable.addAll(set.getCards());
            }
        }
        return stealable;
    }

    public boolean addHouse(PropertyColor color) {
        return propertySets.get(color).addHouse();
    }

    public boolean addHotel(PropertyColor color) {
        return propertySets.get(color).addHotel();
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

    public void transferFullSetTo(Player recipient, PropertyColor color) {
        PropertySet sourceSet = propertySets.get(color);
        if (!sourceSet.isFullSet()) return;
        PropertySet targetSet = recipient.getPropertySets().get(color);
        sourceSet.transferUpgradesTo(targetSet);

        List<Card> cardsToMove = new ArrayList<>(sourceSet.getCards());
        for (Card card : cardsToMove) {
            removePropertyCard(card);
            recipient.receivePropertyCard(card, color);
        }
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
            for (Card card : set.getCards()) {
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
