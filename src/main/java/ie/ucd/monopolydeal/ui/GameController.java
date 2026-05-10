package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.game.DecisionMaker;
import ie.ucd.monopolydeal.game.Game;
import ie.ucd.monopolydeal.model.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class GameController implements DecisionMaker {
    private final Game game = new Game();
    private Card selectedCard = null;

    @FXML private Label statusTitle;
    @FXML private Label statusText;
    @FXML private Label badge;
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
    @FXML private Label handText;
    @FXML private Label boardText;
    @FXML private VBox handCards;
    @FXML private VBox table;

    @FXML private ScrollPane handCardsScroll;
    @FXML private ScrollPane tableScroll;

    @FXML
    private void initialize() {
        // Set buttons
        setActionButton(newGameButton, Color.rgb(37, 99, 235), true);
        setActionButton(playButton, Color.rgb(22, 163, 74), true);
        setActionButton(moveWildButton, Color.rgb(245, 158, 11), false);
        setActionButton(endTurnButton, Color.rgb(71, 85, 105), false);

        // Set status
        statusTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        statusTitle.setTextFill(Color.rgb(71, 85, 105));
        statusText.setWrapText(true);
        statusText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        statusText.setTextFill(Color.rgb(15, 23, 42));
        badge.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));

        // Set content area
        handCards.setFillWidth(true);
        handCardsScroll.setFitToWidth(true);
        handCardsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        handCardsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        handCardsScroll.setPannable(true);
        handCardsScroll.setBackground(setSolidBackground(Color.WHITE));
        handCardsScroll.setBorder(Border.EMPTY);

        table.setFillWidth(true);
        tableScroll.setFitToWidth(true);
        tableScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tableScroll.setPannable(true);

        handText.setFont(Font.font("Segoe UI", 13));
        boardText.setFont(Font.font("Segoe UI", 13));

        refresh();
    }

    @FXML
    private void onNewGame() {
        List<String> names = askNames();
        if (names == null || names.size() < 2) {
            return;
        }

        game.setup(names);
        statusText.setText("Game started.");
        refresh();
    }

    @FXML
    private void onPlaySelected() {
        if (!game.isStarted() || selectedCard == null) {
            return;
        }

        if (game.playCard(selectedCard)) {
            statusText.setText("Played " + selectedCard.getName());
            selectedCard = null;
        } else {
            statusText.setText("Cannot play " + selectedCard.getName());
        }

        refresh();
    }

    @FXML
    private void onMoveWild() {
    }

    @FXML
    private void onEndTurn() {
        if (!game.isStarted()) {
            return;
        }

        selectedCard = null;
        game.endTurn();
        statusText.setText("Turn ended.");
        refresh();
    }

    private void refresh() {
        if (!game.isStarted()) {
            showPregame();
        } else {
            showGame();
        }
        updateButtonStatus();
    }

    private void showPregame() {
        statusTitle.setText("Status");
        statusText.setText("Start a new game.");
        currentPlayer.setText("-");
        actions.setText("0 / 3");
        handCount.setText("0");
        bankTotal.setText("0M");
        completedSets.setText("0 / 3");
        piles.setText("0 / 0");
        handText.setText("No active hand");
        boardText.setText("Start a new game to populate the table");

        selectedCard = null;
        handCards.getChildren().setAll(noCardBox("No cards yet", "Start a new game to render the active hand here."));
        table.getChildren().setAll(noCardBox("No players yet", "Create a game to see hand, bank, and property areas."));

        updateBadge("Ready", Color.rgb(224, 231, 255), Color.rgb(165, 180, 252), Color.rgb(67, 56, 202));
    }

    private void showGame() {
        Player player = game.getCurrPlayer();
        statusTitle.setText("Status");
        currentPlayer.setText(player.getName());
        actions.setText(game.getActionsUsed() + " / " + Player.MAX_ACTIONS_PER_TURN);
        handCount.setText(String.valueOf(player.getCardsAtHand().size()));
        bankTotal.setText(game.getCurrBankTotal() + "M");
        completedSets.setText("0 / 3");
        piles.setText("0 / 0");
        handText.setText(player.getName() + " can act now");
        boardText.setText(game.getPlayers().size() + " players in this match");
        handCards.getChildren().clear();
        table.getChildren().clear();
        updateHand(player);
        updateTable(game.getPlayers());
        updateBadge("Turn Active", Color.rgb(219, 234, 254), Color.rgb(147, 197, 253), Color.rgb(29, 78, 216));
    }

    private List<String> askNames() {
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

    private void updateHand(Player player) {
        handCards.getChildren().clear();
        List<Card> hand = player.getCardsAtHand();

        if (hand.isEmpty()) {
            handCards.getChildren().add(noCardBox("No cards.", "This player has no cards in hand."));
            return;
        }

        for (Card card : hand) {
            handCards.getChildren().add(newHandCard(card));
        }
    }

    private HBox newHandCard(Card card) {
        Label name = new Label(card.getName());
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        name.setTextFill(Color.rgb(15, 23, 42));

        Label type = new Label(cardType(card));
        type.setTextFill(Color.rgb(71, 85, 105));

        Label detail = new Label(cardDetail(card));
        detail.setTextFill(Color.rgb(71, 85, 105));
        detail.setWrapText(true);

        VBox textBox = new VBox(4, name, type, detail);

        HBox cardBox = new HBox(12, newCardBar(cardColor(card)), textBox);
        cardBox.setAlignment(Pos.CENTER_LEFT);
        cardBox.setPadding(new Insets(12));
        cardBox.setMaxWidth(Double.MAX_VALUE);

        if (selectedCard == card) {
            cardBox.setBackground(setSolidBackground(Color.rgb(239, 246, 255)));
            cardBox.setBorder(roundCorner(Color.rgb(37, 99, 235)));
        } else {
            cardBox.setBackground(setSolidBackground(Color.WHITE));
            cardBox.setBorder(roundCorner(Color.rgb(203, 213, 225)));
        }

        cardBox.setOnMouseClicked(e -> {
            selectedCard = card;
            refresh();
        });

        return cardBox;
    }

    private Region newCardBar(Color color) {
        Region bar = new Region();
        bar.setPrefSize(6, 46);
        bar.setMinWidth(6);
        bar.setBackground(setSolidBackground(color));
        return bar;
    }

    private String cardType(Card card) {
        return switch (card) {
            case MoneyCard _ -> "Money";
            case PropertyCard _ -> "Property";
            case WildPropertyCard _ -> "Wild";
            case ActionCard _ -> "Action";
            case null, default -> "Card";
        };
    }

    private String cardDetail(Card card) {
        return switch (card) {
            case PropertyCard propertyCard -> "Color: " + propertyCard.getColor().getName() + " | Value: " + propertyCard.getBankValue() + "M";
            case WildPropertyCard wildCard -> "Current color: " + (wildCard.getCurrentColor() == null ? "Not selected" : wildCard.getCurrentColor().getName()) + " | Value: " + wildCard.getBankValue() + "M";
            case ActionCard actionCard -> "Effect: " + actionCard.getActionType().name() + " | Value: " + actionCard.getBankValue() + "M";
            default -> "Value: " + card.getBankValue() + "M";
        };
    }

    private Color cardColor(Card card) {
        return switch (card) {
            case MoneyCard _ -> Color.rgb(22, 163, 74);
            case PropertyCard _ -> Color.rgb(37, 99, 235);
            case WildPropertyCard _ -> Color.rgb(8, 145, 178);
            case ActionCard _ -> Color.rgb(217, 119, 6);
            case null, default -> Color.rgb(100, 116, 139);
        };
    }

    private void updateTable(List<Player> players) {
        table.getChildren().clear();

        if (players.isEmpty()) {
            table.getChildren().add(new Label("No players in the game."));
            return;
        }

        for (Player player:players) {
            table.getChildren().add(new Label(player.getName() + " | Hand " + player.getCardsAtHand().size() + " | Bank " + player.getCardsAtBank().size()));
        }
    }

    private void updateButtonStatus() {
        playButton.setDisable(!game.isStarted() || selectedCard == null);
        moveWildButton.setDisable(!game.isStarted());
        endTurnButton.setDisable(!game.isStarted());
    }

    private void updateBadge(String text, Color background, Color border, Color foreground) {
        badge.setText(text);
        badge.setBackground(setSolidBackground(background));
        badge.setBorder(roundCorner(border));
        badge.setTextFill(foreground);
    }

    private VBox noCardBox(String titleText, String bodyText) {
        Label title = new Label(titleText);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        Label body = new Label(bodyText);
        body.setWrapText(true);
        body.setTextFill(Color.rgb(71, 85, 105));

        VBox box = new VBox(8, title, body);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(22));
        box.setMaxWidth(Double.MAX_VALUE);
        box.setBackground(setSolidBackground(Color.WHITE));
        box.setBorder(roundCorner(Color.rgb(203, 213, 225)));
        return box;
    }

    private void setActionButton(Button button, Color color, boolean isFilled) {
        button.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        button.setPadding(new Insets(10, 16, 10, 16));
        button.setMinHeight(40);

        if (isFilled) {
            button.setTextFill(Color.WHITE);
            button.setBackground(setSolidBackground(color));
            button.setBorder(roundCorner(color.darker()));
        } else {
            button.setTextFill(color.darker());
            button.setBackground(setSolidBackground(Color.WHITE));
            button.setBorder(roundCorner(color));
        }
    }

    private Background setSolidBackground(Color color) {
        return new Background(new BackgroundFill(color, new CornerRadii(12), Insets.EMPTY));
    }

    private Border roundCorner(Color color) {
        return new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1)));
    }

    @Override public Player selectNextPlayer(Player currentPlayer, List<Player> players, String prompt) { return null; }
    @Override public PropertyColor selectColor(String prompt, List<PropertyColor> players) { return null; }
    @Override public UseMode useCard(ActionCard action) { return null; }
    @Override public WildPropertyCard selectWildCardToMove(Player current, List<WildPropertyCard> wildCards) { return null; }
    @Override public Card selectDiscard(Player current, List<Card> cards) { return null; }
    @Override public Card selectPropertyCard(Player owner, List<Card> cards, String prompt) { return null; }
    @Override public Card selectPaymentCard(Player owner, List<Card> cards, String prompt) { return null; }
    @Override public boolean reconfirm(String prompt) { return false; }
}
