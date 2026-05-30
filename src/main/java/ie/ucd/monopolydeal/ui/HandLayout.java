package ie.ucd.monopolydeal.ui;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

// Handles the horizontal hand-card strip layout
public final class HandLayout {
    private HandLayout() {
    }

    static void configure(HBox handCardsBox, ScrollPane cardsScroll,
                          BorderPane rootPane, Runnable clearSelection) {
        // Configure horizontal hand card strip
        handCardsBox.setFillHeight(false);
        handCardsBox.setAlignment(Pos.CENTER);
        cardsScroll.setFitToHeight(true);
        cardsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        cardsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cardsScroll.setPannable(false);

        // Keep hand strip as tall as viewport
        cardsScroll.viewportBoundsProperty().addListener((observable, oldBounds, newBounds) -> resize(handCardsBox, cardsScroll));
        cardsScroll.setVvalue(0);
        cardsScroll.vvalueProperty().addListener((observable, oldValue, newValue) -> cardsScroll.setVvalue(0));

        // Use mouse wheel to scroll hand horizontally
        cardsScroll.setOnScroll(e -> {
            if (e.getDeltaY() != 0 && handCardsBox.getWidth() > 0) {
                double nextValue = cardsScroll.getHvalue() - e.getDeltaY() / handCardsBox.getWidth();
                cardsScroll.setHvalue(Math.clamp(nextValue, 0, 1));
                e.consume();
            }
        });

        cardsScroll.setBackground(GameUI.solidBackground(Color.WHITE));
        cardsScroll.setBorder(Border.EMPTY);
        cardsScroll.setFocusTraversable(false);
        cardsScroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        cardsScroll.setOnMousePressed(e -> rootPane.requestFocus());

        // Clear selected card when clicking empty hand space
        cardsScroll.setOnMouseClicked(e -> {
            if (e.getTarget() == cardsScroll || e.getTarget() == handCardsBox) {
                clearSelection.run();
            }
        });
    }

    // Keep hand content at least as large as its viewport
    static void resize(HBox handCardsBox, ScrollPane cardsScroll) {
        Bounds bounds = cardsScroll.getViewportBounds();
        handCardsBox.setMinWidth(bounds.getWidth());
        handCardsBox.setMinHeight(bounds.getHeight());
        handCardsBox.setPrefHeight(bounds.getHeight());
    }
}
