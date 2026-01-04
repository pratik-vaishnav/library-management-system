package com.assistive.library.desktop.ui;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;

public final class AutoCompleteSupport {
  private AutoCompleteSupport() {
  }

  public record Item(String display, String value) {
  }

  public static void bind(TextField field, Supplier<List<Item>> suggestionsSupplier) {
    bind(field, suggestionsSupplier, false, 1);
  }

  public static void bind(TextField field, Supplier<List<Item>> suggestionsSupplier, boolean showOnFocus) {
    bind(field, suggestionsSupplier, showOnFocus, 1);
  }

  public static void bind(TextField field,
                          Supplier<List<Item>> suggestionsSupplier,
                          boolean showOnFocus,
                          int minChars) {
    ContextMenu menu = new ContextMenu();
    menu.setAutoHide(true);

    field.textProperty().addListener((obs, oldValue, newValue) -> {
      showSuggestions(field, menu, suggestionsSupplier, newValue, minChars);
    });

    field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
      if (!isFocused) {
        menu.hide();
        return;
      }
      if (showOnFocus) {
        showSuggestions(field, menu, suggestionsSupplier, field.getText(), minChars);
      }
    });
  }

  private static void showSuggestions(TextField field,
                                      ContextMenu menu,
                                      Supplier<List<Item>> suggestionsSupplier,
                                      String value,
                                      int minChars) {
    String input = value == null ? "" : value.trim();
    if (input.length() < minChars) {
      menu.hide();
      return;
    }
    String filter = input.toLowerCase(Locale.ROOT);
    List<Item> suggestions = suggestionsSupplier.get().stream()
        .filter(item -> filter.isEmpty()
            || item.display().toLowerCase(Locale.ROOT).contains(filter)
            || item.value().toLowerCase(Locale.ROOT).contains(filter))
        .limit(8)
        .toList();
    if (suggestions.isEmpty()) {
      menu.hide();
      return;
    }
    menu.getItems().setAll(suggestions.stream().map(item -> {
      MenuItem menuItem = new MenuItem(item.display());
      menuItem.setOnAction(event -> {
        field.setText(item.value());
        field.positionCaret(field.getText().length());
        menu.hide();
      });
      return menuItem;
    }).toList());

    if (!menu.isShowing()) {
      menu.show(field, Side.BOTTOM, 0, 0);
    }
  }
}
