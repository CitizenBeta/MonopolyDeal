package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.model.*;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.List;

// Shared color and color-bar helpers for card rendering
public final class CardColorUI {
    private CardColorUI() {
    }

    // Use split bar for two-color cards
    public static List<PropertyColor> getCardColors(Card card) {
        // Wild property cards can have two possible colors
        if (card instanceof WildPropertyCard wildCard && wildCard.getPossibleColors().size() == 2) {
            return wildCard.getPossibleColors();
        }

        // Rent action cards can also target two colors
        if (card instanceof ActionCard actionCard && actionCard.getColors().size() == 2) {
            return actionCard.getColors();
        }

        return null;
    }

    // Create horizontal or vertical split color bar
    public static Region newColorBar(PropertyColor firstColor, PropertyColor secondColor,
                                     double width, double height, boolean vertical) {
        // Convert game colors to JavaFX colors
        Color leftColor = propertyColor(firstColor);
        Color rightColor = propertyColor(secondColor);
        String direction = vertical ? "to bottom" : "to right";

        // Create fixed-size bar
        Region bar = new Region();
        bar.setPrefSize(width, height);

        // Vertical bars need stable width, horizontal bars need stable height
        if (vertical) {
            bar.setMinWidth(width);
        } else {
            bar.setMinHeight(height);
            bar.setMaxHeight(height);
            bar.setMaxWidth(Double.MAX_VALUE);
        }

        // Use CSS gradient to show two colors in one bar
        bar.setStyle("-fx-background-color: linear-gradient(" + direction + ", "
                + cssColor(leftColor) + " 0%, "
                + cssColor(leftColor) + " 50%, "
                + cssColor(rightColor) + " 50%, "
                + cssColor(rightColor) + " 100%); -fx-background-radius: 12;");
        return bar;
    }

    // Create single-color bar
    public static Region newColorBar(Color color, double width, double height, boolean vertical) {
        // Create fixed-size bar
        Region bar = new Region();
        bar.setPrefSize(width, height);

        // Vertical bars need stable width, horizontal bars need stable height
        if (vertical) {
            bar.setMinWidth(width);
        } else {
            bar.setMinHeight(height);
            bar.setMaxHeight(height);
            bar.setMaxWidth(Double.MAX_VALUE);
        }

        // Apply single solid color
        bar.setBackground(GameUI.solidBackground(color));
        return bar;
    }

    // Main display color for card border and bar
    public static Color cardColor(Card card) {
        return switch (card) {
            case MoneyCard _ -> Color.rgb(21, 128, 61);
            case PropertyCard propertyCard -> propertyColor(propertyCard.getColor());
            case WildPropertyCard wildCard -> {
                if (wildCard.getCurrentColor() == null) {
                    if (wildCard.getPossibleColors().size() == 2) {
                        yield propertyColor(wildCard.getPossibleColors().getFirst());
                    }
                    yield Color.rgb(8, 145, 178);
                }
                yield propertyColor(wildCard.getCurrentColor());
            }
            case ActionCard actionCard -> {
                if (actionCard.getColors().size() == 2) {
                    yield propertyColor(actionCard.getColors().getFirst());
                }
                yield Color.rgb(217, 119, 6);
            }
            case null, default -> Color.rgb(100, 116, 139);
        };
    }

    // Map property colors to JavaFX colors
    public static Color propertyColor(PropertyColor color) {
        return switch (color) {
            case BROWN -> Color.rgb(120, 72, 45);
            case LIGHT_BLUE -> Color.rgb(56, 189, 248);
            case PINK -> Color.rgb(236, 72, 153);
            case ORANGE -> Color.rgb(249, 115, 22);
            case RED -> Color.rgb(220, 38, 38);
            case YELLOW -> Color.rgb(234, 179, 8);
            case GREEN -> Color.rgb(22, 163, 74);
            case DARK_BLUE -> Color.rgb(30, 64, 175);
            case RAILROAD -> Color.rgb(71, 85, 105);
            case UTILITY -> Color.rgb(20, 184, 166);
        };
    }

    // Convert JavaFX Color to CSS rgb
    private static String cssColor(Color color) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return "rgb(" + red + "," + green + "," + blue + ")";
    }
}
