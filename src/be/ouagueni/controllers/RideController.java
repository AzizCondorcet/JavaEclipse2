package be.ouagueni.controllers;

import java.sql.Connection;
import be.ouagueni.dao.RideDAO;
import be.ouagueni.model.Ride;

public class RideController {

    private RideDAO rideDAO;

    public RideController(Connection conn) {
        this.rideDAO = new RideDAO(conn);
    }

    public boolean createRide(Ride ride) {
        try {
            return rideDAO.create(ride);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
