package com.example.lulufindy;

public class WorkingHoursData {
    public String date;
    public String fromTime;
    public String toTime;
    public boolean isHoliday;
    public boolean isSpecial;

    public WorkingHoursData() {

    }

    public WorkingHoursData(String date, String fromTime, String toTime, boolean isHoliday, boolean isSpecial) {
        this.date = date;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.isHoliday = isHoliday;
        this.isSpecial = isSpecial;
    }
}

