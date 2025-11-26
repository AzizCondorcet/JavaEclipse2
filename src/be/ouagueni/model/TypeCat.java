package be.ouagueni.model;

public enum TypeCat {
    RoadBike(2),
    Trial(3),
    Downhill(4),
    Cross(5);

    private final int id;

    TypeCat(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    // Conversion int -> TypeCat
    public static TypeCat fromId(int id) {
        for (TypeCat type : values()) {
            if (type.getId() == id) return type;
        }
        return null; // ou throw new IllegalArgumentException("Type inconnu: " + id);
    }

    // Conversion TypeCat -> int (pour DAO)
    public int toInt() {
        return this.id;
    }
}
