import java.sql.*;

public class RealTimeRepository {
 
    
    
    
    private final String url =
        "jdbc:sqlserver://LAPTOP-I6GCH1M5:1433;databaseName=M4ESPORTSTICKETING;" +
        "integratedSecurity=true;encrypt=true;trustServerCertificate=true;";

    private static final String GREEN  = "\u001B[32m";
    private static final String CYAN   = "\u001B[36m";
    private static final String BOLD   = "\u001B[1m";
    private static final String RESET  = "\u001B[0m";

    public String findCustomerByIDNumber(String idNumber) {
        if (idNumber == null || idNumber.trim().isEmpty()) return null;

        String sql = "SELECT CustomerID FROM Customers WHERE TRIM(IDNumber) = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, idNumber.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("CustomerID");
                }
            }

        } catch (SQLException e) {
            System.err.println("  DB Error (findCustomer): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("  Unexpected Error (findCustomer): " + e.getMessage());
        }
        return null;
    }

    public String registerCustomer(String fullName, String idNumber) {
        if (fullName == null || fullName.trim().isEmpty()) {
            System.out.println("  Registration Error: Full name cannot be empty.");
            return null;
        }
        if (idNumber == null || idNumber.trim().isEmpty()) {
            System.out.println("  Registration Error: ID number cannot be empty.");
            return null;
        }

        String customerId = generateCustomerId(fullName.trim());
        if (customerId == null) {
            System.out.println("  Registration Error: Name must have at least a first and last name.");
            return null;
        }

        String existing = findCustomerByIDNumber(idNumber);
        if (existing != null) {
            System.out.println("  Registration Error: ID Number is already registered to " + existing + ".");
            return null;
        }

        String sql = "INSERT INTO Customers (CustomerID, FullName, IDNumber) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, customerId);
            pstmt.setString(2, fullName.trim());
            pstmt.setString(3, idNumber.trim());

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                return customerId;
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("  Registration Error: CustomerID already exists. " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("  DB Error (registerCustomer): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("  Unexpected Error (registerCustomer): " + e.getMessage());
        }
        return null;
    }

    private String generateCustomerId(String fullName) {
        try {
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length < 2) return null;
            String firstName = parts[0].toUpperCase();
            String lastName  = parts[parts.length - 1].toUpperCase();
            return lastName + "-" + firstName;
        } catch (Exception e) {
            return null;
        }
    }


    public List<SeatHierarchy> getLiveInventory() {
    List<SeatHierarchy> seats = new ArrayList<>();

    String releaseSql =
        "UPDATE Seats SET Status = 'Available', CustomerID = NULL, LockTimestamp = NULL " +
        "WHERE Status = 'Reserved' AND DATEDIFF(minute, LockTimestamp, GETDATE()) >= 15";

    try (Connection conn = DriverManager.getConnection(url)) {

        // Auto-release expired 15-min locks
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(releaseSql);
        } catch (SQLException e) {
            System.err.println("  Warning: Could not auto-release expired seats. " + e.getMessage());
        }

        // Fetch all seats
        String query = "SELECT SeatID, Price, Status, Tier, CustomerID FROM Seats ORDER BY SeatID ASC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                try {
                    String id     = rs.getString("SeatID");
                    double price  = rs.getDouble("Price");
                    String status = rs.getString("Status");
                    String tier   = rs.getString("Tier");
                    String cust   = rs.getString("CustomerID");

                    if (tier != null && tier.trim().equalsIgnoreCase("VIP")) {
                        seats.add(new VIPSeat(id, price, status, cust));
                    } else {
                        seats.add(new RegularSeat(id, price, status, cust));
                    }
                } catch (SQLException rowEx) {
                    System.err.println("  Warning: Skipped a malformed seat row. " + rowEx.getMessage());
                }
            }
        }

    } catch (SQLException e) {
        System.err.println("  Database Sync Error: " + e.getMessage());
        System.out.println("  Could not connect to database. Returning empty inventory.");
    } catch (Exception e) {
        System.err.println("  Unexpected Error in getLiveInventory: " + e.getMessage());
    }

    return seats;
}

    
    public boolean bookSeat(String seatId, String custId) {
    if (seatId == null || seatId.trim().isEmpty()) {
        System.out.println("  Booking Error: Seat ID cannot be empty.");
        return false;
    }
    if (custId == null || custId.trim().isEmpty()) {
        System.out.println("  Booking Error: Customer ID cannot be empty.");
        return false;
    }

    String sql =
        "UPDATE Seats SET Status = 'Reserved', CustomerID = ?, LockTimestamp = GETDATE() " +
        "WHERE TRIM(SeatID) = ? AND TRIM(Status) = 'Available'";

    try (Connection conn = DriverManager.getConnection(url);
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, custId.trim());
        pstmt.setString(2, seatId.trim());
        return pstmt.executeUpdate() > 0;

    } catch (SQLTimeoutException e) {
        System.err.println("  Booking Timeout: Database took too long to respond.");
        return false;
    } catch (SQLException e) {
        System.err.println("  Booking SQL Error: " + e.getMessage());
        return false;
    } catch (Exception e) {
        System.err.println("  Unexpected Booking Error: " + e.getMessage());
        return false;
    }
}
    public boolean processPayment(String seatId, String custId) {
    if (seatId == null || seatId.trim().isEmpty()) {
        System.out.println("  Payment Error: Seat ID cannot be empty.");
        return false;
    }
    if (custId == null || custId.trim().isEmpty()) {
        System.out.println("  Payment Error: Customer ID cannot be empty.");
        return false;
    }

    double price = 0;
    String tier  = "Regular";

    try {
        List<SeatHierarchy> current = getLiveInventory();
        for (SeatHierarchy s : current) {
            if (s.getId().trim().equalsIgnoreCase(seatId.trim())) {
                price = s.getPrice();
                tier  = (s instanceof VIPSeat) ? "VIP" : "Regular";
                break;
            }
        }
    } catch (Exception e) {
        System.err.println("  Warning: Could not fetch seat details before payment. " + e.getMessage());
    }

    String sql =
        "UPDATE Seats SET Status = 'Sold', LockTimestamp = NULL " +
        "WHERE TRIM(SeatID) = ? AND TRIM(CustomerID) = ? AND TRIM(Status) = 'Reserved'";

    try (Connection conn = DriverManager.getConnection(url);
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, seatId.trim());
        pstmt.setString(2, custId.trim());

        boolean success = pstmt.executeUpdate() > 0;
        if (success) {
            printReceiptToConsole(seatId, price, tier, custId);
        }
        return success;

    } catch (SQLTimeoutException e) {
        System.err.println("  Payment Timeout: Database took too long to respond.");
        return false;
    } catch (SQLException e) {
        System.err.println("  Payment SQL Error: " + e.getMessage());
        return false;
    } catch (Exception e) {
        System.err.println("  Unexpected Payment Error: " + e.getMessage());
        return false;
    }
}



    private void printReceiptToConsole(String seat, double price, String tier, String user) {
        try {
            System.out.println(GREEN + "\n  ╔══════════════════════════════════════╗");
            System.out.println(        "  ║     ESPORTS ARENA TICKET RECEIPT     ║");
            System.out.println(        "  ╠══════════════════════════════════════╣");
            System.out.println(        "  ║  Customer : " + BOLD + user + RESET + GREEN);
            System.out.println(        "  ║  Seat ID  : " + seat.trim());
            System.out.println(        "  ║  Tier     : " + tier);
            System.out.println(        "  ║  Price    : P" + price);
            System.out.println(        "  ║  Status   : " + BOLD + "PAID & CONFIRMED" + RESET + GREEN);
            System.out.println(        "  ║  Date     : " + new java.util.Date());
            System.out.println(        "  ╚══════════════════════════════════════╝" + RESET);
            System.out.println(CYAN +  "        Thank you! GG and enjoy the event!" + RESET + "\n");
        } catch (Exception e) {
            System.err.println("  Warning: Could not print receipt. " + e.getMessage());
        }
    }
}