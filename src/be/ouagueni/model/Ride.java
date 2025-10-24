package be.ouagueni.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class Ride implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private LocalDate startPlace;
    private LocalDate startDate; // adapte au type Date si tu veux
    private double fee;
    private Calendar calendar; // ajoute cette ligne pour l'association avec Calendar
    private Set<Inscription> inscriptions = new HashSet<>();
    private Set<Vehicle> vehicles = new HashSet<>();

    public Ride() {}
    public Ride(int id, LocalDate startPlace, LocalDate startDate, double fee) {
        this.id = id; this.startPlace = startPlace; this.startDate = startDate; this.fee = fee;
    }

    // getters / setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public LocalDate getStartPlace() { return startPlace; }
    public void setStartPlace(LocalDate startPlace) { this.startPlace = startPlace; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public double getFee() { return fee; }
    public void setFee(double fee) { this.fee = fee; }

    public Set<Inscription> getInscriptions() { return inscriptions; }
    public void setInscriptions(Set<Inscription> inscriptions) { this.inscriptions = inscriptions; }
    public void addInscription(Inscription ins) { if (ins != null) this.inscriptions.add(ins); }
    public void removeInscription(Inscription ins) { this.inscriptions.remove(ins); }

    public Set<Vehicle> getVehicles() { return vehicles; }
    public void setVehicles(Set<Vehicle> vehicles) { this.vehicles = vehicles; }
    public void addVehicle(Vehicle v) { if (v != null) this.vehicles.add(v); }
    public void removeVehicle(Vehicle v) { this.vehicles.remove(v); }
    
    public Calendar getCalendar() { return calendar; }
    public void setCalendar(Calendar calendar) { this.calendar = calendar; }

    // méthodes utilitaire
    public int getTotalInscriptionNumber() { return inscriptions.size(); }
    public int getAvailableSeatNumber() { return 0; }
}

