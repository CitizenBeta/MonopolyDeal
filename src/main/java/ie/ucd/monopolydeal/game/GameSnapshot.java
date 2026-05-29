package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Captures mutable game state so canceled multistep actions can roll back cleanly
public final class GameSnapshot {
    private final int currentPlayerIndex;
    private final int actionsUsed;
    private final int turnCount;
    private final boolean started;
    private final boolean gameOver;
    private final List<CardHistory.UsedCard> usedCards;
    private final List<PlayerSnapshot> players;

    public GameSnapshot(int currentPlayerIndex, int actionsUsed, int turnCount, boolean started,
                        boolean gameOver, List<CardHistory.UsedCard> usedCards, List<Player> players) {
        this.currentPlayerIndex = currentPlayerIndex;
        this.actionsUsed = actionsUsed;
        this.turnCount = turnCount;
        this.started = started;
        this.gameOver = gameOver;
        this.usedCards = new ArrayList<>(usedCards);
        this.players = new ArrayList<>();

        for (Player player : players) {
            this.players.add(new PlayerSnapshot(player));
        }
    }

    public void restore(Game game) {
        game.restoreSnapshot(currentPlayerIndex, actionsUsed, turnCount, started, gameOver, usedCards);

        for (PlayerSnapshot snapshot : players) {
            snapshot.restore();
        }
    }

    private static final class PlayerSnapshot {
        private final Player player;
        private final List<Card> hand;
        private final List<Card> bank;
        private final Map<PropertyColor, PropertySetSnapshot> propertySets;
        private final Map<WildPropertyCard, PropertyColor> wildColors;

        private PlayerSnapshot(Player player) {
            this.player = player;
            hand = new ArrayList<>(player.getCardsAtHand());
            bank = new ArrayList<>(player.getCardsAtBank());
            propertySets = new EnumMap<>(PropertyColor.class);
            wildColors = new HashMap<>();

            rememberWildColors(player.getCardsAtHand());
            rememberWildColors(player.getCardsAtBank());

            for (Map.Entry<PropertyColor, PropertySet> entry : player.getPropertySets().entrySet()) {
                PropertySet set = entry.getValue();
                propertySets.put(entry.getKey(), new PropertySetSnapshot(
                        new ArrayList<>(set.getCards()),
                        set.getHouseCard(),
                        set.getHotelCard()
                ));
                rememberWildColors(set.getAllCards());
            }
        }

        // Wild cards have mutable color state, so the selected color must be saved separately
        private void rememberWildColors(List<Card> cards) {
            for (Card card : cards) {
                if (card instanceof WildPropertyCard wildCard) {
                    wildColors.put(wildCard, wildCard.getCurrentColor());
                }
            }
        }

        private void restore() {
            player.restoreCards(hand, bank);

            for (Map.Entry<PropertyColor, PropertySetSnapshot> entry : propertySets.entrySet()) {
                PropertySet set = player.getPropertySets().get(entry.getKey());
                PropertySetSnapshot snapshot = entry.getValue();
                set.restore(new ArrayList<>(snapshot.cards()), snapshot.houseCard(), snapshot.hotelCard());
            }

            for (Map.Entry<WildPropertyCard, PropertyColor> entry : wildColors.entrySet()) {
                entry.getKey().setCurrentColor(entry.getValue());
            }
        }
    }

    private record PropertySetSnapshot(List<Card> cards, ActionCard houseCard, ActionCard hotelCard) {
    }
}
