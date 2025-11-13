package com.labhub.CveLabhubBack.cve_lab.entity;

public enum LabStatus {
    ACTIVE,
    TERMINATED;

    public static LabStatus from(String value) {
        if (value == null) {
            return ACTIVE;
        }
        return switch (value.toLowerCase()) {
            case "terminated", "destroyed" -> TERMINATED;
            default -> ACTIVE;
        };
    }
}

