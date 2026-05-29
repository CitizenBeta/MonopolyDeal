package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.game.Game;
import ie.ucd.monopolydeal.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;
import java.util.function.Consumer;

final class GameUI {
    // Static helper class for JavaFX controls
    private GameUI() {
    }

    // Create a box for a used card
    static HBox newUsedCardBox(Game.UsedCard usedCard) {
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
            usedCardBadge = newBadge(usedCard.action().getLabel() + " by " + usedCard.player(),
                    Color.rgb(254, 226, 226), Color.rgb(153, 27, 27));
        } else {
            usedCardBadge = newBadge(usedCard.action().getLabel() + " by " + usedCard.player(),
                    Color.rgb(220, 252, 231), Color.rgb(22, 101, 52));
        }

        // Create a HBox to pack every element
        HBox box = new HBox(12, newUsedCardBar(card), textBox, usedCardBadge);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setFillHeight(true);
        box.setPadding(new Insets(12));
        box.setMaxWidth(Double.MAX_VALUE);
        box.setBackground(solidBackground(Color.WHITE));
        box.setBorder(roundCorner(Color.rgb(203, 213, 225)));
        return box;
    }

    private static Region newUsedCardBar(Card card) {
        List<PropertyColor> colors = getCardColors(card);
        if (colors != null) {
            return newColorBar(colors.get(0), colors.get(1), 6, 44, true);
        }

        return newColorBar(cardColor(card), 6, 44, true);
    }

    // Add a card row in the player's hand table
    static VBox newHandCard(Card card, boolean selected, Consumer<Card> onCardClicked) {
        // Setup card name
        Label name = new Label(cardTitle(card));
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        name.setTextFill(Color.rgb(15, 23, 42));
        name.setWrapText(true);
        name.setMaxWidth(86);

        // Keep card title centered
        StackPane titleBox = new StackPane(name);
        titleBox.setMinHeight(Region.USE_PREF_SIZE);
        titleBox.setPadding(new Insets(0, 0, 3, 0));
        titleBox.setMaxWidth(Double.MAX_VALUE);
        titleBox.setAlignment(Pos.TOP_CENTER);
        name.setAlignment(Pos.CENTER);
        name.setTextAlignment(TextAlignment.CENTER);

        VBox textBox = new VBox(4);
        // Add card detail
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
            box.setBackground(solidBackground(Color.rgb(239, 246, 255)));
            box.setBorder(roundCorner(Color.rgb(37, 99, 235)));
        } else {
            box.setBackground(solidBackground(Color.WHITE));
            box.setBorder(roundCorner(cardColor(card)));
        }

        box.setOnMouseClicked(e -> {
            // Send clicked card back to GameController
            onCardClicked.accept(card);
            e.consume();
        });

        return box;
    }

    private static String cardTitle(Card card) {
        if (card instanceof WildPropertyCard wildCard && wildCard.getPossibleColors().size() == 2) {
            PropertyColor leftColor = wildCard.getPossibleColors().get(0);
            PropertyColor rightColor = wildCard.getPossibleColors().get(1);
            return leftColor.getName() + "/\n" + rightColor.getName() + " Wild";
        }

        return card.getName().replace("/", "/\u200B");
    }

    // Show property color and rent table
    private static VBox newPropertyDetailBox(PropertyColor color) {
        Label colorName = newSmallCardText(color.getName());
        VBox box = new VBox(1, colorName);

        String[] rents = color.getRentDescription().split("\n");
        box.getChildren().add(newRentGrid(rents));

        return box;
    }

    // Split longer rent tables into two columns
    private static HBox newRentGrid(String[] rents) {
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

        for (int i = 0; i < splitIndex; i++) {
            leftColumn.getChildren().add(newSmallCardText(rents[i]));
        }

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
        PropertyColor leftColor = wildCard.getPossibleColors().get(0);
        PropertyColor rightColor = wildCard.getPossibleColors().get(1);

        Label leftRent = newSmallCardText(leftColor.getRentDescription());
        leftRent.setAlignment(Pos.CENTER_LEFT);
        leftRent.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(leftRent, Priority.ALWAYS);

        Label rightRent = newSmallCardText(rightColor.getRentDescription());
        rightRent.setAlignment(Pos.CENTER_RIGHT);
        rightRent.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(rightRent, Priority.ALWAYS);

        HBox rentBox = new HBox(leftRent, rightRent);
        rentBox.setMaxWidth(86);
        rentBox.setAlignment(Pos.CENTER);
        return rentBox;
    }

    // Add color bar for cards
    private static Region newCardBar(Card card) {
        List<PropertyColor> colors = getCardColors(card);
        if (colors != null) {
            return newColorBar(colors.get(0), colors.get(1), 84, 6, false);
        }

        return newColorBar(cardColor(card), 84, 6, false);
    }

    // Use split bar for two-color cards
    private static List<PropertyColor> getCardColors(Card card) {
        if (card instanceof WildPropertyCard wildCard && wildCard.getPossibleColors().size() == 2) {
            return wildCard.getPossibleColors();
        }

        if (card instanceof ActionCard actionCard && actionCard.getColors().size() == 2) {
            return actionCard.getColors();
        }

        return null;
    }

    // Create horizontal or vertical split color bar
    private static Region newColorBar(PropertyColor firstColor, PropertyColor secondColor,
                                      double width, double height, boolean vertical) {
        Color leftColor = propertyColor(firstColor);
        Color rightColor = propertyColor(secondColor);
        String direction = vertical ? "to bottom" : "to right";

        Region bar = new Region();
        bar.setPrefSize(width, height);

        if (vertical) {
            bar.setMinWidth(width);
        } else {
            bar.setMinHeight(height);
            bar.setMaxHeight(height);
            bar.setMaxWidth(Double.MAX_VALUE);
        }

        bar.setStyle("-fx-background-color: linear-gradient(" + direction + ", "
                + cssColor(leftColor) + " 0%, "
                + cssColor(leftColor) + " 50%, "
                + cssColor(rightColor) + " 50%, "
                + cssColor(rightColor) + " 100%); -fx-background-radius: 12;");
        return bar;
    }

    // Create single-color bar
    private static Region newColorBar(Color color, double width, double height, boolean vertical) {
        Region bar = new Region();
        bar.setPrefSize(width, height);

        if (vertical) {
            bar.setMinWidth(width);
        } else {
            bar.setMinHeight(height);
            bar.setMaxHeight(height);
            bar.setMaxWidth(Double.MAX_VALUE);
        }

        bar.setBackground(solidBackground(color));
        return bar;
    }

    // Short text for card faces and used-card history
    static String cardDetail(Card card) {
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
    static String statusCardText(Card card) {
        return switch (card) {
            case MoneyCard moneyCard -> moneyCard.getName();
            case PropertyCard _ -> "property card";
            case WildPropertyCard _ -> "wild card";
            case ActionCard actionCard -> actionCard.getName();
            case null, default -> "card";
        };
    }

    // Main display color for card border and bar
    static Color cardColor(Card card) {
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
    static Color propertyColor(PropertyColor color) {
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

    // Create a player box for each player
    static VBox newPlayerBox(Player player, boolean isCurrent, double minHeight) {
        Label name = new Label(player.getName());
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        name.setTextFill(Color.rgb(15, 23, 42));

        // Add summary
        HBox summaryBox = new HBox(8, newBadge("Hand " + player.getCardsAtHand().size()));
        summaryBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, name, spacer, summaryBox);
        header.setAlignment(Pos.CENTER_LEFT);

        // Show cards in bank
        VBox bank = newBankSection(player);
        // Add property section
        VBox properties = newPropertiesSection(player);

        Separator sectionLine = new Separator();
        sectionLine.setOrientation(javafx.geometry.Orientation.VERTICAL);
        sectionLine.setPadding(new Insets(0, 2, 0, 2));

        HBox cardArea = new HBox(10, bank, sectionLine, properties);
        cardArea.setAlignment(Pos.TOP_LEFT);
        cardArea.setFillHeight(true);
        cardArea.setMinWidth(0);
        cardArea.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(cardArea, Priority.ALWAYS);

        VBox box = new VBox(8, header, new Separator(), cardArea);
        box.setPadding(new Insets(12));
        box.setMinWidth(0);
        box.setPrefHeight(Region.USE_COMPUTED_SIZE);
        box.setMinHeight(minHeight);
        box.setMaxHeight(Double.MAX_VALUE);
        box.setMaxWidth(Double.MAX_VALUE);

        // Add focus for player
        if (isCurrent) {
            box.setBackground(solidBackground(Color.rgb(240, 253, 244)));
            box.setBorder(roundCorner(Color.rgb(34, 197, 94)));
        } else {
            box.setBackground(solidBackground(Color.WHITE));
            box.setBorder(roundCorner(Color.rgb(203, 213, 225)));
        }

        return box;
    }

    // Create compact bank column
    private static VBox newBankSection(Player player) {
        VBox bank = new VBox(4);
        Label bankTitle = new Label("Bank");
        bankTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        bankTitle.setTextFill(Color.rgb(71, 85, 105));
        HBox bankHeader = new HBox(8, bankTitle, newBadge("Bank " + player.getBankTotalValue() + "M"));
        bankHeader.setAlignment(Pos.CENTER_LEFT);
        bank.getChildren().add(bankHeader);

        if (player.getCardsAtBank().isEmpty()) {
            Label emptyBank = new Label("Bank is empty.");
            emptyBank.setWrapText(true);
            emptyBank.setTextFill(Color.rgb(100, 116, 139));
            bank.getChildren().add(emptyBank);
        } else {
            FlowPane bankCards = new FlowPane(6, 6);
            bankCards.setPrefWrapLength(120);
            for (Card card : player.getCardsAtBank()) {
                bankCards.getChildren().add(newBankBox(card));
            }
            bank.getChildren().add(bankCards);
        }
        bank.setMinWidth(110);
        bank.setPrefWidth(130);
        bank.setMaxWidth(150);
        bank.setFillWidth(true);
        HBox.setHgrow(bank, Priority.NEVER);
        return bank;
    }

    // Create properties column
    private static VBox newPropertiesSection(Player player) {
        VBox properties = new VBox(4);
        Label propertiesTitle = new Label("Properties");
        propertiesTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        propertiesTitle.setTextFill(Color.rgb(71, 85, 105));
        HBox propertiesHeader = new HBox(8, propertiesTitle, newBadge("Sets " + player.countCompletedSets() + "/3"));
        propertiesHeader.setAlignment(Pos.CENTER_LEFT);
        properties.getChildren().add(propertiesHeader);

        boolean hasProperties = false;
        FlowPane propertyGroups = new FlowPane(10, 8);
        propertyGroups.setMaxWidth(Double.MAX_VALUE);
        // Wrap according to properties column width
        propertyGroups.prefWrapLengthProperty().bind(properties.widthProperty());

        for (PropertySet propertySet : player.getPropertySets().values()) {
            if (!propertySet.getCards().isEmpty()) {
                hasProperties = true;
                propertyGroups.getChildren().add(newPropertySetBox(propertySet));
            }
        }

        if (!hasProperties) {
            Label emptyProperties = new Label("No properties in play.");
            emptyProperties.setWrapText(true);
            emptyProperties.setTextFill(Color.rgb(100, 116, 139));
            properties.getChildren().add(emptyProperties);
        } else {
            properties.getChildren().add(propertyGroups);
        }
        properties.setMinWidth(0);
        properties.setPrefWidth(0);
        properties.setMaxWidth(Double.MAX_VALUE);
        properties.setFillWidth(true);
        HBox.setHgrow(properties, Priority.ALWAYS);
        return properties;
    }

    // Create one property set group
    private static VBox newPropertySetBox(PropertySet propertySet) {
        PropertyColor color = propertySet.getColor();

        Label colorName = new Label(color.getName());
        colorName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        colorName.setTextFill(propertyColor(color));
        colorName.setMinWidth(Region.USE_PREF_SIZE);

        Label setStatus = new Label("Set " + propertySet.getCards().size()
                + "/" + color.getSize() + " | " + propertySet.calculateRent() + "M");
        setStatus.setFont(Font.font("Segoe UI", 11));
        setStatus.setTextFill(Color.rgb(100, 116, 139));

        VBox header = new VBox(1, colorName, setStatus);
        header.setAlignment(Pos.CENTER_LEFT);

        FlowPane cards = new FlowPane(6, 6);
        for (Card card : propertySet.getCards()) {
            cards.getChildren().add(newPropertyMiniBox(card, color));
        }

        for (Card card : propertySet.getUpgradeCards()) {
            cards.getChildren().add(newPropertyMiniBox(card, color));
        }

        VBox box = new VBox(4, header, cards);
        box.setPadding(new Insets(4, 0, 0, 0));
        box.setMinWidth(160);
        box.setPrefWidth(185);
        box.setMaxWidth(230);
        return box;
    }

    // Create small card pill
    private static HBox newPropertyMiniBox(Card card, PropertyColor color) {
        Label name = new Label(card.getName());
        name.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        name.setTextFill(Color.rgb(15, 23, 42));
        name.setWrapText(true);
        name.setMaxWidth(120);

        Color barColor = propertyColor(color);
        if (card instanceof ActionCard actionCard
                && (actionCard.getActionType() == ActionType.HOUSE || actionCard.getActionType() == ActionType.HOTEL)) {
            barColor = cardColor(card);
        }

        HBox box = new HBox(8, newColorBar(barColor, 4, 20, true), name);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5, 10, 5, 9));
        box.setBackground(solidBackground(Color.WHITE));
        box.setBorder(roundCorner(Color.rgb(203, 213, 225)));
        return box;
    }

    // Create a small card box for bank
    private static HBox newBankBox(Card card) {
        Label name = new Label(card.getName());
        name.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        name.setTextFill(Color.rgb(15, 23, 42));

        HBox box = new HBox(8, newBankCardBar(card), name);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5, 10, 5, 9));
        box.setMinWidth(62);
        box.setBackground(solidBackground(Color.WHITE));
        box.setBorder(roundCorner(Color.rgb(203, 213, 225)));
        return box;
    }

    // Add color bar for small card in bank
    private static Region newBankCardBar(Card card) {
        if (card instanceof WildPropertyCard wildCard && wildCard.getPossibleColors().size() >= 2) {
            return newColorBar(wildCard.getPossibleColors().get(0), wildCard.getPossibleColors().get(1), 4, 20, true);
        }

        return newColorBar(cardColor(card), 4, 20, true);
    }

    // Gray badge by default
    static Label newBadge(String text) {
        return newBadge(text, Color.rgb(241, 245, 249), Color.rgb(51, 65, 85));
    }

    // Overload to colors
    static Label newBadge(String text, Color background, Color foreground) {
        Label label = new Label(text);
        label.setPadding(new Insets(4, 8, 4, 8));
        label.setBackground(solidBackground(background));
        label.setBorder(roundCorner(background.darker()));
        label.setTextFill(foreground);
        return label;
    }

    // Add a box in table when there is no player/card
    static VBox noCardBox(String titleText, String bodyText) {
        Label title = new Label(titleText);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        Label body = new Label(bodyText);
        body.setWrapText(true);
        body.setTextFill(Color.rgb(100, 116, 139));
        body.setMaxWidth(Double.MAX_VALUE);
        body.setAlignment(Pos.CENTER);
        body.setTextAlignment(TextAlignment.CENTER);

        VBox box = new VBox(8, title, body);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(18));
        box.setPrefSize(420, 120);
        box.setMinSize(420, 120);
        box.setMaxSize(420, 120);
        box.setBackground(solidBackground(Color.WHITE));
        box.setBorder(roundCorner(Color.rgb(203, 213, 225)));
        return box;
    }

    static StackPane centeredEmptyBox(ScrollPane scrollPane, String titleText, String bodyText) {
        StackPane wrapper = new StackPane(noCardBox(titleText, bodyText));
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMinHeight(140);
        wrapper.prefWidthProperty().bind(scrollPane.widthProperty().subtract(32));
        return wrapper;
    }

    // Apply unified style for all buttons
    static void setActionButton(Button button, Color color, boolean isFilled) {
        button.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        button.setPadding(new Insets(10, 16, 10, 16));
        button.setMinHeight(40);
        button.setOpacity(1);
        button.disabledProperty().addListener((_, _, _) -> applyActionButtonStyle(button, color, isFilled));
        applyActionButtonStyle(button, color, isFilled);
    }

    private static void applyActionButtonStyle(Button button, Color color, boolean isFilled) {
        if (button.isDisabled()) {
            button.setTextFill(Color.rgb(100, 116, 139));
            if (isFilled) {
                button.setBackground(solidBackground(Color.rgb(226, 232, 240)));
            } else {
                button.setBackground(solidBackground(Color.WHITE));
            }
            button.setBorder(roundCorner(Color.rgb(203, 213, 225)));
            return;
        }

        if (isFilled) {
            button.setTextFill(Color.WHITE);
            button.setBackground(solidBackground(color));
            button.setBorder(roundCorner(color.darker()));
        } else {
            button.setTextFill(color.darker());
            button.setBackground(solidBackground(Color.WHITE));
            button.setBorder(roundCorner(color));
        }
    }

    static Background solidBackground(Color color) {
        return new Background(new BackgroundFill(color, new CornerRadii(12), Insets.EMPTY));
    }

    static Border roundCorner(Color color) {
        return new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1)));
    }
}
