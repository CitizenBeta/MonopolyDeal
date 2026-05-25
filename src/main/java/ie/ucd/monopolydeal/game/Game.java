package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class Game {
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
    private boolean gameOver;

    // Starts a fresh game and deals the initial cards.
    // This method resets all runtime state, so it should only be called when
    // starting or restarting a game.
    public void setup(List<String> names) {
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
        GameSnapshot snapshot = new GameSnapshot();

        // A card action can ask the user for several decisions. Roll back if any step is cancelled.
        if (!playSpecificCard(current, card, dm)) {
            snapshot.restore();
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
            return !receivableColors(current, card).isEmpty();
        }

        // Action cards are allowed here because the player may still choose to bank them.
        return card instanceof MoneyCard || card instanceof ActionCard;
    }

    public int getCurrBankTotal() {
        return totalValue(getCurrPlayer().getCardsAtBank());
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

    private boolean playPassGo(Player player) {
        drawCards(player, PASS_GO_DRAW);
        return true;
    }

    private boolean playDebtCollector(Player player, DecisionMaker dm) {
        return collectFromChosenPlayer(
                player,
                DEBT_COLLECTOR_AMOUNT,
                dm,
                "Choose a player to pay you 5M."
        );
    }

    private boolean playBirthday(Player player, DecisionMaker dm) {
        return collectFromEveryAvailablePlayer(player, BIRTHDAY_AMOUNT, dm);
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
            return collectFromChosenPlayer(
                    player,
                    amount,
                    dm,
                    "Choose a player to pay " + amount + "M rent."
            );
        }

        return collectFromEveryAvailablePlayer(player, amount, dm);
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
            if (card instanceof ActionCard actionCard && card != excludedCard && isRentCard(actionCard)) {
                rentCards.add(card);
            }
        }
        return rentCards;
    }

    private boolean isRentCard(ActionCard actionCard) {
        ActionType type = actionCard.getActionType();
        return type == ActionType.RENT || type == ActionType.MULTI_RENT;
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

        List<Card> cards = stealableCards(target, player);
        Card card = dm.selectPropertyCard(target, cards, "Choose a property to steal.");
        return card != null && transferPropertyCard(target, player, card, dm);
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
                stealableCards(player, target),
                "Choose one of your properties to give."
        );
        if (ownCard == null) {
            return false;
        }

        Card targetCard = dm.selectPropertyCard(
                target,
                stealableCards(target, player),
                "Choose one property to receive."
        );
        return targetCard != null && swapProperties(player, ownCard, target, targetCard, dm);
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

    // Collects money from one selected player.
    // Used by Debt Collector and Multi Rent.
    private boolean collectFromChosenPlayer(Player receiver, int amount, DecisionMaker dm, String prompt) {
        List<Player> targets = playersWithPaymentOptions(receiver);
        Player payer = dm.selectNextPlayer(receiver, targets, prompt);
        return payer != null && collectPayment(payer, receiver, amount, dm);
    }

    // Collects from every opponent who has at least one valid payment option.
    // Returning false when nobody can pay keeps action cards from being wasted on empty targets.
    private boolean collectFromEveryAvailablePlayer(Player receiver, int amount, DecisionMaker dm) {
        List<Player> payers = playersWithPaymentOptions(receiver);
        if (payers.isEmpty()) {
            return false;
        }

        for (Player payer : payers) {
            if (!collectPayment(payer, receiver, amount, dm)) {
                return false;
            }
        }
        return true;
    }

    // Handles one payment transaction.
    // Monopoly Deal allows overpaying, but not underpaying when enough value is available.
    private boolean collectPayment(Player payer, Player receiver, int amount, DecisionMaker dm) {
        if (amount <= 0) {
            return true;
        }

        if (isBlockedByJustSayNo(payer, receiver, dm)) {
            return true;
        }

        List<Card> options = paymentOptions(payer, receiver);
        if (options.isEmpty()) {
            return false;
        }

        int requiredAmount = Math.min(amount, totalValue(options));
        List<Card> selectedCards = dm.selectPaymentCards(payer, options, requiredAmount);
        List<Card> validCards = validatePaymentSelection(options, selectedCards, requiredAmount);
        if (validCards == null) {
            return false;
        }

        for (Card selected : validCards) {
            if (!transferPaymentCard(payer, receiver, selected, dm)) {
                return false;
            }
        }
        return true;
    }

    // Verifies that the selected payment cards are unique, owned by the payer and valuable enough.
    // Returns the cleaned list so the caller can transfer exactly those cards.
    private List<Card> validatePaymentSelection(List<Card> options, List<Card> selectedCards, int requiredAmount) {
        if (selectedCards == null || selectedCards.isEmpty()) {
            return null;
        }

        List<Card> uniqueCards = new ArrayList<>();
        int paid = 0;

        for (Card selected : selectedCards) {
            if (!options.contains(selected) || uniqueCards.contains(selected)) {
                return null;
            }

            uniqueCards.add(selected);
            paid += selected.getBankValue();
        }

        return paid >= requiredAmount ? uniqueCards : null;
    }

    private List<Player> playersWithPaymentOptions(Player receiver) {
        return matchingOtherPlayers(player -> hasPaymentOptions(player, receiver));
    }

    private boolean hasPaymentOptions(Player player, Player receiver) {
        return !paymentOptions(player, receiver).isEmpty();
    }

    private int totalValue(List<Card> cards) {
        int total = 0;
        for (Card card : cards) {
            total += card.getBankValue();
        }
        return total;
    }

    // Payment can come from the bank first, then from transferable properties.
    // Hand cards are never payment options in Monopoly Deal.
    private List<Card> paymentOptions(Player player, Player receiver) {
        List<Card> options = new ArrayList<>(player.getCardsAtBank());

        for (PropertySet set : player.getPropertySets().values()) {
            for (Card card : set.getAllCards()) {
                if (canTransferPaymentCard(player, receiver, card)) {
                    options.add(card);
                }
            }
        }

        return options;
    }

    // Moves a selected payment card to the receiver.
    // Bank cards stay money; property cards must be placed into a valid property set.
    private boolean transferPaymentCard(Player payer, Player receiver, Card card, DecisionMaker dm) {
        if (payer.getCardsAtBank().contains(card)) {
            if (!payer.removeCardFromBank(card)) {
                return false;
            }
            receiver.addCardToBank(card);
            return true;
        }

        return transferPropertyCard(payer, receiver, card, dm);
    }

    // Transfers one property-like card between players.
    // Wild cards may need a new color choice on the receiver's table.
    private boolean transferPropertyCard(Player source, Player receiver, Card card, DecisionMaker dm) {
        PropertyColor sourceColor = source.getPropertyColor(card);
        if (sourceColor == null) {
            return false;
        }

        if (card instanceof ActionCard actionCard) {
            return transferUpgradeCard(source, receiver, actionCard, sourceColor);
        }

        PropertyColor receiveColor = chooseReceiveColor(receiver, card, dm);
        if (receiveColor == null || !source.removePropertyCard(card)) {
            return false;
        }

        receiver.receivePropertyCard(card, receiveColor);
        return true;
    }

    // Transfers a House or Hotel that is already attached to a property set.
    // Upgrade cards keep the same color because they belong to a specific set.
    private boolean transferUpgradeCard(Player source, Player receiver, ActionCard card, PropertyColor color) {
        if (!canReceiveUpgradeCard(source, receiver, card, color)) {
            return false;
        }

        if (!source.removePropertyCard(card)) {
            return false;
        }

        PropertySet receiverSet = receiver.getPropertySets().get(color);
        return card.getActionType() == ActionType.HOUSE
                ? receiverSet.addHouse(card)
                : receiverSet.addHotel(card);
    }

    // Checks whether the receiver can accept a House or Hotel from the source.
    // A House cannot be taken away while its source set still has a Hotel.
    private boolean canReceiveUpgradeCard(Player source, Player receiver, ActionCard card, PropertyColor color) {
        PropertySet receiverSet = receiver.getPropertySets().get(color);
        if (receiverSet == null) {
            return false;
        }

        if (card.getActionType() == ActionType.HOUSE) {
            PropertySet sourceSet = source.getPropertySets().get(color);
            return (sourceSet == null || sourceSet.getHotelCard() == null) && receiverSet.canAddHouse();
        }

        if (card.getActionType() == ActionType.HOTEL) {
            return receiverSet.canAddHotel();
        }

        return false;
    }

    // Swaps two properties as one atomic operation.
    // The method validates both destinations first to avoid half-completed trades.
    private boolean swapProperties(Player firstPlayer, Card firstCard, Player secondPlayer, Card secondCard, DecisionMaker dm) {
        if (!canReceiveAfterRemoving(firstPlayer, secondCard, firstCard)
                || !canReceiveAfterRemoving(secondPlayer, firstCard, secondCard)) {
            return false;
        }

        PropertyColor firstReceiveColor = chooseReceiveColorAfterRemoving(firstPlayer, secondCard, firstCard, dm);
        if (firstReceiveColor == null) {
            return false;
        }

        PropertyColor secondReceiveColor = chooseReceiveColorAfterRemoving(secondPlayer, firstCard, secondCard, dm);
        if (secondReceiveColor == null) {
            return false;
        }

        firstPlayer.removePropertyCard(firstCard);
        secondPlayer.removePropertyCard(secondCard);
        firstPlayer.receivePropertyCard(secondCard, firstReceiveColor);
        secondPlayer.receivePropertyCard(firstCard, secondReceiveColor);
        return true;
    }

    private boolean canReceiveAfterRemoving(Player receiver, Card incoming, Card outgoing) {
        return !receivableColorsAfterRemoving(receiver, incoming, outgoing).isEmpty();
    }

    private boolean canReceiveProperty(Player receiver, Card card, PropertyColor color) {
        return receivableColors(receiver, card).contains(color);
    }

    private boolean canTransferPaymentCard(Player source, Player receiver, Card card) {
        PropertyColor color = source.getPropertyColor(card);
        if (color == null) {
            return false;
        }

        if (card instanceof ActionCard actionCard) {
            return canReceiveUpgradeCard(source, receiver, actionCard, color);
        }

        return !receivableColors(receiver, card).isEmpty();
    }

    private PropertyColor chooseReceiveColor(Player receiver, Card card, DecisionMaker dm) {
        return chooseSingleOrPrompt(
                receivableColors(receiver, card),
                dm,
                "Choose a color for " + card.getName() + " in " + receiver.getName() + "'s table."
        );
    }

    private PropertyColor chooseReceiveColorAfterRemoving(Player receiver, Card incoming, Card outgoing, DecisionMaker dm) {
        return chooseSingleOrPrompt(
                receivableColorsAfterRemoving(receiver, incoming, outgoing),
                dm,
                "Choose a color for " + incoming.getName() + " in " + receiver.getName() + "'s table."
        );
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

    // Lists all colors where the receiver can legally place the incoming property card.
    private List<PropertyColor> receivableColors(Player receiver, Card card) {
        List<PropertyColor> colors = new ArrayList<>();

        if (card instanceof PropertyCard propertyCard) {
            addReceivableColor(receiver, colors, propertyCard.getColor());
        }

        if (card instanceof WildPropertyCard wildCard) {
            for (PropertyColor color : wildCard.getPossibleColors()) {
                addReceivableColor(receiver, colors, color);
            }
        }

        return colors;
    }

    // Like receivableColors(), but pretends the outgoing card has already left.
    // This is needed for Forced Deal because the swap happens at the same time.
    private List<PropertyColor> receivableColorsAfterRemoving(Player receiver, Card incoming, Card outgoing) {
        List<PropertyColor> colors = new ArrayList<>();

        for (PropertyColor color : possiblePropertyColors(incoming)) {
            PropertySet set = receiver.getPropertySets().get(color);
            if (set == null) {
                continue;
            }

            int count = set.getCards().size();
            if (receiver.getPropertyColor(outgoing) == color) {
                count--;
            }

            if (count < color.getSize()) {
                colors.add(color);
            }
        }

        return colors;
    }

    private List<PropertyColor> possiblePropertyColors(Card card) {
        List<PropertyColor> colors = new ArrayList<>();

        if (card instanceof PropertyCard propertyCard) {
            colors.add(propertyCard.getColor());
        }

        if (card instanceof WildPropertyCard wildCard) {
            colors.addAll(wildCard.getPossibleColors());
        }

        return colors;
    }

    private void addReceivableColor(Player receiver, List<PropertyColor> colors, PropertyColor color) {
        PropertySet set = receiver.getPropertySets().get(color);
        if (set != null && set.canAddProperty()) {
            colors.add(color);
        }
    }

    private List<Player> playersForForcedDeal(Player player) {
        return matchingOtherPlayers(target ->
                !stealableCards(player, target).isEmpty()
                        && !stealableCards(target, player).isEmpty()
        );
    }

    private List<Player> playersWithStealableCards(Player receiver) {
        return matchingOtherPlayers(player -> !stealableCards(player, receiver).isEmpty());
    }

    private List<Card> stealableCards(Player source, Player receiver) {
        List<Card> cards = new ArrayList<>();

        for (Card card : source.getStealableCards()) {
            PropertyColor color = source.getPropertyColor(card);
            if (canReceiveProperty(receiver, card, color)) {
                cards.add(card);
            }
        }

        return cards;
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
    private boolean isBlockedByJustSayNo(Player target, Player actor, DecisionMaker dm) {
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

    // Captures the mutable parts of the game before a risky action.
    // It is used to roll back when a multi-step card action is cancelled or becomes invalid.
    private class GameSnapshot {
        private final int savedCurrentPlayerIndex;
        private final int savedActionsUsed;
        private final int savedTurnCount;
        private final boolean savedStarted;
        private final boolean savedGameOver;
        private final List<UsedCard> savedUsedCards;
        private final List<PlayerSnapshot> savedPlayers;

        // Stores game-level fields and a snapshot of every player.
        private GameSnapshot() {
            savedCurrentPlayerIndex = currentPlayerIndex;
            savedActionsUsed = actionsUsed;
            savedTurnCount = turnCount;
            savedStarted = started;
            savedGameOver = gameOver;
            savedUsedCards = new ArrayList<>(usedCards);
            savedPlayers = new ArrayList<>();

            for (Player player : players) {
                savedPlayers.add(new PlayerSnapshot(player));
            }
        }

        // Rebuilds the player's mutable collections from the saved copies.
        private void restore() {
            currentPlayerIndex = savedCurrentPlayerIndex;
            actionsUsed = savedActionsUsed;
            turnCount = savedTurnCount;
            started = savedStarted;
            gameOver = savedGameOver;
            usedCards = new ArrayList<>(savedUsedCards);

            for (PlayerSnapshot snapshot : savedPlayers) {
                snapshot.restore();
            }
        }
    }

    // Captures one player's hand, bank, property sets and wild-card colors.
    private static class PlayerSnapshot {
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

        // Wild cards have mutable color state, so the selected color must be saved separately.
        private void rememberWildColors(List<Card> cards) {
            for (Card card : cards) {
                if (card instanceof WildPropertyCard wildCard) {
                    wildColors.put(wildCard, wildCard.getCurrentColor());
                }
            }
        }

        private void restore() {
            player.getCardsAtHand().clear();
            player.getCardsAtHand().addAll(hand);
            player.getCardsAtBank().clear();
            player.getCardsAtBank().addAll(bank);

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

    private record PropertySetSnapshot(List<Card> cards, ActionCard houseCard, ActionCard hotelCard) {}

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
