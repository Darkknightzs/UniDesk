package sis;

/**
 * Teacher entity representing an institutional faculty member.
 * Extends Person to inherit identity and contact attributes.
 */
public class Teacher extends Person {

    private String teacherId;
    private String subject;

    public Teacher(String teacherId, String name, int age, String phone, String address, String subject) {
        super(name, age, phone, address);
        setTeacherId(teacherId);
        setSubject(subject);
    }

    @Override
    public String getRoleInfo() {
        return String.format("Faculty Member: %s | ID: %s | Specialization: %s | Office: %s | Contact: %s",
                getName(), teacherId, subject, getAddress(), getPhone());
    }

    @Override
    public String toString() {
        return String.format("%s [Faculty ID: %s, Specialization: %s]", getName(), teacherId, subject);
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        if (teacherId == null || teacherId.trim().isEmpty()) {
            throw new IllegalArgumentException("Faculty ID cannot be empty.");
        }
        this.teacherId = teacherId.trim();
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject specialization cannot be empty.");
        }
        this.subject = subject.trim();
    }
}
