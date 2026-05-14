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
        // Set buttons
        setActionButton(newGameButton, Color.rgb(37, 99, 235), true);
        setActionButton(usedCardsButton, Color.rgb(37, 99, 235), false);
        setActionButton(playButton, Color.rgb(22, 163, 74), true);
        setActionButton(moveWildButton, Color.rgb(22, 163, 74), false);
        setActionButton(endTurnButton, Color.rgb(220, 38, 38), false);

        // Set status
        statusTitle.setFont(Font.font("Segoe UI Semibold", 11));
        statusTitle.setTextFill(Color.rgb(71, 85, 105));
        statusText.setWrapText(true);
        statusText.setMinWidth(0);
        statusText.setMaxWidth(Double.MAX_VALUE);
        statusText.setFont(Font.font("Segoe UI Bold", 16));
        statusText.setTextFill(Color.rgb(15, 23, 42));
        leadingPlayer.setWrapText(true);

        // Set hand card area
        handCardsBox.setFillHeight(false);
        handCardsBox.setAlignment(Pos.CENTER);
        cardsScroll.setFitToHeight(true);
        cardsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        cardsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cardsScroll.setPannable(false);
        cardsScroll.viewportBoundsProperty().addListener((_, _, bounds) -> resizeHandArea(bounds.getWidth(), bounds.getHeight()));
        cardsScroll.setVvalue(0);
        cardsScroll.vvalueProperty().addListener((_, _, _) -> cardsScroll.setVvalue(0));
        cardsScroll.setOnScroll(e -> {
            if (e.getDeltaY() != 0) {
                double nextValue = cardsScroll.getHvalue() - e.getDeltaY() / handCardsBox.getWidth();
                cardsScroll.setHvalue(Math.clamp(nextValue, 0, 1));
                e.consume();
            }
        });
        cardsScroll.setBackground(setSolidBackground(Color.WHITE));
        cardsScroll.setBorder(Border.EMPTY);
        cardsScroll.setFocusTraversable(false);
        cardsScroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        cardsScroll.setOnMousePressed(e -> rootPane.requestFocus());
        cardsScroll.setOnMouseClicked(e -> {
            if ((e.getTarget() == cardsScroll || e.getTarget() == handCardsBox) && selectedCard != null) {
                selectedCard = null;
                refresh();
            }
        });

        // Set player table area
        tableBox.setFillHeight(true);
        tableBox.setAlignment(Pos.TOP_CENTER);
        tableScroll.setFitToWidth(true);
        tableScroll.setFitToHeight(true);
        tableScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tableScroll.setPannable(true);
        tableScroll.viewportBoundsProperty().addListener((_, _, bounds) -> resizeTableArea(bounds.getWidth(), bounds.getHeight()));
        tableScroll.setFocusTraversable(false);
        tableScroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        tableScroll.setOnMousePressed(e -> rootPane.requestFocus());

        refresh();
    }

    // Run when the user presses New Game button
    @FXML
    private void onNewGame() {
        List<String> names = askPlayerNames();

        // Player >= 2
        if (names == null || names.size() < 2) {
            return;
        }

        game.setup(names);
        statusText.setText("Game started.");
        refresh();
    }

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
            StackPane box = new StackPane(noCardBox("No cards yet", "No cards have been used or discarded yet"));
            box.setAlignment(Pos.CENTER);
            box.setPrefHeight(360);
            scrollPane.setContent(box);
        } else {
            VBox cardsBox = new VBox(10);
            cardsBox.setPadding(new Insets(10));
            cardsBox.setFillWidth(true);

            for (Game.UsedCard usedCard : usedCards) {
                cardsBox.getChildren().add(newUsedCardBox(usedCard));
            }

            scrollPane.setContent(cardsBox);
        }
        alert.getDialogPane().setContent(scrollPane);
        alert.showAndWait();
    }

    // Create a box for a used card
    private HBox newUsedCardBox(Game.UsedCard usedCard) {
        Card card = usedCard.card();

        // Set card name
        Label name = new Label(card.getName().replace("/", "/\u200B"));
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        name.setTextFill(Color.rgb(15, 23, 42));

        // Set card detail
        String detailText = cardDetail(card);
        Label detail = new Label(detailText);
        detail.setWrapText(true);
        detail.setTextFill(Color.rgb(71, 85, 105));

        VBox textBox;
        if (detailText.isEmpty()) {
            textBox = new VBox(4, name);
        } else {
            textBox = new VBox(4, name, detail);
        }
        textBox.setFillWidth(true);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        // Create a badge for a used card
        Label usedCardBadge;

        if (usedCard.action() == Game.CardAction.DISCARDED) {
            usedCardBadge = newBadge(usedCard.action().getLabel() + " by " + usedCard.player(), Color.rgb(254, 226, 226), Color.rgb(153, 27, 27));
        } else {
            usedCardBadge = newBadge(usedCard.action().getLabel() + " by " + usedCard.player(), Color.rgb(220, 252, 231), Color.rgb(22, 101, 52));
        }

        // Create a HBox to pack every element
        HBox box = new HBox(12, newUsedCardBar(card), textBox, usedCardBadge);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setFillHeight(true);
        box.setPadding(new Insets(12));
        box.setMaxWidth(Double.MAX_VALUE);
        box.setBackground(setSolidBackground(Color.WHITE));
        box.setBorder(roundCorner(Color.rgb(203, 213, 225)));
        return box;
    }

    private Region newUsedCardBar(Card card) {
        if (card instanceof WildPropertyCard wildCard && wildCard.getPossibleColors().size() == 2) {
            return newDoubleColorBar(wildCard.getPossibleColors().get(0), wildCard.getPossibleColors().get(1), 6, 44, true);
        }

        if (card instanceof ActionCard actionCard && actionCard.getColors().size() == 2) {
            return newDoubleColorBar(actionCard.getColors().get(0), actionCard.getColors().get(1), 6, 44, true);
        }

        Region bar = new Region();
        bar.setPrefSize(6, 44);
        bar.setMinWidth(6);
        bar.setBackground(setSolidBackground(cardColor(card)));
        return bar;
    }

    // Run when the user presses Play Selected button
    @FXML
    private void onPlaySelected() {
        if (!game.isStarted() || selectedCard == null) {
            return;
        }

        if (game.playCard(selectedCard,this)) {
            statusText.setText("Played " + statusCardText(selectedCard) + ".");
            selectedCard = null;
        } else {
            statusText.setText("Cannot play " + statusCardText(selectedCard) + ".");
        }

        refresh();
    }

    // Run when the user presses Move wild button
    @FXML
    private void onMoveWild() {
    }

    @FXML
    private void onEndTurn() {
        if (!game.isStarted()) {
            return;
        }

        if (game.endTurn(this)) {
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
        handCardsBox.getChildren().setAll(centeredEmptyBox(cardsScroll, "No cards yet", "Start a new game to render the active hand here"));
        tableBox.getChildren().setAll(centeredEmptyBox(tableScroll, "No players yet", "Create a game to see hand, bank, and property areas"));
        resizeHandArea(cardsScroll.getViewportBounds().getWidth(), cardsScroll.getViewportBounds().getHeight());
        resizeTableArea(tableScroll.getViewportBounds().getWidth(), tableScroll.getViewportBounds().getHeight());

        updateTurnCount(0);
        updateStatusBadge("Ready", Color.rgb(224, 231, 255), Color.rgb(165, 180, 252), Color.rgb(67, 56, 202));
    }

    // Show in-game UI. Important!
    private void showGame() {
        Player player = game.getCurrPlayer();
        statusTitle.setText("Status");
        handCardsTitle.setText(player.getName() + " Hand");
        updateActionsBadge(game.getActionsUsed());
        updateDeckCount();
        leadingPlayer.setText(findLeadingPlayer());
        handCardsBox.getChildren().clear();
        tableBox.getChildren().clear();
        updateHand(player);
        updateTable(game.getPlayers());
        updateTurnCount(game.getTurnCount());
        updateStatusBadge("Turn Active", Color.rgb(219, 234, 254), Color.rgb(147, 197, 253), Color.rgb(29, 78, 216));
    }

    // Pop a dialog to ask players' name
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

    // Find the player who have highest completed sets, then highest bank total
    private String findLeadingPlayer() {
        Player leader = null;
        int bestBank = -1;

        for (Player player : game.getPlayers()) {
            int bankTotal = player.getBankTotalValue();
            if (bankTotal > bestBank) {
                leader = player;
                bestBank = bankTotal;
            }
        }

        if (leader == null) {
            return "-";
        }

        return leader.getName();
    }

    // Update player's hand from backend
    private void updateHand(Player player) {
        handCardsBox.getChildren().clear();
        List<Card> handCards = player.getCardsAtHand();

        if (selectedCard != null && !handCards.contains(selectedCard)) {
            selectedCard = null;
        }

        if (handCards.isEmpty()) {
            handCardsBox.setAlignment(Pos.CENTER);
            handCardsBox.getChildren().add(centeredEmptyBox(cardsScroll, "No cards", "This player has no cards in hand"));
            resizeHandArea(cardsScroll.getViewportBounds().getWidth(), cardsScroll.getViewportBounds().getHeight());
            return;
        }

        handCardsBox.setAlignment(Pos.CENTER);
        for (Card handCard : handCards) {
            handCardsBox.getChildren().add(newHandCard(handCard));
        }
    }

    // Add a card row in the player's hand table
    private VBox newHandCard(Card card) {
        Label name = new Label(cardTitle(card));
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        name.setTextFill(Color.rgb(15, 23, 42));
        name.setWrapText(true);
        name.setMaxWidth(86);

        StackPane titleBox = new StackPane(name);
        titleBox.setMinHeight(Region.USE_PREF_SIZE);
        titleBox.setPadding(new Insets(0, 0, 3, 0));
        titleBox.setMaxWidth(Double.MAX_VALUE);
        titleBox.setAlignment(Pos.TOP_CENTER);
        name.setAlignment(Pos.CENTER);
        name.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        VBox textBox = new VBox(4);
        if (card instanceof MoneyCard) {
            textBox.getChildren().add(titleBox);
        } else {
            textBox.getChildren().addAll(titleBox, new Separator());
            if (card instanceof WildPropertyCard wildCard && wildCard.getPossibleColors().size() == 2) {
                textBox.getChildren().add(newWildRentBox(wildCard));
            } else if (card instanceof PropertyCard propertyCard) {
                textBox.getChildren().add(newPropertyDetailBox(propertyCard.getColor()));
            } else {
                String detailText = cardDetail(card);
                if (!detailText.isEmpty()) {
                    Label detail = new Label(detailText);
                    detail.setFont(Font.font("Segoe UI", 12));
                    detail.setTextFill(Color.rgb(71, 85, 105));
                    detail.setWrapText(true);
                    detail.setMaxWidth(86);
                    detail.setMinHeight(Region.USE_PREF_SIZE);
                    textBox.getChildren().add(detail);
                }
            }
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox cardBox = new VBox(6, textBox, spacer, newCardBar(card));
        cardBox.setAlignment(Pos.TOP_LEFT);
        cardBox.setPadding(new Insets(8));
        cardBox.setPrefSize(104, 154);
        cardBox.setMinSize(96, 146);
        cardBox.setMaxSize(112, 162);

        // Set focus
        if (selectedCard == card) {
            cardBox.setBackground(setSolidBackground(Color.rgb(239, 246, 255)));
            cardBox.setBorder(roundCorner(Color.rgb(37, 99, 235)));
        } else {
            cardBox.setBackground(setSolidBackground(Color.WHITE));
            cardBox.setBorder(roundCorner(cardColor(card)));
        }

        // If clicked again, remove focus
        cardBox.setOnMouseClicked(e -> {
            if (selectedCard == card) {
                selectedCard = null;
            } else {
                selectedCard = card;
            }
            refresh();
            e.consume();
        });

        return cardBox;
    }

    private String cardTitle(Card card) {
        if (card instanceof WildPropertyCard wildCard && wildCard.getPossibleColors().size() == 2) {
            PropertyColor leftColor = wildCard.getPossibleColors().get(0);
            PropertyColor rightColor = wildCard.getPossibleColors().get(1);
            return leftColor.getName() + "/\n" + rightColor.getName() + " Wild";
        }

        return card.getName().replace("/", "/\u200B");
    }

    private VBox newPropertyDetailBox(PropertyColor color) {
        Label colorName = newSmallCardText(color.getName());
        VBox box = new VBox(1, colorName);

        String[] rents = color.getRentDescription().split("\n");
        box.getChildren().add(newRentGrid(rents));

        return box;
    }

    private HBox newRentGrid(String[] rents) {
        VBox leftColumn = new VBox(0);
        VBox rightColumn = new VBox(0);
        int splitIndex = (rents.length + 1) / 2;

        for (int i = 0; i < splitIndex; i++) {
            leftColumn.getChildren().add(newSmallCardText(rents[i]));
        }

        for (int i = splitIndex; i < rents.length; i++) {
            Label rent = newSmallCardText(rents[i]);
            rent.setAlignment(Pos.CENTER_RIGHT);
            rightColumn.getChildren().add(rent);
        }

        leftColumn.setMaxWidth(Double.MAX_VALUE);
        rightColumn.setMaxWidth(Double.MAX_VALUE);
        leftColumn.setAlignment(Pos.TOP_LEFT);
        rightColumn.setAlignment(Pos.TOP_RIGHT);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        HBox rentGrid = new HBox(8, leftColumn, rightColumn);
        rentGrid.setMaxWidth(86);
        return rentGrid;
    }

    private Label newSmallCardText(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", 12));
        label.setTextFill(Color.rgb(71, 85, 105));
        label.setMaxWidth(86);
        return label;
    }

    private HBox newWildRentBox(WildPropertyCard wildCard) {
        PropertyColor leftColor = wildCard.getPossibleColors().get(0);
        PropertyColor rightColor = wildCard.getPossibleColors().get(1);

        Label leftRent = newSmallCardText(leftColor.getRentDescription());
        leftRent.setAlignment(Pos.CENTER_LEFT);
        leftRent.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(leftRent, Priority.ALWAYS);

        Label rightRent = newSmallCardText(rightColor.getRentDescription());
        rightRent.setAlignment(Pos.CENTER_RIGHT);
        rightRent.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(rightRent, Priority.ALWAYS);

        HBox rentBox = new HBox(leftRent, rightRent);
        rentBox.setMaxWidth(86);
        rentBox.setAlignment(Pos.CENTER);
        return rentBox;
    }

    // Add color bar for cards
    private Region newCardBar(Card card) {
        if (card instanceof WildPropertyCard wildCard && wildCard.getPossibleColors().size() == 2) {
            return newDoubleColorBar(wildCard.getPossibleColors().get(0), wildCard.getPossibleColors().get(1), 84, 6, false);
        }

        if (card instanceof ActionCard actionCard && actionCard.getColors().size() == 2) {
            return newDoubleColorBar(actionCard.getColors().get(0), actionCard.getColors().get(1), 84, 6, false);
        }

        if (card instanceof PropertyCard propertyCard) {
            return newCardBarSegment(propertyColor(propertyCard.getColor()));
        }

        return newCardBarSegment(cardColor(card));
    }

    private Region newDoubleColorBar(PropertyColor firstColor, PropertyColor secondColor,
                                     double width, double height, boolean vertical) {
        Color leftColor = propertyColor(firstColor);
        Color rightColor = propertyColor(secondColor);
        String direction;

        if (vertical) {
            direction = "to bottom";
        } else {
            direction = "to right";
        }

        Region bar = new Region();
        bar.setPrefSize(width, height);

        if (vertical) {
            bar.setMinWidth(width);
        } else {
            bar.setMinHeight(height);
            bar.setMaxHeight(height);
            bar.setMaxWidth(Double.MAX_VALUE);
        }

        bar.setStyle("-fx-background-color: linear-gradient(" + direction + ", "
                + cssColor(leftColor) + " 0%, "
                + cssColor(leftColor) + " 50%, "
                + cssColor(rightColor) + " 50%, "
                + cssColor(rightColor) + " 100%); -fx-background-radius: 12;");
        return bar;
    }

    // If the card has only 1 color, use this method
    private Region newCardBarSegment(Color color) {
        Region bar = new Region();
        bar.setPrefSize(84, 6);
        bar.setMinHeight(6);
        bar.setMaxHeight(6);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setBackground(setSolidBackground(color));
        return bar;
    }

    private String cardDetail(Card card) {
        return switch (card) {
            case PropertyCard propertyCard -> propertyCard.getColor().getName() + "\n" + propertyCard.getColor().getRentDescription();
            case WildPropertyCard wildCard -> {
                String currentColor;
                if (wildCard.getCurrentColor() == null) {
                    currentColor = "Not selected";
                } else {
                    currentColor = wildCard.getCurrentColor().getName();
                }
                yield currentColor;
            }
            case ActionCard actionCard -> actionDetail(actionCard.getActionType());
            default -> "";
        };
    }

    private String statusCardText(Card card) {
        return switch (card) {
            case MoneyCard moneyCard -> moneyCard.getName();
            case PropertyCard _ -> "property card";
            case WildPropertyCard _ -> "wild card";
            case ActionCard actionCard -> actionCard.getName();
            case null, default -> "card";
        };
    }

    private String actionDetail(ActionType actionType) {
        return switch (actionType) {
            case TODAY_IS_MY_BIRTHDAY -> "All players pay you 2M.";
            case PASS_GO -> "Draw 2 extra cards.";
            case DEBT_COLLECTOR -> "Force any player to pay you 5M.";
            case RENT -> "All players pay rent for one listed color.";
            case MULTI_RENT -> "One player pays rent for any one color.";
            case DOUBLE_RENT -> "Play with a rent card to double rent.";
            case SLY_DEAL -> "Steal one property. Not from a full set.";
            case FORCED_DEAL -> "Swap one property. Not from a full set.";
            case DEAL_BREAKER -> "Steal a complete property set.";
            case HOUSE -> "Add to a full set to add 3M rent.";
            case HOTEL -> "Add to a full set with a house to add 4M rent.";
            case JUST_SAY_NO -> "Cancel an action played against you.";
        };
    }

    private Color cardColor(Card card) {
        return switch (card) {
            case MoneyCard _ -> Color.rgb(22, 163, 74);
            case PropertyCard propertyCard -> propertyColor(propertyCard.getColor());
            case WildPropertyCard wildCard -> {
                if (wildCard.getCurrentColor() == null) {
                    if (wildCard.getPossibleColors().size() == 2) {
                        yield propertyColor(wildCard.getPossibleColors().get(0));
                    }
                    yield Color.rgb(8, 145, 178);
                }
                yield propertyColor(wildCard.getCurrentColor());
            }
            case ActionCard actionCard -> {
                if (actionCard.getColors().size() == 2) {
                    yield propertyColor(actionCard.getColors().get(0));
                }
                yield Color.rgb(217, 119, 6);
            }
            case null, default -> Color.rgb(100, 116, 139);
        };
    }

    private Color propertyColor(PropertyColor color) {
        return switch (color) {
            case BROWN -> Color.rgb(120, 72, 45);
            case LIGHT_BLUE -> Color.rgb(56, 189, 248);
            case PINK -> Color.rgb(236, 72, 153);
            case ORANGE -> Color.rgb(249, 115, 22);
            case RED -> Color.rgb(220, 38, 38);
            case YELLOW -> Color.rgb(234, 179, 8);
            case GREEN -> Color.rgb(22, 163, 74);
            case DARK_BLUE -> Color.rgb(30, 64, 175);
            case RAILROAD -> Color.rgb(71, 85, 105);
            case UTILITY -> Color.rgb(20, 184, 166);
        };
    }

    private String cssColor(Color color) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return "rgb(" + red + "," + green + "," + blue + ")";
    }

    private void updateTable(List<Player> players) {
        tableBox.getChildren().clear();

        if (players.isEmpty()) {
            tableBox.setAlignment(Pos.CENTER);
            tableBox.getChildren().add(centeredEmptyBox(tableScroll, "No players", "Start a new game to show player boards"));
            resizeTableArea(tableScroll.getViewportBounds().getWidth(), tableScroll.getViewportBounds().getHeight());
            return;
        }

        tableBox.setAlignment(Pos.TOP_CENTER);
        for (Player player : players) {
            VBox playerBox = newPlayerBox(player);
            HBox.setHgrow(playerBox, Priority.ALWAYS);
            tableBox.getChildren().add(playerBox);
        }
    }

    // Create a player box for each player
    private VBox newPlayerBox(Player player) {
        boolean isCurrent = (player == game.getCurrPlayer());

        Label name = new Label(player.getName());
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        name.setTextFill(Color.rgb(15, 23, 42));

        // Add summary
        HBox summaryBox = new HBox(
                8,
                newBadge("Hand " + player.getCardsAtHand().size()),
                newBadge("Bank " + player.getBankTotalValue() + "M"),
                newBadge("Sets 0/3")
        );
        summaryBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, name, spacer, summaryBox);
        header.setAlignment(Pos.CENTER_LEFT);

        // Show cards in bank
        VBox bank = new VBox(4);
        Label bankTitle = new Label("Bank");
        bankTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        bankTitle.setTextFill(Color.rgb(71, 85, 105));
        bank.getChildren().add(bankTitle);

        if (player.getCardsAtBank().isEmpty()) {
            Label emptyBank = new Label("Bank is empty.");
            emptyBank.setWrapText(true);
            emptyBank.setTextFill(Color.rgb(100, 116, 139));
            bank.getChildren().add(emptyBank);
        } else {
            FlowPane bankCards = new FlowPane(6, 6);
            bankCards.setPrefWrapLength(320);
            for (Card card : player.getCardsAtBank()) {
                bankCards.getChildren().add(newBankBox(card));
            }
            bank.getChildren().add(bankCards);
        }

        // Add property section
        VBox properties = new VBox(4);
        Label propertiesTitle = new Label("Properties");
        propertiesTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        propertiesTitle.setTextFill(Color.rgb(71, 85, 105));

        Label emptyProperties = new Label("No properties in play.");
        emptyProperties.setWrapText(true);
        emptyProperties.setTextFill(Color.rgb(100, 116, 139));
        properties.getChildren().addAll(propertiesTitle, emptyProperties);

        VBox box = new VBox(8, header, new Separator(), bank, properties);
        box.setPadding(new Insets(12));
        box.setMinWidth(0);
        box.setPrefHeight(180);
        box.setMinHeight(165);
        box.setMaxHeight(Double.MAX_VALUE);
        box.setMaxWidth(Double.MAX_VALUE);

        // Add focus for player
        if (isCurrent) {
            box.setBackground(setSolidBackground(Color.rgb(240, 253, 244)));
            box.setBorder(roundCorner(Color.rgb(34, 197, 94)));
        } else {
            box.setBackground(setSolidBackground(Color.WHITE));
            box.setBorder(roundCorner(Color.rgb(203, 213, 225)));
        }

        return box;
    }

    // Create a small card box for bank
    private HBox newBankBox(Card card) {
        Label name = new Label(card.getName());
        name.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        name.setTextFill(Color.rgb(15, 23, 42));

        Region bar = newBankCardBar(card);

        HBox box = new HBox(8, bar, name);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5, 10, 5, 9));
        box.setMinWidth(62);
        box.setBackground(setSolidBackground(Color.WHITE));
        box.setBorder(roundCorner(Color.rgb(203, 213, 225)));
        return box;
    }

    // Add color bar for small card in bank
    private Region newBankCardBar(Card card) {
        if (card instanceof WildPropertyCard wildCard && wildCard.getPossibleColors().size() >= 2) {
            return newDoubleColorBar(wildCard.getPossibleColors().get(0), wildCard.getPossibleColors().get(1), 4, 20, true);
        }

        Region bar = new Region();
        bar.setPrefSize(4, 20);
        bar.setMinWidth(4);
        bar.setBackground(setSolidBackground(cardColor(card)));
        return bar;
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

    // Enable/diable a button
    private void updateButtonStatus() {
        // Play selected card is available when,
        // the game has started, has selected a card, has not used all 3 actions
        playButton.setDisable(!game.isStarted() || selectedCard == null || game.getActionsUsed() >= Player.MAX_ACTIONS_PER_TURN);
        moveWildButton.setDisable(true);
        endTurnButton.setDisable(!game.isStarted());
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
        actions.setBackground(setSolidBackground(background));
        actions.setBorder(roundCorner(border));
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
        statusBadge.setBackground(setSolidBackground(background));
        statusBadge.setBorder(roundCorner(border));
        statusBadge.setTextFill(foreground);
    }

    private void resizeHandArea(double width, double height) {
        handCardsBox.setMinWidth(width);
        handCardsBox.setMinHeight(height);
    }

    private void resizeTableArea(double width, double height) {
        tableBox.setMinWidth(width);
        tableBox.setMinHeight(height);
    }

    // Add a box in table when there is no player/card
    private VBox noCardBox(String titleText, String bodyText) {
        Label title = new Label(titleText);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        Label body = new Label(bodyText);
        body.setWrapText(true);
        body.setTextFill(Color.rgb(100, 116, 139));
        body.setMaxWidth(Double.MAX_VALUE);
        body.setAlignment(Pos.CENTER);
        body.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        VBox box = new VBox(8, title, body);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(18));
        box.setPrefSize(420, 120);
        box.setMinSize(420, 120);
        box.setMaxSize(420, 120);
        box.setBackground(setSolidBackground(Color.WHITE));
        box.setBorder(roundCorner(Color.rgb(203, 213, 225)));
        return box;
    }

    private StackPane centeredEmptyBox(ScrollPane scrollPane, String titleText, String bodyText) {
        StackPane wrapper = new StackPane(noCardBox(titleText, bodyText));
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMinHeight(140);
        wrapper.prefWidthProperty().bind(scrollPane.widthProperty().subtract(32));
        return wrapper;
    }

    // Apply unified style for all buttons
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

        VBox optionsBox = new VBox(8);
        optionsBox.setPadding(new Insets(8));

        List<CheckBox> checkBoxes = new ArrayList<>();
        for (Card card : cards) {
            CheckBox checkBox = new CheckBox(discardOptionText(card));
            checkBox.setWrapText(true);
            checkBox.setMaxWidth(Double.MAX_VALUE);
            checkBox.selectedProperty().addListener((event, oldValue, newValue) ->
                    updateDiscardChecks(checkBoxes, okButton, count));
            checkBoxes.add(checkBox);
            optionsBox.getChildren().add(checkBox);
        }

        ScrollPane scrollPane = new ScrollPane(optionsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefSize(420, 280);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
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

    private void updateDiscardChecks(List<CheckBox> checkBoxes, Button okButton, int count) {
        int selectedCount = 0;
        for (CheckBox checkBox : checkBoxes) {
            if (checkBox.isSelected()) {
                selectedCount++;
            }
        }

        okButton.setDisable(selectedCount != count);

        boolean limitReached = selectedCount >= count;
        for (CheckBox checkBox : checkBoxes) {
            checkBox.setDisable(limitReached && !checkBox.isSelected());
        }
    }

    private String discardOptionText(Card card) {
        return card.getName();
    }

    @Override public Card selectPropertyCard(Player owner, List<Card> cards, String prompt) { return null; }
    @Override public Card selectPaymentCard(Player owner, List<Card> cards, String prompt) { return null; }
    @Override public boolean reconfirm(String prompt) { return false; }
}
