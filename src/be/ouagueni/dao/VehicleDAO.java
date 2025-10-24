package be.ouagueni.dao;

import java.sql.Connection;

import be.ouagueni.model.Vehicle;

public class VehicleDAO extends DAO<Vehicle> {
    public VehicleDAO(Connection conn) { super(conn); }

	@Override
	public boolean create(Vehicle obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(Vehicle obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Vehicle obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Vehicle find(int id) {
		// TODO Auto-generated method stub
		return null;
	}

}
