public abstract class Entity {
    private Boolean saved;

    // CONSTURCTORS WHICH SET SAVED TO FALSE BY DEFAULT

    public boolean isSaved(){
        return saved;
    }

    public void save(Database db, Admin admin, String tableName){
        saved = true;
        (db.get(admin, tableName)).add(this);
    }

    public void delete(Database db, Admin admin, String tableName){
        (db.get(admin, tableName)).delete(this);
    }
    
}