module ie.ucd.monopolydeal {
    requires javafx.controls;
    requires javafx.fxml;

    exports ie.ucd.monopolydeal.app;
    exports ie.ucd.monopolydeal.game;
    exports ie.ucd.monopolydeal.model;
    opens ie.ucd.monopolydeal.ui to javafx.fxml;
}
