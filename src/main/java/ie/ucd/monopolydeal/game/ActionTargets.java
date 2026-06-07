package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// Finds legal targets and colors for action-card resolution
public final class ActionTargets {
    private final Game game;
    private final Payment payments;

    ActionTargets(Game game, Payment payments) {
        this.game = game;
        this.payments = payments;
    }

    // Finds colors that can currently produce rent
    List<PropertyColor> getRentColors(Player player, ActionCard action) {
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

    // Returns full sets that can receive either a House or a Hotel
    List<PropertyColor> buildableColors(Player player, boolean forHouse) {
        List<PropertyColor> colors = new ArrayList<>();
        for (PropertySet set : player.getPropertySets().values()) {
            if (canBuildOn(set.getColor())
                    && ((forHouse && set.canAddHouse()) || (!forHouse && set.canAddHotel()))) {
                colors.add(set.getColor());
            }
        }
        return colors;
    }

    List<Player> playersForForcedDeal(Player player) {
        return matchingOtherPlayers(target ->
                !payments.stealableCards(player, target).isEmpty()
                        && !payments.stealableCards(target, player).isEmpty()
        );
    }

    List<Player> playersWithStealableCards(Player receiver) {
        return matchingOtherPlayers(player -> !payments.stealableCards(player, receiver).isEmpty());
    }

    List<Player> playersWithTransferableFullSets(Player receiver) {
        return matchingOtherPlayers(player -> !transferableFullSetColors(player, receiver).isEmpty());
    }

    List<PropertyColor> transferableFullSetColors(Player source, Player receiver) {
        List<PropertyColor> colors = new ArrayList<>();

        for (PropertyColor color : source.getFullSetColors()) {
            PropertySet sourceSet = source.getPropertySets().get(color);
            PropertySet receiverSet = receiver.getPropertySets().get(color);

            // No per-color limit, so a full set can always be stolen onto the receiver,
            // even when they already own properties of that color.
            if (sourceSet != null && receiverSet != null) {
                colors.add(color);
            }
        }

        return colors;
    }

    private boolean canBuildOn(PropertyColor color) {
        return color != PropertyColor.RAILROAD && color != PropertyColor.UTILITY;
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
