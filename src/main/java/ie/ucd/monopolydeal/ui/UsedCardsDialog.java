package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.game.CardHistory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

// Dialog for showing used and discarded cards
public final class UsedCardsDialog {
    private UsedCardsDialog() {
    }

    static void show(List<CardHistory.UsedCard> usedCards) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Used Cards");
        alert.setHeaderText("Used cards in this game. Newest first");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefSize(520, 420);

        if (usedCards.isEmpty()) {
            // Create an empty used cards box
            StackPane box = new StackPane(GameUI.noCardBox("No cards yet", "No cards have been used or discarded yet"));
            box.setAlignment(Pos.CENTER);
            box.setPrefHeight(360);
            scrollPane.setContent(box);
        } else {
            VBox cardsBox = new VBox(10);
            cardsBox.setPadding(new Insets(10));
            cardsBox.setFillWidth(true);

            for (CardHistory.UsedCard usedCard : usedCards) {
                cardsBox.getChildren().add(GameUI.newUsedCardBox(usedCard));
            }

            scrollPane.setContent(cardsBox);
        }
        alert.getDialogPane().setContent(scrollPane);
        alert.showAndWait();
    }
}
