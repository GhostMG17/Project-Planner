package uz.mukhammad.projectplanner.risk.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RiskSeverity {

    LOW("⚪", "Low"),
    MEDIUM("🟡", "Medium"),
    HIGH("🟠", "High"),
    CRITICAL("🔴", "Critical");

    private final String icon;
    private final String label;
}
