package be.ouagueni.dao;

import java.sql.Connection;

import be.ouagueni.model.Inscription;

public class InscriptionDAO extends DAO<Inscription> {
    public InscriptionDAO(Connection conn) { super(conn); }

	@Override
	public boolean create(Inscription obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(Inscription obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Inscription obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Inscription find(int id) {
		// TODO Auto-generated method stub
		return null;
	}

}
