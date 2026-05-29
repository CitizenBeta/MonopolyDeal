package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.game.Game;
import ie.ucd.monopolydeal.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;
import java.util.function.Consumer;

public final class CardUI {
    private CardUI() {
    }

    // Create a box for a used card
    public static HBox newUsedCardBox(Game.UsedCard usedCard) {
        Card card = usedCard.card();

        // Set card name
        Label name = new Label(card.getName().replace("/", "/\u200B"));
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        name.setTextFill(Color.rgb(15, 23, 42));

        // Set card detail
        String detailText = cardDetail(card);
        Label detail = new Label(detailText);
        detail.setWrapText(true);
        detail.setTextFill(Color.rgb(71, 85, 105));

        VBox textBox;
        if (detailText.isEmpty()) {
            textBox = new VBox(4, name);
        } else {
            textBox = new VBox(4, name, detail);
        }
        textBox.setFillWidth(true);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        // Create a badge for a used card
        Label usedCardBadge;
        if (usedCard.action() == Game.CardAction.DISCARDED) {
            usedCardBadge = GameUI.newBadge(usedCard.action().getLabel() + " by " + usedCard.player(),
                    Color.rgb(254, 226, 226), Color.rgb(153, 27, 27));
        } else {
            usedCardBadge = GameUI.newBadge(usedCard.action().getLabel() + " by " + usedCard.player(),
                    Color.rgb(220, 252, 231), Color.rgb(22, 101, 52));
        }

        // Create a HBox to pack every element
        HBox box = new HBox(12, newUsedCardBar(card), textBox, usedCardBadge);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setFillHeight(true);
        box.setPadding(new Insets(12));
        box.setMaxWidth(Double.MAX_VALUE);
        box.setBackground(GameUI.solidBackground(Color.WHITE));
        box.setBorder(GameUI.roundCorner(Color.rgb(203, 213, 225)));
        return box;
    }

    private static Region newUsedCardBar(Card card) {
        // Use split color bar when card has two colors
        List<PropertyColor> colors = getCardColors(card);
        if (colors != null) {
            return newColorBar(colors.get(0), colors.get(1), 6, 44, true);
        }

        return newColorBar(cardColor(card), 6, 44, true);
    }

    // Add a card row in the player's hand table
    public static VBox newHandCard(Card card, boolean selected, Consumer<Card> onCardClicked) {
        // Setup card name
        Label name = new Label(cardTitle(card));
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        name.setTextFill(Color.rgb(15, 23, 42));
        name.setWrapText(true);
        name.setMaxWidth(86);

        StackPane titleBox = new StackPane(name);
        titleBox.setMinHeight(Region.USE_PREF_SIZE);
        titleBox.setPadding(new Insets(0, 0, 3, 0));
        titleBox.setMaxWidth(Double.MAX_VALUE);
        titleBox.setAlignment(Pos.TOP_CENTER);
        name.setAlignment(Pos.CENTER);
        name.setTextAlignment(TextAlignment.CENTER);

        // Add card detail
        VBox textBox = new VBox(4);
        if (card instanceof MoneyCard) {
            textBox.getChildren().add(titleBox);
        } else {
            textBox.getChildren().addAll(titleBox, new Separator());
            if (card instanceof WildPropertyCard wildCard && wildCard.getPossibleColors().size() == 2) {
                textBox.getChildren().add(newWildRentBox(wildCard));
            } else if (card instanceof PropertyCard propertyCard) {
                textBox.getChildren().add(newPropertyDetailBox(propertyCard.getColor()));
            } else {
                String detailText = cardDetail(card);
                if (!detailText.isEmpty()) {
                    Label detail = new Label(detailText);
                    detail.setFont(Font.font("Segoe UI", 12));
                    detail.setTextFill(Color.rgb(71, 85, 105));
                    detail.setWrapText(true);
                    detail.setMaxWidth(86);
                    detail.setMinHeight(Region.USE_PREF_SIZE);
                    textBox.getChildren().add(detail);
                }
            }
        }

        // Adapt to various card detail text
        // Push the bar to correct place
        Region blank = new Region();
        VBox.setVgrow(blank, Priority.ALWAYS);

        // Pack all elements
        VBox box = new VBox(6, textBox, blank, newCardBar(card));
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(8));
        box.setPrefSize(104, 154);
        box.setMinSize(96, 146);
        box.setMaxSize(112, 162);

        // Set focus
        if (selected) {
            box.setBackground(GameUI.solidBackground(Color.rgb(239, 246, 255)));
            box.setBorder(GameUI.roundCorner(Color.rgb(37, 99, 235)));
        } else {
            box.setBackground(GameUI.solidBackground(Color.WHITE));
            box.setBorder(GameUI.roundCorner(cardColor(card)));
        }

        // If clicked again, remove focus
        box.setOnMouseClicked(e -> {
            onCardClicked.accept(card);
            e.consume();
        });

        return box;
    }

    private static String cardTitle(Card card) {
        // Force two-color wild title to break between colors
        if (card instanceof WildPropertyCard wildCard && wildCard.getPossibleColors().size() == 2) {
            PropertyColor leftColor = wildCard.getPossibleColors().get(0);
            PropertyColor rightColor = wildCard.getPossibleColors().get(1);
            return leftColor.getName() + "/\n" + rightColor.getName() + " Wild";
        }

        return card.getName().replace("/", "/\u200B");
    }

    // Show property color and rent table
    private static VBox newPropertyDetailBox(PropertyColor color) {
        // Show color name first
        Label colorName = newSmallCardText(color.getName());
        VBox box = new VBox(1, colorName);

        // Convert rent text into compact card rows
        String[] rents = color.getRentDescription().split("\n");
        box.getChildren().add(newRentGrid(rents));

        return box;
    }

    // Split longer rent tables into two columns
    private static HBox newRentGrid(String[] rents) {
        // Keep short rent tables in one column
        if (rents.length <= 2) {
            VBox singleColumn = new VBox(0);
            for (String rent : rents) {
                singleColumn.getChildren().add(newSmallCardText(rent));
            }

            HBox rentGrid = new HBox(singleColumn);
            rentGrid.setMaxWidth(86);
            return rentGrid;
        }

        VBox leftColumn = new VBox(0);
        VBox rightColumn = new VBox(0);
        int splitIndex = (rents.length + 1) / 2;

        // Fill left side first
        for (int i = 0; i < splitIndex; i++) {
            leftColumn.getChildren().add(newSmallCardText(rents[i]));
        }

        // Align later rent rows to the right side
        for (int i = splitIndex; i < rents.length; i++) {
            Label rent = newSmallCardText(rents[i]);
            rent.setAlignment(Pos.CENTER_RIGHT);
            rightColumn.getChildren().add(rent);
        }

        leftColumn.setMaxWidth(Double.MAX_VALUE);
        rightColumn.setMaxWidth(Double.MAX_VALUE);
        leftColumn.setAlignment(Pos.TOP_LEFT);
        rightColumn.setAlignment(Pos.TOP_RIGHT);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        // Pack both rent columns inside card detail width
        HBox rentGrid = new HBox(8, leftColumn, rightColumn);
        rentGrid.setMaxWidth(86);
        return rentGrid;
    }

    private static Label newSmallCardText(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", 12));
        label.setTextFill(Color.rgb(71, 85, 105));
        label.setMaxWidth(86);
        return label;
    }

    // Show wild card rents on two sides
    private static HBox newWildRentBox(WildPropertyCard wildCard) {
        // Read both possible colors
        PropertyColor leftColor = wildCard.getPossibleColors().get(0);
        PropertyColor rightColor = wildCard.getPossibleColors().get(1);

        // Left color rent stays left aligned
        Label leftRent = newSmallCardText(leftColor.getRentDescription());
        leftRent.setAlignment(Pos.CENTER_LEFT);
        leftRent.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(leftRent, Priority.ALWAYS);

        // Right color rent stays right aligned
        Label rightRent = newSmallCardText(rightColor.getRentDescription());
        rightRent.setAlignment(Pos.CENTER_RIGHT);
        rightRent.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(rightRent, Priority.ALWAYS);

        // Pack two rent tables into one card detail row
        HBox rentBox = new HBox(leftRent, rightRent);
        rentBox.setMaxWidth(86);
        rentBox.setAlignment(Pos.CENTER);
        return rentBox;
    }

    // Add color bar for cards
    private static Region newCardBar(Card card) {
        // Use split color bar for wild and two-color rent cards
        List<PropertyColor> colors = getCardColors(card);
        if (colors != null) {
            return newColorBar(colors.get(0), colors.get(1), 84, 6, false);
        }

        return newColorBar(cardColor(card), 84, 6, false);
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

    // Short text for card faces and used-card history
    public static String cardDetail(Card card) {
        return switch (card) {
            case PropertyCard propertyCard -> propertyCard.getColor().getName() + "\n" + propertyCard.getColor().getRentDescription();
            case WildPropertyCard wildCard -> {
                String currentColor;
                if (wildCard.getCurrentColor() == null) {
                    currentColor = "Not selected";
                } else {
                    currentColor = wildCard.getCurrentColor().getName();
                }
                yield currentColor;
            }
            case ActionCard actionCard -> actionCard.getActionType().getDescription();
            default -> "";
        };
    }

    // Short status text for top status card
    public static String statusCardText(Card card) {
        return switch (card) {
            case MoneyCard moneyCard -> moneyCard.getName();
            case PropertyCard _ -> "property card";
            case WildPropertyCard _ -> "wild card";
            case ActionCard actionCard -> actionCard.getName();
            case null, default -> "card";
        };
    }

    // Main display color for card border and bar
    public static Color cardColor(Card card) {
        return switch (card) {
            case MoneyCard _ -> Color.rgb(22, 163, 74);
            case PropertyCard propertyCard -> propertyColor(propertyCard.getColor());
            case WildPropertyCard wildCard -> {
                if (wildCard.getCurrentColor() == null) {
                    if (wildCard.getPossibleColors().size() == 2) {
                        yield propertyColor(wildCard.getPossibleColors().get(0));
                    }
                    yield Color.rgb(8, 145, 178);
                }
                yield propertyColor(wildCard.getCurrentColor());
            }
            case ActionCard actionCard -> {
                if (actionCard.getColors().size() == 2) {
                    yield propertyColor(actionCard.getColors().get(0));
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
