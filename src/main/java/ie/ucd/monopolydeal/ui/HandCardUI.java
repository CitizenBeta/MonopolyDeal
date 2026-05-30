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
import javafx.scene.text.TextAlignment;

import java.util.List;
import java.util.function.Consumer;

// Builds the compact cards shown in the current player's hand
final class HandCardUI {
    private HandCardUI() {
    }

    static VBox newHandCard(Card card, boolean selected, Consumer<Card> onCardClicked) {
        // Setup card name
        Label name = new Label(CardTextUI.cardTitle(card));
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
                String detailText = CardTextUI.cardDetail(card);
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
            box.setBorder(GameUI.roundCorner(CardColorUI.cardColor(card)));
        }

        // If clicked again, remove focus
        box.setOnMouseClicked(e -> {
            onCardClicked.accept(card);
            e.consume();
        });

        return box;
    }

    // Show property color and rent table
    private static VBox newPropertyDetailBox(PropertyColor color) {
        // Show color name first
        Label colorName = CardTextUI.newSmallCardText(color.getName());
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
                singleColumn.getChildren().add(CardTextUI.newSmallCardText(rent));
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
            leftColumn.getChildren().add(CardTextUI.newSmallCardText(rents[i]));
        }

        // Align later rent rows to the right side
        for (int i = splitIndex; i < rents.length; i++) {
            Label rent = CardTextUI.newSmallCardText(rents[i]);
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

    // Show wild card rents on two sides
    private static HBox newWildRentBox(WildPropertyCard wildCard) {
        // Read both possible colors
        PropertyColor leftColor = wildCard.getPossibleColors().get(0);
        PropertyColor rightColor = wildCard.getPossibleColors().get(1);

        // Left color rent stays left aligned
        Label leftRent = CardTextUI.newSmallCardText(leftColor.getRentDescription());
        leftRent.setAlignment(Pos.CENTER_LEFT);
        leftRent.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(leftRent, Priority.ALWAYS);

        // Right color rent stays right aligned
        Label rightRent = CardTextUI.newSmallCardText(rightColor.getRentDescription());
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
        List<PropertyColor> colors = CardColorUI.getCardColors(card);
        if (colors != null) {
            return CardColorUI.newColorBar(colors.get(0), colors.get(1), 84, 6, false);
        }

        return CardColorUI.newColorBar(CardColorUI.cardColor(card), 84, 6, false);
    }
}
