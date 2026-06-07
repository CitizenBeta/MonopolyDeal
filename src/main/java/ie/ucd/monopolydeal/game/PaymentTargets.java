package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// Finds legal payment cards, property targets and receivers
public final class PaymentTargets {
    private final Game game;

    PaymentTargets(Game game) {
        this.game = game;
    }

    List<Player> playersWithPaymentOptions(Player receiver) {
        return matchingOtherPlayers(player -> hasPaymentOptions(player, receiver));
    }

    private boolean hasPaymentOptions(Player player, Player receiver) {
        return !paymentOptions(player, receiver).isEmpty();
    }

    // Payment can come from the bank first, then from transferable properties
    List<Card> paymentOptions(Player player, Player receiver) {
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

    boolean canReceiveProperty(Player receiver, Card card, PropertyColor color) {
        return receivableColors(receiver, card).contains(color);
    }

    boolean canReceiveCard(Player receiver, Card card) {
        return !receivableColors(receiver, card).isEmpty();
    }

    boolean canReceiveAfterRemoving(Player receiver, Card incoming, Card outgoing) {
        return !receivableColorsAfterRemoving(receiver, incoming, outgoing).isEmpty();
    }

    List<PropertyColor> receivableColors(Player receiver, Card card) {
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
    List<PropertyColor> receivableColorsAfterRemoving(Player receiver, Card incoming, Card outgoing) {
        // With no per-color limit, removing the outgoing card never changes whether the
        // incoming card can be received, so this matches the normal receivable-color check.
        return receivableColors(receiver, incoming);
    }

    List<Card> stealableCards(Player source, Player receiver) {
        List<Card> cards = new ArrayList<>();

        for (Card card : source.getStealableCards()) {
            PropertyColor color = source.getPropertyColor(card);
            if (canReceiveProperty(receiver, card, color)) {
                cards.add(card);
            }
        }

        return cards;
    }

    private boolean canTransferPaymentCard(Player source, Player receiver, Card card) {
        PropertyColor color = source.getPropertyColor(card);
        if (color == null) {
            return false;
        }

        // House/Hotel are paid into the receiver's bank as money, so the receiver can always take them.
        // A House cannot be paid alone while a Hotel still sits on the same set (it would orphan the Hotel).
        if (card instanceof ActionCard actionCard) {
            if (actionCard.getActionType() == ActionType.HOUSE) {
                PropertySet sourceSet = source.getPropertySets().get(color);
                return sourceSet == null || sourceSet.getHotelCard() == null;
            }
            return actionCard.getActionType() == ActionType.HOTEL;
        }

        return !receivableColors(receiver, card).isEmpty();
    }

    private void addReceivableColor(Player receiver, List<PropertyColor> colors, PropertyColor color) {
        // No per-color limit, so any existing set for a valid color can receive the card.
        PropertySet set = receiver.getPropertySets().get(color);
        if (set != null) {
            colors.add(color);
        }
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
