package com.toolbox.shizutools;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.ListPopupWindow;
import com.google.android.material.color.DynamicColors;
import com.toolbox.shizutools.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        String savedTheme = ThemeHelper.getSavedTheme(this);
        applyNightMode(savedTheme);

        DynamicColors.applyToActivityIfAvailable(this);

        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.switchBlackMode.setChecked(ThemeHelper.isBlackModeEnabled(this));

        updateBlackModeVisibility(savedTheme);

        binding.txtCurrentTheme.setText(savedTheme);

        binding.switchBlackMode.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    ThemeHelper.setBlackMode(this, isChecked);
                    ThemeHelper.applyAmoledMode(
                            this, binding.getRoot(), binding.appBar, binding.toolbar);
                });

        setupDarkThemePopup();

        applyAmoledIfEnabled();

        setupLanguagePopup();
    }

    private void applyNightMode(String theme) {
        switch (theme) {
            case "Always on":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "Always off":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void applyAmoledIfEnabled() {
        ThemeHelper.applyAmoledMode(this, binding.getRoot(), binding.appBar, binding.toolbar);
    }

    private void updateBlackModeVisibility(String theme) {
        if ("Always off".equals(theme)) {
            binding.layoutBlackMode.setVisibility(View.GONE);
            ThemeHelper.setBlackMode(this, false);
        } else {
            binding.layoutBlackMode.setVisibility(View.VISIBLE);
        }
    }

    private void setupDarkThemePopup() {
        String[] options = {"Always off", "Always on", "Follow system"};
        ListPopupWindow popupWindow = new ListPopupWindow(this);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, options);

        popupWindow.setAdapter(adapter);
        popupWindow.setAnchorView(binding.btnDarkTheme);
        popupWindow.setModal(true);

        binding.btnDarkTheme.setOnClickListener(v -> popupWindow.show());

        popupWindow.setOnItemClickListener(
                (parent, view, position, id) -> {
                    String selected = options[position];

                    ThemeHelper.applyTheme(this, selected);

                    updateBlackModeVisibility(selected);

                    popupWindow.dismiss();
                    recreate();
                });
    }

    private void setupLanguagePopup() {
        String[] langs = {"Indonesia", "English"};
        String[] codes = {"in", "en"};

        ListPopupWindow popupWindow = new ListPopupWindow(this);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, langs);

        popupWindow.setAdapter(adapter);
        popupWindow.setAnchorView(binding.btnLanguage);
        popupWindow.setModal(true);

        String currentCode = ThemeHelper.getLocale(this);
        binding.txtCurrentLang.setText(currentCode.equals("en") ? "English" : "Indonesia");

        binding.btnLanguage.setOnClickListener(v -> popupWindow.show());

        popupWindow.setOnItemClickListener(
                (parent, view, position, id) -> {
                    String selectedCode = codes[position];
                    String selectedName = langs[position];

                    ThemeHelper.setLocale(this, selectedCode);
                    ThemeHelper.applyLanguage(this);

                    binding.txtCurrentLang.setText(selectedName);

                    popupWindow.dismiss();

                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
    }
}
