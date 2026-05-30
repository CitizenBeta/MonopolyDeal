package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Test decision maker that returns predictable choices
public class TestDecisionMaker implements DecisionMaker {
    private final UseMode useMode;
    private final boolean reconfirmAnswer;

    // Default test flow plays cards and does not counter actions
    public TestDecisionMaker() {
        this(UseMode.PLAY, false);
    }

    // Allow a test to choose play or bank and Just Say No behavior
    public TestDecisionMaker(UseMode useMode, boolean reconfirmAnswer) {
        this.useMode = useMode;
        this.reconfirmAnswer = reconfirmAnswer;
    }

    // Pick the first available target player
    @Override
    public Player selectNextPlayer(Player currentPlayer, List<Player> players, String prompt) {
        return players == null || players.isEmpty() ? null : players.get(0);
    }

    // Pick the first available color
    @Override
    public PropertyColor selectColor(String prompt, List<PropertyColor> colors) {
        return colors == null || colors.isEmpty() ? null : colors.get(0);
    }

    // Return the configured card-use choice
    @Override
    public UseMode useCard(ActionCard action) {
        return useMode;
    }

    // Pick the first movable wild card
    @Override
    public WildPropertyCard selectWildCardToMove(Player current, List<WildPropertyCard> wildCards) {
        return wildCards == null || wildCards.isEmpty() ? null : wildCards.get(0);
    }

    // Discard the last cards in hand when the test does not override this
    @Override
    public List<Card> selectDiscards(Player current, List<Card> cards, int count) {
        if (cards == null || count < 0 || cards.size() < count) {
            return Collections.emptyList();
        }
        return new ArrayList<>(cards.subList(cards.size() - count, cards.size()));
    }

    // Pick the first available property card
    @Override
    public Card selectPropertyCard(Player owner, List<Card> cards, String prompt) {
        return cards == null || cards.isEmpty() ? null : cards.get(0);
    }

    // Choose enough cards to cover the requested payment
    @Override
    public List<Card> selectPaymentCards(Player owner, List<Card> cards, int amount) {
        if (cards == null || cards.isEmpty()) {
            return Collections.emptyList();
        }

        List<Card> selected = new ArrayList<>();
        int total = 0;
        for (Card card : cards) {
            selected.add(card);
            total += card.getBankValue();
            if (total >= amount) {
                break;
            }
        }
        return selected;
    }

    // Return the configured confirmation answer
    @Override
    public boolean reconfirm(String prompt) {
        return reconfirmAnswer;
    }
}
