package be.ouagueni.model;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;



import be.ouagueni.connection.ClubConnection;
import be.ouagueni.ui.OptimisationCovoiturage;


public class AppModel {

    private static AppModel instance;
    private final Connection conn;
    private final Map<Integer, Boolean> cotisationsPayees = new HashMap<>();

    private AppModel() {
        this.conn = ClubConnection.getInstance();
    }

    public static synchronized AppModel getInstance() {
        if (instance == null) {
            instance = new AppModel();
        }
        return instance;
    }

    public Connection getConnection() {
        return conn;
    }
    
    // ===================================================================
    // ==================== JFRAME CREER BALADE ==========================
    // ===================================================================

    public boolean creerBalade(String nbPlacesStr, String lieu, String dateStr,
            String prixStr, TypeCat categorieChoisie, Manager manager)
    {
		try 
		{
			if (categorieChoisie == null) return false;
			if (nbPlacesStr == null || !nbPlacesStr.matches("\\d+")) return false;
			if (lieu == null || lieu.isEmpty()) return false;
			if (prixStr == null || !prixStr.matches("\\d+(\\.\\d{1,2})?")) return false;
			
			int nbPlaces = Integer.parseInt(nbPlacesStr);
			double prix = Double.parseDouble(prixStr.replace(",", "."));
			LocalDateTime dateDepart = LocalDateTime.parse(dateStr,
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
			
			// Détermination de la catégorie
			Category category = manager.getCategory();
			if (category == null) {
			category = new Category(0, categorieChoisie, null, null);
			}
			
			// Création ou récupération du calendrier
			Calendar calendar = category.getCalendar();
			if (calendar == null) {
			calendar = new Calendar(category);
			if (!calendar.createCalendar(calendar, getConnection())) {
			 return false;
			}
			category.setCalendar(calendar);
			}
			
			// Création de la balade
			Ride ride = new Ride(0, lieu, dateDepart, prix, calendar);
			boolean ok = ride.createRide(ride, getConnection());
			
			if (ok) {
			calendar.addRide(ride);
			}
			return ok;
			
			} catch (DateTimeParseException e) {
			return false;
			} catch (Exception e) {
			e.printStackTrace();
			return false;
			}
		}
    
    // ===================================================================
    // ================= JFRAME MANAGER DASHBOARD PANEL ===================
    // ===================================================================

    public List<Ride> getRidesDuManager(Manager manager) {
        Category cat = manager.getCategory();
        if (cat == null || cat.getCalendar() == null || cat.getCalendar().getRides() == null) {
            return List.of(); // liste vide immutable
        }
        return cat.getCalendar().getRides()
                  .stream()
                  .sorted(Comparator.comparing(Ride::getStartDate))
                  .toList();
    }

    public List<Ride> getRidesAvecInscriptions(Manager manager) {
        return getRidesDuManager(manager).stream()
                .filter(r -> r.getInscriptions() != null && !r.getInscriptions().isEmpty())
                .toList();
    }

    public String genererRapportOptimisationCovoiturage(Ride ride) {
        if (ride == null) return "Aucune balade sélectionnée.";
        
        OptimisationCovoiturage.ResultatOptimisation resultat = OptimisationCovoiturage.optimiser(ride);
        return OptimisationCovoiturage.genererRapport(ride, resultat);
    }
    
    /**
     * Rafraîchit complètement une sortie depuis la base
     * Utilisé par le manager avant l'optimisation
     */
    public Ride rafraichirRideDepuisBase(int rideId) {
        System.out.println("Rafraîchissement complet de la sortie ID " + rideId + " depuis la base...");

        Set<Ride> rides = Ride.allRides(conn);
        Ride ride = rides.stream()
                .filter(r -> r.getId() == rideId)
                .findFirst()
                .orElse(null);

        if (ride != null) {
            System.out.println("Sortie " + ride.getStartPlace() + " rechargée : " 
                + ride.getVehicles().size() + " véhicule(s), "
                + ride.getInscriptions().size() + " inscription(s)");
        } else {
            System.out.println("Sortie non trouvée !");
        }

        return ride;
    }
    
    // ===================================================================
    // ====================== LOGIQUE MEMBRE DASHBOARD ==================
    // ===================================================================

    public boolean ajouterCategorieAuMembre(Member member, Category nouvelleCategorie) {
        try {
            Set<Category> toutes = Category.GetAll(conn);
            Set<Integer> dejaPossedees = member.getCategories().stream()
                    .map(Category::getid)
                    .collect(Collectors.toSet());

            if (dejaPossedees.contains(nouvelleCategorie.getid())) {
                return false; // déjà possédée
            }

            member.addCategory(nouvelleCategorie);
            return member.update(conn);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Category> getCategoriesDisponiblesPourMembre(Member member) {
        try {
            Set<Category> toutes = Category.GetAll(conn);
            Set<Integer> possedees = member.getCategories().stream()
                    .map(Category::getid)
                    .collect(Collectors.toSet());

            return toutes.stream()
                    .filter(c -> !possedees.contains(c.getid()))
                    .sorted(Comparator.comparing(c -> c.getNomCategorie().name()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /** 2. Poster disponibilités (conducteur) */
    public boolean posterDisponibilites(Member conducteur, Ride ride, int placesPassagers, int placesVelos) {
        try {
            if (placesPassagers <= 0 && placesVelos <= 0) {
                throw new IllegalArgumentException("Proposez au moins une place.");
            }

            Vehicule vehicle = Vehicule.ensureVehicleExists(conducteur, conn);
            vehicle.setSeatNumber(placesPassagers + 1); // +1 pour le conducteur
            vehicle.setBikeSpotNumber(placesVelos);

            new RideAvailabilityService().postAvailability(
                    conducteur, ride, placesPassagers, placesVelos, conn);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<Ride> getRidesCompatiblesConducteur(Member membre) {
        // Récupère toutes les sorties (existantes)
        Set<Ride> allRides = Ride.allRides(conn);

        // Récupère les types de catégories pratiquées par le membre via ses vélos
        Set<TypeCat> categoriesMembre = membre.getBikes().stream()
                .map(Bike::getType)
                .collect(Collectors.toSet());

        // Si le membre n'a aucun vélo enregistré → pas de sortie compatible
        if (categoriesMembre.isEmpty()) {
            return List.of();
        }

        LocalDate aujourdHui = LocalDate.now();

        return allRides.stream()
                .filter(ride -> ride.getCalendar() != null &&
                        categoriesMembre.contains(ride.getCalendar().getCategory().getNomCategorie()))
                // NOUVEAU : on garde seulement les sorties d'aujourd'hui ou dans le futur
                .filter(ride -> !ride.getStartDate().toLocalDate().isBefore(aujourdHui))
                // Tri par date croissante (la plus proche en premier)
                .sorted(Comparator.comparing(Ride::getStartDate))
                .collect(Collectors.toList());
    }

    /** 3. Réserver une balade (passager) */
    public ReservationResult reserverBalade(Member membre, Ride ride,
            boolean veutEtrePassager, boolean veutTransporterVelo, Bike velo) {

        System.out.println("\n=== TENTATIVE DE RÉSERVATION ===");
        System.out.println("Membre : " + membre.getFirstname() + " " + membre.getName());
        System.out.println("Sortie : " + ride.getStartPlace() + " (" + ride.getStartDate().toLocalDate() + ")");
        System.out.println("Passager : " + veutEtrePassager + " | Avec vélo : " + veutTransporterVelo);

        try {
            // 1. Vérifications de base
            if (!veutEtrePassager && !veutTransporterVelo) {
                return new ReservationResult(false, "Choisissez au moins une option (passager ou vélo).");
            }
            if (veutTransporterVelo && velo == null) {
                return new ReservationResult(false, "Vous devez sélectionner un vélo compatible.");
            }

            // 2. Vérifier s'il est déjà conducteur (pour le message)
            boolean estConducteur = ride.getVehicles().stream()
                    .anyMatch(v -> v.getDriver() != null && v.getDriver().equals(membre));

            // 3. Rechercher un véhicule disponible (chez un AUTRE conducteur)
            int besoinVelos = veutTransporterVelo ? 1 : 0;
            Vehicule vehicle = ride.findAvailableVehicle(membre, veutEtrePassager, besoinVelos, conn);

            if (vehicle == null) {
                String message = estConducteur
                    ? "<html><b>Aucune place disponible chez les autres conducteurs.</b><br><br>"
                    + "Vous êtes déjà conducteur sur cette sortie.<br>"
                    + "Il n'y a plus de place libre dans les autres véhicules.</html>"
                    : "Aucune place disponible pour " +
                      (veutEtrePassager 
                          ? (veutTransporterVelo ? "passager + vélo" : "passager seul")
                          : "vélo seul") + ".";

                return new ReservationResult(false, message);
            }

            System.out.println("Véhicule trouvé → Conducteur : " + vehicle.getDriver().getFirstname() + " " + vehicle.getDriver().getName());

            // 4. Créer l'objet Inscription
            Inscription inscription = new Inscription();
            inscription.setMember(membre);
            inscription.setRide(ride);
            inscription.setPassenger(veutEtrePassager);
            inscription.setBike(veutTransporterVelo);
            inscription.setBikeObj(veutTransporterVelo ? velo : null);

            // 5. Tenter de créer l'inscription en base
            boolean inscriptionCreee = inscription.create(conn);

            if (!inscriptionCreee) {
                // LE DAO A REFUSÉ → c'est un doublon (ou erreur grave)
                return new ReservationResult(false,
                    "<html><center><h3>Vous êtes déjà inscrit à cette sortie</h3></center><br>" +
                    "<b>Sortie :</b> " + ride.getStartPlace() + "<br>" +
                    "<b>Date :</b> " + ride.getStartDate().toLocalDate() + "<br><br>" +
                    "Vous ne pouvez pas vous inscrire plusieurs fois à la même balade,<br>" +
                    "même avec des options différentes (avec ou sans vélo).<br><br>" +
                    "<i>Pour modifier votre inscription,<br>contactez votre conducteur ou le manager.</i></html>");
            }

            // 6. Inscription acceptée → on met à jour les objets en mémoire
            ride.addInscription(inscription);
            membre.addInscription(inscription);
            if (veutEtrePassager) vehicle.addPassenger(membre);
            if (veutTransporterVelo && velo != null) vehicle.addBike(velo);

            // 7. Débit du forfait
            double nouveauSolde = Math.round((membre.getBalance() - ride.getFee()) * 100.0) / 100.0;
            membre.setBalance(nouveauSolde);

            // 8. Sauvegarde finale
            boolean sauvegardeOk = membre.update(conn) && vehicle.update(conn);

            if (!sauvegardeOk) {
                // Très rare, mais on gère quand même
                return new ReservationResult(false, "Réservation enregistrée mais échec de mise à jour du solde/véhicule.");
            }

            // 9. Mise à jour du véhicule dans la liste de la ride (pour cohérence mémoire)
            ride.getVehicles().removeIf(v -> v.getId() == vehicle.getId());
            ride.getVehicles().add(vehicle);

            System.out.println("RÉSERVATION RÉUSSIE ! Solde → " + nouveauSolde + " €");

            return new ReservationResult(true, "Réservation confirmée !", nouveauSolde, vehicle.getDriver());

        } catch (Exception e) {
            System.err.println("ERREUR inattendue lors de la réservation : " + e.getMessage());
            e.printStackTrace();
            return new ReservationResult(false, "Erreur inattendue : " + e.getMessage());
        }
    }

    public List<Ride> getRidesFutures() {
        return Ride.allRides(conn).stream()
                .filter(r -> r.getStartDate() != null && r.getStartDate().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(Ride::getStartDate))
                .collect(Collectors.toList());
    }

    /** 4. Paiement cotisation */
    public double calculerCotisationDue(Member membre) {
        Set<TypeCat> typesUniques = membre.getBikes().stream()
                .map(Bike::getType)
                .collect(Collectors.toSet());
        int nbCategories = typesUniques.size();
        double supplement = 5.0 * Math.max(0, nbCategories);
        return 20.0 + supplement;
    }

    public boolean cotisationEstPayee(Member membre) {
        return cotisationsPayees.getOrDefault(membre.getId(), false);
    }
    
    public boolean payerCotisation(Member membre, double montantPaye) {

        if (cotisationEstPayee(membre)) {
            return false; // déjà payé
        }

        double due = calculerCotisationDue(membre);

        if (montantPaye < due) {
            return false; // montant insuffisant
        }

        // débiter le solde
        double nouveauSolde =
                Math.round((membre.getBalance() - montantPaye) * 100.0) / 100.0;
        membre.setBalance(nouveauSolde);
        membre.update(conn);

        // marquer comme payée
        cotisationsPayees.put(membre.getId(), true);

        return true;
    }



    /** 5. Ajouter fonds */
    public boolean ajouterFonds(Member membre, double montant) {
        if (montant <= 0) return false;
        double nouveauSolde = Math.round((membre.getBalance() + montant) * 100.0) / 100.0;
        membre.setBalance(nouveauSolde);
        return membre.update(conn);
    }

    /** Classe utilitaire pour retour de réservation */
    public static class ReservationResult
    {
        public final boolean succes;
        public final String message;
        public final double nouveauSolde;
        public final Member conducteur;

        public ReservationResult(boolean succes, String message) 
        {
            this(succes, message, 0.0, null);
        }

        public ReservationResult(boolean succes, String message, double nouveauSolde, Member conducteur) 
        {
            this.succes = succes;
            this.message = message;
            this.nouveauSolde = nouveauSolde;
            this.conducteur = conducteur;
        }
    }
    // Concerant le Bike du membre, se référer à BikeDialog.java
    public boolean supprimerVeloDeMembre(Member membre, Bike bike) {
        try {
            // Optionnel : vérifier que le vélo appartient bien au membre
            if (!membre.getBikes().contains(bike)) {
                return false;
            }

            boolean deleted = bike.delete(conn);
            if (deleted) {
                membre.getBikes().remove(bike);
                // Optionnel : nettoyer les inscriptions qui utilisaient ce vélo ?
                // (tu peux ajouter ça plus tard si besoin)
            }
            return deleted;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    // ===================================================================
    // ====================== LOGIQUE ReservationDialog ==================
    // ===================================================================
    
    public List<Ride> getRidesCompatiblesPourMembre(Member membre) 
    {
        if (membre == null || membre.getBikes().isEmpty()) {
            return List.of();
        }

        Set<TypeCat> typesVeloMembre = membre.getBikes().stream()
                .map(Bike::getType)
                .collect(Collectors.toSet());

        return getRidesFutures().stream()
                .filter(ride -> {
                    Category cat = ride.getCalendar().getCategory();
                    return cat != null && typesVeloMembre.contains(cat.getNomCategorie());
                })
                .sorted(Comparator.comparing(Ride::getStartDate))
                .toList();
    }
    /** Retourne les vélos du membre compatibles avec une sortie donnée */
    public List<Bike> getVelosCompatiblesPourRide(Member membre, Ride ride) {
        if (membre == null || ride == null || ride.getCalendar() == null || ride.getCalendar().getCategory() == null) {
            return List.of();
        }

        TypeCat typeRequis = ride.getCalendar().getCategory().getNomCategorie();

        return membre.getBikes().stream()
                .filter(bike -> bike.getType() == typeRequis)
                .sorted(Comparator.comparing(Bike::getWeight))
                .toList();
    }
    public String getLibelleCategorie(TypeCat type) {
        return switch (type) {
            case RoadBike -> "Vélo de route";
            case Cross    -> "Cyclo-cross";
            case Downhill -> "VTT Descente";
            case Trial    -> "Trial";
        };
    }
    // ===================================================================
    // ====================== ReservationDialog ==========================
    // ===================================================================

    /**
     * Version "propre" de la réservation : toute la logique + messages d'erreur riches
     */
    public ReservationResult reserverBaladeAvecVerificationComplete(Member membre,
        Ride ride,
        boolean veutEtrePassager,
        boolean veutTransporterVelo,
        Bike veloChoisi) {

        // 1. Vérifications simples
        if (!veutEtrePassager && !veutTransporterVelo) {
            return new ReservationResult(false, "Veuillez choisir au moins une option (passager ou vélo).");
        }
        if (veutTransporterVelo && veloChoisi == null) {
            return new ReservationResult(false, "Vous devez sélectionner un vélo compatible.");
        }

        // 2. Charger les véhicules pour être sûr d'avoir les données à jour
        ride.loadVehicles(conn);

        // 3. Empêcher un conducteur de réserver comme passager
        if (ride.estConducteur(membre)) {
            return new ReservationResult(false,
                    "<html>Vous êtes déjà conducteur sur cette sortie.</b><br><br>" +
                    "Vous ne pouvez pas réserver une place passager/vélo en plus.");
        }

        // 4. Déléguer à la méthode existante (qui contient toute la logique métier)
        return reserverBalade(membre, ride, veutEtrePassager, veutTransporterVelo, veloChoisi);
    }

    /**
     * Retourne vrai si le membre est conducteur sur au moins un véhicule de la ride
     */
    public boolean estDejaConducteurSurRide(Member membre, Ride ride) {
        if (ride.getVehicles() == null) return false;
        return ride.getVehicles().stream()
                .anyMatch(v -> v.getDriver() != null && v.getDriver().equals(membre));
    }
   
    // dans WelcomePanel.java on recupère les rides via Ride.allRides(conn) et 
    // on les filtres ici
 // AppModel.java
    public Map<TypeCat, List<Object[]>> getPublicUpcomingRidesForTable() {
        
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy 'à' HH:mm", Locale.FRENCH);

        return Ride.allRides(getConnection()).stream()
                .filter(r -> {
                    if (r.getStartDate() == null) return false;
                    if (!r.getStartDate().isAfter(LocalDateTime.now())) return false;
                    if (r.getCalendar() == null) {
                        System.out.println("Ride sans calendar : " + r.getStartPlace());
                        return false;
                    }
                    if (r.getCalendar().getCategory() == null) {
                        System.out.println("Ride sans catégorie : " + r.getStartPlace());
                        return false;
                    }
                    if (r.getCalendar().getCategory().getNomCategorie() == null) {
                        System.out.println("Ride avec catégorie null : " + r.getStartPlace());
                        return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(Ride::getStartDate))
                .collect(Collectors.groupingBy(
                    r -> r.getCalendar().getCategory().getNomCategorie(),  // ← 100% correct maintenant
                    LinkedHashMap::new,
                    Collectors.mapping(r -> new Object[]{
                        r.getStartDate().format(fmt),
                        r.getStartPlace(),
                        String.format("%.2f €", r.getFee()),
                        r.getInscriptions().size() + " membre(s)"
                    }, Collectors.toList())
                ));
    }
    
    // ====================== Recuperé le Vehicle du member  ======================

    public Vehicule getVehicleOfMember(Member member) {
        try {
            return Vehicule.getOrCreateForDriver(member, getConnection());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
}
