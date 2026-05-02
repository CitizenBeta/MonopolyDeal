package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.game.DecisionMaker;
import ie.ucd.monopolydeal.game.Game;
import ie.ucd.monopolydeal.model.*;
import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class GameController implements DecisionMaker {
    private final Game game = new Game();

    @FXML private Label statusTitle;
    @FXML private Label statusText;
    @FXML private Label statusState;
    @FXML private Label currentPlayer;
    @FXML private Label actions;
    @FXML private Label handCount;
    @FXML private Label bankTotal;
    @FXML private Label completedSets;
    @FXML private Label piles;
    @FXML private Button newGameButton;
    @FXML private Button playButton;
    @FXML private Button moveWildButton;
    @FXML private Button endTurnButton;

    @FXML
    private void initialize() {
        refreshView();
    }

    private void refreshView() {
        if (!game.isStarted()) {
            showPreGameState();
            return;
        }

        showGameState();
    }

    private void showPreGameState() {
        statusTitle.setText("Status");
        statusText.setText("Start a new game.");
        statusState.setText("Ready");
        currentPlayer.setText("-");
        actions.setText("0 / 3");
        handCount.setText("0");
        bankTotal.setText("0M");
        completedSets.setText("0 / 3");
        piles.setText("0 / 0");
    }

    private void showGameState() {
        Player current = game.getCurrentPlayer();

        statusTitle.setText("Status");
        statusState.setText("Turn Active");
        currentPlayer.setText(current.getName());
        actions.setText(game.getActionsUsed() + " / " + Player.MAX_ACTIONS_PER_TURN);
        handCount.setText(String.valueOf(current.getCardsAtHand().size()));
        bankTotal.setText(game.getCurrentPlayerBankTotal() + "M");
        completedSets.setText("0 / 3");
        piles.setText("0 / 0");
    }

    @FXML
    private void onNewGame() {
        List<String> names = askPlayerNames();
        if (names == null || names.size() < 2) {
            return;
        }

        game.setup(names);
        statusText.setText("Game started.");
        refreshView();
    }

    @FXML
    private void onPlaySelected() {
    }

    @FXML
    private void onMoveWild() {
    }

    @FXML
    private void onEndTurn() {
    }

    private List<String> askPlayerNames() {
        Integer count = askInt("How many players?", 2, 5);
        if (count == null) {
            return null;
        }

        List<String> names = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            TextInputDialog dialog = new TextInputDialog("Player " + i);
            dialog.setHeaderText("Enter name for player " + i);
            dialog.setContentText("Name:");

            String result = dialog.showAndWait().orElse(null);
            if (result == null) {
                return null;
            }

            String name = result.trim();
            names.add(name.isEmpty() ? "Player " + i : name);
        }

        return names;
    }

    private Integer askInt(String prompt, int min, int max) {
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(min, FXCollections.observableArrayList(IntStream.rangeClosed(min, max).boxed().toList()));
        dialog.setHeaderText(prompt);
        return dialog.showAndWait().orElse(null);
    }

    @Override public Player selectNextPlayer(Player currentPlayer, List<Player> players, String prompt) { return null; }
    @Override public Color selectColor(String prompt, List<Color> players) { return null; }
    @Override public UseMode useCard(ActionCard action) { return null; }
    @Override public WildPropertyCard selectWildCardToMove(Player current, List<WildPropertyCard> wildCards) { return null; }
    @Override public Card selectDiscard(Player current, List<Card> cards) { return null; }
    @Override public Card selectPropertyCard(Player owner, List<Card> cards, String prompt) { return null; }
    @Override public Card selectPaymentCard(Player owner, List<Card> cards, String prompt) { return null; }
    @Override public boolean reconfirm(String prompt) { return false; }
}
