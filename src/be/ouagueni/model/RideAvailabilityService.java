package be.ouagueni.model;

import java.sql.Connection;
import java.sql.SQLException;

public class RideAvailabilityService {

    public void postAvailability(Member member, Ride ride, int seatNumber, int bikeSpotNumber, Connection conn) 
            throws SQLException, IllegalStateException {

        // 1. Vérification dette → OK, on utilise les méthodes autorisées du Member
        if (member.getBalance() < 0) {
            throw new IllegalStateException("Vous avez une dette de " + String.format("%.2f €", -member.getBalance()));
        }

        // 2. Vérification déjà conducteur sur cette ride
        boolean alreadyDriver = ride.getVehicles().stream()
            .anyMatch(v -> v.getDriver() != null && v.getDriver().equals(member));
        if (alreadyDriver) {
            throw new IllegalStateException("Vous avez déjà posté votre voiture pour cette sortie.");
        }

        // 3. Au moins une place
        if (seatNumber <= 0 && bikeSpotNumber <= 0) {
            throw new IllegalArgumentException("Proposez au moins une place.");
        }

        // 4. Récupération/création du véhicule (Vehicle est responsable de ça → OK)
        Vehicule vehicle = Vehicule.getOrCreateForDriver(member, conn);

        // 5. Mise à jour des capacités
        vehicle.setSeatNumber(seatNumber);
        vehicle.setBikeSpotNumber(bikeSpotNumber);

        // 6. Association bidirectionnelle
        vehicle.addRide(ride);
        ride.addVehicle(vehicle);

        // 7. Persistance → déléguée à Vehicle (conforme au diagramme)
        if (!vehicle.save(conn)) {
            throw new SQLException("Échec de la sauvegarde du véhicule");
        }
    }
}