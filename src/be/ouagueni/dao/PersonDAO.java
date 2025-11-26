package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import be.ouagueni.model.Member;
import be.ouagueni.model.Person;
import be.ouagueni.model.Ride;
import be.ouagueni.model.Treasurer;
import be.ouagueni.model.TypeCat;
import be.ouagueni.model.Vehicle;
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
	
	public Person find(String name, String password) {
	    try {
	        String sqlPerson = "SELECT * FROM Person WHERE namesPers = ? AND psw = ?";
	        PreparedStatement psPerson = this.connect.prepareStatement(sqlPerson);
	        psPerson.setString(1, name);
	        psPerson.setString(2, password);
	        ResultSet rsPerson = psPerson.executeQuery();

	        if (!rsPerson.next()) 
	        {
	            rsPerson.close();
	            psPerson.close();
	            return null; 
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
	            
	            // CORRECT : l'ID principal du Member est idMember
	            member.setId(rsMember.getInt("idMember"));  // <-- CHANGEMENT ICI !!!
	            member.setIdMember(rsMember.getInt("idMember"));  // redondant mais clair
	            
	            member.setName(name);
	            member.setFirstname(firstname);
	            member.setTel(tel);
	            member.setPassword(psw);
	            member.setBalance(rsMember.getDouble("balance"));
	            
	            int idMember = rsMember.getInt("idMember");  // déjà fait ci-dessus
	            member.setIdMember(idMember);

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
	            Set<Bike> bikes = new HashSet<>();
	            String sqlBikes = "SELECT * FROM Bike WHERE idMember = ?";
	            try (PreparedStatement psBikes = this.connect.prepareStatement(sqlBikes)) {
	                psBikes.setInt(1, idMember);
	                try (ResultSet rsBikes = psBikes.executeQuery()) {
	                    while (rsBikes.next()) {
	                        Bike bike = new Bike();
	                        bike.setId(rsBikes.getInt("idBike"));
	                        bike.setWeight(rsBikes.getDouble("weight"));
	                        bike.setLength(rsBikes.getDouble("length"));
	                        
	                        int typeId = rsBikes.getInt("bikeType");
	                        TypeCat typeCat = TypeCat.fromId(typeId);
	                        if (typeCat != null) {
	                            bike.setType(typeCat);
	                        }
	                        
	                        bike.setOwner(member);
	                        bikes.add(bike);
	                    }
	                }
	            }
	            member.setBikes(bikes);

	         System.out.println("Chargement des catégories pour le membre ID " + idMember + "...");

	         Set<Category> categories = new HashSet<>();
	         String sqlCategories = """
	             SELECT c.idCategory, t.nameType
	             FROM Category_Member cm
	             INNER JOIN Category c ON cm.IDCategory = c.idCategory
	             INNER JOIN Type t ON c.Type = t.idType
	             WHERE cm.IDMember = ?
	             """;

	         try (PreparedStatement psCat = connect.prepareStatement(sqlCategories)) {
	             psCat.setInt(1, idMember);
	             try (ResultSet rsCat = psCat.executeQuery()) {
	                 while (rsCat.next()) {
	                     Category cat = new Category();
	                     cat.setid(rsCat.getInt("idCategory"));
	                     TypeCat typeCat = TypeCat.valueOf(rsCat.getString("nameType"));
	                     cat.setNomCategorie(typeCat);
	                     categories.add(cat);
	                 }
	             }
	         } catch (SQLException e) {
	             System.out.println("Erreur chargement catégories : " + e.getMessage());
	             e.printStackTrace();
	         }

	         member.setCategories(categories);
	         System.out.println("Catégories chargées : " + categories.size() + 
	             " → " + categories.stream().map(c -> c.getNomCategorie().name()).collect(Collectors.toList()));

	         return member; // Member complet
	        }

	        rsMember.close();
	        psMember.close();

	     // 3. Vérifier si c'est un Manager
	        String sqlManager = "SELECT * FROM Manager WHERE idPerson = ?";
	        try (PreparedStatement psManager = this.connect.prepareStatement(sqlManager)) 
	        {
	            psManager.setInt(1, personId);
	            try (ResultSet rsManager = psManager.executeQuery()) {

	                if (rsManager.next()) {
	                    Manager manager = new Manager();
	                    manager.setId(personId);
	                    manager.setName(name);
	                    manager.setFirstname(firstname);
	                    manager.setTel(tel);
	                    manager.setPassword(psw);
	                    int idManager = rsManager.getInt("idManager");

	                    // Map membre → idMember pour éviter les doublons
	                    java.util.Map<Integer, Member> memberById = new java.util.HashMap<>();

	                    // === CHARGEMENT CATÉGORIE + CALENDRIER + RIDES + INSCRIPTIONS ===
	                    String sqlCategory = """
	                        SELECT cat.idCategory, cat.Type, cal.idCalendar,
	                               r.idRide, r.num, r.startPlace, r.startDate, r.fee
	                        FROM Category cat
	                        INNER JOIN Calendar cal ON cat.idCategory = cal.idCategory
	                        LEFT JOIN Ride r ON cal.idCalendar = r.idCalendar
	                        WHERE cat.idManager = ?
	                        """;

	                    try (PreparedStatement psCategory = this.connect.prepareStatement(sqlCategory)) {
	                        psCategory.setInt(1, idManager);
	                        try (ResultSet rsCategory = psCategory.executeQuery()) {

	                            Category category = null;
	                            Calendar calendar = null;
	                            Set<Ride> rides = new HashSet<>();

	                            while (rsCategory.next()) {
	                                if (category == null) {
	                                    category = new Category();
	                                    category.setid(rsCategory.getInt("idCategory"));
	                                    TypeCat typeCat = TypeCat.fromId(rsCategory.getInt("Type"));
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
	                                    java.sql.Timestamp ts = rsCategory.getTimestamp("startDate");
	                                    if (ts != null) ride.setStartDate(ts.toLocalDateTime());
	                                    ride.setFee(rsCategory.getDouble("fee"));
	                                    ride.setCalendar(calendar);

	                                    // === INSCRIPTIONS DU RIDE ===
	                                    String sqlIns = """
	                                        SELECT i.idInscription, i.passenger, i.bike, i.idMember,
	                                               p.id, p.namesPers, p.firstname, p.tel, p.psw, m.balance,
	                                               b.idBike, b.weight, b.bikeType, b.length
	                                        FROM Inscription i
	                                        INNER JOIN Member m ON i.idMember = m.idMember
	                                        INNER JOIN Person p ON m.idPerson = p.id
	                                        LEFT JOIN Bike b ON i.idBike = b.idBike
	                                        WHERE i.idRide = ?
	                                        """;

	                                    try (PreparedStatement psIns = this.connect.prepareStatement(sqlIns)) {
	                                        psIns.setInt(1, rideId);
	                                        try (ResultSet rsIns = psIns.executeQuery()) {
	                                            Set<Inscription> inscriptions = new HashSet<>();
	                                            while (rsIns.next()) {
	                                                int idMember = rsIns.getInt("idMember");

	                                                Member member = memberById.computeIfAbsent(idMember, id -> {
	                                                	try {
	                                                        Member m = new Member();
	                                                        m.setIdMember(idMember);
	                                                        m.setId(rsIns.getInt("id"));                    // maintenant OK car dans un try
	                                                        m.setName(rsIns.getString("namesPers"));
	                                                        m.setFirstname(rsIns.getString("firstname"));
	                                                        m.setTel(rsIns.getString("tel"));
	                                                        m.setPassword(rsIns.getString("psw"));
	                                                        m.setBalance(rsIns.getDouble("balance"));
	                                                        return m;
	                                                    } catch (SQLException e) {
	                                                        throw new RuntimeException("Erreur lors de la lecture du membre ID " + idMember, e);
	                                                    }	                                                });

	                                                Inscription ins = new Inscription(
	                                                    rsIns.getInt("idInscription"),
	                                                    rsIns.getBoolean("passenger"),
	                                                    rsIns.getBoolean("bike")
	                                                );
	                                                ins.setMember(member);
	                                                ins.setRide(ride);

	                                                if (rsIns.getBoolean("bike") && rsIns.getObject("idBike") != null) {
	                                                    TypeCat bikeType = TypeCat.fromId(rsIns.getInt("bikeType"));
	                                                    Bike bike = new Bike(
	                                                        rsIns.getInt("idBike"),
	                                                        rsIns.getDouble("weight"),
	                                                        bikeType,
	                                                        rsIns.getDouble("length"),
	                                                        member
	                                                    );
	                                                    ins.setBikeObj(bike);
	                                                }
	                                                inscriptions.add(ins);
	                                            }
	                                            ride.setInscriptions(inscriptions);
	                                        }
	                                    }
	                                    rides.add(ride);
	                                }
	                            }

	                            if (!memberById.isEmpty()) {
	                                String inClause = memberById.keySet().stream()
	                                    .map(String::valueOf)
	                                    .collect(Collectors.joining(","));

	                                String sqlVeh = "SELECT * FROM Vehicule WHERE idMemberDriver IN (" + inClause + ")";

	                                try (PreparedStatement psVeh = this.connect.prepareStatement(sqlVeh);
	                                     ResultSet rsVeh = psVeh.executeQuery()) {

	                                    // 🔹 Map pour stocker les véhicules par membre
	                                    Map<Integer, Vehicle> vehiclesByMember = new HashMap<>();

	                                    while (rsVeh.next()) {
	                                        Vehicle vehicle = new Vehicle();
	                                        vehicle.setId(rsVeh.getInt("idVehicule"));
	                                        vehicle.setSeatNumber(rsVeh.getInt("seatNumber"));
	                                        vehicle.setBikeSpotNumber(rsVeh.getInt("bikeSpotNumber"));

	                                        int driverId = rsVeh.getInt("idMemberDriver");
	                                        Member driver = memberById.get(driverId);
	                                        if (driver != null) {
	                                            vehicle.setDriver(driver);
	                                            driver.setDriver(vehicle);
	                                            vehiclesByMember.put(driverId, vehicle);
	                                        }
	                                    }

	                                    // 🔹 MAINTENANT : Associer les véhicules aux Rides
	                                    for (Ride ride : rides) {
	                                        Set<Vehicle> vehiclesDuRide = new HashSet<>();
	                                        
	                                        if (ride.getInscriptions() != null) {
	                                            for (Inscription ins : ride.getInscriptions()) {
	                                                Member member = ins.getMember();
	                                                if (member != null && vehiclesByMember.containsKey(member.getIdMember())) {
	                                                    vehiclesDuRide.add(vehiclesByMember.get(member.getIdMember()));
	                                                }
	                                            }
	                                        }
	                                        
	                                        ride.setVehicles(vehiclesDuRide);
	                                    }
	                                }
	                                System.out.println("Véhicules chargés et associés aux rides.");
	                            }

	                            // Finalisation des relations
	                            if (calendar != null) calendar.setRides(rides);
	                            if (category != null) manager.setCategory(category);

	                            System.out.println("Manager chargé avec succès – covoiturage prêt !");
	                            return manager;
	                        }
	                    }
	                }
	            }
	        } catch (SQLException e) {
	            e.printStackTrace();
	            return null;
	        }
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
