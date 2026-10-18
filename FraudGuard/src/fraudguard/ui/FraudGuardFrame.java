package fraudguard.ui;

import fraudguard.db.DBConnection;
import fraudguard.service.TransactionService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FraudGuardFrame extends JFrame {
    private final TransactionService service = new TransactionService();

    private final JTextField senderField = new JTextField("1001", 8);
    private final JTextField receiverField = new JTextField("1002", 8);
    private final JTextField amountField = new JTextField("1000", 10);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Sender", "Receiver", "Amount", "Date",
                    "Score", "Risk", "Status", "Reason"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(tableModel);
    private final JLabel summaryLabel = new JLabel("Loading summary...");

    public FraudGuardFrame() {
        setTitle("FraudGuard | Smart Transaction Fraud Detection");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 680);
        setMinimumSize(new Dimension(950, 560));
        setLocationRelativeTo(null);

        buildInterface();
        refreshData();
    }

    private void buildInterface() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        root.setBackground(new Color(241, 245, 249));
        setContentPane(root);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("FRAUDGUARD");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        JLabel subtitle = new JLabel(
                "Simulated transaction monitoring • Explainable risk rules");
        subtitle.setForeground(new Color(203, 213, 225));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 4));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(subtitle);

        header.add(heading, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        center.add(buildTransactionForm(), BorderLayout.NORTH);

        table.setRowHeight(25);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                "Transaction history and review queue"));

        center.add(scrollPane, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(8, 8));
        footer.setOpaque(false);

        summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        footer.add(summaryLabel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);

        JButton approveButton = new JButton("Approve selected");
        JButton rejectButton = new JButton("Reject selected");
        JButton refreshButton = new JButton("Refresh");

        approveButton.addActionListener(event -> reviewSelected(true));
        rejectButton.addActionListener(event -> reviewSelected(false));
        refreshButton.addActionListener(event -> refreshData());

        actions.add(approveButton);
        actions.add(rejectButton);
        actions.add(refreshButton);

        footer.add(actions, BorderLayout.SOUTH);
        root.add(footer, BorderLayout.SOUTH);
    }

    private JPanel buildTransactionForm() {
        JPanel panel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 12));

        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                "Create simulated transfer"));

        panel.add(new JLabel("Sender account:"));
        panel.add(senderField);

        panel.add(new JLabel("Receiver account:"));
        panel.add(receiverField);

        panel.add(new JLabel("Amount (₹):"));
        panel.add(amountField);

        JButton createButton = new JButton("Analyze & submit");
        createButton.addActionListener(event -> createTransaction());
        panel.add(createButton);

        JLabel hint = new JLabel("Demo accounts: 1001, 1002, 1003");
        hint.setForeground(new Color(71, 85, 105));
        panel.add(hint);

        return panel;
    }

    private void createTransaction() {
        try {
            int sender = Integer.parseInt(senderField.getText().trim());
            int receiver = Integer.parseInt(receiverField.getText().trim());
            BigDecimal amount = new BigDecimal(amountField.getText().trim());

            long id = service.createTransaction(sender, receiver, amount);

            JOptionPane.showMessageDialog(
                    this,
                    "Transaction #" + id + " saved.\n" +
                            "Refresh the table to see its risk score and status.",
                    "Transaction analyzed",
                    JOptionPane.INFORMATION_MESSAGE);

            refreshData();

        } catch (NumberFormatException exception) {
            showError("Enter valid numeric account IDs and amount.");

        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());

        } catch (SQLException exception) {
            showError("Database error: " + exception.getMessage()
                    + "\nCheck MySQL is running and DBConnection.java has the correct password.");
        }
    }

    private void reviewSelected(boolean approve) {
        int selectedRow = table.getSelectedRow();

        if (selectedRow < 0) {
            showError("Select a transaction row first.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        long id = ((Number) tableModel.getValueAt(modelRow, 0)).longValue();
        String status = String.valueOf(tableModel.getValueAt(modelRow, 7));

        if (!"REVIEW".equals(status)) {
            showError(
                    "Only transactions with REVIEW status can be approved or rejected.");
            return;
        }

        int answer = JOptionPane.showConfirmDialog(
                this,
                (approve ? "Approve" : "Reject") + " transaction #" + id + "?",
                "Confirm review",
                JOptionPane.YES_NO_OPTION);

        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            service.reviewTransaction(id, approve);
            refreshData();

            JOptionPane.showMessageDialog(
                    this, "Review decision saved.");

        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());

        } catch (SQLException exception) {
            showError("Database error: " + exception.getMessage());
        }
    }

    private void refreshData() {
        tableModel.setRowCount(0);

        int total = 0;
        int flagged = 0;
        int high = 0;

        String sql = "SELECT transaction_id, sender_account_id, " +
                "receiver_account_id, amount, created_at, risk_score, " +
                "risk_level, status, reason FROM transactions " +
                "ORDER BY transaction_id DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                total++;

                String status = result.getString("status");
                String risk = result.getString("risk_level");

                if ("REVIEW".equals(status)) {
                    flagged++;
                }

                if ("HIGH".equals(risk)) {
                    high++;
                }

                tableModel.addRow(new Object[]{
                        result.getLong("transaction_id"),
                        result.getInt("sender_account_id"),
                        result.getInt("receiver_account_id"),
                        "₹" + result.getBigDecimal("amount").toPlainString(),
                        result.getTimestamp("created_at"),
                        result.getInt("risk_score"),
                        risk,
                        status,
                        result.getString("reason")
                });
            }

            summaryLabel.setText(
                    "Transactions: " + total +
                            "     Awaiting review: " + flagged +
                            "     High-risk alerts: " + high);

        } catch (SQLException exception) {
            summaryLabel.setText(
                    "Could not load database. Check MySQL and DBConnection.java.");

            JOptionPane.showMessageDialog(
                    this,
                    "Could not connect to MySQL.\n" + exception.getMessage()
                            + "\nRun database.sql and update the password in DBConnection.java.",
                    "Database connection error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "FraudGuard", JOptionPane.WARNING_MESSAGE);
    }
}