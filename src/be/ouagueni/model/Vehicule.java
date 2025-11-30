package be.ouagueni.model;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import be.ouagueni.dao.VehiculeDAO;

public class Vehicule implements Serializable {
    private static final long serialVersionUID = 6135289786991520925L;
    private int id;
    private int seatNumber;
    private int bikeSpotNumber;
    private Set<Member> passengers = new HashSet<>(); 
    private Member driver;
    private Set<Bike> bikes = new HashSet<>(); 
    private Set<Ride> rides = new HashSet<>(); 

    public Vehicule() {}
    
    public Vehicule(int id, int seatNumber, int bikeSpotNumber) {
        this.id = id; 
        this.seatNumber = seatNumber; 
        this.bikeSpotNumber = bikeSpotNumber;
    }
    
    public Vehicule(int id, int seatNumber, int bikeSpotNumber, Member driver, Member passenger, Ride ride) {
        this.id = id;
        this.seatNumber = seatNumber;
        this.bikeSpotNumber = bikeSpotNumber;
        
        if (driver == null) {
            throw new IllegalArgumentException("Un véhicule doit avoir 1 conducteur");
        }
        if (passenger == null) {
            throw new IllegalArgumentException("Un véhicule doit avoir au moins 1 passager");
        }
        if (ride == null) {
            throw new IllegalArgumentException("Un véhicule doit avoir au moins 1 ride");
        }
        
        this.driver = driver;
        this.passengers.add(passenger);
        this.rides.add(ride);
    }

    public Vehicule(int seatNumber2, int bikeSpotNumber2, Member member)
    {
    		seatNumber = seatNumber2;
    		bikeSpotNumber = bikeSpotNumber2;
    		driver = member;
	}

	public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }
    
    public int getSeatNumber() { 
        return seatNumber; 
    }
    
    public void setSeatNumber(int seatNumber) { 
        this.seatNumber = seatNumber; 
    }
    
    public int getBikeSpotNumber() { 
        return bikeSpotNumber; 
    }
    
    public void setBikeSpotNumber(int bikeSpotNumber) { 
        this.bikeSpotNumber = bikeSpotNumber; 
    }

    public Member getDriver() { 
        return driver; 
    }
    
    public void setDriver(Member driver) {
        if (driver == null) {
            throw new IllegalArgumentException("Un véhicule doit avoir 1 conducteur");
        }
        this.driver = driver; 
    }

    public Set<Member> getPassengers() { 
        return passengers; 
    }
    
    public void setPassengers(Set<Member> passengers) {
        if (passengers == null || passengers.isEmpty()) {
            throw new IllegalArgumentException("Un véhicule doit avoir au moins 1 passager");
        }
        this.passengers = passengers; 
    }
    
    public void addPassenger(Member m) { 
        if (m != null) {
            passengers.add(m);
        }
    }
    
    public void removePassenger(Member m) {
        if (this.passengers.size() <= 1) {
            throw new IllegalStateException("Un véhicule doit avoir au moins 1 passager");
        }
        passengers.remove(m); 
    }

    public Set<Bike> getBikes() { 
        return bikes; 
    }
    
    public void setBikes(Set<Bike> bikes) { 
        if (bikes == null) {
            this.bikes = new HashSet<>();
        } else {
            this.bikes = bikes;
        }
    }
    
    public void addBike(Bike b) { 
        if (b != null) {
            bikes.add(b);
        }
    }
    
    public void removeBike(Bike b) { 
        bikes.remove(b); 
    }

    public Set<Ride> getRides() { 
        return rides; 
    }
    
    public void setRides(Set<Ride> rides) {
        if (rides == null || rides.isEmpty()) {
            throw new IllegalArgumentException("Un véhicule doit avoir au moins 1 ride");
        }
        this.rides = rides; 
    }
    
    public void addRide(Ride ride) { 
        if (ride != null) {
            rides.add(ride);
        }
    }
    
    public void removeRide(Ride ride) {
        if (this.rides.size() <= 1) {
            throw new IllegalStateException("Un véhicule doit avoir au moins 1 ride");
        }
        rides.remove(ride); 
    }
    public boolean create(Connection conn) {
        VehiculeDAO dao = new VehiculeDAO(conn);
        return dao.create(this);
    }
    public int getAvailableSeatNumber() {
        long used = getPassengers().size();
        return seatNumber - (int) used;
    }

    public int getAvailableBikeSpotNumber(Ride ride) {
        if (ride == null) return bikeSpotNumber;

        long usedBikes = getPassengers().stream()
                .filter(passenger -> passenger != null)
                .flatMap(passenger -> passenger.getInscriptions().stream())
                .filter(ins -> ins.getRide() != null && ins.getRide().equals(ride))
                .filter(Inscription::isBike)
                .count();

        return bikeSpotNumber - (int) usedBikes;
    }
  
    public boolean update(Connection conn) throws SQLException {
        VehiculeDAO dao = new VehiculeDAO(conn);
        return dao.update(this);
    }
    
    public boolean save(Connection conn) throws SQLException {
        VehiculeDAO dao = new VehiculeDAO(conn);
        return dao.save(this); 
    }

    public static Vehicule getOrCreateForDriver(Member driver, Connection conn) throws SQLException {
        if (driver == null || driver.getIdMember() <= 0) {
            return null;
        }

        VehiculeDAO dao = new VehiculeDAO(conn);
        Vehicule v = dao.findByDriverId(driver.getIdMember());

        if (v != null) {
            v.setDriver(driver);
            return v;
        }

        return null;
    }
    
    public Vehicule findByDriverId(int id,Connection conn) throws SQLException {
        VehiculeDAO dao = new VehiculeDAO(conn);
        return dao.findByDriverId(id); 
    }
    
    public static Vehicule ensureVehicleExists(Member driver, Connection conn) throws SQLException {
    		Vehicule v = getOrCreateForDriver(driver, conn); 
        if (v != null) {
            return v;
        }

        Vehicule newVehicle = new Vehicule(1, 0, driver);
        VehiculeDAO dao = new VehiculeDAO(conn);
        if (dao.create(newVehicle)) {
            return newVehicle;
        }
        throw new SQLException("Échec de la création du véhicule");
    }
    
}