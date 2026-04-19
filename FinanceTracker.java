import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class FinanceTracker extends JFrame {

    // ── GROQ API ───────────────────────────────────────────
    static String API_KEY = "";
    static final String API_URL  = "https://api.groq.com/openai/v1/chat/completions";
    static final String AI_MODEL = "llama-3.1-8b-instant";

    // ── FILES ──────────────────────────────────────────────
    static final String DATA_FILE   = "transactions.csv";
    static final String BUDGET_FILE = "budgets.csv";

    // ── COLOURS ────────────────────────────────────────────
    static final Color C_BG      = new Color(12,  14,  23);
    static final Color C_SURFACE = new Color(20,  23,  38);
    static final Color C_CARD    = new Color(26,  30,  50);
    static final Color C_BORDER  = new Color(45,  52,  80);
    static final Color C_ACCENT  = new Color(99,  179, 237);
    static final Color C_GREEN   = new Color(72,  199, 142);
    static final Color C_RED     = new Color(252, 100, 100);
    static final Color C_YELLOW  = new Color(250, 189, 47);
    static final Color C_PURPLE  = new Color(160, 120, 255);
    static final Color C_TEXT    = new Color(220, 225, 240);
    static final Color C_MUTED   = new Color(120, 130, 160);
    static final Color C_HOVER   = new Color(36,  42,  68);
    static final Color C_SEL     = new Color(50,  60,  95);

    // ── FONTS ──────────────────────────────────────────────
    static final Font F_TITLE = new Font("Segoe UI", Font.BOLD,  22);
    static final Font F_HEAD  = new Font("Segoe UI", Font.BOLD,  15);
    static final Font F_BODY  = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font F_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    static final Font F_BOLD  = new Font("Segoe UI", Font.BOLD,  13);
    static final Font F_MONO  = new Font("Consolas", Font.PLAIN, 12);
    static final Font F_NUM   = new Font("Segoe UI", Font.BOLD,  26);

    // ── CATEGORIES ─────────────────────────────────────────
    static final String[] EXP_CATS = {
        "Food & Dining", "Transport", "Education",
        "Entertainment", "Shopping",  "Healthcare",
        "Rent & Housing","Utilities", "Other"
    };
    static final String[] INC_CATS = {
        "Salary", "Pocket Money", "Freelance", "Part-time", "Other Income"
    };

    // ── DATA ───────────────────────────────────────────────
    static ArrayList<String[]>       transactions = new ArrayList<>();
    static HashMap<String, Double>   budgets      = new HashMap<>();

    // ── UI REFS ────────────────────────────────────────────
    JPanel            contentArea;
    JLabel            lblBalance, lblIncome, lblExpense, lblScore;
    DefaultTableModel tableModel;
    JTextArea         aiOutput;
    JLabel            statusBar;
    String            activeTab = "dashboard";

    // ══════════════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(
                    UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new FinanceTracker().setVisible(true);
        });
    }

    public FinanceTracker() {
        setTitle("Smart Finance Tracker");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 740);
        setMinimumSize(new Dimension(920, 580));
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG);
        loadData();
        buildUI();
    }

    // ──────────────────────────────────────────────────────
    void buildUI() {
        setLayout(new BorderLayout());
        add(buildSidebar(),   BorderLayout.WEST);
        add(buildContent(),   BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        showTab("dashboard");
    }

    // ══════════════════════════════════════════════════════
    //  SIDEBAR
    // ══════════════════════════════════════════════════════
    JPanel buildSidebar() {
        JPanel sb = new JPanel();
        sb.setPreferredSize(new Dimension(210, 0));
        sb.setBackground(C_SURFACE);
        sb.setLayout(new BoxLayout(sb, BoxLayout.Y_AXIS));
        sb.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, C_BORDER));

        // Logo block
        JPanel logo = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 18));
        logo.setBackground(C_SURFACE);
        logo.setMaximumSize(new Dimension(210, 68));

        // Painted accent square as icon
        JPanel icon = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(C_SURFACE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                String t = "FT";
                g2.drawString(t,
                    (getWidth()  - fm.stringWidth(t)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        icon.setPreferredSize(new Dimension(32, 32));
        icon.setBackground(C_SURFACE);

        JPanel txt = new JPanel();
        txt.setBackground(C_SURFACE);
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        JLabel name = new JLabel("FinTrack");
        name.setFont(new Font("Segoe UI", Font.BOLD, 16));
        name.setForeground(C_TEXT);
        JLabel sub = new JLabel("Personal Finance");
        sub.setFont(F_SMALL);
        sub.setForeground(C_MUTED);
        txt.add(name); txt.add(sub);

        logo.add(icon); logo.add(txt);
        sb.add(logo);
        sb.add(hRule());
        sb.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel nav = new JLabel("  MENU");
        nav.setFont(new Font("Segoe UI", Font.BOLD, 9));
        nav.setForeground(new Color(70, 80, 110));
        nav.setMaximumSize(new Dimension(210, 22));
        nav.setAlignmentX(LEFT_ALIGNMENT);
        sb.add(nav);
        sb.add(Box.createRigidArea(new Dimension(0, 4)));

        String[][] items = {
            {"Dashboard",       "dashboard"},
            {"Add Transaction",  "add"},
            {"Transactions",     "transactions"},
            {"AI Advisor",       "ai"},
            {"Budgets",          "budgets"},
            {"Settings",         "settings"},
        };
        for (String[] it : items) sb.add(navBtn(it[0], it[1]));

        sb.add(Box.createVerticalGlue());
        sb.add(hRule());
        JLabel ver = new JLabel("  v2.0  |  Groq AI");
        ver.setFont(F_SMALL);
        ver.setForeground(new Color(50, 60, 90));
        ver.setMaximumSize(new Dimension(210, 30));
        ver.setBorder(new EmptyBorder(8, 0, 10, 0));
        sb.add(ver);
        return sb;
    }

    JButton navBtn(String label, String tab) {
        JButton b = new JButton("  " + label);
        b.setForeground(C_MUTED);
        b.setBackground(C_SURFACE);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(F_BODY);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMaximumSize(new Dimension(210, 42));
        b.setPreferredSize(new Dimension(210, 42));
        b.setBorder(new EmptyBorder(0, 6, 0, 0));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!activeTab.equals(tab)) {
                    b.setBackground(C_HOVER);
                    b.setForeground(C_TEXT);
                }
            }
            public void mouseExited(MouseEvent e) {
                if (!activeTab.equals(tab)) {
                    b.setBackground(C_SURFACE);
                    b.setForeground(C_MUTED);
                }
            }
        });
        b.addActionListener(e -> showTab(tab));
        return b;
    }

    void highlightNav() {
        Component west = ((BorderLayout) getContentPane().getLayout())
                            .getLayoutComponent(BorderLayout.WEST);
        if (!(west instanceof JPanel)) return;
        for (Component c : ((JPanel) west).getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                String lbl  = b.getText().trim();
                boolean act = tabFor(lbl).equals(activeTab);
                b.setBackground(act ? C_HOVER    : C_SURFACE);
                b.setForeground(act ? C_ACCENT   : C_MUTED);
                b.setFont(act ? F_BOLD : F_BODY);
            }
        }
    }

    String tabFor(String lbl) {
        switch (lbl) {
            case "Dashboard":       return "dashboard";
            case "Add Transaction": return "add";
            case "Transactions":    return "transactions";
            case "AI Advisor":      return "ai";
            case "Budgets":         return "budgets";
            case "Settings":        return "settings";
            default:                return "";
        }
    }

    // ══════════════════════════════════════════════════════
    //  CONTENT CARDS
    // ══════════════════════════════════════════════════════
    JPanel buildContent() {
        contentArea = new JPanel(new CardLayout());
        contentArea.setBackground(C_BG);
        contentArea.add(panelDashboard(),    "dashboard");
        contentArea.add(panelAdd(),          "add");
        contentArea.add(panelTransactions(), "transactions");
        contentArea.add(panelAI(),           "ai");
        contentArea.add(panelBudgets(),      "budgets");
        contentArea.add(panelSettings(),     "settings");
        return contentArea;
    }

    void showTab(String name) {
        activeTab = name;
        ((CardLayout) contentArea.getLayout()).show(contentArea, name);
        if (name.equals("dashboard"))    refreshDashboard();
        if (name.equals("transactions")) refreshTable();
        highlightNav();
    }

    // ══════════════════════════════════════════════════════
    //  DASHBOARD
    // ══════════════════════════════════════════════════════
    JPanel panelDashboard() {
        JPanel p = bg(new BorderLayout(0, 18));
        p.setBorder(new EmptyBorder(24, 26, 24, 26));

        JPanel topRow = bg(new BorderLayout());
        JButton refresh = pillBtn("Refresh", C_ACCENT);
        refresh.addActionListener(e -> refreshDashboard());
        topRow.add(pageTitle("Dashboard"), BorderLayout.WEST);
        topRow.add(refresh,                BorderLayout.EAST);
        p.add(topRow, BorderLayout.NORTH);

        JPanel cards = bg(new GridLayout(1, 4, 14, 0));
        lblBalance = numLabel("--", C_ACCENT);
        lblIncome  = numLabel("--", C_GREEN);
        lblExpense = numLabel("--", C_RED);
        lblScore   = numLabel("--", C_PURPLE);
        cards.add(statCard("Net Balance",    lblBalance, C_ACCENT));
        cards.add(statCard("Total Income",   lblIncome,  C_GREEN));
        cards.add(statCard("Total Expenses", lblExpense, C_RED));
        cards.add(statCard("Health Score",   lblScore,   C_PURPLE));

        JPanel lower = bg(new GridLayout(1, 2, 14, 0));
        lower.add(recentPanel());
        lower.add(catBarPanel());

        JPanel center = bg(new BorderLayout(0, 14));
        center.add(cards, BorderLayout.NORTH);
        center.add(lower, BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    JPanel statCard(String label, JLabel valLbl, Color accent) {
        JPanel c = card();
        c.setLayout(new BorderLayout(0, 10));
        JPanel bar = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
            }
        };
        bar.setPreferredSize(new Dimension(0, 4));
        bar.setOpaque(false);
        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(F_SMALL);
        lbl.setForeground(C_MUTED);
        valLbl.setHorizontalAlignment(SwingConstants.CENTER);
        c.add(bar,    BorderLayout.NORTH);
        c.add(valLbl, BorderLayout.CENTER);
        c.add(lbl,    BorderLayout.SOUTH);
        return c;
    }

    JPanel recentPanel() {
        JPanel c = card();
        c.setLayout(new BorderLayout(0, 10));
        c.add(secLabel("Recent Transactions"), BorderLayout.NORTH);
        JPanel list = new JPanel();
        list.setName("recentList");
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(C_CARD);
        c.add(scrollOf(list), BorderLayout.CENTER);
        return c;
    }

    JPanel catBarPanel() {
        JPanel c = card();
        c.setLayout(new BorderLayout(0, 10));
        c.add(secLabel("Spending by Category"), BorderLayout.NORTH);
        JPanel bars = new JPanel();
        bars.setName("catBars");
        bars.setLayout(new BoxLayout(bars, BoxLayout.Y_AXIS));
        bars.setBackground(C_CARD);
        c.add(scrollOf(bars), BorderLayout.CENTER);
        return c;
    }

    void refreshDashboard() {
        double income  = totalIncome();
        double expense = totalExpense();
        double balance = income - expense;
        int    score   = healthScore();

        lblBalance.setText(String.format("Rs %.0f", balance));
        lblBalance.setForeground(balance >= 0 ? C_ACCENT : C_RED);
        lblIncome.setText(String.format("Rs %.0f",  income));
        lblExpense.setText(String.format("Rs %.0f", expense));
        lblScore.setText(score + " / 100");
        lblScore.setForeground(score >= 70 ? C_GREEN : score >= 40 ? C_YELLOW : C_RED);
        refreshRecentList();
        refreshCatBars();
    }

    void refreshRecentList() {
        JPanel list = findPanel(contentArea, "recentList");
        if (list == null) return;
        list.removeAll();
        if (transactions.isEmpty()) {
            list.add(mutedLbl("  No transactions yet."));
        } else {
            int from = Math.max(0, transactions.size() - 7);
            for (int i = transactions.size() - 1; i >= from; i--) {
                String[] t  = transactions.get(i);
                boolean inc = t[0].equals("Income");
                JPanel row  = new JPanel(new BorderLayout());
                row.setBackground(C_CARD);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
                row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));
                JLabel d = new JLabel("  " + t[2]);
                d.setFont(F_BODY); d.setForeground(C_TEXT);
                JLabel a = new JLabel((inc ? "+Rs " : "-Rs ") + t[3] + "  ");
                a.setFont(F_BOLD); a.setForeground(inc ? C_GREEN : C_RED);
                row.add(d, BorderLayout.WEST);
                row.add(a, BorderLayout.EAST);
                list.add(row);
            }
        }
        list.revalidate(); list.repaint();
    }

    void refreshCatBars() {
        JPanel bars = findPanel(contentArea, "catBars");
        if (bars == null) return;
        bars.removeAll();
        double total = totalExpense();
        if (total == 0) { bars.add(mutedLbl("  No expenses yet.")); }
        else {
            for (String cat : EXP_CATS) {
                double amt = catTotal(cat);
                if (amt <= 0) continue;
                int pct = (int) Math.min(100, (amt / total) * 100);
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setBackground(C_CARD);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                row.setBorder(new EmptyBorder(4, 4, 4, 8));
                JLabel lbl = new JLabel(cat);
                lbl.setFont(F_SMALL); lbl.setForeground(C_MUTED);
                lbl.setPreferredSize(new Dimension(110, 16));
                JProgressBar bar = new JProgressBar(0, 100);
                bar.setValue(pct);
                bar.setForeground(pct > 50 ? C_RED : pct > 30 ? C_YELLOW : C_GREEN);
                bar.setBackground(C_BORDER);
                bar.setStringPainted(true);
                bar.setString(String.format("Rs %.0f  (%d%%)", amt, pct));
                bar.setFont(F_SMALL);
                bar.setBorder(null);
                row.add(lbl, BorderLayout.WEST);
                row.add(bar, BorderLayout.CENTER);
                bars.add(row);
            }
        }
        bars.revalidate(); bars.repaint();
    }

    // ══════════════════════════════════════════════════════
    //  ADD TRANSACTION
    // ══════════════════════════════════════════════════════
    JPanel panelAdd() {
        JPanel outer = bg(new BorderLayout(0, 20));
        outer.setBorder(new EmptyBorder(24, 26, 24, 26));
        outer.add(pageTitle("Add Transaction"), BorderLayout.NORTH);

        JPanel card = card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(5, 12, 5, 12);

        JToggleButton incBtn = toggleBtn("Income",  C_GREEN);
        JToggleButton expBtn = toggleBtn("Expense", C_RED);
        ButtonGroup bg2 = new ButtonGroup();
        bg2.add(incBtn); bg2.add(expBtn);
        expBtn.setSelected(true);

        JPanel typeRow = new JPanel(new GridLayout(1, 2, 10, 0));
        typeRow.setBackground(C_CARD);
        typeRow.add(incBtn); typeRow.add(expBtn);

        JComboBox<String> catBox = styledCombo(EXP_CATS);
        JTextField descF  = field("e.g. Grocery, Uber, Netflix");
        JTextField amtF   = field("Amount in Rs");
        JTextField dateF  = field(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

        incBtn.addActionListener(e ->
            catBox.setModel(new DefaultComboBoxModel<>(INC_CATS)));
        expBtn.addActionListener(e ->
            catBox.setModel(new DefaultComboBoxModel<>(EXP_CATS)));

        formRow(card, g, 0, "Transaction Type", typeRow);
        formRow(card, g, 1, "Category",          catBox);
        formRow(card, g, 2, "Description",        descF);
        formRow(card, g, 3, "Amount (Rs)",         amtF);
        formRow(card, g, 4, "Date (dd/MM/yyyy)",   dateF);

        JButton addBtn   = pillBtn("Add Transaction", C_ACCENT);
        JButton clearBtn = pillBtn("Clear",           C_BORDER);

        addBtn.addActionListener(e -> {
            try {
                String type = incBtn.isSelected() ? "Income" : "Expense";
                String cat  = (String) catBox.getSelectedItem();
                String desc = descF.getText().trim();
                String amts = amtF.getText().trim();
                String date = dateF.getText().trim();
                if (desc.isEmpty()) throw new Exception("Please enter a description.");
                if (amts.isEmpty()) throw new Exception("Please enter an amount.");
                double amount = Double.parseDouble(amts);
                if (amount <= 0) throw new Exception("Amount must be positive.");
                transactions.add(new String[]{
                    type, cat, desc, String.format("%.2f", amount), date
                });
                saveData();
                refreshDashboard();
                refreshTable();
                descF.setText(""); amtF.setText("");
                setStatus("Added: " + type + "  Rs " + amount + "  [" + cat + "]");
                showTab("dashboard");
            } catch (NumberFormatException ex) {
                err("Please enter a valid number for amount.");
            } catch (Exception ex) {
                err(ex.getMessage());
            }
        });

        clearBtn.addActionListener(e -> {
            descF.setText(""); amtF.setText("");
            expBtn.setSelected(true);
            catBox.setModel(new DefaultComboBoxModel<>(EXP_CATS));
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.setBackground(C_CARD);
        btnRow.add(addBtn); btnRow.add(clearBtn);
        g.gridx=0; g.gridy=12; g.gridwidth=2;
        g.insets=new Insets(22, 12, 12, 12);
        card.add(btnRow, g);

        JPanel wrap = bg(new GridBagLayout());
        GridBagConstraints wg = new GridBagConstraints();
        wg.fill = GridBagConstraints.BOTH;
        wg.weightx = 0.5; wg.weighty = 1.0;
        wrap.add(card, wg);
        outer.add(wrap, BorderLayout.CENTER);
        return outer;
    }

    // ══════════════════════════════════════════════════════
    //  TRANSACTIONS TABLE
    // ══════════════════════════════════════════════════════
    JPanel panelTransactions() {
        JPanel p = bg(new BorderLayout(0, 16));
        p.setBorder(new EmptyBorder(24, 26, 24, 26));

        JTextField searchF = field("Search by description or category...");
        searchF.setPreferredSize(new Dimension(240, 32));
        JButton exportBtn = pillBtn("Export CSV", C_PURPLE);
        JButton deleteBtn = pillBtn("Delete Row", C_RED);
        exportBtn.addActionListener(e -> exportCSV());

        JPanel top = bg(new BorderLayout(12, 0));
        top.add(pageTitle("All Transactions"), BorderLayout.WEST);
        JPanel tr = bg(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JLabel sl = new JLabel("Search:");
        sl.setForeground(C_MUTED); sl.setFont(F_SMALL);
        tr.add(sl); tr.add(searchF); tr.add(exportBtn); tr.add(deleteBtn);
        top.add(tr, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        String[] cols = {"#", "Type", "Category", "Description", "Amount", "Date"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        styleTable(table);

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { setStatus("Select a row to delete."); return; }
            if (JOptionPane.showConfirmDialog(this, "Delete this transaction?",
                    "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                transactions.remove(row);
                saveData(); refreshTable(); refreshDashboard();
                setStatus("Transaction deleted.");
            }
        });

        searchF.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filterTable(searchF.getText().trim()); }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        sp.getViewport().setBackground(C_CARD);
        sp.setBackground(C_CARD);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    void refreshTable() {
        if (tableModel == null) return;
        tableModel.setRowCount(0);
        for (int i = 0; i < transactions.size(); i++) {
            String[] t = transactions.get(i);
            tableModel.addRow(new Object[]{
                i + 1, t[0], t[1], t[2], "Rs " + t[3], t[4]
            });
        }
    }

    void filterTable(String q) {
        if (tableModel == null) return;
        tableModel.setRowCount(0);
        int n = 1;
        for (String[] t : transactions) {
            if (t[2].toLowerCase().contains(q.toLowerCase()) ||
                t[1].toLowerCase().contains(q.toLowerCase())) {
                tableModel.addRow(new Object[]{ n, t[0], t[1], t[2], "Rs "+t[3], t[4] });
            }
            n++;
        }
    }

    // ══════════════════════════════════════════════════════
    //  AI ADVISOR
    // ══════════════════════════════════════════════════════
    JPanel panelAI() {
        JPanel p = bg(new BorderLayout(0, 16));
        p.setBorder(new EmptyBorder(24, 26, 24, 26));
        p.add(pageTitle("AI Advisor  -  Powered by Groq"), BorderLayout.NORTH);

        aiOutput = new JTextArea();
        aiOutput.setEditable(false);
        aiOutput.setBackground(C_CARD);
        aiOutput.setForeground(C_TEXT);
        aiOutput.setFont(F_MONO);
        aiOutput.setLineWrap(true);
        aiOutput.setWrapStyleWord(true);
        aiOutput.setBorder(new EmptyBorder(16, 18, 16, 18));
        aiOutput.setText(
            "Welcome to your AI Financial Advisor\n" +
            "=========================================\n\n" +
            "Click 'Analyse My Spending' to get personalised\n" +
            "advice based on your actual transaction data.\n\n" +
            "The AI will:\n" +
            "  > Review your income vs expense ratio\n" +
            "  > Identify your highest spending categories\n" +
            "  > Give specific money-saving tips\n" +
            "  > Suggest a realistic savings goal\n\n" +
            "You can also type a custom question below.\n\n" +
            "NOTE: Add your free Groq API key in Settings first.\n" +
            "      Get one free at console.groq.com"
        );

        JScrollPane sp = new JScrollPane(aiOutput);
        sp.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        sp.getViewport().setBackground(C_CARD);
        p.add(sp, BorderLayout.CENTER);

        JTextField customQ = field("Type a custom question here...");
        JButton analyseBtn = pillBtn("Analyse My Spending", C_ACCENT);
        JButton askBtn     = pillBtn("Ask",                 C_PURPLE);
        JProgressBar prog  = new JProgressBar();
        prog.setIndeterminate(false);
        prog.setVisible(false);
        prog.setForeground(C_ACCENT);
        prog.setBackground(C_SURFACE);
        prog.setBorder(null);

        analyseBtn.addActionListener(e -> {
            if (transactions.isEmpty()) { err("Add some transactions first."); return; }
            runAI(buildPrompt(null), analyseBtn, prog);
        });
        askBtn.addActionListener(e -> {
            String q = customQ.getText().trim();
            if (!q.isEmpty()) runAI(buildPrompt(q), askBtn, prog);
        });

        JPanel qRow = bg(new BorderLayout(8, 0));
        qRow.add(customQ, BorderLayout.CENTER);
        qRow.add(askBtn,  BorderLayout.EAST);

        JPanel btnRow = bg(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnRow.add(analyseBtn);

        JPanel bottom = bg(new BorderLayout(0, 8));
        bottom.add(btnRow, BorderLayout.NORTH);
        bottom.add(qRow,   BorderLayout.CENTER);
        bottom.add(prog,   BorderLayout.SOUTH);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    void runAI(String prompt, JButton btn, JProgressBar prog) {
        btn.setEnabled(false);
        prog.setVisible(true);
        prog.setIndeterminate(true);
        aiOutput.setText("Connecting to Groq AI...\n\nPlease wait.");

        SwingWorker<String, Void> w = new SwingWorker<>() {
            protected String doInBackground() { return callGroqAPI(prompt); }
            protected void done() {
                try { aiOutput.setText(get()); }
                catch (Exception ex) { aiOutput.setText("Error: " + ex.getMessage()); }
                btn.setEnabled(true);
                prog.setVisible(false);
                prog.setIndeterminate(false);
            }
        };
        w.execute();
    }

    String buildPrompt(String custom) {
        double income  = totalIncome();
        double expense = totalExpense();
        double balance = income - expense;
        int    score   = healthScore();

        StringBuilder sb = new StringBuilder();
        sb.append("You are a friendly financial advisor. Be concise (under 300 words), ")
          .append("practical and encouraging.\n\n")
          .append("=== FINANCIAL SUMMARY ===\n")
          .append(String.format("Total Income : Rs %.2f\n", income))
          .append(String.format("Total Expense: Rs %.2f\n", expense))
          .append(String.format("Net Balance  : Rs %.2f\n", balance))
          .append(String.format("Health Score : %d/100\n\n", score))
          .append("Spending by Category:\n");
        for (String cat : EXP_CATS) {
            double a = catTotal(cat);
            if (a > 0) sb.append(String.format("  %-20s Rs %.2f\n", cat, a));
        }
        sb.append("\nRecent Transactions:\n");
        int from = Math.max(0, transactions.size() - 5);
        for (int i = from; i < transactions.size(); i++) {
            String[] t = transactions.get(i);
            sb.append(String.format("  %s | %s | %s | Rs %s\n",
                t[0], t[1], t[2], t[3]));
        }
        if (custom != null && !custom.isEmpty()) {
            sb.append("\nQuestion: ").append(custom);
        } else {
            sb.append("\nPlease provide:\n")
              .append("1. Brief overall assessment\n")
              .append("2. Top 2-3 spending concerns\n")
              .append("3. Three practical money-saving tips\n")
              .append("4. A specific savings target for next month");
        }
        return sb.toString();
    }

    // ── GROQ API CALL ──────────────────────────────────────
    String callGroqAPI(String prompt) {
        if (API_KEY == null || API_KEY.trim().isEmpty()) {
            return "No API Key configured.\n\n" +
                   "Go to Settings and paste your free Groq API key.\n" +
                   "Get one at: console.groq.com  (no credit card needed)";
        }
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",  "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY.trim());
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(45000);

            // Escape prompt for JSON
            String safe = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");

            String body = "{\"model\":\"" + AI_MODEL + "\","
                + "\"max_tokens\":1024,"
                + "\"messages\":[{\"role\":\"user\",\"content\":\""
                + safe + "\"}]}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream is = (code == 200)
                ? conn.getInputStream() : conn.getErrorStream();
            BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder res = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) res.append(line);
            br.close();

            // Parse "content":"..." from JSON
            String raw    = res.toString();
            String marker = "\"content\":\"";
            int idx = raw.indexOf(marker);
            if (idx < 0) return "API Error (code " + code + "):\n" + raw;

            int s = idx + marker.length();
            int e = s;
            while (e < raw.length()) {
                char ch = raw.charAt(e);
                if (ch == '"' && raw.charAt(e - 1) != '\\') break;
                e++;
            }
            String text = raw.substring(s, e)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");

            return "AI Advisor Response\n"
                 + "=".repeat(44) + "\n\n"
                 + text.trim()
                 + "\n\n" + "=".repeat(44)
                 + "\nTip: Set monthly limits in the Budgets tab.";

        } catch (Exception ex) {
            return "Connection failed: " + ex.getMessage() + "\n\n"
                 + "Check:\n"
                 + "  > Internet connection is active\n"
                 + "  > API key is correct (starts with gsk_)\n"
                 + "  > Go to Settings and re-paste the key";
        }
    }

    // ══════════════════════════════════════════════════════
    //  BUDGETS
    // ══════════════════════════════════════════════════════
    JPanel panelBudgets() {
        JPanel p = bg(new BorderLayout(0, 16));
        p.setBorder(new EmptyBorder(24, 26, 24, 26));
        p.add(pageTitle("Monthly Budgets"), BorderLayout.NORTH);

        JPanel grid = bg(new GridLayout(0, 3, 14, 14));
        for (String cat : EXP_CATS) {
            double spent  = catTotal(cat);
            double budget = budgets.getOrDefault(cat, 0.0);
            int    pct    = budget > 0
                ? (int) Math.min(100, (spent / budget) * 100) : 0;

            JPanel c = card();
            c.setLayout(new BorderLayout(0, 8));

            JLabel catLbl = new JLabel(cat);
            catLbl.setFont(F_BOLD); catLbl.setForeground(C_TEXT);

            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(pct);
            bar.setForeground(pct > 80 ? C_RED : pct > 60 ? C_YELLOW : C_GREEN);
            bar.setBackground(C_BORDER);
            bar.setStringPainted(true);
            bar.setString(budget > 0
                ? String.format("Rs %.0f / Rs %.0f  (%d%%)", spent, budget, pct)
                : String.format("Rs %.0f spent  (no limit)", spent));
            bar.setFont(F_SMALL);
            bar.setBorder(null);

            JTextField input = field(budget > 0
                ? String.format("%.0f", budget) : "Set limit...");

            JButton save = pillBtn("Set", C_ACCENT);
            save.addActionListener(e -> {
                try {
                    double b = Double.parseDouble(input.getText().trim());
                    budgets.put(cat, b);
                    saveBudgets();
                    setStatus("Budget saved: " + cat);
                } catch (NumberFormatException ex) { err("Enter a valid number."); }
            });

            JPanel inputRow = new JPanel(new BorderLayout(6, 0));
            inputRow.setBackground(C_CARD);
            inputRow.add(input, BorderLayout.CENTER);
            inputRow.add(save,  BorderLayout.EAST);

            c.add(catLbl,   BorderLayout.NORTH);
            c.add(bar,      BorderLayout.CENTER);
            c.add(inputRow, BorderLayout.SOUTH);
            grid.add(c);
        }

        JScrollPane sp = new JScrollPane(grid);
        sp.setBorder(null);
        sp.getViewport().setBackground(C_BG);
        sp.setBackground(C_BG);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ══════════════════════════════════════════════════════
    //  SETTINGS
    // ══════════════════════════════════════════════════════
    JPanel panelSettings() {
        JPanel p = bg(new BorderLayout(0, 16));
        p.setBorder(new EmptyBorder(24, 26, 24, 26));
        p.add(pageTitle("Settings"), BorderLayout.NORTH);

        JPanel c = card();
        c.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 14, 10, 14);
        g.fill   = GridBagConstraints.HORIZONTAL;
        g.gridwidth = 2;

        JLabel apiTitle = new JLabel("Groq API Key");
        apiTitle.setFont(F_HEAD); apiTitle.setForeground(C_TEXT);

        JLabel hint = new JLabel(
            "<html><span style='color:#647087;font-size:11px'>" +
            "Free at console.groq.com  -  key starts with gsk_..." +
            "</span></html>");

        JPasswordField apiField = new JPasswordField(API_KEY);
        styleField(apiField);

        JButton saveKey  = pillBtn("Save Key",       C_ACCENT);
        JButton clearAll = pillBtn("Clear All Data",  C_RED);

        saveKey.addActionListener(e -> {
            API_KEY = new String(apiField.getPassword()).trim();
            setStatus("API Key saved for this session.");
        });
        clearAll.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                    "Delete ALL transactions? This cannot be undone.",
                    "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                transactions.clear();
                saveData();
                refreshDashboard(); refreshTable();
                setStatus("All data cleared.");
            }
        });

        JLabel aboutTitle = new JLabel("About");
        aboutTitle.setFont(F_HEAD); aboutTitle.setForeground(C_TEXT);
        JLabel about = new JLabel(
            "<html><span style='color:#647087;font-size:11px'>" +
            "Smart Finance Tracker v2.0<br>" +
            "Built with Java Swing + Groq AI (Llama 3 8B)<br>" +
            "Data saved locally to transactions.csv</span></html>");

        g.gridx=0; g.gridy=0; c.add(apiTitle,  g);
        g.gridy=1;             c.add(hint,       g);
        g.gridy=2;             c.add(apiField,   g);
        g.gridwidth=1;
        g.gridy=3; g.gridx=0; c.add(saveKey,    g);
                   g.gridx=1; c.add(clearAll,    g);
        g.gridwidth=2;
        g.gridy=4; g.gridx=0;
        g.insets=new Insets(28,14,4,14);  c.add(aboutTitle, g);
        g.gridy=5;
        g.insets=new Insets(4,14,14,14);  c.add(about,      g);

        JPanel wrap = bg(new GridBagLayout());
        GridBagConstraints wg = new GridBagConstraints();
        wg.fill=GridBagConstraints.BOTH;
        wg.weightx=0.45; wg.weighty=0.6;
        wrap.add(c, wg);
        p.add(wrap, BorderLayout.CENTER);
        return p;
    }

    // ══════════════════════════════════════════════════════
    //  STATUS BAR
    // ══════════════════════════════════════════════════════
    JLabel buildStatusBar() {
        statusBar = new JLabel("  Ready");
        statusBar.setFont(F_SMALL);
        statusBar.setForeground(C_MUTED);
        statusBar.setBackground(C_SURFACE);
        statusBar.setOpaque(true);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER),
            new EmptyBorder(5, 14, 5, 14)));
        return statusBar;
    }

    void setStatus(String msg) {
        if (statusBar != null) statusBar.setText("  " + msg);
    }

    // ══════════════════════════════════════════════════════
    //  CALCULATIONS
    // ══════════════════════════════════════════════════════
    double totalIncome() {
        double t = 0;
        for (String[] tx : transactions)
            if (tx[0].equals("Income")) t += Double.parseDouble(tx[3]);
        return t;
    }

    double totalExpense() {
        double t = 0;
        for (String[] tx : transactions)
            if (tx[0].equals("Expense")) t += Double.parseDouble(tx[3]);
        return t;
    }

    double catTotal(String cat) {
        double t = 0;
        for (String[] tx : transactions)
            if (tx[0].equals("Expense") && tx[1].equals(cat))
                t += Double.parseDouble(tx[3]);
        return t;
    }

    int healthScore() {
        if (transactions.isEmpty()) return 0;
        double income  = totalIncome();
        double expense = totalExpense();
        if (income == 0) return 10;
        int score = 0;
        score += (int) Math.max(0, Math.min(50,
            ((income - expense) / income) * 100));
        score += Math.min(20, transactions.size() * 2);
        int kept = 0, total = 0;
        for (String cat : EXP_CATS) {
            double b = budgets.getOrDefault(cat, 0.0);
            if (b > 0) { total++; if (catTotal(cat) <= b) kept++; }
        }
        score += total > 0 ? (int)((double) kept / total * 30) : 15;
        return Math.max(0, Math.min(100, score));
    }

    // ══════════════════════════════════════════════════════
    //  FILE I/O
    // ══════════════════════════════════════════════════════
    void saveData() {
        try (PrintWriter pw = new PrintWriter(
                new BufferedWriter(new FileWriter(DATA_FILE)))) {
            for (String[] t : transactions)
                pw.println(String.join("|", t));
        } catch (IOException ex) {
            setStatus("Could not save: " + ex.getMessage());
        }
    }

    void loadData() {
        File f = new File(DATA_FILE);
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length == 5) transactions.add(parts);
                }
            } catch (IOException ignored) {}
        }
        File bf = new File(BUDGET_FILE);
        if (bf.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(bf))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length == 2)
                        budgets.put(parts[0], Double.parseDouble(parts[1]));
                }
            } catch (IOException ignored) {}
        }
    }

    void saveBudgets() {
        try (PrintWriter pw = new PrintWriter(
                new BufferedWriter(new FileWriter(BUDGET_FILE)))) {
            for (Map.Entry<String, Double> e : budgets.entrySet())
                pw.println(e.getKey() + "|" + e.getValue());
        } catch (IOException ignored) {}
    }

    void exportCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("finance_export.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw =
                    new PrintWriter(new FileWriter(fc.getSelectedFile()))) {
                pw.println("Type,Category,Description,Amount,Date");
                for (String[] t : transactions)
                    pw.println(String.join(",", t));
                setStatus("Exported: " + fc.getSelectedFile().getName());
            } catch (IOException ex) { err("Export failed: " + ex.getMessage()); }
        }
    }

    // ══════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════
    JPanel bg(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(C_BG);
        return p;
    }

    JPanel card() {
        JPanel c = new JPanel();
        c.setBackground(C_CARD);
        c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER, 1),
            new EmptyBorder(16, 16, 16, 16)));
        return c;
    }

    JLabel pageTitle(String t) {
        JLabel l = new JLabel(t);
        l.setFont(F_TITLE);
        l.setForeground(C_TEXT);
        l.setBorder(new EmptyBorder(0, 0, 6, 0));
        return l;
    }

    JLabel secLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(F_BOLD);
        l.setForeground(C_ACCENT);
        return l;
    }

    JLabel numLabel(String t, Color c) {
        JLabel l = new JLabel(t, SwingConstants.CENTER);
        l.setFont(F_NUM);
        l.setForeground(c);
        return l;
    }

    JLabel mutedLbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        l.setForeground(C_MUTED);
        return l;
    }

    JButton pillBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(bg.equals(C_BORDER) ? C_TEXT : Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFont(F_BOLD);
        b.setBorder(new EmptyBorder(8, 18, 8, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    JToggleButton toggleBtn(String text, Color accent) {
        JToggleButton b = new JToggleButton(text);
        b.setFocusPainted(false);
        b.setFont(F_BOLD);
        b.setBackground(C_CARD);
        b.setForeground(accent);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent, 1),
            new EmptyBorder(8, 12, 8, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    JTextField field(String hint) {
        JTextField f = new JTextField();
        styleField(f);
        f.setToolTipText(hint);
        return f;
    }

    void styleField(JTextField f) {
        f.setBackground(new Color(14, 18, 34));
        f.setForeground(C_TEXT);
        f.setCaretColor(C_ACCENT);
        f.setFont(F_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER, 1),
            new EmptyBorder(6, 10, 6, 10)));
    }

    JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> b = new JComboBox<>(items);
        b.setBackground(new Color(14, 18, 34));
        b.setForeground(C_TEXT);
        b.setFont(F_BODY);
        return b;
    }

    void formRow(JPanel p, GridBagConstraints g, int row,
                 String label, JComponent field) {
        JLabel l = new JLabel(label);
        l.setForeground(C_MUTED); l.setFont(F_SMALL);
        g.gridx=0; g.gridy=row*2;   g.gridwidth=2; g.insets=new Insets(10,12,2,12);
        p.add(l, g);
        g.gridy=row*2+1; g.insets=new Insets(0,12,0,12);
        p.add(field, g);
    }

    void styleTable(JTable t) {
        t.setBackground(C_CARD);
        t.setForeground(C_TEXT);
        t.setFont(F_BODY);
        t.setRowHeight(38);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionBackground(C_SEL);
        t.setSelectionForeground(C_TEXT);
        t.setFocusable(false);
        JTableHeader h = t.getTableHeader();
        h.setBackground(C_SURFACE);
        h.setForeground(C_ACCENT);
        h.setFont(F_BOLD);
        h.setReorderingAllowed(false);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel,
                    boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tbl,val,sel,foc,r,c);
                setBackground(sel ? C_SEL
                    : (r % 2 == 0 ? C_CARD : new Color(22, 26, 44)));
                setForeground(C_TEXT);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                if (c == 1 && val != null) {
                    setForeground(val.toString().equals("Income")
                        ? C_GREEN : C_RED);
                    setFont(F_BOLD);
                } else {
                    setFont(F_BODY);
                }
                return this;
            }
        });
    }

    JScrollPane scrollOf(JPanel content) {
        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null);
        sp.getViewport().setBackground(C_CARD);
        sp.setBackground(C_CARD);
        return sp;
    }

    JSeparator hRule() {
        JSeparator s = new JSeparator();
        s.setForeground(C_BORDER);
        s.setMaximumSize(new Dimension(210, 1));
        return s;
    }

    void err(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error",
            JOptionPane.ERROR_MESSAGE);
    }

    JPanel findPanel(Container root, String name) {
        for (Component c : root.getComponents()) {
            if (c instanceof JPanel) {
                JPanel jp = (JPanel) c;
                if (name.equals(jp.getName())) return jp;
                JPanel found = findPanel(jp, name);
                if (found != null) return found;
            } else if (c instanceof Container) {
                JPanel found = findPanel((Container) c, name);
                if (found != null) return found;
            }
        }
        return null;
    }
}
