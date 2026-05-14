package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private List<Player> players = new ArrayList<>();
    private int currentPlayerIndex;
    private int actionsUsed;
    private int turnCount;
    private boolean started;
    private Deck deck = new Deck() ;
    private List<UsedCard> usedCards = new ArrayList<>();
    private boolean gameOver;

    public void setup(List<String> names) {
        players.clear();
        usedCards.clear();
        deck = new Deck();
        currentPlayerIndex = 0;
        actionsUsed = 0;
        turnCount = 0;
        started = true;
        gameOver = false;

        for (int i = 0; i < names.size(); i++) {
            Player player = new Player(names.get(i), i + 1);
            player.addCardToHand(new MoneyCard("1M", 1));
            player.addCardToHand(new MoneyCard("2M", 2));
            player.addCardToHand(new MoneyCard("3M", 3));
            drawCards(player,2);
            players.add(player);
        }

        startTurn();
    }

    public List<UsedCard> getUsedCards() {
        return new ArrayList<>(usedCards);
    }

    public boolean isOver(){
        return gameOver;
    }

    public boolean isStarted() {
        return started;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getCurrPlayer() {
        return players.get(currentPlayerIndex);
    }

    public int getActionsUsed() {
        return actionsUsed;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public int getDrawPileNumber() {
        return deck.getDrawPileNumber();
    }

    public int getTotalCardNumber() {
        return deck.getTotalCardNumber();
    }

    public List<Player> getOtherPlayers(){
        List<Player> otherPlayers = new ArrayList<>();
        otherPlayers.addAll(getPlayers());
        otherPlayers.remove(getCurrPlayer());
        return otherPlayers;
    }

    public boolean playCard(Card card, DecisionMaker dm) {
        if (!started || card == null) {
            return false;
        }

        Player current = getCurrPlayer();
        if (!current.getCardsAtHand().contains(card)) {
            return false;
        }

        if (actionsUsed >= Player.MAX_ACTIONS_PER_TURN) {
            return false;
        }

        playSpecificCard(current,card,dm);
        actionsUsed++;
        current.removeCardFromHand(card);
        addUsedCard(current, card, CardAction.PLAYED);
        return true;
    }

    public boolean playSpecificCard(Player player, Card card, DecisionMaker dm){
        if(card instanceof MoneyCard) {
            player.addCardToBank(card);
            return true;
        }
        if(card instanceof PropertyCard){

        }
        return true;
    }

    public int getCurrBankTotal() {
        int total = 0;
        for (Card card : getCurrPlayer().getCardsAtBank()) {
            total += card.getBankValue();
        }
        return total;
    }

    public boolean endTurn(DecisionMaker dm) {
        if (!started || players.isEmpty()) {
            return false;
        }
        Player currPlayer = getCurrPlayer();
        int discardCount = currPlayer.getCardsAtHand().size() - Player.MAX_CARDS_AT_HAND;

        if (discardCount > 0) {
            List<Card> discards = dm.selectDiscards(currPlayer, currPlayer.getCardsAtHand(), discardCount);
            if (discards == null || discards.size() != discardCount) {
                return false;
            }

            for (Card discard : discards) {
                if (!currPlayer.getCardsAtHand().contains(discard)) {
                    return false;
                }
            }

            for (Card discard : discards) {
                currPlayer.removeCardFromHand(discard);
                deck.discard(discard);
                addUsedCard(currPlayer, discard, CardAction.DISCARDED);
            }
        }

        currentPlayerIndex++;
        if (currentPlayerIndex >= players.size()) {
            currentPlayerIndex = 0;
        }

        actionsUsed = 0;
        startTurn();
        return true;
    }

    private void startTurn() {
        Player player = getCurrPlayer();
        int drawCardsNumber = 2;

        if (player.getCardsAtHand().isEmpty()) {
            drawCardsNumber = 5;
        }

        drawCards(player, drawCardsNumber);
        if (currentPlayerIndex == 0) {
            turnCount++;
        }
    }

    private void drawCards(Player player, int number){
        for(int i = 0; i < number; i++){
           Card card = deck.draw();
           if(card!=null){
               player.addCardToHand(card);
           }
        }
    }

    private void addUsedCard(Player player, Card card, CardAction action) {
        usedCards.addFirst(new UsedCard(action, player.getName(), card));
    }

    public enum CardAction {
        PLAYED("Played"),
        DISCARDED("Discarded");

        private final String label;

        CardAction(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public record UsedCard(CardAction action, String player, Card card) {}
}
