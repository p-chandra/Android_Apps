package com.example.dateideagenerator;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int ACCENT = Color.rgb(166, 66, 83);
    private static final int INK = Color.rgb(42, 33, 35);
    private static final int MUTED = Color.rgb(116, 101, 103);
    private final List<Idea> ideas = new ArrayList<>();
    private final Set<String> shown = new HashSet<>();
    private final Random random = new Random();
    private final List<Button> stageButtons = new ArrayList<>(), moodButtons = new ArrayList<>(), energyButtons = new ArrayList<>();
    private int stage;
    private String mood, energy;
    private TextView matchCount, resultTitle, resultDescription;
    private LinearLayout resultCard;
    private Button generate;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        loadIdeas();
        setContentView(buildScreen());
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(248, 243, 239));
        LinearLayout page = column();
        page.setPadding(dp(22), dp(34), dp(22), dp(40));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));

        TextView mark = text("DATE NIGHT, SOLVED", 11, ACCENT);
        mark.setTypeface(Typeface.DEFAULT_BOLD); mark.setLetterSpacing(.18f);
        page.addView(mark);
        TextView heading = text("What should\nwe do tonight?", 37, INK);
        heading.setTypeface(Typeface.create("sans", Typeface.BOLD));
        heading.setLineSpacing(0, .92f);
        page.addView(heading, margins(0, 10, 0, 8));
        page.addView(text("A little chemistry, a little spontaneity. Pick your vibe and we’ll handle the plan.", 16, MUTED), margins(0, 0, 0, 27));

        page.addView(sectionTitle("01", "Relationship stage"));
        LinearLayout stages = row(); page.addView(stages, margins(0, 11, 0, 24));
        addOption(stages, "New", 1, null, stageButtons);
        addOption(stages, "Serious", 2, null, stageButtons);
        addOption(stages, "Long-term", 3, null, stageButtons);

        page.addView(sectionTitle("02", "Activity type"));
        LinearLayout moods = row(); page.addView(moods, margins(0, 11, 0, 24));
        addOption(moods, "Indoor", 0, "Indoor", moodButtons);
        addOption(moods, "Outdoor", 0, "Outdoor", moodButtons);

        page.addView(sectionTitle("03", "Energy level"));
        LinearLayout energies = row(); page.addView(energies, margins(0, 11, 0, 20));
        addOption(energies, "Relaxed", 0, "Relaxed", energyButtons);
        addOption(energies, "Active", 0, "Active", energyButtons);

        matchCount = text("Choose your preferences to see matches", 13, MUTED);
        matchCount.setGravity(Gravity.CENTER);
        page.addView(matchCount, margins(0, 2, 0, 12));

        generate = new Button(this);
        generate.setText("Find our date"); generate.setTextSize(15); generate.setTypeface(Typeface.DEFAULT_BOLD);
        generate.setTextColor(Color.WHITE); generate.setEnabled(false); generate.setAlpha(.45f);
        generate.setBackground(rounded(ACCENT, 14, Color.TRANSPARENT));
        generate.setOnClickListener(v -> generateIdea());
        page.addView(generate, fixedHeight(58));

        resultCard = column(); resultCard.setVisibility(View.GONE);
        resultCard.setPadding(dp(20), dp(20), dp(20), dp(20));
        resultCard.setBackground(rounded(Color.WHITE, 18, Color.rgb(225, 211, 211)));
        page.addView(resultCard, margins(0, 22, 0, 0));
        TextView label = text("YOUR DATE IDEA", 11, ACCENT); label.setTypeface(Typeface.DEFAULT_BOLD); label.setLetterSpacing(.14f);
        resultCard.addView(label);
        resultTitle = text("", 25, INK); resultTitle.setTypeface(Typeface.DEFAULT_BOLD);
        resultCard.addView(resultTitle, margins(0, 9, 0, 8));
        resultDescription = text("", 16, MUTED); resultDescription.setLineSpacing(dp(5), 1f);
        resultCard.addView(resultDescription);
        return scroll;
    }

    private void addOption(LinearLayout parent, String title, int stageValue, String textValue, List<Button> group) {
        Button button = new Button(this);
        button.setText(title); button.setTextSize(14); button.setAllCaps(false); button.setTextColor(INK);
        button.setBackground(rounded(Color.WHITE, 12, Color.rgb(218, 204, 202)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1);
        params.setMargins(dp(4), 0, dp(4), 0); parent.addView(button, params); group.add(button);
        button.setOnClickListener(v -> {
            if (stageValue > 0) stage = stageValue;
            if ("Indoor".equals(textValue) || "Outdoor".equals(textValue)) mood = textValue;
            if ("Relaxed".equals(textValue) || "Active".equals(textValue)) energy = textValue;
            select(group, button); shown.clear(); resultCard.setVisibility(View.GONE); updateState();
        });
    }

    private void select(List<Button> group, Button selected) {
        for (Button button : group) {
            boolean active = button == selected;
            button.setTextColor(active ? Color.WHITE : INK);
            button.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
            button.setBackground(rounded(active ? ACCENT : Color.WHITE, 12, active ? ACCENT : Color.rgb(218,204,202)));
        }
    }

    private void updateState() {
        int count = matchingIdeas().size();
        boolean ready = stage > 0 && mood != null && energy != null;
        matchCount.setText(count + (count == 1 ? " matching date idea" : " matching date ideas"));
        generate.setEnabled(ready); generate.setAlpha(ready ? 1f : .45f);
    }

    private List<Idea> matchingIdeas() {
        List<Idea> matches = new ArrayList<>();
        for (Idea idea : ideas) {
            if ((stage == 0 || idea.stage <= stage) && (mood == null || idea.mood.equals(mood) || idea.mood.equals("Mixed")) && (energy == null || idea.energy.equals(energy))) matches.add(idea);
        }
        return matches;
    }

    private void generateIdea() {
        List<Idea> matches = matchingIdeas();
        List<Idea> available = new ArrayList<>();
        for (Idea idea : matches) if (!shown.contains(idea.title)) available.add(idea);
        if (available.isEmpty()) { shown.clear(); available.addAll(matches); }
        if (available.isEmpty()) return;
        Idea idea = available.get(random.nextInt(available.size())); shown.add(idea.title);
        resultTitle.setText(idea.title); resultDescription.setText(idea.description.trim()); resultCard.setVisibility(View.VISIBLE);
        generate.setText("Give us another idea"); resultCard.announceForAccessibility("Recommended date: " + idea.title);
    }

    private void loadIdeas() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.date_ideas)));
            StringBuilder json = new StringBuilder(); String line;
            while ((line = reader.readLine()) != null) json.append(line);
            JSONArray values = new JSONArray(json.toString());
            for (int i=0; i<values.length(); i++) {
                JSONObject item = values.getJSONObject(i);
                ideas.add(new Idea(item.getString("title"), item.getString("description"), item.getInt("dating-length"), item.getString("mood"), item.getString("energy")));
            }
        } catch (Exception error) { throw new IllegalStateException("Unable to load date ideas", error); }
    }

    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);return v;}
    private TextView text(String s,float z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);return v;}
    private TextView sectionTitle(String n,String title){TextView v=text(n+"   "+title.toUpperCase(),12,INK);v.setTypeface(Typeface.DEFAULT_BOLD);v.setLetterSpacing(.08f);return v;}
    private GradientDrawable rounded(int c,int r,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
    private LinearLayout.LayoutParams margins(int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private LinearLayout.LayoutParams fixedHeight(int h){return new LinearLayout.LayoutParams(-1,dp(h));}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private static final class Idea { final String title,description,mood,energy; final int stage; Idea(String t,String d,int s,String m,String e){title=t;description=d;stage=s;mood=m;energy=e;} }
}
