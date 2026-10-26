# 🏫 UniDesk - Student Information System (SIS)
### Production-Grade Desktop Application in Java & Swing

A modern desktop **Student Information System** built strictly using core **Java (JDK 21 LTS)** and **Java Swing** with zero external build tools (no Maven, no Gradle). All GUI components, vector graphics, chart animations, and design theming are unified into [`MainFrame.java`](file:///c:/Users/Dark/Documents/code/UniDesk/sis/MainFrame.java).

---

## 🎨 New Features: UniDesk Brand Logo & Smooth Animations

1. **Official UniDesk Vector Logo**:
   - Scalable vector logo featuring a modern royal-to-navy squircle badge, graduation mortarboard diamond with 3D facet lighting, golden tassel ribbon, open academic knowledge book, and gold center excellence star.
   - Displayed prominently in the main header bar and set as multi-resolution native window & taskbar icon (`setIconImages`).
2. **60 FPS Bar Chart Rise Animation**:
   - When switching to the **Analytics Dashboard** tab or when records are updated, the grade distribution bars smoothly rise from zero with a cubic ease-out interpolation (`1 - (1-t)^3`).
   - Bar labels and percentages fade in smoothly as the bars reach full height.
3. **Stat Cards Animated Count-Up**:
   - The primary metrics (**Total Enrolled**, **Average Marks**, **Passing Rate**) animate and count up dynamically from zero upon dashboard display.

---

## 📂 Project Structure (`sis` package)

```
UniDesk/
├── data/
│   ├── students.csv          # Auto-persisted dataset (115 scholars across 5 courses)
│   └── teachers.csv          # Auto-persisted faculty dataset (27 faculty members)
├── lib/
│   └── flatlaf.jar           # Modern FlatLaf Look & Feel
├── sis/
│   ├── Person.java           # Abstract base class (Encapsulation, Validation)
│   ├── Student.java          # Student subclass (Course, Marks, Grade, Status)
│   ├── Teacher.java          # Faculty subclass (ID, Specialization)
│   ├── StudentManager.java   # Business logic, collections, cohort analytics (No GUI)
│   ├── FileHandler.java      # CSV persistence, bulk I/O, printable reports
│   ├── MainFrame.java        # UNIFIED GUI LAYER:
│   │                         #   • MainFrame (Window, 3 Tabs, Tables, Forms, Badges)
│   │                         #   • UniDesk Official Vector Logo & Icon Engine
│   │                         #   • 60fps Smooth Chart Rise & Metric Count-Up Animations
│   │                         #   • ThemeManager (Light/Dark design system & styles)
│   │                         #   • BarChartPanel (Java2D animated grade distribution)
│   │                         #   • LookAndFeel setup & GUI main launcher
│   └── Main.java             # Entry point (delegates to MainFrame)
├── run.bat                   # 1-Click compiler and launcher script
├── run.ps1                   # PowerShell runner script
├── build_jar.bat             # Standalone executable JAR packager
├── manifest.txt              # JAR Manifest configuration
└── README.md                 # Documentation
```

---

## 🚀 Running the Application

### 1-Click Launch (Recommended)
```powershell
.\run.bat
```

### Manual Compile & Run
```bash
# 1. Compile with plain javac
javac -d out sis/*.java

# 2. Run with FlatLaf
java -cp "out;lib/*" sis.Main
# OR directly:
java -cp "out;lib/*" sis.MainFrame

# Or run without external libraries (Nimbus fallback)
java -cp out sis.Main
```
