package ie.ucd.monopolydeal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WildPropertyCard extends Card {
    private final List<PropertyColor> possibleColors;
    // Null means the wild card has not yet been assigned to a property set on the table.
    private PropertyColor currentColor;

    public WildPropertyCard(String name, List<PropertyColor> possibleColors, int bankValue) {
        super(name, bankValue);
        // Defensive copy prevents later external changes from altering this card's valid colors.
        this.possibleColors = new ArrayList<>(possibleColors);
    }

    public List<PropertyColor> getPossibleColors() {
        // Exposes valid choices without allowing callers to mutate the backing list.
        return Collections.unmodifiableList(possibleColors);
    }

    public PropertyColor getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(PropertyColor currentColor) {
        // Passing null deliberately resets the card back to an unplaced state.
        if (currentColor == null) {
            this.currentColor = null;
            return;
        }

        // Only colors declared for this specific wild card are accepted.
        if (!possibleColors.contains(currentColor)) {
            throw new IllegalArgumentException("Invalid color for this wild card.");
        }
        this.currentColor = currentColor;
    }

    @Override
    public String getDetail() {
        // Joins possible colors into the same compact format used by card descriptions.
        String colors = possibleColors.stream().map(PropertyColor::getName).collect(Collectors.joining("/"));
        // Keeps the detail string meaningful before the card is placed into a set.
        String current = currentColor == null ? "unplaced" : currentColor.getName();
        return getName() + " [Wild, " + colors + ", current " + current + ", bank "
                + getBankValue() + "M]";
    }
}
