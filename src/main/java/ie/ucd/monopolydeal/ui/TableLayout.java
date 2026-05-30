package ie.ucd.monopolydeal.ui;

import ie.ucd.monopolydeal.game.Game;
import ie.ucd.monopolydeal.model.Player;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.List;

// Handles player-table sizing and horizontal scrolling
public final class TableLayout {
    // Show at most three players before horizontal scrolling
    private static final int MAX_VISIBLE_PLAYERS = 3;

    private TableLayout() {
    }

    static void configure(HBox tableBox, ScrollPane tableScroll, BorderPane rootPane, Runnable resizeTable) {
        // Configure horizontally scrolling player table
        tableBox.setFillHeight(true);
        tableBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        tableScroll.setFitToWidth(false);
        tableScroll.setFitToHeight(false);
        tableScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tableScroll.setPannable(true);

        // Resize player boards when window changes
        tableScroll.viewportBoundsProperty().addListener((observable, oldBounds, newBounds) -> resizeTable.run());
        tableScroll.setFocusTraversable(false);
        tableScroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        tableScroll.setOnMousePressed(e -> rootPane.requestFocus());
    }

    // Show up to three player boards before scrolling
    static void resize(Game game, HBox tableBox, ScrollPane tableScroll) {
        int playerCount = 0;
        if (game.isStarted()) {
            playerCount = game.getPlayers().size();
        }

        double width = tableScroll.getViewportBounds().getWidth();
        double height = tableScroll.getViewportBounds().getHeight();
        double contentWidth = tableContentWidth(tableBox, width, playerCount);

        // Make table wider than viewport only when more than three players exist
        tableBox.setMinWidth(contentWidth);
        tableBox.setPrefWidth(contentWidth);
        tableBox.setMinHeight(height);

        // Apply equal width and height to all visible player boards
        double playerWidth = tablePlayerWidth(tableBox, width, playerCount);
        for (javafx.scene.Node child : tableBox.getChildren()) {
            if (child instanceof Region region) {
                if (playerCount > 0 && !region.prefWidthProperty().isBound()) {
                    region.setMinWidth(playerWidth);
                    region.setPrefWidth(playerWidth);
                    region.setMaxWidth(playerWidth);
                }
                region.setMinHeight(playerBoxMinHeight(tableScroll));
            }
        }
    }

    // Scroll table to keep current player visible
    static void scrollToCurrentPlayer(Game game, HBox tableBox, ScrollPane tableScroll) {
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
    static double playerBoxMinHeight(ScrollPane tableScroll) {
        double viewportHeight = tableScroll.getViewportBounds().getHeight();
        if (viewportHeight <= 0) {
            return 165;
        }
        return Math.max(165, viewportHeight - 18);
    }

    // Calculate one player board width
    private static double tablePlayerWidth(HBox tableBox, double viewportWidth, int playerCount) {
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
    private static double tableContentWidth(HBox tableBox, double viewportWidth, int playerCount) {
        if (playerCount <= 0 || viewportWidth <= 0) {
            return viewportWidth;
        }

        double playerWidth = tablePlayerWidth(tableBox, viewportWidth, playerCount);
        Insets padding = tableBox.getPadding();
        double spacing = tableBox.getSpacing() * Math.max(0, playerCount - 1);
        double contentWidth = padding.getLeft() + padding.getRight() + playerWidth * playerCount + spacing;
        return Math.max(viewportWidth, contentWidth);
    }
}
