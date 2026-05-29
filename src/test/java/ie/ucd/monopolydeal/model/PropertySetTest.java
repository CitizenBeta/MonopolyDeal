package ie.ucd.monopolydeal.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PropertySetTest {

    @Test
    void calculateRentShouldUseNumberOfProperties() {
        PropertySet set = new PropertySet(PropertyColor.BROWN);

        assertEquals(0, set.calculateRent());

        set.addProperty(new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN));
        assertEquals(1, set.calculateRent());
        assertFalse(set.isFullSet());

        set.addProperty(new PropertyCard("Baltic Avenue", 1, PropertyColor.BROWN));
        assertEquals(2, set.calculateRent());
        assertTrue(set.isFullSet());
    }

    @Test
    void fullSetShouldAcceptHouseThenHotel() {
        PropertySet set = new PropertySet(PropertyColor.DARK_BLUE);
        ActionCard house = new ActionCard("House", 3, ActionType.HOUSE);
        ActionCard hotel = new ActionCard("Hotel", 4, ActionType.HOTEL);

        assertFalse(set.addHouse(house));
        assertFalse(set.addHotel(hotel));

        set.addProperty(new PropertyCard("Park Place", 4, PropertyColor.DARK_BLUE));
        set.addProperty(new PropertyCard("Boardwalk", 4, PropertyColor.DARK_BLUE));

        assertTrue(set.isFullSet());
        assertTrue(set.addHouse(house));
        assertTrue(set.addHotel(hotel));
        assertEquals(1, set.getHouseCount());
        assertEquals(1, set.getHotelCount());
        assertEquals(15, set.calculateRent());
    }

    @Test
    void railroadAndUtilityShouldNotAcceptBuildings() {
        PropertySet railroad = new PropertySet(PropertyColor.RAILROAD);
        PropertySet utility = new PropertySet(PropertyColor.UTILITY);
        ActionCard house = new ActionCard("House", 3, ActionType.HOUSE);
        ActionCard hotel = new ActionCard("Hotel", 4, ActionType.HOTEL);

        railroad.addProperty(new PropertyCard("Reading Railroad", 2, PropertyColor.RAILROAD));
        railroad.addProperty(new PropertyCard("Pennsylvania Railroad", 2, PropertyColor.RAILROAD));
        railroad.addProperty(new PropertyCard("B. & O. Railroad", 2, PropertyColor.RAILROAD));
        railroad.addProperty(new PropertyCard("Short Line", 2, PropertyColor.RAILROAD));
        utility.addProperty(new PropertyCard("Electric Company", 2, PropertyColor.UTILITY));
        utility.addProperty(new PropertyCard("Water Works", 2, PropertyColor.UTILITY));

        assertTrue(railroad.isFullSet());
        assertTrue(utility.isFullSet());
        assertFalse(railroad.addHouse(house));
        assertFalse(railroad.addHotel(hotel));
        assertFalse(utility.addHouse(house));
        assertFalse(utility.addHotel(hotel));
    }

    @Test
    void cannotAddMorePropertiesThanColorSize() {
        PropertySet set = new PropertySet(PropertyColor.BROWN);

        assertTrue(set.addProperty(new PropertyCard("Mediterranean Avenue", 1, PropertyColor.BROWN)));
        assertTrue(set.addProperty(new PropertyCard("Baltic Avenue", 1, PropertyColor.BROWN)));
        assertFalse(set.addProperty(new PropertyCard("Extra Brown", 1, PropertyColor.BROWN)));
        assertEquals(2, set.getPropertyCount());
    }

    @Test
    void removingHouseShouldAlsoRemoveHotel() {
        PropertySet set = new PropertySet(PropertyColor.DARK_BLUE);
        ActionCard house = new ActionCard("House", 3, ActionType.HOUSE);
        ActionCard hotel = new ActionCard("Hotel", 4, ActionType.HOTEL);

        set.addProperty(new PropertyCard("Park Place", 4, PropertyColor.DARK_BLUE));
        set.addProperty(new PropertyCard("Boardwalk", 4, PropertyColor.DARK_BLUE));
        set.addHouse(house);
        set.addHotel(hotel);

        set.removeHouse();

        assertEquals(0, set.getHouseCount());
        assertEquals(0, set.getHotelCount());
    }
}
