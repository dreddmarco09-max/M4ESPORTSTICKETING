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


    //Featured 2.2

    //Featured 1.1 Bookeat
    //Featured 1.2 Proc payment kurt

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