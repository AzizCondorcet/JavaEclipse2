package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import be.ouagueni.model.Inscription;

public class InscriptionDAO extends DAO<Inscription> {
    public InscriptionDAO(Connection conn) { super(conn); }

    @Override
    public boolean create(Inscription ins) {
        if (ins == null || ins.getMember() == null || ins.getRide() == null) {
            System.err.println("Inscription nulle ou incomplète");
            return false;
        }

        int idMember = ins.getMember().getIdMember();
        int idRide   = ins.getRide().getId();

        // Étape 1 : Vérifier s'il existe déjà
        String checkSql = "SELECT COUNT(*) FROM Inscription WHERE idMember = ? AND idRide = ?";
        try (PreparedStatement checkPs = connect.prepareStatement(checkSql)) {
            checkPs.setInt(1, idMember);
            checkPs.setInt(2, idRide);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.err.println("Doublon détecté : ce membre est déjà inscrit à cette balade !");
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification du doublon : " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        // Étape 2 : Insertion normale (sans FROM dual ni NOT EXISTS)
        String insertSql = "INSERT INTO Inscription (idMember, idRide, passenger, bike, idBike) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connect.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, idMember);
            ps.setInt(2, idRide);
            ps.setBoolean(3, ins.isPassenger());
            ps.setBoolean(4, ins.isBike());

            if (ins.isBike() && ins.getBikeObj() != null && ins.getBikeObj().getId() > 0) {
                ps.setInt(5, ins.getBikeObj().getId());
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);  // parfait
            }

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.err.println("Échec de l'insertion (aucune ligne affectée)");
                return false;
            }

            // Récupérer l'ID généré
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    ins.setId(generatedKeys.getInt(1));
                    System.out.println("Inscription créée avec ID : " + ins.getId());
                }
            }

            System.out.println("Inscription créée avec succès !");
            return true;

        } catch (SQLException e) {
            System.err.println("Erreur SQL lors de la création de l'inscription : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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
