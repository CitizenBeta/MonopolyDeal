package ie.ucd.monopolydeal.model;

import java.util.ArrayList;
import java.util.List;

public class ActionCard extends Card {
    private final ActionType actionType;
    private final List<PropertyColor> colors;

    public ActionCard(String name, int bankValue, ActionType actionType) {
        super(name, bankValue);
        this.actionType = actionType;
        this.colors = new ArrayList<>();
    }

    public ActionCard(String name, int bankValue, ActionType actionType, List<PropertyColor> colors) {
        super(name, bankValue);
        this.actionType = actionType;
        this.colors = colors;
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
