package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class Game {
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 5;

    private static final int INITIAL_HAND_SIZE = 5;
    private static final int NORMAL_TURN_DRAW = 2;
    private static final int PASS_GO_DRAW = 2;
    private static final int DEBT_COLLECTOR_AMOUNT = 5;
    private static final int BIRTHDAY_AMOUNT = 2;

    private List<Player> players = new ArrayList<>();
    private int currentPlayerIndex;
    private int actionsUsed;
    private int turnCount;
    private boolean started;
    private Deck deck = new Deck();
    private List<UsedCard> usedCards = new ArrayList<>();
    private final Payment payments = new Payment(this);
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
    public List<UsedCard> getUsedCards() {
        return new ArrayList<>(usedCards);
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
                usedCards,
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
        usedCards.clear();
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
        addUsedCard(player, card, CardAction.PLAYED);
        gameOver = player.hasWon();
    }

    // Handles action cards after the player chooses whether to use them as money or as actions.
    // Each action type is delegated to a small method to keep the switch easy to read.
    private boolean playActionCard(Player player, ActionCard action, DecisionMaker dm) {
        UseMode mode = dm.useCard(action);
        if (mode == null) {
            return false;
        }

        if (mode == UseMode.BANK) {
            player.addCardToBank(action);
            return true;
        }

        return switch (action.getActionType()) {
            case PASS_GO -> playPassGo(player);
            case DEBT_COLLECTOR -> playDebtCollector(player, dm);
            case TODAY_IS_MY_BIRTHDAY -> playBirthday(player, dm);
            case RENT, MULTI_RENT -> playRentCard(player, action, dm, 1);
            case DOUBLE_RENT -> playDoubleRent(player, action, dm);
            case SLY_DEAL -> playSlyDeal(player, dm);
            case FORCED_DEAL -> playForcedDeal(player, dm);
            case DEAL_BREAKER -> playDealBreaker(player, dm);
            case HOUSE -> playHouse(player, action, dm);
            case HOTEL -> playHotel(player, action, dm);
            case JUST_SAY_NO -> false;
        };
    }

    private boolean canResolveActionCard(Player player, ActionCard action) {
        if (action == null) {
            return false;
        }

        return switch (action.getActionType()) {
            case PASS_GO -> true;
            case DEBT_COLLECTOR, TODAY_IS_MY_BIRTHDAY -> !payments.playersWithPaymentOptions(player).isEmpty();
            case RENT, MULTI_RENT -> canResolveRentCard(player, action);
            case DOUBLE_RENT -> canResolveDoubleRent(player, action);
            case SLY_DEAL -> !playersWithStealableCards(player).isEmpty();
            case FORCED_DEAL -> !playersForForcedDeal(player).isEmpty();
            case DEAL_BREAKER -> !playersWithTransferableFullSets(player).isEmpty();
            case HOUSE -> !buildableColors(player, true).isEmpty();
            case HOTEL -> !buildableColors(player, false).isEmpty();
            case JUST_SAY_NO -> false;
        };
    }

    // Check rent card color and payment target
    private boolean canResolveRentCard(Player player, ActionCard action) {
        return !getRentColors(player, action).isEmpty()
                && !payments.playersWithPaymentOptions(player).isEmpty();
    }

    // Check Double Rent card and remaining actions
    private boolean canResolveDoubleRent(Player player, ActionCard doubleRent) {
        if (actionsUsed + 2 > Player.MAX_ACTIONS_PER_TURN) {
            return false;
        }

        for (Card card : rentCardsInHandExcept(player, doubleRent)) {
            if (card instanceof ActionCard rentCard && canResolveRentCard(player, rentCard)) {
                return true;
            }
        }

        return false;
    }

    private boolean playPassGo(Player player) {
        drawCards(player, PASS_GO_DRAW);
        return true;
    }

    private boolean playDebtCollector(Player player, DecisionMaker dm) {
        return payments.collectFromChosenPlayer(
                player,
                DEBT_COLLECTOR_AMOUNT,
                dm,
                "Choose a player to pay you 5M."
        );
    }

    private boolean playBirthday(Player player, DecisionMaker dm) {
        return payments.collectFromEveryAvailablePlayer(player, BIRTHDAY_AMOUNT, dm);
    }

    // Shared rent logic for normal rent, multi rent and doubled rent.
    // The multiplier is normally 1, or 2 when Double Rent is applied.
    private boolean playRentCard(Player player, ActionCard action, DecisionMaker dm, int multiplier) {
        PropertyColor color = chooseRentColor(player, action, dm);
        if (color == null) {
            return false;
        }

        int amount = player.getPropertySets().get(color).calculateRent() * multiplier;
        if (amount <= 0) {
            return false;
        }

        if (action.getActionType() == ActionType.MULTI_RENT) {
            return payments.collectFromChosenPlayer(
                    player,
                    amount,
                    dm,
                    "Choose a player to pay " + amount + "M rent."
            );
        }

        return payments.collectFromEveryAvailablePlayer(player, amount, dm);
    }

    private PropertyColor chooseRentColor(Player player, ActionCard action, DecisionMaker dm) {
        return chooseSingleOrPrompt(getRentColors(player, action), dm, "Choose rent color.");
    }

    // Finds colors that can currently produce rent.
    // Multi-rent can use any color, while normal rent is limited by the card's printed colors.
    private List<PropertyColor> getRentColors(Player player, ActionCard action) {
        List<PropertyColor> sourceColors = action.getActionType() == ActionType.MULTI_RENT
                ? PropertyColor.getColors()
                : action.getColors();

        List<PropertyColor> colors = new ArrayList<>();
        for (PropertyColor color : sourceColors) {
            PropertySet set = player.getPropertySets().get(color);
            if (set != null && set.calculateRent() > 0) {
                colors.add(color);
            }
        }
        return colors;
    }

    // Double Rent consumes two actions: the Double Rent card and the selected rent card.
    // The selected rent card is removed manually because playCard() only removes the outer card.
    private boolean playDoubleRent(Player player, ActionCard doubleRent, DecisionMaker dm) {
        if (actionsUsed + 2 > Player.MAX_ACTIONS_PER_TURN) {
            return false;
        }

        List<Card> rentCards = rentCardsInHandExcept(player, doubleRent);
        Card selected = dm.selectPropertyCard(player, rentCards, "Choose a rent card to double.");
        if (!(selected instanceof ActionCard rentCard)) {
            return false;
        }

        if (!playRentCard(player, rentCard, dm, 2)) {
            return false;
        }

        player.removeCardFromHand(rentCard);
        addUsedCard(player, rentCard, CardAction.PLAYED);
        actionsUsed++;
        return true;
    }

    private List<Card> rentCardsInHandExcept(Player player, Card excludedCard) {
        List<Card> rentCards = new ArrayList<>();
        for (Card card : player.getCardsAtHand()) {
            if (card instanceof ActionCard actionCard && card != excludedCard && isStandardRentCard(actionCard)) {
                rentCards.add(card);
            }
        }
        return rentCards;
    }

    private boolean isStandardRentCard(ActionCard actionCard) {
        return actionCard.getActionType() == ActionType.RENT;
    }

    // Sly Deal steals one transferable property from another player.
    // Full sets are excluded by Player.getStealableCards().
    private boolean playSlyDeal(Player player, DecisionMaker dm) {
        List<Player> targets = playersWithStealableCards(player);
        Player target = dm.selectNextPlayer(player, targets, "Choose a player to steal from.");
        if (target == null) {
            return false;
        }

        if (isBlockedByJustSayNo(target, player, dm)) {
            return true;
        }

        List<Card> cards = payments.stealableCards(target, player);
        Card card = dm.selectPropertyCard(target, cards, "Choose a property to steal.");
        return card != null && payments.transferPropertyCard(target, player, card, dm);
    }

    // Forced Deal swaps one property from each player.
    // Both sides are validated before either card is removed.
    private boolean playForcedDeal(Player player, DecisionMaker dm) {
        List<Player> targets = playersForForcedDeal(player);
        Player target = dm.selectNextPlayer(player, targets, "Choose a player to trade with.");
        if (target == null) {
            return false;
        }

        if (isBlockedByJustSayNo(target, player, dm)) {
            return true;
        }

        Card ownCard = dm.selectPropertyCard(
                player,
                payments.stealableCards(player, target),
                "Choose one of your properties to give."
        );
        if (ownCard == null) {
            return false;
        }

        Card targetCard = dm.selectPropertyCard(
                target,
                payments.stealableCards(target, player),
                "Choose one property to receive."
        );
        return targetCard != null && payments.swapProperties(player, ownCard, target, targetCard, dm);
    }

    // Deal Breaker transfers a complete set if the receiver has enough room for that set.
    private boolean playDealBreaker(Player player, DecisionMaker dm) {
        List<Player> targets = playersWithTransferableFullSets(player);
        Player target = dm.selectNextPlayer(player, targets, "Choose a player with a full set.");
        if (target == null) {
            return false;
        }

        if (isBlockedByJustSayNo(target, player, dm)) {
            return true;
        }

        PropertyColor color = chooseSingleOrPrompt(
                transferableFullSetColors(target, player),
                dm,
                "Choose a full set to steal."
        );
        return color != null && target.transferFullSetTo(player, color);
    }

    private boolean playHouse(Player player, ActionCard house, DecisionMaker dm) {
        PropertyColor color = chooseSingleOrPrompt(
                buildableColors(player, true),
                dm,
                "Choose a full set for House."
        );
        return color != null && player.addHouse(color, house);
    }

    private boolean playHotel(Player player, ActionCard hotel, DecisionMaker dm) {
        PropertyColor color = chooseSingleOrPrompt(
                buildableColors(player, false),
                dm,
                "Choose a full set for Hotel."
        );
        return color != null && player.addHotel(color, hotel);
    }

    // Returns full sets that can receive either a House or a Hotel.
    // Railroads and utilities cannot receive buildings.
    private List<PropertyColor> buildableColors(Player player, boolean forHouse) {
        List<PropertyColor> colors = new ArrayList<>();
        for (PropertySet set : player.getPropertySets().values()) {
            if (canBuildOn(set.getColor())
                    && ((forHouse && set.canAddHouse()) || (!forHouse && set.canAddHotel()))) {
                colors.add(set.getColor());
            }
        }
        return colors;
    }

    private boolean canBuildOn(PropertyColor color) {
        return color != PropertyColor.RAILROAD && color != PropertyColor.UTILITY;
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

    private List<Player> playersForForcedDeal(Player player) {
        return matchingOtherPlayers(target ->
                !payments.stealableCards(player, target).isEmpty()
                        && !payments.stealableCards(target, player).isEmpty()
        );
    }

    private List<Player> playersWithStealableCards(Player receiver) {
        return matchingOtherPlayers(player -> !payments.stealableCards(player, receiver).isEmpty());
    }

    private List<Player> playersWithTransferableFullSets(Player receiver) {
        return matchingOtherPlayers(player -> !transferableFullSetColors(player, receiver).isEmpty());
    }

    private List<PropertyColor> transferableFullSetColors(Player source, Player receiver) {
        List<PropertyColor> colors = new ArrayList<>();

        for (PropertyColor color : source.getFullSetColors()) {
            PropertySet sourceSet = source.getPropertySets().get(color);
            PropertySet receiverSet = receiver.getPropertySets().get(color);

            if (sourceSet != null
                    && receiverSet != null
                    && receiverSet.getCards().size() + sourceSet.getCards().size() <= color.getSize()) {
                colors.add(color);
            }
        }

        return colors;
    }

    // Small filtering helper used by the action-card target search methods.
    private List<Player> matchingOtherPlayers(Predicate<Player> predicate) {
        List<Player> matches = new ArrayList<>();
        for (Player player : getOtherPlayers()) {
            if (predicate.test(player)) {
                matches.add(player);
            }
        }
        return matches;
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
        addUsedCard(target, justSayNo, CardAction.PLAYED);

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
            addUsedCard(player, discard, CardAction.DISCARDED);
        }

        return true;
    }

    // Ensures the discard choice has the right count, belongs to the player and contains no duplicates.
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
    private void drawCards(Player player, int number) {
        for (int i = 0; i < number; i++) {
            Card card = deck.draw();
            if (card != null) {
                player.addCardToHand(card);
            }
        }
    }

    private void addUsedCard(Player player, Card card, CardAction action) {
        // Keep the newest record first while staying compatible with older Java versions.
        usedCards.add(0, new UsedCard(action, player.getName(), card));
    }

    public void restoreSnapshot(int currentPlayerIndex, int actionsUsed, int turnCount,
                                boolean started, boolean gameOver, List<UsedCard> usedCards) {
        this.currentPlayerIndex = currentPlayerIndex;
        this.actionsUsed = actionsUsed;
        this.turnCount = turnCount;
        this.started = started;
        this.gameOver = gameOver;
        this.usedCards = new ArrayList<>(usedCards);
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
