package be.ouagueni.model;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

import be.ouagueni.dao.InscriptionDAO;

public class Inscription implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private boolean passenger;
    private boolean bike;
    private Member member;
    private Ride ride;
    private Bike bikeObj;

    public Inscription() {}
    public Inscription(int id, boolean passenger, boolean bike) {
        this.id = id; this.passenger = passenger; this.bike = bike;
    }

    // getters / setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public boolean isPassenger() { return passenger; }
    public void setPassenger(boolean passenger) { this.passenger = passenger; }
    public boolean isBike() { return bike; }
    public void setBike(boolean bike) { this.bike = bike; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public Ride getRide() { return ride; }
    public void setRide(Ride ride) { this.ride = ride; }
    public Bike getBikeObj() { return bikeObj; }
    public void setBikeObj(Bike bikeObj) { this.bikeObj = bikeObj; }
    
    public boolean create(Connection conn) throws SQLException {
        InscriptionDAO dao = new InscriptionDAO(conn);
        return dao.create(this);
    }
}

