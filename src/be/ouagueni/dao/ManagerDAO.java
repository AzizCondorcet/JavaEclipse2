package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import be.ouagueni.model.Manager;
import be.ouagueni.model.Person;
import be.ouagueni.model.TypeCat;

public class ManagerDAO extends DAO<Manager> {
    public ManagerDAO(Connection conn) { super(conn); }

	@Override
	public boolean create(Manager obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(Manager obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Manager obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Manager find(int id) {
		// TODO Auto-generated method stub
		return null;
	}

}
