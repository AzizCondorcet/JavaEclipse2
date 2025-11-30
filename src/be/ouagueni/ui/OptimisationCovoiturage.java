package be.ouagueni.ui;

import be.ouagueni.model.*;
import java.util.*;
import java.util.stream.Collectors;

public class OptimisationCovoiturage {

    public static class ResultatOptimisation {
        private final Map<Vehicule, List<Member>> affectationPassagers = new HashMap<>();
        private final Map<Vehicule, List<Bike>> affectationVelos = new HashMap<>();
        private final List<Member> passagersNonAffectes = new ArrayList<>();
        private final List<Bike> velosNonAffectes = new ArrayList<>();
        private int nombreVehiculesUtilises;
        private boolean optimisationReussie;
        private String messageResultat;

        // Getters
        public Map<Vehicule, List<Member>> getAffectationPassagers() { return affectationPassagers; }
        public Map<Vehicule, List<Bike>> getAffectationVelos() { return affectationVelos; }
        public List<Member> getPassagersNonAffectes() { return passagersNonAffectes; }
        public List<Bike> getVelosNonAffectes() { return velosNonAffectes; }
        public int getNombreVehiculesUtilises() { return nombreVehiculesUtilises; }
        public boolean isOptimisationReussie() { return optimisationReussie; }
        public String getMessageResultat() { return messageResultat; }

        // Setters
        public void setNombreVehiculesUtilises(int nb) { this.nombreVehiculesUtilises = nb; }
        public void setOptimisationReussie(boolean b) { this.optimisationReussie = b; }
        public void setMessageResultat(String msg) { this.messageResultat = msg; }
    }

    /**
     * Optimisation First-Fit Decreasing (tri par capacité totale décroissante)
     */
    public static ResultatOptimisation optimiser(Ride ride) {
        ResultatOptimisation resultat = new ResultatOptimisation();

        List<Vehicule> vehicules = ride.getVehicles().stream()
                .filter(v -> v.getDriver() != null)
                .sorted((v1, v2) -> Integer.compare(
                        v2.getSeatNumber() + v2.getBikeSpotNumber(),
                        v1.getSeatNumber() + v1.getBikeSpotNumber()))
                .toList();

        for (Vehicule v : vehicules) {
            resultat.getAffectationPassagers().put(v, new ArrayList<>());
            resultat.getAffectationVelos().put(v, new ArrayList<>());
            v.getPassengers().clear();
            v.getBikes().clear();
        }

        // 2. Passagers à placer
        List<Member> passagers = ride.getInscriptions().stream()
                .filter(Inscription::isPassenger)
                .map(Inscription::getMember)
                .filter(Objects::nonNull)
                .filter(m -> vehicules.stream().noneMatch(v -> v.getDriver().equals(m))) // pas conducteur
                .toList();

        // Placement des passagers 
        for (Member p : passagers) {
            boolean placeTrouvee = false;
            for (Vehicule v : vehicules) {
                if (v.getDriver().equals(p)) continue;
                if (v.getPassengers().size() < v.getSeatNumber() - 1) { // -1 = conducteur déjà dedans
                    v.getPassengers().add(p);
                    resultat.getAffectationPassagers().get(v).add(p);
                    placeTrouvee = true;
                    break;
                }
            }
            if (!placeTrouvee) {
                resultat.getPassagersNonAffectes().add(p);
            }
        }

        // 3. Vélos à placer
        List<Bike> velos = ride.getInscriptions().stream()
                .filter(Inscription::isBike)
                .map(Inscription::getBikeObj)
                .filter(Objects::nonNull)
                .toList();

        for (Bike velo : velos) {
            boolean placeTrouvee = false;
            for (Vehicule v : vehicules) {
                if (v.getBikes().size() < v.getBikeSpotNumber()) {
                    v.getBikes().add(velo);
                    resultat.getAffectationVelos().get(v).add(velo);
                    placeTrouvee = true;
                    break;
                }
            }
            if (!placeTrouvee) {
                resultat.getVelosNonAffectes().add(velo);
            }
        }

        // 4. Statistiques finales
        int vehiculesUtilises = (int) vehicules.stream()
                .filter(v -> !v.getPassengers().isEmpty() || !v.getBikes().isEmpty())
                .count();
        resultat.setNombreVehiculesUtilises(vehiculesUtilises);

        int passagersPlaces = passagers.size() - resultat.getPassagersNonAffectes().size();
        int velosPlaces = velos.size() - resultat.getVelosNonAffectes().size();

        boolean succes = resultat.getPassagersNonAffectes().isEmpty() && resultat.getVelosNonAffectes().isEmpty();
        resultat.setOptimisationReussie(succes);

        String msg = String.format(
            "Optimisation terminée : %d véhicule(s) utilisé(s)\n" +
            "Passagers : %d/%d placés\n" +
            "Vélos     : %d/%d placés",
            vehiculesUtilises,
            passagersPlaces, passagers.size(),
            velosPlaces, velos.size()
        );
        resultat.setMessageResultat(msg);

        return resultat;
    }


    public static List<String> suggererAmeliorations(Ride ride, ResultatOptimisation resultat) {
        List<String> suggestions = new ArrayList<>();

        int passagersManquants = resultat.getPassagersNonAffectes().size();
        int velosManquants = resultat.getVelosNonAffectes().size();

        if (passagersManquants > 0) {
            suggestions.add(String.format("Il manque %d place(s) passager → recrutez des conducteurs !", passagersManquants));
        }
        if (velosManquants > 0) {
            suggestions.add(String.format("Il manque %d place(s) vélo → cherchez des porte-vélos !", velosManquants));
        }

        // Véhicules très peu remplis
        long vehiculesSousUtilises = ride.getVehicles().stream()
                .filter(v -> v.getDriver() != null)
                .filter(v -> {
                    int placesUtilisees = v.getPassengers().size() + v.getBikes().size();
                    int capaciteReelle = (v.getSeatNumber() - 1) + v.getBikeSpotNumber(); // conducteur prend 1 place
                    return capaciteReelle >= 3 && placesUtilisees <= 1; // ex: 5 places → max 1 passager/vélo
                })
                .count();

        if (vehiculesSousUtilises > 1) {
            suggestions.add(String.format("%d véhicule(s) sont très peu remplis → essayez de regrouper les participants.", vehiculesSousUtilises));
        }

        return suggestions;
    }

    public static String genererRapport(Ride ride, ResultatOptimisation resultat) {
        StringBuilder r = new StringBuilder();
        r.append("═══════════════════════════════════════════════════\n");
        r.append("   RAPPORT D'OPTIMISATION COVOITURAGE\n");
        r.append("═══════════════════════════════════════════════════\n\n");
        r.append("Balade : ").append(ride.getStartPlace())
         .append(" - ").append(ride.getStartDate().toLocalDate()).append("\n\n");

        r.append(resultat.getMessageResultat()).append("\n\n");
        r.append("Détail par véhicule :\n");
        r.append("───────────────────────────────────────────────────\n");

        List<Vehicule> vehiculesTries = ride.getVehicles().stream()
                .filter(v -> v.getDriver() != null)
                .sorted(Comparator.comparingInt(v -> - (v.getSeatNumber() + v.getBikeSpotNumber())))
                .toList();

        for (Vehicule v : vehiculesTries) {
            List<Member> passagers = resultat.getAffectationPassagers().getOrDefault(v, List.of());
            List<Bike> velos = resultat.getAffectationVelos().getOrDefault(v, List.of());

            r.append(String.format("Conducteur : %s %s\n",
                    v.getDriver().getFirstname(), v.getDriver().getName().toUpperCase()));
            r.append(String.format("   Passagers (%d/%d) : %s\n",
                    passagers.size(), v.getSeatNumber() - 1,
                    passagers.isEmpty() ? "Aucun" : passagers.stream()
                            .map(m -> m.getFirstname())
                            .collect(Collectors.joining(", "))));
            r.append(String.format("   Vélos (%d/%d) : %s\n\n",
                    velos.size(), v.getBikeSpotNumber(),
                    velos.isEmpty() ? "Aucun" : velos.stream()
                            .map(b -> "#" + b.getId())
                            .collect(Collectors.joining(", "))));
        }

        var suggestions = suggererAmeliorations(ride, resultat);
        if (!suggestions.isEmpty()) {
            r.append("Suggestions :\n");
            r.append("───────────────────────────────────────────────────\n");
            suggestions.forEach(s -> r.append(" • ").append(s).append("\n"));
        } else {
            r.append("Aucune suggestion — tout est optimisé !\n");
        }

        return r.toString();
    }
}