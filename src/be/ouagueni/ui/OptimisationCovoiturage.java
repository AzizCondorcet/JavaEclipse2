package be.ouagueni.ui;

import be.ouagueni.model.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service d'optimisation du covoiturage pour les balades du club
 * Objectif: Minimiser le nombre de véhicules nécessaires et équilibrer la charge
 */
public class OptimisationCovoiturage {

    /**
     * Résultat de l'optimisation
     */
    public static class ResultatOptimisation {
        private Map<Vehicle, List<Member>> affectationPassagers;
        private Map<Vehicle, List<Bike>> affectationVelos;
        private List<Member> passagersNonAffectes;
        private List<Bike> velosNonAffectes;
        private int nombreVehiculesUtilises;
        private boolean optimisationReussie;
        private String messageResultat;

        public ResultatOptimisation() {
            this.affectationPassagers = new HashMap<>();
            this.affectationVelos = new HashMap<>();
            this.passagersNonAffectes = new ArrayList<>();
            this.velosNonAffectes = new ArrayList<>();
        }

        // Getters et setters
        public Map<Vehicle, List<Member>> getAffectationPassagers() { return affectationPassagers; }
        public Map<Vehicle, List<Bike>> getAffectationVelos() { return affectationVelos; }
        public List<Member> getPassagersNonAffectes() { return passagersNonAffectes; }
        public List<Bike> getVelosNonAffectes() { return velosNonAffectes; }
        public int getNombreVehiculesUtilises() { return nombreVehiculesUtilises; }
        public boolean isOptimisationReussie() { return optimisationReussie; }
        public String getMessageResultat() { return messageResultat; }

        public void setNombreVehiculesUtilises(int nb) { this.nombreVehiculesUtilises = nb; }
        public void setOptimisationReussie(boolean b) { this.optimisationReussie = b; }
        public void setMessageResultat(String msg) { this.messageResultat = msg; }
    }

    /**
     * Optimise l'affectation des passagers et vélos aux véhicules disponibles
     * Stratégie: First-Fit Decreasing (FFD) - On remplit d'abord les plus grands véhicules
     */
    public static ResultatOptimisation optimiser(Ride ride) {
        ResultatOptimisation resultat = new ResultatOptimisation();

        // 1. Récupérer les listes nécessaires
        List<Member> passagers = ride.getInscriptions().stream()
                .filter(Inscription::isPassenger)
                .map(Inscription::getMember)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Bike> velosATransporter = ride.getInscriptions().stream()
                .filter(Inscription::isBike)
                .map(Inscription::getBikeObj)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 2. Trier les véhicules par capacité décroissante (les plus grands d'abord)
        List<Vehicle> vehiculesDispos = new ArrayList<>(ride.getVehicles());
        vehiculesDispos.sort((v1, v2) -> {
            int cap1 = v1.getSeatNumber() + v1.getBikeSpotNumber();
            int cap2 = v2.getSeatNumber() + v2.getBikeSpotNumber();
            return Integer.compare(cap2, cap1);
        });

        // 3. Réinitialiser toutes les affectations actuelles
        for (Vehicle v : vehiculesDispos) {
            v.getPassengers().clear();
            v.getBikes().clear();
            resultat.getAffectationPassagers().put(v, new ArrayList<>());
            resultat.getAffectationVelos().put(v, new ArrayList<>());
        }

        // 4. Affecter les passagers (en excluant les conducteurs)
        for (Member passager : passagers) {
            boolean affecte = false;
            
            // Ne pas affecter un conducteur comme passager
            boolean estConducteur = vehiculesDispos.stream()
                    .anyMatch(v -> v.getDriver() != null && v.getDriver().equals(passager));
            
            if (estConducteur) continue;

            for (Vehicle v : vehiculesDispos) {
                if (v.getDriver() == null) continue;
                
                int placesDispos = v.getSeatNumber() - resultat.getAffectationPassagers().get(v).size();
                
                if (placesDispos > 0) {
                    resultat.getAffectationPassagers().get(v).add(passager);
                    v.getPassengers().add(passager);
                    affecte = true;
                    break;
                }
            }

            if (!affecte) {
                resultat.getPassagersNonAffectes().add(passager);
            }
        }

        // 5. Affecter les vélos
        for (Bike velo : velosATransporter) {
            boolean affecte = false;

            for (Vehicle v : vehiculesDispos) {
                if (v.getDriver() == null) continue;
                
                int placesVeloDispos = v.getBikeSpotNumber() - resultat.getAffectationVelos().get(v).size();
                
                if (placesVeloDispos > 0) {
                    resultat.getAffectationVelos().get(v).add(velo);
                    v.getBikes().add(velo);
                    affecte = true;
                    break;
                }
            }

            if (!affecte) {
                resultat.getVelosNonAffectes().add(velo);
            }
        }

        // 6. Compter les véhicules réellement utilisés
        int vehiculesUtilises = 0;
        for (Vehicle v : vehiculesDispos) {
            if (v.getDriver() != null && 
                (!resultat.getAffectationPassagers().get(v).isEmpty() || 
                 !resultat.getAffectationVelos().get(v).isEmpty())) {
                vehiculesUtilises++;
            }
        }
        resultat.setNombreVehiculesUtilises(vehiculesUtilises);

        // 7. Déterminer si l'optimisation est réussie
        boolean reussite = resultat.getPassagersNonAffectes().isEmpty() && 
                          resultat.getVelosNonAffectes().isEmpty();
        resultat.setOptimisationReussie(reussite);

        // 8. Générer un message de résultat
        StringBuilder msg = new StringBuilder();
        msg.append(String.format("Optimisation terminée : %d véhicule(s) utilisé(s)\n", vehiculesUtilises));
        msg.append(String.format("✓ %d passager(s) affecté(s)\n", passagers.size() - resultat.getPassagersNonAffectes().size()));
        msg.append(String.format("✓ %d vélo(s) affecté(s)\n", velosATransporter.size() - resultat.getVelosNonAffectes().size()));
        
        if (!resultat.getPassagersNonAffectes().isEmpty()) {
            msg.append(String.format("⚠ %d passager(s) non affecté(s) - Véhicules insuffisants!\n", 
                    resultat.getPassagersNonAffectes().size()));
        }
        if (!resultat.getVelosNonAffectes().isEmpty()) {
            msg.append(String.format("⚠ %d vélo(s) non affecté(s) - Places vélos insuffisantes!\n", 
                    resultat.getVelosNonAffectes().size()));
        }

        resultat.setMessageResultat(msg.toString());

        return resultat;
    }

    public static List<String> suggererAmeliorations(Ride ride, ResultatOptimisation resultat) {
        List<String> suggestions = new ArrayList<>();

        if (!resultat.isOptimisationReussie()) {
            if (!resultat.getPassagersNonAffectes().isEmpty()) {
                int manque = resultat.getPassagersNonAffectes().size();
                suggestions.add(String.format("Il manque au moins %d place(s) passager. Contactez des membres supplémentaires pour qu'ils deviennent conducteurs.", manque));
            }
            
            if (!resultat.getVelosNonAffectes().isEmpty()) {
                int manque = resultat.getVelosNonAffectes().size();
                suggestions.add(String.format("Il manque %d place(s) vélo. Cherchez des véhicules avec porte-vélos ou remorque.", manque));
            }
        }

        // Vérifier l'équilibre de charge
        Map<Vehicle, Integer> charges = new HashMap<>();
        for (Vehicle v : ride.getVehicles()) {
            if (v.getDriver() != null) {
                int charge = v.getPassengers().size() + v.getBikes().size();
                int capacite = v.getSeatNumber() + v.getBikeSpotNumber();
                charges.put(v, charge * 100 / Math.max(1, capacite));
            }
        }

        long vehiculesSousUtilises = charges.values().stream().filter(c -> c < 50).count();
        if (vehiculesSousUtilises > 0 && resultat.getNombreVehiculesUtilises() > 1) {
            suggestions.add(String.format("%d véhicule(s) sont sous-utilisés. Envisagez de réduire le nombre de conducteurs pour économiser.", vehiculesSousUtilises));
        }

        return suggestions;
    }

    /**
     * Affiche un rapport détaillé de l'optimisation
     */
    public static String genererRapport(Ride ride, ResultatOptimisation resultat) {
        StringBuilder rapport = new StringBuilder();
        rapport.append("═══════════════════════════════════════════════════\n");
        rapport.append("   RAPPORT D'OPTIMISATION DU COVOITURAGE\n");
        rapport.append("═══════════════════════════════════════════════════\n\n");

        rapport.append(resultat.getMessageResultat()).append("\n");

        rapport.append("Détail par véhicule :\n");
        rapport.append("───────────────────────────────────────────────────\n");

        for (Vehicle v : ride.getVehicles()) {
            if (v.getDriver() == null) continue;

            List<Member> passagers = resultat.getAffectationPassagers().get(v);
            List<Bike> velos = resultat.getAffectationVelos().get(v);

            if (passagers == null || velos == null) continue;

            rapport.append(String.format("🚗 Conducteur: %s\n", v.getDriver().getFirstname()));
            rapport.append(String.format("   Passagers (%d/%d): ", passagers.size(), v.getSeatNumber()));
            
            if (passagers.isEmpty()) {
                rapport.append("Aucun\n");
            } else {
                rapport.append(passagers.stream()
                        .map(Member::getFirstname)
                        .collect(Collectors.joining(", ")))
                        .append("\n");
            }

            rapport.append(String.format("   Vélos (%d/%d): ", velos.size(), v.getBikeSpotNumber()));
            if (velos.isEmpty()) {
                rapport.append("Aucun\n");
            } else {
                rapport.append(velos.stream()
                        .map(b -> String.format("#%d", b.getId()))
                        .collect(Collectors.joining(", ")))
                        .append("\n");
            }
            rapport.append("\n");
        }

        List<String> suggestions = suggererAmeliorations(ride, resultat);
        if (!suggestions.isEmpty()) {
            rapport.append("Suggestions d'amélioration :\n");
            rapport.append("───────────────────────────────────────────────────\n");
            for (String suggestion : suggestions) {
                rapport.append("💡 ").append(suggestion).append("\n");
            }
        }

        return rapport.toString();
    }
}