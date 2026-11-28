package com.rental.report;

import java.util.ArrayList;
import java.util.List;

/** Result object returned by ReportGenerator (diagram return type "Report"). */
public class Report {

    private final String title;
    private final List<String> lines = new ArrayList<>();

    public Report(String title) {
        this.title = title;
    }

    public void addLine(String line) {
        lines.add(line);
    }

    public String getTitle() {
        return title;
    }

    public List<String> getLines() {
        return lines;
    }

    /** Printable text of the report. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        for (int i = 0; i < title.length(); i++) {
            sb.append('=');
        }
        sb.append("\n\n");
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }
}
