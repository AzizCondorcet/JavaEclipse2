package be.ouagueni.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import be.ouagueni.connection.ClubConnection;
import be.ouagueni.ui.OptimisationCovoiturage;

public class AppModel {

    private static AppModel instance;
    private final Connection conn;

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

            Vehicle vehicle = Vehicle.getOrCreateForDriver(conducteur, conn);
            vehicle.setSeatNumber(placesPassagers);
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
        Set<Ride> allRides = Ride.allRides(conn);
        Set<TypeCat> categoriesMembre = membre.getBikes().stream()
                .map(Bike::getType)
                .collect(Collectors.toSet());

        if (categoriesMembre.isEmpty()) return List.of();

        return allRides.stream()
                .filter(ride -> ride.getCalendar() != null &&
                        categoriesMembre.contains(ride.getCalendar().getCategory().getNomCategorie()))
                .sorted(Comparator.comparing(Ride::getStartDate))
                .collect(Collectors.toList());
    }

    /** 3. Réserver une balade (passager) */
    public ReservationResult reserverBalade(Member membre, Ride ride,
                                             boolean veutEtrePassager, boolean veutTransporterVelo, Bike velo) {
        try {
            // Vérifications
            if (!veutEtrePassager && !veutTransporterVelo) {
                return new ReservationResult(false, "Choisissez au moins une option.");
            }

            if (veutTransporterVelo && velo == null) {
                return new ReservationResult(false, "Vélo requis mais non sélectionné.");
            }

            // Déjà inscrit ?
            boolean dejaInscrit = ride.getInscriptions().stream()
                    .anyMatch(ins -> ins.getMember() != null && ins.getMember().equals(membre));
            if (dejaInscrit) {
                return new ReservationResult(false, "Vous êtes déjà inscrit à cette sortie.");
            }

            // Trouver un véhicule disponible
            int besoinVelos = veutTransporterVelo ? 1 : 0;
            Vehicle vehicle = ride.findAvailableVehicle(veutEtrePassager, besoinVelos, conn);
            if (vehicle == null) {
                String besoin = veutEtrePassager
                        ? (veutTransporterVelo ? "passager + vélo" : "passager")
                        : "vélo";
                return new ReservationResult(false, "Aucune place disponible pour " + besoin + ".");
            }

            // Créer l'inscription
            Inscription inscription = new Inscription();
            inscription.setMember(membre);
            inscription.setRide(ride);
            inscription.setPassenger(veutEtrePassager);
            inscription.setBike(veutTransporterVelo);
            inscription.setBikeObj(veutTransporterVelo ? velo : null);

            ride.addInscription(inscription);
            membre.addInscription(inscription);
            if (veutEtrePassager) vehicle.addPassenger(membre);
            if (veutTransporterVelo && velo != null) vehicle.addBike(velo);

            double nouveauSolde = Math.round((membre.getBalance() - ride.getFee()) * 100.0) / 100.0;
            membre.setBalance(nouveauSolde);

            boolean succes = inscription.create(conn) && membre.update(conn) && vehicle.update(conn);

            if (succes) {
                return new ReservationResult(true, "Réservation confirmée !", nouveauSolde, vehicle.getDriver());
            } else {
                return new ReservationResult(false, "Échec de l'enregistrement en base.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return new ReservationResult(false, "Erreur base de données : " + e.getMessage());
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

    public boolean payerCotisation(Member membre, double montantPaye) {
        double due = calculerCotisationDue(membre);
        if (montantPaye <= 0) return false;

        double nouveauSolde = Math.round((membre.getBalance() - montantPaye) * 100.0) / 100.0;
        membre.setBalance(nouveauSolde);
        return membre.update(conn);
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
}
