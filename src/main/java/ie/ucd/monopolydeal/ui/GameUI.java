package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.game.CardHistory;
import ie.ucd.monopolydeal.model.Card;
import ie.ucd.monopolydeal.model.Player;
import ie.ucd.monopolydeal.model.PropertyColor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.function.Consumer;

public final class GameUI {
    private GameUI() {
    }

    public static HBox newUsedCardBox(CardHistory.UsedCard usedCard) {
        return CardUI.newUsedCardBox(usedCard);
    }

    public static VBox newHandCard(Card card, boolean selected, Consumer<Card> onCardClicked) {
        return CardUI.newHandCard(card, selected, onCardClicked);
    }

    public static String cardDetail(Card card) {
        return CardUI.cardDetail(card);
    }

    public static String statusCardText(Card card) {
        return CardUI.statusCardText(card);
    }

    public static Color cardColor(Card card) {
        return CardColorUI.cardColor(card);
    }

    public static Color propertyColor(PropertyColor color) {
        return CardColorUI.propertyColor(color);
    }

    public static VBox newPlayerBox(Player player, boolean isCurrent, boolean isWinner, double minHeight) {
        return PlayerUI.newPlayerBox(player, isCurrent, isWinner, minHeight);
    }

    // Gray badge by default
    public static Label newBadge(String text) {
        return newBadge(text, Color.rgb(241, 245, 249), Color.rgb(51, 65, 85));
    }

    // Overload to colors
    public static Label newBadge(String text, Color background, Color foreground) {
        // Set badge text
        Label label = new Label(text);

        // Apply pill spacing and colors
        label.setPadding(new Insets(4, 8, 4, 8));
        label.setBackground(solidBackground(background));
        label.setBorder(roundCorner(background.darker()));
        label.setTextFill(foreground);
        return label;
    }

    // Add a box in table when there is no player/card
    public static VBox noCardBox(String titleText, String bodyText) {
        // Set empty-state title
        Label title = new Label(titleText);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        // Set empty-state body
        Label body = new Label(bodyText);
        body.setWrapText(true);
        body.setTextFill(Color.rgb(100, 116, 139));
        body.setMaxWidth(Double.MAX_VALUE);
        body.setAlignment(Pos.CENTER);
        body.setTextAlignment(TextAlignment.CENTER);

        // Pack title and body into a fixed placeholder card
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

    public static StackPane centeredEmptyBox(ScrollPane scrollPane, String titleText, String bodyText) {
        // Wrap the empty card so it stays centered in the scroll area
        StackPane wrapper = new StackPane(noCardBox(titleText, bodyText));
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMinHeight(140);

        // Follow scroll area width without touching the card size
        wrapper.prefWidthProperty().bind(scrollPane.widthProperty().subtract(32));
        return wrapper;
    }

    // Apply unified style for all buttons
    public static void setActionButton(Button button, Color color, boolean isFilled) {
        // Set stable button size and text style
        button.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        button.setPadding(new Insets(10, 16, 10, 16));
        button.setMinHeight(40);
        button.setOpacity(1);

        // Reapply style whenever disabled state changes
        button.disabledProperty().addListener((_, _, _) -> applyActionButtonStyle(button, color, isFilled));
        applyActionButtonStyle(button, color, isFilled);
    }

    private static void applyActionButtonStyle(Button button, Color color, boolean isFilled) {
        // Disabled buttons keep shape but use muted colors
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

        // Filled buttons use the main action color
        if (isFilled) {
            button.setTextFill(Color.WHITE);
            button.setBackground(solidBackground(color));
            button.setBorder(roundCorner(color.darker()));
        } else {
            // Outline buttons keep a white background
            button.setTextFill(color.darker());
            button.setBackground(solidBackground(Color.WHITE));
            button.setBorder(roundCorner(color));
        }
    }

    public static Background solidBackground(Color color) {
        return new Background(new BackgroundFill(color, new CornerRadii(12), Insets.EMPTY));
    }

    public static Border roundCorner(Color color) {
        return new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1)));
    }
}
