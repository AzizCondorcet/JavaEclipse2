package be.ouagueni.model;

import java.io.Serializable;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import be.ouagueni.dao.RideDAO;

public class Ride implements Serializable 
{
    private static final long serialVersionUID = 1L;
    private int id;
    private int num;
    private String startPlace;
    private LocalDateTime startDate; // adapte au type Date si tu veux
    private double fee;
    private Calendar calendar; // ajoute cette ligne pour l'association avec Calendar
    private Set<Inscription> inscriptions = new HashSet<>();
    private Set<Vehicule> vehicles = new HashSet<>();

    public Ride() {}
    public Ride(int num,String startPlace, LocalDateTime startDate, double fee,Calendar calendar) {
        this.num = num; this.startPlace = startPlace; this.startDate = startDate; this.fee = fee;
        this.calendar = calendar;
    }

    // getters / setters
    public int getnum() { return num; }
    public void setnum(int num) { this.num = num; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getStartPlace() { return startPlace; }
    public void setStartPlace(String startPlace) { this.startPlace = startPlace; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public double getFee() { return fee; }
    public void setFee(double fee) { this.fee = fee; }

    public Set<Inscription> getInscriptions() { return inscriptions; }
    public void setInscriptions(Set<Inscription> inscriptions) { this.inscriptions = inscriptions; }
    public void addInscription(Inscription ins) { if (ins != null) this.inscriptions.add(ins); }
    public void removeInscription(Inscription ins) { this.inscriptions.remove(ins); }

    public Set<Vehicule> getVehicles() { return vehicles; }
    public void setVehicles(Set<Vehicule> vehicles) {
        if (vehicles != null) {
            this.vehicles.clear();
            this.vehicles.addAll(vehicles);
        }
    }
    public void addVehicle(Vehicule v) { if (v != null) this.vehicles.add(v); }
    public void removeVehicle(Vehicule v) { this.vehicles.remove(v); }
    
    public Calendar getCalendar() { return calendar; }
    public void setCalendar(Calendar calendar) { this.calendar = calendar; }

    // méthodes utilitaire
    public int getTotalInscriptionNumber() { return inscriptions.size(); }
    
    public int getTotalBikeSpotNumber() {
        return vehicles.stream()
                .filter(v -> v.getDriver() != null) // ne compte que les véhicules avec un conducteur
                .mapToInt(Vehicule::getBikeSpotNumber)
                .sum();
    }
    
    @Override
    public String toString() {
        // Format clair et lisible pour le trésorier
        String date = startDate != null 
            ? startDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            : "Date inconnue";
        
        return String.format("%s → %s €  (%s)",
                startPlace != null ? startPlace : "Lieu inconnu",
                fee,
                date);
    }
    
    public boolean createRide(Ride ride,Connection conn) 
    {
    		RideDAO dao = new RideDAO(conn);
    		return dao.create(ride);
    }
    public static Set<Ride> allRides(Connection conn) 
    {
    		RideDAO dao = new RideDAO(conn);
    		return dao.getAllRides();
    }
    public Vehicule findAvailableVehicle(Member membre, boolean needPassengerSeat, int needBikeSpots, Connection conn) {
        System.out.println("=== findAvailableVehicle() pour " + membre.getFirstname() + " ===");
        System.out.println("Besoin passager : " + needPassengerSeat + " | Besoin place vélo : " + needBikeSpots);

        for (Vehicule v : this.getVehicles()) {
            if (v.getDriver() == null) {
                System.out.println("  → Véhicule ignoré : pas de conducteur");
                continue;
            }

            System.out.println("  → Véhicule de " + v.getDriver().getFirstname() +
                    " | Places passagers : " + v.getSeatNumber() + " (occupées: " + v.getPassengers().size() + ")" +
                    " | Places vélo : " + v.getBikeSpotNumber() + " (occupées: " + v.getBikes().size() + ")");

            // INTERDIRE d'être passager dans SON propre véhicule
            if (v.getDriver().equals(membre)) {
                System.out.println("  → IGNORÉ : c'est SON propre véhicule !");
                continue;
            }

            int availableSeats = v.getSeatNumber() - v.getPassengers().size();
            int availableBikeSpots = v.getBikeSpotNumber() - v.getBikes().size();

            boolean hasSeat = !needPassengerSeat || availableSeats > 0;
            boolean hasBikeSpot = needBikeSpots == 0 || availableBikeSpots >= needBikeSpots;

            System.out.println("     → Places dispo : " + availableSeats + " | Vélo dispo : " + availableBikeSpots);
            System.out.println("     → hasSeat = " + hasSeat + " | hasBikeSpot = " + hasBikeSpot);

            if (hasSeat && hasBikeSpot) {
                System.out.println("  → VÉHICULE CHOISI : " + v.getDriver().getFirstname() + " !");
                return v;
            }
        }

        System.out.println("  → AUCUN véhicule disponible chez les autres conducteurs");
        return null;
    }
    public void loadVehicles(Connection conn)
    {
        if (conn == null) return;
        RideDAO rideDAO = new RideDAO(conn);
        Set<Vehicule> loadedVehicles = rideDAO.getVehiclesForRide(this.getId());
        this.vehicles.clear();
        this.vehicles.addAll(loadedVehicles);
    } 
    
    // === AJOUTE ÇA DANS TA CLASSE Ride ===

    public int getNeededSeatNumber() {
        return (int) inscriptions.stream()
                .filter(Inscription::isPassenger)
                .count();
    }

    public int getNeededBikeSpotNumber() {
        return (int) inscriptions.stream()
                .filter(Inscription::isBike)
                .filter(ins -> ins.getBikeObj() != null)
                .count();
    }

    public int getAvailableSeatNumber() {
        return vehicles.stream()
                .filter(v -> v.getDriver() != null)
                .mapToInt(v -> v.getSeatNumber() - v.getPassengers().size())
                .sum();
    }

    public int getAvailableBikeSpotNumber() {
        return vehicles.stream()
                .filter(v -> v.getDriver() != null)
                .mapToInt(v -> v.getBikeSpotNumber() - v.getBikes().size())
                .sum();
    }
    public boolean estConducteur(Member membre) {
        if (membre == null) return false;
        return vehicles.stream()
                .map(Vehicule::getDriver)
                .filter(d -> d != null && d.getIdMember() != 0)    // protection
                .anyMatch(d -> d.getIdMember() == membre.getIdMember());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ride ride = (Ride) o;
        return id == ride.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
    
}

