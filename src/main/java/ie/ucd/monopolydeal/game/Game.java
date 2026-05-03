package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.Card;
import ie.ucd.monopolydeal.model.MoneyCard;
import ie.ucd.monopolydeal.model.Player;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private final List<Player> players = new ArrayList<>();
    private int currentPlayerIndex;
    private int actionsUsed;
    private boolean started;

    public void setup(List<String> names) {
        players.clear();
        currentPlayerIndex = 0;
        actionsUsed = 0;
        started = true;

        for (int i = 0; i < names.size(); i++) {
            Player player = new Player(names.get(i), i + 1);
            player.addCardToHand(new MoneyCard("1M", 1));
            player.addCardToHand(new MoneyCard("2M", 2));
            player.addCardToHand(new MoneyCard("3M", 3));
            players.add(player);
        }
    }

    public boolean isStarted() {
        return started;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public int getActionsUsed() {
        return actionsUsed;
    }

    public boolean playCard(Card card) {
        if (!started || card == null) {
            return false;
        }

        Player current = getCurrentPlayer();
        if (!current.getCardsAtHand().contains(card)) {
            return false;
        }

        if (actionsUsed >= Player.MAX_ACTIONS_PER_TURN) {
            return false;
        }

        current.addCardToBank(card);
        actionsUsed++;
        return true;
    }

    public int getCurrentPlayerBankTotal() {
        int total = 0;
        for (Card card : getCurrentPlayer().getCardsAtBank()) {
            total += card.getBankValue();
        }
        return total;
    }

    public void endTurn() {
        if (!started || players.isEmpty()) {
            return;
        }

        currentPlayerIndex++;
        if (currentPlayerIndex >= players.size()) {
            currentPlayerIndex = 0;
        }

        actionsUsed = 0;
    }
}
