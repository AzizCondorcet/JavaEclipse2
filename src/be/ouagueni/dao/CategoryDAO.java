package be.ouagueni.dao;

import java.sql.Connection;

import be.ouagueni.model.Category;

public class CategoryDAO extends DAO<Category> {
    public CategoryDAO(Connection conn) { super(conn); }
	@Override
	public boolean create(Category obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(Category obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Category obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Category find(int id) {
		// TODO Auto-generated method stub
		return null;
	}

}
