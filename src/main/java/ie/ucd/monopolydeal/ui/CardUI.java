package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.game.CardHistory;
import ie.ucd.monopolydeal.model.Card;
import ie.ucd.monopolydeal.model.PropertyColor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.function.Consumer;

public final class CardUI {
    private CardUI() {
    }

    // Create a box for a used card
    public static HBox newUsedCardBox(CardHistory.UsedCard usedCard) {
        Card card = usedCard.card();

        // Set card name
        Label name = new Label(card.getName().replace("/", "/\u200B"));
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        name.setTextFill(Color.rgb(15, 23, 42));

        // Set card detail
        String detailText = CardTextUI.cardDetail(card);
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
        if (usedCard.action() == CardHistory.CardAction.DISCARDED) {
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

    // Add a card row in the player's hand table
    public static VBox newHandCard(Card card, boolean selected, Consumer<Card> onCardClicked, Consumer<Card> onCardDoubleClicked) {
        return HandCardUI.newHandCard(card, selected, onCardClicked, onCardDoubleClicked);
    }

    // Short text for card faces and used-card history
    public static String cardDetail(Card card) {
        return CardTextUI.cardDetail(card);
    }

    // Short status text for top status card
    public static String statusCardText(Card card) {
        return CardTextUI.statusCardText(card);
    }

    private static Region newUsedCardBar(Card card) {
        // Use split color bar when card has two colors
        List<PropertyColor> colors = CardColorUI.getCardColors(card);
        if (colors != null) {
            return CardColorUI.newColorBar(colors.get(0), colors.get(1), 6, 44, true);
        }

        return CardColorUI.newColorBar(CardColorUI.cardColor(card), 6, 44, true);
    }
}
