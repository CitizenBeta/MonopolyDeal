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

    boolean canReceiveUpgradeCard(Player source, Player receiver, ActionCard card, PropertyColor color) {
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

        if (card instanceof ActionCard actionCard) {
            return canReceiveUpgradeCard(source, receiver, actionCard, color);
        }

        return !receivableColors(receiver, card).isEmpty();
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
