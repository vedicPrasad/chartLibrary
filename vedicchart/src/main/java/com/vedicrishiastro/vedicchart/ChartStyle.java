package com.vedicrishiastro.vedicchart;

public enum ChartStyle {
    NORTH,
    SOUTH,
    EAST;

    public static ChartStyle from(int value) {
        if (value == 1) {
            return SOUTH;
        }
        if (value == 2) {
            return EAST;
        }
        return NORTH;
    }

    public static ChartStyle from(String value) {
        if (value == null) {
            return NORTH;
        }
        String normalized = value.trim().toLowerCase();
        if ("south".equals(normalized) || "south_indian".equals(normalized) || "southindian".equals(normalized)) {
            return SOUTH;
        }
        if ("east".equals(normalized) || "east_indian".equals(normalized) || "eastindian".equals(normalized)) {
            return EAST;
        }
        return NORTH;
    }
}
