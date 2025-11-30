package be.ouagueni.model;

import java.io.Serializable;
import java.sql.Connection;

import be.ouagueni.dao.CalendarDAO;

public class Calendar implements Serializable {

	private static final long serialVersionUID = -2079458858083609116L;
	private int id;
    private Category category;

    public Calendar() {}
    public Calendar(Category category) {
        this.id = 0;           
        this.category = category;
    }
    
    public Calendar(int id, Category category) {
        this.id = id;
        this.category = category;
    }

    
    public int getid() { return id; }
    public void setid(int id) { this.id = id; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public boolean createCalendar(Connection conn) 
    {
    		CalendarDAO dao = new CalendarDAO(conn);
        return dao.create(this);
    }
    
    
}

