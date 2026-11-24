import java.util.*;
// ---------- IDENTIFIERS ----------
record CustomerId(int value) {
    public CustomerId {
        if (value <= 0)
            throw new IllegalArgumentException("Invalid customer ID");
    }
}
record BookingId(int value) {
    public BookingId {
        if (value <= 0)
            throw new IllegalArgumentException("Invalid booking ID");
    }
}

record ParkingSpotId(int value) {
    public ParkingSpotId {
        if (value <= 0)
            throw new IllegalArgumentException("Invalid spot ID");
    }
}

record PaymentId(int value) {
    public PaymentId {
        if (value <= 0)
            throw new IllegalArgumentException("Invalid payment ID");
    }
}

// ---------- MONEY ----------

record Money(long paise, String currency) {
    public Money {
        if (paise < 0)
            throw new IllegalArgumentException("Invalid amount");

        if (currency == null || currency.isBlank())
            throw new IllegalArgumentException("Currency required");

        currency = currency.toUpperCase(Locale.ROOT);
    }

    public Money multiply(int hours) {
        if (hours <= 0)
            throw new IllegalArgumentException("Hours must be positive");

        return new Money(Math.multiplyExact(paise, hours), currency);
    }

    @Override
    public String toString() {
        return String.format("%s %d.%02d",
                currency, paise / 100, paise % 100);
    }
}

// ---------- CUSTOMER ----------

class Customer {
    private final CustomerId id;
    private final String name;

    public Customer(CustomerId id, String name) {
        if (id == null || name == null || name.isBlank())
            throw new IllegalArgumentException("Invalid customer");

        this.id = id;
        this.name = name;
    }

    public CustomerId getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

// ---------- PARKING SPOTS ----------

enum SpotStatus {
    AVAILABLE, OCCUPIED
}

record ParkingSpotView(
        ParkingSpotId id,
        String location,
        String type,
        SpotStatus status,
        String vehicleNumber
) {}

sealed abstract class ParkingSpot
        permits CarSpot, BikeSpot, EVSpot {

    private final ParkingSpotId id;
    private final String location;
    private SpotStatus status;
    private String vehicleNumber;

    public ParkingSpot(ParkingSpotId id, String location) {
        if (id == null || location == null || location.isBlank())
            throw new IllegalArgumentException("Invalid parking spot");

        this.id = id;
        this.location = location;
        this.status = SpotStatus.AVAILABLE;
    }

    protected abstract Money getRate();

    public abstract String getType();

    public Money calculateFare(int hours) {
        return getRate().multiply(hours);
    }

    public boolean isAvailable() {
        return status == SpotStatus.AVAILABLE;
    }

    public void park(String vehicle) {
        if (!isAvailable())
            throw new IllegalStateException("Spot already occupied");

        if (vehicle == null || vehicle.isBlank())
            throw new IllegalArgumentException("Vehicle required");

        vehicleNumber = vehicle;
        status = SpotStatus.OCCUPIED;
    }

    public void release() {
        if (isAvailable())
            throw new IllegalStateException("Spot is already free");

        vehicleNumber = null;
        status = SpotStatus.AVAILABLE;
    }

    public ParkingSpotId getId() {
        return id;
    }

    public ParkingSpotView getDetails() {
        return new ParkingSpotView(
                id, location, getType(), status, vehicleNumber);
    }
}

// ---------- SPOT TYPES ----------

final class CarSpot extends ParkingSpot {
    public CarSpot(ParkingSpotId id, String location) {
        super(id, location);
    }

    protected Money getRate() {
        return new Money(6000, "INR");
    }

    public String getType() {
        return "CAR";
    }
}

final class BikeSpot extends ParkingSpot {
    public BikeSpot(ParkingSpotId id, String location) {
        super(id, location);
    }

    protected Money getRate() {
        return new Money(2500, "INR");
    }

    public String getType() {
        return "BIKE";
    }
}

final class EVSpot extends ParkingSpot {
    public EVSpot(ParkingSpotId id, String location) {
        super(id, location);
    }

    protected Money getRate() {
        return new Money(8000, "INR");
    }

    public String getType() {
        return "ELECTRIC VEHICLE";
    }
}

// ---------- PAYMENT ----------

record PaymentReceipt(
        PaymentId paymentId,
        BookingId bookingId,
        Money amount,
        boolean successful
) {}

interface PaymentMethod {
    PaymentReceipt pay(
            PaymentId paymentId,
            BookingId bookingId,
            Money amount);
}

class UpiPayment implements PaymentMethod {
    public PaymentReceipt pay(
            PaymentId paymentId,
            BookingId bookingId,
            Money amount) {

        System.out.println("UPI payment completed: " + amount);

        return new PaymentReceipt(
                paymentId, bookingId, amount, true);
    }
}

class CardPayment implements PaymentMethod {
    public PaymentReceipt pay(
            PaymentId paymentId,
            BookingId bookingId,
            Money amount) {

        System.out.println("Card payment completed: " + amount);

        return new PaymentReceipt(
                paymentId, bookingId, amount, true);
    }
}

// ---------- BOOKING ----------

enum BookingStatus {
    PENDING, CONFIRMED, CANCELLED
}

record BookingView(
        BookingId bookingId,
        CustomerId customerId,
        ParkingSpotId spotId,
        String vehicle,
        Money fare,
        BookingStatus status
) {}

class Booking {
    private final BookingId id;
    private final CustomerId customerId;
    private final ParkingSpotId spotId;
    private final String vehicle;
    private final Money fare;

    private BookingStatus status;
    private PaymentReceipt receipt;

    public Booking(BookingId id, CustomerId customerId,
                   ParkingSpotId spotId, String vehicle, Money fare) {

        if (id == null || customerId == null || spotId == null
                || vehicle == null || vehicle.isBlank() || fare == null)
            throw new IllegalArgumentException("Invalid booking");

        this.id = id;
        this.customerId = customerId;
        this.spotId = spotId;
        this.vehicle = vehicle;
        this.fare = fare;
        this.status = BookingStatus.PENDING;
    }

    public void confirm(PaymentReceipt receipt) {
        if (status != BookingStatus.PENDING)
            throw new IllegalStateException("Booking not pending");

        if (receipt == null || !receipt.successful()
                || !receipt.bookingId().equals(id)
                || !receipt.amount().equals(fare))
            throw new IllegalArgumentException("Invalid payment");

        this.receipt = receipt;
        status = BookingStatus.CONFIRMED;
    }

    public void cancel() {
        if (status != BookingStatus.CONFIRMED)
            throw new IllegalStateException("Cannot cancel booking");

        status = BookingStatus.CANCELLED;
    }

    public BookingId getId() {
        return id;
    }

    public ParkingSpotId getSpotId() {
        return spotId;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public PaymentReceipt getReceipt() {
        return receipt;
    }

    public BookingView getDetails() {
        return new BookingView(
                id, customerId, spotId, vehicle, fare, status);
    }
}

// ---------- PARKING SERVICE ----------

class WaypointService {

    private final Map<CustomerId, Customer> customers = new HashMap<>();
    private final Map<ParkingSpotId, ParkingSpot> spots = new HashMap<>();
    private final Map<BookingId, Booking> bookings = new HashMap<>();
    private final Set<String> vehicles = new HashSet<>();

    private final PaymentMethod paymentMethod;

    private int bookingNumber = 1;
    private int paymentNumber = 1;

    public WaypointService(PaymentMethod paymentMethod) {
        if (paymentMethod == null)
            throw new IllegalArgumentException("Payment method required");

        this.paymentMethod = paymentMethod;
    }

    public void addCustomer(Customer customer) {
        if (customer == null ||
                customers.putIfAbsent(customer.getId(), customer) != null)
            throw new IllegalArgumentException("Customer already exists");
    }

    public void addSpot(ParkingSpot spot) {
        if (spot == null ||
                spots.putIfAbsent(spot.getId(), spot) != null)
            throw new IllegalArgumentException("Spot already exists");
    }

    public void addVehicle(String vehicle) {
        if (vehicle == null || vehicle.isBlank())
            throw new IllegalArgumentException("Invalid vehicle");

        String number = vehicle.trim().toUpperCase(Locale.ROOT);

        if (!vehicles.add(number))
            throw new IllegalArgumentException("Vehicle already registered");
    }

    public List<ParkingSpotView> getSpots() {
        List<ParkingSpotView> result = new ArrayList<>();

        for (ParkingSpot spot : spots.values())
            result.add(spot.getDetails());

        return List.copyOf(result);
    }

    public Booking reserve(CustomerId customerId,
                           ParkingSpotId spotId,
                           String vehicle,
                           int hours) {

        if (!customers.containsKey(customerId))
            throw new IllegalArgumentException("Customer not found");

        if (hours <= 0)
            throw new IllegalArgumentException("Invalid hours");

        if (vehicle == null || vehicle.isBlank())
            throw new IllegalArgumentException("Vehicle required");

        String number = vehicle.trim().toUpperCase(Locale.ROOT);

        if (!vehicles.contains(number))
            throw new IllegalArgumentException("Vehicle not registered");

        ParkingSpot spot = spots.get(spotId);

        if (spot == null)
            throw new IllegalArgumentException("Spot not found");

        if (!spot.isAvailable())
            throw new IllegalStateException("Spot is occupied");

        Money fare = spot.calculateFare(hours);

        BookingId bookingId = new BookingId(bookingNumber++);
        PaymentId paymentId = new PaymentId(paymentNumber++);

        boolean parked = false;

        try {
            spot.park(number);
            parked = true;

            Booking booking = new Booking(
                    bookingId, customerId, spotId, number, fare);

            PaymentReceipt receipt =
                    paymentMethod.pay(paymentId, bookingId, fare);

            booking.confirm(receipt);
            bookings.put(bookingId, booking);

            return booking;

        } catch (RuntimeException e) {
            if (parked)
                spot.release();

            throw e;
        }
    }

    public void cancelBooking(BookingId id) {
        Booking booking = bookings.get(id);

        if (booking == null)
            throw new IllegalArgumentException("Booking not found");

        booking.cancel();

        spots.get(booking.getSpotId()).release();
    }

    public void displayBookings() {
        System.out.println("\n----- ALL BOOKINGS -----");

        for (Booking booking : bookings.values()) {
            BookingView b = booking.getDetails();

            System.out.println(
                    "Booking: " + b.bookingId()
                    + " | Customer: " + b.customerId()
                    + " | Spot: " + b.spotId()
                    + " | Vehicle: " + b.vehicle()
                    + " | Fare: " + b.fare()
                    + " | Status: " + b.status()
            );
        }
    }
}

// ---------- MAIN PROGRAM ----------

public class WaypointDemo {

    public static void main(String[] args) {

        PaymentMethod payment = new UpiPayment();

        WaypointService service = new WaypointService(payment);

        // Register customers
        service.addCustomer(
                new Customer(new CustomerId(101), "Arjun"));

        service.addCustomer(
                new Customer(new CustomerId(102), "Kiran"));

        // Add parking spots
        service.addSpot(
                new CarSpot(new ParkingSpotId(201), "Hyderabad"));

        service.addSpot(
                new BikeSpot(new ParkingSpotId(202), "Miyapur"));

        service.addSpot(
                new EVSpot(new ParkingSpotId(203), "Kukatpally"));

        // Register vehicles
        service.addVehicle("TS08XY1234");
        service.addVehicle("TS09AB5678");
        service.addVehicle("TS10CD9012");

        // Initial availability
        System.out.println("===== PARKING AVAILABILITY =====");

        for (ParkingSpotView spot : service.getSpots()) {
            System.out.println(
                    spot.spotId() + " | "
                    + spot.location() + " | "
                    + spot.type() + " | "
                    + spot.status());
        }

        // Car booking
        System.out.println("\n===== CAR BOOKING =====");

        Booking first = service.reserve(
                new CustomerId(101),
                new ParkingSpotId(201),
                "TS08XY1234",
                2);

        System.out.println("Booking ID: " + first.getId());
        System.out.println("Amount: " + first.getReceipt().amount());
        System.out.println("Status: " + first.getStatus());

        // Bike booking
        System.out.println("\n===== BIKE BOOKING =====");

        Booking second = service.reserve(
                new CustomerId(102),
                new ParkingSpotId(202),
                "TS09AB5678",
                3);

        System.out.println("Booking ID: " + second.getId());
        System.out.println("Amount: " + second.getReceipt().amount());
        System.out.println("Status: " + second.getStatus());

        // Invalid booking test
        System.out.println("\n===== BOOKING VALIDATION =====");

        try {
            service.reserve(
                    new CustomerId(102),
                    new ParkingSpotId(201),
                    "TS09AB5678",
                    1);

        } catch (IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Cancel booking
        System.out.println("\n===== CANCEL BOOKING =====");

        service.cancelBooking(first.getId());

        System.out.println("Booking status: " + first.getStatus());

        // Final availability
        System.out.println("\n===== FINAL PARKING STATUS =====");

        for (ParkingSpotView spot : service.getSpots()) {
            System.out.println(
                    spot.spotId() + " | "
                    + spot.location() + " | "
                    + spot.status() + " | "
                    + spot.vehicleNumber());
        }

        // Display bookings
        service.displayBookings();
    }
}