import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class TaskManagerDashboard extends JFrame {

    // ─── Data ───────────────────────────────────────────────────────────────
    private DefaultTableModel tableModel;
    private JTable taskTable;
    private JTextField tfTitle, tfAssignee;
    private JComboBox<String> cbPriority, cbStatus, cbFilter;
    private JProgressBar pbTotal, pbHigh, pbMedium, pbLow;
    private JLabel lblTotalCount, lblDoneCount, lblPendingCount;
    private JTextArea taNotes;
    private JList<String> activityList;
    private DefaultListModel<String> activityModel;
    private int taskIdCounter = 4;

    private static final String[] COLUMNS = {"ID", "Title", "Assignee", "Priority", "Status", "Progress"};
    private static final Color BG         = new Color(11, 16, 28);
    private static final Color SURFACE    = new Color(30, 41, 59);
    private static final Color SURFACE2   = new Color(51, 65, 85);
    private static final Color ACCENT     = new Color(56, 189, 248);
    private static final Color ACCENT2    = new Color(99, 102, 241);
    private static final Color SUCCESS    = new Color(34, 197, 94);
    private static final Color WARNING    = new Color(251, 191, 36);
    private static final Color DANGER     = new Color(239, 68, 68);
    private static final Color TEXT       = new Color(226, 232, 240);
    private static final Color TEXT_MUTED = new Color(148, 163, 184);
    private static final Font  FONT_BODY  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  FONT_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font  FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font  FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    // ─── Constructor ────────────────────────────────────────────────────────
    public TaskManagerDashboard() {
        setTitle("TaskFlow — Dashboard");
        setSize(1100, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));

        buildMenuBar();
        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildMainArea(),  BorderLayout.CENTER);

        populateSampleData();
        refreshStats();
        setVisible(true);
    }

    // ─── Menu Bar ───────────────────────────────────────────────────────────
    private void buildMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.setBackground(SURFACE);
        mb.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SURFACE2));

        JMenu mFile = darkMenu("File");
        mFile.add(darkMenuItem("New Task",   "Ctrl+N", e -> clearForm()));
        mFile.add(darkMenuItem("Export CSV", "Ctrl+E", e -> exportCSV()));
        mFile.addSeparator();
        mFile.add(darkMenuItem("Exit",       "Ctrl+Q", e -> System.exit(0)));

        JMenu mEdit = darkMenu("Edit");
        mEdit.add(darkMenuItem("Delete Selected", "Del",    e -> deleteSelected()));
        mEdit.add(darkMenuItem("Clear All Tasks",  null,    e -> clearAllTasks()));

        JMenu mView = darkMenu("View");
        JCheckBoxMenuItem cbDark = new JCheckBoxMenuItem("Dark Mode");
        cbDark.setSelected(true); cbDark.setForeground(TEXT); cbDark.setBackground(SURFACE);
        mView.add(cbDark);

        JMenu mHelp = darkMenu("Help");
        mHelp.add(darkMenuItem("About", null, e ->
                JOptionPane.showMessageDialog(this,
                        "TaskFlow Dashboard v1.0\nBuilt with Java Swing\n\nControls used:\nJFrame, JMenuBar, JTabbedPane,\nJTable, JTextField, JComboBox,\nJProgressBar, JList, JTextArea,\nJButton, JLabel, JCheckBox",
                        "About TaskFlow", JOptionPane.INFORMATION_MESSAGE)));

        mb.add(mFile); mb.add(mEdit); mb.add(mView); mb.add(mHelp);
        setJMenuBar(mb);
    }

    private JMenu darkMenu(String name) {
        JMenu m = new JMenu(name);
        m.setForeground(TEXT); m.setFont(FONT_BODY);
        return m;
    }
    private JMenuItem darkMenuItem(String name, String accel, ActionListener al) {
        JMenuItem mi = new JMenuItem(name);
        mi.setForeground(TEXT); mi.setBackground(SURFACE); mi.setFont(FONT_BODY);
        if (accel != null) mi.setAccelerator(KeyStroke.getKeyStroke(accel));
        mi.addActionListener(al);
        return mi;
    }

    // ─── Top Bar ────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(SURFACE);
        p.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("  TaskFlow Dashboard");
        title.setFont(FONT_TITLE); title.setForeground(ACCENT);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(styledLabel("Filter:", TEXT_MUTED, FONT_SMALL));
        cbFilter = styledCombo(new String[]{"All Tasks", "High Priority", "In Progress", "Completed", "Pending"});
        cbFilter.addActionListener(e -> applyFilter());
        right.add(cbFilter);

        JButton btnRefresh = accentButton("⟳ Refresh");
        btnRefresh.addActionListener(e -> refreshStats());
        right.add(btnRefresh);

        p.add(title, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ─── Main Area ──────────────────────────────────────────────────────────
    private JSplitPane buildMainArea() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildRightPanel());
        split.setDividerLocation(330);
        split.setDividerSize(4);
        split.setBackground(BG);
        split.setBorder(null);
        return split;
    }

    // ─── Left Panel (Stats + Form) ──────────────────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel p = new JPanel();
        p.setBackground(BG);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(12, 12, 12, 6));

        p.add(buildStatCards());
        p.add(Box.createVerticalStrut(12));
        p.add(buildProgressSection());
        p.add(Box.createVerticalStrut(12));
        p.add(buildFormPanel());
        p.add(Box.createVerticalStrut(12));
        p.add(buildActivityLog());
        return p;
    }

    private JPanel buildStatCards() {
        JPanel p = new JPanel(new GridLayout(1, 3, 8, 0));
        p.setOpaque(false);
        lblTotalCount   = bigStatCard("Total", "0", ACCENT);
        lblDoneCount    = bigStatCard("Done",  "0", SUCCESS);
        lblPendingCount = bigStatCard("Pending","0", WARNING);
        // We return the panels from bigStatCard via a container trick
        JPanel[] cards = buildStatCardPanels();
        for (JPanel c : cards) p.add(c);
        return p;
    }

    private JPanel[] buildStatCardPanels() {
        lblTotalCount   = new JLabel("0"); lblTotalCount.setFont(new Font("Segoe UI", Font.BOLD, 28)); lblTotalCount.setForeground(ACCENT);
        lblDoneCount    = new JLabel("0"); lblDoneCount.setFont(new Font("Segoe UI", Font.BOLD, 28)); lblDoneCount.setForeground(SUCCESS);
        lblPendingCount = new JLabel("0"); lblPendingCount.setFont(new Font("Segoe UI", Font.BOLD, 28)); lblPendingCount.setForeground(WARNING);

        return new JPanel[]{
                makeStatCard("Total Tasks",  lblTotalCount,  ACCENT),
                makeStatCard("Completed",    lblDoneCount,   SUCCESS),
                makeStatCard("Pending",      lblPendingCount, WARNING)
        };
    }

    private JPanel makeStatCard(String label, JLabel numLabel, Color accent) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(SURFACE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
                new EmptyBorder(10, 12, 10, 12)));
        JLabel lbl = styledLabel(label, TEXT_MUTED, FONT_SMALL);
        p.add(lbl, BorderLayout.NORTH);
        p.add(numLabel, BorderLayout.CENTER);
        return p;
    }

    private JLabel bigStatCard(String l, String v, Color c) { return new JLabel(v); }

    private JPanel buildProgressSection() {
        JPanel p = surfacePanel("Progress Overview");
        p.setLayout(new BorderLayout());

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(new EmptyBorder(8, 0, 0, 0));

        pbTotal  = styledProgressBar(ACCENT);
        pbHigh   = styledProgressBar(DANGER);
        pbMedium = styledProgressBar(WARNING);
        pbLow    = styledProgressBar(SUCCESS);

        inner.add(progressRow("Overall Completion", pbTotal));
        inner.add(Box.createVerticalStrut(6));
        inner.add(progressRow("High Priority",      pbHigh));
        inner.add(Box.createVerticalStrut(6));
        inner.add(progressRow("Medium Priority",    pbMedium));
        inner.add(Box.createVerticalStrut(6));
        inner.add(progressRow("Low Priority",       pbLow));

        p.add(inner, BorderLayout.CENTER);
        return wrapInSurface(p, "Progress Overview");
    }

    private JPanel progressRow(String label, JProgressBar bar) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel lbl = styledLabel(label, TEXT_MUTED, FONT_SMALL);
        lbl.setPreferredSize(new Dimension(120, 16));
        row.add(lbl, BorderLayout.WEST);
        row.add(bar,  BorderLayout.CENTER);
        return row;
    }

    private JProgressBar styledProgressBar(Color color) {
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue(0);
        pb.setStringPainted(true);
        pb.setBackground(SURFACE2);
        pb.setForeground(color);
        pb.setFont(FONT_SMALL);
        pb.setBorder(null);
        pb.setPreferredSize(new Dimension(0, 16));
        return pb;
    }

    private JPanel buildFormPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(SURFACE);
        outer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SURFACE2),
                new EmptyBorder(12, 14, 12, 14)));

        JLabel title = styledLabel("Add / Edit Task", ACCENT, FONT_BOLD);
        outer.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(10, 0, 0, 0));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(4,0,4,6);

        tfTitle    = darkTextField(); tfTitle.setToolTipText("Task title");
        tfAssignee = darkTextField(); tfAssignee.setToolTipText("Assignee name");
        cbPriority = styledCombo(new String[]{"High", "Medium", "Low"});
        cbStatus   = styledCombo(new String[]{"Pending", "In Progress", "Completed"});

        gc.gridx=0; gc.gridy=0; gc.weightx=0.3; form.add(styledLabel("Title",    TEXT_MUTED, FONT_SMALL), gc);
        gc.gridx=1; gc.weightx=0.7; form.add(tfTitle, gc);
        gc.gridx=0; gc.gridy=1; gc.weightx=0.3; form.add(styledLabel("Assignee", TEXT_MUTED, FONT_SMALL), gc);
        gc.gridx=1; gc.weightx=0.7; form.add(tfAssignee, gc);
        gc.gridx=0; gc.gridy=2; gc.weightx=0.3; form.add(styledLabel("Priority", TEXT_MUTED, FONT_SMALL), gc);
        gc.gridx=1; gc.weightx=0.7; form.add(cbPriority, gc);
        gc.gridx=0; gc.gridy=3; gc.weightx=0.3; form.add(styledLabel("Status",   TEXT_MUTED, FONT_SMALL), gc);
        gc.gridx=1; gc.weightx=0.7; form.add(cbStatus, gc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnRow.setOpaque(false);
        JButton btnAdd = accentButton("+ Add Task");
        JButton btnDel = dangerButton("✕ Delete");
        JButton btnClr = ghostButton("Clear");
        btnAdd.addActionListener(e -> addTask());
        btnDel.addActionListener(e -> deleteSelected());
        btnClr.addActionListener(e -> clearForm());
        btnRow.add(btnAdd); btnRow.add(btnDel); btnRow.add(btnClr);

        gc.gridx=0; gc.gridy=4; gc.gridwidth=2; gc.insets=new Insets(8,0,0,0);
        form.add(btnRow, gc);

        outer.add(form, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildActivityLog() {
        activityModel = new DefaultListModel<>();
        activityModel.addElement("▶  Dashboard loaded");
        activityList  = new JList<>(activityModel);
        activityList.setBackground(SURFACE);
        activityList.setForeground(TEXT_MUTED);
        activityList.setFont(FONT_SMALL);
        activityList.setFixedCellHeight(22);
        activityList.setBorder(null);

        JScrollPane sp = new JScrollPane(activityList);
        sp.setBorder(null);
        sp.getViewport().setBackground(SURFACE);
        sp.setPreferredSize(new Dimension(0, 90));

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(SURFACE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SURFACE2),
                new EmptyBorder(10, 14, 10, 14)));
        p.add(styledLabel("Activity Log", ACCENT, FONT_BOLD), BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ─── Right Panel (Tabs: Table + Notes) ──────────────────────────────────
    private JTabbedPane buildRightPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(SURFACE);
        tabs.setForeground(TEXT);
        tabs.setFont(FONT_BODY);

        tabs.addTab("📋  Task List",  buildTableTab());
        tabs.addTab("📝  Notes",      buildNotesTab());
        tabs.addTab("📊  Summary",    buildSummaryTab());

        tabs.setBackgroundAt(0, SURFACE);
        tabs.setForegroundAt(0, ACCENT);
        return tabs;
    }

    private JPanel buildTableTab() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 4; }
            @Override public Class<?> getColumnClass(int c) { return String.class; }
        };

        taskTable = new JTable(tableModel);
        taskTable.setBackground(SURFACE);
        taskTable.setForeground(TEXT);
        taskTable.setFont(FONT_BODY);
        taskTable.setRowHeight(32);
        taskTable.setGridColor(SURFACE2);
        taskTable.setSelectionBackground(ACCENT2);
        taskTable.setSelectionForeground(Color.WHITE);
        taskTable.setShowHorizontalLines(true);
        taskTable.setShowVerticalLines(false);
        taskTable.setIntercellSpacing(new Dimension(0, 1));
        taskTable.getTableHeader().setBackground(new Color(15, 23, 60));
        taskTable.getTableHeader().setForeground(ACCENT);
        taskTable.getTableHeader().setFont(FONT_BOLD);
        taskTable.getTableHeader().setReorderingAllowed(false);

        // Column widths
        int[] widths = {40, 200, 100, 80, 100, 80};
        for (int i = 0; i < widths.length; i++)
            taskTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Priority color renderer
        taskTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(CENTER);
                String val = v == null ? "" : v.toString();
                if (!sel) {
                    switch (val) {
                        case "High":   setForeground(DANGER);  break;
                        case "Medium": setForeground(WARNING); break;
                        case "Low":    setForeground(SUCCESS); break;
                        default:       setForeground(TEXT);
                    }
                }
                setBackground(sel ? ACCENT2 : SURFACE);
                return this;
            }
        });

        // Status color renderer
        taskTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(CENTER);
                String val = v == null ? "" : v.toString();
                if (!sel) {
                    switch (val) {
                        case "Completed":  setForeground(SUCCESS); break;
                        case "In Progress":setForeground(ACCENT);  break;
                        case "Pending":    setForeground(WARNING); break;
                        default:           setForeground(TEXT);
                    }
                }
                setBackground(sel ? ACCENT2 : SURFACE);
                return this;
            }
        });

        // Row selection listener → fill form
        taskTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && taskTable.getSelectedRow() >= 0) {
                int row = taskTable.getSelectedRow();
                tfTitle.setText(tableModel.getValueAt(row, 1).toString());
                tfAssignee.setText(tableModel.getValueAt(row, 2).toString());
                cbPriority.setSelectedItem(tableModel.getValueAt(row, 3).toString());
                cbStatus.setSelectedItem(tableModel.getValueAt(row, 4).toString());
            }
        });

        JScrollPane sp = new JScrollPane(taskTable);
        sp.setBorder(null);
        sp.getViewport().setBackground(SURFACE);
        sp.getVerticalScrollBar().setBackground(SURFACE2);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildNotesTab() {
        taNotes = new JTextArea("Write your notes here...\n");
        taNotes.setBackground(SURFACE);
        taNotes.setForeground(TEXT);
        taNotes.setCaretColor(ACCENT);
        taNotes.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        taNotes.setLineWrap(true);
        taNotes.setWrapStyleWord(true);
        taNotes.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane sp = new JScrollPane(taNotes);
        sp.setBorder(null);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildSummaryTab() {
        JPanel p = new JPanel();
        p.setBackground(BG);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[][] rows = {
                {"Total Tasks",    "0"},
                {"Completed",      "0"},
                {"In Progress",    "0"},
                {"Pending",        "0"},
                {"High Priority",  "0"},
                {"Medium Priority","0"},
                {"Low Priority",   "0"},
        };

        for (String[] row : rows) {
            JPanel r = new JPanel(new BorderLayout());
            r.setBackground(SURFACE);
            r.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, SURFACE2),
                    new EmptyBorder(10, 14, 10, 14)));
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            r.add(styledLabel(row[0], TEXT_MUTED, FONT_BODY), BorderLayout.WEST);
            r.add(styledLabel(row[1], ACCENT, FONT_BOLD), BorderLayout.EAST);
            p.add(r);
        }
        return p;
    }

    // ─── Sample Data ─────────────────────────────────────────────────────────
    private void populateSampleData() {
        Object[][] tasks = {
                {"#001", "Design login page UI",   "ToLa",   "High",   "Completed",   "100%"},
                {"#002", "Setup PostgreSQL schema", "Dara",   "High",   "In Progress", "60%"},
                {"#003", "Build Telegram Bot API",  "Sophal", "Medium", "In Progress", "45%"},
                {"#004", "Write unit tests",        "ToLa",   "Low",    "Pending",     "0%"},
        };
        for (Object[] row : tasks) tableModel.addRow(row);
    }

    // ─── Actions ─────────────────────────────────────────────────────────────
    private void addTask() {
        String title = tfTitle.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Task title cannot be empty!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        taskIdCounter++;
        String id = String.format("#%03d", taskIdCounter);
        tableModel.addRow(new Object[]{
                id, title, tfAssignee.getText().trim(),
                cbPriority.getSelectedItem(), cbStatus.getSelectedItem(), "0%"
        });
        log("Added task: " + title);
        clearForm();
        refreshStats();
    }

    private void deleteSelected() {
        int row = taskTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a task to delete."); return; }
        String title = tableModel.getValueAt(row, 1).toString();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete task: \"" + title + "\"?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            tableModel.removeRow(row);
            log("Deleted: " + title);
            refreshStats();
        }
    }

    private void clearAllTasks() {
        int confirm = JOptionPane.showConfirmDialog(this, "Clear ALL tasks?",
                "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            tableModel.setRowCount(0);
            log("All tasks cleared");
            refreshStats();
        }
    }

    private void clearForm() {
        tfTitle.setText(""); tfAssignee.setText("");
        cbPriority.setSelectedIndex(0); cbStatus.setSelectedIndex(0);
        taskTable.clearSelection();
    }

    private void applyFilter() {
        String filter = cbFilter.getSelectedItem().toString();
        tableModel.setRowCount(0);
        populateSampleData();
        if (filter.equals("All Tasks")) return;
        for (int i = tableModel.getRowCount() - 1; i >= 0; i--) {
            String priority = tableModel.getValueAt(i, 3).toString();
            String status   = tableModel.getValueAt(i, 4).toString();
            boolean keep = false;
            switch (filter) {
                case "High Priority": keep = priority.equals("High"); break;
                case "In Progress":   keep = status.equals("In Progress"); break;
                case "Completed":     keep = status.equals("Completed"); break;
                case "Pending":       keep = status.equals("Pending"); break;
            }
            if (!keep) tableModel.removeRow(i);
        }
        log("Filter applied: " + filter);
    }

    private void exportCSV() {
        StringBuilder sb = new StringBuilder(String.join(",", COLUMNS) + "\n");
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            for (int j = 0; j < COLUMNS.length; j++) {
                sb.append(tableModel.getValueAt(i, j));
                if (j < COLUMNS.length - 1) sb.append(",");
            }
            sb.append("\n");
        }
        JOptionPane.showMessageDialog(this, "CSV Preview:\n\n" + sb.toString().substring(0, Math.min(sb.length(), 300)) + "...",
                "Export CSV", JOptionPane.INFORMATION_MESSAGE);
        log("Exported CSV (" + tableModel.getRowCount() + " rows)");
    }

    // ─── Stats Refresh ───────────────────────────────────────────────────────
    private void refreshStats() {
        int total = tableModel.getRowCount();
        int done = 0, inProg = 0, pending = 0, high = 0, medium = 0, low = 0;
        for (int i = 0; i < total; i++) {
            String status   = tableModel.getValueAt(i, 4).toString();
            String priority = tableModel.getValueAt(i, 3).toString();
            if (status.equals("Completed"))   done++;
            if (status.equals("In Progress")) inProg++;
            if (status.equals("Pending"))     pending++;
            if (priority.equals("High"))   high++;
            if (priority.equals("Medium")) medium++;
            if (priority.equals("Low"))    low++;
        }
        lblTotalCount.setText(String.valueOf(total));
        lblDoneCount.setText(String.valueOf(done));
        lblPendingCount.setText(String.valueOf(pending));

        pbTotal.setValue(total > 0 ? (done * 100 / total) : 0);
        pbHigh.setValue(high   > 0 ? (countDoneByPriority("High")   * 100 / high)   : 0);
        pbMedium.setValue(medium > 0 ? (countDoneByPriority("Medium") * 100 / medium) : 0);
        pbLow.setValue(low    > 0 ? (countDoneByPriority("Low")    * 100 / low)    : 0);
    }

    private int countDoneByPriority(String priority) {
        int count = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++)
            if (tableModel.getValueAt(i, 3).toString().equals(priority)
                    && tableModel.getValueAt(i, 4).toString().equals("Completed")) count++;
        return count;
    }

    private void log(String msg) {
        activityModel.add(0, "▶  " + msg);
        if (activityModel.size() > 20) activityModel.remove(activityModel.size() - 1);
    }

    // ─── UI Helpers ──────────────────────────────────────────────────────────
    private JLabel styledLabel(String text, Color color, Font font) {
        JLabel l = new JLabel(text); l.setForeground(color); l.setFont(font); return l;
    }
    private JTextField darkTextField() {
        JTextField f = new JTextField();
        f.setBackground(SURFACE2); f.setForeground(TEXT); f.setCaretColor(ACCENT);
        f.setFont(FONT_BODY); f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SURFACE2), new EmptyBorder(4, 8, 4, 8)));
        return f;
    }
    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setBackground(SURFACE2); cb.setForeground(TEXT); cb.setFont(FONT_BODY);
        return cb;
    }
    private JButton accentButton(String text) {
        JButton b = new JButton(text); b.setFont(FONT_BOLD);
        b.setBackground(ACCENT2); b.setForeground(Color.WHITE);
        b.setBorder(new EmptyBorder(7, 14, 7, 14)); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private JButton dangerButton(String text) {
        JButton b = new JButton(text); b.setFont(FONT_BOLD);
        b.setBackground(DANGER); b.setForeground(Color.WHITE);
        b.setBorder(new EmptyBorder(7, 14, 7, 14)); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private JButton ghostButton(String text) {
        JButton b = new JButton(text); b.setFont(FONT_BODY);
        b.setBackground(SURFACE2); b.setForeground(TEXT_MUTED);
        b.setBorder(new EmptyBorder(7, 14, 7, 14)); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private JPanel surfacePanel(String t) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(SURFACE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SURFACE2),
                new EmptyBorder(12, 14, 12, 14)));
        return p;
    }
    private JPanel wrapInSurface(JPanel inner, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(SURFACE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SURFACE2),
                new EmptyBorder(12, 14, 12, 14)));
        p.add(styledLabel(title, ACCENT, FONT_BOLD), BorderLayout.NORTH);
        p.add(inner, BorderLayout.CENTER);
        return p;
    }


}