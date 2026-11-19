package uz.mukhammad.projectplanner.risk.records;

import java.util.ArrayList;
import java.util.List;

public record ProjectHealth(boolean atRisk, boolean manual, long overdue, long blocked) {

    public String label() {
        return atRisk ? "🔴 At risk" : "🟢 On track";
    }

    public String reason() {
        List<String> parts = new ArrayList<>();
        if (manual) {
            parts.add("flagged by PM");
        }
        if (overdue > 0) {
            parts.add(overdue + " overdue");
        }
        if (blocked > 0) {
            parts.add(blocked + " blocked");
        }
        return String.join(", ", parts);
    }
}
