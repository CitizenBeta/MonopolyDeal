package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.game.DecisionMaker;
import ie.ucd.monopolydeal.game.Game;
import ie.ucd.monopolydeal.model.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.List;

public class GameController implements DecisionMaker {
    private final Game game = new Game();

    @FXML private Label statusTitle;
    @FXML private Label statusText;
    @FXML private Label statusState;

    @FXML
    private void initialize() {
        refreshView();
    }

    private void refreshView() {
        statusTitle.setText("Status");
        statusText.setText("Start a new game.");
        statusState.setText("Ready");
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
