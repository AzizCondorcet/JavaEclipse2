package be.ouagueni.dao;

import java.sql.Connection;

import be.ouagueni.model.Bike;

public class BikeDAO extends DAO<Bike> {
    public BikeDAO(Connection conn) { super(conn); }

    @Override
    public boolean create(Bike obj) {
        // TODO : implémenter INSERT SQL
        return false;
    }

    @Override
    public boolean delete(Bike obj) {
        // TODO : implémenter DELETE SQL
        return false;
    }

    @Override
    public boolean update(Bike obj) {
        // TODO : implémenter UPDATE SQL
        return false;
    }

    @Override
    public Bike find(int id) {
        // TODO : implémenter UPDATE SQL
        return null;
    }
}

