package ie.ucd.monopolydeal.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// Shared radio-button dialog helpers
final class DialogChoices {
    private DialogChoices() {
    }

    static <T> T chooseOption(String title, String prompt, List<T> options, Function<T, String> text) {
        return chooseOption(title, prompt, options, text, true);
    }

    static <T> T chooseOption(String title, String prompt, List<T> options, Function<T, String> text,
                              boolean autoChooseSingleOption) {
        // No options means the action cannot continue
        if (options == null || options.isEmpty()) {
            return null;
        }

        // Skip dialog when only one real choice exists
        if (autoChooseSingleOption && options.size() == 1) {
            return options.getFirst();
        }

        // Build base dialog
        Dialog<T> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(prompt);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ToggleGroup group = new ToggleGroup();
        VBox optionsBox = new VBox(8);
        optionsBox.setPadding(new Insets(8));

        // Create one radio button for each option
        List<RadioButton> radioButtons = new ArrayList<>();
        for (T option : options) {
            RadioButton radioButton = new RadioButton(text.apply(option));
            radioButton.setToggleGroup(group);
            radioButton.setWrapText(true);
            radioButton.setMaxWidth(Double.MAX_VALUE);
            radioButtons.add(radioButton);
            optionsBox.getChildren().add(radioButton);
        }
        radioButtons.getFirst().setSelected(true);

        // Use scroll area when the option list is long
        ScrollPane scrollPane = new ScrollPane(optionsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefSize(420, Math.min(280, Math.max(120, options.size() * 38 + 24)));

        // OK requires one selected radio button
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(group.selectedToggleProperty().isNull());

        dialog.getDialogPane().setContent(scrollPane);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                // Map selected radio button back to the original option
                Toggle selectedToggle = group.getSelectedToggle();
                for (int i = 0; i < radioButtons.size(); i++) {
                    if (radioButtons.get(i) == selectedToggle) {
                        return options.get(i);
                    }
                }
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }
}
