package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Game {
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 5;

    private static final int INITIAL_HAND_SIZE = 5;
    private static final int NORMAL_TURN_DRAW = 2;

    private List<Player> players = new ArrayList<>();
    private int currentPlayerIndex;
    private int actionsUsed;
    private int turnCount;
    private boolean started;
    private Deck deck = new Deck();
    private final CardHistory cardHistory = new CardHistory();
    private final Payment payments = new Payment(this);
    private final ActionResolver actionResolver = new ActionResolver(this, payments);
    private boolean gameOver;

    // Starts a fresh game and deals the initial cards.
    // This method resets all runtime state, so it should only be called when
    // starting or restarting a game.
    public void setup(List<String> names) {
        if (names == null || names.size() < MIN_PLAYERS || names.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException("Monopoly Deal requires 2 to 5 players.");
        }

        resetState();
        for (int i = 0; i < names.size(); i++) {
            Player player = new Player(names.get(i), i + 1);
            drawCards(player, INITIAL_HAND_SIZE);
            players.add(player);
        }

        startTurn();
    }

    // Returns a defensive copy so callers cannot directly edit the history list.
    public List<CardHistory.UsedCard> getUsedCards() {
        return cardHistory.getUsedCards();
    }

    public boolean isOver() {
        return gameOver;
    }

    public Player getWinner() {
        for (Player player : players) {
            if (player.hasWon()) {
                return player;
            }
        }
        return null;
    }

    public boolean isStarted() {
        return started;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
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

    // Returns all players except the current one.
    // Many action cards use this helper to find valid targets.
    public List<Player> getOtherPlayers() {
        List<Player> otherPlayers = new ArrayList<>(players);
        otherPlayers.remove(getCurrPlayer());
        return otherPlayers;
    }

    // Attempts to play one card from the current player's hand.
    // If a later decision fails, the game is restored to the state before the card was played.
    public boolean playCard(Card card, DecisionMaker dm) {
        if (!canTryCardFromCurrentHand(card)) {
            return false;
        }

        Player current = getCurrPlayer();
        GameSnapshot snapshot = new GameSnapshot(
                currentPlayerIndex,
                actionsUsed,
                turnCount,
                started,
                gameOver,
                cardHistory.getUsedCards(),
                players
        );

        // A card action can ask the user for several decisions. Roll back if any step is cancelled.
        if (!playSpecificCard(current, card, dm)) {
            snapshot.restore(this);
            return false;
        }

        finishPlayedCard(current, card);
        return true;
    }

    // Applies the actual effect of a card.
    // Public visibility is kept because other parts of the project may call this method directly.
    public boolean playSpecificCard(Player player, Card card, DecisionMaker dm) {
        if (card instanceof MoneyCard) {
            player.addCardToBank(card);
            return true;
        }

        if (card instanceof PropertyCard propertyCard) {
            return player.addProperty(propertyCard);
        }

        if (card instanceof WildPropertyCard wildCard) {
            PropertyColor color = chooseSingleOrPrompt(
                    wildCard.getPossibleColors(),
                    dm,
                    "Choose a color for " + wildCard.getName()
            );
            return color != null && player.addWildProperty(wildCard, color);
        }

        if (card instanceof ActionCard actionCard) {
            return playActionCard(player, actionCard, dm);
        }

        return false;
    }

    // Checks only whether the card can be started legally.
    // Some action cards may still fail later if the player cancels a decision or no target exists.
    public boolean canPlayCard(Card card) {
        if (!canTryCardFromCurrentHand(card)) {
            return false;
        }

        Player current = getCurrPlayer();

        if (card instanceof PropertyCard || card instanceof WildPropertyCard) {
            return payments.canReceiveCard(current, card);
        }

        // Action cards are allowed here because the player may still choose to bank them
        return card instanceof MoneyCard || card instanceof ActionCard;
    }

    // Check whether an action card can resolve as an action
    public boolean canPlayActionCard(ActionCard action) {
        if (!canTryCardFromCurrentHand(action)) {
            return false;
        }

        return canResolveActionCard(getCurrPlayer(), action);
    }

    public int getCurrBankTotal() {
        return getCurrPlayer().getBankTotalValue();
    }

    // Ends the current turn after forcing the player to discard down to the hand limit.
    // The next player's draw happens inside startTurn().
    public boolean endTurn(DecisionMaker dm) {
        if (!started || players.isEmpty() || gameOver) {
            return false;
        }

        if (updateGameOver()) {
            return true;
        }

        Player current = getCurrPlayer();
        int discardCount = current.getCardsAtHand().size() - Player.MAX_CARDS_AT_HAND;

        if (discardCount > 0 && !discardExtraCards(current, discardCount, dm)) {
            return false;
        }

        advanceToNextPlayer();
        startTurn();
        return true;
    }

    // Clears all mutable game state before a new game starts.
    // Keeping this in one place avoids missing fields when setup() changes later.
    private void resetState() {
        players.clear();
        cardHistory.clear();
        deck = new Deck();
        currentPlayerIndex = 0;
        actionsUsed = 0;
        turnCount = 0;
        started = true;
        gameOver = false;
    }

    // Common guard for playCard() and canPlayCard().
    // It prevents repeated null, game-over, action-limit and ownership checks.
    private boolean canTryCardFromCurrentHand(Card card) {
        return started
                && !gameOver
                && card != null
                && !players.isEmpty()
                && actionsUsed < Player.MAX_ACTIONS_PER_TURN
                && getCurrPlayer().getCardsAtHand().contains(card);
    }

    // Final bookkeeping after a card effect succeeds.
    // The card is removed only after the effect has been accepted.
    private void finishPlayedCard(Player player, Card card) {
        actionsUsed++;
        player.removeCardFromHand(card);
        addUsedCard(player, card, CardHistory.CardAction.PLAYED);
        updateGameOver();
    }

    private boolean updateGameOver() {
        gameOver = getWinner() != null;
        return gameOver;
    }

    // Handles action cards after the player chooses whether to use them as money or as actions.
    // Each action type is delegated to a small method to keep the switch easy to read.
    private boolean playActionCard(Player player, ActionCard action, DecisionMaker dm) {
        return actionResolver.playActionCard(player, action, dm);
    }

    private boolean canResolveActionCard(Player player, ActionCard action) {
        return actionResolver.canResolveActionCard(player, action);
    }

    private PropertyColor chooseSingleOrPrompt(List<PropertyColor> colors, DecisionMaker dm, String prompt) {
        if (colors.isEmpty()) {
            return null;
        }

        if (colors.size() == 1) {
            return colors.get(0);
        }

        return dm.selectColor(prompt, colors);
    }

    // Resolves a Just Say No chain.
    // If the target says no, the actor gets a chance to counter with another Just Say No.
    public boolean isBlockedByJustSayNo(Player target, Player actor, DecisionMaker dm) {
        ActionCard justSayNo = findJustSayNo(target);
        if (justSayNo == null) {
            return false;
        }

        if (!dm.reconfirm(target.getName() + ": use Just Say No against " + actor.getName() + "?")) {
            return false;
        }

        target.removeCardFromHand(justSayNo);
        addUsedCard(target, justSayNo, CardHistory.CardAction.PLAYED);

        // Just Say No can be countered by another Just Say No, so keep checking both sides.
        return !isBlockedByJustSayNo(actor, target, dm);
    }

    private ActionCard findJustSayNo(Player player) {
        for (Card card : player.getCardsAtHand()) {
            if (card instanceof ActionCard actionCard && actionCard.getActionType() == ActionType.JUST_SAY_NO) {
                return actionCard;
            }
        }

        return null;
    }

    // Moves discarded cards to the bottom of the draw pile and records them in history.
    private boolean discardExtraCards(Player player, int discardCount, DecisionMaker dm) {
        List<Card> discards = dm.selectDiscards(player, player.getCardsAtHand(), discardCount);
        if (!isValidDiscardSelection(player, discards, discardCount)) {
            return false;
        }

        for (Card discard : discards) {
            player.removeCardFromHand(discard);
            deck.putAtDrawPileBottom(discard);
            addUsedCard(player, discard, CardHistory.CardAction.DISCARDED);
        }

        return true;
    }

    // Ensures the discard choice has the right count, belongs to the player and contains no duplicates
    private boolean isValidDiscardSelection(Player player, List<Card> discards, int expectedCount) {
        if (discards == null || discards.size() != expectedCount) {
            return false;
        }

        List<Card> checkedCards = new ArrayList<>();
        for (Card discard : discards) {
            if (!player.getCardsAtHand().contains(discard) || checkedCards.contains(discard)) {
                return false;
            }
            checkedCards.add(discard);
        }

        return true;
    }

    // Advances the turn pointer and resets the per-turn action counter.
    private void advanceToNextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        actionsUsed = 0;
    }

    // Draws cards for the active player.
    // A player with an empty hand draws five cards; otherwise they draw two.
    private void startTurn() {
        Player player = getCurrPlayer();
        int cardsToDraw = player.getCardsAtHand().isEmpty() ? INITIAL_HAND_SIZE : NORMAL_TURN_DRAW;

        drawCards(player, cardsToDraw);
        if (currentPlayerIndex == 0) {
            turnCount++;
        }
    }

    // Draws up to the requested number of cards.
    // If the deck runs out, drawing simply stops.
    void drawCards(Player player, int number) {
        for (int i = 0; i < number; i++) {
            Card card = deck.draw();
            if (card != null) {
                player.addCardToHand(card);
            }
        }
    }

    void addUsedCard(Player player, Card card, CardHistory.CardAction action) {
        cardHistory.add(player, card, action);
    }

    void addActionUsed() {
        actionsUsed++;
    }

    public void restoreSnapshot(int currentPlayerIndex, int actionsUsed, int turnCount,
                                boolean started, boolean gameOver, List<CardHistory.UsedCard> usedCards) {
        this.currentPlayerIndex = currentPlayerIndex;
        this.actionsUsed = actionsUsed;
        this.turnCount = turnCount;
        this.started = started;
        this.gameOver = gameOver;
        cardHistory.restore(usedCards);
    }
}
