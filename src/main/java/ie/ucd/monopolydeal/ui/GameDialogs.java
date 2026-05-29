package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.game.DecisionMaker;
import ie.ucd.monopolydeal.model.*;
import javafx.collections.FXCollections;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public final class GameDialogs implements DecisionMaker {
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
        return DiscardDialog.selectDiscards(current, cards, count);
    }

    @Override
    public Card selectPropertyCard(Player owner, List<Card> cards, String prompt) {
        return DialogChoices.chooseOption("Choose Card", prompt, cards, card -> cardOptionText(owner, card));
    }

    @Override
    public List<Card> selectPaymentCards(Player owner, List<Card> cards, int amount) {
        return PaymentDialog.selectPaymentCards(owner, cards, amount);
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
}
