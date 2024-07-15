package expensetrackerui;
 
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.category.DefaultCategoryDataset;

@SuppressWarnings("deprecation")
class ExpenseTrackerUI {

    private static final int EXPENSE_FONT_SIZE = 14;
    private static final int LOGIN_FONT_SIZE = 16;
    private static final String FONT_FAMILY = "Helvitica";

    private Connection connection;
    private JFrame loginFrame;
    private JFrame expenseFrame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField emailField;
    private JTextField phoneField;
    private String loggedInUser;
    private JTable table;
    private JTextField dateField;
    private JComboBox<String> currencyComboBox;
    private JComboBox<String> categoryComboBox;
    private JComboBox<String> chartComboBox;
    private JLabel lblTotalAmount;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                ExpenseTrackerUI window = new ExpenseTrackerUI();
                window.initializeLoginUI();
                window.loginFrame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private JTextField descField;
    private JTextField amountField;

    public ExpenseTrackerUI() {
        connectToDatabase();
        initializeExpenseTrackerUI();
    }

    private void initializeExpenseTrackerUI() {

        Font defaultFont = new Font(FONT_FAMILY, Font.BOLD, EXPENSE_FONT_SIZE);
        UIManager.put("TextField.font", defaultFont);
        UIManager.put("PasswordField.font", defaultFont);
        UIManager.put("TextArea.font", defaultFont);
        UIManager.put("ComboBox.font", defaultFont);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("Button.font", defaultFont);
        UIManager.put("Table.font", defaultFont);
        UIManager.put("TableHeader.font", defaultFont);
        UIManager.put("OptionPane.messageFont", defaultFont);
        UIManager.put("OptionPane.buttonFont", defaultFont);

        expenseFrame = new JFrame();
        expenseFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        expenseFrame.getContentPane().setBackground(Color.BLACK);
        ImageIcon icon = new ImageIcon(
            "C:\\Users\\KUNSH\\Downloads\\WhatsApp Image 2024-03-21 at 11.14.20_1b3c5b1b.jpg");
        expenseFrame.setIconImage(icon.getImage());
        expenseFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        expenseFrame.setTitle("Expense Tracker");

        ImageIcon backgroundImage = new ImageIcon(
                "C:\\Users\\KUNSH\\Downloads\\WhatsApp Image 2024-04-09 at 19.08.23_c408675e.jpg");
        @SuppressWarnings("unused")
        JLabel backgroundLabel = new JLabel(backgroundImage) {
            @Override
            public void paintComponent(Graphics g) {
                Dimension size = loginFrame.getSize();
                g.drawImage(backgroundImage.getImage(), 0, 0, size.width, size.height, null);
            }
        };

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        expenseFrame.getContentPane().add(topPanel, BorderLayout.NORTH);
        topPanel.setBackground(new Color(191, 216, 189));

        JLabel lblTotalExpense = new JLabel("Total Expense : INR");
        topPanel.add(lblTotalExpense);

        lblTotalAmount = new JLabel();
        topPanel.add(lblTotalAmount);

        JButton btnPrintRecords = new JButton("Print Records");
        btnPrintRecords.addActionListener(e -> printExpenseRecords());
        topPanel.add(btnPrintRecords);

        String[] chartOptions = { "~Chart Based Analysis~", "Daily Expenses", "Monthly Expenses", "Category Expenses" };
        chartComboBox = new JComboBox<>(chartOptions);
        chartComboBox.setSelectedItem("~Chart based Analysis~");
        chartComboBox.addActionListener(e -> generateSelectedChart());
        topPanel.add(chartComboBox);

        JButton btnProfile = new JButton("Profile");
        btnProfile.addActionListener(e -> showProfile());
        topPanel.add(btnProfile);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        expenseFrame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        bottomPanel.setBackground(new Color(191, 216, 189));

        JLabel dateLabel = new JLabel("Date:");
        bottomPanel.add(dateLabel);

        dateField = new JTextField(10);
        dateField.setText("dd/mm/yy");
        dateField.setForeground(new Color(119, 123, 126));
        bottomPanel.add(dateField);

        JLabel descLabel = new JLabel("Description:");
        bottomPanel.add(descLabel);

        descField = new JTextField(10);
        bottomPanel.add(descField);

        JLabel amountLabel = new JLabel("Amount:");
        bottomPanel.add(amountLabel);

        amountField = new JTextField(10);
        bottomPanel.add(amountField);

        currencyComboBox = new JComboBox<>(new String[] { "INR", "USD", "EUR", "GBP" });
        bottomPanel.add(currencyComboBox);

        JLabel categoryLabel = new JLabel("Category:");
        bottomPanel.add(categoryLabel);

        categoryComboBox = new JComboBox<>(
                new String[] { "Select Category", "Groceries", "Utilities", "Transportation", "Entertainment" });
        bottomPanel.add(categoryComboBox);

        JButton btnAddExpense = new JButton("Add Expense");
        btnAddExpense.addActionListener(e -> addExpense());
        bottomPanel.add(btnAddExpense);

        JButton btnDeleteExpense = new JButton("Delete Expense");
        btnDeleteExpense.addActionListener(e -> deleteExpense());
        bottomPanel.add(btnDeleteExpense);

        JButton btnEditExpense = new JButton("Edit Expense");
        btnEditExpense.addActionListener(e -> editExpense());
        bottomPanel.add(btnEditExpense);

        JScrollPane scrollPane = new JScrollPane();
        expenseFrame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        scrollPane.getViewport().setBackground(new Color(191, 216, 189));

        table = new JTable();
        table.setModel(new DefaultTableModel(
                new Object[][] { { null, null, null, null, null } },
                new String[] { "Date", "Description", "Currency", "Amount", "Category" }));
        scrollPane.setViewportView(table);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    table.setRowSelectionInterval(row, row);
                }
            }
        });

        applyButtonStyle(btnPrintRecords);
        applyButtonStyle(btnProfile);
        applyButtonStyle(btnAddExpense);
        applyButtonStyle(btnDeleteExpense);
        applyButtonStyle(btnEditExpense);

        updateTable();
    }

    private void initializeLoginUI() {

        Font defaultFont = new Font(FONT_FAMILY, Font.BOLD, LOGIN_FONT_SIZE);

        UIManager.put("TextField.font", defaultFont); 
        UIManager.put("PasswordField.font", defaultFont); 
        UIManager.put("Label.font", defaultFont); 
        UIManager.put("Button.font", defaultFont); 
 
        loginFrame = new JFrame();
        loginFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        ImageIcon icon = new ImageIcon(
                "C:\\Users\\KUNSH\\Downloads\\WhatsApp Image 2024-03-21 at 11.14.20_1b3c5b1b.jpg");
        loginFrame.setIconImage(icon.getImage());
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setTitle("Expense Tracker - Sign In");

        ImageIcon backgroundImage = new ImageIcon(
                "C:\\Users\\KUNSH\\Downloads\\WhatsApp Image 2024-04-09 at 19.08.23_c408675e.jpg");
        JLabel backgroundLabel = new JLabel(backgroundImage) {
            @Override
            public void paintComponent(Graphics g) {
                Dimension size = loginFrame.getSize();
                g.drawImage(backgroundImage.getImage(), 0, 0, size.width, size.height, null);
            }
        };

        loginFrame.setContentPane(backgroundLabel);

        loginFrame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); 
        JLabel lblUsername = new JLabel("Username:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        loginFrame.add(lblUsername, gbc);

        usernameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        loginFrame.add(usernameField, gbc);

        JLabel lblPassword = new JLabel("Password:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        loginFrame.add(lblPassword, gbc);

        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        loginFrame.add(passwordField, gbc);

        JButton btnLogin = new JButton("Sign in");
        btnLogin.addActionListener(e -> authenticate());
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        loginFrame.add(btnLogin, gbc);

        JButton btnSignUp = new JButton("Sign Up");
        btnSignUp.addActionListener(e -> {
            @SuppressWarnings("unused")
            String username = usernameField.getText();
            @SuppressWarnings("unused")
            String password = new String(passwordField.getPassword());
            signUp();
        });
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        loginFrame.add(btnSignUp, gbc);

        loginFrame.setLocationRelativeTo(null);

        loginFrame.setVisible(true);
    }

    private void signUp() {
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        emailField = new JTextField(20);
        phoneField = new JTextField(20);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(10, 10, 10, 10);

        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(new JLabel("Username:"), constraints);

        constraints.gridx = 1;
        panel.add(usernameField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        panel.add(new JLabel("Password:"), constraints);

        constraints.gridx = 1;
        panel.add(passwordField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        panel.add(new JLabel("Email:"), constraints);

        constraints.gridx = 1;
        panel.add(emailField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        panel.add(new JLabel("Phone:"), constraints);

        constraints.gridx = 1;
        panel.add(phoneField, constraints);

        int result = JOptionPane.showConfirmDialog(null, panel, "Sign Up", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText();
            char[] passwordChars = passwordField.getPassword();
            String password = new String(passwordChars);
            String email = emailField.getText();
            String phone = phoneField.getText();

            if (username.isEmpty() || passwordChars.length == 0 || email.isEmpty() || phone.isEmpty()) {
                showErrorDialog("Please fill in all fields.");
                return;
            }

            try {
                String query = "INSERT INTO users (username, password, email, phone) VALUES (?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, username);
                    statement.setString(2, password);
                    statement.setString(3, email);
                    statement.setString(4, phone);
                    statement.executeUpdate();
                    JOptionPane.showMessageDialog(null, "User signed up successfully!");
                }
            } catch (SQLException e) {
                showErrorDialog("Failed to sign up: " + e.getMessage());
            } finally {
                Arrays.fill(passwordChars, ' ');
            }
        }
    }

    private void authenticate() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        try {
            String sql = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, username);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        String storedPassword = resultSet.getString("password");
                        if (password.equals(storedPassword)) {
                            loggedInUser = username;
                            loginFrame.dispose();
                            expenseFrame.getContentPane().removeAll();
                            expenseFrame.revalidate();
                            expenseFrame.repaint();
                            initializeExpenseTrackerUI();
                            expenseFrame.setVisible(true);
                        } else {
                            JOptionPane.showMessageDialog(expenseFrame, "Incorrect password", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(expenseFrame, "Username not found", "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to authenticate: " + e.getMessage());
        }
    }

    private void showProfile() {
        try {
            String username = usernameField.getText();
            String query = "SELECT phone, email, total_expense FROM users WHERE username = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, username);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String phone = resultSet.getString("phone");
                        String email = resultSet.getString("email");

                        Object[] message = {
                                "Username: " + username + "\n" +
                                        "Phone: " + phone + "\n" +
                                        "Email: " + email + "\n"
                        };

                        int option = JOptionPane.showOptionDialog(
                                expenseFrame,
                                message,
                                "Profile Information",
                                JOptionPane.DEFAULT_OPTION,
                                JOptionPane.INFORMATION_MESSAGE,
                                null,
                                new String[] { "Edit Profile", "Logout" },
                                "Logout"
                        );

                        if (option == 0) {
                            editProfile();
                        } else if (option == 1) { 
                            logout();
                        }
                    } else {
                        JOptionPane.showMessageDialog(expenseFrame, "User not found", "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to fetch profile information: " + e.getMessage());
        }
    }

    private void editProfile() {
        JTextField emailField = new JTextField(20);
        JTextField phoneField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);

        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("New Email:"));
        panel.add(emailField);
        panel.add(new JLabel("New Phone:"));
        panel.add(phoneField);
        panel.add(new JLabel("New Password:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(
                loginFrame,
                panel,
                "Edit Profile",
                JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String newEmail = emailField.getText();
            String newPhone = phoneField.getText();
            String newPassword = new String(passwordField.getPassword());

            try {
                String updateProfileQuery = "UPDATE users SET email = ?, phone = ?, password = ? WHERE username = ?";
                try (PreparedStatement updateProfileStatement = connection.prepareStatement(updateProfileQuery)) {
                    updateProfileStatement.setString(1, newEmail);
                    updateProfileStatement.setString(2, newPhone);
                    updateProfileStatement.setString(3, newPassword);
                    updateProfileStatement.setString(4, loggedInUser);
                    int rowsAffected = updateProfileStatement.executeUpdate();
                    if (rowsAffected > 0) {
                        JOptionPane.showMessageDialog(
                                loginFrame,
                                "Profile updated successfully.",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(
                                loginFrame,
                                "Failed to update profile. Please try again.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (SQLException e) {
                showErrorDialog("Failed to update profile: " + e.getMessage());
            }
        }
    }

    private void logout() {
        loggedInUser = null;
        initializeLoginUI();
    }

    private Connection getConnection() throws SQLException {
        String url = "jdbc:oracle:thin:@localhost:1521:xe";
        String user = "system";
        String password = "tiger";
        return DriverManager.getConnection(url, user, password);
    }

    private void connectToDatabase() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            connection = getConnection();
        } catch (ClassNotFoundException e) {
            showErrorDialog("Oracle JDBC driver not found.");
            System.exit(1);
        } catch (SQLException e) {
            showErrorDialog("Failed to connect to the database: " + e.getMessage());
            System.exit(1);
        }
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(expenseFrame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void addExpense() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            java.util.Date utilDate = dateFormat.parse(dateField.getText());
            java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
            String categoryName = categoryComboBox.getSelectedItem().toString();
            String selectedCurrency = currencyComboBox.getSelectedItem().toString();
            double amount = Double.parseDouble(amountField.getText());

            if (!selectedCurrency.equals("INR")) {
                switch (selectedCurrency) {
                    case "USD":
                        amount *= 83.38;
                        break;
                    case "EUR":
                        amount *= 90.06;
                        break;
                    case "GBP":
                        amount *= 105.26;
                        break;
                }
            }

            String sql = "INSERT INTO expense (EXPENSE_ID, EXPENSE_DATE, DESCRIPTION, AMOUNT, CURRENCY, CATEGORY_NAME) VALUES (SEQ_EXPENSE.NEXTVAL, ?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setDate(1, sqlDate);
                preparedStatement.setString(2, descField.getText());
                preparedStatement.setDouble(3, amount); 
                preparedStatement.setString(4, "INR");
                preparedStatement.setString(5, categoryName);

                int rowsAffected = preparedStatement.executeUpdate();
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(expenseFrame, "Expense added successfully", "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    updateTable();
                } else {
                    JOptionPane.showMessageDialog(expenseFrame, "Failed to add expense", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException | SQLException | ParseException ex) {
            showErrorDialog("Failed to add expense: " + ex.getMessage());
        }
    }

    private void editExpense() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            String date = table.getValueAt(selectedRow, 0).toString();
            String description = table.getValueAt(selectedRow, 1).toString();
            String currency = table.getValueAt(selectedRow, 2).toString();
            double amount = Double.parseDouble(table.getValueAt(selectedRow, 3).toString()); 
            String category = table.getValueAt(selectedRow, 4).toString(); 

            JTextField newDateField = new JTextField(date);
            JTextField newDescField = new JTextField(description);
            JComboBox<String> newCurrencyComboBox = new JComboBox<>(new String[] { "INR", "USD", "EUR", "GBP" });
            newCurrencyComboBox.setSelectedItem(currency); 

            JTextField newAmountField = new JTextField(String.valueOf(amount));
            JComboBox<String> newCategoryComboBox = new JComboBox<>(
                    new String[] { "Select Category", "Groceries", "Utilities", "Transportation", "Entertainment" });
            newCategoryComboBox.setSelectedItem(category);

            Object[] message = {
                    "Date (dd/MM/yyyy):", newDateField,
                    "Description:", newDescField,
                    "Currency:", newCurrencyComboBox, 
                    "Amount:", newAmountField,
                    "Category:", newCategoryComboBox
            };

            int option = JOptionPane.showConfirmDialog(expenseFrame, message, "Edit Expense",
                    JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                    String newDateText = newDateField.getText().trim();
                    java.util.Date utilDate = dateFormat.parse(newDateText);
                    java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());

                    String newCurrency = newCurrencyComboBox.getSelectedItem().toString(); 
                    String newCategory = newCategoryComboBox.getSelectedItem().toString();

                    String sql = "UPDATE expense SET EXPENSE_DATE = ?, DESCRIPTION = ?, CURRENCY = ?, AMOUNT = ?, CATEGORY_NAME = ? WHERE EXPENSE_DATE = ? AND DESCRIPTION = ? AND CURRENCY = ? AND AMOUNT = ? AND CATEGORY_NAME = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                        preparedStatement.setDate(1, sqlDate);
                        preparedStatement.setString(2, newDescField.getText());
                        preparedStatement.setString(3, newCurrency); 
                        preparedStatement.setDouble(4, Double.parseDouble(newAmountField.getText()));
                        preparedStatement.setString(5, newCategory);
                        preparedStatement.setDate(6, sqlDate);
                        preparedStatement.setString(7, description);
                        preparedStatement.setString(8, currency);
                        preparedStatement.setDouble(9, amount);
                        preparedStatement.setString(10, category);

                        int rowsAffected = preparedStatement.executeUpdate();
                        if (rowsAffected > 0) {
                            JOptionPane.showMessageDialog(expenseFrame, "Expense updated successfully", "Success",
                                    JOptionPane.INFORMATION_MESSAGE);
                            updateTable();
                        } else {
                            JOptionPane.showMessageDialog(expenseFrame, "Failed to update expense", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } catch (NumberFormatException | SQLException | ParseException ex) {
                    showErrorDialog("Failed to update expense: " + ex.getMessage());
                }
            }
        } else {
            JOptionPane.showMessageDialog(expenseFrame, "Please select an expense to edit", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

    private void deleteExpense() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                        java.util.Date utilDate = dateFormat.parse(table.getValueAt(selectedRow, 0).toString());
                        java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
                        String category = table.getValueAt(selectedRow, 4).toString(); 

                        int confirm = JOptionPane.showConfirmDialog(expenseFrame,
                                "Are you sure you want to delete this expense?", "Confirm Delete",
                                JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            String sql = "DELETE FROM expense WHERE EXPENSE_DATE = ? AND CATEGORY_NAME = ?";
                            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                                preparedStatement.setDate(1, sqlDate);
                                preparedStatement.setString(2, category);

                                int rowsAffected = preparedStatement.executeUpdate();
                                if (rowsAffected > 0) {
                                    return null;
                                } else {
                                    throw new SQLException("Failed to delete expense");
                                }
                            }
                        }
                    } catch (ParseException | SQLException ex) {
                        showErrorDialog("Failed to delete expense: " + ex.getMessage());
                    }
                    return null;
                }

                @Override
                protected void done() {
                    updateTable(); 
                }
            };

            worker.execute(); 
        } else {
            JOptionPane.showMessageDialog(expenseFrame, "Please select an expense to delete", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTable() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT TO_CHAR(EXPENSE_DATE, 'DD/MM/YYYY'), DESCRIPTION, CURRENCY, AMOUNT, CATEGORY_NAME FROM expense")) {

            while (resultSet.next()) {
                String date = resultSet.getString(1);
                String description = resultSet.getString(2); 
                String currency = resultSet.getString(3);
                double amount = resultSet.getDouble(4); 
                String category = resultSet.getString(5); 

                Object[] rowData = { date, description, currency, amount, category };
                model.addRow(rowData);
            }

            double totalAmount = 0;
            for (int i = 0; i < model.getRowCount(); i++) {
                totalAmount += (double) model.getValueAt(i, 3); 
            }
            lblTotalAmount.setText(String.format("%.2f", totalAmount));

            TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);

            for (int row = 0; row < model.getRowCount(); row++) {
                for (int col = 0; col < model.getColumnCount(); col++) {
                    table.getColumnModel().getColumn(col).setCellRenderer(new CustomRenderer());
                }
            }
            table.getTableHeader().setDefaultRenderer(new CustomHeaderRenderer());

        } catch (SQLException e) {
            showErrorDialog("Failed to update table: " + e.getMessage());
        }
    }

    class CustomRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component cellComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
            cellComponent.setBackground(new Color(255,255, 255));
            return cellComponent;
        }
    }

    class CustomHeaderRenderer implements TableCellRenderer {
        JLabel label;

        public CustomHeaderRenderer() {
            label = new JLabel();
            label.setOpaque(true);
            label.setForeground(Color.WHITE);
            label.setBackground(new Color(0, 46, 44));
            label.setHorizontalAlignment(SwingConstants.CENTER);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            label.setText(value.toString());
            return label;
        }
    }

    private void generateSelectedChart() {
        String selectedChart = (String) chartComboBox.getSelectedItem();
        switch (selectedChart) {
            case "Daily Expenses":
                generateDailyExpenseChart();
                break;
            case "Monthly Expenses":
                generateMonthlyExpenseChart();
                break;
            case "Category Expenses":
                generatePieChart();
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void generatePieChart() {
        @SuppressWarnings("rawtypes")
        DefaultPieDataset dataset = new DefaultPieDataset();

        List<Expense> allExpenses = getAllExpenses();

        if (!allExpenses.isEmpty()) {
            Map<String, Double> categoryExpenses = new HashMap<>();
            for (Expense expense : allExpenses) {
                String category = expense.getCategory();
                double amount = expense.getAmount();
                categoryExpenses.put(category, categoryExpenses.getOrDefault(category, 0.0) + amount);
            }

            for (Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
                dataset.setValue(entry.getKey(), entry.getValue());
            }

            JFreeChart chart = ChartFactory.createPieChart3D("Expense Distribution by Category", dataset, true, true,
                    false);
            PiePlot3D plot = (PiePlot3D) chart.getPlot();
            plot.setStartAngle(290); 
            plot.setForegroundAlpha(0.7f); 
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(500, 400));

            JFrame chartFrame = new JFrame("Expense Distribution Pie Chart");
            chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            chartFrame.getContentPane().add(chartPanel);
            chartFrame.pack();
            chartFrame.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(expenseFrame, "No expenses available.", "Information",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void generateDailyExpenseChart() {
        List<Expense> allExpenses = getAllExpenses();

        Map<LocalDate, Double> dailyExpenses = calculateDailyExpenses(allExpenses);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<LocalDate, Double> entry : dailyExpenses.entrySet()) {
            dataset.addValue(entry.getValue(), "Expense", entry.getKey().toString());
        }

        JFreeChart lineChart = ChartFactory.createLineChart(
                "Daily Expenses",
                "Date",
                "Expense Amount",
                dataset);

        JFrame chartFrame = new JFrame("Daily Expenses Chart");
        chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        chartFrame.getContentPane().add(new ChartPanel(lineChart));
        chartFrame.pack();
        chartFrame.setVisible(true);
    }

    private Map<LocalDate, Double> calculateDailyExpenses(List<Expense> allExpenses) {
        Map<LocalDate, Double> dailyExpenses = new HashMap<>();

        for (Expense expense : allExpenses) {
            LocalDate date = LocalDate.parse(expense.getDate().split(" ")[0],
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            double amount = expense.getAmount();

            dailyExpenses.put(date, dailyExpenses.getOrDefault(date, 0.0) + amount);
        }

        return dailyExpenses;
    }

    @SuppressWarnings("unused")
    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private void generateMonthlyExpenseChart() {
        List<Expense> allExpenses = getAllExpenses();

        Map<YearMonth, Double> monthlyExpenses = calculateMonthlyExpenses(allExpenses);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<YearMonth, Double> entry : monthlyExpenses.entrySet()) {
            dataset.addValue(entry.getValue(), "Expense", entry.getKey().toString());
        }

        JFreeChart lineChart = ChartFactory.createLineChart(
                "Monthly Expenses",
                "Month",
                "Expense Amount",
                dataset);


        JFrame chartFrame = new JFrame("Monthly Expenses Chart");
        chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        chartFrame.getContentPane().add(new ChartPanel(lineChart));
        chartFrame.pack();
        chartFrame.setVisible(true);
    }

    private Map<YearMonth, Double> calculateMonthlyExpenses(List<Expense> allExpenses) {
        Map<YearMonth, Double> monthlyExpenses = new HashMap<>();

        for (Expense expense : allExpenses) {
            LocalDate expenseDate = LocalDate.parse(expense.getDate().split(" ")[0],
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            YearMonth yearMonth = YearMonth.from(expenseDate);
            double amount = expense.getAmount();
            monthlyExpenses.put(yearMonth, monthlyExpenses.getOrDefault(yearMonth, 0.0) + amount);
        }
        return monthlyExpenses;
    }

    private List<Expense> getAllExpenses() {
        List<Expense> allExpenses = new ArrayList<>();

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM expense");

            while (resultSet.next()) {
                String date = resultSet.getString("EXPENSE_DATE");
                String description = resultSet.getString("DESCRIPTION");
                double amount = resultSet.getDouble("AMOUNT");
                String category = resultSet.getString("CATEGORY_NAME");

                allExpenses.add(new Expense(date, description, amount, category));
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to retrieve expenses: " + e.getMessage());
        }

        return allExpenses;
    }

    private void printExpenseRecords() {
        try {
            PrinterJob printerJob = PrinterJob.getPrinterJob();
            printerJob.setPrintable(table.getPrintable(
                    JTable.PrintMode.FIT_WIDTH, null, null));
            if (printerJob.printDialog()) {
                printerJob.print();
            }
        } catch (PrinterException ex) {
            showErrorDialog("Error while printing: " + ex.getMessage());
        }
    }

    public class Expense {
        private String date;
        private String description;
        private double amount;
        private String category;

        public Expense(String date, String description, double amount, String category) {
            this.date = date;
            this.description = description;
            this.amount = amount;
            this.category = category;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }

    private void applyButtonStyle(JButton button) {
        button.setBackground(new Color(247,147,70));
        button.setForeground(Color.BLACK);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false); 
        button.setOpaque(true); 
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); 
        button.setBorder(new RoundBorder(10, Color.BLACK)); 
    }

    class RoundBorder implements Border {
        private int radius;
        private Color color;

        RoundBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(this.radius, this.radius, this.radius, this.radius);
        }

        public boolean isBorderOpaque() {
            return true;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(color);
            g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }
    }
}
