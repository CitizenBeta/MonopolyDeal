package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.Card;
import ie.ucd.monopolydeal.model.Player;

import java.util.ArrayList;
import java.util.List;

// Records played and discarded cards in newest-first order
public final class CardHistory {
    private List<UsedCard> usedCards = new ArrayList<>();

    public List<UsedCard> getUsedCards() {
        return new ArrayList<>(usedCards);
    }

    void add(Player player, Card card, CardAction action) {
        // Keep the newest record first while staying compatible with older Java versions
        usedCards.add(0, new UsedCard(action, player.getName(), card));
    }

    void clear() {
        usedCards.clear();
    }

    void restore(List<UsedCard> usedCards) {
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
