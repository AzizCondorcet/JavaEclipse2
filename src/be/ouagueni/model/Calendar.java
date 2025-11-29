package be.ouagueni.model;

import java.io.Serializable;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

import be.ouagueni.dao.CalendarDAO;

public class Calendar implements Serializable {

	private static final long serialVersionUID = -2079458858083609116L;
	private int id;
    private Category category;

    public Calendar() {}
    public Calendar(Category category) {
        this.category = category;
    }

    
    public int getid() { return id; }
    public void setid(int id) { this.id = id; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public boolean createCalendar(Calendar cal,Connection conn) 
    {
    		CalendarDAO dao = new CalendarDAO(conn);
        return dao.create(cal);
    }
}

