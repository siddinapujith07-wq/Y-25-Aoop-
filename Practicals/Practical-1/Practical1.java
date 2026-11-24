import java.time.LocalDateTime;
import java.util.*;

class Money {
    long paise;

    Money(double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount");

        paise = Math.round(amount * 100);
    }

    Money(long paise) {
        this.paise = paise;
    }

    public String toString() {
        return "Rs. " + String.format("%.2f", paise / 100.0);
    }
}

enum AccountStatus {
    ACTIVE, FROZEN, CLOSED
}

class LedgerEntry {
    private final String type;
    private final Money amount;
    private final String time;

    LedgerEntry(String type, Money amount) {
        this.type = type;
        this.amount = amount;
        this.time = LocalDateTime.now().toString();
    }

    public String toString() {
        return type + " | " + amount + " | " + time;
    }
}

class BankAccount {
    private final String number;
    private Money balance;
    private final Money overdraft;
    private AccountStatus status;

    private final List<LedgerEntry> ledger = new ArrayList<>();

    BankAccount(String number, double balance, double overdraft) {
        if (number == null || number.isEmpty())
            throw new IllegalArgumentException("Invalid account number");

        if (balance < 0 || overdraft < 0)
            throw new IllegalArgumentException("Invalid balance or overdraft");

        this.number = number;
        this.balance = new Money(Math.round(balance * 100));
        this.overdraft = new Money(Math.round(overdraft * 100));
        this.status = AccountStatus.ACTIVE;
    }

    private void check() {
        if (status != AccountStatus.ACTIVE)
            throw new RuntimeException("Account is not active");
    }

    void deposit(double amount) {
        check();

        Money m = new Money(amount);
        balance = new Money(balance.paise + m.paise);

        ledger.add(new LedgerEntry("Deposit", m));

        System.out.println("Deposit successful");
    }

    void withdraw(double amount) {
        check();

        Money m = new Money(amount);

        if (balance.paise - m.paise < -overdraft.paise)
            throw new RuntimeException("Overdraft limit exceeded");

        balance = new Money(balance.paise - m.paise);

        ledger.add(new LedgerEntry("Withdrawal", m));

        System.out.println("Withdrawal successful");
    }

    void freeze() {
        check();
        status = AccountStatus.FROZEN;
        System.out.println("Account frozen");
    }

    void unfreeze() {
        if (status != AccountStatus.FROZEN)
            throw new RuntimeException("Account is not frozen");

        status = AccountStatus.ACTIVE;
        System.out.println("Account unfrozen");
    }

    void close() {
        check();

        if (balance.paise != 0)
            throw new RuntimeException("Balance must be zero");

        status = AccountStatus.CLOSED;
        System.out.println("Account closed");
    }

    void show() {
        System.out.println("\nAccount Number: " + number);
        System.out.println("Balance: " + balance);
        System.out.println("Overdraft Limit: " + overdraft);
        System.out.println("Status: " + status);
    }

    void showLedger() {
        System.out.println("\nTransaction History");

        if (ledger.isEmpty()) {
            System.out.println("No transactions available");
            return;
        }

        for (LedgerEntry entry : ledger)
            System.out.println(entry);
    }

    void validateTransfer(double amount) {
        check();

        Money m = new Money(amount);

        if (balance.paise - m.paise < -overdraft.paise)
            throw new RuntimeException("Insufficient balance");
    }

    void receiveTransfer(double amount) {
        check();
        Money m = new Money(amount);

        balance = new Money(balance.paise + m.paise);
        ledger.add(new LedgerEntry("Transfer Received", m));
    }

    void sendTransfer(double amount) {
        Money m = new Money(amount);

        balance = new Money(balance.paise - m.paise);
        ledger.add(new LedgerEntry("Transfer Sent", m));
    }

    long getBalance() {
        return balance.paise;
    }

    int getLedgerSize() {
        return ledger.size();
    }

    void restore(long oldBalance, int oldSize) {
        balance = new Money(oldBalance);
        ledger.subList(oldSize, ledger.size()).clear();
    }
}

class FundTransfer {

    static synchronized void transfer(
            BankAccount from, BankAccount to, double amount) {

        if (from == to)
            throw new RuntimeException("Same account transfer not allowed");

        // Validate both accounts before changing balances
        from.validateTransfer(amount);
        to.validateTransfer(amount);

        long oldFrom = from.getBalance();
        long oldTo = to.getBalance();

        int oldFromSize = from.getLedgerSize();
        int oldToSize = to.getLedgerSize();

        try {
            from.sendTransfer(amount);
            to.receiveTransfer(amount);

            System.out.println("Transfer successful");

        } catch (RuntimeException e) {
            from.restore(oldFrom, oldFromSize);
            to.restore(oldTo, oldToSize);

            throw new RuntimeException("Transfer failed");
        }
    }
}

public class Practical1 {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        Map<String, BankAccount> accounts = new HashMap<>();

        while (true) {

            System.out.println("\n===== BANKING APPLICATION =====");
            System.out.println("1. Create Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Freeze Account");
            System.out.println("6. Unfreeze Account");
            System.out.println("7. Close Account");
            System.out.println("8. Account Details");
            System.out.println("9. Transaction History");
            System.out.println("10. Exit");

            System.out.print("Enter choice: ");

            try {
                int ch = sc.nextInt();

                if (ch == 10)
                    break;

                if (ch == 1) {

                    System.out.print("Account number: ");
                    String n = sc.next();

                    if (accounts.containsKey(n))
                        throw new RuntimeException("Account already exists");

                    System.out.print("Initial balance: ");
                    double b = sc.nextDouble();

                    System.out.print("Overdraft limit: ");
                    double o = sc.nextDouble();

                    accounts.put(n, new BankAccount(n, b, o));

                    System.out.println("Account created successfully");
                }

                else {

                    System.out.print("Account number: ");
                    String n = sc.next();

                    BankAccount a = accounts.get(n);

                    if (a == null)
                        throw new RuntimeException("Account not found");

                    switch (ch) {

                        case 2:
                            System.out.print("Amount: ");
                            a.deposit(sc.nextDouble());
                            break;

                        case 3:
                            System.out.print("Amount: ");
                            a.withdraw(sc.nextDouble());
                            break;

                        case 4:
                            System.out.print("Destination account: ");
                            String d = sc.next();

                            BankAccount b = accounts.get(d);

                            if (b == null)
                                throw new RuntimeException(
                                    "Destination account not found"
                                );

                            System.out.print("Amount: ");
                            FundTransfer.transfer(a, b, sc.nextDouble());
                            break;

                        case 5:
                            a.freeze();
                            break;

                        case 6:
                            a.unfreeze();
                            break;

                        case 7:
                            a.close();
                            break;

                        case 8:
                            a.show();
                            break;

                        case 9:
                            a.showLedger();
                            break;

                        default:
                            System.out.println("Invalid choice");
                    }
                }

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                sc.nextLine();
            }
        }

        sc.close();
        System.out.println("Banking application closed");
    }
}