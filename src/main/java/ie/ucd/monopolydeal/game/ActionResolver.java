package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// Resolves action-card legality and effects for Game
final class ActionResolver {
    private static final int PASS_GO_DRAW = 2;
    private static final int DEBT_COLLECTOR_AMOUNT = 5;
    private static final int BIRTHDAY_AMOUNT = 2;

    private final Game game;
    private final Payment payments;

    ActionResolver(Game game, Payment payments) {
        this.game = game;
        this.payments = payments;
    }

    // Handles action cards after the player chooses whether to use them as money or as actions
    boolean playActionCard(Player player, ActionCard action, DecisionMaker dm) {
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

    boolean canResolveActionCard(Player player, ActionCard action) {
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
        if (game.getActionsUsed() + 2 > Player.MAX_ACTIONS_PER_TURN) {
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
        game.drawCards(player, PASS_GO_DRAW);
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

    // Shared rent logic for normal rent, multi rent and doubled rent
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

    // Finds colors that can currently produce rent
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

    // Double Rent consumes two actions: the Double Rent card and the selected rent card
    private boolean playDoubleRent(Player player, ActionCard doubleRent, DecisionMaker dm) {
        if (game.getActionsUsed() + 2 > Player.MAX_ACTIONS_PER_TURN) {
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
        game.addUsedCard(player, rentCard, Game.CardAction.PLAYED);
        game.addActionUsed();
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

    // Sly Deal steals one transferable property from another player
    private boolean playSlyDeal(Player player, DecisionMaker dm) {
        List<Player> targets = playersWithStealableCards(player);
        Player target = dm.selectNextPlayer(player, targets, "Choose a player to steal from.");
        if (target == null) {
            return false;
        }

        if (game.isBlockedByJustSayNo(target, player, dm)) {
            return true;
        }

        List<Card> cards = payments.stealableCards(target, player);
        Card card = dm.selectPropertyCard(target, cards, "Choose a property to steal.");
        return card != null && payments.transferPropertyCard(target, player, card, dm);
    }

    // Forced Deal swaps one property from each player
    private boolean playForcedDeal(Player player, DecisionMaker dm) {
        List<Player> targets = playersForForcedDeal(player);
        Player target = dm.selectNextPlayer(player, targets, "Choose a player to trade with.");
        if (target == null) {
            return false;
        }

        if (game.isBlockedByJustSayNo(target, player, dm)) {
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

    // Deal Breaker transfers a complete set if the receiver has enough room for that set
    private boolean playDealBreaker(Player player, DecisionMaker dm) {
        List<Player> targets = playersWithTransferableFullSets(player);
        Player target = dm.selectNextPlayer(player, targets, "Choose a player with a full set.");
        if (target == null) {
            return false;
        }

        if (game.isBlockedByJustSayNo(target, player, dm)) {
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

    // Returns full sets that can receive either a House or a Hotel
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

    // Small filtering helper used by the action-card target search methods
    private List<Player> matchingOtherPlayers(Predicate<Player> predicate) {
        List<Player> matches = new ArrayList<>();
        for (Player player : game.getOtherPlayers()) {
            if (predicate.test(player)) {
                matches.add(player);
            }
        }
        return matches;
    }
}
