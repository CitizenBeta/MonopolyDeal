package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScriptedDecisionMaker implements DecisionMaker {
    private final UseMode useMode;
    private final boolean reconfirmAnswer;

    public ScriptedDecisionMaker() {
        this(UseMode.PLAY, false);
    }

    public ScriptedDecisionMaker(UseMode useMode, boolean reconfirmAnswer) {
        this.useMode = useMode;
        this.reconfirmAnswer = reconfirmAnswer;
    }

    @Override
    public Player selectNextPlayer(Player currentPlayer, List<Player> players, String prompt) {
        return players == null || players.isEmpty() ? null : players.get(0);
    }

    @Override
    public PropertyColor selectColor(String prompt, List<PropertyColor> colors) {
        return colors == null || colors.isEmpty() ? null : colors.get(0);
    }

    @Override
    public UseMode useCard(ActionCard action) {
        return useMode;
    }

    @Override
    public WildPropertyCard selectWildCardToMove(Player current, List<WildPropertyCard> wildCards) {
        return wildCards == null || wildCards.isEmpty() ? null : wildCards.get(0);
    }

    @Override
    public List<Card> selectDiscards(Player current, List<Card> cards, int count) {
        if (cards == null || count < 0 || cards.size() < count) {
            return Collections.emptyList();
        }
        return new ArrayList<>(cards.subList(cards.size() - count, cards.size()));
    }

    @Override
    public Card selectPropertyCard(Player owner, List<Card> cards, String prompt) {
        return cards == null || cards.isEmpty() ? null : cards.get(0);
    }

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

    @Override
    public boolean reconfirm(String prompt) {
        return reconfirmAnswer;
    }
}
