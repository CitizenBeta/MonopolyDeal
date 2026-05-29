package ie.ucd.monopolydeal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PropertySet {
    private final PropertyColor color;
    // Holds only property cards; house and hotel upgrades are tracked separately.
    private final List<Card> cards = new ArrayList<>();
    private ActionCard houseCard;
    private ActionCard hotelCard;

    public PropertySet(PropertyColor color) {
        this.color = color;
    }

    public PropertyColor getColor() {
        return color;
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    public List<Card> getUpgradeCards() {
        List<Card> upgrades = new ArrayList<>();
        if (houseCard != null) {
            upgrades.add(houseCard);
        }
        if (hotelCard != null) {
            upgrades.add(hotelCard);
        }
        return Collections.unmodifiableList(upgrades);
    }

    public List<Card> getAllCards() {
        // Combines base properties and upgrades for asset-value and ownership checks.
        List<Card> allCards = new ArrayList<>(cards);
        allCards.addAll(getUpgradeCards());
        return Collections.unmodifiableList(allCards);
    }

    public ActionCard getHouseCard() {
        return houseCard;
    }

    public ActionCard getHotelCard() {
        return hotelCard;
    }

    public int getPropertyCount() {
        return cards.size();
    }

    public int getHouseCount() {
        if (houseCard == null) {
            return 0;
        }
        return 1;
    }

    public int getHotelCount() {
        if (hotelCard == null) {
            return 0;
        }
        return 1;
    }

    public boolean isFullSet() {
        return cards.size() >= color.getSize();
    }

    public boolean addProperty(Card card) {
        // Refuses extra cards once the set has reached the Monopoly color size.
        if (!canAddProperty()) {
            return false;
        }
        cards.add(card);
        return true;
    }

    public void removeProperty(Card card) {
        cards.remove(card);
    }

    public boolean canAddProperty() {
        return cards.size() < color.getSize();
    }

    public boolean addHouse(ActionCard card) {
        // A house must be the correct action type and must satisfy the set-state rules.
        if (!canAddHouse() || card == null || card.getActionType() != ActionType.HOUSE) {
            return false;
        }
        houseCard = card;
        return true;
    }

    public boolean addHotel(ActionCard card) {
        // A hotel depends on an existing house and the absence of another hotel.
        if (!canAddHotel() || card == null || card.getActionType() != ActionType.HOTEL) {
            return false;
        }
        hotelCard = card;
        return true;
    }

    public boolean removeUpgradeCard(Card card) {
        if (card == houseCard) {
            houseCard = null;
            // Removing the house also removes the hotel because a hotel cannot stand alone.
            if (hotelCard != null) {
                hotelCard = null;
            }
            return true;
        }

        if (card == hotelCard) {
            hotelCard = null;
            return true;
        }

        return false;
    }

    public void removeHouse() {
        houseCard = null;
        hotelCard = null;
    }

    public void removeHotel() {
        hotelCard = null;
    }

    public boolean canAddHouse() {
        return canHaveBuildings() && isFullSet() && houseCard == null && hotelCard == null;
    }

    public boolean canAddHotel() {
        return canHaveBuildings() && isFullSet() && hotelCard == null && houseCard != null;
    }

    private boolean canHaveBuildings() {
        return color != PropertyColor.RAILROAD && color != PropertyColor.UTILITY;
    }

    private void clearUpgrades() {
        houseCard = null;
        hotelCard = null;
    }

    public int calculateRent() {
        if (cards.isEmpty()) {
            return 0;
        }
        int rent = color.getRent(cards.size());
        // Upgrade bonuses apply only after the property group is complete.
        if (isFullSet()) {
            rent += getHouseCount() * 3;
            rent += getHotelCount() * 4;
        }
        return rent;
    }

    public void transferUpgradesTo(PropertySet target) {
        // Copies upgrades only into empty target upgrade slots.
        if (houseCard != null && target.houseCard == null) {
            target.houseCard = houseCard;
        }
        if (hotelCard != null && target.hotelCard == null) {
            target.hotelCard = hotelCard;
        }
        clearUpgrades();
    }

    public void restore(List<Card> propertyCards, ActionCard houseCard, ActionCard hotelCard) {
        // Rebuilds the set from a saved state, replacing all current cards and upgrades.
        cards.clear();
        cards.addAll(propertyCards);
        this.houseCard = houseCard;
        this.hotelCard = hotelCard;
    }

    public String summary() {
        return color.getName() + " set: " + cards.size() + "/" + color.getSize() +
                ", rent=" + calculateRent() + "M, house=" + getHouseCount() + ", hotel=" + getHotelCount();
    }
}
