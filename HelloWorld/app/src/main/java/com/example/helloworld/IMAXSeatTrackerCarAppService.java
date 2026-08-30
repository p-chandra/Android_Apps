package com.example.helloworld;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.car.app.*;
import androidx.car.app.model.*;
import androidx.car.app.validation.HostValidator;

public class IMAXSeatTrackerCarAppService extends CarAppService {
    @NonNull @Override public HostValidator createHostValidator() { return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR; }
    @NonNull @Override public Session onCreateSession(@NonNull SessionInfo info) {
        return new Session() { @NonNull @Override public Screen onCreateScreen(@NonNull Intent intent) { return new TrackerScreen(getCarContext()); } };
    }
    private static final class TrackerScreen extends Screen {
        TrackerScreen(@NonNull CarContext context) { super(context); }
        @NonNull @Override public Template onGetTemplate() {
            Pane pane = new Pane.Builder()
                    .addRow(new Row.Builder().setTitle("IMAX Seat Tracker").addText("Choose movies and dates in the phone app.").build())
                    .addRow(new Row.Builder().setTitle("Tracking ready").addText("Your availability summary will appear here.").build()).build();
            return new PaneTemplate.Builder(pane).setTitle("IMAX Seat Tracker").setHeaderAction(Action.APP_ICON).build();
        }
    }
}
