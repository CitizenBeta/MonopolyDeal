package ie.ucd.monopolydeal.model;

import java.util.Arrays;
import java.util.List;

public enum Color {
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


    Color(String displayName, int fullSetSize, int[] rents) {
        this.displayName = displayName;
        this.fullSetSize = fullSetSize;

        // 添加 rents != null 防止传入 null 时抛出空指针异常
        this.rents = rents != null ? rents.clone() : new int[0];
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getFullSetSize() {
        return fullSetSize;
    }

    public int getRent(int propertyCount) {
        int bounded = Math.max(1, Math.min(propertyCount, rents.length));
        return rents[bounded - 1];
    }

    public static List<Color> getColors() {
        return Arrays.asList(values());
    }
}