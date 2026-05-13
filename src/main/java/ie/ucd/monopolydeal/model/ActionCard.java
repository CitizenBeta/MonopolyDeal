package ie.ucd.monopolydeal.model;

import java.util.ArrayList;
import java.util.List;

public class ActionCard implements Card {
    private final String name;
    private final int bankValue;
    private final ActionType actionType;
    private final List<PropertyColor> colors;

    public ActionCard(String name, int bankValue, ActionType actionType) {
        this.name = name;
        this.bankValue = bankValue;
        this.actionType = actionType;
        this.colors = new ArrayList<>();
    }

    public ActionCard(String name, int bankValue, ActionType actionType,List<PropertyColor> colors){
        this.name = name;
        this.bankValue = bankValue;
        this.actionType = actionType;
        this.colors = colors;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getBankValue() {
        return bankValue;
    }

    @Override
    public String getDetail() {
        return name + " [Action, " + actionType + ", bank " + bankValue + "M]";
    }

    public ActionType getActionType() {
        return actionType;
    }

    public List<PropertyColor> getColors() {
        return colors;
    }

    @Override
    public String toString() {
        return getDetail();
    }
}
