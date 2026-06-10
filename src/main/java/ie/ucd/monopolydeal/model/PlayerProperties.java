package ie.ucd.monopolydeal.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Property-table operations used by Player
final class PlayerProperties {
    private PlayerProperties() {
    }

    static boolean receivePropertyCard(Map<PropertyColor, PropertySet> propertySets, Card card, PropertyColor color) {
        PropertySet set = propertySets.get(color);
        if (set == null) {
            return false;
        }

        // Incoming wild cards must be configured to the recipient set's color
        if (card instanceof WildPropertyCard wild) {
            if (!wild.getPossibleColors().contains(color)) {
                return false;
            }
            wild.setCurrentColor(color);
        }

        // Fixed-color property cards cannot be received into a different color set
        if (card instanceof PropertyCard propertyCard && propertyCard.getColor() != color) {
            return false;
        }

        return set.addProperty(card);
    }

    static boolean removePropertyCard(Map<PropertyColor, PropertySet> propertySets, Card card) {
        // Upgrades are searched together with property cards because both live under PropertySet
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

    static PropertyColor getPropertyColor(Map<PropertyColor, PropertySet> propertySets, Card card) {
        // Checks all cards in a set so upgrades and wild cards can be resolved through the same lookup
        for (Map.Entry<PropertyColor, PropertySet> entry : propertySets.entrySet()) {
            if (entry.getValue().getAllCards().contains(card)) {
                return entry.getKey();
            }
        }
        return null;
    }

    static List<Card> getStealableCards(Map<PropertyColor, PropertySet> propertySets) {
        List<Card> stealable = new ArrayList<>();
        for (PropertySet set : propertySets.values()) {
            // Completed sets are protected from stealable-card effects
            if (!set.isFullSet()) {
                stealable.addAll(set.getCards());
            }
        }
        return stealable;
    }

    static boolean transferFullSetTo(Map<PropertyColor, PropertySet> propertySets,
                                     Player recipient, PropertyColor color) {
        PropertySet sourceSet = propertySets.get(color);
        PropertySet targetSet = recipient.getPropertySets().get(color);
        if (sourceSet == null || targetSet == null || !sourceSet.isFullSet()) {
            return false;
        }

        // No per-color limit, so a stolen full set merges into the recipient even if they
        // already own that color.
        if (!sourceSet.transferUpgradesTo(targetSet)) {
            return false;
        }

        // Copies first because the source set is mutated while the transfer loop runs
        List<Card> cardsToMove = new ArrayList<>(sourceSet.getCards());
        for (Card card : cardsToMove) {
            removePropertyCard(propertySets, card);
            if (!recipient.receivePropertyCard(card, color)) {
                return false;
            }
        }
        return true;
    }
}
