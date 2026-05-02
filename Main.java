import java.util.*;

public class Main {
    
    private static final RealTimeRepository repo = new RealTimeRepository();
    private static final Scanner sc = new Scanner(System.in);

    private static String currentSessionUser = null;
    private static String currentSessionName = null;
    
    private static final String GREEN  = "\u001B[32m";
    private static final String ORANGE = "\u001B[33m";
    private static final String GRAY   = "\u001B[37m";
    private static final String RED    = "\u001B[31m";
    private static final String CYAN   = "\u001B[36m";
    private static final String BOLD   = "\u001B[1m";
    private static final String RESET  = "\u001B[0m";

    public static void main(String[] args) {
        printBanner();

        customerLoginFlow();

        while (true) {
            try {
                printMenu();
                String rawInput = sc.nextLine().trim();

                if (rawInput.isEmpty()) {
                    System.out.println(RED + "  Input Error: Please enter a number (1-6)." + RESET);
                    continue;
                }

                int choice;
                try {
                    choice = Integer.parseInt(rawInput);
                } catch (NumberFormatException e) {
                    System.out.println(RED + "  Input Error: \"" + rawInput + "\" is not valid. Enter a number 1-6." + RESET);
                    continue;
                }

                switch (choice) {
                    case 1: renderMap();      break;
                    case 2: handleBooking();  break;
                    case 3: handlePayment();  break;
                    case 4: showDashboard();  break;
                    case 5: filterTickets();  break;
                    case 6:
                        System.out.println(CYAN + "\n  Shutting down system. GG!" + RESET);
                        return;
                    default:
                        System.out.println(RED + "  Invalid Option: \"" + choice + "\" is out of range. Choose 1-6." + RESET);
                }

            } catch (NoSuchElementException e) {
                System.out.println(RED + "  System Error: Input stream closed unexpectedly." + RESET);
                break;
            } catch (Exception e) {
                System.out.println(RED + "  Unexpected Error: " + e.getMessage() + ". Please try again." + RESET);
            }
        }
    }

    private static void customerLoginFlow() {
        while (currentSessionUser == null) {
            try {
                System.out.println(CYAN + "\n  ╔══════════════════════════════════════╗");
                System.out.println(       "  ║         CUSTOMER CHECK-IN            ║");
                System.out.println(       "  ╚══════════════════════════════════════╝" + RESET);
                System.out.println("  [1] I already have an account (returning customer)");
                System.out.println("  [2] Register as a new customer");
                System.out.print("  Choose: ");

                String rawChoice = sc.nextLine().trim();

                if (rawChoice.isEmpty()) {
                    System.out.println(RED + "  Input Error: Please enter 1 or 2." + RESET);
                    continue;
                }

                int choice;
                try {
                    choice = Integer.parseInt(rawChoice);
                } catch (NumberFormatException e) {
                    System.out.println(RED + "  Input Error: \"" + rawChoice + "\" is not valid. Enter 1 or 2." + RESET);
                    continue;
                }

                if (choice == 1) {
                    returningCustomerLogin();
                } else if (choice == 2) {
                    newCustomerRegister();
                } else {
                    System.out.println(RED + "  Invalid Option: Enter 1 or 2." + RESET);
                }

            } catch (NoSuchElementException e) {
                System.out.println(RED + "  Input Error: No input detected." + RESET);
            } catch (Exception e) {
                System.out.println(RED + "  Unexpected Error during login: " + e.getMessage() + RESET);
            }
        }
    }

    private static void returningCustomerLogin() {
        try {
            System.out.println(CYAN + "\n  [ Returning Customer Login ]" + RESET);
            System.out.print("  Enter your ID Number: ");
            String idNumber = sc.nextLine().trim();

            if (idNumber.isEmpty()) {
                System.out.println(RED + "  Input Error: ID Number cannot be empty." + RESET);
                return;
            }

            String customerId = repo.findCustomerByIDNumber(idNumber);

            if (customerId == null) {
                System.out.println(ORANGE + "  No account found with ID Number: " + idNumber + RESET);
                System.out.println("  Please register as a new customer (option 2).");
                return;
            }

            currentSessionUser = customerId;
    
            currentSessionName = formatNameFromId(customerId);

            System.out.println(GREEN + "\n  ✔ Welcome back, " + BOLD + currentSessionName + RESET + GREEN + "!" + RESET);
            System.out.println(CYAN + "  Session ID: " + currentSessionUser + RESET);

        } catch (NoSuchElementException e) {
            System.out.println(RED + "  Input Error: No input detected." + RESET);
        } catch (Exception e) {
            System.out.println(RED + "  Error during login: " + e.getMessage() + RESET);
        }
    }

    private static void newCustomerRegister() {
        try {
            System.out.println(CYAN + "\n  [ New Customer Registration ]" + RESET);

            System.out.print("  Enter your Full Name (First Last): ");
            String fullName = sc.nextLine().trim();

            if (fullName.isEmpty()) {
                System.out.println(RED + "  Input Error: Full name cannot be empty." + RESET);
                return;
            }

            if (!fullName.matches("[a-zA-Z]+(\\s+[a-zA-Z]+)+")) {
                System.out.println(RED + "  Input Error: \"" + fullName + "\" is not valid. Enter your full name (letters only, e.g. Marco Dredd)." + RESET);
                return;
            }

            System.out.print("  Enter your ID Number (e.g. 2023-00123): ");
            String idNumber = sc.nextLine().trim();

            if (idNumber.isEmpty()) {
                System.out.println(RED + "  Input Error: ID Number cannot be empty." + RESET);
                return;
            }

            if (!idNumber.matches("[a-zA-Z0-9\\-]+")) {
                System.out.println(RED + "  Input Error: \"" + idNumber + "\" is not valid. Use letters, numbers, and dashes only." + RESET);
                return;
            }

            System.out.println(CYAN + "\n  ─── Confirm Registration ───────────────" + RESET);
            System.out.println("  Full Name  : " + fullName);
            System.out.println("  ID Number  : " + idNumber);
            System.out.print("  Save to database? [Y/N]: ");
            String confirm = sc.nextLine().trim().toUpperCase();

            if (confirm.isEmpty() || (!confirm.equals("Y") && !confirm.equals("N"))) {
                System.out.println(RED + "  Input Error: Enter Y or N." + RESET);
                return;
            }

            if (confirm.equals("N")) {
                System.out.println(ORANGE + "  Registration cancelled." + RESET);
                return;
            }

            String customerId = repo.registerCustomer(fullName, idNumber);

            if (customerId != null) {
                currentSessionUser = customerId;
                currentSessionName = fullName;
                System.out.println(GREEN + "\n  ✔ Registration successful! Welcome, " + BOLD + fullName + RESET + GREEN + "!" + RESET);
                System.out.println(CYAN + "  Your Session ID: " + BOLD + customerId + RESET);
                System.out.println(ORANGE + "  Keep your ID Number safe — you will need it to log in next time." + RESET);
            } else {
                System.out.println(RED + "  Registration failed. Please try again or use a different ID Number." + RESET);
            }

        } catch (NoSuchElementException e) {
            System.out.println(RED + "  Input Error: No input detected." + RESET);
        } catch (Exception e) {
            System.out.println(RED + "  Error during registration: " + e.getMessage() + RESET);
        }
    }

    private static String formatNameFromId(String customerId) {
        try {
            String[] parts = customerId.split("-");
            if (parts.length < 2) return customerId;
            String first = parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1).toLowerCase();
            String last  = parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1).toLowerCase();
            return first + " " + last;
        } catch (Exception e) {
            return customerId;
        }
    }

    private static void printBanner() {
        try {
            System.out.println(CYAN + BOLD);
            System.out.println("  ███████╗███████╗██████╗  ██████╗ ██████╗ ████████╗███████╗");
            System.out.println("  ██╔════╝██╔════╝██╔══██╗██╔═══██╗██╔══██╗╚══██╔══╝██╔════╝");
            System.out.println("  █████╗  ███████╗██████╔╝██║   ██║██████╔╝   ██║   ███████╗");
            System.out.println("  ██╔══╝  ╚════██║██╔═══╝ ██║   ██║██╔══██╗   ██║   ╚════██║");
            System.out.println("  ███████╗███████║██║     ╚██████╔╝██║  ██║   ██║   ███████║");
            System.out.println("  ╚══════╝╚══════╝╚═╝      ╚═════╝ ╚═╝  ╚═╝   ╚═╝   ╚══════╝");
            System.out.println("              A R E N A   T I C K E T I N G   S Y S T E M");
            System.out.println(RESET);
        } catch (Exception e) {
            System.out.println("ESPORTS ARENA TICKETING SYSTEM");
        }
    }

    private static void printMenu() {
        try {
            System.out.println(CYAN + "\n  ╔══════════════════════════════════════╗");
            System.out.println(       "  ║       ESPORTS TICKETING SYSTEM       ║");
            System.out.println(       "  ╠══════════════════════════════════════╣" + RESET);
            System.out.println("  Logged in as: " + BOLD + currentSessionName + RESET + "  (" + currentSessionUser + ")");
            System.out.println(CYAN + "  ──────────────────────────────────────" + RESET);
            System.out.println("  [1] " + GREEN  + "View Arena Map"        + RESET + "  (Filter by Row)");
            System.out.println("  [2] " + ORANGE + "Reserve a Seat"        + RESET + "  (1 Seat Limit)");
            System.out.println("  [3] " + GREEN  + "Confirm Payment"       + RESET + "  & Get Receipt");
            System.out.println("  [4] " + CYAN   + "My Tickets Dashboard"  + RESET);
            System.out.println("  [5] " + CYAN   + "Filter Ticket Status"  + RESET);
            System.out.println("  [6] " + RED    + "Exit"                  + RESET);
            System.out.println(CYAN + "  ╚══════════════════════════════════════╝" + RESET);
            System.out.print("  Action: ");
        } catch (Exception e) {
            System.out.print("\nChoose [1-6]: ");
        }
    }

    //Featured 2.1 rendermap
    //Featured 1.1 HandleBooking
    //Feature 1.2 ->
    //Featured 1.3 DB

    private static void filterTickets() {
        try {
            System.out.println(CYAN + "\n  ╔══════════════════════════════════════╗");
            System.out.println(       "  ║         FILTER TICKET STATUS         ║");
            System.out.println(       "  ╚══════════════════════════════════════╝" + RESET);
            System.out.println("  [1] Show Available Seats");
            System.out.println("  [2] Show Reserved Seats");
            System.out.println("  [3] Show Sold Seats");
            System.out.println("  [4] Show All Seats");
            System.out.print("  Filter: ");

            String rawFilter = sc.nextLine().trim();

            if (rawFilter.isEmpty()) {
                System.out.println(RED + "  Input Error: Filter cannot be empty. Enter 1, 2, 3, or 4." + RESET);
                return;
            }

            int f;
            try {
                f = Integer.parseInt(rawFilter);
            } catch (NumberFormatException e) {
                System.out.println(RED + "  Input Error: \"" + rawFilter + "\" is not valid. Enter 1, 2, 3, or 4." + RESET);
                return;
            }

            String filterChoice;
            switch (f) {
                case 1: filterChoice = "Available"; break;
                case 2: filterChoice = "Reserved";  break;
                case 3: filterChoice = "Sold";      break;
                case 4: filterChoice = "ALL";       break;
                default:
                    System.out.println(RED + "  Invalid Option: \"" + f + "\" is out of range. Enter 1-4." + RESET);
                    return;
            }

            List<SeatHierarchy> allSeats = repo.getLiveInventory();

            if (allSeats == null || allSeats.isEmpty()) {
                System.out.println(ORANGE + "  No seat data available." + RESET);
                return;
            }

            System.out.println(CYAN + "\n  ──────────────────────────────────────" + RESET);
            System.out.println("  Filter: " + BOLD + filterChoice.toUpperCase() + RESET);
            System.out.println(CYAN + "  ──────────────────────────────────────" + RESET);

            boolean found = false;
            for (SeatHierarchy s : allSeats) {
                try {
                    String status = s.getStatus().trim();
                    if (filterChoice.equals("ALL") || status.equalsIgnoreCase(filterChoice)) {
                        String type  = (s instanceof VIPSeat) ? "[VIP]" : "[Regular]";
                        String color = status.equalsIgnoreCase("Available") ? GREEN
                                     : status.equalsIgnoreCase("Reserved")  ? ORANGE
                                     : GRAY;
                        String owner = (s.getCustomerId() != null && !s.getCustomerId().trim().isEmpty())
                                     ? "  Owner: " + s.getCustomerId().trim() : "";

                        System.out.println("  " + color + s.getId() + RESET
                                + "  " + CYAN + type + RESET
                                + "  " + color + status.toUpperCase() + RESET
                                + "  P" + s.getPrice() + owner);
                        found = true;
                    }
                } catch (Exception rowEx) {
                    System.err.println("  Warning: Error displaying a seat row. " + rowEx.getMessage());
                }
            }

            if (!found) System.out.println("  No seats found with status: " + filterChoice);

            System.out.println(CYAN + "  ──────────────────────────────────────" + RESET);

        } catch (NoSuchElementException e) {
            System.out.println(RED + "  Input Error: No input detected." + RESET);
        } catch (Exception e) {
            System.out.println(RED + "  Error during filter: " + e.getMessage() + RESET);
        }
    }
}