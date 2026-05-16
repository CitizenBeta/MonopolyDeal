package ie.ucd.monopolydeal.model;

import java.util.ArrayList;
import java.util.List;

public class PropertySet {
    private final PropertyColor color;
    private final List<Card> cards = new ArrayList<>();
    private int houseCount;
    private int hotelCount;

    public PropertySet(PropertyColor color) {
        this.color = color;
    }

    public PropertyColor getColor() {
        return color;
    }

    public List<Card> getCards() {
        return cards;
    }

    public int getPropertyCount() {
        return cards.size();
    }

    public int getHouseCount() {
        return houseCount;
    }

    public int getHotelCount() {
        return hotelCount;
    }

    public boolean isFullSet() {
        return cards.size() >= color.getSize();
    }

    public boolean addProperty(Card card) {
        if (!canAddProperty()) {
            return false;
        }
        cards.add(card);
        return true;
    }

    public void removeProperty(Card card) {
        cards.remove(card);
        if (!isFullSet()) {
            clearUpgrades();
        }
    }

    public boolean canAddProperty() {
        return cards.size() < color.getSize();
    }

    public boolean addHouse() {
        if (!canAddHouse()) {
            return false;
        }
        houseCount = 1;
        return true;
    }

    public boolean addHotel() {
        if (!canAddHotel()) {
            return false;
        }
        hotelCount = 1;
        houseCount = 0;
        return true;
    }

    public void removeHouse() {
        houseCount = 0;
    }

    public void removeHotel() {
        hotelCount = 0;
    }

    public boolean canAddHouse() {
        return isFullSet() && houseCount == 0 && hotelCount == 0;
    }

    public boolean canAddHotel() {
        return isFullSet() && hotelCount == 0 && houseCount == 1;
    }

    private void clearUpgrades() {
        houseCount = 0;
        hotelCount = 0;
    }

    public int calculateRent() {
        if (cards.isEmpty()) {
            return 0;
        }
        int rent = color.getRent(cards.size());
        rent += houseCount * 3;
        rent += hotelCount * 4;
        return rent;
    }

    public void transferUpgradesTo(PropertySet target) {
        target.houseCount += houseCount;
        target.hotelCount += hotelCount;
        if (target.houseCount > 1) target.houseCount = 1;
        if (target.hotelCount > 1) target.hotelCount = 1;
        clearUpgrades();
    }

    public String summary() {
        return color.getName() + " set: " + cards.size() + "/" + color.getSize() +
                ", rent=" + calculateRent() + "M, house=" + houseCount + ", hotel=" + hotelCount;
    }
}
