package be.ouagueni.model;

import java.io.Serializable;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import be.ouagueni.dao.MemberDAO;

public class Member extends Person implements Serializable {

	private static final long serialVersionUID = -25458080844517046L;
	private double balance;
	private int idMember;
    private Set<Inscription> inscriptions = new HashSet<>();
    private Set<Category> categories = new HashSet<>(); 
    private Set<Bike> bikes = new HashSet<>(); 
    private Vehicule driver;
    private Set<Vehicule> passengers = new HashSet<>(); 

    public Member() { super(); }
    public Member(String name, String firstname) {
        super(0, name, null, null, firstname);
    }
    public Member(int id, String name, String firstname, String tel, String password,
            Category category, Bike bike) {
	  super(id, name, firstname, tel, password);
	  if (category == null || bike == null) {
	      throw new IllegalArgumentException("Un membre doit avoir au moins 1 catégorie et 1 vélo");
	  }
	  this.categories.add(category);
	  this.bikes.add(bike);
    }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
	public int getIdMember() { return idMember; }
	public void setIdMember(int idMember) { this.idMember = idMember; }
    public Set<Inscription> getInscriptions() { return inscriptions; }
    public void setInscriptions(Set<Inscription> inscriptions) { this.inscriptions = inscriptions; }
    public void addInscription(Inscription ins) { if (ins != null) this.inscriptions.add(ins); }
    public void removeInscription(Inscription ins) { this.inscriptions.remove(ins); }
    public Set<Category> getCategories() { return categories; }
    
    public void setCategories(Set<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("Un membre doit avoir au moins 1 catégorie");}this.categories = categories; }
    
    public void addCategory(Category category) { 
        if (category != null) {this.categories.add(category);}}
    
    public void removeCategory(Category category) {
        if (this.categories.size() <= 1) {
            throw new IllegalStateException("Un membre doit avoir au moins 1 catégorie");}this.categories.remove(category);}
    
    public Set<Bike> getBikes() { 
        return bikes; 
    }
    
    public void setBikes(Set<Bike> bikes) {
        if (bikes == null || bikes.isEmpty()) {
            throw new IllegalArgumentException("Un membre doit avoir au moins 1 vélo");}this.bikes = bikes;}
    
    public void addBike(Bike bike) { 
        if (bike != null) {this.bikes.add(bike);}}
    
    public void removeBike(Bike bike) {
        if (this.bikes.size() <= 1) {
            throw new IllegalStateException("Un membre doit avoir au moins 1 vélo");}this.bikes.remove(bike); }
    
    public Vehicule getDriver() {return driver;}
    
    public void setDriver(Vehicule driver) {this.driver = driver;}
    
    public Set<Vehicule> getPassengers() { return passengers; }
    
    public void setPassengers(Set<Vehicule> passengers) { 
        if (passengers == null) {this.passengers = new HashSet<>();} else {this.passengers = passengers; }}
    
    public void addPassenger(Vehicule vehicle) { if (vehicle != null) {this.passengers.add(vehicle);}}
    
    public void removePassenger(Vehicule vehicle) { this.passengers.remove(vehicle); }
    
    public boolean isPassengerIn(Vehicule vehicle) {return this.passengers.contains(vehicle);}

    public double calculateBalance() {
        Set<TypeCat> uniqueTypes = getBikes().stream()
                .map(Bike::getType)
                .collect(Collectors.toSet());
        int categoryCount = uniqueTypes.size();
        double total = 20.0 + 5.0 * Math.max(0, categoryCount);
        return Math.round(total * 100.0) / 100.0;
    }
    public boolean checkBalance() {
        return this.balance <= 0.0;
    }
    public boolean create(Connection conn) 
    {
    		MemberDAO dao = new MemberDAO(conn);
    		return dao.create(this);
    }
    
    public boolean update(Connection conn) 
    {
    		MemberDAO dao = new MemberDAO(conn);
    		return dao.update(this);
    }
}
