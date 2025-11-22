package be.ouagueni.dao;

import java.sql.*;
import be.ouagueni.model.*;

public class MemberDAO extends DAO<Member> {

    public MemberDAO(Connection conn) {
        super(conn);
    }

    @Override
    public boolean create(Member member) {
        String sqlPerson = "INSERT INTO Person (namesPers, firstname, tel, psw) VALUES (?, ?, ?, ?)";
        String sqlMember = "INSERT INTO Member (idPerson, balance) VALUES (?, ?)";
        String sqlCat    = "INSERT INTO Category_Member (IDMember, IDCategory) VALUES (?, ?)";
        String sqlBike   = "INSERT INTO Bike (weight, bikeType, length, idMember, idVehicle) VALUES (?, ?, ?, ?, ?)";

        int idMember = 0;

        try {
            connect.setAutoCommit(false);

            // 1. Person
            try (PreparedStatement ps = connect.prepareStatement(sqlPerson, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, member.getName());
                ps.setString(2, member.getFirstname());
                ps.setString(3, member.getTel());
                ps.setString(4, member.getPassword());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) member.setId(rs.getInt(1));
                }
            }

            // 2. Member
            try (PreparedStatement ps = connect.prepareStatement(sqlMember, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, member.getId());
                ps.setDouble(2, member.getBalance());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        idMember = rs.getInt(1);
                        member.setIdMember(idMember);
                    }
                }
            }

            for (Category cat : member.getCategories()) {
                try (PreparedStatement ps = connect.prepareStatement(sqlCat)) {
                    ps.setInt(1, idMember);
                    ps.setInt(2, cat.getid());
                    ps.executeUpdate();
                }
            }

            // 4. Vélos
            for (Bike bike : member.getBikes()) {
                bike.setOwner(member);
                try (PreparedStatement ps = connect.prepareStatement(sqlBike, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setDouble(1, bike.getWeight());
                    ps.setInt(2, bike.getTypeAsInt());
                    ps.setDouble(3, bike.getLength());
                    ps.setInt(4, idMember);
                    ps.setNull(5, java.sql.Types.INTEGER);
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) bike.setId(rs.getInt(1));
                    }
                }
            }

            connect.commit();
            System.out.println("Membre créé avec succès ! ID: " + idMember + 
                " | Catégories: " + member.getCategories().size());
            return true;

        } catch (SQLException e) {
            try { connect.rollback(); } catch (SQLException ignored) {}
            e.printStackTrace();
            return false;
        } finally {
            try { connect.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    @Override
    public boolean delete(Member obj) { return false; }

    @Override
    public boolean update(Member member) {
        String deleteCats = "DELETE FROM Category_Member WHERE IDMember = ?";
        String insertCat  = "INSERT INTO Category_Member (IDMember, IDCategory) VALUES (?, ?)";
        String updateBalance = "UPDATE Member SET balance = ? WHERE idMember = ?";

        try {
            connect.setAutoCommit(false);

            // 1. Supprimer toutes les anciennes associations catégorie/membre
            try (PreparedStatement ps = connect.prepareStatement(deleteCats)) {
                ps.setInt(1, member.getIdMember());
                ps.executeUpdate();
            }

            // 2. Réinsérer les catégories actuelles du membre
            for (Category cat : member.getCategories()) {
                try (PreparedStatement ps = connect.prepareStatement(insertCat)) {
                    ps.setInt(1, member.getIdMember());
                    ps.setInt(2, cat.getid());
                    ps.executeUpdate();
                }
            }

            // 3. Mettre à jour le solde
            try (PreparedStatement ps = connect.prepareStatement(updateBalance)) {
                ps.setDouble(1, member.getBalance());
                ps.setInt(2, member.getIdMember());
                ps.executeUpdate();
            }

            connect.commit();
            return true;

        } catch (SQLException e) {
            try { connect.rollback(); } catch (SQLException ignored) {}
            e.printStackTrace();
            return false;
        } finally {
            try { connect.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }
    @Override
    public Member find(int id) { return null; }
}
