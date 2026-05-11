package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.Card;
import ie.ucd.monopolydeal.model.MoneyCard;
import ie.ucd.monopolydeal.model.Player;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private List<Player> players = new ArrayList<>();
    private int currentPlayerIndex;
    private int actionsUsed;
    private int turnCount;
    private boolean started;
    private Deck deck = new Deck() ;
    private List<String> log = new ArrayList<>();
    private boolean gameOver;
    private int turn;
    public void setup(List<String> names) {
        players.clear();
        currentPlayerIndex = 0;
        actionsUsed = 0;
        turnCount = 1;
        started = true;
        gameOver = false;
        turn = 1;

        for (int i = 0; i < names.size(); i++) {
            Player player = new Player(names.get(i), i + 1);
            player.addCardToHand(new MoneyCard("1M", 1));
            player.addCardToHand(new MoneyCard("2M", 2));
            player.addCardToHand(new MoneyCard("3M", 3));
            drawCards(player,2);
            players.add(player);
        }
    }


    public List<String> getLog(){
        return log;
    }

    public boolean isOver(){
        return gameOver;
    }

    public int getTurn(){
        return turn;
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
        return true;
    }

    public void playSpecificCard(Player player, Card card, DecisionMaker dm){
        player.addCardToBank(card);
    }

    public int getCurrBankTotal() {
        int total = 0;
        for (Card card : getCurrPlayer().getCardsAtBank()) {
            total += card.getBankValue();
        }
        return total;
    }

    public void endTurn(DecisionMaker dm) {
        if (!started || players.isEmpty()) {
            return;
        }
        Player currPlayer = getCurrPlayer();
        while(currPlayer.getCardsAtHand().size()>Player.MAX_CARDS_AT_HAND){
            Card discard = dm.selectDiscard(currPlayer,currPlayer.getCardsAtHand());
            if(discard == null){
                discard = currPlayer.getCardsAtHand().getLast();
            }
            currPlayer.removeCardFromHand(discard);
        }

        currentPlayerIndex++;
        if (currentPlayerIndex >= players.size()) {
            currentPlayerIndex = 0;
        }

        actionsUsed = 0;
        turnCount++;

        if(turn == 1){
            turn ++;
            return;
        }

        int drawCardsNumber;
        if(getCurrPlayer().getCardsAtHand().isEmpty()){
            drawCardsNumber = 5;
        }else{
            drawCardsNumber = 2;
        }
        drawCards(getCurrPlayer(),drawCardsNumber);
        turn++;
    }

    private void drawCards(Player player, int number){
        for(int i = 0; i < number; i++){
           Card card = deck.draw();
           if(card!=null){
               player.addCardToHand(card);
           }
        }
    }



}
