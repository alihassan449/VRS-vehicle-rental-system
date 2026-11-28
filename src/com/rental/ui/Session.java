package com.rental.ui;

import com.rental.model.Staff;

/** Holds the staff member who is currently logged in (chosen in the header of the main window). */
public class Session {

    private Staff currentStaff;

    public Session(Staff currentStaff) {
        this.currentStaff = currentStaff;
    }

    public Staff getCurrentStaff() {
        return currentStaff;
    }

    public void setCurrentStaff(Staff currentStaff) {
        this.currentStaff = currentStaff;
    }
}
