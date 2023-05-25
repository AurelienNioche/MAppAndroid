package com.aureliennioche.stepcounterplugin;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.sql.Date;

@Entity
public class StepRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo()
    public Date date;

    @ColumnInfo(name = "step_number")
    public int stepNumber;

    public StepRecord(int id, Date date, int stepNumber) {
        this.id = id;
        this.date = date;
        this.stepNumber = stepNumber;
    }

    @Ignore
    public StepRecord(Date date, int stepNumber) {
        this.date = date;
        this.stepNumber = stepNumber;
    }
}
