// be.ouagueni.model.Member.java
package be.ouagueni.model;

import be.ouagueni.dao.MemberDAO;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Comparator;

public class Member extends Person implements Serializable {

    private static final long serialVersionUID = -25458080844517046L;
    private double balance;
    private int idMember;
    private Set<Inscription> inscriptions = new HashSet<>();
    private Set<Category> categories = new HashSet<>();
    private Set<Bike> bikes = new HashSet<>();
    private Vehicle driver;
    private Set<Vehicle> passengers = new HashSet<>();

    // ====================== CONSTRUCTEURS ======================
    public Member() { super(); }
    public Member(String name, String firstname) {
        super(0, name, null, null, firstname);
    }
    public Member(int id, String name, String firstname, String tel, String password,
                  Category category, Bike bike) {
        super(id, name, firstname, tel, password);
        if (category == null || bike == null) {
            throw new IllegalArgumentException("Un membre doit avoir au moins 1 catégorie et 1 vélo");
        }
        this.categories.add(category);
        this.bikes.add(bike);
    }

    // ====================== GETTERS / SETTERS ======================
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public int getIdMember() { return idMember; }
    public void setIdMember(int idMember) { this.idMember = idMember; }
    public Set<Inscription> getInscriptions() { return inscriptions; }
    public void setInscriptions(Set<Inscription> inscriptions) { this.inscriptions = inscriptions; }
    public void addInscription(Inscription ins) { if (ins != null) this.inscriptions.add(ins); }
    public void removeInscription(Inscription ins) { this.inscriptions.remove(ins); }

    public Set<Category> getCategories() { return categories; }
    public void setCategories(Set<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("Un membre doit avoir au moins 1 catégorie");
        }
        this.categories = categories;
    }
    public void addCategory(Category category) { if (category != null) this.categories.add(category); }
    public void removeCategory(Category category) {
        if (this.categories.size() <= 1) {
            throw new IllegalStateException("Un membre doit avoir au moins 1 catégorie");
        }
        this.categories.remove(category);
    }

    public Set<Bike> getBikes() { return bikes; }
    public void setBikes(Set<Bike> bikes) {
        if (bikes == null || bikes.isEmpty()) {
            throw new IllegalArgumentException("Un membre doit avoir au moins 1 vélo");
        }
        this.bikes = bikes;
    }
    public void addBike(Bike bike) { if (bike != null) this.bikes.add(bike); }
    public void removeBike(Bike bike) {
        if (this.bikes.size() <= 1) {
            throw new IllegalStateException("Un membre doit avoir au moins 1 vélo");
        }
        this.bikes.remove(bike);
    }

    public Vehicle getDriver() { return driver; }
    public void setDriver(Vehicle driver) { this.driver = driver; }
    public Set<Vehicle> getPassengers() { return passengers; }
    public void setPassengers(Set<Vehicle> passengers) {
        this.passengers = (passengers == null) ? new HashSet<>() : passengers;
    }
    public void addPassenger(Vehicle vehicle) { if (vehicle != null) this.passengers.add(vehicle); }
    public void removePassenger(Vehicle vehicle) { this.passengers.remove(vehicle); }
    public boolean isPassengerIn(Vehicle vehicle) { return this.passengers.contains(vehicle); }

    // ====================== LOGIQUE MÉTIER ======================

    // 1. Cotisation
    public record CotisationInfo(double amountDue, String formattedDetails, Set<TypeCat> detectedCategories) {}

    public CotisationInfo getCotisationInfo() {
        Set<TypeCat> types = getBikes().stream().map(Bike::getType).collect(Collectors.toSet());
        int count = types.size();
        double supplement = 5.0 * Math.max(0, count);
        double total = 20.0 + supplement;

        String details = String.format(
            "<html><h3>Cotisation annuelle</h3>" +
            "Base : <b>20,00 €</b><br>" +
            "Catégories détectées : <b>%d</b> (%s)<br>" +
            "Supplément : <b>%.2f €</b><br><hr>" +
            "<b>TOTAL À PAYER : %.2f €</b><br><br>" +
            "Solde actuel : <b>%.2f €</b></html>",
            count,
            types.isEmpty() ? "aucune" : types.stream().map(Enum::name).collect(Collectors.joining(", ")),
            supplement, total, balance
        );

        return new CotisationInfo(total, details, types);
    }

    public record CategorySelectionResult(List<Category> availableCategories, String message, boolean hasAvailable) {}

    public CategorySelectionResult getAvailableCategories(Connection conn) throws SQLException {
        Set<Category> all = Category.GetAll(conn);
        Set<Integer> owned = this.categories.stream().map(Category::getid).collect(Collectors.toSet());

        List<Category> available = all.stream()
                .filter(c -> !owned.contains(c.getid()))
                .sorted(Comparator.comparing(c -> c.getNomCategorie().name()))
                .toList();

        if (available.isEmpty()) {
            return new CategorySelectionResult(List.of(), "Vous possédez déjà toutes les catégories disponibles !", false);
        }
        return new CategorySelectionResult(available, null, true);
    }

    public boolean addCategoryAndPersist(Category category, Connection conn) {
        if (category == null) return false;
        this.addCategory(category);
        return this.update(conn);
    }

    public record AvailabilityResult(boolean success, String message) {}

    public AvailabilityResult postDriverAvailability(Ride ride, int passengerSeats, int bikeSpots, Connection conn) {
        try {
            if (passengerSeats <= 0 && bikeSpots <= 0) {
                return new AvailabilityResult(false, "Proposez au moins une place.");
            }

            Vehicle vehicle = Vehicle.getOrCreateForDriver(this, conn);
            vehicle.setSeatNumber(passengerSeats);
            vehicle.setBikeSpotNumber(bikeSpots);
            vehicle.addRide(ride);

            boolean saved = vehicle.update(conn);
            return new AvailabilityResult(saved,
                saved ? "Disponibilités enregistrées avec succès !" : "Échec de la sauvegarde.");
        } catch (Exception e) {
            return new AvailabilityResult(false, "Erreur : " + e.getMessage());
        }
    }

    public record ReservationResult(boolean success, String message, double newBalance, Vehicle assignedVehicle) {}

    public ReservationResult bookRide(Ride ride, boolean asPassenger, boolean withBike, Bike selectedBike, Connection conn) {
        try {
            if (ride.getInscriptions().stream()
                    .anyMatch(i -> i.getMember() != null && i.getMember().equals(this))) {
                return new ReservationResult(false, "Vous êtes déjà inscrit à cette sortie.", balance, null);
            }

            // Signature exacte de ta méthode existante
            Vehicle vehicle = ride.findAvailableVehicle(asPassenger, withBike ? 1 : 0, conn);
            if (vehicle == null) {
                String besoin = asPassenger && withBike ? "passager + vélo" :
                               asPassenger ? "passager" : "vélo";
                return new ReservationResult(false, "Aucune place disponible pour " + besoin + ".", balance, null);
            }

            Inscription inscription = new Inscription();
            inscription.setMember(this);
            inscription.setRide(ride);
            inscription.setPassenger(asPassenger);
            inscription.setBike(withBike);
            inscription.setBikeObj(withBike ? selectedBike : null);

            ride.addInscription(inscription);
            this.addInscription(inscription);
            if (asPassenger) vehicle.addPassenger(this);
            if (withBike && selectedBike != null) vehicle.addBike(selectedBike);

            double newBalance = Math.round((balance - ride.getFee()) * 100.0) / 100.0;
            this.setBalance(newBalance);

            boolean success = inscription.create(conn) && this.update(conn) && vehicle.update(conn);

            String msg = success
                ? "Réservation confirmée ! Débit : " + String.format("%.2f €", ride.getFee())
                : "Échec de l'enregistrement.";

            return new ReservationResult(success, msg, newBalance, vehicle);

        } catch (Exception e) {
            return new ReservationResult(false, "Erreur : " + e.getMessage(), balance, null);
        }
    }

    // 5. Paiements
    public boolean payAmount(double amount, Connection conn) {
        if (amount <= 0) return false;
        this.setBalance(Math.round((balance - amount) * 100.0) / 100.0);
        return this.update(conn);
    }

    public boolean depositFunds(double amount, Connection conn) {
        if (amount <= 0) return false;
        this.setBalance(Math.round((balance + amount) * 100.0) / 100.0);
        return this.update(conn);
    }

    // 6. Statut
    public boolean canParticipate() {
        return balance >= 0;
    }

    public String getBalanceStatus() {
        if (balance > 0) return "Crédit : " + String.format("%.2f €", balance);
        if (balance == 0) return "À jour";
        return "Dette : " + String.format("%.2f €", -balance);
    }

    // ====================== DAO ======================
    public boolean create(Connection conn) {
        return new MemberDAO(conn).create(this);
    }

    public boolean update(Connection conn) {
        return new MemberDAO(conn).update(this);
    }
}