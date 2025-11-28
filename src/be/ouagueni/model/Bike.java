package be.ouagueni.model;

import java.io.Serializable;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

import be.ouagueni.dao.BikeDAO;
import be.ouagueni.dao.MemberDAO;

public class Bike implements Serializable {
    private static final long serialVersionUID = -4193360624407477516L;

    private int id;
    private double weight;
    private TypeCat type; // utilisation de l'enum
    private double length;
    private Member owner;
    private Vehicule vehicle;
    private Set<Inscription> inscriptions = new HashSet<>();

    public Bike() {}

    public Bike(int id, double weight, TypeCat type, double length, Member owner) {
        setId(id);
        setWeight(weight);
        setType(type);
        setLength(length);
        setOwner(owner);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (id < 0) throw new IllegalArgumentException("L'ID doit être supérieur ou égal à 0.");
        this.id = id;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        if (weight <= 0) throw new IllegalArgumentException("Le poids doit être supérieur à 0.");
        this.weight = weight;
    }

    public TypeCat getType() {
        return type;
    }

    public void setType(TypeCat type) {
        if (type == null) throw new IllegalArgumentException("Le type de vélo est obligatoire.");
        this.type = type;
    }

    // Utilitaire pour DAO : récupérer l'int correspondant pour SQL
    public int getTypeAsInt() {
        return type.toInt();
    }

    // Utilitaire pour DAO : setter depuis int SQL
    public void setTypeFromInt(int value) {
        this.type = TypeCat.fromId(value);
        if (this.type == null) throw new IllegalArgumentException("Type de vélo inconnu : " + value);
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        if (length <= 0) throw new IllegalArgumentException("La longueur doit être supérieure à 0.");
        this.length = length;
    }

    public Member getOwner() {
        return owner;
    }

    public void setOwner(Member owner) {
        this.owner = owner;
    }

    public Vehicule getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicule vehicle) {
        this.vehicle = vehicle;
    }

    public Set<Inscription> getInscriptions() {
        return inscriptions;
    }

    public void setInscriptions(Set<Inscription> inscriptions) {
        if (inscriptions == null) throw new IllegalArgumentException("Les inscriptions ne peuvent pas être null.");
        this.inscriptions = inscriptions;
    }

    public void addInscription(Inscription ins) {
        if (ins != null) this.inscriptions.add(ins);
    }

    public void removeInscription(Inscription ins) {
        if (ins != null) this.inscriptions.remove(ins);
    }
    public boolean update(Connection conn) 
    {
    		BikeDAO dao = new BikeDAO(conn);
    		return dao.update(this);
    }
    public boolean create(Connection conn) 
    {
    		BikeDAO dao = new BikeDAO(conn);
    		return dao.create(this);
    }
    public boolean delete(Connection conn) 
    {
    		BikeDAO dao = new BikeDAO(conn);
    		return dao.delete(this);
    }
    public Bike find(Connection conn, int id) 
    {
    		BikeDAO dao = new BikeDAO(conn);
    		return dao.find(id);
    }
}
