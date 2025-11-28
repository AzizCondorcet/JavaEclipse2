package be.ouagueni.dao;

import be.ouagueni.model.Bike;
import be.ouagueni.model.Calendar;
import be.ouagueni.model.Category;
import be.ouagueni.model.Inscription;
import be.ouagueni.model.Member;
import be.ouagueni.model.Ride;
import be.ouagueni.model.Vehicule;

public abstract class AbstractDAOFactory {
    public static final int DAO_FACTORY = 0;
    public static final int XML_DAO_FACTORY = 1; 

    // Méthodes abstraites pour obtenir les DAO
    public abstract DAO<Member> getMemberDAO();
    public abstract DAO<Bike> getBikeDAO();
    public abstract DAO<Vehicule> getVehicleDAO();
    public abstract DAO<Inscription> getInscriptionDAO();
    public abstract DAO<Ride> getRideDAO();
    public abstract DAO<Category> getCategoryDAO();
    public abstract DAO<Calendar> getCalendarDAO();

    // Méthode factory statique
    public static AbstractDAOFactory getFactory(int type) {
        switch (type) {
            case DAO_FACTORY:
                return new DAOFactory();
            default:
                return null;
        }
    }
}