package be.ouagueni.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Bike implements Serializable {
	private static final long serialVersionUID = -4193360624407477516L;
	private int id;
    private double weight;
    private String type;
    private double length;
    private Member owner;
    private Vehicle vehicle;
    private Set<Inscription> inscriptions = new HashSet<>();

    public Bike() {}
    public Bike (int id,double weight, String type, double length, Member owner) {
        this.weight = weight;
        this.type = type;
        this.length = length;
        this.owner = owner;
        this.id=id;
    }


    public double getid() { return id; }
    public void setid(int id) { this.id = id; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getLength() { return length; }
    public void setLength(double length) { this.length = length; }
    public Member getOwner() { return owner; }
    public void setOwner(Member owner) { this.owner = owner; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    public Set<Inscription> getInscriptions() { return inscriptions; }
    public void setInscriptions(Set<Inscription> inscriptions) { this.inscriptions = inscriptions; }
    public void addInscription(Inscription ins) { if (ins != null) this.inscriptions.add(ins); }
    public void removeInscription(Inscription ins) { this.inscriptions.remove(ins); }
    
}

