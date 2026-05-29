package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// Handles payment and property-transfer rules used by action cards
public final class Payment {
    private final Game game;

    public Payment(Game game) {
        this.game = game;
    }

    // Collects money from one selected player
    public boolean collectFromChosenPlayer(Player receiver, int amount, DecisionMaker dm, String prompt) {
        List<Player> targets = playersWithPaymentOptions(receiver);
        Player payer = dm.selectNextPlayer(receiver, targets, prompt);
        return payer != null && collectPayment(payer, receiver, amount, dm);
    }

    // Collects from every opponent who has at least one valid payment option
    public boolean collectFromEveryAvailablePlayer(Player receiver, int amount, DecisionMaker dm) {
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

    public List<Player> playersWithPaymentOptions(Player receiver) {
        return matchingOtherPlayers(player -> hasPaymentOptions(player, receiver));
    }

    private boolean hasPaymentOptions(Player player, Player receiver) {
        return !paymentOptions(player, receiver).isEmpty();
    }

    // Handles one payment transaction
    private boolean collectPayment(Player payer, Player receiver, int amount, DecisionMaker dm) {
        if (amount <= 0) {
            return true;
        }

        if (game.isBlockedByJustSayNo(payer, receiver, dm)) {
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

    private int totalValue(List<Card> cards) {
        int total = 0;
        for (Card card : cards) {
            total += card.getBankValue();
        }
        return total;
    }

    // Verifies that the selected payment cards are unique, owned by the payer and valuable enough
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

        if (paid >= requiredAmount) {
            return uniqueCards;
        }
        return null;
    }

    // Payment can come from the bank first, then from transferable properties
    private List<Card> paymentOptions(Player player, Player receiver) {
        List<Card> options = new ArrayList<>();
        for (Card card : player.getCardsAtBank()) {
            if (card.getBankValue() > 0) {
                options.add(card);
            }
        }

        for (PropertySet set : player.getPropertySets().values()) {
            for (Card card : set.getAllCards()) {
                if (card.getBankValue() > 0 && canTransferPaymentCard(player, receiver, card)) {
                    options.add(card);
                }
            }
        }

        return options;
    }

    // Moves a selected payment card to the receiver
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

    // Transfers one property-like card between players
    public boolean transferPropertyCard(Player source, Player receiver, Card card, DecisionMaker dm) {
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

    // Transfers a House or Hotel that is already attached to a property set
    private boolean transferUpgradeCard(Player source, Player receiver, ActionCard card, PropertyColor color) {
        if (!canReceiveUpgradeCard(source, receiver, card, color)) {
            return false;
        }

        if (!source.removePropertyCard(card)) {
            return false;
        }

        PropertySet receiverSet = receiver.getPropertySets().get(color);
        if (card.getActionType() == ActionType.HOUSE) {
            return receiverSet.addHouse(card);
        }
        return receiverSet.addHotel(card);
    }

    // Checks whether the receiver can accept a House or Hotel from the source
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

    // Swaps two properties as one atomic operation
    public boolean swapProperties(Player firstPlayer, Card firstCard, Player secondPlayer, Card secondCard, DecisionMaker dm) {
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

    public boolean canReceiveProperty(Player receiver, Card card, PropertyColor color) {
        return receivableColors(receiver, card).contains(color);
    }

    public boolean canReceiveCard(Player receiver, Card card) {
        return !receivableColors(receiver, card).isEmpty();
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

    // Lists all colors where the receiver can legally place the incoming property card
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

    // Like receivableColors(), but pretends the outgoing card has already left
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

    public List<Card> stealableCards(Player source, Player receiver) {
        List<Card> cards = new ArrayList<>();

        for (Card card : source.getStealableCards()) {
            PropertyColor color = source.getPropertyColor(card);
            if (canReceiveProperty(receiver, card, color)) {
                cards.add(card);
            }
        }

        return cards;
    }

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
