package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.Card;
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
            players.add(new Player(names.get(i), i + 1));
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

    public int getCurrentPlayerBankTotal() {
        int total = 0;
        for (Card card : getCurrentPlayer().getCardsAtBank()) {
            total += card.getBankValue();
        }
        return total;
    }
}
