package be.ouagueni.model;

import java.io.Serializable;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import be.ouagueni.dao.RideDAO;

public class Ride implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private int num;
    private String startPlace;
    private LocalDateTime startDate; // adapte au type Date si tu veux
    private double fee;
    private Calendar calendar; // ajoute cette ligne pour l'association avec Calendar
    private Set<Inscription> inscriptions = new HashSet<>();
    private Set<Vehicle> vehicles = new HashSet<>();

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

    public Set<Vehicle> getVehicles() { return vehicles; }
    public void setVehicles(Set<Vehicle> vehicles) { this.vehicles = vehicles; }
    public void addVehicle(Vehicle v) { if (v != null) this.vehicles.add(v); }
    public void removeVehicle(Vehicle v) { this.vehicles.remove(v); }
    
    public Calendar getCalendar() { return calendar; }
    public void setCalendar(Calendar calendar) { this.calendar = calendar; }

    // méthodes utilitaire
    public int getTotalInscriptionNumber() { return inscriptions.size(); }
    public int getAvailableSeatNumber() { return 0; }
    
    @Override
    public String toString() {
        return "Ride{" +
                "id=" + id +
                ", num=" + num +
                ", startPlace='" + startPlace + '\'' +
                ", startDate=" + startDate +
                ", fee=" + fee +
                ", calendarId=" + (calendar != null ? calendar.getid() : "null") +
                ", totalInscriptions=" + (inscriptions != null ? inscriptions.size() : 0) +
                ", totalVehicles=" + (vehicles != null ? vehicles.size() : 0) +
                '}';
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
}

