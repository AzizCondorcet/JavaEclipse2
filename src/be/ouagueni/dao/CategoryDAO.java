package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import be.ouagueni.model.Category;
import be.ouagueni.model.Manager;

public class CategoryDAO extends DAO<Category> {
    public CategoryDAO(Connection conn) { super(conn); }
	@Override
	public boolean create(Category category) {
	
	    return false; // en cas d'erreur
	}



	@Override
	public boolean delete(Category obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Category category) {
	    try {
	        // 1️⃣ Récupérer l'ID du Type correspondant au nom de la catégorie
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

	        // 2️⃣ Récupérer l'ID du manager correspondant à ce type
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

	        // 3️⃣ Récupérer les infos du Manager (héritées de Person)
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
	                    // ✅ Instanciation conforme à ta classe Manager
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

	        // 4️⃣ Mettre à jour uniquement l’objet Category (pas la BD)
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

}
