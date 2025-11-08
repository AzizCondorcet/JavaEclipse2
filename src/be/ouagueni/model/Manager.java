package be.ouagueni.model;

public class Manager extends Person {

    private static final long serialVersionUID = -148424375150907897L;
    private Category category; 
    
    public Manager() { 
        super(); 
    }
    
    public Manager(String name, String firstname) {
        super(0, name, null, null, firstname);
    }
    
    public Manager(int id, String name, String firstname, String tel, String password) {
        super(id, name, firstname, tel, password);
    }
    
    // Getter et Setter pour Category
    public Category getCategory() { 
        return category; 
    }
    
    public void setCategory(Category category) { 
        this.category = category;
    }
    
    @Override
    public String toString() {
        return "Manager[id=" + getId() + ", name=" + getName() + 
               ", firstname=" + getFirstname() + 
               ", category=" + (category != null ? category.getNomCategorie() : "null") + "]";
    }
}