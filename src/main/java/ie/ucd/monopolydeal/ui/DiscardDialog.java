package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.model.Card;
import ie.ucd.monopolydeal.model.Player;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

// Dialog used when a player must discard down to seven cards
final class DiscardDialog {
    private DiscardDialog() {
    }

    static List<Card> selectDiscards(Player current, List<Card> cards, int count) {
        if (cards.isEmpty()) {
            return null;
        }

        Dialog<List<Card>> dialog = new Dialog<>();
        dialog.setTitle("Discard Cards");
        dialog.setHeaderText(current.getName() + " has more than 7 cards. Choose " + count + " to discard.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        // Shared option container for radio or checkbox UI
        VBox optionsBox = new VBox(8);
        optionsBox.setPadding(new Insets(8));

        // Use radio buttons for exactly one discard
        if (count == 1) {
            return selectSingleDiscard(dialog, okButton, optionsBox, cards);
        }

        // Use check boxes when multiple cards must be discarded
        return selectMultipleDiscards(dialog, okButton, optionsBox, cards, count);
    }

    // Use radio buttons when discarding one card
    private static List<Card> selectSingleDiscard(Dialog<List<Card>> dialog, Button okButton,
                                                  VBox optionsBox, List<Card> cards) {
        // Build one radio button per discardable card
        ToggleGroup group = new ToggleGroup();
        List<RadioButton> radioButtons = new ArrayList<>();
        for (Card card : cards) {
            RadioButton radioButton = new RadioButton(discardOptionText(card));
            radioButton.setToggleGroup(group);
            radioButton.setWrapText(true);
            radioButton.setMaxWidth(Double.MAX_VALUE);
            radioButtons.add(radioButton);
            optionsBox.getChildren().add(radioButton);
        }

        okButton.disableProperty().bind(group.selectedToggleProperty().isNull());

        // Put options into a scroll area
        ScrollPane scrollPane = new ScrollPane(optionsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefSize(420, 280);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                // Return the chosen single card
                Toggle selectedToggle = group.getSelectedToggle();
                for (int i = 0; i < radioButtons.size(); i++) {
                    if (radioButtons.get(i) == selectedToggle) {
                        return List.of(cards.get(i));
                    }
                }
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    // Use check boxes when discarding multiple cards
    private static List<Card> selectMultipleDiscards(Dialog<List<Card>> dialog, Button okButton,
                                                     VBox optionsBox, List<Card> cards, int count) {
        // Build one checkbox per discardable card
        List<CheckBox> checkBoxes = new ArrayList<>();
        for (Card card : cards) {
            CheckBox checkBox = new CheckBox(discardOptionText(card));
            checkBox.setWrapText(true);
            checkBox.setMaxWidth(Double.MAX_VALUE);
            checkBox.selectedProperty().addListener((_, _, _) -> updateDiscardChecks(checkBoxes, okButton, count));
            checkBoxes.add(checkBox);
            optionsBox.getChildren().add(checkBox);
        }

        // Put options into a scroll area
        ScrollPane scrollPane = new ScrollPane(optionsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefSize(420, 280);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                // Return all selected discard cards
                List<Card> selectedCards = new ArrayList<>();
                for (int i = 0; i < checkBoxes.size(); i++) {
                    if (checkBoxes.get(i).isSelected()) {
                        selectedCards.add(cards.get(i));
                    }
                }
                return selectedCards;
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    // Enable OK only at the exact discard count
    private static void updateDiscardChecks(List<CheckBox> checkBoxes, Button okButton, int count) {
        // Count selected discard choices
        int selectedCount = 0;
        for (CheckBox checkBox : checkBoxes) {
            if (checkBox.isSelected()) {
                selectedCount++;
            }
        }

        okButton.setDisable(selectedCount != count);

        // Lock extra choices after reaching the required number
        boolean limitReached = selectedCount >= count;
        for (CheckBox checkBox : checkBoxes) {
            checkBox.setDisable(limitReached && !checkBox.isSelected());
        }
    }

    private static String discardOptionText(Card card) {
        return card.getName();
    }
}
