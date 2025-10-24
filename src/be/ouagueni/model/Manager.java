package be.ouagueni.model;


public class Manager extends Person {

	/* j'pense pas qu'il y a d'attribut Category ici*/
	private static final long serialVersionUID = -148424375150907897L;
	public Manager() { super(); }
    public Manager(int id, String name, String firstname, String tel, String password) {
        super(id, name, firstname, tel, password);
    }
}

