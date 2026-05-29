package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.game.Game;
import ie.ucd.monopolydeal.model.Card;
import ie.ucd.monopolydeal.model.Player;
import ie.ucd.monopolydeal.model.PropertyColor;
import ie.ucd.monopolydeal.model.WildPropertyCard;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

public class GameController {
    // Show at most three players before horizontal scrolling
    private static final int MAX_VISIBLE_PLAYERS = 3;

    private final Game game = new Game();
    // Handle all DecisionMaker prompts in GameDialogs
    private final GameDialogs dialogs = new GameDialogs();
    // Track highlighted hand card
    private Card selectedCard;

    @FXML private Label statusTitle;
    @FXML private Label statusText;
    @FXML private Label statusBadge;
    @FXML private Label turnCount;
    @FXML private Label actions;
    @FXML private Label piles;
    @FXML private Label leadingPlayer;
    @FXML private Button newGameButton;
    @FXML private Button usedCardsButton;
    @FXML private Button playButton;
    @FXML private Button moveWildButton;
    @FXML private Button endTurnButton;
    @FXML private Label handCardsTitle;
    @FXML private HBox handCardsBox;
    @FXML private HBox tableBox;

    @FXML private BorderPane rootPane;
    @FXML private ScrollPane cardsScroll;
    @FXML private ScrollPane tableScroll;

    @FXML
    private void initialize() {
        // Hide Play when an action has no legal target
        dialogs.setActionPlayChecker(game::canPlayActionCard);
        configureButtons();
        configureStatusArea();
        configureHandArea();
        configureTableArea();
        refresh();
    }

    private void configureButtons() {
        // Set buttons
        GameUI.setActionButton(newGameButton, Color.rgb(37, 99, 235), true);
        GameUI.setActionButton(usedCardsButton, Color.rgb(37, 99, 235), false);
        GameUI.setActionButton(playButton, Color.rgb(22, 163, 74), true);
        GameUI.setActionButton(moveWildButton, Color.rgb(22, 163, 74), false);
        GameUI.setActionButton(endTurnButton, Color.rgb(220, 38, 38), false);
    }

    private void configureStatusArea() {
        // Set status
        statusTitle.setFont(Font.font("Segoe UI Semibold", 11));
        statusTitle.setTextFill(Color.rgb(71, 85, 105));
        statusText.setWrapText(true);
        statusText.setMinWidth(0);
        statusText.setMaxWidth(Double.MAX_VALUE);
        statusText.setFont(Font.font("Segoe UI Bold", 16));
        statusText.setTextFill(Color.rgb(15, 23, 42));
        leadingPlayer.setWrapText(true);
    }

    private void configureHandArea() {
        // Set hand card area
        handCardsBox.setFillHeight(false);
        handCardsBox.setAlignment(Pos.CENTER);
        cardsScroll.setFitToHeight(true);
        cardsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        cardsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cardsScroll.setPannable(false);
        // Keep hand strip as tall as viewport
        cardsScroll.viewportBoundsProperty().addListener((_, _, bounds) -> resizeHandArea(bounds.getWidth(), bounds.getHeight()));
        cardsScroll.setVvalue(0);
        cardsScroll.vvalueProperty().addListener((_, _, _) -> cardsScroll.setVvalue(0));
        // Use mouse wheel to scroll hand horizontally
        cardsScroll.setOnScroll(e -> {
            if (e.getDeltaY() != 0) {
                double nextValue = cardsScroll.getHvalue() - e.getDeltaY() / handCardsBox.getWidth();
                cardsScroll.setHvalue(Math.clamp(nextValue, 0, 1));
                e.consume();
            }
        });
        cardsScroll.setBackground(GameUI.solidBackground(Color.WHITE));
        cardsScroll.setBorder(Border.EMPTY);
        cardsScroll.setFocusTraversable(false);
        cardsScroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        cardsScroll.setOnMousePressed(e -> rootPane.requestFocus());
        // Clear selected card when clicking empty hand space
        cardsScroll.setOnMouseClicked(e -> {
            if ((e.getTarget() == cardsScroll || e.getTarget() == handCardsBox) && selectedCard != null) {
                selectedCard = null;
                refresh();
            }
        });
    }

    private void configureTableArea() {
        // Set player table area
        tableBox.setFillHeight(true);
        tableBox.setAlignment(Pos.TOP_CENTER);
        tableScroll.setFitToWidth(false);
        tableScroll.setFitToHeight(false);
        tableScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tableScroll.setPannable(true);
        // Resize player boards when window changes
        tableScroll.viewportBoundsProperty().addListener((_, _, bounds) -> resizeTableArea(bounds.getWidth(), bounds.getHeight()));
        tableScroll.setFocusTraversable(false);
        tableScroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        tableScroll.setOnMousePressed(e -> rootPane.requestFocus());
    }

    // Run when the user presses New Game button
    @FXML
    private void onNewGame() {
        List<String> names = dialogs.askPlayerNames();
        // Player >= 2
        if (names == null || names.size() < 2) {
            return;
        }

        game.setup(names);
        statusText.setText("Game started.");
        refresh();
    }

    // Show used/discarded cards, newest first
    @FXML
    private void onUsedCards() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Used Cards");
        alert.setHeaderText("Used cards in this game. Newest first");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefSize(520, 420);

        List<Game.UsedCard> usedCards = game.getUsedCards();
        if (usedCards.isEmpty()) {
            // Create an empty used cards box
            StackPane box = new StackPane(GameUI.noCardBox("No cards yet", "No cards have been used or discarded yet"));
            box.setAlignment(Pos.CENTER);
            box.setPrefHeight(360);
            scrollPane.setContent(box);
        } else {
            VBox cardsBox = new VBox(10);
            cardsBox.setPadding(new Insets(10));
            cardsBox.setFillWidth(true);

            for (Game.UsedCard usedCard : usedCards) {
                cardsBox.getChildren().add(GameUI.newUsedCardBox(usedCard));
            }

            scrollPane.setContent(cardsBox);
        }
        alert.getDialogPane().setContent(scrollPane);
        alert.showAndWait();
    }

    // Run when the user presses Play Selected button
    @FXML
    private void onPlaySelected() {
        if (!canPlaySelectedCard()) {
            if (selectedCard != null) {
                statusText.setText("Cannot play " + GameUI.statusCardText(selectedCard) + ".");
            }
            refresh();
            return;
        }

        if (game.playCard(selectedCard, dialogs)) {
            if (game.isOver()) {
                Player winner = game.getWinner();
                if (winner == null) {
                    statusText.setText("Game over.");
                } else {
                    statusText.setText(winner.getName() + " won.");
                }
            } else {
                statusText.setText("Played " + GameUI.statusCardText(selectedCard) + ".");
            }
            selectedCard = null;
        } else {
            statusText.setText("Cannot play " + GameUI.statusCardText(selectedCard) + ".");
        }

        refresh();
    }

    // Run when the user presses Move wild button
    @FXML
    private void onMoveWild() {
        if (!game.isStarted() || game.isOver()) {
            return;
        }

        Player current = game.getCurrPlayer();
        WildPropertyCard wildCard = dialogs.selectWildCardToMove(current, current.getPlacedWildCards());
        if (wildCard == null) {
            return;
        }

        PropertyColor oldColor = wildCard.getCurrentColor();
        PropertyColor newColor = dialogs.selectColor("Move " + wildCard.getName() + " to:", wildCard.getPossibleColors());
        if (newColor == null) {
            return;
        }

        current.moveExistingWild(wildCard, newColor);
        if (wildCard.getCurrentColor() != oldColor && wildCard.getCurrentColor() == newColor) {
            statusText.setText("Moved wild card.");
        } else {
            statusText.setText("Wild card not moved.");
        }

        selectedCard = null;
        refresh();
    }

    @FXML
    private void onEndTurn() {
        if (!game.isStarted() || game.isOver()) {
            return;
        }

        if (game.endTurn(dialogs)) {
            selectedCard = null;
            statusText.setText("Turn ended.");
        } else {
            statusText.setText("Discard required.");
        }
        refresh();
    }

    // Refresh view after user's action
    private void refresh() {
        if (!game.isStarted()) {
            showPregame();
        } else {
            showGame();
        }
        updateButtonStatus();
    }

    // Show empty UI
    private void showPregame() {
        statusTitle.setText("Status");
        statusText.setText("Start a new game.");
        handCardsTitle.setText("Current Hand");
        updateActionsBadge(0);
        updateDeckCount();
        leadingPlayer.setText("-");

        selectedCard = null;
        handCardsBox.setAlignment(Pos.CENTER);
        tableBox.setAlignment(Pos.CENTER);
        handCardsBox.getChildren().setAll(GameUI.centeredEmptyBox(cardsScroll, "No cards yet", "Start a new game to render the active hand here"));
        tableBox.getChildren().setAll(GameUI.centeredEmptyBox(tableScroll, "No players yet", "Create a game to see hand, bank, and property areas"));
        resizeHandArea(cardsScroll.getViewportBounds().getWidth(), cardsScroll.getViewportBounds().getHeight());
        resizeTableArea(tableScroll.getViewportBounds().getWidth(), tableScroll.getViewportBounds().getHeight());

        updateTurnCount(0);
        updateStatusBadge("Ready", Color.rgb(224, 231, 255), Color.rgb(165, 180, 252), Color.rgb(67, 56, 202));
    }

    // Show in-game UI
    private void showGame() {
        Player player = game.getCurrPlayer();
        statusTitle.setText("Status");
        handCardsTitle.setText(player.getName() + " Hand");
        updateActionsBadge(game.getActionsUsed());
        updateDeckCount();
        leadingPlayer.setText(leadingPlayer());
        handCardsBox.getChildren().clear();
        tableBox.getChildren().clear();
        updateHand(player);
        updateTable(game.getPlayers());
        updateTurnCount(game.getTurnCount());
        if (game.isOver()) {
            updateStatusBadge("Game Over", Color.rgb(254, 226, 226), Color.rgb(252, 165, 165), Color.rgb(153, 27, 27));
        } else {
            updateStatusBadge("Turn Active", Color.rgb(219, 234, 254), Color.rgb(147, 197, 253), Color.rgb(29, 78, 216));
        }
    }

    // Find the player who have highest completed sets, then highest bank total
    private String leadingPlayer() {
        Player highestPlayer = null;
        int highestSets = -1;
        int highestBank = -1;

        for (Player player : game.getPlayers()) {
            int completedSets = player.countCompletedSets();
            int bankTotal = player.getBankTotalValue();

            if (completedSets > highestSets || (completedSets == highestSets && bankTotal > highestBank)) {
                highestPlayer = player;
                highestSets = completedSets;
                highestBank = bankTotal;
            }
        }

        if (highestPlayer == null) {
            return "-";
        }

        return highestPlayer.getName();
    }

    // Update player's hand from backend
    private void updateHand(Player player) {
        handCardsBox.getChildren().clear();
        List<Card> handCards = player.getCardsAtHand();

        // Clear selection after card leaves hand
        if (selectedCard != null && !handCards.contains(selectedCard)) {
            selectedCard = null;
        }

        if (handCards.isEmpty()) {
            handCardsBox.setAlignment(Pos.CENTER);
            handCardsBox.getChildren().add(GameUI.centeredEmptyBox(cardsScroll, "No cards", "This player has no cards in hand"));
            resizeHandArea(cardsScroll.getViewportBounds().getWidth(), cardsScroll.getViewportBounds().getHeight());
            return;
        }

        handCardsBox.setAlignment(Pos.CENTER);
        for (Card handCard : handCards) {
            handCardsBox.getChildren().add(GameUI.newHandCard(handCard, selectedCard == handCard, this::toggleSelectedCard));
        }
    }

    // If clicked again, remove focus
    private void toggleSelectedCard(Card card) {
        if (selectedCard == card) {
            selectedCard = null;
        } else {
            selectedCard = card;
        }
        refresh();
    }

    // Rebuild player boards from game state
    private void updateTable(List<Player> players) {
        tableBox.getChildren().clear();

        if (players.isEmpty()) {
            tableBox.setAlignment(Pos.CENTER);
            tableBox.getChildren().add(GameUI.centeredEmptyBox(tableScroll, "No players", "Start a new game to show player boards"));
            resizeTableArea(tableScroll.getViewportBounds().getWidth(), tableScroll.getViewportBounds().getHeight());
            return;
        }

        tableBox.setAlignment(Pos.TOP_CENTER);
        for (Player player : players) {
            VBox playerBox = GameUI.newPlayerBox(player, player == game.getCurrPlayer(), playerBoxMinHeight());
            HBox.setHgrow(playerBox, Priority.NEVER);
            tableBox.getChildren().add(playerBox);
        }

        resizeTableArea(tableScroll.getViewportBounds().getWidth(), tableScroll.getViewportBounds().getHeight());
        // Scroll to current player after layout
        Platform.runLater(this::scrollTableToCurrentPlayer);
    }

    // Enable/diable a button
    private void updateButtonStatus() {
        playButton.setDisable(!canPlaySelectedCard());
        boolean canMoveWild = false;
        if (game.isStarted() && !game.isOver()) {
            canMoveWild = !game.getCurrPlayer().getPlacedWildCards().isEmpty();
        }
        moveWildButton.setDisable(!canMoveWild);
        endTurnButton.setDisable(!game.isStarted() || game.isOver());
    }

    private boolean canPlaySelectedCard() {
        if (!game.isStarted() || game.isOver() || selectedCard == null) {
            return false;
        }

        return game.canPlayCard(selectedCard);
    }

    private void updateActionsBadge(int actionsUsed) {
        Color background;
        Color border;
        Color foreground;

        if (actionsUsed >= Player.MAX_ACTIONS_PER_TURN) {
            background = Color.rgb(254, 226, 226);
            border = Color.rgb(252, 165, 165);
            foreground = Color.rgb(153, 27, 27);
        } else if (actionsUsed == 2) {
            background = Color.rgb(255, 247, 237);
            border = Color.rgb(253, 186, 116);
            foreground = Color.rgb(194, 65, 12);
        } else {
            background = Color.rgb(220, 252, 231);
            border = Color.rgb(134, 239, 172);
            foreground = Color.rgb(22, 101, 52);
        }

        actions.setText("Actions " + actionsUsed + " / " + Player.MAX_ACTIONS_PER_TURN);
        actions.setBackground(GameUI.solidBackground(background));
        actions.setBorder(GameUI.roundCorner(border));
        actions.setTextFill(foreground);
    }

    private void updateTurnCount(int count) {
        turnCount.setText(String.valueOf(count));
    }

    private void updateDeckCount() {
        piles.setText(game.getDrawPileNumber() + " / " + game.getTotalCardNumber());
    }

    private void updateStatusBadge(String text, Color background, Color border, Color foreground) {
        statusBadge.setText(text);
        statusBadge.setBackground(GameUI.solidBackground(background));
        statusBadge.setBorder(GameUI.roundCorner(border));
        statusBadge.setTextFill(foreground);
    }

    private void resizeHandArea(double width, double height) {
        handCardsBox.setMinWidth(width);
        handCardsBox.setMinHeight(height);
    }

    // Show up to three player boards before scrolling
    private void resizeTableArea(double width, double height) {
        int playerCount = 0;
        if (game.isStarted()) {
            playerCount = game.getPlayers().size();
        }

        double contentWidth = tableContentWidth(width, playerCount);

        tableBox.setMinWidth(contentWidth);
        tableBox.setPrefWidth(contentWidth);
        tableBox.setMinHeight(height);

        double playerWidth = tablePlayerWidth(width, playerCount);
        for (javafx.scene.Node child : tableBox.getChildren()) {
            if (child instanceof Region region) {
                if (playerCount > 0 && !region.prefWidthProperty().isBound()) {
                    region.setMinWidth(playerWidth);
                    region.setPrefWidth(playerWidth);
                    region.setMaxWidth(playerWidth);
                }
                region.setMinHeight(playerBoxMinHeight());
            }
        }
    }

    // Calculate one player board width
    private double tablePlayerWidth(double viewportWidth, int playerCount) {
        if (playerCount <= 0 || viewportWidth <= 0) {
            return 0;
        }

        int visiblePlayers = Math.min(playerCount, MAX_VISIBLE_PLAYERS);
        Insets padding = tableBox.getPadding();
        double spacing = tableBox.getSpacing() * (visiblePlayers - 1);
        double width = viewportWidth - padding.getLeft() - padding.getRight() - spacing;
        return Math.max(260, width / visiblePlayers);
    }

    // Calculate total table content width
    private double tableContentWidth(double viewportWidth, int playerCount) {
        if (playerCount <= 0 || viewportWidth <= 0) {
            return viewportWidth;
        }

        double playerWidth = tablePlayerWidth(viewportWidth, playerCount);
        Insets padding = tableBox.getPadding();
        double spacing = tableBox.getSpacing() * Math.max(0, playerCount - 1);
        double contentWidth = padding.getLeft() + padding.getRight() + playerWidth * playerCount + spacing;
        return Math.max(viewportWidth, contentWidth);
    }

    // Scroll table to keep current player visible
    private void scrollTableToCurrentPlayer() {
        if (!game.isStarted() || tableBox.getChildren().isEmpty()) {
            tableScroll.setHvalue(0);
            return;
        }

        List<Player> players = game.getPlayers();
        int playerCount = players.size();
        if (playerCount <= MAX_VISIBLE_PLAYERS) {
            tableScroll.setHvalue(0);
            return;
        }

        int currentIndex = players.indexOf(game.getCurrPlayer());
        if (currentIndex < 0) {
            tableScroll.setHvalue(0);
            return;
        }

        int maxStartIndex = playerCount - MAX_VISIBLE_PLAYERS;
        int startIndex = currentIndex - 1;
        if (startIndex < 0) {
            startIndex = 0;
        }
        if (startIndex > maxStartIndex) {
            startIndex = maxStartIndex;
        }

        tableScroll.setHvalue((double) startIndex / maxStartIndex);
    }

    // Match player board height to visible table area
    private double playerBoxMinHeight() {
        double viewportHeight = tableScroll.getViewportBounds().getHeight();
        if (viewportHeight <= 0) {
            return 165;
        }
        return Math.max(165, viewportHeight - 18);
    }
}
