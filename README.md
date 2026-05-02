# M4ESPORTSTICKETING
public abstract class SeatHierarchy {

    private String id;
    private double price;
    private String status;
    private String customerId;

    public SeatHierarchy(String id, double price, String status, String customerId) {
        this.id         = (id != null)         ? id.trim()         : "";
        this.price      = (price >= 0)         ? price             : 0;
        this.status     = (status != null)     ? status.trim()     : "Available";
        this.customerId = (customerId != null) ? customerId.trim() : null;
    }

    // Getters only — no setters, fields cannot be changed from outside
    public String getId()         { return id; }
    public double getPrice()      { return price; }
    public String getStatus()     { return status; }
    public String getCustomerId() { return customerId; }

    // Abstract method — subclasses must implement
    public abstract void displayInfo();
}

// ─── VIP SEAT ──────────────────────────────────────────────────────────────────
class VIPSeat extends SeatHierarchy {

    public VIPSeat(String id, double price, String status, String customerId) {
        super(id, price, status, customerId);
    }

    @Override
    public void displayInfo() {
        System.out.println("  [VIP] Seat: " + getId() + "  Price: P" + getPrice());
    }
}

// ─── REGULAR SEAT ──────────────────────────────────────────────────────────────
class RegularSeat extends SeatHierarchy {

    public RegularSeat(String id, double price, String status, String customerId) {
        super(id, price, status, customerId);
    }

    @Override
    public void displayInfo() {
        System.out.println("  [Regular] Seat: " + getId() + "  Price: P" + getPrice());
    }
}
