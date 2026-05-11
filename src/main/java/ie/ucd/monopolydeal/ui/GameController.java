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
    @FXML private Label handCardsTitle;
    @FXML private Label handCardsText;
    @FXML private Label boardText;
    @FXML private VBox handCardsBox;
    @FXML private VBox tableBox;

    @FXML private ScrollPane cardsScroll;
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
        handCardsBox.setFillWidth(true);
        cardsScroll.setFitToWidth(true);
        cardsScroll.viewportBoundsProperty().addListener((event, oldBounds, newBounds) -> {
            handCardsBox.setMinHeight(newBounds.getHeight());
        });
        cardsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cardsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        cardsScroll.setPannable(true);
        cardsScroll.setBackground(setSolidBackground(Color.WHITE));
        cardsScroll.setBorder(Border.EMPTY);
        cardsScroll.setOnMouseClicked(event -> {
            if (event.getTarget() == cardsScroll || event.getTarget() == handCardsBox && selectedCard != null) {
                selectedCard = null;
                refresh();
            }
        });

        tableBox.setFillWidth(true);
        tableScroll.setFitToWidth(true);
        tableScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tableScroll.setPannable(true);

        handCardsText.setFont(Font.font("Segoe UI", 13));
        boardText.setFont(Font.font("Segoe UI", 13));

        refresh();
    }

    @FXML
    private void onNewGame() {
        List<String> names = askPlayerNames();
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
        handCardsTitle.setText("Current Hand");
        currentPlayer.setText("-");
        actions.setText("0 / 3");
        handCount.setText("0");
        bankTotal.setText("0M");
        completedSets.setText("0 / 3");
        piles.setText("0 / 0");
        handCardsText.setText("No active hand");
        boardText.setText("Start a new game to populate the table");

        selectedCard = null;
        handCardsBox.getChildren().setAll(noCardBox("No cards yet", "Start a new game to render the active hand here."));
        tableBox.getChildren().setAll(noCardBox("No players yet", "Create a game to see hand, bank, and property areas."));

        updateBadge("Ready", Color.rgb(224, 231, 255), Color.rgb(165, 180, 252), Color.rgb(67, 56, 202));
    }

    private void showGame() {
        Player player = game.getCurrPlayer();
        statusTitle.setText("Status");
        handCardsTitle.setText(player.getName() + " Hand");
        currentPlayer.setText(player.getName());
        actions.setText(game.getActionsUsed() + " / " + Player.MAX_ACTIONS_PER_TURN);
        handCount.setText(String.valueOf(player.getCardsAtHand().size()));
        bankTotal.setText(game.getCurrBankTotal() + "M");
        completedSets.setText("0 / 3");
        piles.setText("0 / 0");
        handCardsText.setText(player.getName() + " can act now");
        boardText.setText(game.getPlayers().size() + " players in this match");
        handCardsBox.getChildren().clear();
        tableBox.getChildren().clear();
        updateHand(player);
        updateTable(game.getPlayers());
        updateBadge("Turn Active", Color.rgb(219, 234, 254), Color.rgb(147, 197, 253), Color.rgb(29, 78, 216));
    }

    private List<String> askPlayerNames() {
        ChoiceDialog<Integer> playerNumDialog = new ChoiceDialog<>(2, FXCollections.observableArrayList(IntStream.rangeClosed(2, 5).boxed().toList()));
        playerNumDialog.setHeaderText("How many players?");
        Integer count = playerNumDialog.showAndWait().orElse(null);

        if (count == null) {
            return null;
        }

        List<String> names = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            TextInputDialog playerNameDialog = new TextInputDialog("Player " + i);
            playerNameDialog.setHeaderText("Enter name for player " + i);
            playerNameDialog.setContentText("Name:");

            String result = playerNameDialog.showAndWait().orElse(null);
            if (result == null) {
                return null;
            }

            String name = result.trim();
            if (name.isEmpty()) {
                names.add("Player " + i);
            } else {
                names.add(name);
            }
        }

        return names;
    }

    private void updateHand(Player player) {
        handCardsBox.getChildren().clear();
        List<Card> handCards = player.getCardsAtHand();

        if (handCards.isEmpty()) {
            handCardsBox.getChildren().add(noCardBox("No cards.", "This player has no cards in hand."));
            return;
        }

        for (Card handCard : handCards) {
            handCardsBox.getChildren().add(newHandCard(handCard));
        }
    }

    private HBox newHandCard(Card card) {
        Label name = new Label(card.getName());
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        name.setTextFill(Color.rgb(15, 23, 42));

        VBox textBox = new VBox(4, name);
        String detailText = cardDetail(card);
        if (!detailText.isEmpty()) {
            Label detail = new Label(detailText);
            detail.setTextFill(Color.rgb(71, 85, 105));
            detail.setWrapText(true);
            textBox.getChildren().add(detail);
        }

        Region bar = newCardBar(cardColor(card));
        HBox cardBox = new HBox(12, bar, textBox);
        cardBox.setAlignment(Pos.CENTER_LEFT);
        cardBox.setFillHeight(true);
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
        bar.setPrefWidth(6);
        bar.setMinWidth(6);
        bar.setMaxWidth(6);
        bar.setMaxHeight(Double.MAX_VALUE);
        bar.setBackground(setSolidBackground(color));
        return bar;
    }

    private String cardDetail(Card card) {
        return switch (card) {
            case PropertyCard propertyCard -> propertyCard.getColor().getName();
            case WildPropertyCard wildCard -> {
                String currentColor;
                if (wildCard.getCurrentColor() == null) {
                    currentColor = "Not selected";
                } else {
                    currentColor = wildCard.getCurrentColor().getName();
                }
                yield currentColor;
            }
            case ActionCard actionCard -> actionCard.getActionType().name();
            default -> "";
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
        tableBox.getChildren().clear();

        if (players.isEmpty()) {
            tableBox.getChildren().add(noCardBox("No players.", "Start a new game to show player boards."));
            return;
        }

        for (Player player : players) {
            tableBox.getChildren().add(newPlayerBox(player));
        }
    }

    private VBox newPlayerBox(Player player) {
        boolean isCurrent = (player == game.getCurrPlayer());

        Label name = new Label(player.getName());
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        name.setTextFill(Color.rgb(15, 23, 42));

        String turnText;
        Color turnBackground;
        Color turnTextColor;

        // Set badge
        if (isCurrent) {
            turnText = "Current Turn";
            turnBackground = Color.rgb(220, 252, 231);
            turnTextColor = Color.rgb(22, 101, 52);
        } else {
            turnText = "Waiting";
            turnBackground = Color.rgb(241, 245, 249);
            turnTextColor = Color.rgb(71, 85, 105);
        }

        HBox header = new HBox(8, name,
                newBadge("P" + player.getNumber(), Color.rgb(226, 232, 240), Color.rgb(15, 23, 42)),
                newBadge(turnText, turnBackground, turnTextColor)
        );
        header.setAlignment(Pos.CENTER_LEFT);

        HBox summaryBox = new HBox(8, newBadge("Hand " + player.getCardsAtHand().size()), newBadge("Bank " + player.getBankTotalValue() + "M"));
        summaryBox.setAlignment(Pos.CENTER_LEFT);

        VBox bank = new VBox(4);
        Label bankTitle = new Label("Bank");
        bankTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        bankTitle.setTextFill(Color.rgb(71, 85, 105));
        bank.getChildren().add(bankTitle);

        if (player.getCardsAtBank().isEmpty()) {
            Label emptyBank = new Label("Bank is empty.");
            emptyBank.setTextFill(Color.rgb(100, 116, 139));
            bank.getChildren().add(emptyBank);
        } else {
            FlowPane bankCards = new FlowPane(6, 6);
            bankCards.setPrefWrapLength(320);
            for (Card card : player.getCardsAtBank()) {
                bankCards.getChildren().add(newBankCard(card));
            }
            bank.getChildren().add(bankCards);
        }

        VBox box = new VBox(8, header, summaryBox, bank);
        box.setPadding(new Insets(12));
        box.setMaxWidth(Double.MAX_VALUE);

        // Set focus
        if (isCurrent) {
            box.setBackground(setSolidBackground(Color.rgb(239, 246, 255)));
            box.setBorder(roundCorner(Color.rgb(96, 165, 250)));
        } else {
            box.setBackground(setSolidBackground(Color.WHITE));
            box.setBorder(roundCorner(Color.rgb(203, 213, 225)));
        }

        return box;
    }

    private HBox newBankCard(Card card) {
        Label name = new Label(card.getName());
        name.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        name.setTextFill(Color.rgb(15, 23, 42));

        Region bar = new Region();
        bar.setPrefSize(4, 20);
        bar.setMinWidth(4);
        bar.setBackground(setSolidBackground(cardColor(card)));

        HBox box = new HBox(8, bar, name);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5, 10, 5, 9));
        box.setMinWidth(62);
        box.setBackground(setSolidBackground(Color.WHITE));
        box.setBorder(roundCorner(Color.rgb(203, 213, 225)));
        return box;
    }

    // Gray badge by default
    private Label newBadge(String text) {
        return newBadge(text, Color.rgb(241, 245, 249), Color.rgb(51, 65, 85));
    }

    // Overload to add more options
    private Label newBadge(String text, Color background, Color foreground) {
        Label label = new Label(text);
        label.setPadding(new Insets(4, 8, 4, 8));
        label.setBackground(setSolidBackground(background));
        label.setBorder(roundCorner(background.darker()));
        label.setTextFill(foreground);
        return label;
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
