package ie.ucd.monopolydeal.game;

import ie.ucd.monopolydeal.model.MoneyCard;
import ie.ucd.monopolydeal.model.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Tests played and discarded card history
class CardHistoryTest {

    // New records are stored newest first
    @Test
    void addShouldStoreNewestUsedCardFirst() {
        CardHistory history = new CardHistory();
        Player player = new Player("Alice", 1);
        MoneyCard first = new MoneyCard("1M", 1);
        MoneyCard second = new MoneyCard("2M", 2);

        history.add(player, first, CardHistory.CardAction.PLAYED);
        history.add(player, second, CardHistory.CardAction.DISCARDED);

        List<CardHistory.UsedCard> records = history.getUsedCards();
        assertSame(second, records.get(0).card());
        assertEquals(CardHistory.CardAction.DISCARDED, records.get(0).action());
        assertSame(first, records.get(1).card());
        assertEquals("Alice", records.get(0).player());
    }

    // Returned history list is a copy
    @Test
    void getUsedCardsShouldReturnCopy() {
        CardHistory history = new CardHistory();
        Player player = new Player("Alice", 1);
        MoneyCard card = new MoneyCard("1M", 1);
        history.add(player, card, CardHistory.CardAction.PLAYED);

        List<CardHistory.UsedCard> copy = history.getUsedCards();
        copy.clear();

        assertEquals(1, history.getUsedCards().size());
    }

    // Restore replaces the current history with a saved list
    @Test
    void restoreShouldReplaceHistory() {
        CardHistory history = new CardHistory();
        Player player = new Player("Alice", 1);
        MoneyCard oldCard = new MoneyCard("1M", 1);
        MoneyCard savedCard = new MoneyCard("2M", 2);
        List<CardHistory.UsedCard> saved = List.of(
                new CardHistory.UsedCard(CardHistory.CardAction.DISCARDED, "Bob", savedCard)
        );
        history.add(player, oldCard, CardHistory.CardAction.PLAYED);

        history.restore(saved);

        assertEquals(1, history.getUsedCards().size());
        assertSame(savedCard, history.getUsedCards().get(0).card());
        assertEquals("Bob", history.getUsedCards().get(0).player());
    }
}
