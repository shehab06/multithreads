package Entity;

public class Admin extends User {
    private int workingHours;

    public Admin(String username, String password,java.util.Date dateOfBirth, Role role, int workingHours){
        super(username, password, role, dateOfBirth);
        this.workingHours = workingHours;
    }

    @Override
    public String toString() {
        return "Role: " + getRole() + "\nWorking Hours: " + workingHours;
    }

    public int getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(int workingHours) {
        this.workingHours = workingHours;
    }

}