package be.ouagueni.model;

import java.io.Serializable;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

import be.ouagueni.dao.VehicleDAO;

public class Vehicle implements Serializable {
    private static final long serialVersionUID = 6135289786991520925L;
    private int id;
    private int seatNumber;
    private int bikeSpotNumber;
    private Set<Member> passengers = new HashSet<>(); // 1..* : au moins 1 passager
    private Member driver; // 1 : exactement 1 conducteur (pas un Set!)
    private Set<Bike> bikes = new HashSet<>(); // 0..* : peut être vide
    private Set<Ride> rides = new HashSet<>(); // 1..* : au moins 1 ride

    // Constructeurs
    public Vehicle() {}
    
    public Vehicle(int id, int seatNumber, int bikeSpotNumber) {
        this.id = id; 
        this.seatNumber = seatNumber; 
        this.bikeSpotNumber = bikeSpotNumber;
    }
    
    public Vehicle(int id, int seatNumber, int bikeSpotNumber, Member driver, Member passenger, Ride ride) {
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

    public Vehicle(int seatNumber2, int bikeSpotNumber2, Member member)
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
        VehicleDAO dao = new VehicleDAO(conn);
        return dao.create(this);
    }
    
}