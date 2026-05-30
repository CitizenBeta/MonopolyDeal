package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.game.Game;
import ie.ucd.monopolydeal.model.Card;
import ie.ucd.monopolydeal.model.Player;
import ie.ucd.monopolydeal.model.PropertyColor;
import ie.ucd.monopolydeal.model.WildPropertyCard;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

public class GameController {
    // Main game state
    private final Game game = new Game();
    // Handle all DecisionMaker prompts in GameDialogs
    private final GameDialogs dialogs = new GameDialogs();
    // Track highlighted hand card
    private Card selectedCard;

    // Top status card
    @FXML private Label statusTitle;
    @FXML private Label statusText;
    @FXML private Label statusBadge;

    // Global statistic cards
    @FXML private Label turnCount;
    @FXML private Label actions;
    @FXML private Label piles;
    @FXML private Label leadingPlayer;

    // Bottom action buttons
    @FXML private Button newGameButton;
    @FXML private Button usedCardsButton;
    @FXML private Button playButton;
    @FXML private Button moveWildButton;
    @FXML private Button endTurnButton;

    // Main content areas
    @FXML private Label handCardsTitle;
    @FXML private HBox handCardsBox;
    @FXML private HBox tableBox;

    // Root and scroll containers
    @FXML private BorderPane rootPane;
    @FXML private ScrollPane cardsScroll;
    @FXML private ScrollPane tableScroll;

    @FXML
    private void initialize() {
        // Connect UI decisions to backend action checks
        // Hide Play when an action has no legal target
        dialogs.setActionPlayChecker(game::canPlayActionCard);
        configureButtons();
        configureStatusArea();
        configureHandArea();
        configureTableArea();
        refresh();
    }

    private void configureButtons() {
        // Set button colors and disabled styles
        GameUI.setActionButton(newGameButton, Color.rgb(37, 99, 235), true);
        GameUI.setActionButton(usedCardsButton, Color.rgb(37, 99, 235), false);
        GameUI.setActionButton(playButton, Color.rgb(22, 163, 74), true);
        GameUI.setActionButton(moveWildButton, Color.rgb(22, 163, 74), false);
        GameUI.setActionButton(endTurnButton, Color.rgb(220, 38, 38), false);
    }

    private void configureStatusArea() {
        // Set status text styles
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
        HandLayout.configure(handCardsBox, cardsScroll, rootPane, () -> {
            if (selectedCard != null) {
                selectedCard = null;
                refresh();
            }
        });
    }

    private void configureTableArea() {
        TableLayout.configure(tableBox, tableScroll, rootPane, this::resizeTableArea);
    }

    // Run when the user presses New Game button
    @FXML
    private void onNewGame() {
        List<String> names = dialogs.askPlayerNames();
        // Need at least two players
        if (names == null || names.size() < 2) {
            return;
        }

        statusText.setText("Game started.");
        game.setup(names);
        refresh();
    }

    // Show used/discarded cards, newest first
    @FXML
    private void onUsedCards() {
        UsedCardsDialog.show(game.getUsedCards());
    }

    // Run when the user presses Play Selected button
    @FXML
    private void onPlaySelected() {
        // Guard against disabled or stale selections
        if (!canPlaySelectedCard()) {
            if (selectedCard != null) {
                statusText.setText("Cannot play " + GameUI.statusCardText(selectedCard) + ".");
            }
            refresh();
            return;
        }

        Card cardToPlay = selectedCard;
        if (game.playCard(cardToPlay, dialogs)) {
            if (game.isOver()) {
                // Show winner after the backend marks the game as over
                Player winner = game.getWinner();
                if (winner == null) {
                    statusText.setText("Game over.");
                } else {
                    statusText.setText(winner.getName() + " won.");
                }
            } else {
                statusText.setText("Played " + GameUI.statusCardText(cardToPlay) + ".");
            }
            selectedCard = null;
        } else {
            statusText.setText("Cannot play " + GameUI.statusCardText(cardToPlay) + ".");
        }

        refresh();
    }

    // Run when the user presses Move wild button
    @FXML
    private void onMoveWild() {
        // Wild cards can only move during an active game
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

    // Run when the user presses End Turn button
    @FXML
    private void onEndTurn() {
        // Ignore end turn before the game starts or after it ends
        if (!game.isStarted() || game.isOver()) {
            return;
        }

        if (game.endTurn(dialogs)) {
            // Successful end turn also clears any old hand selection
            selectedCard = null;
            statusText.setText("Turn ended.");
        } else {
            statusText.setText("Discard required.");
        }
        refresh();
    }

    // Refresh view after user's action
    private void refresh() {
        // Choose between pregame and in-game layouts
        if (!game.isStarted()) {
            showPregame();
        } else {
            showGame();
        }
        updateButtonStatus();
    }

    // Show empty UI
    private void showPregame() {
        // Reset labels to the initial screen state
        statusTitle.setText("Status");
        statusText.setText("Start a new game.");
        handCardsTitle.setText("Current Hand");
        updateActionsBadge(0);
        updateDeckCount();
        leadingPlayer.setText("-");

        selectedCard = null;
        // Center empty placeholder cards
        handCardsBox.setAlignment(Pos.CENTER);
        tableBox.setAlignment(Pos.CENTER);
        handCardsBox.getChildren().setAll(GameUI.centeredEmptyBox(cardsScroll, "No cards yet", "Start a new game to render the active hand here"));
        tableBox.getChildren().setAll(GameUI.centeredEmptyBox(tableScroll, "No players yet", "Create a game to see hand, bank, and property areas"));
        resizeHandArea();
        resizeTableArea();

        updateTurnCount(0);
        updateStatusBadge("Ready", Color.rgb(224, 231, 255), Color.rgb(165, 180, 252), Color.rgb(67, 56, 202));
    }

    // Show in-game UI
    private void showGame() {
        Player player = game.getCurrPlayer();
        // Rebuild labels and content from current backend state
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
            // Green badge for finished game
            updateStatusBadge("Game Over", Color.rgb(220, 252, 231), Color.rgb(134, 239, 172), Color.rgb(22, 101, 52));
        } else {
            // Blue badge for active turn
            updateStatusBadge("Turn Active", Color.rgb(219, 234, 254), Color.rgb(147, 197, 253), Color.rgb(29, 78, 216));
        }
    }

    // Find player with most completed sets, then highest bank total
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
            resizeHandArea();
            return;
        }

        handCardsBox.setAlignment(Pos.CENTER);
        // Recreate every hand card because selection changes card styling
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
            resizeTableArea();
            return;
        }

        tableBox.setAlignment(Pos.TOP_CENTER);
        Player winner = game.getWinner();
        // Add one player board per player
        for (Player player : players) {
            VBox playerBox = GameUI.newPlayerBox(
                    player,
                    player == game.getCurrPlayer(),
                    game.isOver() && player == winner,
                    playerBoxMinHeight()
            );
            HBox.setHgrow(playerBox, Priority.NEVER);
            tableBox.getChildren().add(playerBox);
        }

        resizeTableArea();
        // Scroll to current player after layout
        Platform.runLater(this::scrollTableToCurrentPlayer);
    }

    // Enable/disable action buttons
    private void updateButtonStatus() {
        playButton.setDisable(!canPlaySelectedCard());
        // Move Wild is only useful if current player has placed wild cards
        boolean canMoveWild = false;
        if (game.isStarted() && !game.isOver()) {
            canMoveWild = !game.getCurrPlayer().getPlacedWildCards().isEmpty();
        }
        moveWildButton.setDisable(!canMoveWild);
        endTurnButton.setDisable(!game.isStarted() || game.isOver());
    }

    // Check selected card before enabling Play button
    private boolean canPlaySelectedCard() {
        if (!game.isStarted() || game.isOver() || selectedCard == null) {
            return false;
        }

        return game.canPlayCard(selectedCard);
    }

    // Update action badge color by used action count
    private void updateActionsBadge(int actionsUsed) {
        Color background;
        Color border;
        Color foreground;

        // Red at 3, orange at 2, blue at 0 or 1
        if (actionsUsed >= Player.MAX_ACTIONS_PER_TURN) {
            background = Color.rgb(254, 226, 226);
            border = Color.rgb(252, 165, 165);
            foreground = Color.rgb(153, 27, 27);
        } else if (actionsUsed == 2) {
            background = Color.rgb(255, 247, 237);
            border = Color.rgb(253, 186, 116);
            foreground = Color.rgb(194, 65, 12);
        } else {
            background = Color.rgb(219, 234, 254);
            border = Color.rgb(147, 197, 253);
            foreground = Color.rgb(29, 78, 216);
        }

        actions.setText("Actions " + actionsUsed + " / " + Player.MAX_ACTIONS_PER_TURN);
        actions.setBackground(GameUI.solidBackground(background));
        actions.setBorder(GameUI.roundCorner(border));
        actions.setTextFill(foreground);
    }

    // Update turn counter label
    private void updateTurnCount(int count) {
        turnCount.setText(String.valueOf(count));
    }

    // Update deck remaining and total card counter
    private void updateDeckCount() {
        piles.setText(game.getDrawPileNumber() + " / " + game.getTotalCardNumber());
    }

    // Update main game status badge
    private void updateStatusBadge(String text, Color background, Color border, Color foreground) {
        statusBadge.setText(text);
        statusBadge.setBackground(GameUI.solidBackground(background));
        statusBadge.setBorder(GameUI.roundCorner(border));
        statusBadge.setTextFill(foreground);
    }

    private void resizeHandArea() {
        HandLayout.resize(handCardsBox, cardsScroll);
    }

    private void resizeTableArea() {
        TableLayout.resize(game, tableBox, tableScroll);
    }

    private void scrollTableToCurrentPlayer() {
        TableLayout.scrollToCurrentPlayer(game, tableBox, tableScroll);
    }

    private double playerBoxMinHeight() {
        return TableLayout.playerBoxMinHeight(tableScroll);
    }
}
