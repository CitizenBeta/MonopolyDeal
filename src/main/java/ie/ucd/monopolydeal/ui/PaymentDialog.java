package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.model.Card;
import ie.ucd.monopolydeal.model.Player;
import ie.ucd.monopolydeal.model.PropertyColor;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

// Dialog used when a player must choose cards for payment
final class PaymentDialog {
    private PaymentDialog() {
    }

    static List<Card> selectPaymentCards(Player owner, List<Card> cards, int amount) {
        if (cards.isEmpty()) {
            return null;
        }

        Dialog<List<Card>> dialog = new Dialog<>();
        dialog.setTitle("Choose Payment");
        dialog.setHeaderText(paymentPrompt(owner, amount, 0));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        // Create checkbox payment options
        VBox optionsBox = new VBox(8);
        optionsBox.setPadding(new Insets(8));

        List<CheckBox> checkBoxes = new ArrayList<>();
        for (Card card : cards) {
            CheckBox checkBox = new CheckBox(paymentOptionText(owner, card));
            checkBox.setWrapText(true);
            checkBox.setMaxWidth(Double.MAX_VALUE);
            checkBox.selectedProperty().addListener((_, _, _) ->
                    updatePaymentChecks(cards, checkBoxes, okButton, dialog, owner, amount));
            checkBoxes.add(checkBox);
            optionsBox.getChildren().add(checkBox);
        }

        // Keep payment list scrollable
        ScrollPane scrollPane = new ScrollPane(optionsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefSize(420, 280);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                // Return selected payment cards
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

    private static String paymentOptionText(Player owner, Card card) {
        // Find whether payment card is from bank or table
        String location;
        if (owner != null && owner.getCardsAtBank().contains(card)) {
            location = "Bank";
        } else {
            PropertyColor color = null;
            if (owner != null) {
                color = owner.getPropertyColor(card);
            }
            if (color == null) {
                location = "Table";
            } else {
                location = color.getName();
            }
        }

        // Include value because Monopoly Deal has no change
        return card.getName() + " - " + location + " - " + card.getBankValue() + "M";
    }

    // Disable extra payment choices after enough value is selected
    private static void updatePaymentChecks(List<Card> cards, List<CheckBox> checkBoxes, Button okButton,
                                            Dialog<List<Card>> dialog, Player owner, int amount) {
        // Sum selected payment value
        int selectedTotal = 0;
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                selectedTotal += cards.get(i).getBankValue();
            }
        }

        okButton.setDisable(selectedTotal < amount);
        dialog.setHeaderText(paymentPrompt(owner, amount, selectedTotal));

        // Prevent selecting extra cards once payment is enough
        boolean enoughSelected = selectedTotal >= amount;
        for (CheckBox checkBox : checkBoxes) {
            checkBox.setDisable(enoughSelected && !checkBox.isSelected());
        }
    }

    private static String paymentPrompt(Player owner, int amount, int selectedTotal) {
        return owner.getName() + " must pay " + amount + "M. Selected " + selectedTotal + "M.";
    }
}
