package ie.ucd.monopolydeal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WildPropertyCard implements Card {
    private final String name;
    private final List<PropertyColor> possibleColors;
    private final int bankValue;
    private PropertyColor currentColor;

    public WildPropertyCard(String name, List<PropertyColor> possibleColors, int bankValue) {
        this.name = name;
        this.possibleColors = new ArrayList<>(possibleColors);
        this.bankValue = bankValue;
    }

    public List<PropertyColor> getPossibleColors() {
        return Collections.unmodifiableList(possibleColors);
    }

    public PropertyColor getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(PropertyColor currentColor) {
        if (!possibleColors.contains(currentColor)) {
            throw new IllegalArgumentException("Invalid color for this wild card.");
        }
        this.currentColor = currentColor;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getBankValue() {
        return bankValue;
    }

    @Override
    public String getDetail() {
        String colors = possibleColors.stream().map(PropertyColor::getName).collect(Collectors.joining("/"));
        String current = currentColor == null ? "unplaced" : currentColor.getName();
        return name + " [Wild, " + colors + ", current " + current + ", bank " + bankValue + "M]";
    }

    @Override
    public String toString() {
        return getDetail();
    }
}
