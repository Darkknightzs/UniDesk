package sis;

/**
 * Student entity representing an enrolled institutional scholar.
 * Extends Person to inherit identity and contact attributes.
 */
public class Student extends Person {

    private final int rollNo;
    private String course;
    private double marks; // 0.0 to 100.0

    public Student(int rollNo, String name, int age, String phone, String address, String course, double marks) {
        super(name, age, phone, address);
        if (rollNo <= 0) {
            throw new IllegalArgumentException("Roll Number must be a positive integer.");
        }
        this.rollNo = rollNo;
        setCourse(course);
        setMarks(marks);
    }

    /**
     * Calculates institutional letter grade based on marks.
     */
    public String getGrade() {
        if (marks >= 90.0) return "A+";
        if (marks >= 80.0) return "A";
        if (marks >= 70.0) return "B";
        if (marks >= 60.0) return "C";
        if (marks >= 40.0) return "D";
        return "F";
    }

    /**
     * Evaluates passing standing (Passing threshold: >= 40.0).
     */
    public String getStatus() {
        return (marks >= 40.0) ? "Pass" : "Fail";
    }

    @Override
    public String getRoleInfo() {
        return String.format("Student: %s | Roll No: %d | Dept: %s | Score: %.1f%% | Grade: %s (%s)",
                getName(), rollNo, course, marks, getGrade(), getStatus());
    }

    @Override
    public String toString() {
        return String.format("%s [Roll #%d, %s, %.1f Marks - %s]",
                getName(), rollNo, course, marks, getGrade());
    }

    public int getRollNo() {
        return rollNo;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        if (course == null || course.trim().isEmpty()) {
            throw new IllegalArgumentException("Course cannot be empty.");
        }
        this.course = course.trim();
    }

    public double getMarks() {
        return marks;
    }

    public void setMarks(double marks) {
        if (marks < 0.0 || marks > 100.0) {
            throw new IllegalArgumentException("Marks must be between 0.0 and 100.0.");
        }
        this.marks = Math.round(marks * 100.0) / 100.0;
    }
}
