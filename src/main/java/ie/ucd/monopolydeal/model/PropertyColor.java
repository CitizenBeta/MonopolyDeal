package ie.ucd.monopolydeal.model;

import java.util.Arrays;
import java.util.List;

public enum PropertyColor {
    BROWN("Brown", 2, new int[]{1, 2}),
    LIGHT_BLUE("Light Blue", 3, new int[]{1, 2, 3}),
    PINK("Pink", 3, new int[]{1, 2, 4}),
    ORANGE("Orange", 3, new int[]{1, 3, 5}),
    RED("Red", 3, new int[]{2, 3, 6}),
    YELLOW("Yellow", 3, new int[]{2, 4, 6}),
    GREEN("Green", 3, new int[]{2, 4, 7}),
    DARK_BLUE("Dark Blue", 2, new int[]{3, 8}),
    RAILROAD("Railroad", 4, new int[]{1, 2, 3, 4}),
    UTILITY("Utility", 2, new int[]{1, 2});

    private final String displayName;
    private final int fullSetSize;
    private final int[] rents;


    PropertyColor(String displayName, int fullSetSize, int[] rents) {
        this.displayName = displayName;
        this.fullSetSize = fullSetSize;


        this.rents = rents != null ? rents.clone() : new int[0];
    }

    public String getName() {
        return displayName;
    }

    public int getSize() {
        return fullSetSize;
    }

    public int getRent(int propertyCount) {
        if (propertyCount <= 0 || rents.length == 0) {
            return 0;
        }

        int bounded = Math.min(propertyCount, rents.length);
        return rents[bounded - 1];
    }

    public String getRentDescription() {
        StringBuilder description = new StringBuilder();

        for (int i = 0; i < rents.length; i++) {
            if (i > 0) {
                description.append("\n");
            }
            description.append(i + 1).append("=").append(rents[i]).append("M");
        }

        return description.toString();
    }

    public static List<PropertyColor> getColors() {
        return Arrays.asList(values());
    }
}
