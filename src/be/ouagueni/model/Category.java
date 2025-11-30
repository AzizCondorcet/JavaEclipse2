package be.ouagueni.model;

import java.io.Serializable;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

import be.ouagueni.dao.CategoryDAO;

public class Category implements Serializable {

	private static final long serialVersionUID = 5610587733056598846L;
	private int id;
    private TypeCat type;
    private Manager manager;
    private Set<Member> Members = new HashSet<>();
    // COMPOSITION 
    private Calendar calendar;

    public Category() {}
    public Category(	int id,TypeCat nomCategorie, Manager manager) 
    {
	    this.id = id;
	    	this.type = nomCategorie; 
	    	this.manager = manager; 
	    	this.calendar = new Calendar(this);
    }

    public int getid() { return id; }
    public void setid(int id) { this.id = id; }
    public TypeCat getNomCategorie() { return type; }
    public void setNomCategorie(TypeCat nomCategorie) { this.type = nomCategorie; }
    public Manager getManager() { return manager; }
    public void setManager(Manager manager) { this.manager = manager; }
    public Set<Member> getMembers() { return Members; }
    public void setMembers(Set<Member> members) { Members = members; }
    public void addMember(Member m) { if (m != null) Members.add(m); }
    public void removeMember(Member m) { Members.remove(m); }
    public Calendar getCalendar() { return calendar; }
    public void setCalendar(Calendar calendar) { this.calendar = calendar; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id == category.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
    
    @Override
    public String toString() {
		return "Category[nomCategorie=" + type + ", manager=" + manager + "]";
	}
    public boolean updateCategory(Category ca,Connection conn) 
    {
    		CategoryDAO dao = new CategoryDAO(conn);
    		return dao.update(ca);
    }
    public static Set<Category> GetAll(Connection conn) 
    {
    		CategoryDAO dao = new CategoryDAO(conn);
        return dao.GetAll();
    }


}

