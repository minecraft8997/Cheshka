package com.deewend.cheshka;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.text.DecimalFormat;

public class StatisticsActivity extends CheshkaActivity {
    private static final DecimalFormat NUMBER_FORMATTER = new DecimalFormat("#.##");

    /** @noinspection DataFlowIssue*/
    @Override
    @SuppressLint("SetTextI18n")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_statistics);

        Bundle extras = getIntent().getExtras();
        if (extras == null) throw new IllegalStateException("No data provided");

        String opponentName = extras.getString("opponentName");
        int[] myStats = extras.getIntArray("myStats");
        int[] myOverallStats = extras.getIntArray("myOverallStats");
        int[] opponentStats = extras.getIntArray("opponentStats");
        int[] opponentOverallStats = extras.getIntArray("opponentOverallStats");
        Helper.requireNonNull("Bad intent bundle",
                opponentName, myStats, myOverallStats, opponentStats, opponentOverallStats);

        ((TextView) findViewById(R.id.opponent_name_view)).setText(opponentName);

        int sumMyStat = 0;
        int sumOpponentStat = 0;
        int sumMyOverallStat = 0;
        int sumOpponentOverallStat = 0;
        LinearLayout contents = findViewById(R.id.statistics_contents);
        for (int i = 0; i < 7; i++) {
            boolean sumEntry = (i == 6);

            LinearLayout entry = (LinearLayout) getLayoutInflater()
                    .inflate(R.layout.statistics_entry, contents, false);

            int n = i + 1;
            DiceView diceView = entry.findViewById(R.id.dice_face_view);
            if (sumEntry) {
                diceView.setVisibility(View.GONE);
                entry.findViewById(R.id.sum_view).setVisibility(View.VISIBLE);
            } else {
                diceView.setDigit(n);
                diceView.setMode(DiceView.MODE_ROLLING_EXACT_DIGIT);
                runCheshkaView(diceView);
                String contentDescription = getString(R.string.dice_face_view_description_text, n);
                diceView.setContentDescription(contentDescription);
                diceView.setOnClickListener((v) -> ((DiceView) v).diceFrame = 0);
            }
            TextView myCount = entry.findViewById(R.id.my_count);
            TextView opponentCount = entry.findViewById(R.id.opponent_count);
            TextView myOverallCount = entry.findViewById(R.id.my_overall_count);
            TextView opponentOverallCount = entry.findViewById(R.id.opponent_overall_count);

            int myStat;
            int opponentStat;
            int myOverallStat;
            int opponentOverallStat;
            if (sumEntry) {
                myStat = sumMyStat;
                opponentStat = sumOpponentStat;
                myOverallStat = sumMyOverallStat;
                opponentOverallStat = sumOpponentOverallStat;
            } else {
                myStat = myStats[i];
                opponentStat = opponentStats[i];
                myOverallStat = myOverallStats[i];
                opponentOverallStat = opponentOverallStats[i];

                sumMyStat += myStat;
                sumOpponentStat += opponentStat;
                sumMyOverallStat += myOverallStat;
                sumOpponentOverallStat += opponentOverallStat;
            }
            myCount.setText(String.valueOf(myStat));
            myOverallCount.setText("/" + myOverallStat);
            opponentCount.setText(String.valueOf(opponentStat));
            opponentOverallCount.setText("/" + opponentOverallStat);

            int color = 0xFFB2BEB5;
            String sign = "";
            double percentage = 0.0D;
            if (myStat > opponentStat) {
                color = 0xFFC8F336;
                sign = "+";
                percentage = (myStat - opponentStat) / (double) opponentStat;
            }
            if (myStat < opponentStat) {
                color = 0xFFFF0000;
                sign = "-";
                percentage = (opponentStat - myStat) / (double) myStat;
            }
            percentage *= 100.0D;

            TextView difference = entry.findViewById(R.id.difference);
            difference.setTextColor(color);
            difference.setText(sign + NUMBER_FORMATTER.format(percentage) + "%");

            contents.addView(entry, n);
        }
    }

    @Override
    public void onBackPressed() {
        onBackPressed0();
    }
}
