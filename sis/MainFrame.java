package sis;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * ============================================================================
 * [UNIFIED GUI LAYER: MAIN APPLICATION FRAME]
 * ----------------------------------------------------------------------------
 * Consolidates all Swing GUI presentation, components, vector graphics,
 * charts, and design theming into a single cohesive file:
 *  1. MainFrame (Main window, Navigation tabs, CRUD Forms, Table & Badges)
 *  2. ThemeManager (Light/Dark design system, fonts, and recursion engine)
 *  3. BarChartPanel (Java2D custom anti-aliased grade distribution chart)
 *  4. IconFactory (Scalable vector icons with zero external asset dependencies)
 * ============================================================================
 */
public class MainFrame extends JFrame {

    private final StudentManager studentManager;
    private final ThemeManager themeManager;

    // Navigation & Containers
    private JTabbedPane tabbedPane;
    private JPanel headerPanel;
    private JLabel headerTitleLabel;
    private JLabel headerSubLabel;
    private JButton themeToggleBtn;
    private JButton aboutBtn;
    private JLabel statusBarLabel;

    // --- Tab 1: Students ---
    private JTextField rollNoField;
    private JTextField nameField;
    private JTextField ageField;
    private JTextField phoneField;
    private JTextField addressField;
    private JComboBox<String> courseComboBox;
    private JTextField marksField;

    private JButton addBtn;
    private JButton updateBtn;
    private JButton deleteBtn;
    private JButton clearBtn;

    private JTextField searchField;
    private JComboBox<String> courseFilterCombo;
    private JTable studentTable;
    private DefaultTableModel studentTableModel;
    private TableRowSorter<DefaultTableModel> studentSorter;

    private JButton importCsvBtn;
    private JButton exportCsvBtn;
    private JButton exportReportBtn;

    // --- Tab 2: Analytics Dashboard ---
    private JLabel totalStudentsVal;
    private JLabel avgMarksVal;
    private JLabel topperVal;
    private JLabel passPctVal;
    private JLabel topperSubtext;
    private JLabel passSubtext;
    private BarChartPanel barChartPanel;
    private JTable courseStatsTable;
    private DefaultTableModel courseStatsTableModel;

    // --- Tab 3: Faculty ---
    private JTextField teacherIdField;
    private JTextField teacherNameField;
    private JTextField teacherAgeField;
    private JTextField teacherPhoneField;
    private JTextField teacherAddressField;
    private JTextField teacherSubjectField;

    private JButton addTeacherBtn;
    private JButton updateTeacherBtn;
    private JButton deleteTeacherBtn;
    private JButton clearTeacherBtn;
    private JButton viewProfileBtn;

    private JTextField teacherSearchField;
    private JLabel facultyCountBadge;
    private JTable teacherTable;
    private DefaultTableModel teacherTableModel;
    private TableRowSorter<DefaultTableModel> teacherSorter;

    // Status bar dynamic theming
    private JPanel statusBarPanel;
    private final List<JLabel> keycapLabels = new ArrayList<>();
    private final List<JLabel> keycapActionLabels = new ArrayList<>();

    // Interactive Table Hover States
    private int hoveredStudentRow = -1;
    private int hoveredTeacherRow = -1;
    private int hoveredCourseStatsRow = -1;

    // =========================================================================
    // CONSTRUCTOR & INITIALIZATION
    // =========================================================================

    public MainFrame(StudentManager studentManager, ThemeManager themeManager) {
        this.studentManager = studentManager;
        this.themeManager = themeManager;

        setTitle("UniDesk - Student Information System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 740);
        setMinimumSize(new Dimension(960, 620));
        setLocationRelativeTo(null);
        List<Image> appIcons = new ArrayList<>();
        for (int s : new int[]{16, 24, 32, 48, 64, 128, 256}) {
            appIcons.add(((IconFactory.VectorIcon) IconFactory.createUniDeskLogo(s)).toImage());
        }
        setIconImages(appIcons);

        initComponents();
        setupShortcuts();
        refreshAllViews();

        themeManager.addThemeChangeListener(this::applyThemeStyles);
        applyThemeStyles();
        updateWindowTitleBarTheme();

        // Initial smooth dashboard animation on launch
        SwingUtilities.invokeLater(this::startDashboardAnimations);
    }

    private void initComponents() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.putClientProperty("panelType", "root");
        setContentPane(rootPanel);

        // 1. Top Header Bar
        rootPanel.add(createHeaderBar(), BorderLayout.NORTH);

        // 2. Center Tabs (Students, Analytics Dashboard, Faculty)
        tabbedPane = new JTabbedPane();
        tabbedPane.setUI(new ThemeManager.ModernTabbedPaneUI(themeManager));
        tabbedPane.setFont(ThemeManager.BOLD_FIELD_FONT);
        tabbedPane.setBackground(themeManager.getHeaderBg());
        tabbedPane.setForeground(themeManager.getTextColor());
        tabbedPane.setBorder(null);

        tabbedPane.addTab("  Students Directory", IconFactory.createGraduationCapIcon(18, themeManager.getTextColor()), createStudentsTab());
        tabbedPane.addTab("  Analytics Dashboard", IconFactory.createAnalyticsIcon(18, themeManager.getTextColor()), createDashboardTab());
        tabbedPane.addTab("  Academic Faculty", IconFactory.createFacultyIcon(18, themeManager.getTextColor()), createFacultyTab());

        // Smooth reactive animation trigger when user switches to Analytics Dashboard
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 1) {
                startDashboardAnimations();
            }
        });

        rootPanel.add(tabbedPane, BorderLayout.CENTER);

        // 3. Bottom Status Bar
        rootPanel.add(createStatusBar(), BorderLayout.SOUTH);
    }

    // =========================================================================
    // 1. HEADER BAR
    // =========================================================================

    private JPanel createHeaderBar() {
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.putClientProperty("isHeaderBar", Boolean.TRUE);

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        brandPanel.setOpaque(false);

        // Official UniDesk Logo
        JLabel logoLabel = new JLabel(IconFactory.createUniDeskLogo(46));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);

        headerTitleLabel = new JLabel("UniDesk");
        headerTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

        titleRow.add(headerTitleLabel);

        headerSubLabel = new JLabel("Campus Academic Management & Institutional Registry");
        headerSubLabel.setFont(ThemeManager.SUBHEADER_FONT);

        textPanel.add(titleRow);
        textPanel.add(headerSubLabel);

        brandPanel.add(logoLabel);
        brandPanel.add(textPanel);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setOpaque(false);

        themeToggleBtn = new JButton();
        themeToggleBtn.setToolTipText("Toggle interface color scheme (Light/Dark)");
        themeToggleBtn.addActionListener(e -> themeManager.toggleTheme());

        aboutBtn = new JButton(" About");
        aboutBtn.setToolTipText("System specifications and version details");
        aboutBtn.addActionListener(e -> showAboutDialog());

        actionsPanel.add(themeToggleBtn);
        actionsPanel.add(aboutBtn);

        headerPanel.add(brandPanel, BorderLayout.WEST);
        headerPanel.add(actionsPanel, BorderLayout.EAST);
        updateHeaderTheme();
        return headerPanel;
    }

    private void updateHeaderTheme() {
        boolean dark = themeManager.isDark();
        headerPanel.setBackground(themeManager.getHeaderBg());
        headerPanel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, themeManager.getCardBorder()),
                new EmptyBorder(12, 20, 12, 20)
        ));

        if (headerTitleLabel != null) {
            headerTitleLabel.setForeground(dark ? Color.WHITE : Color.decode("#0F172A"));
        }
        if (headerSubLabel != null) {
            headerSubLabel.setForeground(dark ? Color.decode("#94A3B8") : Color.decode("#64748B"));
        }
        if (themeToggleBtn != null) {
            Color btnBg = dark ? Color.decode("#1E293B") : Color.decode("#F1F5F9");
            Color btnFg = dark ? Color.WHITE : Color.decode("#0F172A");
            Color btnHover = dark ? Color.decode("#334155") : Color.decode("#E2E8F0");
            ThemeManager.styleFlatButton(themeToggleBtn, btnBg, btnFg, btnHover);
            themeToggleBtn.setIcon(IconFactory.createSunMoonIcon(dark, 15, btnFg));
            themeToggleBtn.setText(dark ? " Light Mode" : " Dark Mode");
        }
        if (aboutBtn != null) {
            Color btnBg = dark ? Color.decode("#1E293B") : Color.decode("#F1F5F9");
            Color btnFg = dark ? Color.WHITE : Color.decode("#0F172A");
            Color btnHover = dark ? Color.decode("#334155") : Color.decode("#E2E8F0");
            ThemeManager.styleFlatButton(aboutBtn, btnBg, btnFg, btnHover);
            aboutBtn.setIcon(IconFactory.createInfoIcon(15, btnFg));
        }
    }

    // =========================================================================
    // 2. TAB 1: STUDENTS (CRUD FORM, TABLE & BADGES, LIVE SEARCH & CSV)
    // =========================================================================

    private JPanel createStudentsTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.putClientProperty("panelType", "mainBg");
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        panel.add(createStudentFormCard(), BorderLayout.WEST);
        panel.add(createStudentTableCard(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStudentFormCard() {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.putClientProperty("panelType", "card");
        card.setPreferredSize(new Dimension(340, 0));

        JPanel formHeader = new JPanel(new GridLayout(2, 1, 0, 2));
        formHeader.setOpaque(false);
        formHeader.setBorder(new EmptyBorder(0, 0, 4, 0));

        JLabel formTitle = new JLabel(" Student Registration", IconFactory.createGraduationCapIcon(18, ThemeManager.ACCENT_BLUE), SwingConstants.LEFT);
        formTitle.setFont(ThemeManager.TITLE_FONT);
        formTitle.setForeground(themeManager.getTextColor());

        JLabel formSub = new JLabel("Enter scholar records & academic details");
        formSub.setFont(ThemeManager.SMALL_FONT);
        formSub.setForeground(themeManager.getSubtextColor());
        formSub.putClientProperty("role", "subtext");

        formHeader.add(formTitle);
        formHeader.add(formSub);
        card.add(formHeader, BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        rollNoField = new JTextField();
        nameField = new JTextField();
        ageField = new JTextField();
        phoneField = new JTextField();
        addressField = new JTextField();
        courseComboBox = new JComboBox<>(new String[]{"BCA", "MCA", "BBA", "B.Tech", "B.Com"});
        marksField = new JTextField();

        int row = 0;
        addFormField(fieldsPanel, gbc, "Roll Number (Unique ID):", rollNoField, row++);
        addFormField(fieldsPanel, gbc, "Full Student Name:", nameField, row++);
        addFormField(fieldsPanel, gbc, "Enrolled Course:", courseComboBox, row++);
        addFormField(fieldsPanel, gbc, "Academic Marks (0 - 100):", marksField, row++);
        addFormField(fieldsPanel, gbc, "Age (5 - 100):", ageField, row++);
        addFormField(fieldsPanel, gbc, "Contact Phone (10 Digits):", phoneField, row++);
        addFormField(fieldsPanel, gbc, "Residential Address:", addressField, row++);

        // Vertical glue anchor to prevent fields from expanding or shifting into vertical center
        gbc.gridx = 0;
        gbc.gridy = row * 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        fieldsPanel.add(Box.createGlue(), gbc);

        JScrollPane fieldsScroll = new JScrollPane(fieldsPanel);
        fieldsScroll.putClientProperty("borderlessScroll", Boolean.TRUE);
        themeManager.styleFormScrollPane(fieldsScroll);

        card.add(fieldsScroll, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(new EmptyBorder(6, 0, 0, 0));

        addBtn = new JButton(" Add Student", IconFactory.createAddIcon(14, Color.WHITE));
        updateBtn = new JButton(" Update", IconFactory.createEditIcon(14, Color.WHITE));
        deleteBtn = new JButton(" Delete", IconFactory.createDeleteIcon(14, Color.WHITE));
        clearBtn = new JButton(" Clear Form", IconFactory.createClearIcon(14, Color.WHITE));

        ThemeManager.styleFlatButton(addBtn, ThemeManager.SUCCESS_GREEN, Color.WHITE, Color.decode("#059669"));
        ThemeManager.styleFlatButton(updateBtn, ThemeManager.ACCENT_BLUE, Color.WHITE, Color.decode("#1D4ED8"));
        ThemeManager.styleFlatButton(deleteBtn, ThemeManager.DANGER_RED, Color.WHITE, Color.decode("#DC2626"));
        ThemeManager.styleFlatButton(clearBtn, Color.decode("#64748B"), Color.WHITE, Color.decode("#475569"));

        addBtn.setToolTipText("Register new student (Shortcut: Enter)");
        updateBtn.setToolTipText("Save modifications to selected record");
        deleteBtn.setToolTipText("Remove selected student (Shortcut: Delete)");
        clearBtn.setToolTipText("Clear form fields (Shortcut: Ctrl+N)");

        addBtn.addActionListener(e -> onAddStudent());
        updateBtn.addActionListener(e -> onUpdateStudent());
        deleteBtn.addActionListener(e -> onDeleteStudent());
        clearBtn.addActionListener(e -> clearStudentForm());

        btnPanel.add(addBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(clearBtn);

        card.add(btnPanel, BorderLayout.SOUTH);
        return card;
    }

    private void addFormField(JPanel container, GridBagConstraints gbc, String labelText, JComponent field, int row) {
        gbc.gridx = 0;
        gbc.gridy = row * 2;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(row == 0 ? 0 : 2, 2, 0, 2);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(ThemeManager.SMALL_FONT);
        lbl.putClientProperty("role", "subtext");
        container.add(lbl, gbc);

        gbc.gridy = (row * 2) + 1;
        gbc.insets = new Insets(0, 2, 3, 2);
        themeManager.styleInputField(field);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 28));
        container.add(field, gbc);
    }

    private JPanel createStudentTableCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.putClientProperty("panelType", "card");

        // Search & Filter Toolbar
        JPanel filterToolbar = new JPanel(new BorderLayout(10, 0));
        filterToolbar.setOpaque(false);

        JPanel searchBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        searchBoxPanel.setOpaque(false);
        JLabel searchIcon = new JLabel(" Search:", IconFactory.createSearchIcon(15, themeManager.getSubtextColor()), SwingConstants.LEFT);
        searchIcon.setFont(ThemeManager.BOLD_FIELD_FONT);
        searchField = new JTextField(18);
        themeManager.styleInputField(searchField);
        searchField.setToolTipText("Search live by Name, Roll No, Course, Phone, Address, Grade");

        searchBoxPanel.add(searchIcon);
        searchBoxPanel.add(searchField);

        JPanel courseFilterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        courseFilterPanel.setOpaque(false);
        JLabel filterLbl = new JLabel("Department Filter:");
        filterLbl.setFont(ThemeManager.SMALL_FONT);
        filterLbl.putClientProperty("role", "subtext");
        courseFilterCombo = new JComboBox<>(new String[]{"All Courses", "BCA", "MCA", "BBA", "B.Tech", "B.Com"});
        themeManager.styleInputField(courseFilterCombo);

        courseFilterPanel.add(filterLbl);
        courseFilterPanel.add(courseFilterCombo);

        filterToolbar.add(searchBoxPanel, BorderLayout.WEST);
        filterToolbar.add(courseFilterPanel, BorderLayout.EAST);
        card.add(filterToolbar, BorderLayout.NORTH);

        // Columns: Roll No, Name, Course, Marks, Grade, Status, Age, Phone, Address
        String[] columns = {"Roll No", "Student Name", "Course", "Marks", "Grade", "Status", "Age", "Phone", "Address"};
        studentTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 6) return Integer.class;
                if (columnIndex == 3) return Double.class;
                return String.class;
            }
        };

        studentTable = new JTable(studentTableModel);
        studentTable.setRowHeight(38);
        studentTable.setFont(ThemeManager.TABLE_FONT);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.setShowGrid(false);
        studentTable.setIntercellSpacing(new Dimension(0, 0));
        studentTable.setShowHorizontalLines(true);
        studentTable.setShowVerticalLines(false);
        studentTable.setGridColor(themeManager.isDark() ? Color.decode("#1E293B") : Color.decode("#F1F5F9"));

        studentTable.getTableHeader().setDefaultRenderer(new ModernHeaderRenderer());
        studentTable.getTableHeader().setPreferredSize(new Dimension(0, 36));
        studentTable.getTableHeader().setReorderingAllowed(false);

        studentSorter = new TableRowSorter<>(studentTableModel);
        studentTable.setRowSorter(studentSorter);

        TableColumnModel colModel = studentTable.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(60);
        colModel.getColumn(1).setPreferredWidth(210);
        colModel.getColumn(2).setPreferredWidth(85);
        colModel.getColumn(3).setPreferredWidth(70);
        colModel.getColumn(4).setPreferredWidth(70);
        colModel.getColumn(5).setPreferredWidth(90);
        colModel.getColumn(6).setPreferredWidth(50);
        colModel.getColumn(7).setPreferredWidth(110);
        colModel.getColumn(8).setPreferredWidth(180);

        StudentCellRenderer renderer = new StudentCellRenderer();
        for (int i = 0; i < studentTable.getColumnCount(); i++) {
            studentTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // Table Hover Row Tracker
        studentTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int r = studentTable.rowAtPoint(e.getPoint());
                if (r != hoveredStudentRow) {
                    hoveredStudentRow = r;
                    studentTable.repaint();
                }
            }
        });
        studentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredStudentRow != -1) {
                    hoveredStudentRow = -1;
                    studentTable.repaint();
                }
            }
        });

        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onStudentRowSelected();
            }
        });

        JScrollPane scrollPane = new JScrollPane(studentTable);
        themeManager.styleModernScrollPane(scrollPane);
        card.add(scrollPane, BorderLayout.CENTER);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        courseFilterCombo.addActionListener(e -> applyFilters());

        // Bottom Actions Toolbar
        JPanel bottomToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        bottomToolbar.setOpaque(false);

        importCsvBtn = new JButton(" Import CSV", IconFactory.createImportIcon(14, Color.WHITE));
        exportCsvBtn = new JButton(" Export CSV", IconFactory.createExportIcon(14, Color.WHITE));
        exportReportBtn = new JButton(" Export Report", IconFactory.createReportIcon(14, Color.WHITE));

        ThemeManager.styleFlatButton(importCsvBtn, Color.decode("#475569"), Color.WHITE, Color.decode("#334155"));
        ThemeManager.styleFlatButton(exportCsvBtn, ThemeManager.ACCENT_BLUE, Color.WHITE, Color.decode("#1D4ED8"));
        ThemeManager.styleFlatButton(exportReportBtn, Color.decode("#0D9488"), Color.WHITE, Color.decode("#0F766E"));

        importCsvBtn.setToolTipText("Import students from CSV file");
        exportCsvBtn.setToolTipText("Export active records to CSV (Shortcut: Ctrl+S)");
        exportReportBtn.setToolTipText("Generate printable academic audit document");

        importCsvBtn.addActionListener(e -> onImportCsv());
        exportCsvBtn.addActionListener(e -> onExportCsv());
        exportReportBtn.addActionListener(e -> onExportReport());

        bottomToolbar.add(importCsvBtn);
        bottomToolbar.add(exportCsvBtn);
        bottomToolbar.add(exportReportBtn);

        card.add(bottomToolbar, BorderLayout.SOUTH);
        return card;
    }

    // =========================================================================
    // 3. TAB 2: ANALYTICS DASHBOARD
    // =========================================================================

    private JPanel createDashboardTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.putClientProperty("panelType", "mainBg");
        panel.setBorder(new EmptyBorder(14, 18, 14, 18));

        // 4 Stat Cards
        JPanel statCardsGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        statCardsGrid.setOpaque(false);

        totalStudentsVal = new JLabel("0");
        avgMarksVal = new JLabel("0.0");
        topperVal = new JLabel("N/A");
        passPctVal = new JLabel("0.0%");

        topperSubtext = new JLabel("No topper registered");
        passSubtext = new JLabel("Passing threshold: >= 40%");

        statCardsGrid.add(createMetricCard("Total Enrolled", totalStudentsVal, "Active scholars in registry", ThemeManager.ACCENT_BLUE, IconFactory.createGraduationCapIcon(18, ThemeManager.ACCENT_BLUE)));
        statCardsGrid.add(createMetricCard("Average Marks", avgMarksVal, "Institutional performance / 100", Color.decode("#8B5CF6"), IconFactory.createAnalyticsIcon(18, Color.decode("#8B5CF6"))));
        statCardsGrid.add(createMetricCard("Class Topper", topperVal, topperSubtext, ThemeManager.TOPPER_GOLD, IconFactory.createTrophyIcon(18, ThemeManager.TOPPER_GOLD)));
        statCardsGrid.add(createMetricCard("Passing Rate", passPctVal, passSubtext, ThemeManager.SUCCESS_GREEN, IconFactory.createShieldCheckIcon(18, ThemeManager.SUCCESS_GREEN)));

        panel.add(statCardsGrid, BorderLayout.NORTH);

        // Center Split: Bar Chart (Left) & Course Breakdown Table (Right)
        JPanel centerSplit = new JPanel(new GridBagLayout());
        centerSplit.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 8);

        JPanel chartCard = new JPanel(new BorderLayout());
        chartCard.putClientProperty("panelType", "card");
        chartCard.setBorder(themeManager.createCardBorder());
        barChartPanel = new BarChartPanel(studentManager, themeManager);
        chartCard.add(barChartPanel, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.weightx = 0.62;
        centerSplit.add(chartCard, gbc);

        JPanel courseCard = new JPanel(new BorderLayout(8, 8));
        courseCard.putClientProperty("panelType", "card");
        courseCard.setBorder(themeManager.createCardBorder());

        JLabel courseTitle = new JLabel("Departmental Cohort Breakdown");
        courseTitle.setFont(ThemeManager.TITLE_FONT);
        courseCard.add(courseTitle, BorderLayout.NORTH);

        String[] courseCols = {"Course", "Enrolled", "Avg Score", "Pass %", "Stream Topper 🏆"};
        courseStatsTableModel = new DefaultTableModel(courseCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        courseStatsTable = new JTable(courseStatsTableModel);
        courseStatsTable.setRowHeight(36);
        courseStatsTable.setFont(ThemeManager.TABLE_FONT);
        courseStatsTable.setShowGrid(false);
        courseStatsTable.setIntercellSpacing(new Dimension(0, 0));
        courseStatsTable.setShowHorizontalLines(true);
        courseStatsTable.setShowVerticalLines(false);
        courseStatsTable.setGridColor(themeManager.isDark() ? Color.decode("#1E293B") : Color.decode("#F1F5F9"));
        courseStatsTable.getTableHeader().setDefaultRenderer(new ModernHeaderRenderer());
        courseStatsTable.getTableHeader().setPreferredSize(new Dimension(0, 34));
        courseStatsTable.getTableHeader().setReorderingAllowed(false);
        courseStatsTable.setCursor(new Cursor(Cursor.HAND_CURSOR));

        CourseStatsCellRenderer statsRenderer = new CourseStatsCellRenderer();
        for (int i = 0; i < courseStatsTable.getColumnCount(); i++) {
            courseStatsTable.getColumnModel().getColumn(i).setCellRenderer(statsRenderer);
        }
        courseStatsTable.getColumnModel().getColumn(0).setPreferredWidth(75);
        courseStatsTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        courseStatsTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        courseStatsTable.getColumnModel().getColumn(3).setPreferredWidth(85);
        courseStatsTable.getColumnModel().getColumn(4).setPreferredWidth(170);

        // Table Hover Row Tracker for Course Stats
        courseStatsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int r = courseStatsTable.rowAtPoint(e.getPoint());
                if (r != hoveredCourseStatsRow) {
                    hoveredCourseStatsRow = r;
                    courseStatsTable.repaint();
                }
            }
        });
        courseStatsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredCourseStatsRow != -1) {
                    hoveredCourseStatsRow = -1;
                    courseStatsTable.repaint();
                }
            }
        });

        JScrollPane courseScroll = new JScrollPane(courseStatsTable);
        themeManager.styleModernScrollPane(courseScroll);
        courseCard.add(courseScroll, BorderLayout.CENTER);

        gbc.gridx = 1;
        gbc.weightx = 0.38;
        gbc.insets = new Insets(0, 8, 0, 0);
        centerSplit.add(courseCard, gbc);

        panel.add(centerSplit, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Object subtextObj, Color accentColor, Icon icon) {
        HoverMetricCard card = new HoverMetricCard(new BorderLayout(8, 6), accentColor, themeManager);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(ThemeManager.SMALL_FONT);
        titleLbl.putClientProperty("role", "subtext");

        JLabel iconLbl = new JLabel(icon);

        top.add(titleLbl, BorderLayout.WEST);
        top.add(iconLbl, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        valueLabel.setFont(ThemeManager.STAT_VALUE_FONT);
        valueLabel.setForeground(themeManager.getTextColor());
        card.add(valueLabel, BorderLayout.CENTER);

        if (subtextObj instanceof JLabel) {
            JLabel subLbl = (JLabel) subtextObj;
            subLbl.setFont(ThemeManager.SMALL_FONT);
            subLbl.putClientProperty("role", "subtext");
            card.add(subLbl, BorderLayout.SOUTH);
        } else if (subtextObj instanceof String) {
            JLabel subLbl = new JLabel((String) subtextObj);
            subLbl.setFont(ThemeManager.SMALL_FONT);
            subLbl.putClientProperty("role", "subtext");
            card.add(subLbl, BorderLayout.SOUTH);
        }

        return card;
    }

    /**
     * Interactive Metric Card featuring smooth 60fps hover elevation, ambient glow,
     * color wash, and accent pill expansion animations.
     */
    private static class HoverMetricCard extends JPanel {
        private final Color accentColor;
        private final ThemeManager themeManager;
        private float hoverFactor = 0.0f;
        private javax.swing.Timer hoverTimer;

        public HoverMetricCard(BorderLayout layout, Color accentColor, ThemeManager themeManager) {
            super(layout);
            this.accentColor = accentColor;
            this.themeManager = themeManager;
            setOpaque(false);
            putClientProperty("panelType", "hoverMetricCard");
            setBorder(new EmptyBorder(14, 16, 14, 16));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    animateHover(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    animateHover(false);
                }
            });
        }

        @Override
        public void removeNotify() {
            if (hoverTimer != null) {
                hoverTimer.stop();
                hoverTimer = null;
            }
            super.removeNotify();
        }

        private void animateHover(boolean forward) {
            if (hoverTimer != null && hoverTimer.isRunning()) {
                hoverTimer.stop();
            }
            hoverTimer = new javax.swing.Timer(14, ev -> {
                if (forward) {
                    hoverFactor += 0.15f;
                    if (hoverFactor >= 1.0f) {
                        hoverFactor = 1.0f;
                        if (hoverTimer != null) hoverTimer.stop();
                    }
                } else {
                    hoverFactor -= 0.15f;
                    if (hoverFactor <= 0.0f) {
                        hoverFactor = 0.0f;
                        if (hoverTimer != null) hoverTimer.stop();
                    }
                }
                repaint();
            });
            hoverTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = 14;

            // Ambient Glow Drop Shadow
            if (hoverFactor > 0.02f) {
                int glowAlpha = (int) (34 * hoverFactor);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), glowAlpha));
                g2.fillRoundRect(0, 2, w, h - 2, arc + 4, arc + 4);
            }

            // Card Body Background
            g2.setColor(themeManager.getCardBg());
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            // Subtle Tint Wash
            if (hoverFactor > 0.02f) {
                int tintAlpha = (int) (20 * hoverFactor);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), tintAlpha));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
            }

            // Smooth Border Interpolation
            Color defaultBorder = themeManager.getCardBorder();
            int br = (int) (defaultBorder.getRed() + (accentColor.getRed() - defaultBorder.getRed()) * hoverFactor);
            int bg = (int) (defaultBorder.getGreen() + (accentColor.getGreen() - defaultBorder.getGreen()) * hoverFactor);
            int bb = (int) (defaultBorder.getBlue() + (accentColor.getBlue() - defaultBorder.getBlue()) * hoverFactor);
            float strokeW = 1.0f + 1.2f * hoverFactor;
            g2.setStroke(new BasicStroke(strokeW));
            g2.setColor(new Color(br, bg, bb));
            g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);

            // Expanding Top Accent Indicator Pill
            g2.setColor(accentColor);
            int pillWidth = (int) (38 + 24 * hoverFactor);
            g2.fillRoundRect(16, 0, pillWidth, (int) (3 + hoverFactor), 2, 2);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =========================================================================
    // 4. TAB 3: FACULTY DIRECTORY & PROFILE CARD
    // =========================================================================

    private JPanel createFacultyTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.putClientProperty("panelType", "mainBg");
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel formCard = new JPanel(new BorderLayout(8, 8));
        formCard.putClientProperty("panelType", "card");
        formCard.setPreferredSize(new Dimension(340, 0));

        JPanel formHeader = new JPanel(new GridLayout(2, 1, 0, 2));
        formHeader.setOpaque(false);
        formHeader.setBorder(new EmptyBorder(0, 0, 4, 0));

        JLabel formTitle = new JLabel(" Faculty Member Profile", IconFactory.createFacultyIcon(18, Color.decode("#7C3AED")), SwingConstants.LEFT);
        formTitle.setFont(ThemeManager.TITLE_FONT);
        formTitle.setForeground(themeManager.getTextColor());

        JLabel formSub = new JLabel("Enter professor records & institutional assignments");
        formSub.setFont(ThemeManager.SMALL_FONT);
        formSub.setForeground(themeManager.getSubtextColor());
        formSub.putClientProperty("role", "subtext");

        formHeader.add(formTitle);
        formHeader.add(formSub);
        formCard.add(formHeader, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        teacherIdField = new JTextField();
        teacherNameField = new JTextField();
        teacherAgeField = new JTextField();
        teacherPhoneField = new JTextField();
        teacherAddressField = new JTextField();
        teacherSubjectField = new JTextField();

        int row = 0;
        addFormField(fields, gbc, "Faculty ID (e.g. FAC-101):", teacherIdField, row++);
        addFormField(fields, gbc, "Professor Name:", teacherNameField, row++);
        addFormField(fields, gbc, "Specialization / Subject:", teacherSubjectField, row++);
        addFormField(fields, gbc, "Age (5 - 100):", teacherAgeField, row++);
        addFormField(fields, gbc, "Contact Phone (10 Digits):", teacherPhoneField, row++);
        addFormField(fields, gbc, "Office / Chamber Address:", teacherAddressField, row++);

        gbc.gridx = 0;
        gbc.gridy = row * 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        fields.add(Box.createGlue(), gbc);

        JScrollPane fieldsScroll = new JScrollPane(fields);
        fieldsScroll.putClientProperty("borderlessScroll", Boolean.TRUE);
        themeManager.styleFormScrollPane(fieldsScroll);

        formCard.add(fieldsScroll, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout(0, 6));
        southPanel.setOpaque(false);
        southPanel.setBorder(new EmptyBorder(6, 0, 0, 0));

        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnPanel.setOpaque(false);

        addTeacherBtn = new JButton(" Add Faculty", IconFactory.createAddIcon(14, Color.WHITE));
        updateTeacherBtn = new JButton(" Update", IconFactory.createEditIcon(14, Color.WHITE));
        deleteTeacherBtn = new JButton(" Delete", IconFactory.createDeleteIcon(14, Color.WHITE));
        clearTeacherBtn = new JButton(" Clear Form", IconFactory.createClearIcon(14, Color.WHITE));

        ThemeManager.styleFlatButton(addTeacherBtn, ThemeManager.SUCCESS_GREEN, Color.WHITE, Color.decode("#059669"));
        ThemeManager.styleFlatButton(updateTeacherBtn, ThemeManager.ACCENT_BLUE, Color.WHITE, Color.decode("#1D4ED8"));
        ThemeManager.styleFlatButton(deleteTeacherBtn, ThemeManager.DANGER_RED, Color.WHITE, Color.decode("#DC2626"));
        ThemeManager.styleFlatButton(clearTeacherBtn, Color.decode("#64748B"), Color.WHITE, Color.decode("#475569"));

        addTeacherBtn.setToolTipText("Register new faculty profile");
        updateTeacherBtn.setToolTipText("Save modifications to selected faculty profile");
        deleteTeacherBtn.setToolTipText("Remove selected faculty member");
        clearTeacherBtn.setToolTipText("Clear faculty form fields");

        addTeacherBtn.addActionListener(e -> onAddTeacher());
        updateTeacherBtn.addActionListener(e -> onUpdateTeacher());
        deleteTeacherBtn.addActionListener(e -> onDeleteTeacher());
        clearTeacherBtn.addActionListener(e -> clearTeacherForm());

        btnPanel.add(addTeacherBtn);
        btnPanel.add(updateTeacherBtn);
        btnPanel.add(deleteTeacherBtn);
        btnPanel.add(clearTeacherBtn);

        viewProfileBtn = new JButton(" View Full Dossier / Profile", IconFactory.createProfileCardIcon(14, Color.WHITE));
        ThemeManager.styleFlatButton(viewProfileBtn, Color.decode("#7C3AED"), Color.WHITE, Color.decode("#6D28D9"));
        viewProfileBtn.setPreferredSize(new Dimension(0, 32));
        viewProfileBtn.setToolTipText("Display comprehensive institutional role profile");
        viewProfileBtn.addActionListener(e -> onViewInstitutionalProfile());

        southPanel.add(btnPanel, BorderLayout.CENTER);
        southPanel.add(viewProfileBtn, BorderLayout.SOUTH);

        formCard.add(southPanel, BorderLayout.SOUTH);
        panel.add(formCard, BorderLayout.WEST);

        // Faculty Table Card
        JPanel tableCard = new JPanel(new BorderLayout(10, 10));
        tableCard.putClientProperty("panelType", "card");

        JPanel headerToolbar = new JPanel(new BorderLayout(10, 0));
        headerToolbar.setOpaque(false);

        JPanel titleWithBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleWithBadge.setOpaque(false);
        JLabel tableTitle = new JLabel("Academic Faculty Directory & Departmental Staff");
        tableTitle.setFont(ThemeManager.TITLE_FONT);

        facultyCountBadge = new JLabel(String.format(" %d Faculty Members ", studentManager.getAllTeachers().size()));
        facultyCountBadge.setFont(ThemeManager.BADGE_FONT);
        facultyCountBadge.setOpaque(true);
        facultyCountBadge.setBackground(Color.decode("#7C3AED"));
        facultyCountBadge.setForeground(Color.WHITE);
        facultyCountBadge.setBorder(new EmptyBorder(2, 6, 2, 6));

        titleWithBadge.add(tableTitle);
        titleWithBadge.add(facultyCountBadge);

        // Live Search Bar for Faculty
        JPanel searchBoxPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchBoxPanel.setOpaque(false);
        JLabel searchIcon = new JLabel(" Search Faculty:", IconFactory.createSearchIcon(15, themeManager.getSubtextColor()), SwingConstants.LEFT);
        searchIcon.setFont(ThemeManager.BOLD_FIELD_FONT);
        teacherSearchField = new JTextField(18);
        themeManager.styleInputField(teacherSearchField);
        teacherSearchField.setToolTipText("Filter live by Faculty ID, Name, Specialization, Phone, Office");
        teacherSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyTeacherFilter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyTeacherFilter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyTeacherFilter(); }
        });

        searchBoxPanel.add(searchIcon);
        searchBoxPanel.add(teacherSearchField);

        headerToolbar.add(titleWithBadge, BorderLayout.WEST);
        headerToolbar.add(searchBoxPanel, BorderLayout.EAST);
        tableCard.add(headerToolbar, BorderLayout.NORTH);

        String[] cols = {"Faculty ID", "Professor Name", "Specialization", "Age", "Phone", "Office Location"};
        teacherTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        teacherTable = new JTable(teacherTableModel);
        teacherTable.setRowHeight(38);
        teacherTable.setFont(ThemeManager.TABLE_FONT);
        teacherTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        teacherTable.setShowGrid(false);
        teacherTable.setIntercellSpacing(new Dimension(0, 0));
        teacherTable.setShowHorizontalLines(true);
        teacherTable.setShowVerticalLines(false);
        teacherTable.setGridColor(themeManager.isDark() ? Color.decode("#1E293B") : Color.decode("#F1F5F9"));
        teacherTable.getTableHeader().setDefaultRenderer(new ModernHeaderRenderer());
        teacherTable.getTableHeader().setPreferredSize(new Dimension(0, 36));
        teacherTable.getTableHeader().setReorderingAllowed(false);
        teacherTable.setCursor(new Cursor(Cursor.HAND_CURSOR));

        teacherSorter = new TableRowSorter<>(teacherTableModel);
        teacherTable.setRowSorter(teacherSorter);

        TeacherCellRenderer teacherRenderer = new TeacherCellRenderer();
        for (int i = 0; i < teacherTable.getColumnCount(); i++) {
            teacherTable.getColumnModel().getColumn(i).setCellRenderer(teacherRenderer);
        }

        TableColumnModel colModel = teacherTable.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(85);   // Faculty ID
        colModel.getColumn(1).setPreferredWidth(170);  // Professor Name
        colModel.getColumn(2).setPreferredWidth(210);  // Specialization (Badge pill)
        colModel.getColumn(3).setPreferredWidth(55);   // Age
        colModel.getColumn(4).setPreferredWidth(110);  // Phone
        colModel.getColumn(5).setPreferredWidth(210);  // Office Location

        // Table Hover Row Tracker for Faculty Table
        teacherTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int r = teacherTable.rowAtPoint(e.getPoint());
                if (r != hoveredTeacherRow) {
                    hoveredTeacherRow = r;
                    teacherTable.repaint();
                }
            }
        });
        teacherTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredTeacherRow != -1) {
                    hoveredTeacherRow = -1;
                    teacherTable.repaint();
                }
            }
        });

        teacherTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onTeacherRowSelected();
            }
        });

        JScrollPane scroll = new JScrollPane(teacherTable);
        themeManager.styleModernScrollPane(scroll);
        tableCard.add(scroll, BorderLayout.CENTER);

        panel.add(tableCard, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // 5. STATUS BAR
    // =========================================================================

    private JPanel createStatusBar() {
        statusBarPanel = new JPanel(new BorderLayout());
        statusBarPanel.putClientProperty("panelType", "mainBg");

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        statusBarLabel = new JLabel("● 115 Enrolled Scholars   |   📊 Average: 76.4%   |   🛡️ Passing Rate: 98.3%");
        statusBarLabel.setFont(ThemeManager.SMALL_FONT);

        leftPanel.add(statusBarLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);

        keycapLabels.clear();
        keycapActionLabels.clear();
        rightPanel.add(createKeycapChip("Ctrl+N", "Clear"));
        rightPanel.add(createKeycapChip("Ctrl+S", "Save CSV"));
        rightPanel.add(createKeycapChip("Delete", "Remove"));

        statusBarPanel.add(leftPanel, BorderLayout.WEST);
        statusBarPanel.add(rightPanel, BorderLayout.EAST);
        updateStatusBarTheme();
        return statusBarPanel;
    }

    private JPanel createKeycapChip(String key, String action) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        chip.setOpaque(false);

        JLabel keyLbl = new JLabel(" " + key + " ");
        keyLbl.setFont(ThemeManager.BADGE_FONT);
        keyLbl.setOpaque(true);
        keycapLabels.add(keyLbl);

        JLabel actLbl = new JLabel(action);
        actLbl.setFont(ThemeManager.SMALL_FONT);
        keycapActionLabels.add(actLbl);

        chip.add(keyLbl);
        chip.add(actLbl);
        return chip;
    }

    // =========================================================================
    // SHORTCUTS
    // =========================================================================

    private void setupShortcuts() {
        JRootPane root = getRootPane();

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "action_clear");
        root.getActionMap().put("action_clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (tabbedPane.getSelectedIndex() == 2) {
                    clearTeacherForm();
                } else {
                    clearStudentForm();
                }
            }
        });

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "action_save");
        root.getActionMap().put("action_save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onExportCsv();
            }
        });

        studentTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "table_delete");
        studentTable.getActionMap().put("table_delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onDeleteStudent();
            }
        });

        teacherTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "teacher_delete");
        teacherTable.getActionMap().put("teacher_delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onDeleteTeacher();
            }
        });
    }

    // =========================================================================
    // STUDENT CRUD HANDLERS
    // =========================================================================

    private void onAddStudent() {
        try {
            int roll = parseInteger(rollNoField.getText(), "Roll Number");
            String name = nameField.getText();
            int age = parseInteger(ageField.getText(), "Age");
            String phone = phoneField.getText();
            String address = addressField.getText();
            String course = (String) courseComboBox.getSelectedItem();
            double marks = parseDouble(marksField.getText(), "Marks");

            Student student = new Student(roll, name, age, phone, address, course, marks);
            studentManager.addStudent(student);
            FileHandler.autoSave(studentManager);

            clearStudentForm();
            refreshAllViews();
            showToast("Scholar #" + roll + " (" + name + ") registered successfully!", true);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Input Validation", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onUpdateStudent() {
        if (rollNoField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a student from the table to update.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int roll = Integer.parseInt(rollNoField.getText().trim());
            if (studentManager.getStudent(roll) == null) {
                throw new IllegalArgumentException("Cannot update: Scholar #" + roll + " does not exist in registry. Use 'Add' to register new scholars.");
            }
            String name = nameField.getText();
            int age = parseInteger(ageField.getText(), "Age");
            String phone = phoneField.getText();
            String address = addressField.getText();
            String course = (String) courseComboBox.getSelectedItem();
            double marks = parseDouble(marksField.getText(), "Marks");

            Student student = new Student(roll, name, age, phone, address, course, marks);
            studentManager.updateStudent(student);
            FileHandler.autoSave(studentManager);

            refreshAllViews();
            showToast("Scholar record #" + roll + " updated successfully!", true);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Update failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDeleteStudent() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a student to delete.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = studentTable.convertRowIndexToModel(selectedRow);
        int rollNo = (Integer) studentTableModel.getValueAt(modelRow, 0);
        String studentName = (String) studentTableModel.getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                String.format("Are you sure you want to delete student #%d (%s)?\nThis action cannot be undone.", rollNo, studentName),
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                studentManager.deleteStudent(rollNo);
                FileHandler.autoSave(studentManager);
                clearStudentForm();
                refreshAllViews();
                showToast("Scholar #" + rollNo + " removed from registry.", false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Deletion failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearStudentForm() {
        studentTable.clearSelection();
        rollNoField.setText("");
        rollNoField.setEditable(true);
        nameField.setText("");
        ageField.setText("");
        phoneField.setText("");
        addressField.setText("");
        courseComboBox.setSelectedIndex(0);
        marksField.setText("");
        rollNoField.requestFocusInWindow();
    }

    private void onStudentRowSelected() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = studentTable.convertRowIndexToModel(selectedRow);
            int roll = (Integer) studentTableModel.getValueAt(modelRow, 0);
            Student s = studentManager.getStudent(roll);
            if (s != null) {
                rollNoField.setText(String.valueOf(s.getRollNo()));
                rollNoField.setEditable(false);
                nameField.setText(s.getName());
                ageField.setText(String.valueOf(s.getAge()));
                phoneField.setText(s.getPhone());
                addressField.setText(s.getAddress());
                courseComboBox.setSelectedItem(s.getCourse());
                marksField.setText(String.valueOf(s.getMarks()));
            }
        }
    }

    // =========================================================================
    // FACULTY CRUD HANDLERS
    // =========================================================================

    private void onAddTeacher() {
        try {
            String tid = teacherIdField.getText().trim();
            String name = teacherNameField.getText().trim();
            int age = parseInteger(teacherAgeField.getText().trim(), "Age");
            String phone = teacherPhoneField.getText().trim();
            String address = teacherAddressField.getText().trim();
            String subject = teacherSubjectField.getText().trim();

            Teacher teacher = new Teacher(tid, name, age, phone, address, subject);
            studentManager.addTeacher(teacher);
            FileHandler.autoSaveTeachers(studentManager);
            clearTeacherForm();
            refreshTeacherTable();
            showToast("Faculty profile '" + tid + "' registered successfully!", true);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onUpdateTeacher() {
        try {
            String tid = teacherIdField.getText().trim();
            if (tid.isEmpty()) {
                throw new IllegalArgumentException("Please select a faculty member from the directory to update.");
            }
            if (studentManager.getTeacher(tid) == null) {
                throw new IllegalArgumentException("Cannot update: Faculty member '" + tid + "' does not exist. Use 'Add' to register new faculty.");
            }
            String name = teacherNameField.getText().trim();
            int age = parseInteger(teacherAgeField.getText().trim(), "Age");
            String phone = teacherPhoneField.getText().trim();
            String address = teacherAddressField.getText().trim();
            String subject = teacherSubjectField.getText().trim();

            Teacher teacher = new Teacher(tid, name, age, phone, address, subject);
            studentManager.updateTeacher(teacher);
            FileHandler.autoSaveTeachers(studentManager);
            clearTeacherForm();
            refreshTeacherTable();
            showToast("Faculty profile '" + tid + "' updated successfully!", true);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyTeacherFilter() {
        if (teacherSorter == null) return;
        String query = teacherSearchField.getText().trim();
        if (query.isEmpty()) {
            teacherSorter.setRowFilter(null);
        } else {
            teacherSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
        }
    }

    private void onDeleteTeacher() {
        int selectedRow = teacherTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a faculty member to delete.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = teacherTable.convertRowIndexToModel(selectedRow);
        String tid = (String) teacherTableModel.getValueAt(modelRow, 0);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete faculty profile '" + tid + "'?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                studentManager.deleteTeacher(tid);
                FileHandler.autoSaveTeachers(studentManager);
                clearTeacherForm();
                refreshTeacherTable();
                showToast("Faculty profile '" + tid + "' deleted.", false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearTeacherForm() {
        teacherTable.clearSelection();
        teacherIdField.setText("");
        teacherIdField.setEditable(true);
        teacherNameField.setText("");
        teacherAgeField.setText("");
        teacherPhoneField.setText("");
        teacherAddressField.setText("");
        teacherSubjectField.setText("");
    }

    private void onTeacherRowSelected() {
        int selected = teacherTable.getSelectedRow();
        if (selected >= 0) {
            int modelRow = teacherTable.convertRowIndexToModel(selected);
            String tid = (String) teacherTableModel.getValueAt(modelRow, 0);
            Teacher t = studentManager.getTeacher(tid);
            if (t != null) {
                teacherIdField.setText(t.getTeacherId());
                teacherIdField.setEditable(false);
                teacherNameField.setText(t.getName());
                teacherAgeField.setText(String.valueOf(t.getAge()));
                teacherPhoneField.setText(t.getPhone());
                teacherAddressField.setText(t.getAddress());
                teacherSubjectField.setText(t.getSubject());
            }
        }
    }

    private void onViewInstitutionalProfile() {
        int facultyRow = teacherTable.getSelectedRow();
        int studentRow = studentTable.getSelectedRow();

        Person targetPerson = null;
        if (facultyRow >= 0) {
            int modelRow = teacherTable.convertRowIndexToModel(facultyRow);
            String tid = (String) teacherTableModel.getValueAt(modelRow, 0);
            targetPerson = studentManager.getTeacher(tid);
        } else if (studentRow >= 0) {
            int modelRow = studentTable.convertRowIndexToModel(studentRow);
            int roll = (Integer) studentTableModel.getValueAt(modelRow, 0);
            targetPerson = studentManager.getStudent(roll);
        } else if (!studentManager.getAllTeachers().isEmpty()) {
            targetPerson = studentManager.getAllTeachers().get(0);
        }

        if (targetPerson == null) {
            JOptionPane.showMessageDialog(this, "Please select a student or faculty member to view their profile.",
                    "No Record Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel profileCard = new JPanel(new BorderLayout(14, 14));
        profileCard.setBorder(new EmptyBorder(16, 18, 16, 18));
        profileCard.setBackground(themeManager.getCardBg());

        JPanel pTop = new JPanel(new BorderLayout());
        pTop.setOpaque(false);
        JLabel badgeLbl = new JLabel(targetPerson instanceof Student ? "SCHOLAR IDENTITY CARD" : "FACULTY ACADEMIC PROFILE");
        badgeLbl.setFont(ThemeManager.BADGE_FONT);
        badgeLbl.setForeground(ThemeManager.ACCENT_BLUE);
        pTop.add(badgeLbl, BorderLayout.WEST);

        JLabel nameLbl = new JLabel(targetPerson.getName());
        nameLbl.setFont(ThemeManager.TITLE_FONT);
        nameLbl.setForeground(themeManager.getTextColor());
        pTop.add(nameLbl, BorderLayout.SOUTH);
        profileCard.add(pTop, BorderLayout.NORTH);

        JPanel infoGrid = new JPanel(new GridLayout(4, 1, 0, 8));
        infoGrid.setOpaque(false);

        infoGrid.add(createProfileRow("Contact Phone:", targetPerson.getPhone()));
        infoGrid.add(createProfileRow("Age & Tenure:", targetPerson.getAge() + " years"));
        infoGrid.add(createProfileRow("Address / Location:", targetPerson.getAddress()));

        JLabel roleSummary = new JLabel("Summary: " + targetPerson.getRoleInfo());
        roleSummary.setFont(ThemeManager.SMALL_FONT);
        roleSummary.setForeground(themeManager.getSubtextColor());
        infoGrid.add(roleSummary);

        profileCard.add(infoGrid, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, profileCard, "Institutional Identity Profile", JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel createProfileRow(String label, String val) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(ThemeManager.BOLD_FIELD_FONT);
        l.setForeground(themeManager.getTextColor());
        JLabel v = new JLabel(val);
        v.setFont(ThemeManager.FIELD_FONT);
        v.setForeground(themeManager.getSubtextColor());
        row.add(l);
        row.add(v);
        return row;
    }

    // =========================================================================
    // MODERN FLOATING TOAST NOTIFICATION ENGINE
    // =========================================================================

    private JPanel toastPanel;
    private javax.swing.Timer toastTimer;

    public void showToast(String message, boolean isSuccess) {
        SwingUtilities.invokeLater(() -> {
            JLayeredPane layeredPane = getLayeredPane();
            if (layeredPane == null) return;

            if (toastTimer != null && toastTimer.isRunning()) {
                toastTimer.stop();
            }
            if (toastPanel != null && toastPanel.getParent() != null) {
                layeredPane.remove(toastPanel);
            }

            Color bg = themeManager.isDark() ? Color.decode("#0F172A") : Color.WHITE;
            Color borderCol = isSuccess ? ThemeManager.SUCCESS_GREEN : ThemeManager.ACCENT_BLUE;

            toastPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth();
                    int h = getHeight();
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, w, h, 18, 18);
                    g2.setColor(borderCol);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            toastPanel.setOpaque(false);
            toastPanel.setBorder(new EmptyBorder(3, 14, 3, 16));

            JLabel iconLbl = new JLabel(isSuccess ? "✓" : "ℹ");
            iconLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            iconLbl.setForeground(borderCol);
            toastPanel.add(iconLbl);

            JLabel textLbl = new JLabel(message);
            textLbl.setFont(ThemeManager.BOLD_FIELD_FONT);
            textLbl.setForeground(themeManager.getTextColor());
            toastPanel.add(textLbl);

            Dimension pref = toastPanel.getPreferredSize();
            int paneW = layeredPane.getWidth();
            int x = Math.max(10, (paneW - pref.width) / 2);
            int y = 56;
            toastPanel.setBounds(x, y, pref.width, pref.height);

            layeredPane.add(toastPanel, JLayeredPane.POPUP_LAYER);
            layeredPane.repaint();

            toastTimer = new javax.swing.Timer(2800, e -> {
                if (toastPanel != null && toastPanel.getParent() != null) {
                    layeredPane.remove(toastPanel);
                    layeredPane.repaint();
                }
            });
            toastTimer.setRepeats(false);
            toastTimer.start();
        });
    }

    // =========================================================================
    // IMPORT / EXPORT HANDLERS
    // =========================================================================

    private void onImportCsv() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Select Students CSV File");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            FileHandler.ImportResult result = FileHandler.importFromCsv(selectedFile, studentManager);

            refreshAllViews();

            StringBuilder msg = new StringBuilder();
            msg.append(String.format("Import Complete!\n\n• Successfully Imported: %d\n• Skipped Rows: %d\n",
                    result.getImportedCount(), result.getSkippedCount()));

            if (!result.getIssues().isEmpty()) {
                msg.append("\nNotices:\n");
                int limit = Math.min(5, result.getIssues().size());
                for (int i = 0; i < limit; i++) {
                    msg.append(" - ").append(result.getIssues().get(i)).append("\n");
                }
            }

            JOptionPane.showMessageDialog(this, msg.toString(), "CSV Import Summary",
                    result.getSkippedCount() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onExportCsv() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Save Students Dataset as CSV");
        chooser.setSelectedFile(new File("students_export.csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
                target = new File(target.getParentFile(), target.getName() + ".csv");
            }
            try {
                FileHandler.saveToCsv(target, studentManager);
                JOptionPane.showMessageDialog(this, "Dataset exported successfully to:\n" + target.getAbsolutePath(),
                        "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onExportReport() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Export Academic Audit Report");
        chooser.setSelectedFile(new File("Academic_Report.txt"));
        chooser.setFileFilter(new FileNameExtensionFilter("Text Documents (*.txt)", "txt"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase(Locale.ROOT).endsWith(".txt")) {
                target = new File(target.getParentFile(), target.getName() + ".txt");
            }
            try {
                FileHandler.exportPrintableReport(target, studentManager);
                JOptionPane.showMessageDialog(this, "Audit report generated successfully at:\n" + target.getAbsolutePath(),
                        "Report Created", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Report generation failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =========================================================================
    // REFRESH & THEME STYLING
    // =========================================================================

    private void applyFilters() {
        String keyword = searchField.getText().trim();
        String selectedCourse = (String) courseFilterCombo.getSelectedItem();

        RowFilter<DefaultTableModel, Object> rf = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ?> entry) {
                if (selectedCourse != null && !selectedCourse.equals("All Courses")) {
                    String courseInTable = entry.getStringValue(2);
                    if (!selectedCourse.equalsIgnoreCase(courseInTable)) {
                        return false;
                    }
                }
                if (!keyword.isEmpty()) {
                    String term = keyword.toLowerCase(Locale.ROOT);
                    for (int i = 0; i < entry.getValueCount(); i++) {
                        if (entry.getStringValue(i).toLowerCase(Locale.ROOT).contains(term)) {
                            return true;
                        }
                    }
                    return false;
                }
                return true;
            }
        };

        studentSorter.setRowFilter(rf);
        studentTable.repaint();
    }

    private void refreshAllViews() {
        refreshStudentTable();
        refreshTeacherTable();
        refreshDashboardStats();
        updateStatusBar();
    }

    private void refreshStudentTable() {
        studentTableModel.setRowCount(0);
        for (Student s : studentManager.getAllStudents()) {
            studentTableModel.addRow(new Object[]{
                    s.getRollNo(),
                    s.getName(),
                    s.getCourse(),
                    s.getMarks(),
                    s.getGrade(),
                    s.getStatus(),
                    s.getAge(),
                    s.getPhone(),
                    s.getAddress()
            });
        }
    }

    private void refreshTeacherTable() {
        teacherTableModel.setRowCount(0);
        for (Teacher t : studentManager.getAllTeachers()) {
            teacherTableModel.addRow(new Object[]{
                    t.getTeacherId(),
                    t.getName(),
                    t.getSubject(),
                    t.getAge(),
                    t.getPhone(),
                    t.getAddress()
            });
        }
        if (facultyCountBadge != null) {
            facultyCountBadge.setText(String.format(" %d Faculty Members ", studentManager.getAllTeachers().size()));
        }
    }

    private void refreshDashboardStats() {
        totalStudentsVal.setText(String.valueOf(studentManager.getTotalCount()));
        avgMarksVal.setText(String.format("%.1f", studentManager.getAverageMarks()));

        Student topper = studentManager.getTopper();
        if (topper != null) {
            topperVal.setText(String.format("%.1f", topper.getMarks()));
            topperSubtext.setText(String.format("%s (#%d - %s)", topper.getName(), topper.getRollNo(), topper.getCourse()));
        } else {
            topperVal.setText("N/A");
            topperSubtext.setText("No students enrolled");
        }

        passPctVal.setText(String.format("%.1f%%", studentManager.getPassPercentage()));
        int passCount = (int) Math.round(studentManager.getTotalCount() * (studentManager.getPassPercentage() / 100.0));
        passSubtext.setText(String.format("%d passed / %d failed", passCount, studentManager.getTotalCount() - passCount));

        if (barChartPanel != null) {
            barChartPanel.repaint();
        }

        if (courseStatsTableModel != null) {
            courseStatsTableModel.setRowCount(0);
            for (StudentManager.CourseStats cs : studentManager.getCourseBreakdown()) {
                courseStatsTableModel.addRow(new Object[]{
                        cs.getCourseName(),
                        cs.getEnrolledCount() + " scholars",
                        String.format("%.1f", cs.getAverageMarks()),
                        String.format("%.1f%%", cs.getPassPercentage()),
                        cs.getTopperInfo()
                });
            }
        }
    }

    private javax.swing.Timer countUpTimer;

    /**
     * Smooth animated count-up for stat cards and animated bar growth on dashboard.
     */
    public void startDashboardAnimations() {
        if (barChartPanel != null) {
            barChartPanel.startAnimation();
        }

        int targetTotal = studentManager.getTotalCount();
        double targetAvg = studentManager.getAverageMarks();
        double targetPass = studentManager.getPassPercentage();

        if (countUpTimer != null && countUpTimer.isRunning()) {
            countUpTimer.stop();
        }

        final long startTime = System.currentTimeMillis();
        final long duration = 600;

        countUpTimer = new javax.swing.Timer(16, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1.0f, (float) elapsed / duration);
            float eased = (float) (1.0 - Math.pow(1.0 - progress, 3)); // Cubic ease-out

            int currentTotal = Math.round(targetTotal * eased);
            double currentAvg = Math.round(targetAvg * eased * 10.0) / 10.0;
            double currentPass = Math.round(targetPass * eased * 10.0) / 10.0;

            totalStudentsVal.setText(String.valueOf(currentTotal));
            avgMarksVal.setText(String.format("%.1f", currentAvg));
            passPctVal.setText(String.format("%.1f%%", currentPass));

            if (progress >= 1.0f) {
                ((javax.swing.Timer) e.getSource()).stop();
                refreshDashboardStats();
            }
        });
        countUpTimer.start();
    }

    private void updateStatusBar() {
        if (statusBarLabel != null) {
            statusBarLabel.setText(String.format("● Total Enrolled: %d   |   📊 Average Score: %.2f   |   🛡️ Pass Rate: %.1f%%",
                    studentManager.getTotalCount(),
                    studentManager.getAverageMarks(),
                    studentManager.getPassPercentage()));
            statusBarLabel.setForeground(themeManager.isDark() ? Color.decode("#94A3B8") : Color.decode("#475569"));
        }
    }

    private void updateStatusBarTheme() {
        boolean dark = themeManager.isDark();
        if (statusBarPanel != null) {
            statusBarPanel.setBackground(themeManager.getMainBg());
            statusBarPanel.setBorder(new CompoundBorder(
                    new MatteBorder(1, 0, 0, 0, themeManager.getCardBorder()),
                    new EmptyBorder(8, 20, 8, 20)
            ));
        }
        if (statusBarLabel != null) {
            statusBarLabel.setForeground(dark ? Color.decode("#94A3B8") : Color.decode("#475569"));
        }
        for (JLabel keyLbl : keycapLabels) {
            keyLbl.setBackground(dark ? Color.decode("#1E293B") : Color.decode("#E2E8F0"));
            keyLbl.setForeground(dark ? Color.decode("#93C5FD") : Color.decode("#1D4ED8"));
            keyLbl.setBorder(new CompoundBorder(
                    new LineBorder(dark ? Color.decode("#334155") : Color.decode("#CBD5E1"), 1, true),
                    new EmptyBorder(2, 6, 2, 6)
            ));
        }
        for (JLabel actLbl : keycapActionLabels) {
            actLbl.setForeground(dark ? Color.decode("#94A3B8") : Color.decode("#64748B"));
        }
    }

    private void updateWindowTitleBarTheme() {
        boolean dark = themeManager.isDark();
        Color titleBg = themeManager.getHeaderBg();
        Color titleFg = dark ? Color.WHITE : Color.decode("#0F172A");

        JRootPane root = getRootPane();
        if (root != null) {
            root.putClientProperty("JRootPane.titleBarBackground", titleBg);
            root.putClientProperty("JRootPane.titleBarForeground", titleFg);
            root.putClientProperty("JRootPane.titleBarTheme", dark ? "dark" : "light");
        }

        UIManager.put("TitlePane.background", titleBg);
        UIManager.put("TitlePane.foreground", titleFg);
        UIManager.put("TitlePane.inactiveForeground", dark ? Color.decode("#64748B") : Color.decode("#94A3B8"));
        UIManager.put("TitlePane.buttonHoverBackground", dark ? Color.decode("#1E293B") : Color.decode("#E2E8F0"));
    }

    private void applyThemeStyles() {
        themeManager.applyThemeRecursively(this);
        updateHeaderTheme();
        updateStatusBarTheme();
        updateWindowTitleBarTheme();

        if (tabbedPane != null && tabbedPane.getTabCount() >= 3) {
            Color tabIconColor = themeManager.isDark() ? themeManager.getTextColor() : Color.decode("#475569");
            tabbedPane.setIconAt(0, IconFactory.createGraduationCapIcon(18, tabIconColor));
            tabbedPane.setIconAt(1, IconFactory.createAnalyticsIcon(18, tabIconColor));
            tabbedPane.setIconAt(2, IconFactory.createFacultyIcon(18, tabIconColor));
        }
        if (facultyCountBadge != null) {
            facultyCountBadge.setText(String.format(" %d Faculty Members ", studentManager.getAllTeachers().size()));
        }
        repaint();
    }

    private void showAboutDialog() {
        JPanel aboutCard = new JPanel(new GridLayout(5, 1, 0, 4));
        aboutCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel pName = new JLabel("UniDesk Campus Management System");
        pName.setFont(ThemeManager.BOLD_FIELD_FONT);
        JLabel pRole = new JLabel("Academic Department of Computer Applications");
        JLabel pAuthor = new JLabel("Modular Architecture: Separation of Logic, UI & Persistence");
        JLabel pPlatform = new JLabel("Engine: Java Swing (Modern Flat Interface)");
        JLabel pNotice = new JLabel("Production Ready Campus Academic System");
        pNotice.setFont(ThemeManager.SMALL_FONT);

        aboutCard.add(pName);
        aboutCard.add(pRole);
        aboutCard.add(pAuthor);
        aboutCard.add(pPlatform);
        aboutCard.add(pNotice);

        JOptionPane.showMessageDialog(this, aboutCard, "About UniDesk", JOptionPane.PLAIN_MESSAGE, IconFactory.createUniDeskLogo(54));
    }

    private int parseInteger(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid integer.");
        }
    }

    private double parseDouble(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid numerical value.");
        }
    }

    // =========================================================================
    // BADGE CELL RENDERER
    // =========================================================================

    /**
     * Reusable pill badge component for high-fidelity status, grade, and course chips.
     */
    public static class BadgeLabel extends JLabel {
        private Color badgeBg = Color.GRAY;
        private Color badgeBorder = null;
        private Color badgeFg = Color.WHITE;
        private boolean isPill = true;

        public BadgeLabel() {
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(ThemeManager.BADGE_FONT);
        }

        public void setBadge(String text, Color bg, Color border, Color fg, boolean pill) {
            setText(text);
            this.badgeBg = bg;
            this.badgeBorder = border;
            this.badgeFg = fg;
            this.isPill = pill;
            setForeground(fg);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            FontMetrics fm = g2.getFontMetrics(getFont());
            String text = getText() != null ? getText() : "";
            int textW = fm.stringWidth(text);
            int badgeW = Math.min(w - 10, textW + 20);
            int badgeX = (w - badgeW) / 2;
            int badgeH = h - 10;
            int badgeY = 5;
            int arc = isPill ? badgeH : 8;

            if (badgeW > 0 && badgeH > 0) {
                g2.setColor(badgeBg);
                g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, arc, arc);

                if (badgeBorder != null) {
                    g2.setColor(badgeBorder);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(badgeX, badgeY, badgeW, badgeH, arc, arc);
                }
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private class StudentCellRenderer extends DefaultTableCellRenderer {
        private final BadgeLabel badge = new BadgeLabel();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            int modelRow = table.convertRowIndexToModel(row);
            int roll = (Integer) studentTableModel.getValueAt(modelRow, 0);
            String status = (String) studentTableModel.getValueAt(modelRow, 5);

            String selectedFilter = (courseFilterCombo != null) ? (String) courseFilterCombo.getSelectedItem() : null;
            Student streamTopper = null;
            if (selectedFilter != null && !selectedFilter.equalsIgnoreCase("All Courses")) {
                streamTopper = studentManager.getTopperByCourse(selectedFilter);
            }
            boolean isTopper = (streamTopper != null && streamTopper.getRollNo() == roll);

            // Column 5: Status Pill Badge
            if (column == 5) {
                boolean isPass = "Pass".equalsIgnoreCase(status);
                Color bg = isPass ? (themeManager.isDark() ? new Color(6, 78, 59, 210) : new Color(220, 252, 231))
                                  : (themeManager.isDark() ? new Color(69, 10, 10, 210) : new Color(254, 226, 226));
                Color border = isPass ? (themeManager.isDark() ? new Color(16, 185, 129, 90) : new Color(134, 239, 172))
                                      : (themeManager.isDark() ? new Color(239, 68, 68, 90) : new Color(252, 165, 165));
                Color fg = isPass ? (themeManager.isDark() ? Color.decode("#34D399") : Color.decode("#15803D"))
                                  : (themeManager.isDark() ? Color.decode("#F87171") : Color.decode("#B91C1C"));
                badge.setBadge(isPass ? "● Pass" : "● Fail", bg, border, fg, true);
                return badge;
            }

            // Column 4: Grade Capsule Badge
            if (column == 4) {
                String gr = value != null ? value.toString() : "";
                Color bg, border, fg;
                if ("A+".equals(gr)) {
                    bg = themeManager.isDark() ? new Color(6, 78, 59, 210) : new Color(209, 250, 229);
                    border = themeManager.isDark() ? new Color(16, 185, 129, 100) : new Color(110, 231, 183);
                    fg = themeManager.isDark() ? Color.decode("#34D399") : Color.decode("#047857");
                } else if ("A".equals(gr)) {
                    bg = themeManager.isDark() ? new Color(30, 58, 138, 210) : new Color(219, 234, 254);
                    border = themeManager.isDark() ? new Color(59, 130, 246, 100) : new Color(147, 197, 253);
                    fg = themeManager.isDark() ? Color.decode("#60A5FA") : Color.decode("#1D4ED8");
                } else if ("B".equals(gr)) {
                    bg = themeManager.isDark() ? new Color(49, 46, 129, 210) : new Color(224, 231, 255);
                    border = themeManager.isDark() ? new Color(99, 102, 241, 100) : new Color(165, 180, 252);
                    fg = themeManager.isDark() ? Color.decode("#A5B4FC") : Color.decode("#4338CA");
                } else if ("C".equals(gr)) {
                    bg = themeManager.isDark() ? new Color(120, 53, 15, 210) : new Color(254, 243, 199);
                    border = themeManager.isDark() ? new Color(245, 158, 11, 100) : new Color(252, 211, 77);
                    fg = themeManager.isDark() ? Color.decode("#FBBF24") : Color.decode("#B45309");
                } else if ("D".equals(gr)) {
                    bg = themeManager.isDark() ? new Color(124, 45, 18, 210) : new Color(255, 237, 213);
                    border = themeManager.isDark() ? new Color(251, 146, 60, 100) : new Color(253, 186, 116);
                    fg = themeManager.isDark() ? Color.decode("#FB923C") : Color.decode("#C2410C");
                } else {
                    bg = themeManager.isDark() ? new Color(69, 10, 10, 210) : new Color(254, 226, 226);
                    border = themeManager.isDark() ? new Color(239, 68, 68, 100) : new Color(252, 165, 165);
                    fg = themeManager.isDark() ? Color.decode("#F87171") : Color.decode("#B91C1C");
                }
                badge.setBadge(gr, bg, border, fg, true);
                return badge;
            }

            // Column 2: Course Chip Badge
            if (column == 2) {
                String crs = value != null ? value.toString() : "";
                Color bg = themeManager.isDark() ? new Color(30, 41, 59, 190) : new Color(241, 245, 249);
                Color border = themeManager.isDark() ? new Color(71, 85, 105, 100) : new Color(203, 213, 225);
                Color fg = themeManager.isDark() ? Color.decode("#E2E8F0") : Color.decode("#334155");
                badge.setBadge(crs, bg, border, fg, false);
                return badge;
            }

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            JLabel lbl = (JLabel) c;
            lbl.setIcon(null);

            // Column 1: Student Name with Topper Decoration
            if (column == 1) {
                lbl.setFont(ThemeManager.BOLD_FIELD_FONT);
                if (isTopper) {
                    lbl.setIcon(IconFactory.createTrophyIcon(16, themeManager.isDark() ? Color.decode("#FBBF24") : Color.decode("#D97706")));
                    lbl.setIconTextGap(8);
                    lbl.setText(value + "  [TOPPER]");
                    if (!isSelected) {
                        lbl.setForeground(themeManager.isDark() ? Color.decode("#FBBF24") : Color.decode("#B45309"));
                    }
                } else {
                    lbl.setIcon(null);
                    lbl.setText(value != null ? value.toString() : "");
                    if (!isSelected) lbl.setForeground(themeManager.getTextColor());
                }
            } else if (column == 0 || column == 3 || column == 6) {
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                if (column == 0 && !isSelected) {
                    lbl.setForeground(themeManager.getSubtextColor());
                    lbl.setFont(ThemeManager.BADGE_FONT);
                }
            } else {
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
            }

            applyRowBgAndBorder(lbl, isSelected, isTopper, row, column);
            return lbl;
        }

        private void applyRowBgAndBorder(JLabel lbl, boolean isSelected, boolean isTopper, int row, int column) {
            if (isSelected) {
                lbl.setBackground(themeManager.getTableSelectionBg());
                lbl.setForeground(themeManager.getTableSelectionFg());
            } else if (isTopper) {
                lbl.setBackground(themeManager.getTopperRowBg());
            } else if (row == hoveredStudentRow) {
                lbl.setBackground(themeManager.isDark() ? Color.decode("#152033") : Color.decode("#EEF2FF"));
                lbl.setForeground(themeManager.getTextColor());
            } else {
                lbl.setBackground(row % 2 == 0 ? themeManager.getTableBg() : themeManager.getTableAltBg());
                lbl.setForeground(themeManager.getTableFg());
            }

            if (column == 0 && row == hoveredStudentRow && !isSelected) {
                lbl.setBorder(new CompoundBorder(
                        new MatteBorder(0, 3, 0, 0, ThemeManager.ACCENT_BLUE),
                        new EmptyBorder(0, 7, 0, 10)
                ));
            } else if (column == 0 && isTopper && !isSelected) {
                lbl.setBorder(new CompoundBorder(
                        new MatteBorder(0, 3, 0, 0, Color.decode("#F59E0B")),
                        new EmptyBorder(0, 7, 0, 10)
                ));
            } else {
                lbl.setBorder(new EmptyBorder(0, 10, 0, 10));
            }
        }
    }

    private class TeacherCellRenderer extends DefaultTableCellRenderer {
        private final BadgeLabel badge = new BadgeLabel();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            // Column 2: Specialization Chip
            if (column == 2) {
                String spec = value != null ? value.toString() : "";
                Color bg = themeManager.isDark() ? new Color(49, 46, 129, 180) : new Color(224, 231, 255);
                Color border = themeManager.isDark() ? new Color(99, 102, 241, 100) : new Color(165, 180, 252);
                Color fg = themeManager.isDark() ? Color.decode("#C7D2FE") : Color.decode("#4338CA");
                badge.setBadge(spec, bg, border, fg, false);
                return badge;
            }

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            JLabel lbl = (JLabel) c;

            if (isSelected) {
                lbl.setBackground(themeManager.getTableSelectionBg());
                lbl.setForeground(themeManager.getTableSelectionFg());
            } else {
                if (row == hoveredTeacherRow) {
                    lbl.setBackground(themeManager.isDark() ? Color.decode("#152033") : Color.decode("#EEF2FF"));
                } else {
                    lbl.setBackground(row % 2 == 0 ? themeManager.getTableBg() : themeManager.getTableAltBg());
                }
                lbl.setForeground(themeManager.getTableFg());
            }

            if (column == 0 || column == 3 || column == 4) {
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
            }

            if (column == 0 && row == hoveredTeacherRow && !isSelected) {
                lbl.setBorder(new CompoundBorder(
                        new MatteBorder(0, 3, 0, 0, Color.decode("#7C3AED")),
                        new EmptyBorder(0, 7, 0, 10)
                ));
            } else {
                lbl.setBorder(new EmptyBorder(0, 10, 0, 10));
            }

            return lbl;
        }
    }

    private class CourseStatsCellRenderer extends DefaultTableCellRenderer {
        private final BadgeLabel badge = new BadgeLabel();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            // Column 3: Pass Percentage Pill Badge
            if (column == 3) {
                String valStr = value != null ? value.toString() : "";
                double pct = 0.0;
                try {
                    pct = Double.parseDouble(valStr.replace("%", "").trim());
                } catch (Exception ignored) {}
                boolean highPass = pct >= 80.0;
                Color bg = highPass ? (themeManager.isDark() ? new Color(6, 78, 59, 210) : new Color(220, 252, 231))
                                    : (themeManager.isDark() ? new Color(120, 53, 15, 210) : new Color(254, 243, 199));
                Color border = highPass ? (themeManager.isDark() ? new Color(16, 185, 129, 90) : new Color(134, 239, 172))
                                        : (themeManager.isDark() ? new Color(245, 158, 11, 90) : new Color(252, 211, 77));
                Color fg = highPass ? (themeManager.isDark() ? Color.decode("#34D399") : Color.decode("#15803D"))
                                    : (themeManager.isDark() ? Color.decode("#FBBF24") : Color.decode("#B45309"));
                badge.setBadge(valStr, bg, border, fg, true);
                return badge;
            }

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            JLabel lbl = (JLabel) c;

            if (isSelected) {
                lbl.setBackground(themeManager.getTableSelectionBg());
                lbl.setForeground(themeManager.getTableSelectionFg());
            } else {
                if (row == hoveredCourseStatsRow) {
                    lbl.setBackground(themeManager.isDark() ? Color.decode("#152033") : Color.decode("#EEF2FF"));
                } else {
                    lbl.setBackground(row % 2 == 0 ? themeManager.getTableBg() : themeManager.getTableAltBg());
                }
                lbl.setForeground(themeManager.getTableFg());
            }

            if (column == 4) {
                lbl.setFont(ThemeManager.BOLD_FIELD_FONT);
                lbl.setIcon(IconFactory.createTrophyIcon(15, themeManager.isDark() ? Color.decode("#FBBF24") : Color.decode("#D97706")));
                lbl.setIconTextGap(6);
                lbl.setText(value != null ? value.toString() : "N/A");
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                if (!isSelected) {
                    lbl.setForeground(themeManager.isDark() ? Color.decode("#FBBF24") : Color.decode("#B45309"));
                }
            } else {
                lbl.setIcon(null);
                if (column == 0) {
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                } else {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                }
            }

            if (column == 0 && row == hoveredCourseStatsRow && !isSelected) {
                lbl.setBorder(new CompoundBorder(
                        new MatteBorder(0, 3, 0, 0, ThemeManager.ACCENT_BLUE),
                        new EmptyBorder(0, 5, 0, 5)
                ));
            } else {
                lbl.setBorder(new EmptyBorder(0, 8, 0, 8));
            }

            return lbl;
        }
    }

    private class ModernHeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBackground(themeManager.getTableHeaderBg());
            setForeground(themeManager.getTableHeaderFg());
            setFont(ThemeManager.BADGE_FONT);
            if (value != null) {
                setText(value.toString().toUpperCase(Locale.ROOT));
            }
            if (column == 0 || column == 2 || column == 3 || column == 4 || column == 5 || column == 6) {
                setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }
            setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, themeManager.isDark() ? Color.decode("#1E293B") : Color.decode("#E2E8F0")),
                    new EmptyBorder(8, 12, 8, 12)
            ));
            return this;
        }
    }

    // =========================================================================
    // MAIN ENTRY POINT & LOOK AND FEEL INITIALIZATION
    // =========================================================================

    public static void main(String[] args) {
        ThemeManager tm = new ThemeManager();
        setupLookAndFeel(tm.isDark());

        SwingUtilities.invokeLater(() -> {
            try {
                StudentManager sm = new StudentManager();
                FileHandler.initializeData(sm);

                MainFrame frame = new MainFrame(sm, tm);
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Startup error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void setupLookAndFeel(boolean isDark) {
        boolean applied = false;
        String lafClass = isDark ? "com.formdev.flatlaf.FlatDarkLaf" : "com.formdev.flatlaf.FlatLightLaf";
        try {
            Class<?> clazz = Class.forName(lafClass);
            java.lang.reflect.Method setup = clazz.getMethod("setup");
            setup.invoke(null);
            JFrame.setDefaultLookAndFeelDecorated(true);
            applied = true;
            System.out.println("[UniDesk] FlatLaf modern Look & Feel (" + (isDark ? "Dark" : "Light") + ") initialized.");
        } catch (Throwable ignored) {}

        if (!applied) {
            try {
                File libDir = new File("lib");
                if (libDir.exists() && libDir.isDirectory()) {
                    File[] jars = libDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar") && name.toLowerCase().contains("flatlaf"));
                    if (jars != null && jars.length > 0) {
                        URLClassLoader loader = new URLClassLoader(new URL[]{jars[0].toURI().toURL()}, MainFrame.class.getClassLoader());
                        Class<?> clazz = Class.forName(lafClass, true, loader);
                        Object laf = clazz.getDeclaredConstructor().newInstance();
                        if (laf instanceof LookAndFeel) {
                            UIManager.setLookAndFeel((LookAndFeel) laf);
                            JFrame.setDefaultLookAndFeelDecorated(true);
                            applied = true;
                            System.out.println("[UniDesk] FlatLaf dynamically loaded from " + jars[0].getName());
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (!applied) {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equalsIgnoreCase(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        applied = true;
                        System.out.println("[UniDesk] Note: FlatLaf unavailable, defaulted to Nimbus Look & Feel.");
                        break;
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (!applied) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                System.out.println("[UniDesk] Defaulted to System Look & Feel: " + UIManager.getLookAndFeel().getName());
            } catch (Throwable ignored) {}
        }
    }
}

/**
 * ============================================================================
 * [THEME & STYLING ENGINE]
 * ============================================================================
 */
class ThemeManager {

    private static final String SETTINGS_FILE = "settings.properties";
    private static final String PROP_THEME = "theme";

    private boolean isDark;
    private final List<Runnable> listeners = new ArrayList<>();

    public static final Font HEADER_FONT = resolveFont("Segoe UI", Font.BOLD, 20);
    public static final Font TITLE_FONT = resolveFont("Segoe UI", Font.BOLD, 15);
    public static final Font SUBHEADER_FONT = resolveFont("Segoe UI", Font.PLAIN, 13);
    public static final Font FIELD_FONT = resolveFont("Segoe UI", Font.PLAIN, 13);
    public static final Font BOLD_FIELD_FONT = resolveFont("Segoe UI", Font.BOLD, 13);
    public static final Font TABLE_FONT = resolveFont("Segoe UI", Font.PLAIN, 13);
    public static final Font SMALL_FONT = resolveFont("Segoe UI", Font.PLAIN, 12);
    public static final Font BADGE_FONT = resolveFont("Segoe UI", Font.BOLD, 11);
    public static final Font STAT_VALUE_FONT = resolveFont("Segoe UI", Font.BOLD, 28);

    public static final Color ACCENT_BLUE = Color.decode("#2563EB");
    public static final Color SUCCESS_GREEN = Color.decode("#10B981");
    public static final Color DANGER_RED = Color.decode("#EF4444");
    public static final Color WARNING_AMBER = Color.decode("#F59E0B");
    public static final Color TOPPER_GOLD = Color.decode("#D97706");

    public ThemeManager() {
        this.isDark = loadThemePreference();
    }

    public boolean isDark() {
        return isDark;
    }

    public void setDark(boolean dark) {
        if (this.isDark != dark) {
            this.isDark = dark;
            saveThemePreference(dark);
            try {
                String lafName = dark ? "com.formdev.flatlaf.FlatDarkLaf" : "com.formdev.flatlaf.FlatLightLaf";
                Class<?> lafClass = Class.forName(lafName);
                java.lang.reflect.Method setupMethod = lafClass.getMethod("setup");
                setupMethod.invoke(null);
            } catch (Throwable ignored) {}
            notifyListeners();
        }
    }

    public void toggleTheme() {
        setDark(!isDark);
    }

    public void addThemeChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Color getHeaderBg() {
        return isDark ? Color.decode("#0B0F19") : Color.WHITE;
    }

    public Color getMainBg() {
        return isDark ? Color.decode("#060911") : Color.decode("#F8FAFC");
    }

    public Color getCardBg() {
        return isDark ? Color.decode("#0F172A") : Color.WHITE;
    }

    public Color getCardBorder() {
        return isDark ? Color.decode("#1E293B") : Color.decode("#E2E8F0");
    }

    public Color getTextColor() {
        return isDark ? Color.decode("#F8FAFC") : Color.decode("#0F172A");
    }

    public Color getSubtextColor() {
        return isDark ? Color.decode("#94A3B8") : Color.decode("#64748B");
    }

    public Color getInputBg() {
        return isDark ? Color.decode("#090D16") : Color.WHITE;
    }

    public Color getInputFg() {
        return isDark ? Color.decode("#F8FAFC") : Color.decode("#0F172A");
    }

    public Color getInputBorder() {
        return isDark ? Color.decode("#1E293B") : Color.decode("#CBD5E1");
    }

    public Color getTableBg() {
        return isDark ? Color.decode("#0F172A") : Color.WHITE;
    }

    public Color getTableAltBg() {
        return isDark ? Color.decode("#0B1120") : Color.decode("#F8FAFC");
    }

    public Color getTableFg() {
        return isDark ? Color.decode("#E2E8F0") : Color.decode("#1E293B");
    }

    public Color getTableSelectionBg() {
        return isDark ? Color.decode("#1E3A8A") : Color.decode("#E0E7FF");
    }

    public Color getTableSelectionFg() {
        return isDark ? Color.WHITE : Color.decode("#1E1B4B");
    }

    public Color getTableHeaderBg() {
        return isDark ? Color.decode("#090E1A") : Color.decode("#F1F5F9");
    }

    public Color getTableHeaderFg() {
        return isDark ? Color.decode("#94A3B8") : Color.decode("#475569");
    }

    public Color getTopperRowBg() {
        return isDark ? Color.decode("#1A180E") : Color.decode("#FEFCE8");
    }

    public Color getPassBadgeFg() {
        return isDark ? Color.decode("#34D399") : Color.decode("#15803D");
    }

    public Color getFailBadgeFg() {
        return isDark ? Color.decode("#F87171") : Color.decode("#B91C1C");
    }

    private boolean loadThemePreference() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(in);
                return "dark".equalsIgnoreCase(props.getProperty(PROP_THEME, "light"));
            } catch (Exception ignored) {}
        }
        return false;
    }

    private void saveThemePreference(boolean dark) {
        try (FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
            Properties props = new Properties();
            props.setProperty(PROP_THEME, dark ? "dark" : "light");
            props.store(out, "SIS Settings");
        } catch (Exception e) {
            System.err.println("Could not save theme preference: " + e.getMessage());
        }
    }

    public static void styleFlatButton(JButton button, Color baseBg, Color fgColor, Color hoverBg) {
        button.setUI(new ModernFlatButtonUI(baseBg, fgColor, hoverBg));
    }

    /**
     * Advanced Button UI Delegate providing 60fps hover color transitions,
     * anti-aliased rounded corners, ambient drop-shadow, and micro-elevation.
     */
    public static class ModernFlatButtonUI extends BasicButtonUI {
        private final Color baseBg;
        private final Color fgColor;
        private final Color hoverBg;
        private float hoverFactor = 0.0f;
        private javax.swing.Timer timer;

        public ModernFlatButtonUI(Color baseBg, Color fgColor, Color hoverBg) {
            this.baseBg = baseBg;
            this.fgColor = fgColor;
            this.hoverBg = hoverBg;
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            AbstractButton b = (AbstractButton) c;
            b.setOpaque(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setContentAreaFilled(false);
            b.setForeground(fgColor);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            b.setBorder(new EmptyBorder(8, 14, 8, 14));
            b.setFont(BOLD_FIELD_FONT);
        }

        @Override
        protected void installListeners(AbstractButton b) {
            super.installListeners(b);
            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (b.isEnabled()) startAnimation(b, true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    startAnimation(b, false);
                }
            });
        }

        private void startAnimation(AbstractButton b, boolean forward) {
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            timer = new javax.swing.Timer(12, ev -> {
                if (forward) {
                    hoverFactor += 0.14f;
                    if (hoverFactor >= 1.0f) {
                        hoverFactor = 1.0f;
                        timer.stop();
                    }
                } else {
                    hoverFactor -= 0.14f;
                    if (hoverFactor <= 0.0f) {
                        hoverFactor = 0.0f;
                        timer.stop();
                    }
                }
                b.repaint();
            });
            timer.start();
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = b.getWidth();
            int h = b.getHeight();
            boolean pressed = b.getModel().isPressed();

            int r = (int) (baseBg.getRed() + (hoverBg.getRed() - baseBg.getRed()) * hoverFactor);
            int gr = (int) (baseBg.getGreen() + (hoverBg.getGreen() - baseBg.getGreen()) * hoverFactor);
            int bl = (int) (baseBg.getBlue() + (hoverBg.getBlue() - baseBg.getBlue()) * hoverFactor);
            Color currentBg = new Color(r, gr, bl);

            int arc = 10;
            int yOffset = pressed ? 1 : (hoverFactor > 0.1f ? -1 : 0);

            // Ambient Drop Shadow on Hover
            if (hoverFactor > 0.05f && !pressed) {
                int shadowAlpha = (int) (38 * hoverFactor);
                g2.setColor(new Color(0, 0, 0, shadowAlpha));
                g2.fillRoundRect(1, 3, w - 2, h - 3, arc, arc);
            }

            // Button Body Shape
            g2.setColor(currentBg);
            g2.fillRoundRect(0, yOffset > 0 ? yOffset : 0, w, h + (yOffset < 0 ? yOffset : 0), arc, arc);

            // Highlight Border Outline
            if (hoverFactor > 0.05f) {
                int borderAlpha = (int) (90 * hoverFactor);
                g2.setColor(new Color(255, 255, 255, borderAlpha));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, yOffset > 0 ? yOffset : 0, w - 1, h + (yOffset < 0 ? yOffset : 0) - 1, arc, arc);
            }

            g2.dispose();
            super.paint(g, c);
        }
    }

    public Border createCardBorder() {
        return new CompoundBorder(
                new LineBorder(getCardBorder(), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        );
    }

    public Border createDefaultInputBorder() {
        return new CompoundBorder(
                new LineBorder(getInputBorder(), 1, true),
                new EmptyBorder(4, 9, 4, 9)
        );
    }

    public Border createFocusedInputBorder() {
        return new CompoundBorder(
                new LineBorder(ACCENT_BLUE, 2, true),
                new EmptyBorder(3, 8, 3, 8)
        );
    }

    public Border createHoverInputBorder() {
        return new CompoundBorder(
                new LineBorder(isDark ? Color.decode("#334155") : Color.decode("#94A3B8"), 1, true),
                new EmptyBorder(4, 9, 4, 9)
        );
    }

    public void styleInputField(JComponent comp) {
        comp.setFont(FIELD_FONT);
        comp.setBackground(getInputBg());
        comp.setForeground(getInputFg());
        comp.setBorder(comp.hasFocus() ? createFocusedInputBorder() : createDefaultInputBorder());

        if (!Boolean.TRUE.equals(comp.getClientProperty("styledListenersInstalled"))) {
            comp.putClientProperty("styledListenersInstalled", Boolean.TRUE);

            comp.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    comp.setBorder(createFocusedInputBorder());
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    comp.setBorder(createDefaultInputBorder());
                }
            });

            comp.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!comp.hasFocus()) comp.setBorder(createHoverInputBorder());
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!comp.hasFocus()) comp.setBorder(createDefaultInputBorder());
                }
            });
        }

        if (comp instanceof JTextField) {
            ((JTextField) comp).setCaretColor(getInputFg());
        } else if (comp instanceof JComboBox) {
            JComboBox<?> combo = (JComboBox<?>) comp;
            combo.setUI(new ModernComboBoxUI(this));
            combo.setRenderer(new ModernComboRenderer(this));
        }
    }

    public void styleModernScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(new LineBorder(getCardBorder(), 1, true));
        scrollPane.getViewport().setBackground(getTableBg());
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI(this));
        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI(this));
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 8));
    }

    public void styleFormScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI(this));
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    }

    public static class ModernComboBoxUI extends BasicComboBoxUI {
        private final ThemeManager themeManager;

        public ModernComboBoxUI(ThemeManager tm) {
            this.themeManager = tm;
        }

        @Override
        protected JButton createArrowButton() {
            JButton btn = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth();
                    int h = getHeight();
                    g2.setColor(themeManager.getInputBg());
                    g2.fillRect(0, 0, w, h);

                    // Sleek Chevron
                    g2.setColor(themeManager.getSubtextColor());
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = w / 2 - 2;
                    int cy = h / 2 - 1;
                    g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
                    g2.drawLine(cx, cy + 2, cx + 4, cy - 2);
                    g2.dispose();
                }
            };
            btn.setBorder(BorderFactory.createEmptyBorder());
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setOpaque(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return btn;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(themeManager.getInputBg());
            g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            g2.dispose();
        }
    }

    public static class ModernComboRenderer extends DefaultListCellRenderer {
        private final ThemeManager themeManager;

        public ModernComboRenderer(ThemeManager tm) {
            this.themeManager = tm;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setFont(ThemeManager.FIELD_FONT);
            setBorder(new EmptyBorder(8, 12, 8, 12));
            if (isSelected) {
                setBackground(ThemeManager.ACCENT_BLUE);
                setForeground(Color.WHITE);
            } else {
                setBackground(themeManager.getInputBg());
                setForeground(themeManager.getInputFg());
            }
            return this;
        }
    }

    public static class ModernScrollBarUI extends BasicScrollBarUI {
        private final ThemeManager themeManager;

        public ModernScrollBarUI(ThemeManager tm) {
            this.themeManager = tm;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
        @Override
        protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }

        private JButton createZeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(themeManager.getMainBg());
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(themeManager.isDark() ? (isThumbRollover() ? Color.decode("#475569") : Color.decode("#334155"))
                                              : (isThumbRollover() ? Color.decode("#94A3B8") : Color.decode("#CBD5E1")));
            g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 1, thumbBounds.width - 2, thumbBounds.height - 2, 6, 6);
            g2.dispose();
        }
    }

    public static class ModernTabbedPaneUI extends BasicTabbedPaneUI {
        private final ThemeManager themeManager;

        public ModernTabbedPaneUI(ThemeManager tm) {
            this.themeManager = tm;
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(8, 20, 8, 20);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            contentBorderInsets = new Insets(0, 0, 0, 0);
        }

        @Override
        protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(themeManager.getHeaderBg());
            g2.fillRect(0, 0, tabPane.getWidth(), tabPane.getHeight());
            // Subtle hairline
            g2.setColor(themeManager.getCardBorder());
            int tabAreaH = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
            g2.drawLine(0, tabAreaH - 1, tabPane.getWidth(), tabAreaH - 1);
            g2.dispose();
            super.paintTabArea(g, tabPlacement, selectedIndex);
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected) {
                // Soft elevated pill
                g2.setColor(themeManager.isDark() ? new Color(37, 99, 235, 40) : new Color(239, 246, 255));
                g2.fillRoundRect(x + 4, y + 4, w - 8, h - 8, 8, 8);
                // Electric Blue bottom indicator bar
                g2.setColor(ThemeManager.ACCENT_BLUE);
                g2.fillRoundRect(x + 12, y + h - 3, w - 24, 3, 2, 2);
            } else if (tabIndex == getRolloverTab()) {
                g2.setColor(themeManager.isDark() ? new Color(255, 255, 255, 12) : new Color(241, 245, 249));
                g2.fillRoundRect(x + 4, y + 4, w - 8, h - 8, 8, 8);
            }
            g2.dispose();
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            // Suppress default Swing 3D white highlight bevel borders
            if (isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(themeManager.isDark() ? new Color(59, 130, 246, 70) : new Color(191, 219, 254));
                g2.drawRoundRect(x + 4, y + 4, w - 8, h - 8, 8, 8);
                g2.dispose();
            }
        }

        @Override
        protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(isSelected ? ThemeManager.BOLD_FIELD_FONT : ThemeManager.FIELD_FONT);
            g2.setColor(isSelected ? (themeManager.isDark() ? Color.WHITE : Color.decode("#1E40AF")) : themeManager.getSubtextColor());
            g2.drawString(title, textRect.x, textRect.y + metrics.getAscent());
            g2.dispose();
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {}

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {}
    }

    public void applyThemeRecursively(Component component) {
        if (component == null) return;

        if (component instanceof JComponent && Boolean.TRUE.equals(((JComponent) component).getClientProperty("isHeaderBar"))) {
            component.setBackground(getHeaderBg());
        }

        if (component instanceof JPanel) {
            JPanel panel = (JPanel) component;
            String panelType = (String) panel.getClientProperty("panelType");
            if ("card".equals(panelType)) {
                panel.setBackground(getCardBg());
                panel.setBorder(createCardBorder());
            } else if ("root".equals(panelType) || "mainBg".equals(panelType)) {
                panel.setBackground(getMainBg());
            }
        } else if (component instanceof JLabel) {
            JLabel label = (JLabel) component;
            String role = (String) label.getClientProperty("role");
            if ("subtext".equals(role)) {
                label.setForeground(getSubtextColor());
            } else if (!"header".equals(role)) {
                label.setForeground(getTextColor());
            }
        } else if (component instanceof JTextField || component instanceof JComboBox) {
            styleInputField((JComponent) component);
        } else if (component instanceof JTable) {
            JTable table = (JTable) component;
            table.setBackground(getTableBg());
            table.setForeground(getTableFg());
            table.setSelectionBackground(getTableSelectionBg());
            table.setSelectionForeground(getTableSelectionFg());
            table.setGridColor(isDark ? Color.decode("#1E293B") : Color.decode("#F1F5F9"));

            JTableHeader header = table.getTableHeader();
            if (header != null) {
                header.setBackground(getTableHeaderBg());
                header.setForeground(getTableHeaderFg());
                header.setFont(BOLD_FIELD_FONT);
            }
        } else if (component instanceof JTabbedPane) {
            JTabbedPane tab = (JTabbedPane) component;
            tab.setBackground(getHeaderBg());
            tab.setForeground(getTextColor());
            tab.setFont(BOLD_FIELD_FONT);
        } else if (component instanceof JScrollPane) {
            JScrollPane sp = (JScrollPane) component;
            if (Boolean.TRUE.equals(sp.getClientProperty("borderlessScroll"))) {
                styleFormScrollPane(sp);
            } else {
                styleModernScrollPane(sp);
            }
        }

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                applyThemeRecursively(child);
            }
        }
    }

    private static Font resolveFont(String family, int style, int size) {
        Font font = new Font(family, style, size);
        if (font.getFamily().equalsIgnoreCase("Dialog") && !family.equalsIgnoreCase("Dialog")) {
            return new Font(Font.SANS_SERIF, style, size);
        }
        return font;
    }
}

/**
 * ============================================================================
 * [BAR CHART PANEL: DATA VISUALIZATION]
 * ============================================================================
 */
class BarChartPanel extends JPanel {

    private final StudentManager studentManager;
    private final ThemeManager themeManager;

    private float animProgress = 0.0f;
    private javax.swing.Timer animTimer;
    private int hoveredBar = -1;

    private static final Color COLOR_A_PLUS = Color.decode("#10B981");
    private static final Color COLOR_A      = Color.decode("#3B82F6");
    private static final Color COLOR_B      = Color.decode("#6366F1");
    private static final Color COLOR_C      = Color.decode("#F59E0B");
    private static final Color COLOR_D      = Color.decode("#FB923C");
    private static final Color COLOR_F      = Color.decode("#EF4444");

    public BarChartPanel(StudentManager studentManager, ThemeManager themeManager) {
        this.studentManager = studentManager;
        this.themeManager = themeManager;
        setOpaque(false);
        setPreferredSize(new Dimension(580, 320));
        setMinimumSize(new Dimension(380, 220));

        // Interactive Bar Hover Tracking
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int old = hoveredBar;
                hoveredBar = getBarIndexAt(e.getX(), e.getY());
                if (old != hoveredBar) {
                    setCursor(hoveredBar >= 0 ? new Cursor(Cursor.HAND_CURSOR) : new Cursor(Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredBar != -1) {
                    hoveredBar = -1;
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }
        });

        startAnimation();
    }

    private int getBarIndexAt(int mouseX, int mouseY) {
        int width = getWidth();
        int height = getHeight();
        int marginLeft = 50;
        int marginRight = 30;
        int marginTop = 48;
        int marginBottom = 56;
        int chartWidth = width - marginLeft - marginRight;
        int chartHeight = height - marginTop - marginBottom;
        int baselineY = marginTop + chartHeight;

        if (chartWidth <= 0 || chartHeight <= 0) return -1;
        if (mouseY < marginTop - 20 || mouseY > baselineY + 30) return -1;

        double slotWidth = (double) chartWidth / 6;
        int barWidth = Math.max(22, Math.min(58, (int) (slotWidth * 0.52)));

        for (int i = 0; i < 6; i++) {
            int barX = (int) (marginLeft + (i * slotWidth) + (slotWidth - barWidth) / 2);
            if (mouseX >= barX - 4 && mouseX <= barX + barWidth + 4) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Triggers smooth 60fps bar rising animation with cubic ease-out.
     */
    public void startAnimation() {
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        animProgress = 0.0f;
        animTimer = new javax.swing.Timer(16, e -> {
            animProgress += 0.045f;
            if (animProgress >= 1.0f) {
                animProgress = 1.0f;
                ((javax.swing.Timer) e.getSource()).stop();
            }
            repaint();
        });
        animTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getWidth();
        int height = getHeight();

        Map<String, Integer> dist = studentManager.getGradeDistribution();
        String[] grades = {"A+", "A", "B", "C", "D", "F"};
        String[] gradeRanges = {">=90%", "80-89%", "70-79%", "60-69%", "40-59%", "<40%"};
        Color[] barColors = {COLOR_A_PLUS, COLOR_A, COLOR_B, COLOR_C, COLOR_D, COLOR_F};

        int totalStudents = studentManager.getTotalCount();
        int maxVal = 1;
        for (int count : dist.values()) {
            if (count > maxVal) {
                maxVal = count;
            }
        }
        int yAxisMax = Math.max(5, (int) (Math.ceil(maxVal / 4.0) * 4));

        int marginLeft = 50;
        int marginRight = 30;
        int marginTop = 48;
        int marginBottom = 56;

        int chartWidth = width - marginLeft - marginRight;
        int chartHeight = height - marginTop - marginBottom;

        if (chartWidth <= 0 || chartHeight <= 0) {
            g2.dispose();
            return;
        }

        g2.setFont(ThemeManager.TITLE_FONT);
        g2.setColor(themeManager.getTextColor());
        g2.drawString("Academic Grade Distribution", marginLeft, marginTop - 24);

        g2.setFont(ThemeManager.SMALL_FONT);
        g2.setColor(themeManager.getSubtextColor());
        g2.drawString(String.format("Cohort breakdown across %d enrolled scholars", totalStudents),
                marginLeft, marginTop - 8);

        int gridSteps = 4;
        Stroke dottedStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 3.0f}, 0.0f);
        Stroke solidStroke = new BasicStroke(1.2f);

        for (int i = 0; i <= gridSteps; i++) {
            int val = (yAxisMax * i) / gridSteps;
            int y = marginTop + chartHeight - (int) ((double) val / yAxisMax * chartHeight);

            g2.setFont(ThemeManager.SMALL_FONT);
            g2.setColor(themeManager.getSubtextColor());
            String valStr = String.valueOf(val);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(valStr, marginLeft - fm.stringWidth(valStr) - 10, y + 4);

            g2.setColor(themeManager.isDark() ? new Color(255, 255, 255, 20) : new Color(0, 0, 0, 16));
            g2.setStroke(dottedStroke);
            g2.drawLine(marginLeft, y, marginLeft + chartWidth, y);
        }

        int baselineY = marginTop + chartHeight;
        g2.setStroke(solidStroke);
        g2.setColor(themeManager.getCardBorder());
        g2.drawLine(marginLeft, baselineY, marginLeft + chartWidth, baselineY);

        int numBars = grades.length;
        double slotWidth = (double) chartWidth / numBars;
        int barWidth = Math.max(22, Math.min(58, (int) (slotWidth * 0.52)));

        for (int i = 0; i < numBars; i++) {
            String grade = grades[i];
            int count = dist.getOrDefault(grade, 0);
            Color barColor = barColors[i];

            float eased = (float) (1.0 - Math.pow(1.0 - animProgress, 3));
            int fullBarHeight = (int) (((double) count / yAxisMax) * chartHeight);
            int barHeight = (int) (fullBarHeight * eased);

            boolean isHovered = (i == hoveredBar);
            int currentBarWidth = isHovered ? barWidth + 4 : barWidth;
            int barX = (int) (marginLeft + (i * slotWidth) + (slotWidth - currentBarWidth) / 2);
            int barY = baselineY - (isHovered ? barHeight + 3 : barHeight);

            if (barHeight > 0) {
                GradientPaint gradient = new GradientPaint(
                        barX, barY, isHovered ? barColor.brighter() : barColor,
                        barX, baselineY, new Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), isHovered ? 230 : 160)
                );
                g2.setPaint(gradient);
                int arc = Math.min(12, currentBarWidth / 2);
                g2.fillRoundRect(barX, barY, currentBarWidth, barHeight + (isHovered ? 3 : 0) + arc, arc, arc);

                // Hover glow ring
                if (isHovered) {
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2.2f));
                    g2.drawRoundRect(barX - 1, barY - 1, currentBarWidth + 2, barHeight + 3 + arc + 2, arc + 2, arc + 2);
                }

                g2.setColor(barColor);
                g2.fillRect(barX, baselineY - 2, currentBarWidth, 2);

                String countStr = String.valueOf(count);
                if (totalStudents > 0) {
                    double pct = ((double) count / totalStudents) * 100.0;
                    if (currentBarWidth >= 40) {
                        countStr = String.format("%d (%.0f%%)", count, pct);
                    }
                }

                if (animProgress > 0.35f) {
                    g2.setFont(isHovered ? ThemeManager.BOLD_FIELD_FONT : ThemeManager.BADGE_FONT);
                    FontMetrics fm = g2.getFontMetrics();
                    int textW = fm.stringWidth(countStr);
                    int textX = barX + (currentBarWidth - textW) / 2;
                    int textY = barY - (isHovered ? 10 : 6);

                    float labelAlpha = Math.min(1.0f, (animProgress - 0.35f) / 0.65f);
                    Color baseText = isHovered ? barColor : themeManager.getTextColor();
                    g2.setColor(new Color(baseText.getRed(), baseText.getGreen(), baseText.getBlue(), (int)(labelAlpha * 255)));
                    g2.drawString(countStr, textX, textY);
                }
            } else {
                g2.setFont(ThemeManager.SMALL_FONT);
                g2.setColor(themeManager.getSubtextColor());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("0", barX + (currentBarWidth - fm.stringWidth("0")) / 2, baselineY - 6);
            }

            g2.setFont(ThemeManager.BOLD_FIELD_FONT);
            g2.setColor(barColor);
            FontMetrics fmLarge = g2.getFontMetrics();
            int gradeW = fmLarge.stringWidth(grade);
            g2.drawString(grade, barX + (currentBarWidth - gradeW) / 2, baselineY + 18);

            g2.setFont(ThemeManager.BADGE_FONT);
            g2.setColor(themeManager.getSubtextColor());
            FontMetrics fmSub = g2.getFontMetrics();
            String range = gradeRanges[i];
            int rangeW = fmSub.stringWidth(range);
            g2.drawString(range, barX + (currentBarWidth - rangeW) / 2, baselineY + 32);
        }

        // Floating Interactive Glassmorphism Tooltip for Hovered Bar
        if (hoveredBar >= 0 && hoveredBar < numBars) {
            String grade = grades[hoveredBar];
            int count = dist.getOrDefault(grade, 0);
            Color barColor = barColors[hoveredBar];
            double pct = totalStudents > 0 ? ((double) count / totalStudents) * 100.0 : 0.0;

            int fullBarHeight = (int) (((double) count / yAxisMax) * chartHeight);
            float eased = (float) (1.0 - Math.pow(1.0 - animProgress, 3));
            int bHeight = (int) (fullBarHeight * eased);

            int currentBarWidth = barWidth + 4;
            int bX = (int) (marginLeft + (hoveredBar * slotWidth) + (slotWidth - currentBarWidth) / 2);
            int bY = baselineY - (bHeight + 3);

            int tipW = 150;
            int tipH = 46;
            int tipX = Math.max(marginLeft, Math.min(width - marginRight - tipW, bX + (currentBarWidth - tipW) / 2));
            int tipY = Math.max(marginTop - 40, bY - tipH - 18);

            // Tooltip Shadow & Background
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillRoundRect(tipX + 2, tipY + 2, tipW, tipH, 10, 10);

            g2.setColor(themeManager.isDark() ? new Color(15, 23, 42, 240) : new Color(30, 41, 59, 240));
            g2.fillRoundRect(tipX, tipY, tipW, tipH, 10, 10);

            g2.setColor(barColor);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(tipX, tipY, tipW, tipH, 10, 10);

            // Little Downward Arrow
            int arrowCenterX = Math.max(tipX + 16, Math.min(tipX + tipW - 16, bX + currentBarWidth / 2));
            int[] ax = {arrowCenterX - 5, arrowCenterX + 5, arrowCenterX};
            int[] ay = {tipY + tipH, tipY + tipH, tipY + tipH + 5};
            g2.fillPolygon(ax, ay, 3);

            // Tooltip Text Lines
            g2.setFont(ThemeManager.BADGE_FONT);
            g2.setColor(Color.WHITE);
            g2.drawString(String.format("Grade %s (Score %s)", grade, gradeRanges[hoveredBar]), tipX + 12, tipY + 18);

            g2.setFont(ThemeManager.SMALL_FONT);
            g2.setColor(barColor);
            g2.drawString(String.format("%d Scholars (%.1f%% of total)", count, pct), tipX + 12, tipY + 36);
        }

        g2.dispose();
    }
}

/**
 * ============================================================================
 * [ICON FACTORY: SCALABLE VECTOR ICONS]
 * ============================================================================
 */
class IconFactory {

    @FunctionalInterface
    public interface IconPainter {
        void paint(Graphics2D g2, int width, int height, Color color);
    }

    public static class VectorIcon implements Icon {
        private final int width;
        private final int height;
        private Color color;
        private final IconPainter painter;

        public VectorIcon(int size, Color color, IconPainter painter) {
            this(size, size, color, painter);
        }

        public VectorIcon(int width, int height, Color color, IconPainter painter) {
            this.width = width;
            this.height = height;
            this.color = color;
            this.painter = painter;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            painter.paint(g2, width, height, color);
            g2.dispose();
        }

        @Override
        public int getIconWidth() { return width; }
        @Override
        public int getIconHeight() { return height; }

        public Image toImage() {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            paintIcon(null, g2, 0, 0);
            g2.dispose();
            return img;
        }
    }

    /**
     * Scalable official vector logo for UniDesk SIS.
     * Features modern squircle badge, graduation mortarboard diamond with 3D facet lighting,
     * golden tassel ribbon, open academic knowledge book, and gold center excellence star.
     */
    public static Icon createUniDeskLogo(int size) {
        return new VectorIcon(size, size, Color.WHITE, (g2, w, h, col) -> {
            // 1. Sleek Squircle Badge Background
            int arc = (int) (w * 0.24);
            GradientPaint bgGrad = new GradientPaint(
                    0, 0, Color.decode("#2563EB"),
                    (float) w, (float) h, Color.decode("#1E3A8A")
            );
            g2.setPaint(bgGrad);
            g2.fillRoundRect(1, 1, (int) w - 2, (int) h - 2, arc, arc);

            // Glass Rim
            g2.setColor(new Color(255, 255, 255, 75));
            g2.setStroke(new BasicStroke(Math.max(1.0f, (float) w * 0.02f)));
            g2.drawRoundRect(1, 1, (int) w - 2, (int) h - 2, arc, arc);

            // 2. Mortarboard Diamond
            double cy = h * 0.38;
            Path2D leftFacet = new Path2D.Double();
            leftFacet.moveTo(w * 0.50, h * 0.20);
            leftFacet.lineTo(w * 0.16, cy);
            leftFacet.lineTo(w * 0.50, h * 0.52);
            leftFacet.closePath();
            g2.setColor(new Color(230, 240, 255));
            g2.fill(leftFacet);

            Path2D rightFacet = new Path2D.Double();
            rightFacet.moveTo(w * 0.50, h * 0.20);
            rightFacet.lineTo(w * 0.84, cy);
            rightFacet.lineTo(w * 0.50, h * 0.52);
            rightFacet.closePath();
            g2.setColor(Color.WHITE);
            g2.fill(rightFacet);

            // 3. Mortarboard Skull Base
            Path2D base = new Path2D.Double();
            base.moveTo(w * 0.32, h * 0.46);
            base.lineTo(w * 0.32, h * 0.58);
            base.curveTo(w * 0.32, h * 0.67, w * 0.68, h * 0.67, w * 0.68, h * 0.58);
            base.lineTo(w * 0.68, h * 0.46);
            base.lineTo(w * 0.50, h * 0.52);
            base.closePath();
            g2.setColor(new Color(200, 220, 250));
            g2.fill(base);

            // 4. Golden Button & Tassel
            int btnR = Math.max(2, (int) (w * 0.032));
            g2.setColor(Color.decode("#FBBF24"));
            g2.fillOval((int) (w * 0.50 - btnR), (int) (cy - btnR), btnR * 2, btnR * 2);

            // Tassel Ribbon
            g2.setColor(Color.decode("#F59E0B"));
            g2.setStroke(new BasicStroke(Math.max(1.2f, (float) w * 0.024f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D tassel = new Path2D.Double();
            tassel.moveTo(w * 0.50, cy);
            tassel.lineTo(w * 0.74, h * 0.44);
            tassel.lineTo(w * 0.82, h * 0.56);
            tassel.lineTo(w * 0.82, h * 0.68);
            g2.draw(tassel);

            int dropR = Math.max(2, (int) (w * 0.032));
            g2.setColor(Color.decode("#FBBF24"));
            g2.fillOval((int) (w * 0.82 - dropR), (int) (h * 0.68 - dropR), dropR * 2, dropR * 2);

            // 5. Open Knowledge Book Pages at bottom
            Path2D leftPage = new Path2D.Double();
            leftPage.moveTo(w * 0.22, h * 0.72);
            leftPage.curveTo(w * 0.34, h * 0.69, w * 0.44, h * 0.73, w * 0.48, h * 0.75);
            leftPage.lineTo(w * 0.48, h * 0.86);
            leftPage.curveTo(w * 0.44, h * 0.84, w * 0.34, h * 0.80, w * 0.22, h * 0.83);
            leftPage.closePath();
            g2.setColor(new Color(255, 255, 255, 220));
            g2.fill(leftPage);

            Path2D rightPage = new Path2D.Double();
            rightPage.moveTo(w * 0.78, h * 0.72);
            rightPage.curveTo(w * 0.66, h * 0.69, w * 0.56, h * 0.73, w * 0.52, h * 0.75);
            rightPage.lineTo(w * 0.52, h * 0.86);
            rightPage.curveTo(w * 0.56, h * 0.84, w * 0.66, h * 0.80, w * 0.78, h * 0.83);
            rightPage.closePath();
            g2.setColor(new Color(255, 255, 255, 245));
            g2.fill(rightPage);

            // Center Gold Star of Excellence
            int starR = Math.max(2, (int) (w * 0.030));
            g2.setColor(Color.decode("#FBBF24"));
            g2.fillOval((int) (w * 0.50 - starR), (int) (h * 0.79 - starR), starR * 2, starR * 2);
        });
    }

    public static Icon createGraduationCapIcon(int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Path2D cap = new Path2D.Double();
            cap.moveTo(w * 0.5, h * 0.18);
            cap.lineTo(w * 0.92, h * 0.38);
            cap.lineTo(w * 0.5, h * 0.58);
            cap.lineTo(w * 0.08, h * 0.38);
            cap.closePath();
            g2.fill(cap);

            Path2D base = new Path2D.Double();
            base.moveTo(w * 0.24, h * 0.48);
            base.lineTo(w * 0.24, h * 0.70);
            base.curveTo(w * 0.24, h * 0.85, w * 0.76, h * 0.85, w * 0.76, h * 0.70);
            base.lineTo(w * 0.76, h * 0.48);
            g2.draw(base);

            g2.drawLine((int)(w * 0.92), (int)(h * 0.38), (int)(w * 0.92), (int)(h * 0.68));
            g2.fillOval((int)(w * 0.88), (int)(h * 0.68), (int)(w * 0.08), (int)(h * 0.12));
        });
    }

    public static Icon createAnalyticsIcon(int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int)(w * 0.1), (int)(h * 0.88), (int)(w * 0.9), (int)(h * 0.88));

            g2.fillRoundRect((int)(w * 0.20), (int)(h * 0.52), (int)(w * 0.16), (int)(h * 0.36), 3, 3);
            g2.fillRoundRect((int)(w * 0.44), (int)(h * 0.22), (int)(w * 0.16), (int)(h * 0.66), 3, 3);
            g2.fillRoundRect((int)(w * 0.68), (int)(h * 0.38), (int)(w * 0.16), (int)(h * 0.50), 3, 3);
        });
    }

    public static Icon createFacultyIcon(int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval((int)(w * 0.32), (int)(h * 0.14), (int)(w * 0.36), (int)(h * 0.36));

            Path2D body = new Path2D.Double();
            body.moveTo(w * 0.14, h * 0.84);
            body.curveTo(w * 0.14, h * 0.58, w * 0.86, h * 0.58, w * 0.86, h * 0.84);
            g2.draw(body);
        });
    }

    public static Icon createAddIcon(int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int)(w * 0.5), (int)(h * 0.2), (int)(w * 0.5), (int)(h * 0.8));
            g2.drawLine((int)(w * 0.2), (int)(h * 0.5), (int)(w * 0.8), (int)(h * 0.5));
        });
    }

    public static Icon createEditIcon(int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Path2D pen = new Path2D.Double();
            pen.moveTo(w * 0.68, h * 0.16);
            pen.lineTo(w * 0.84, h * 0.32);
            pen.lineTo(w * 0.36, h * 0.80);
            pen.lineTo(w * 0.18, h * 0.82);
            pen.lineTo(w * 0.20, h * 0.64);
            pen.closePath();
            g2.draw(pen);
        });
    }

    public static Icon createDeleteIcon(int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Path2D bin = new Path2D.Double();
            bin.moveTo(w * 0.26, h * 0.36);
            bin.lineTo(w * 0.30, h * 0.84);
            bin.curveTo(w * 0.30, h * 0.88, w * 0.70, h * 0.88, w * 0.70, h * 0.84);
            bin.lineTo(w * 0.74, h * 0.36);
            g2.draw(bin);

            g2.drawLine((int)(w * 0.18), (int)(h * 0.36), (int)(w * 0.82), (int)(h * 0.36));
            g2.drawArc((int)(w * 0.38), (int)(h * 0.20), (int)(w * 0.24), (int)(h * 0.24), 0, 180);
        });
    }

    public static Icon createClearIcon(int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawArc((int)(w * 0.20), (int)(h * 0.20), (int)(w * 0.60), (int)(h * 0.60), 45, 270);

            Path2D arrow = new Path2D.Double();
            arrow.moveTo(w * 0.76, h * 0.28);
            arrow.lineTo(w * 0.80, h * 0.46);
            arrow.lineTo(w * 0.62, h * 0.42);
            g2.draw(arrow);
        });
    }

    public static Icon createSearchIcon(int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval((int)(w * 0.18), (int)(h * 0.18), (int)(w * 0.48), (int)(h * 0.48));
            g2.drawLine((int)(w * 0.58), (int)(h * 0.58), (int)(w * 0.82), (int)(h * 0.82));
        });
    }

    public static Icon createImportIcon(int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            g2.drawLine((int)(w * 0.5), (int)(h * 0.18), (int)(w * 0.5), (int)(h * 0.64));
            g2.drawLine((int)(w * 0.32), (int)(h * 0.46), (int)(w * 0.5), (int)(h * 0.64));
            g2.drawLine((int)(w * 0.68), (int)(h * 0.46), (int)(w * 0.5), (int)(h * 0.64));

            Path2D tray = new Path2D.Double();
            tray.moveTo(w * 0.20, h * 0.60);
            tray.lineTo(w * 0.20, h * 0.82);
            tray.lineTo(w * 0.80, h * 0.82);
            tray.lineTo(w * 0.80, h * 0.60);
            g2.draw(tray);
        });
    }

    public static Icon createExportIcon(int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            g2.drawLine((int)(w * 0.5), (int)(h * 0.64), (int)(w * 0.5), (int)(h * 0.18));
            g2.drawLine((int)(w * 0.32), (int)(h * 0.36), (int)(w * 0.5), (int)(h * 0.18));
            g2.drawLine((int)(w * 0.68), (int)(h * 0.36), (int)(w * 0.5), (int)(h * 0.18));

            Path2D tray = new Path2D.Double();
            tray.moveTo(w * 0.20, h * 0.60);
            tray.lineTo(w * 0.20, h * 0.82);
            tray.lineTo(w * 0.80, h * 0.82);
            tray.lineTo(w * 0.80, h * 0.60);
            g2.draw(tray);
        });
    }

    public static Icon createReportIcon(int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Path2D doc = new Path2D.Double();
            doc.moveTo(w * 0.22, h * 0.16);
            doc.lineTo(w * 0.62, h * 0.16);
            doc.lineTo(w * 0.78, h * 0.32);
            doc.lineTo(w * 0.78, h * 0.84);
            doc.lineTo(w * 0.22, h * 0.84);
            doc.closePath();
            g2.draw(doc);

            g2.drawLine((int)(w * 0.62), (int)(h * 0.16), (int)(w * 0.62), (int)(h * 0.32));
            g2.drawLine((int)(w * 0.62), (int)(h * 0.32), (int)(w * 0.78), (int)(h * 0.32));

            g2.drawLine((int)(w * 0.34), (int)(h * 0.46), (int)(w * 0.66), (int)(h * 0.46));
            g2.drawLine((int)(w * 0.34), (int)(h * 0.60), (int)(w * 0.66), (int)(h * 0.60));
            g2.drawLine((int)(w * 0.34), (int)(h * 0.72), (int)(w * 0.52), (int)(h * 0.72));
        });
    }

    public static Icon createSunMoonIcon(boolean isDark, int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            if (isDark) {
                g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawOval((int)(w * 0.32), (int)(h * 0.32), (int)(w * 0.36), (int)(h * 0.36));

                int cx = w / 2;
                int cy = h / 2;
                int r1 = (int)(w * 0.26);
                int r2 = (int)(w * 0.38);

                for (int i = 0; i < 8; i++) {
                    double angle = i * Math.PI / 4.0;
                    int x1 = (int)(cx + Math.cos(angle) * r1);
                    int y1 = (int)(cy + Math.sin(angle) * r1);
                    int x2 = (int)(cx + Math.cos(angle) * r2);
                    int y2 = (int)(cy + Math.sin(angle) * r2);
                    g2.drawLine(x1, y1, x2, y2);
                }
            } else {
                Area moon = new Area(new Ellipse2D.Double(w * 0.22, h * 0.18, w * 0.58, h * 0.58));
                Area cutout = new Area(new Ellipse2D.Double(w * 0.38, h * 0.12, w * 0.50, h * 0.50));
                moon.subtract(cutout);
                g2.fill(moon);
            }
        });
    }

    public static Icon createInfoIcon(int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval((int)(w * 0.16), (int)(h * 0.16), (int)(w * 0.68), (int)(h * 0.68));
            g2.fillOval((int)(w * 0.46), (int)(h * 0.32), (int)(w * 0.08), (int)(h * 0.08));
            g2.drawLine((int)(w * 0.5), (int)(h * 0.46), (int)(w * 0.5), (int)(h * 0.68));
        });
    }

    public static Icon createProfileCardIcon(int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect((int)(w * 0.16), (int)(h * 0.18), (int)(w * 0.68), (int)(h * 0.64), 6, 6);
            g2.drawOval((int)(w * 0.28), (int)(h * 0.32), (int)(w * 0.18), (int)(w * 0.18));
            g2.drawLine((int)(w * 0.54), (int)(h * 0.38), (int)(w * 0.74), (int)(h * 0.38));
            g2.drawLine((int)(w * 0.54), (int)(h * 0.50), (int)(w * 0.70), (int)(h * 0.50));
        });
    }

    public static Icon createTrophyIcon(int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Path2D cup = new Path2D.Double();
            cup.moveTo(w * 0.28, h * 0.18);
            cup.lineTo(w * 0.72, h * 0.18);
            cup.curveTo(w * 0.72, h * 0.58, w * 0.28, h * 0.58, w * 0.28, h * 0.18);
            g2.draw(cup);

            g2.drawArc((int)(w * 0.18), (int)(h * 0.22), (int)(w * 0.18), (int)(h * 0.20), 90, 180);
            g2.drawArc((int)(w * 0.64), (int)(h * 0.22), (int)(w * 0.18), (int)(h * 0.20), 270, 180);

            g2.drawLine((int)(w * 0.5), (int)(h * 0.56), (int)(w * 0.5), (int)(h * 0.72));
            g2.drawLine((int)(w * 0.32), (int)(h * 0.78), (int)(w * 0.68), (int)(h * 0.78));
        });
    }

    public static Icon createShieldCheckIcon(int size, Color color) {
        return new VectorIcon(size, color, (g2, w, h, col) -> {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval((int)(w * 0.16), (int)(h * 0.16), (int)(w * 0.68), (int)(h * 0.68));

            Path2D check = new Path2D.Double();
            check.moveTo(w * 0.34, h * 0.50);
            check.lineTo(w * 0.46, h * 0.64);
            check.lineTo(w * 0.68, h * 0.38);
            g2.draw(check);
        });
    }
}
