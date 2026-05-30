package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public final class PlayerUI {
    private PlayerUI() {
    }

    // Create a player box for each player
    public static VBox newPlayerBox(Player player, boolean isCurrent, boolean isWinner, double minHeight) {
        Label name = new Label(player.getName());
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        name.setTextFill(Color.rgb(15, 23, 42));

        HBox titleBox = new HBox(8, name);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        if (isWinner) {
            titleBox.getChildren().add(newWinnerBadge());
        }

        // Add summary
        HBox summaryBox = new HBox(8, GameUI.newBadge("Hand " + player.getCardsAtHand().size()));
        summaryBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, titleBox, spacer, summaryBox);
        header.setAlignment(Pos.CENTER_LEFT);

        // Show cards in bank
        VBox bank = newBankSection(player);
        // Add property section
        VBox properties = newPropertiesSection(player);

        // Draw divider between bank and properties
        Separator sectionLine = new Separator();
        sectionLine.setOrientation(javafx.geometry.Orientation.VERTICAL);
        sectionLine.setPadding(new Insets(0, 2, 0, 2));

        // Pack bank and properties side by side
        HBox cardArea = new HBox(10, bank, sectionLine, properties);
        cardArea.setAlignment(Pos.TOP_LEFT);
        cardArea.setFillHeight(true);
        cardArea.setMinWidth(0);
        cardArea.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(cardArea, Priority.ALWAYS);

        // Pack player header and table area
        VBox box = new VBox(8, header, new Separator(), cardArea);
        box.setPadding(new Insets(12));
        box.setMinWidth(0);
        box.setPrefHeight(Region.USE_COMPUTED_SIZE);
        box.setMinHeight(minHeight);
        box.setMaxHeight(Double.MAX_VALUE);
        box.setMaxWidth(Double.MAX_VALUE);

        // Add winner focus after the game ends
        if (isWinner) {
            box.setBackground(GameUI.solidBackground(Color.rgb(240, 253, 244)));
            box.setBorder(GameUI.roundCorner(Color.rgb(34, 197, 94)));
        } else if (isCurrent) {
            box.setBackground(GameUI.solidBackground(Color.rgb(239, 246, 255)));
            box.setBorder(GameUI.roundCorner(Color.rgb(37, 99, 235)));
        } else {
            box.setBackground(GameUI.solidBackground(Color.WHITE));
            box.setBorder(GameUI.roundCorner(Color.rgb(203, 213, 225)));
        }

        return box;
    }

    private static Label newWinnerBadge() {
        Label badge = GameUI.newBadge("Winner", Color.rgb(220, 252, 231), Color.rgb(22, 101, 52));
        badge.setBorder(GameUI.roundCorner(Color.rgb(134, 239, 172)));
        badge.setFont(Font.font("Segoe UI Semibold", 12));
        return badge;
    }

    // Create compact bank column
    private static VBox newBankSection(Player player) {
        // Set bank title and total value
        VBox bank = new VBox(4);
        Label bankTitle = new Label("Bank");
        bankTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        bankTitle.setTextFill(Color.rgb(71, 85, 105));
        HBox bankHeader = new HBox(8, bankTitle, GameUI.newBadge("Bank " + player.getBankTotalValue() + "M"));
        bankHeader.setAlignment(Pos.CENTER_LEFT);
        bank.getChildren().add(bankHeader);

        // Show empty text when no bank cards exist
        if (player.getCardsAtBank().isEmpty()) {
            Label emptyBank = new Label("Bank is empty.");
            emptyBank.setWrapText(true);
            emptyBank.setTextFill(Color.rgb(100, 116, 139));
            bank.getChildren().add(emptyBank);
        } else {
            // Show cards in bank
            FlowPane bankCards = new FlowPane(6, 6);
            bankCards.setPrefWrapLength(120);
            for (Card card : player.getCardsAtBank()) {
                bankCards.getChildren().add(newBankBox(card));
            }
            bank.getChildren().add(bankCards);
        }

        // Keep bank narrower than properties
        bank.setMinWidth(110);
        bank.setPrefWidth(130);
        bank.setMaxWidth(150);
        bank.setFillWidth(true);
        HBox.setHgrow(bank, Priority.NEVER);
        return bank;
    }

    // Create properties column
    private static VBox newPropertiesSection(Player player) {
        // Set properties title and completed set count
        VBox properties = new VBox(4);
        Label propertiesTitle = new Label("Properties");
        propertiesTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        propertiesTitle.setTextFill(Color.rgb(71, 85, 105));
        HBox propertiesHeader = new HBox(8, propertiesTitle, newSetsBadge(player.countCompletedSets()));
        propertiesHeader.setAlignment(Pos.CENTER_LEFT);
        properties.getChildren().add(propertiesHeader);

        boolean hasProperties = false;
        FlowPane propertyGroups = new FlowPane(10, 8);
        propertyGroups.setMaxWidth(Double.MAX_VALUE);

        // Wrap property groups based on available column width
        propertyGroups.prefWrapLengthProperty().bind(properties.widthProperty());

        // Add property section
        for (PropertySet propertySet : player.getPropertySets().values()) {
            if (!propertySet.getCards().isEmpty()) {
                hasProperties = true;
                propertyGroups.getChildren().add(newPropertySetBox(propertySet));
            }
        }

        // Show empty text when no properties exist
        if (!hasProperties) {
            Label emptyProperties = new Label("No properties in play.");
            emptyProperties.setWrapText(true);
            emptyProperties.setTextFill(Color.rgb(100, 116, 139));
            properties.getChildren().add(emptyProperties);
        } else {
            properties.getChildren().add(propertyGroups);
        }

        // Let properties take all remaining space
        properties.setMinWidth(0);
        properties.setPrefWidth(0);
        properties.setMaxWidth(Double.MAX_VALUE);
        properties.setFillWidth(true);
        HBox.setHgrow(properties, Priority.ALWAYS);
        return properties;
    }

    private static Label newSetsBadge(int completedSets) {
        Label badge;
        if (completedSets >= 3) {
            badge = GameUI.newBadge("Sets " + completedSets + "/3", Color.rgb(220, 252, 231), Color.rgb(22, 101, 52));
            badge.setBorder(GameUI.roundCorner(Color.rgb(134, 239, 172)));
        } else if (completedSets == 2) {
            badge = GameUI.newBadge("Sets " + completedSets + "/3", Color.rgb(219, 234, 254), Color.rgb(29, 78, 216));
            badge.setBorder(GameUI.roundCorner(Color.rgb(147, 197, 253)));
        } else {
            badge = GameUI.newBadge("Sets " + completedSets + "/3");
        }

        return badge;
    }

    // Create one property set group
    private static VBox newPropertySetBox(PropertySet propertySet) {
        PropertyColor color = propertySet.getColor();

        // Set color title
        Label colorName = new Label(color.getName());
        colorName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        colorName.setTextFill(CardColorUI.propertyColor(color));
        colorName.setMinWidth(Region.USE_PREF_SIZE);

        // Show set progress and current rent
        Label setStatus = new Label("Set " + propertySet.getCards().size()
                + "/" + color.getSize() + " | " + propertySet.calculateRent() + "M");
        setStatus.setFont(Font.font("Segoe UI", 11));
        setStatus.setTextFill(Color.rgb(100, 116, 139));

        VBox header = new VBox(1, colorName, setStatus);
        header.setAlignment(Pos.CENTER_LEFT);

        // Add normal property cards first
        FlowPane cards = new FlowPane(6, 6);
        for (Card card : propertySet.getCards()) {
            cards.getChildren().add(newPropertyMiniBox(card, color));
        }

        // Add house and hotel cards after properties
        for (Card card : propertySet.getUpgradeCards()) {
            cards.getChildren().add(newPropertyMiniBox(card, color));
        }

        // Pack one color group
        VBox box = new VBox(4, header, cards);
        box.setPadding(new Insets(4, 0, 0, 0));
        box.setMinWidth(160);
        box.setPrefWidth(185);
        box.setMaxWidth(230);
        return box;
    }

    // Create small card pill
    private static HBox newPropertyMiniBox(Card card, PropertyColor color) {
        // Set card name
        Label name = new Label(card.getName());
        name.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        name.setTextFill(Color.rgb(15, 23, 42));
        name.setWrapText(true);
        name.setMaxWidth(120);

        Color barColor = CardColorUI.propertyColor(color);
        // Upgrade cards use action-card color instead of property color
        if (card instanceof ActionCard actionCard
                && (actionCard.getActionType() == ActionType.HOUSE || actionCard.getActionType() == ActionType.HOTEL)) {
            barColor = CardColorUI.cardColor(card);
        }

        // Pack color bar and card name
        HBox box = new HBox(8, CardColorUI.newColorBar(barColor, 4, 20, true), name);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5, 10, 5, 9));
        box.setBackground(GameUI.solidBackground(Color.WHITE));
        box.setBorder(GameUI.roundCorner(Color.rgb(203, 213, 225)));
        return box;
    }

    // Create a small card box for bank
    private static HBox newBankBox(Card card) {
        // Set card name
        Label name = new Label(bankCardText(card));
        name.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        name.setTextFill(Color.rgb(15, 23, 42));

        // Pack all elements
        HBox box = new HBox(8, newBankCardBar(card), name);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5, 10, 5, 9));
        box.setMinWidth(62);
        box.setBackground(GameUI.solidBackground(Color.WHITE));
        box.setBorder(GameUI.roundCorner(Color.rgb(203, 213, 225)));
        return box;
    }

    private static String bankCardText(Card card) {
        return card.getBankValue() + "M";
    }

    // Add color bar for small card in bank
    private static Region newBankCardBar(Card card) {
        // Banked wild cards still show both possible colors
        if (card instanceof WildPropertyCard wildCard && wildCard.getPossibleColors().size() >= 2) {
            return CardColorUI.newColorBar(wildCard.getPossibleColors().get(0), wildCard.getPossibleColors().get(1), 4, 20, true);
        }

        // Other bank cards use their main display color
        return CardColorUI.newColorBar(CardColorUI.cardColor(card), 4, 20, true);
    }
}
