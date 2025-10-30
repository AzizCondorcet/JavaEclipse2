package be.ouagueni.controllers;

import java.sql.Connection;

import be.ouagueni.dao.CategoryDAO;
import be.ouagueni.model.Category;


public class CategoryController {
	
	private CategoryDAO categoryDAO;
	
    public CategoryController(Connection conn) {
        this.categoryDAO = new CategoryDAO(conn);
    }
    
    public boolean updateCategory(Category ca) {
        try {
            return categoryDAO.update(ca);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
