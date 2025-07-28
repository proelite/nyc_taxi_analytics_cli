package com.xiaodi.taxi.etl.model;

public record NormalizedColumns(String pickupColumn, String dropoffColumn, String taxiType) {
    public boolean hasTaxiType() {
        return taxiType != null;
    }
}
