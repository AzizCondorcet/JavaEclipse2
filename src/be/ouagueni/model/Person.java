package be.ouagueni.model;

import java.io.Serializable;
import java.sql.Connection;

import be.ouagueni.dao.PersonDAO;

public abstract class Person implements Serializable {

    private static final long serialVersionUID = 7376318509521189223L;
    protected int id;
    protected String name;
    protected String firstname;
    protected String tel;
    protected String password;

    public Person() {}

    public Person(int id, String name, String firstname, String tel, String password) {
        this.id = id;
        setName(name);
        setFirstname(firstname);
        setTel(tel);
        setPassword(password);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Le nom est obligatoire.");
        this.name = name;
    }

    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) {
        if (firstname == null || firstname.trim().isEmpty())
            throw new IllegalArgumentException("Le prénom est obligatoire.");
        this.firstname = firstname;
    }

    public String getTel() { return tel; }
    public void setTel(String tel) {
        if (tel == null || tel.trim().isEmpty())
            throw new IllegalArgumentException("Le numéro de téléphone est obligatoire.");
        this.tel = tel;
    }

    public String getPassword() { return password; }
    public void setPassword(String password) {
        if (password == null || password.trim().isEmpty())
            throw new IllegalArgumentException("Le mot de passe est obligatoire.");
        this.password = password;
    }

    @Override
    public String toString() {
        return "Person[id=" + id + ", name=" + name + ", firstname=" + firstname + "]";
    }
    
    public static Person login(String name, String password,Connection conn) {
        PersonDAO dao = new PersonDAO(conn);
        return dao.findByNameAndPassword(name,password);
    }
}
