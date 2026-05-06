package com.example.lulufindy;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kizitonwose.calendarview.CalendarView;
import com.kizitonwose.calendarview.model.CalendarDay;
import com.kizitonwose.calendarview.model.CalendarMonth;
import com.kizitonwose.calendarview.model.DayOwner;
import com.kizitonwose.calendarview.ui.DayBinder;
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Calendar extends AppCompatActivity {

    private CalendarView calendarView;
    private final Map<String, WorkingHoursData> workingHoursMap = new HashMap<>();
    private TextView monthTitle;
    private Button backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        backBtn = findViewById(R.id.backBTN);
        backBtn.setOnClickListener(v -> finish());

        calendarView = findViewById(R.id.calendarView);
        monthTitle = findViewById(R.id.monthTitle);
        ImageButton btnPrev = findViewById(R.id.btnPrevMonth);
        ImageButton btnNext = findViewById(R.id.btnNextMonth);

        YearMonth currentMonth = YearMonth.now();
        YearMonth firstMonth = currentMonth.minusMonths(1);
        YearMonth lastMonth = currentMonth.plusMonths(12);
        DayOfWeek firstDayOfWeek = DayOfWeek.MONDAY;

        calendarView.setup(firstMonth, lastMonth, firstDayOfWeek);
        calendarView.scrollToMonth(currentMonth);

        calendarView.setMonthScrollListener(month -> {
            String label = month.getYearMonth().getMonth()
                    .getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.getYearMonth().getYear();
            monthTitle.setText(label);
            return null;
        });

        btnPrev.setOnClickListener(v -> {
            CalendarMonth visibleMonth = calendarView.findFirstVisibleMonth();
            if (visibleMonth != null) {
                calendarView.scrollToMonth(visibleMonth.getYearMonth().minusMonths(1));
            }
        });

        btnNext.setOnClickListener(v -> {
            CalendarMonth visibleMonth = calendarView.findFirstVisibleMonth();
            if (visibleMonth != null) {
                calendarView.scrollToMonth(visibleMonth.getYearMonth().plusMonths(1));
            }
        });

        calendarView.setOnTouchListener((v, event) -> true);

        calendarView.setDayBinder(new DayBinder<DayViewContainer>() {
            @Override
            public DayViewContainer create(View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(DayViewContainer container, CalendarDay day) {
                container.day = day;
                container.textView.setText(String.valueOf(day.getDate().getDayOfMonth()));
                container.textView.setBackgroundColor(Color.TRANSPARENT);
                container.textView.setTextColor(Color.BLACK);
                container.textView.setOnClickListener(null);

                if (day.getOwner() == DayOwner.THIS_MONTH) {
                    LocalDate date = day.getDate();
                    String formattedDate = String.format("%02d/%02d/%04d",
                            date.getDayOfMonth(), date.getMonthValue(), date.getYear());

                    if (workingHoursMap.containsKey(formattedDate)) {
                        WorkingHoursData data = workingHoursMap.get(formattedDate);
                        if (data.isHoliday) {
                            container.textView.setBackgroundColor(Color.RED);
                            container.textView.setTextColor(Color.WHITE);
                        } else if (data.isSpecial) {
                            container.textView.setBackgroundColor(Color.BLUE);
                            container.textView.setTextColor(Color.WHITE);
                        } else {
                            container.textView.setBackgroundColor(Color.GREEN);
                            container.textView.setTextColor(Color.WHITE);
                        }

                        container.textView.setOnClickListener(v -> {
                            String message = "Ημερομηνία: " + data.date + "\n" +
                                    "ΩΡΕΣ ΛΕΙΤΟΥΡΓΙΑΣ" +"\n" +
                                    "Από: " + (data.fromTime != null ? data.fromTime : "-") + "\n" +
                                    "Έως: " + (data.toTime != null ? data.toTime : "-") ;

                            new AlertDialog.Builder(Calendar.this)
                                    .setTitle("Πληροφορίες Ημέρας")
                                    .setMessage(message)
                                    .setPositiveButton("OK", null)
                                    .show();
                        });
                    }
                } else {
                    container.textView.setTextColor(Color.LTGRAY);
                }
            }
        });

        calendarView.setMonthHeaderBinder(new MonthHeaderFooterBinder<MonthViewContainer>() {
            @Override
            public MonthViewContainer create(View view) {
                return new MonthViewContainer(view);
            }

            @Override
            public void bind(MonthViewContainer container, CalendarMonth month) {

            }
        });

        fetchWorkingHoursFromFirebase();
    }

    private void fetchWorkingHoursFromFirebase() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("working_hours");

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                workingHoursMap.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    WorkingHoursData data = child.getValue(WorkingHoursData.class);
                    if (data != null && data.date != null) {
                        workingHoursMap.put(data.date, data);
                    }
                }

                calendarView.notifyCalendarChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }
}
