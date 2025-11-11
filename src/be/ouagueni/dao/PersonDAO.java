package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import be.ouagueni.model.Member;
import be.ouagueni.model.Person;
import be.ouagueni.model.Ride;
import be.ouagueni.model.Treasurer;
import be.ouagueni.model.TypeCat;
import be.ouagueni.model.Bike;
import be.ouagueni.model.Calendar;
import be.ouagueni.model.Category;
import be.ouagueni.model.Manager;
import be.ouagueni.model.Inscription;

public class PersonDAO extends DAO<Person> {
    public PersonDAO(Connection conn) { super(conn); }

	@Override
	public boolean create(Person obj) {
	    return false;
	}
    
	@Override
	public boolean delete(Person obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Person obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Person find(int id) {
		return null;
	}
	
	public Person findByNameAndPassword(String name, String password) {
	    try {
	        // 1️⃣ Récupérer la personne de base
	        String sqlPerson = "SELECT * FROM Person WHERE namesPers = ? AND psw = ?";
	        PreparedStatement psPerson = this.connect.prepareStatement(sqlPerson);
	        psPerson.setString(1, name);
	        psPerson.setString(2, password);
	        ResultSet rsPerson = psPerson.executeQuery();

	        if (!rsPerson.next()) {
	            rsPerson.close();
	            psPerson.close();
	            return null; // personne non trouvée
	        }

	        int personId = rsPerson.getInt("id");
	        String firstname = rsPerson.getString("firstname");
	        String tel = rsPerson.getString("tel");
	        String psw = rsPerson.getString("psw");

	        rsPerson.close();
	        psPerson.close();

	        // 2️⃣ Vérifier si c'est un Member
	        String sqlMember = "SELECT * FROM Member WHERE idPerson = ?";
	        PreparedStatement psMember = this.connect.prepareStatement(sqlMember);
	        psMember.setInt(1, personId);
	        ResultSet rsMember = psMember.executeQuery();

	        if (rsMember.next()) {
	            Member member = new Member();
	            member.setId(personId);
	            member.setName(name);
	            member.setFirstname(firstname);
	            member.setTel(tel);
	            member.setPassword(psw);
	            member.setBalance(rsMember.getDouble("balance"));
	            int idMember = rsMember.getInt("idMember");

	            rsMember.close();
	            psMember.close();

	            // 🔹 Hydrater les inscriptions, vélos, rides, calendrier et catégorie
	            Set<Inscription> inscriptions = new HashSet<>();
	            String sqlInscriptions =
	                "SELECT i.*, " +
	                "b.idBike AS bike_idBike, b.weight AS bike_weight, b.bikeType AS bike_type, b.length AS bike_length, b.idVehicule AS bike_idVehicule, " +
	                "r.idRide AS ride_idRide, r.num AS ride_num, r.startPlace AS ride_startPlace, r.startDate AS ride_startDate, r.fee AS ride_fee, " +
	                "cal.idCalendar AS cal_idCalendar, " +
	                "cat.idCategory AS cat_idCategory, cat.Type AS cat_Type, cat.idManager AS cat_idManager " +
	                "FROM Inscription i " +
	                "LEFT JOIN Bike b ON i.idBike = b.idBike " +
	                "INNER JOIN Ride r ON i.idRide = r.idRide " +
	                "INNER JOIN Calendar cal ON r.idCalendar = cal.idCalendar " +
	                "INNER JOIN Category cat ON cal.idCategory = cat.idCategory " +
	                "WHERE i.idMember = ?";

	            PreparedStatement psInscriptions = this.connect.prepareStatement(sqlInscriptions);
	            psInscriptions.setInt(1, idMember);
	            ResultSet rsInscriptions = psInscriptions.executeQuery();

	            while (rsInscriptions.next()) {
	                Inscription inscription = new Inscription(
	                    rsInscriptions.getInt("idInscription"),
	                    rsInscriptions.getBoolean("passenger"),
	                    rsInscriptions.getBoolean("bike")
	                );
	                inscription.setMember(member);

	                // Calendar
	                Calendar calendar = new Calendar();
	                calendar.setid(rsInscriptions.getInt("cal_idCalendar"));

	                // Category
	                Category category = new Category();
	                category.setid(rsInscriptions.getInt("cat_idCategory"));
	                int typeId = rsInscriptions.getInt("cat_Type");
	                TypeCat typeCat = switch (typeId) {
	                    case 1 -> TypeCat.MountainBike;
	                    case 2 -> TypeCat.RoadBike;
	                    case 3 -> TypeCat.Trial;
	                    case 4 -> TypeCat.Downhill;
	                    case 5 -> TypeCat.Cross;
	                    default -> null;
	                };
	                if (typeCat != null) category.setNomCategorie(typeCat);

	                calendar.setCategory(category);
	                category.setCalendar(calendar);

	                // Ride
	                Ride ride = new Ride();
	                ride.setId(rsInscriptions.getInt("ride_idRide"));
	                ride.setnum(rsInscriptions.getInt("ride_num"));
	                ride.setStartPlace(rsInscriptions.getString("ride_startPlace"));
	                java.sql.Timestamp timestamp = rsInscriptions.getTimestamp("ride_startDate");
	                if (timestamp != null) ride.setStartDate(timestamp.toLocalDateTime());
	                ride.setFee(rsInscriptions.getDouble("ride_fee"));
	                ride.setCalendar(calendar);
	                inscription.setRide(ride);

	                // Bike
	                if (rsInscriptions.getBoolean("bike") && rsInscriptions.getObject("bike_idBike") != null) {
	                    int typeId2 = rsInscriptions.getInt("bike_type"); // Maintenant c'est un int en BDD
	                    TypeCat typeCat2 = TypeCat.fromId(typeId2);

	                    Bike bike = new Bike(
	                        rsInscriptions.getInt("bike_idBike"),
	                        rsInscriptions.getDouble("bike_weight"),
	                        typeCat2,
	                        rsInscriptions.getDouble("bike_length"),
	                        member
	                    );
	                    inscription.setBikeObj(bike);
	                }

	                inscriptions.add(inscription);
	            }

	            rsInscriptions.close();
	            psInscriptions.close();
	            member.setInscriptions(inscriptions);

	            return member; // Member complet
	        }

	        rsMember.close();
	        psMember.close();

	        // 3️⃣ Vérifier si c'est un Manager
	        String sqlManager = "SELECT * FROM Manager WHERE idPerson = ?";
	        PreparedStatement psManager = this.connect.prepareStatement(sqlManager);
	        psManager.setInt(1, personId);
	        ResultSet rsManager = psManager.executeQuery();

	        if (rsManager.next()) {
	            Manager manager = new Manager();
	            manager.setId(personId);
	            manager.setName(name);
	            manager.setFirstname(firstname);
	            manager.setTel(tel);
	            manager.setPassword(psw);
	            int idManager = rsManager.getInt("idManager");

	            rsManager.close();
	            psManager.close();

	            // 🔹 Hydrater Category, Calendar et Rides avec inscriptions intégrées
	            String sqlCategory =
	                "SELECT cat.idCategory, cat.Type, cal.idCalendar, r.idRide, r.num, r.startPlace, r.startDate, r.fee " +
	                "FROM Category cat " +
	                "INNER JOIN Calendar cal ON cat.idCategory = cal.idCategory " +
	                "LEFT JOIN Ride r ON cal.idCalendar = r.idCalendar " +
	                "WHERE cat.idManager = ?";

	            PreparedStatement psCategory = this.connect.prepareStatement(sqlCategory);
	            psCategory.setInt(1, idManager);
	            ResultSet rsCategory = psCategory.executeQuery();

	            Category category = null;
	            Calendar calendar = null;
	            Set<Ride> rides = new HashSet<>();

	            while (rsCategory.next()) {
	                if (category == null) {
	                    category = new Category();
	                    category.setid(rsCategory.getInt("idCategory"));

	                    int typeId = rsCategory.getInt("Type");
	                    TypeCat typeCat = switch (typeId) {
	                        case 1 -> TypeCat.MountainBike;
	                        case 2 -> TypeCat.RoadBike;
	                        case 3 -> TypeCat.Trial;
	                        case 4 -> TypeCat.Downhill;
	                        case 5 -> TypeCat.Cross;
	                        default -> null;
	                    };
	                    if (typeCat != null) category.setNomCategorie(typeCat);

	                    category.setManager(manager);

	                    calendar = new Calendar();
	                    calendar.setid(rsCategory.getInt("idCalendar"));
	                    calendar.setCategory(category);
	                    category.setCalendar(calendar);
	                }

	                int rideId = rsCategory.getInt("idRide");
	                if (rideId > 0) {
	                    Ride ride = new Ride();
	                    ride.setId(rideId);
	                    ride.setnum(rsCategory.getInt("num"));
	                    ride.setStartPlace(rsCategory.getString("startPlace"));
	                    java.sql.Timestamp timestamp = rsCategory.getTimestamp("startDate");
	                    if (timestamp != null) ride.setStartDate(timestamp.toLocalDateTime());
	                    ride.setFee(rsCategory.getDouble("fee"));
	                    ride.setCalendar(calendar);

	                    // 🔹 Intégrer les inscriptions du ride directement
	                    String sqlInscriptions =
	                        "SELECT i.idInscription, i.passenger, i.bike, " +
	                        "m.idPerson, p.namesPers, p.firstname, p.tel, p.psw, m.balance, " +
	                        "b.idBike, b.weight, b.bikeType, b.length " +
	                        "FROM Inscription i " +
	                        "INNER JOIN Member m ON i.idMember = m.idMember " +
	                        "INNER JOIN Person p ON m.idPerson = p.id " +
	                        "LEFT JOIN Bike b ON i.idBike = b.idBike " +
	                        "WHERE i.idRide = ?";

	                    try (PreparedStatement psIns = this.connect.prepareStatement(sqlInscriptions)) {
	                        psIns.setInt(1, rideId);
	                        try (ResultSet rsIns = psIns.executeQuery()) {
	                            Set<Inscription> rideInscriptions = new HashSet<>();
	                            while (rsIns.next()) {
	                                Inscription ins = new Inscription(
	                                    rsIns.getInt("idInscription"),
	                                    rsIns.getBoolean("passenger"),
	                                    rsIns.getBoolean("bike")
	                                );

	                                Member memberIns = new Member();
	                                memberIns.setId(rsIns.getInt("idPerson"));
	                                memberIns.setName(rsIns.getString("namesPers"));
	                                memberIns.setFirstname(rsIns.getString("firstname"));
	                                memberIns.setTel(rsIns.getString("tel"));
	                                memberIns.setPassword(rsIns.getString("psw"));
	                                memberIns.setBalance(rsIns.getDouble("balance"));

	                                ins.setMember(memberIns);

	                                if (rsIns.getBoolean("bike") && rsIns.getObject("idBike") != null) {
	                                    int typeId3 = rsIns.getInt("bikeType"); // type stocké en int dans la BDD
	                                    TypeCat typeCat3 = TypeCat.fromId(typeId3); // convertir en TypeCat

	                                    Bike bike = new Bike(
	                                        rsIns.getInt("idBike"),
	                                        rsIns.getDouble("weight"),
	                                        typeCat3,
	                                        rsIns.getDouble("length"),
	                                        memberIns
	                                    );
	                                    ins.setBikeObj(bike);
	                                }


	                                rideInscriptions.add(ins);
	                            }
	                            ride.setInscriptions(rideInscriptions);
	                        }
	                    }

	                    rides.add(ride);
	                }
	            }

	            rsCategory.close();
	            psCategory.close();

	            if (calendar != null && !rides.isEmpty()) calendar.setRides(rides);
	            if (category != null) manager.setCategory(category);

	            return manager; // Manager complet
	        }

	        rsManager.close();
	        psManager.close();

	        // 4️⃣ Vérifier si c'est un Tresurer
	        String sqlTresurer = "SELECT * FROM Tresurer WHERE idPerson = ?";
	        PreparedStatement psTresurer = this.connect.prepareStatement(sqlTresurer);
	        psTresurer.setInt(1, personId);
	        ResultSet rsTresurer = psTresurer.executeQuery();

	        if (rsTresurer.next()) {
	            Treasurer tresurer = new Treasurer(personId, name, firstname, tel, psw);
	            
	            rsTresurer.close();
	            psTresurer.close();
	            
	            return tresurer;
	        }

	        rsTresurer.close();
	        psTresurer.close();

	        return null;


	    } catch (SQLException e) {
	        e.printStackTrace();
	        return null;
	    }
	}
}
