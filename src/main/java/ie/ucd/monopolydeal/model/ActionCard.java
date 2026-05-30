package ie.ucd.monopolydeal.model;

import java.util.List;
import java.util.Objects;

public class ActionCard extends Card {
    private final ActionType actionType;
    private final List<PropertyColor> colors;

    public ActionCard(String name, int bankValue, ActionType actionType) {
        super(name, bankValue);
        this.actionType = Objects.requireNonNull(actionType, "actionType");
        this.colors = List.of();
    }

    public ActionCard(String name, int bankValue, ActionType actionType, List<PropertyColor> colors) {
        super(name, bankValue);
        this.actionType = Objects.requireNonNull(actionType, "actionType");
        if (colors == null) {
            this.colors = List.of();
        } else {
            this.colors = List.copyOf(colors);
        }
    }

    @Override
    public String getDetail() {
        return getName() + " [Action, " + actionType + ", bank " + getBankValue() + "M]";
    }

    public ActionType getActionType() {
        return actionType;
    }

    public List<PropertyColor> getColors() {
        return colors;
    }
}
