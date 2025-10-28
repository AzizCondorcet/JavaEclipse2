package be.ouagueni.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Calendar implements Serializable {

	private static final long serialVersionUID = -2079458858083609116L;
	private int id;
    private Set<Ride> rides = new HashSet<>();
    private Category category;

    public Calendar(	int id,Category category) { this.category = category; 
    this.id = id;}

    
    public double getid() { return id; }
    public void setid(int id) { this.id = id; }
    public Set<Ride> getRides() { return rides; }
    public void setRides(Set<Ride> rides) { this.rides = rides; }
    public void addRide(Ride r) { if (r != null) rides.add(r); }
    public void removeRide(Ride r) { rides.remove(r); }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
}

