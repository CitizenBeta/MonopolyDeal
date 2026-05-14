package ie.ucd.monopolydeal.model;

public enum ActionType {
    TODAY_IS_MY_BIRTHDAY("All players pay you 2M."),
    PASS_GO("Draw 2 extra cards."),
    DEBT_COLLECTOR("Force any player to pay you 5M."),
    RENT("All players pay rent for one listed color."),
    MULTI_RENT("One player pays rent for any one color."),
    DOUBLE_RENT("Play with a rent card to double rent."),
    SLY_DEAL("Steal one property. Not from a full set."),
    FORCED_DEAL("Swap one property. Not from a full set."),
    DEAL_BREAKER("Steal a complete property set."),
    HOUSE("Add to a full set to add 3M rent."),
    HOTEL("Add to a full set with a house to add 4M rent."),
    JUST_SAY_NO("Cancel an action played against you.");

    private final String description;

    ActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
