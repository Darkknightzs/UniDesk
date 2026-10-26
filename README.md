# 🏫 UniDesk - Student Information System

A modern, high-performance desktop **Student Information System (SIS)** built using pure **Java (JDK 21 LTS)** and **Java Swing**. UniDesk provides comprehensive student enrollment, faculty records, academic performance analytics, and dynamic light/dark theming without relying on heavy external frameworks or complex build systems.

---

## ✨ Features

- **🎓 Student Directory & Record Management**:
  - Full CRUD operations (Add, Update, Search, Delete) for student records.
  - University-style roll number generation (e.g., Year + Course Code + Scholar ID).
  - Instant live search by Roll Number, Name, Course, or Contact details.
  - Stream-specific filtering (BCA, B.Tech, MCA, BBA, B.Com) and automatic stream topper recognition.

- **👨‍🏫 Faculty Directory**:
  - Manage faculty profiles, departmental allocations, subjects taught, and contact information.
  - Dedicated search and filtering across academic departments.

- **📊 Real-Time Analytics Dashboard**:
  - Summary metric cards: Total Enrolled Scholars, Batch Average Marks, Passing Percentage, and Top Performers.
  - Interactive Course Breakdown table with enrolled counts, average performance, and stream toppers.
  - Java2D animated Grade Distribution Bar Chart (Grades A+, A, B, C, F) with smooth cubic easing.

- **🌓 Dynamic Design & Theme Engine**:
  - Built-in **Dark Mode** and **Light Mode** switch with instant UI update.
  - Settings auto-saved across application launches.
  - Multi-resolution vector application branding rendered natively with Java2D graphics.

- **💾 Data Persistence & Portability**:
  - Auto-persists student and faculty datasets in CSV format under the `data/` directory.
  - Bulk CSV Import and Export capabilities for easy data migration and backups.
  - Automatic directory initialization and robust schema validation.

---

## 🛠️ Technology Stack & OOP Concepts

- **Language**: Java (JDK 17+ / JDK 21 LTS recommended)
- **GUI Framework**: Java Swing & Java2D graphics engine
- **Look & Feel**: [FlatLaf](https://www.formdev.com/flatlaf/) for modern, flat cross-platform styling
- **Object-Oriented Programming (OOP) Principles**:
  - **Abstraction**: Abstract `Person` base class defining core identity contracts.
  - **Inheritance & Polymorphism**: `Student` and `Teacher` extend `Person` with specialized attributes and behaviors.
  - **Encapsulation**: Strict property validation (roll numbers, phone formatting, marks constraints, age bounds).
  - **Layered Architecture**: Clear separation between Data Models, Business Logic (`StudentManager`), Storage (`FileHandler`), and UI (`MainFrame`).

---

## 📂 Project Structure

```text
UniDesk/
├── data/
│   ├── students.csv          # Persisted student database
│   └── teachers.csv          # Persisted faculty database
├── lib/
│   └── flatlaf.jar           # Modern Look & Feel library
├── sis/                      # Source package
│   ├── Person.java           # Abstract base class
│   ├── Student.java          # Student model
│   ├── Teacher.java          # Faculty model
│   ├── StudentManager.java   # Collections, queries, & analytics logic
│   ├── FileHandler.java      # CSV persistence, parsing, & validation
│   ├── MainFrame.java        # Modern Swing GUI, vector icons, & charts
│   └── Main.java             # Entry point launcher
├── run.bat                   # 1-Click Windows batch launcher
├── run.ps1                   # PowerShell runner script
├── build_jar.bat             # Standalone executable JAR packager
├── manifest.txt              # Manifest configuration
└── README.md                 # Project documentation
```

---

## ⚙️ System Requirements

- **Operating System**: Windows, macOS, or Linux
- **Java Runtime**: JDK 17 or JDK 21+ installed and configured on your system `PATH`

---

## 🚀 Getting Started

### 1. 1-Click Launch (Windows)
Double-click `run.bat` or execute in PowerShell:
```powershell
.\run.bat
# or
.\run.ps1
```

### 2. Manual Compile & Run
Compile all classes and launch using `javac` and `java`:
```bash
# Compile
javac -cp "lib/*" -d out sis/*.java

# Run
java -cp "out;lib/*" sis.Main
```

### 3. Build Standalone JAR
To create a self-contained executable JAR for distribution:
```cmd
build_jar.bat
```
Run the generated JAR directly:
```bash
java -jar UniDesk.jar
```
