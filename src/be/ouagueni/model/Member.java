package be.ouagueni.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Member extends Person implements Serializable {

	private static final long serialVersionUID = -25458080844517046L;
	private double balance;
    private Set<Inscription> inscriptions = new HashSet<>();
    private Set<Category> categories = new HashSet<>(); 
    private Set<Bike> bikes = new HashSet<>(); 
    private Vehicle driver;
    private Set<Vehicle> passengers = new HashSet<>(); 

    public Member() { super(); }
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
    
    public Vehicle getDriver() {return driver;}
    
    public void setDriver(Vehicle driver) {this.driver = driver;}
    
    public Set<Vehicle> getPassengers() { return passengers; }
    
    public void setPassengers(Set<Vehicle> passengers) { 
        if (passengers == null) {this.passengers = new HashSet<>();} else {this.passengers = passengers; }}
    
    public void addPassenger(Vehicle vehicle) { if (vehicle != null) {this.passengers.add(vehicle);}}
    
    public void removePassenger(Vehicle vehicle) { this.passengers.remove(vehicle); }
    
    public boolean isPassengerIn(Vehicle vehicle) {return this.passengers.contains(vehicle);}

    // méthodes métier de ton diagramme
    public double calculateBalance() { return balance; /* TODO: calcul réel */ }
    public boolean checkBalance() { return balance >= 0; }
}
