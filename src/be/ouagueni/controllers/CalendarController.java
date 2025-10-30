package be.ouagueni.controllers;

import java.sql.Connection;

import be.ouagueni.dao.CalendarDAO;
import be.ouagueni.model.Calendar;

public class CalendarController {
	
	private CalendarDAO calendarDAO;
	
    public CalendarController(Connection conn) {
        this.calendarDAO = new CalendarDAO(conn);
    }
    
    public boolean createCalendar(Calendar cal) {
        try {
            return calendarDAO.create(cal);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
