package be.ouagueni.dao;

import java.sql.Connection;
import be.ouagueni.connection.ClubConnection;
import be.ouagueni.model.Bike;
import be.ouagueni.model.Calendar;
import be.ouagueni.model.Category;
import be.ouagueni.model.Inscription;
import be.ouagueni.model.Member;
import be.ouagueni.model.Ride;
import be.ouagueni.model.Vehicule;

public class DAOFactory extends AbstractDAOFactory {
    protected static final Connection conn = ClubConnection.getInstance();

    @Override
    public DAO<Member> getMemberDAO() { 
        return new MemberDAO(conn); 
    }

    @Override
    public DAO<Bike> getBikeDAO() { 
        return new BikeDAO(conn); 
    }

    @Override
    public DAO<Vehicule> getVehicleDAO() { 
        return new VehiculeDAO(conn); 
    }

    @Override
    public DAO<Inscription> getInscriptionDAO() { 
        return new InscriptionDAO(conn); 
    }

    @Override
    public DAO<Ride> getRideDAO() { 
        return new RideDAO(conn); 
    }

    @Override
    public DAO<Category> getCategoryDAO() { 
        return new CategoryDAO(conn); 
    }

    @Override
    public DAO<Calendar> getCalendarDAO() { 
        return new CalendarDAO(conn); 
    }
}