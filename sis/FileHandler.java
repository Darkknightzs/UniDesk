package sis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Persistence layer for CSV storage, bulk importing, exporting, and report generation.
 */
public class FileHandler {

    public static final String DEFAULT_CSV_FILE = "students.csv";
    public static final String DEFAULT_TEACHERS_CSV_FILE = "teachers.csv";
    private static final String CSV_HEADER = "RollNo,Name,Age,Phone,Address,Course,Marks";
    private static final String TEACHERS_CSV_HEADER = "TeacherID,Name,Age,Phone,Address,Subject";

    public static class ImportResult {
        private final int importedCount;
        private final int skippedCount;
        private final List<String> issues;

        public ImportResult(int importedCount, int skippedCount, List<String> issues) {
            this.importedCount = importedCount;
            this.skippedCount = skippedCount;
            this.issues = issues;
        }

        public int getImportedCount() { return importedCount; }
        public int getSkippedCount() { return skippedCount; }
        public List<String> getIssues() { return issues; }
    }

    /**
     * Initializes dataset on startup:
     * - If students.csv exists, loads from it.
     * - If empty or less than 10 records, augments with comprehensive institutional dataset.
     * - If teachers.csv exists, loads from it; otherwise preloads faculty dataset and persists it.
     */
    public static void initializeData(StudentManager manager) {
        File file = new File(DEFAULT_CSV_FILE);
        if (file.exists()) {
            try {
                loadFromCsv(file, manager);
            } catch (Exception e) {
                System.err.println("Warning: Auto-load from " + DEFAULT_CSV_FILE + " failed: " + e.getMessage());
            }
        }

        if (manager.getTotalCount() < 10) {
            preloadRichStudentDataset(manager);
            try {
                saveToCsv(file, manager);
            } catch (Exception e) {
                System.err.println("Warning: Could not save expanded dataset: " + e.getMessage());
            }
        }

        File teacherFile = new File(DEFAULT_TEACHERS_CSV_FILE);
        if (teacherFile.exists()) {
            try {
                loadTeachersFromCsv(teacherFile, manager);
            } catch (Exception e) {
                System.err.println("Warning: Auto-load from " + DEFAULT_TEACHERS_CSV_FILE + " failed: " + e.getMessage());
            }
        }

        if (manager.getAllTeachers().isEmpty()) {
            preloadFacultyDataset(manager);
            try {
                saveTeachersToCsv(teacherFile, manager);
            } catch (Exception e) {
                System.err.println("Warning: Could not save faculty dataset: " + e.getMessage());
            }
        }
    }

    /**
     * Preloads 115 comprehensive student records across all 5 courses (B.Tech, BCA, MCA, BBA, B.Com)
     * using official institutional cohort names.
     */
    public static void preloadRichStudentDataset(StudentManager manager) {
        Student[] dataset = new Student[]{
                new Student(2411101, "Abhishek", 19, "9846755997", "210, Model Town, Patiala, Punjab", "B.Tech", 80.2),
                new Student(2411103, "Aryan Kumar", 21, "9645478489", "62, Model Town, Hoshiarpur, Punjab", "B.Tech", 89.1),
                new Student(2411105, "Heena", 22, "9558707162", "146, Civil Lines, Ludhiana, Punjab", "B.Tech", 70.0),
                new Student(2411107, "Jasmeet Kaur", 19, "9486355044", "280, Pakhowal Road, Ludhiana, Punjab", "B.Tech", 78.2),
                new Student(2411109, "Jyoti", 20, "9756864894", "235, Dugri Phase 1, Ludhiana, Punjab", "B.Tech", 84.7),
                new Student(2411111, "Kamaldeep Kaur", 20, "9681693731", "114, Mall Road, Amritsar, Punjab", "B.Tech", 70.0),
                new Student(2411113, "Kiranjot Kaur", 22, "9752013747", "166, Majitha Road, Amritsar, Punjab", "B.Tech", 83.4),
                new Student(2411115, "Manpreet Kaur Dhillon", 21, "9101421373", "140, Urban Estate Phase 2, Jalandhar, Punjab", "B.Tech", 82.3),
                new Student(2411117, "Navjot Kaur", 19, "9638402065", "114, Guru Hargobind Nagar, Phagwara, Punjab", "B.Tech", 71.9),
                new Student(2411119, "Pankaj", 20, "9688689457", "210, Model Town, Patiala, Punjab", "B.Tech", 75.6),
                new Student(2411121, "Pardeep Singh", 22, "9250570155", "230, Mall Road, Kapurthala, Punjab", "B.Tech", 83.4),
                new Student(2411123, "Prabhjeet Singh", 20, "9549125902", "174, Dalhousie Road, Pathankot, Punjab", "B.Tech", 31.9),
                new Student(2411125, "Prithvi", 20, "9403308049", "129, Mall Road, Firozpur Cantt, Punjab", "B.Tech", 66.6),
                new Student(2411127, "Ravinder Singh", 21, "9825906026", "21, BRS Nagar, Ludhiana, Punjab", "B.Tech", 79.1),
                new Student(2411129, "Rehan Ansari", 19, "9885341835", "193, Phase 5, Mohali, Punjab", "B.Tech", 75.8),
                new Student(2411131, "Robinpreet Singh Dhillon", 21, "9586220618", "211, Civil Lines, Bathinda, Punjab", "B.Tech", 77.8),
                new Student(2411133, "Rohit Kumar", 22, "9481276140", "193, Mall Road, Amritsar, Punjab", "B.Tech", 83.7),
                new Student(2411135, "Simranjeet Singh", 19, "9824282848", "225, Sector 70, Mohali, Punjab", "B.Tech", 92.6),
                new Student(2411137, "Simranpreet Kaur", 20, "9415073588", "221, Ghumar Mandi, Ludhiana, Punjab", "B.Tech", 81.6),
                new Student(2411139, "Sourav Tiwari", 20, "9185434043", "40, Guru Hargobind Nagar, Phagwara, Punjab", "B.Tech", 89.1),
                new Student(2411141, "Sulekh", 19, "9605706050", "251, Urban Estate, Patiala, Punjab", "B.Tech", 80.5),
                new Student(2411143, "Suraj Kumar Singh", 20, "9443800536", "115, Urban Estate Phase 2, Jalandhar, Punjab", "B.Tech", 78.9),
                new Student(2422201, "Adarsh Kumar", 22, "9742988886", "184, Phase 3B2, Mohali, Punjab", "MCA", 84.1),
                new Student(2422203, "Aditya Kumar", 23, "9780703559", "183, Phase 7, Mohali, Punjab", "MCA", 87.3),
                new Student(2422205, "Bhawanpreet Kaur", 24, "9960617105", "100, Guru Hargobind Nagar, Phagwara, Punjab", "MCA", 74.2),
                new Student(2422207, "Mandeep Kaur", 22, "9658615116", "81, Model Town, Jalandhar, Punjab", "MCA", 42.4),
                new Student(2422209, "Nikesh Kumar", 21, "9245221600", "265, Urban Estate, Patiala, Punjab", "MCA", 73.7),
                new Student(2422211, "Nishu Raj Singh", 22, "9989037974", "123, Leela Bhawan, Patiala, Punjab", "MCA", 67.9),
                new Student(2422213, "Piyush Lamba", 21, "9406942347", "102, Phase 3B2, Mohali, Punjab", "MCA", 43.6),
                new Student(2422215, "Prince Kumar", 21, "9932293720", "224, Lawrence Road, Amritsar, Punjab", "MCA", 78.5),
                new Student(2422217, "Pushwinder Singh", 23, "9237490943", "175, Sector 71, Mohali, Punjab", "MCA", 78.9),
                new Student(2422219, "Rajveer Kaur", 23, "9947389855", "167, BRS Nagar, Ludhiana, Punjab", "MCA", 83.3),
                new Student(2422221, "Ravi Shah Sonar", 22, "9536712452", "127, Sarabha Nagar, Ludhiana, Punjab", "MCA", 87.3),
                new Student(2422223, "Rishikesh Kumar", 22, "9261267453", "87, Leela Bhawan, Patiala, Punjab", "MCA", 30.7),
                new Student(2422225, "Ritik Kumar", 22, "9747534357", "205, Model Town Phase 3, Bathinda, Punjab", "MCA", 88.2),
                new Student(2422227, "Sahajpreet Singh", 24, "9407826175", "49, Civil Lines, Bathinda, Punjab", "MCA", 74.3),
                new Student(2422229, "Satish Kumar", 24, "9592700491", "267, Ranjit Avenue, Amritsar, Punjab", "MCA", 58.1),
                new Student(2422231, "Sehajvir Singh", 24, "9935295565", "179, Model Town Phase 3, Bathinda, Punjab", "MCA", 76.2),
                new Student(2422233, "Shiva Tiwari", 21, "9529802713", "264, Urban Estate, Patiala, Punjab", "MCA", 91.6),
                new Student(2422235, "Shrikant", 21, "9643537110", "227, Dugri Phase 1, Ludhiana, Punjab", "MCA", 72.0),
                new Student(2422237, "Simran Kaur", 24, "9649100111", "181, Mall Road, Amritsar, Punjab", "MCA", 83.9),
                new Student(2422239, "Simranjeet Kaur", 24, "9500281764", "130, Lawrence Road, Amritsar, Punjab", "MCA", 87.5),
                new Student(2422241, "Sonu Singh", 24, "9508330277", "249, Majitha Road, Amritsar, Punjab", "MCA", 65.3),
                new Student(2422243, "Sukhmandeep Singh", 21, "9520050243", "83, GT Road, Bathinda, Punjab", "MCA", 61.5),
                new Student(2422245, "Suraj", 23, "9882531698", "81, Model Town, Jalandhar, Punjab", "MCA", 85.0),
                new Student(2433402, "Ajayveer Kaur", 22, "9710113735", "98, Phase 5, Mohali, Punjab", "BCA", 70.6),
                new Student(2433404, "Anchal Kumari", 19, "9834749471", "79, GT Road, Bathinda, Punjab", "BCA", 80.8),
                new Student(2433406, "Anjali Rani", 18, "9221676298", "181, Model Town Phase 3, Bathinda, Punjab", "BCA", 67.1),
                new Student(2433408, "Anmol Kaur", 19, "9728488346", "117, Civil Lines, Bathinda, Punjab", "BCA", 84.4),
                new Student(2433410, "Dilraj Kaur", 19, "9605085551", "236, Main Bazaar, Moga, Punjab", "BCA", 81.8),
                new Student(2433412, "Harshpreet Singh", 18, "9160543231", "134, Sarabha Nagar, Ludhiana, Punjab", "BCA", 79.6),
                new Student(2433414, "Kajal Kumari", 21, "9718100553", "224, Ranjit Avenue, Amritsar, Punjab", "BCA", 67.3),
                new Student(2433416, "Kamalveer Singh", 19, "9733849461", "48, Lawrence Road, Amritsar, Punjab", "BCA", 71.3),
                new Student(2433418, "Ajaypal Singh", 21, "6239559392", "142, Model Town, Ludhiana, Punjab", "BCA", 90.0),
                new Student(2433421, "Neeraj Kumar", 21, "9750578875", "63, Cantt Road, Jalandhar, Punjab", "BCA", 78.9),
                new Student(2433423, "Om Parkash", 21, "9635207734", "178, Model Town, Phagwara, Punjab", "BCA", 71.3),
                new Student(2433425, "Prakash Kumar Gupta", 18, "9267241160", "275, Phase 7, Mohali, Punjab", "BCA", 85.2),
                new Student(2433428, "Prem Kumar", 18, "9744326862", "181, Civil Lines, Jalandhar, Punjab", "BCA", 83.5),
                new Student(2433430, "Prince", 18, "9553352141", "220, Sunam Road, Sangrur, Punjab", "BCA", 85.3),
                new Student(2433432, "Rajinder Kaur", 20, "9914200697", "105, Sarabha Nagar, Ludhiana, Punjab", "BCA", 76.1),
                new Student(2433435, "Rajveer Singh", 20, "9270483079", "137, Dugri Phase 1, Ludhiana, Punjab", "BCA", 70.1),
                new Student(2433437, "Ranjodh Singh", 20, "9766656191", "116, College Road, Rupnagar, Punjab", "BCA", 71.0),
                new Student(2433440, "Ranvir Singh", 21, "9763203721", "32, Model Town, Ludhiana, Punjab", "BCA", 74.9),
                new Student(2433442, "Saloni", 20, "9622276702", "75, Phase 5, Mohali, Punjab", "BCA", 87.8),
                new Student(2433445, "Sarwan Kumar", 18, "9247594257", "222, Model Town, Patiala, Punjab", "BCA", 80.1),
                new Student(2433448, "Shimpi Kumari", 21, "9721723308", "175, Pakhowal Road, Ludhiana, Punjab", "BCA", 86.3),
                new Student(2433451, "Simranjot Kaur", 21, "9618388662", "190, Phase 7, Mohali, Punjab", "BCA", 73.6),
                new Student(2433453, "Summi Khatoon", 20, "9774544636", "199, Civil Lines, Jalandhar, Punjab", "BCA", 72.0),
                new Student(2433456, "Swayam Sharma", 18, "9739929078", "76, Mall Road, Kapurthala, Punjab", "BCA", 86.2),
                new Student(2433458, "Taranjot Singh", 20, "9670428694", "200, Cantt Road, Jalandhar, Punjab", "BCA", 70.6),
                new Student(2433461, "Vinay Kumar", 20, "9213776234", "84, Leela Bhawan, Patiala, Punjab", "BCA", 85.7),
                new Student(2433464, "Vishal", 21, "9234035034", "50, Urban Estate Phase 2, Jalandhar, Punjab", "BCA", 46.4),
                new Student(2444301, "Aarti Kumari", 21, "9426713568", "146, Baradari Gardens, Patiala, Punjab", "BBA", 77.7),
                new Student(2444303, "Ajay Yadav", 21, "9545758708", "250, Sector 70, Mohali, Punjab", "BBA", 75.4),
                new Student(2444305, "Anshika Gupta", 18, "9139792775", "45, Court Road, Hoshiarpur, Punjab", "BBA", 82.1),
                new Student(2444307, "Chhaya Chouhan", 21, "9989148573", "89, Mall Road, Kapurthala, Punjab", "BBA", 77.0),
                new Student(2444309, "Guddu Kumar", 21, "9173771192", "191, Samrala Road, Khanna, Punjab", "BBA", 78.6),
                new Student(2444311, "Harmanjot Kaur", 21, "9715856213", "112, College Road, Rupnagar, Punjab", "BBA", 93.6),
                new Student(2444313, "Harsimran Singh", 18, "9181436175", "248, BRS Nagar, Ludhiana, Punjab", "BBA", 84.6),
                new Student(2444315, "Kundan Kumar Jha", 21, "9454739536", "154, Civil Lines, Jalandhar, Punjab", "BBA", 70.4),
                new Student(2444317, "Prabhdeep Singh", 18, "9817643432", "224, Main Bazaar, Moga, Punjab", "BBA", 90.9),
                new Student(2444319, "Prince Kumar Mishra", 18, "9698616084", "126, Sector 70, Mohali, Punjab", "BBA", 81.7),
                new Student(2444321, "Puneet Singh", 19, "9537793211", "181, Court Road, Hoshiarpur, Punjab", "BBA", 88.6),
                new Student(2444323, "Rishu Kumar", 18, "9606976406", "35, GT Road, Bathinda, Punjab", "BBA", 84.1),
                new Student(2444325, "Riya Sen", 19, "9798058297", "123, Green Avenue, Amritsar, Punjab", "BBA", 61.0),
                new Student(2444327, "Sachin Kumar", 21, "9649957940", "117, Cantt Road, Jalandhar, Punjab", "BBA", 76.4),
                new Student(2444329, "Sangeeta Kumari", 18, "9299991462", "170, Majitha Road, Amritsar, Punjab", "BBA", 90.5),
                new Student(2444331, "Sanmeet Kaur", 19, "9732083719", "250, Sector 71, Mohali, Punjab", "BBA", 74.4),
                new Student(2444333, "Sanni Kumar", 20, "9718062872", "270, Ghumar Mandi, Ludhiana, Punjab", "BBA", 75.3),
                new Student(2444335, "Satnam Singh", 21, "9842928960", "188, Model Town, Hoshiarpur, Punjab", "BBA", 71.9),
                new Student(2444337, "Sonu Kumar", 20, "9657993592", "132, Green Avenue, Amritsar, Punjab", "BBA", 52.3),
                new Student(2444339, "Vishal Pandey", 18, "9137738565", "270, Samrala Road, Khanna, Punjab", "BBA", 67.3),
                new Student(2455501, "Chahat Sinha", 21, "9940430953", "14, Model Town, Phagwara, Punjab", "B.Com", 69.3),
                new Student(2455503, "Diya Gupta", 19, "9982660170", "132, Dalhousie Road, Pathankot, Punjab", "B.Com", 83.7),
                new Student(2455505, "Hardeep Singh", 20, "9829517649", "129, Sunam Road, Sangrur, Punjab", "B.Com", 71.0),
                new Student(2455507, "Harjit Kaur", 18, "9812661311", "53, Mall Road, Firozpur Cantt, Punjab", "B.Com", 87.7),
                new Student(2455509, "Harpreet Singh", 18, "9660834451", "157, Model Town, Ludhiana, Punjab", "B.Com", 72.2),
                new Student(2455511, "Intikhab Ansari", 20, "9589353681", "258, Ghumar Mandi, Ludhiana, Punjab", "B.Com", 71.4),
                new Student(2455513, "Khushpreet Kaur", 20, "9107265884", "166, Green Avenue, Amritsar, Punjab", "B.Com", 92.7),
                new Student(2455515, "Ompreet Kaur", 21, "9407815165", "112, Baradari Gardens, Patiala, Punjab", "B.Com", 89.1),
                new Student(2455517, "Pratik Rai", 20, "9559526883", "121, Samrala Road, Khanna, Punjab", "B.Com", 69.5),
                new Student(2455519, "Rajbeer Singh", 21, "9628743680", "120, Baradari Gardens, Patiala, Punjab", "B.Com", 75.0),
                new Student(2455521, "Rajbir Kaur Grewal", 20, "9456603813", "114, Civil Lines, Ludhiana, Punjab", "B.Com", 76.3),
                new Student(2455523, "Raju Kumar", 20, "9544363740", "147, Pakhowal Road, Ludhiana, Punjab", "B.Com", 60.5),
                new Student(2455525, "Saneha Gill", 19, "9551424736", "252, Model Town, Ludhiana, Punjab", "B.Com", 96.5),
                new Student(2455527, "Sania", 18, "9923716262", "144, Civil Lines, Ludhiana, Punjab", "B.Com", 78.0),
                new Student(2455529, "Saraswati Kumari", 20, "9946432106", "72, Court Road, Hoshiarpur, Punjab", "B.Com", 71.4),
                new Student(2455531, "Shubham", 19, "9287151354", "69, Ranjit Avenue, Amritsar, Punjab", "B.Com", 66.0),
                new Student(2455533, "Shubham Kumar", 21, "9563135291", "129, Model Town, Hoshiarpur, Punjab", "B.Com", 70.3),
                new Student(2455535, "Simranjit Kaur", 21, "9761971807", "41, Phase 3B2, Mohali, Punjab", "B.Com", 87.8),
                new Student(2455537, "Suman Kumari", 18, "9946373362", "277, Model Town, Jalandhar, Punjab", "B.Com", 68.2),
                new Student(2455539, "Swastik Swayam", 18, "9659449995", "277, Model Town, Phagwara, Punjab", "B.Com", 65.1),
                new Student(2455541, "Tahseen Khan", 20, "9911626139", "119, Main Bazaar, Moga, Punjab", "B.Com", 71.8),
                new Student(2455543, "Varinder Singh", 20, "9418970598", "56, Dalhousie Road, Pathankot, Punjab", "B.Com", 84.4),
                new Student(2455545, "Yogesh Kumar", 20, "9667299173", "100, Sunam Road, Sangrur, Punjab", "B.Com", 72.8)
        };

        for (Student s : dataset) {
            if (manager.getStudent(s.getRollNo()) == null) {
                try {
                    manager.addStudent(s);
                } catch (Exception ignored) {}
            }
        }
    }

    public static void preloadFacultyDataset(StudentManager manager) {
        Teacher[] faculty = new Teacher[]{
                // ── BCA Faculty (user-provided) ──
                new Teacher("FAC-101", "Shivam Sood", 30, "9811223344", "Model Town, Ludhiana, Punjab", "Full Stack Development"),
                new Teacher("FAC-102", "Rahul Soni", 32, "9822334455", "Sarabha Nagar, Ludhiana, Punjab", "Cloud Computing"),
                new Teacher("FAC-103", "Iqra Nazir", 28, "9833445566", "BRS Nagar, Ludhiana, Punjab", "Data Structures & Algorithms"),
                new Teacher("FAC-104", "Ripandeep Kaur", 34, "9844556677", "Civil Lines, Ludhiana, Punjab", "Computer System Architecture"),
                new Teacher("FAC-105", "Anushka Mohal", 29, "9855667788", "Dugri Phase 1, Ludhiana, Punjab", "Programming in Java"),
                new Teacher("FAC-106", "Navjot Kaur", 31, "9866778899", "Pakhowal Road, Ludhiana, Punjab", "PHP & Web Development"),

                // ── B.Tech Faculty ──
                new Teacher("FAC-201", "Dr. Harpreet Singh Bedi", 45, "9871112233", "Sector 70, Mohali, Punjab", "Engineering Mathematics"),
                new Teacher("FAC-202", "Prof. Amarjeet Kaur", 40, "9872223344", "Phase 7, Mohali, Punjab", "Digital Electronics"),
                new Teacher("FAC-203", "Er. Gurpreet Singh Sandhu", 38, "9873334455", "Model Town, Jalandhar, Punjab", "Operating Systems"),
                new Teacher("FAC-204", "Dr. Manish Arora", 47, "9874445566", "Urban Estate, Patiala, Punjab", "Computer Networks"),
                new Teacher("FAC-205", "Prof. Neha Sharma", 35, "9875556677", "Ranjit Avenue, Amritsar, Punjab", "Software Engineering"),
                new Teacher("FAC-206", "Er. Vikrant Thakur", 33, "9876667788", "Mall Road, Kapurthala, Punjab", "Compiler Design"),

                // ── MCA Faculty ──
                new Teacher("FAC-301", "Dr. Sukhwinder Kaur", 44, "9881112233", "Phase 3B2, Mohali, Punjab", "Advanced Database Management"),
                new Teacher("FAC-302", "Prof. Rajeev Bansal", 41, "9882223344", "Leela Bhawan, Patiala, Punjab", "Machine Learning & AI"),
                new Teacher("FAC-303", "Dr. Priyanka Mehta", 39, "9883334455", "Lawrence Road, Amritsar, Punjab", "Distributed Systems"),
                new Teacher("FAC-304", "Prof. Deepak Kumar Joshi", 43, "9884445566", "GT Road, Bathinda, Punjab", "Advanced Java & Frameworks"),
                new Teacher("FAC-305", "Dr. Jasleen Kaur Bhatia", 37, "9885556677", "Court Road, Hoshiarpur, Punjab", "Cyber Security & Cryptography"),

                // ── BBA Faculty ──
                new Teacher("FAC-401", "Dr. Parveen Kumar Goyal", 50, "9891112233", "Baradari Gardens, Patiala, Punjab", "Business Strategy & Analytics"),
                new Teacher("FAC-402", "Prof. Simran Kaur Gill", 36, "9892223344", "Green Avenue, Amritsar, Punjab", "Marketing Management"),
                new Teacher("FAC-403", "Dr. Rohit Verma", 42, "9893334455", "Ghumar Mandi, Ludhiana, Punjab", "Financial Management"),
                new Teacher("FAC-404", "Prof. Amanpreet Kaur", 34, "9894445566", "Cantt Road, Jalandhar, Punjab", "Organizational Behaviour"),
                new Teacher("FAC-405", "Dr. Naveen Sharma", 48, "9895556677", "Main Bazaar, Moga, Punjab", "Business Communication"),

                // ── B.Com Faculty ──
                new Teacher("FAC-501", "Dr. Kuldeep Singh Randhawa", 52, "9901112233", "Model Town Phase 3, Bathinda, Punjab", "Corporate Accounting"),
                new Teacher("FAC-502", "Prof. Meenakshi Devi", 38, "9902223344", "Majitha Road, Amritsar, Punjab", "Business Law & Taxation"),
                new Teacher("FAC-503", "Dr. Rajinder Pal Singh", 46, "9903334455", "Samrala Road, Khanna, Punjab", "Cost & Management Accounting"),
                new Teacher("FAC-504", "Prof. Anita Kumari", 40, "9904445566", "Dalhousie Road, Pathankot, Punjab", "Economics & Statistics"),
                new Teacher("FAC-505", "Dr. Balwinder Kaur", 44, "9905556677", "College Road, Rupnagar, Punjab", "Auditing & Corporate Governance")
        };

        for (Teacher t : faculty) {
            if (manager.getTeacher(t.getTeacherId()) == null) {
                try {
                    manager.addTeacher(t);
                } catch (Exception ignored) {}
            }
        }
    }

    public static synchronized void autoSave(StudentManager manager) {
        try {
            saveToCsv(new File(DEFAULT_CSV_FILE), manager);
        } catch (Exception e) {
            System.err.println("Error during auto-save: " + e.getMessage());
        }
    }

    public static synchronized void autoSaveTeachers(StudentManager manager) {
        try {
            saveTeachersToCsv(new File(DEFAULT_TEACHERS_CSV_FILE), manager);
        } catch (Exception e) {
            System.err.println("Error during faculty auto-save: " + e.getMessage());
        }
    }

    public static void saveTeachersToCsv(File targetFile, StudentManager manager) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8))) {
            writer.write(TEACHERS_CSV_HEADER);
            writer.newLine();

            for (Teacher t : manager.getAllTeachers()) {
                StringBuilder sb = new StringBuilder();
                sb.append(escapeCsv(t.getTeacherId())).append(",");
                sb.append(escapeCsv(t.getName())).append(",");
                sb.append(t.getAge()).append(",");
                sb.append(escapeCsv(t.getPhone())).append(",");
                sb.append(escapeCsv(t.getAddress())).append(",");
                sb.append(escapeCsv(t.getSubject()));
                writer.write(sb.toString());
                writer.newLine();
            }
        }
    }

    public static void loadTeachersFromCsv(File sourceFile, StudentManager manager) throws Exception {
        if (!sourceFile.exists()) return;
        List<Teacher> loaded = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(sourceFile), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // skip header
            int lineNum = 1;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;
                List<String> tokens = parseCsvLine(line);
                if (tokens.size() < 6) continue;

                try {
                    String tid = tokens.get(0).trim();
                    String name = tokens.get(1).trim();
                    int age = Integer.parseInt(tokens.get(2).trim());
                    String phone = tokens.get(3).trim();
                    String address = tokens.get(4).trim();
                    String subject = tokens.get(5).trim();

                    loaded.add(new Teacher(tid, name, age, phone, address, subject));
                } catch (Exception ex) {
                    System.err.println("Skipping malformed teacher row at line " + lineNum + ": " + ex.getMessage());
                }
            }
        }

        for (Teacher t : loaded) {
            try {
                if (manager.getTeacher(t.getTeacherId()) != null) {
                    manager.updateTeacher(t);
                } else {
                    manager.addTeacher(t);
                }
            } catch (Exception ignored) {}
        }
    }

    public static void saveToCsv(File targetFile, StudentManager manager) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8))) {
            writer.write(CSV_HEADER);
            writer.newLine();

            for (Student s : manager.getAllStudents()) {
                StringBuilder sb = new StringBuilder();
                sb.append(s.getRollNo()).append(",");
                sb.append(escapeCsv(s.getName())).append(",");
                sb.append(s.getAge()).append(",");
                sb.append(escapeCsv(s.getPhone())).append(",");
                sb.append(escapeCsv(s.getAddress())).append(",");
                sb.append(escapeCsv(s.getCourse())).append(",");
                sb.append(s.getMarks());
                writer.write(sb.toString());
                writer.newLine();
            }
        }
    }

    public static void loadFromCsv(File sourceFile, StudentManager manager) throws Exception {
        if (!sourceFile.exists()) return;
        List<Student> loaded = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(sourceFile), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            int lineNum = 1;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;
                List<String> tokens = parseCsvLine(line);
                if (tokens.size() < 7) continue;

                try {
                    int roll = Integer.parseInt(tokens.get(0).trim());
                    String name = tokens.get(1).trim();
                    int age = Integer.parseInt(tokens.get(2).trim());
                    String phone = tokens.get(3).trim();
                    String address = tokens.get(4).trim();
                    String course = tokens.get(5).trim();
                    double marks = Double.parseDouble(tokens.get(6).trim());

                    loaded.add(new Student(roll, name, age, phone, address, course, marks));
                } catch (Exception ex) {
                    System.err.println("Skipping malformed row at line " + lineNum + ": " + ex.getMessage());
                }
            }
        }

        for (Student s : loaded) {
            try {
                if (manager.getStudent(s.getRollNo()) != null) {
                    manager.updateStudent(s);
                } else {
                    manager.addStudent(s);
                }
            } catch (Exception ignored) {}
        }
    }

    public static ImportResult importFromCsv(File sourceFile, StudentManager manager) {
        int imported = 0;
        int skipped = 0;
        List<String> issues = new ArrayList<>();

        if (!sourceFile.exists()) {
            issues.add("Selected file does not exist: " + sourceFile.getName());
            return new ImportResult(0, 0, issues);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(sourceFile), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            int lineNum = 1;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                List<String> tokens = parseCsvLine(line);
                if (tokens.size() < 7) {
                    skipped++;
                    issues.add(String.format("Row %d: Incomplete row (expected 7 columns, got %d)", lineNum, tokens.size()));
                    continue;
                }

                try {
                    int roll = Integer.parseInt(tokens.get(0).trim());
                    String name = tokens.get(1).trim();
                    int age = Integer.parseInt(tokens.get(2).trim());
                    String phone = tokens.get(3).trim();
                    String address = tokens.get(4).trim();
                    String course = tokens.get(5).trim();
                    double marks = Double.parseDouble(tokens.get(6).trim());

                    Student student = new Student(roll, name, age, phone, address, course, marks);
                    manager.addStudent(student);
                    imported++;
                } catch (NumberFormatException nfe) {
                    skipped++;
                    issues.add(String.format("Row %d: Numeric parse error (Roll/Age/Marks)", lineNum));
                } catch (IllegalArgumentException iae) {
                    skipped++;
                    issues.add(String.format("Row %d: Validation error: %s", lineNum, iae.getMessage()));
                } catch (Exception ex) {
                    skipped++;
                    issues.add(String.format("Row %d: Unexpected error: %s", lineNum, ex.getMessage()));
                }
            }
        } catch (Exception e) {
            issues.add("File read error: " + e.getMessage());
        }

        if (imported > 0) {
            autoSave(manager);
        }
        return new ImportResult(imported, skipped, issues);
    }

    public static void exportPrintableReport(File targetFile, StudentManager manager) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8))) {

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.write("========================================================================================================");
            writer.newLine();
            writer.write("                               STUDENT INFORMATION SYSTEM - ACADEMIC AUDIT REPORT                      ");
            writer.newLine();
            writer.write("                               Generated on: " + timestamp);
            writer.newLine();
            writer.write("========================================================================================================");
            writer.newLine();
            writer.newLine();

            writer.write(String.format("%-8s | %-20s | %-8s | %-6s | %-6s | %-6s | %-4s | %-12s | %-22s%n",
                    "Roll No", "Student Name", "Course", "Marks", "Grade", "Status", "Age", "Phone", "Address"));
            writer.write("--------------------------------------------------------------------------------------------------------");
            writer.newLine();

            for (Student s : manager.getAllStudents()) {
                writer.write(String.format("%-8d | %-20s | %-8s | %6.1f | %-6s | %-6s | %-4d | %-12s | %-22s%n",
                        s.getRollNo(),
                        truncate(s.getName(), 20),
                        s.getCourse(),
                        s.getMarks(),
                        s.getGrade(),
                        s.getStatus(),
                        s.getAge(),
                        s.getPhone(),
                        truncate(s.getAddress(), 22)));
            }

            writer.write("========================================================================================================");
            writer.newLine();
            writer.write("INSTITUTIONAL PERFORMANCE ANALYTICS & SUMMARY:");
            writer.newLine();
            writer.write("--------------------------------------------------------------------------------------------------------");
            writer.newLine();
            writer.write(String.format("Total Enrolled Students : %d%n", manager.getTotalCount()));
            writer.write(String.format("Institutional Average   : %.2f / 100.00%n", manager.getAverageMarks()));
            writer.write(String.format("Pass Rate Percentage    : %.1f%%%n", manager.getPassPercentage()));

            Student topper = manager.getTopper();
            if (topper != null) {
                writer.write(String.format("Academic Topper         : %s (#%d, Course: %s) with %.1f Marks (Grade: %s)%n",
                        topper.getName(), topper.getRollNo(), topper.getCourse(), topper.getMarks(), topper.getGrade()));
            } else {
                writer.write("Academic Topper         : N/A\n");
            }

            writer.newLine();
            writer.write("Grade Distribution Count:\n");
            Map<String, Integer> dist = manager.getGradeDistribution();
            for (Map.Entry<String, Integer> entry : dist.entrySet()) {
                writer.write(String.format("  Grade %-2s : %d students%n", entry.getKey(), entry.getValue()));
            }

            writer.newLine();
            writer.write("Course Performance Breakdown:\n");
            for (StudentManager.CourseStats cs : manager.getCourseBreakdown()) {
                writer.write(String.format("  %-8s : %2d students enrolled | Average: %5.1f | Pass Rate: %5.1f%%%n",
                        cs.getCourseName(), cs.getEnrolledCount(), cs.getAverageMarks(), cs.getPassPercentage()));
            }

            writer.write("========================================================================================================");
            writer.newLine();
            writer.write("                                          END OF REPORT                                                 ");
            writer.newLine();
            writer.write("========================================================================================================");
            writer.newLine();
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> list = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    list.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        list.add(current.toString());
        return list;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }
}
