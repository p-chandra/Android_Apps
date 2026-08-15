package com.example.androidautotemplate;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.car.app.SessionInfo;
import androidx.car.app.model.Action;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.validation.HostValidator;

public final class TemplateCarAppService extends CarAppService {
    @NonNull
    @Override
    public HostValidator createHostValidator() {
        // Development only. Restrict trusted hosts if this becomes a real app.
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    @NonNull
    @Override
    public Session onCreateSession(@NonNull SessionInfo sessionInfo) {
        return new TemplateSession();
    }

    private static final class TemplateSession extends Session {
        @NonNull
        @Override
        public Screen onCreateScreen(@NonNull Intent intent) {
            return new HomeScreen(getCarContext());
        }
    }

    private static final class HomeScreen extends Screen {
        HomeScreen(CarContext carContext) {
            super(carContext);
        }

        @NonNull
        @Override
        public Template onGetTemplate() {
            Row status = new Row.Builder()
                    .setTitle("Template connected")
                    .addText("Replace this pane with your Android Auto experience.")
                    .build();

            Pane pane = new Pane.Builder()
                    .addRow(status)
                    .addAction(new Action.Builder()
                            .setTitle("Refresh")
                            .setOnClickListener(this::invalidate)
                            .build())
                    .build();

            return new PaneTemplate.Builder(pane)
                    .setTitle("Android Auto Template")
                    .setHeaderAction(Action.APP_ICON)
                    .build();
        }
    }
}
