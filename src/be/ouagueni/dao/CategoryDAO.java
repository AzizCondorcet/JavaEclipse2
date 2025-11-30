package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import be.ouagueni.model.Category;
import be.ouagueni.model.Manager;
import be.ouagueni.model.TypeCat;

public class CategoryDAO extends DAO<Category> {
    public CategoryDAO(Connection conn) { super(conn); }
	@Override
	public boolean create(Category category) {
	
	    return false; 
	}

	@Override
	public boolean delete(Category obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Category category) {
	    try {
	        int typeId = -1;
	        String sqlType = "SELECT idType FROM Type WHERE nameType = ?";
	        try (PreparedStatement psType = connect.prepareStatement(sqlType)) {
	            psType.setString(1, category.getNomCategorie().name());
	            try (ResultSet rsType = psType.executeQuery()) {
	                if (rsType.next()) {
	                    typeId = rsType.getInt("idType");
	                } else {
	                    throw new Exception("Type inconnu : " + category.getNomCategorie().name());
	                }
	            }
	        }

	        // Récupérer l'ID du manager 
	        int managerId = -1;
	        int CatID =-1;
	        String sqlManager = "SELECT idManager ,idCategory FROM Category WHERE Type = ?";
	        try (PreparedStatement psManager = connect.prepareStatement(sqlManager)) {
	            psManager.setInt(1, typeId);
	            try (ResultSet rsManager = psManager.executeQuery()) {
	                if (rsManager.next()) {
	                    managerId = rsManager.getInt("idManager");
	                    CatID = rsManager.getInt("idCategory");
	                } else {
	                    throw new Exception("Aucun manager trouvé pour ce type !");
	                }
	            }
	        }

	        // Récupérer les infos du Manager
	        String sqlFullManager = """
	            	SELECT
				    p.id,
				    p.namesPers,
				    p.firstname,
				    p.tel,
				    p.psw
				FROM
				    Manager AS m
				    INNER JOIN Person AS p ON m.idPerson = p.id
				WHERE
				    m.idManager = ?;
	        """;

	        Manager manager = null;
	        try (PreparedStatement psFull = connect.prepareStatement(sqlFullManager)) {
	            psFull.setInt(1, managerId);
	            try (ResultSet rs = psFull.executeQuery()) {
	                if (rs.next()) {
	                    // Instanciation conforme 
	                    manager = new Manager(
	                        rs.getInt("id"),
	                        rs.getString("namesPers"),
	                        rs.getString("firstname"),
	                        rs.getString("tel"),
	                        rs.getString("psw")
	                    );
	                } else {
	                    throw new Exception("Manager non trouvé avec idManager = " + managerId);
	                }
	            }
	        }

	        // Mettre à jour uniquement l’objet Category 
	        category.setid(CatID); 
	        category.setManager(manager);
	        return true;

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return false;
	}

	@Override
	public Category find(int id) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Set<Category> GetAll() {
	    Set<Category> categories = new HashSet<>();
	    String sql = """
	        SELECT 
	            c.idCategory,
	            t.idType,
	            t.nameType,
	            m.idManager,
	            p.id AS personId,
	            p.namesPers,
	            p.firstname,
	            p.tel,
	            p.psw
	        FROM Category c
	        INNER JOIN Type t ON c.Type = t.idType
	        LEFT JOIN Manager m ON c.idManager = m.idManager
	        LEFT JOIN Person p ON m.idPerson = p.id
	        ORDER BY t.nameType
	        """;

	    try (PreparedStatement ps = connect.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {

	        while (rs.next()) {
	            TypeCat typeCat = TypeCat.valueOf(rs.getString("nameType"));

	            Manager manager = null;
	            if (rs.getObject("idManager") != null) {
	                manager = new Manager(
	                    rs.getInt("personId"),
	                    rs.getString("namesPers"),
	                    rs.getString("firstname"),
	                    rs.getString("tel"),
	                    rs.getString("psw")
	                );
	                manager.setId(rs.getInt("idManager"));
	            }

	            Category cat = new Category();
	            cat.setid(rs.getInt("idCategory"));
	            cat.setNomCategorie(typeCat);
	            cat.setManager(manager);

	            categories.add(cat);
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    return categories;
	}

}
