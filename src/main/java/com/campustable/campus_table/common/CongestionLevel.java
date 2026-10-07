package com.campustable.campus_table.common;

public enum CongestionLevel {
    RELAXED, NORMAL, CROWDED, VERY_CROWDED;

    // ponytail: 임시 구간(30/60/85%). 구간이 확정되면 설정값으로 이동
    public static CongestionLevel of(double usageRate) {
        if (usageRate < 30) return RELAXED;
        if (usageRate < 60) return NORMAL;
        if (usageRate < 85) return CROWDED;
        return VERY_CROWDED;
    }
}
