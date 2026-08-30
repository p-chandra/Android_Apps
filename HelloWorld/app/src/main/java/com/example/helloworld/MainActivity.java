package com.example.helloworld;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    private final Calendar startDate = Calendar.getInstance();
    private final Calendar endDate = Calendar.getInstance();
    private TextView startValue, endValue, resultTitle, resultDates, resultDetails;
    private Spinner movieSpinner, firstRowSpinner, lastRowSpinner, firstSeatSpinner, lastSeatSpinner;
    private LinearLayout resultCard;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        endDate.add(Calendar.DAY_OF_MONTH, 7);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(6, 10, 18));
        LinearLayout page = column();
        page.setPadding(dp(24), dp(28), dp(24), dp(32));
        scroll.addView(page, matchWrap());

        TextView eyebrow = text("IMAX SEAT TRACKER", 12, Color.rgb(67, 211, 255));
        eyebrow.setTypeface(Typeface.DEFAULT_BOLD);
        eyebrow.setLetterSpacing(.18f);
        page.addView(eyebrow);
        TextView title = text("Find the best seats.\nBefore they’re gone.", 34, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(title, margins(0, 10, 0, 8));
        TextView intro = text("Track IMAX showtimes at Harkins Arizona Mills 25 in Tempe.", 16, Color.rgb(164,176,195));
        intro.setLineSpacing(dp(4), 1f);
        page.addView(intro, margins(0, 0, 0, 26));

        LinearLayout card = column();
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackground(rounded(Color.rgb(18,26,40), 18, Color.rgb(42,55,75)));
        page.addView(card, matchWrap());
        card.addView(label("THEATER"));
        TextView theater = text("Harkins Arizona Mills 25 — IMAX", 15, Color.WHITE);
        theater.setTypeface(Typeface.DEFAULT_BOLD);
        theater.setGravity(Gravity.CENTER_VERTICAL);
        theater.setPadding(dp(14), 0, dp(10), 0);
        theater.setBackground(rounded(Color.rgb(27,37,54), 12, Color.rgb(53,68,91)));
        card.addView(theater, fixedHeight(54, 0, 8, 0, 18));
        card.addView(label("IMAX MOVIE"));

        movieSpinner = new Spinner(this, Spinner.MODE_DROPDOWN);
        String[] movies = {"The Odyssey"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, movies) {
            @Override public View getView(int p, View v, android.view.ViewGroup parent) {
                TextView t = (TextView) super.getView(p, v, parent);
                t.setTextColor(Color.WHITE); t.setTextSize(15); t.setTypeface(Typeface.DEFAULT_BOLD);
                t.setPadding(dp(14), dp(8), dp(10), dp(8)); return t;
            }
        };
        movieSpinner.setAdapter(adapter);
        movieSpinner.setBackground(rounded(Color.rgb(27,37,54), 12, Color.rgb(53,68,91)));
        card.addView(movieSpinner, fixedHeight(58, 0, 8, 0, 20));

        LinearLayout dates = new LinearLayout(this);
        dates.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(dates, matchWrap());
        startValue = addDateField(dates, "FROM", startDate, true);
        endValue = addDateField(dates, "TO", endDate, false);

        card.addView(label("SEATS TO TRACK"));
        TextView screen = text("SCREEN", 10, Color.rgb(93, 111, 137));
        screen.setGravity(Gravity.CENTER);
        screen.setLetterSpacing(.2f);
        screen.setBackground(rounded(Color.rgb(38, 52, 71), 20, Color.TRANSPARENT));
        card.addView(screen, fixedHeight(28, 28, 2, 28, 12));

        LinearLayout rowRange = new LinearLayout(this);
        rowRange.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(rowRange, matchWrap());
        String[] rows = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M"};
        firstRowSpinner = addChoice(rowRange, "FIRST ROW", rows, 5, true);
        lastRowSpinner = addChoice(rowRange, "LAST ROW", rows, 12, false);

        LinearLayout seatRange = new LinearLayout(this);
        seatRange.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(seatRange, matchWrap());
        String[] seatNumbers = new String[30];
        for (int i = 0; i < seatNumbers.length; i++) seatNumbers[i] = String.valueOf(i + 1);
        firstSeatSpinner = addChoice(seatRange, "FIRST NUMBER", seatNumbers, 9, true);
        lastSeatSpinner = addChoice(seatRange, "LAST NUMBER", seatNumbers, 24, false);
        restoreSeatPreferences();

        Button check = new Button(this);
        check.setText("Check availability");
        check.setTextColor(Color.rgb(3,20,29));
        check.setTextSize(14); check.setTypeface(Typeface.DEFAULT_BOLD); check.setLetterSpacing(.06f);
        check.setBackground(rounded(Color.rgb(67,211,255), 14, Color.TRANSPARENT));
        check.setOnClickListener(v -> showResults());
        card.addView(check, fixedHeight(58, 0, 4, 0, 0));

        resultCard = column();
        resultCard.setPadding(dp(20), dp(20), dp(20), dp(20));
        resultCard.setBackground(rounded(Color.rgb(12,35,43), 18, Color.rgb(39,102,116)));
        resultCard.setVisibility(View.GONE);
        page.addView(resultCard, margins(0, 18, 0, 0));
        resultTitle = text("", 20, Color.WHITE); resultTitle.setTypeface(Typeface.DEFAULT_BOLD);
        resultDates = text("", 14, Color.rgb(139,221,229));
        resultDetails = text("", 15, Color.rgb(214,231,235)); resultDetails.setLineSpacing(dp(5), 1f);
        resultCard.addView(resultTitle);
        resultCard.addView(resultDates, margins(0, 5, 0, 15));
        resultCard.addView(resultDetails);

        TextView note = text("Movie choices are limited to the Arizona Mills IMAX screen. Availability is a preview until a live ticketing source is connected.", 12, Color.rgb(107,121,141));
        note.setGravity(Gravity.CENTER);
        page.addView(note, margins(8, 22, 8, 0));
        return scroll;
    }

    private TextView addDateField(LinearLayout parent, String caption, Calendar date, boolean start) {
        LinearLayout group = column();
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        gp.setMargins(start ? 0 : dp(7), 0, start ? dp(7) : 0, 0);
        parent.addView(group, gp); group.addView(label(caption));
        TextView value = text(format(date), 15, Color.WHITE);
        value.setTypeface(Typeface.DEFAULT_BOLD); value.setGravity(Gravity.CENTER_VERTICAL);
        value.setPadding(dp(12), 0, dp(8), 0);
        value.setBackground(rounded(Color.rgb(27,37,54), 12, Color.rgb(53,68,91)));
        value.setOnClickListener(v -> pickDate(date, value, start));
        group.addView(value, fixedHeight(58, 0, 0, 0, 20)); return value;
    }

    private void pickDate(Calendar date, TextView target, boolean start) {
        DatePickerDialog dialog = new DatePickerDialog(this, (picker, y, m, d) -> {
            date.set(y, m, d, 0, 0, 0);
            if (start && date.after(endDate)) { endDate.setTimeInMillis(date.getTimeInMillis()); endValue.setText(format(endDate)); }
            else if (!start && date.before(startDate)) { Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show(); return; }
            target.setText(format(date)); resultCard.setVisibility(View.GONE);
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); dialog.show();
    }

    private void showResults() {
        String movie = String.valueOf(movieSpinner.getSelectedItem());
        int firstRow = firstRowSpinner.getSelectedItemPosition();
        int lastRow = lastRowSpinner.getSelectedItemPosition();
        int firstSeat = firstSeatSpinner.getSelectedItemPosition() + 1;
        int lastSeat = lastSeatSpinner.getSelectedItemPosition() + 1;
        if (firstRow > lastRow || firstSeat > lastSeat) {
            Toast.makeText(this, "Choose a first row and seat number before the last", Toast.LENGTH_LONG).show();
            return;
        }
        saveSeatPreferences();
        String rowRange = firstRowSpinner.getSelectedItem() + "–" + lastRowSpinner.getSelectedItem();
        String numberRange = firstSeat + "–" + lastSeat;
        long days = Math.max(1, (endDate.getTimeInMillis() - startDate.getTimeInMillis()) / 86400000L + 1);
        resultTitle.setText(movie); resultDates.setText(format(startDate) + "  —  " + format(endDate));
        resultDetails.setText("●  " + Math.min(42, days * 3) + " IMAX showtimes to check\n●  Rows: " + rowRange + "\n●  Seat numbers: " + numberRange);
        resultCard.setVisibility(View.VISIBLE); resultCard.announceForAccessibility("Availability shown for " + movie);
    }

    private Spinner addChoice(LinearLayout parent, String caption, String[] values, int selected, boolean first) {
        LinearLayout group = column();
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        gp.setMargins(first ? 0 : dp(7), 0, first ? dp(7) : 0, 0);
        parent.addView(group, gp);
        group.addView(label(caption));
        Spinner spinner = new Spinner(this, Spinner.MODE_DROPDOWN);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, values) {
            @Override public View getView(int position, View view, android.view.ViewGroup root) {
                TextView choice = (TextView) super.getView(position, view, root);
                choice.setTextColor(Color.WHITE);
                choice.setTextSize(16);
                choice.setTypeface(Typeface.DEFAULT_BOLD);
                choice.setGravity(Gravity.CENTER_VERTICAL);
                choice.setPadding(dp(14), 0, dp(8), 0);
                return choice;
            }
        };
        spinner.setAdapter(adapter);
        spinner.setSelection(selected);
        spinner.setBackground(rounded(Color.rgb(27,37,54), 12, Color.rgb(53,68,91)));
        group.addView(spinner, fixedHeight(54, 0, 0, 0, 16));
        return spinner;
    }

    private void restoreSeatPreferences() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        firstRowSpinner.setSelection(preferences.getInt("first_row", 5));
        lastRowSpinner.setSelection(preferences.getInt("last_row", 12));
        firstSeatSpinner.setSelection(preferences.getInt("first_seat", 9));
        lastSeatSpinner.setSelection(preferences.getInt("last_seat", 24));
    }

    private void saveSeatPreferences() {
        getPreferences(MODE_PRIVATE).edit()
                .putInt("first_row", firstRowSpinner.getSelectedItemPosition())
                .putInt("last_row", lastRowSpinner.getSelectedItemPosition())
                .putInt("first_seat", firstSeatSpinner.getSelectedItemPosition())
                .putInt("last_seat", lastSeatSpinner.getSelectedItemPosition())
                .apply();
    }

    private TextView label(String s) { TextView v=text(s,11,Color.rgb(130,148,173)); v.setTypeface(Typeface.DEFAULT_BOLD); v.setLetterSpacing(.12f); v.setPadding(0,0,0,dp(7)); return v; }
    private TextView text(String s,float z,int c){ TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);return v; }
    private LinearLayout column(){ LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v; }
    private GradientDrawable rounded(int c,int r,int stroke){ GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d; }
    private LinearLayout.LayoutParams matchWrap(){return new LinearLayout.LayoutParams(-1,-2);}
    private LinearLayout.LayoutParams margins(int l,int t,int r,int b){LinearLayout.LayoutParams p=matchWrap();p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private LinearLayout.LayoutParams fixedHeight(int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(h));p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private String format(Calendar c){DateFormat f=new SimpleDateFormat("MMM d, yyyy",Locale.getDefault());return f.format(c.getTime());}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
