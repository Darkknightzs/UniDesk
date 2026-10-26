package sis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Institutional records and analytics manager.
 * Manages student and faculty datasets and calculates real-time institutional metrics.
 */
public class StudentManager {

    private final Map<Integer, Student> students = new LinkedHashMap<>();
    private final Map<String, Teacher> teachers = new LinkedHashMap<>();

    /**
     * DTO containing aggregated performance metrics for a specific academic course cohort.
     */
    public static class CourseStats {
        private final String courseName;
        private final int enrolledCount;
        private final double averageMarks;
        private final double passPercentage;
        private final String topperInfo;

        public CourseStats(String courseName, int enrolledCount, double averageMarks, double passPercentage, String topperInfo) {
            this.courseName = courseName;
            this.enrolledCount = enrolledCount;
            this.averageMarks = averageMarks;
            this.passPercentage = passPercentage;
            this.topperInfo = topperInfo;
        }

        public String getCourseName() { return courseName; }
        public int getEnrolledCount() { return enrolledCount; }
        public double getAverageMarks() { return averageMarks; }
        public double getPassPercentage() { return passPercentage; }
        public String getTopperInfo() { return topperInfo; }
    }

    // =========================================================================
    // STUDENT OPERATIONS
    // =========================================================================

    public synchronized void addStudent(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("Student record cannot be null.");
        }
        if (students.containsKey(student.getRollNo())) {
            throw new IllegalArgumentException("Duplicate Roll No: A student with Roll No #"
                    + student.getRollNo() + " already exists.");
        }
        students.put(student.getRollNo(), student);
    }

    public synchronized void updateStudent(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("Student record cannot be null.");
        }
        if (!students.containsKey(student.getRollNo())) {
            throw new IllegalArgumentException("Cannot update: Student with Roll No #"
                    + student.getRollNo() + " was not found.");
        }
        students.put(student.getRollNo(), student);
    }

    public synchronized void deleteStudent(int rollNo) {
        if (!students.containsKey(rollNo)) {
            throw new IllegalArgumentException("Cannot delete: Student with Roll No #"
                    + rollNo + " was not found.");
        }
        students.remove(rollNo);
    }

    public synchronized Student getStudent(int rollNo) {
        return students.get(rollNo);
    }

    public synchronized List<Student> getAllStudents() {
        return Collections.unmodifiableList(new ArrayList<>(students.values()));
    }

    public synchronized List<Student> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllStudents();
        }
        String term = keyword.trim().toLowerCase(Locale.ROOT);
        List<Student> results = new ArrayList<>();
        for (Student s : students.values()) {
            boolean matchesRoll = String.valueOf(s.getRollNo()).contains(term);
            boolean matchesName = s.getName().toLowerCase(Locale.ROOT).contains(term);
            boolean matchesCourse = s.getCourse().toLowerCase(Locale.ROOT).contains(term);
            boolean matchesPhone = s.getPhone().toLowerCase(Locale.ROOT).contains(term);
            boolean matchesAddress = s.getAddress().toLowerCase(Locale.ROOT).contains(term);
            boolean matchesGrade = s.getGrade().toLowerCase(Locale.ROOT).equalsIgnoreCase(term);
            boolean matchesStatus = s.getStatus().toLowerCase(Locale.ROOT).startsWith(term);

            if (matchesRoll || matchesName || matchesCourse || matchesPhone || matchesAddress || matchesGrade || matchesStatus) {
                results.add(s);
            }
        }
        return results;
    }

    public synchronized List<Student> filterByCourse(String course) {
        if (course == null || course.equalsIgnoreCase("All") || course.equalsIgnoreCase("All Courses")) {
            return getAllStudents();
        }
        List<Student> results = new ArrayList<>();
        for (Student s : students.values()) {
            if (s.getCourse().equalsIgnoreCase(course.trim())) {
                results.add(s);
            }
        }
        return results;
    }

    // =========================================================================
    // STATISTICAL & ANALYTICAL METRICS
    // =========================================================================

    public synchronized int getTotalCount() {
        return students.size();
    }

    public synchronized double getAverageMarks() {
        if (students.isEmpty()) return 0.0;
        double sum = 0.0;
        for (Student s : students.values()) {
            sum += s.getMarks();
        }
        double avg = sum / students.size();
        return Math.round(avg * 100.0) / 100.0;
    }

    public synchronized Student getTopper() {
        if (students.isEmpty()) return null;
        Student topper = null;
        for (Student s : students.values()) {
            if (topper == null || s.getMarks() > topper.getMarks()) {
                topper = s;
            }
        }
        return topper;
    }

    /**
     * Returns the highest-scoring student in a specific academic course/stream.
     */
    public synchronized Student getTopperByCourse(String course) {
        if (course == null || course.trim().isEmpty() || course.equalsIgnoreCase("All") || course.equalsIgnoreCase("All Courses")) {
            return null;
        }
        Student topper = null;
        for (Student s : students.values()) {
            if (s.getCourse().equalsIgnoreCase(course.trim())) {
                if (topper == null || s.getMarks() > topper.getMarks()) {
                    topper = s;
                }
            }
        }
        return topper;
    }

    public synchronized double getPassPercentage() {
        if (students.isEmpty()) return 0.0;
        int passCount = 0;
        for (Student s : students.values()) {
            if ("Pass".equals(s.getStatus())) {
                passCount++;
            }
        }
        double pct = ((double) passCount / students.size()) * 100.0;
        return Math.round(pct * 10.0) / 10.0;
    }

    public synchronized Map<String, Integer> getGradeDistribution() {
        Map<String, Integer> dist = new LinkedHashMap<>();
        dist.put("A+", 0);
        dist.put("A", 0);
        dist.put("B", 0);
        dist.put("C", 0);
        dist.put("D", 0);
        dist.put("F", 0);

        for (Student s : students.values()) {
            String grade = s.getGrade();
            dist.put(grade, dist.getOrDefault(grade, 0) + 1);
        }
        return dist;
    }

    /**
     * Calculates breakdown statistics for each academic course including the course topper.
     */
    public synchronized List<CourseStats> getCourseBreakdown() {
        String[] standardCourses = {"BCA", "B.Tech", "MCA", "BBA", "B.Com"};
        List<CourseStats> list = new ArrayList<>();

        for (String cName : standardCourses) {
            int count = 0;
            double sum = 0.0;
            int pass = 0;

            for (Student s : students.values()) {
                if (s.getCourse().equalsIgnoreCase(cName)) {
                    count++;
                    sum += s.getMarks();
                    if ("Pass".equalsIgnoreCase(s.getStatus())) {
                        pass++;
                    }
                }
            }

            double avg = count > 0 ? (sum / count) : 0.0;
            double passPct = count > 0 ? (((double) pass / count) * 100.0) : 0.0;
            Student top = getTopperByCourse(cName);
            String topperStr = top != null ? String.format("%s (%.1f%%)", top.getName(), top.getMarks()) : "N/A";
            list.add(new CourseStats(cName, count, Math.round(avg * 10.0) / 10.0, Math.round(passPct * 10.0) / 10.0, topperStr));
        }
        return list;
    }

    // =========================================================================
    // FACULTY OPERATIONS
    // =========================================================================

    public synchronized void addTeacher(Teacher teacher) {
        if (teacher == null) {
            throw new IllegalArgumentException("Faculty record cannot be null.");
        }
        if (teachers.containsKey(teacher.getTeacherId())) {
            throw new IllegalArgumentException("Duplicate ID: Faculty member with ID '"
                    + teacher.getTeacherId() + "' already exists.");
        }
        teachers.put(teacher.getTeacherId(), teacher);
    }

    public synchronized void deleteTeacher(String teacherId) {
        if (!teachers.containsKey(teacherId)) {
            throw new IllegalArgumentException("Cannot delete: Faculty ID '" + teacherId + "' not found.");
        }
        teachers.remove(teacherId);
    }

    public synchronized void updateTeacher(Teacher teacher) {
        if (teacher == null) {
            throw new IllegalArgumentException("Faculty record cannot be null.");
        }
        if (!teachers.containsKey(teacher.getTeacherId())) {
            throw new IllegalArgumentException("Cannot update: Faculty ID '" + teacher.getTeacherId() + "' not found.");
        }
        teachers.put(teacher.getTeacherId(), teacher);
    }

    public synchronized List<Teacher> getAllTeachers() {
        return Collections.unmodifiableList(new ArrayList<>(teachers.values()));
    }

    public synchronized Teacher getTeacher(String teacherId) {
        return teachers.get(teacherId);
    }
}
