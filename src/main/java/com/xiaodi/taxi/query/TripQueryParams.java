package com.xiaodi.taxi.query;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Value object holding aggregation parameters.
 */
public final class TripQueryParams {
    public static String EMPTY_VALUE = "*";

    private final String pickupDatetime;
    private final String dropoffDatetime;
    private final String puLocationID;
    private final String doLocationID;
    private final boolean groupByPayment;
    private final String vendorID;
    private final String taxiType;

    @Contract(pure = true)
    private TripQueryParams(@NotNull Builder b) {
        this.pickupDatetime = b.pickupDatetime;
        this.dropoffDatetime = b.dropoffDatetime;
        this.puLocationID = b.puLocationID;
        this.doLocationID = b.doLocationID;
        this.groupByPayment = b.groupByPayment;
        this.vendorID = b.vendorID;
        this.taxiType = b.taxiType;
    }

    @Contract(" -> new")
    public static @NotNull Builder builder() { return new Builder(); }
    public static final class Builder {
        private String pickupDatetime = EMPTY_VALUE;
        private String dropoffDatetime = EMPTY_VALUE;
        private String puLocationID = EMPTY_VALUE;
        private String doLocationID = EMPTY_VALUE;
        private boolean groupByPayment;
        private String vendorID = EMPTY_VALUE;
        private String taxiType = EMPTY_VALUE;

        public Builder pickupDatetime(String dt) { this.pickupDatetime = dt; return this; }
        public Builder dropoffDatetime(String dt) { this.dropoffDatetime = dt; return this; }
        public Builder puLocationID(String id) { this.puLocationID = id; return this; }
        public Builder doLocationID(String id) { this.doLocationID = id; return this; }
        public Builder groupByPayment(boolean flag) { this.groupByPayment = flag; return this; }
        public Builder vendorID(String id) { this.vendorID = id; return this; }
        public Builder taxiType(String type) { this.taxiType = type; return this; }
        @Contract(value = " -> new", pure = true)
        public @NotNull TripQueryParams build() { return new TripQueryParams(this); }
    }

    // getters...
    public String getPickupDatetime() { return pickupDatetime; }
    public String getDropoffDatetime() { return dropoffDatetime; }
    public String getPuLocationID() { return puLocationID; }
    public String getDoLocationID() { return doLocationID; }
    public boolean isGroupByPayment() { return groupByPayment; }
    public String getVendorID() { return vendorID; }
    public String getTaxiType() { return taxiType; }
}
