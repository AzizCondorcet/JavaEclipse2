package be.ouagueni.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Category implements Serializable {

	private static final long serialVersionUID = 5610587733056598846L;
    private String nomCategorie;
    private Manager manager;
    private Set<Member> Members = new HashSet<>();
    private Calendar calendar;

    public Category() {}
    public Category(String nomCategorie, Manager manager,Calendar calendar) 
    { this.nomCategorie = nomCategorie; this.manager = manager; this.calendar=calendar; }

    public String getNomCategorie() { return nomCategorie; }
    public void setNomCategorie(String nomCategorie) { this.nomCategorie = nomCategorie; }
    public Manager getManager() { return manager; }
    public void setManager(Manager manager) { this.manager = manager; }
    public Set<Member> getMembers() { return Members; }
    public void setMembers(Set<Member> members) { Members = members; }
    public void addMember(Member m) { if (m != null) Members.add(m); }
    public void removeMember(Member m) { Members.remove(m); }
    public Calendar getCalendar() { return calendar; }
    public void setCalendar(Calendar calendar) { this.calendar = calendar; }

    @Override
    public String toString() {
		return "Category[nomCategorie=" + nomCategorie + ", manager=" + manager + "]";
	}
}

