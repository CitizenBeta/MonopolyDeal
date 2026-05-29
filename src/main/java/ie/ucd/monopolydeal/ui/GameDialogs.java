package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.game.DecisionMaker;
import ie.ucd.monopolydeal.model.*;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

final class GameDialogs implements DecisionMaker {
    // Get action-card legality check from Game
    private Predicate<ActionCard> actionPlayChecker = action -> true;

    void setActionPlayChecker(Predicate<ActionCard> actionPlayChecker) {
        if (actionPlayChecker == null) {
            this.actionPlayChecker = action -> true;
            return;
        }

        this.actionPlayChecker = actionPlayChecker;
    }
    // Pop a dialog to ask players' name
    List<String> askPlayerNames() {
        // Ask how many players will join
        ChoiceDialog<Integer> playerNumDialog = new ChoiceDialog<>(2,
                FXCollections.observableArrayList(IntStream.rangeClosed(2, 5).boxed().toList()));
        playerNumDialog.setHeaderText("How many players?");
        Integer count = playerNumDialog.showAndWait().orElse(null);

        if (count == null) {
            return null;
        }

        List<String> names = new ArrayList<>();
        // Ask each player's name one by one
        for (int i = 1; i <= count; i++) {
            TextInputDialog playerNameDialog = new TextInputDialog("Player " + i);
            playerNameDialog.setHeaderText("Enter name for player " + i);
            playerNameDialog.setContentText("Name:");

            String result = playerNameDialog.showAndWait().orElse(null);
            if (result == null) {
                return null;
            }

            String name = result.trim();
            // Use default name if input is blank
            if (name.isEmpty()) {
                names.add("Player " + i);
            } else {
                names.add(name);
            }
        }

        return names;
    }
    @Override
    public Player selectNextPlayer(Player currentPlayer, List<Player> players, String prompt) {
        return DialogChoices.chooseOption("Choose Player", prompt, players, Player::getName);
    }
    @Override
    public PropertyColor selectColor(String prompt, List<PropertyColor> players) {
        return DialogChoices.chooseOption("Choose Color", prompt, players, PropertyColor::getName);
    }

    @Override
    public UseMode useCard(ActionCard action) {
        List<UseMode> modes = new ArrayList<>();
        // Build available use modes from action legality
        // Hide Play when action effect cannot resolve
        if (action.getActionType() != ActionType.JUST_SAY_NO && actionPlayChecker.test(action)) {
            modes.add(UseMode.PLAY);
        }
        modes.add(UseMode.BANK);

        // Do not auto-bank when Bank is the only option
        return DialogChoices.chooseOption("Use Card", "How do you want to use " + action.getName() + "?", modes, mode -> {
            if (mode == UseMode.PLAY) {
                return "Play card";
            }
            return "Bank for " + action.getBankValue() + "M";
        }, false);
    }

    @Override
    public WildPropertyCard selectWildCardToMove(Player current, List<WildPropertyCard> wildCards) {
        return DialogChoices.chooseOption("Move Wild", "Choose a wild card to move for " + current.getName(), wildCards, card -> {
            if (card.getCurrentColor() == null) {
                return card.getName() + " - Not selected";
            }
            return card.getName() + " - " + card.getCurrentColor().getName();
        });
    }

    @Override
    public List<Card> selectDiscards(Player current, List<Card> cards, int count) {
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
    private List<Card> selectSingleDiscard(Dialog<List<Card>> dialog, Button okButton,
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
    private List<Card> selectMultipleDiscards(Dialog<List<Card>> dialog, Button okButton,
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
    private void updateDiscardChecks(List<CheckBox> checkBoxes, Button okButton, int count) {
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

    private String discardOptionText(Card card) {
        return card.getName();
    }

    @Override
    public Card selectPropertyCard(Player owner, List<Card> cards, String prompt) {
        return DialogChoices.chooseOption("Choose Card", prompt, cards, card -> cardOptionText(owner, card));
    }

    // Enable OK once selected payment reaches required value
    @Override
    public List<Card> selectPaymentCards(Player owner, List<Card> cards, int amount) {
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

    @Override
    public boolean reconfirm(String prompt) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(prompt);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private String cardOptionText(Player owner, Card card) {
        // Show current property color when the card is on table
        if (owner != null) {
            PropertyColor color = owner.getPropertyColor(card);
            if (color != null) {
                return card.getName() + " - " + color.getName();
            }
        }
        return card.getName();
    }

    private String paymentOptionText(Player owner, Card card) {
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
    private void updatePaymentChecks(List<Card> cards, List<CheckBox> checkBoxes, Button okButton,
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

    private String paymentPrompt(Player owner, int amount, int selectedTotal) {
        return owner.getName() + " must pay " + amount + "M. Selected " + selectedTotal + "M.";
    }
}
