package com.menamiit.smartcityproject.entity;

public enum ComplaintCategory {
    WATER,
    STREET_LIGHT,
    ROAD,
    SANITATION,
    DRAINAGE,
    PARK,
    ELECTRICITY,
    OTHER;

    public Department mappedDepartment() {
        return switch (this) {
            case WATER -> Department.WATER_SUPPLY;
            case STREET_LIGHT, ELECTRICITY -> Department.ELECTRICAL;
            case ROAD, DRAINAGE -> Department.PUBLIC_WORKS;
            case SANITATION -> Department.SANITATION;
            case PARK -> Department.PARKS;
            case OTHER -> Department.GENERAL;
        };
    }
}
