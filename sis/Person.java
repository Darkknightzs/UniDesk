package sis;

/**
 * ============================================================================
 * [OOP CONCEPT: ENCAPSULATION & ABSTRACTION]
 * ----------------------------------------------------------------------------
 * 1. Abstraction: Person is an abstract class representing common attributes
 *    and behaviors of all human entities in the academic institution.
 * 2. Encapsulation: All fields (name, age, phone, address) are declared private
 *    and only accessible via validated getter and setter methods.
 * ============================================================================
 */
public abstract class Person {

    // Encapsulation: Private member variables
    private String name;
    private int age;
    private String phone;
    private String address;

    /**
     * Parameterized constructor for Person.
     * Validates input values to maintain state integrity.
     */
    public Person(String name, int age, String phone, String address) {
        setName(name);
        setAge(age);
        setPhone(phone);
        setAddress(address);
    }

    // --- Abstract Method demonstrating Polymorphic behavior in subclasses ---
    /**
     * Polymorphic method to be implemented by child classes (Student, Teacher).
     * @return Formatted role information string.
     */
    public abstract String getRoleInfo();

    // --- Encapsulation: Getters and Setters with Business Validation ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        this.name = name.trim();
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        if (age < 5 || age > 100) {
            throw new IllegalArgumentException("Age must be between 5 and 100.");
        }
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if (phone == null || !phone.trim().matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Phone must be exactly 10 digits.");
        }
        this.phone = phone.trim();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = (address == null) ? "" : address.trim();
    }

    @Override
    public String toString() {
        return String.format("%s (Age: %d, Phone: %s, Address: %s)", name, age, phone, address);
    }
}
