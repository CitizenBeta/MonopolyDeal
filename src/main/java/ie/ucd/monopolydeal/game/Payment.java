package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;

import java.util.ArrayList;
import java.util.List;

// Handles payment and property-transfer rules used by action cards
public final class Payment {
    private final Game game;
    private final PaymentTargets targets;

    public Payment(Game game) {
        this.game = game;
        this.targets = new PaymentTargets(game);
    }

    // Collects money from one selected player
    public boolean collectFromChosenPlayer(Player receiver, int amount, DecisionMaker dm, String prompt) {
        List<Player> players = playersWithPaymentOptions(receiver);
        Player payer = dm.selectNextPlayer(receiver, players, prompt);
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
        return targets.playersWithPaymentOptions(receiver);
    }

    // Handles one payment transaction
    private boolean collectPayment(Player payer, Player receiver, int amount, DecisionMaker dm) {
        if (amount <= 0) {
            return true;
        }

        if (game.isBlockedByJustSayNo(payer, receiver, dm)) {
            return true;
        }

        List<Card> options = targets.paymentOptions(payer, receiver);
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
        if (!targets.canReceiveUpgradeCard(source, receiver, card, color)) {
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

    // Swaps two properties as one atomic operation
    public boolean swapProperties(Player firstPlayer, Card firstCard, Player secondPlayer, Card secondCard, DecisionMaker dm) {
        if (!targets.canReceiveAfterRemoving(firstPlayer, secondCard, firstCard)
                || !targets.canReceiveAfterRemoving(secondPlayer, firstCard, secondCard)) {
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

    public boolean canReceiveProperty(Player receiver, Card card, PropertyColor color) {
        return targets.canReceiveProperty(receiver, card, color);
    }

    public boolean canReceiveCard(Player receiver, Card card) {
        return targets.canReceiveCard(receiver, card);
    }

    private PropertyColor chooseReceiveColor(Player receiver, Card card, DecisionMaker dm) {
        return chooseSingleOrPrompt(
                targets.receivableColors(receiver, card),
                dm,
                "Choose a color for " + card.getName() + " in " + receiver.getName() + "'s table."
        );
    }

    private PropertyColor chooseReceiveColorAfterRemoving(Player receiver, Card incoming, Card outgoing, DecisionMaker dm) {
        return chooseSingleOrPrompt(
                targets.receivableColorsAfterRemoving(receiver, incoming, outgoing),
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

    public List<Card> stealableCards(Player source, Player receiver) {
        return targets.stealableCards(source, receiver);
    }
}
