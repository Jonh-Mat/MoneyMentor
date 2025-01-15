package com.moneymentor.UI;

import com.moneymentor.Database.DatabaseConnection;
import com.moneymentor.Models.Budget;
import com.moneymentor.Models.Category;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

public class BudgetDialog extends javax.swing.JDialog {
    private final int userId;
    private DatabaseConnection dbConnection;
    private DefaultTableModel budgetTableModel;
    private Map<Integer, Budget> currentBudgets = new HashMap<>();
    private boolean budgetsModified = false;

    public BudgetDialog(java.awt.Frame parent, boolean modal, int userId) {
        super(parent, modal);
        this.userId = userId;
        this.dbConnection = DatabaseConnection.getInstance();
        initComponents();
        setupComponents();
        loadBudgetData();
        
        setPreferredSize(new java.awt.Dimension(800, 400));
        setSize(new java.awt.Dimension(800, 400));
        setResizable(false);
    
        // Center the dialog on the screen
        setLocationRelativeTo(parent);
        
    }
        
      private void setupComponents() {
        // Setup month combo box
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        cmbMonth.setModel(new DefaultComboBoxModel<>(months));

        // Setup year combo box
        int currentYear = LocalDateTime.now().getYear();
        String[] years = new String[5];
        for (int i = 0; i < 5; i++) {
            years[i] = String.valueOf(currentYear + i);
        }
        cmbYear.setModel(new DefaultComboBoxModel<>(years));

        // Set current month and year
        cmbMonth.setSelectedIndex(LocalDateTime.now().getMonthValue() - 1);
        cmbYear.setSelectedItem(String.valueOf(currentYear));

        // Setup table model
        String[] columnNames = {"Category", "Budget Amount", "Spent Amount", "Remaining"};
        budgetTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Only budget amount is editable
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column > 0 ? BigDecimal.class : String.class;
            }
        };
        tblBudgets.setModel(budgetTableModel);
            // Add table model listener to detect changes
        budgetTableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 1) {
                budgetsModified = true;
                btnSave.setEnabled(true);
            }
        });        
        
        // Add event listeners
        btnAdd.addActionListener(e -> showAddBudgetDialog());
        btnSave.addActionListener(e -> saveBudgetChanges());
        btnClose.addActionListener(e -> dispose());
        cmbMonth.addActionListener(e -> loadBudgetData());
        cmbYear.addActionListener(e -> loadBudgetData());

        // Initially disable save button
        btnSave.setEnabled(false);
    }

     private void loadBudgetData() {
        budgetTableModel.setRowCount(0);
        currentBudgets.clear();

        int month = cmbMonth.getSelectedIndex() + 1;
        int year = Integer.parseInt((String)cmbYear.getSelectedItem());
        LocalDateTime monthYear = LocalDateTime.of(year, month, 1, 0, 0);

        try {
            List<Budget> budgets = dbConnection.getBudgets(userId, monthYear);

            for (Budget budget : budgets) {
                String categoryName = dbConnection.getCategoryName(budget.getCategoryId());
                BigDecimal spent = dbConnection.getSpentAmount(userId, budget.getCategoryId(), monthYear);
                BigDecimal remaining = budget.getAmount().subtract(spent);

                budgetTableModel.addRow(new Object[]{
                    categoryName,
                    budget.getAmount(),
                    spent,
                    remaining
                });

                currentBudgets.put(budget.getCategoryId(), budget);
            }

            budgetsModified = false;
            btnSave.setEnabled(false);

        } catch (Exception ex) {
            showError("Error loading budget data: " + ex.getMessage());
        }
    }

    private void showAddBudgetDialog() {
    int month = cmbMonth.getSelectedIndex() + 1;
    int year = Integer.parseInt((String)cmbYear.getSelectedItem());
    LocalDateTime monthYear = LocalDateTime.of(year, month, 1, 0, 0);

    List<Category> availableCategories = dbConnection.getUnbudgetedCategories(userId, monthYear);

    if (availableCategories.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "All expense categories already have budgets set.",
            "Information",
            JOptionPane.INFORMATION_MESSAGE);
        return;
    }

    Category selectedCategory = (Category) JOptionPane.showInputDialog(this,
        "Select category:",
        "Add Budget",
        JOptionPane.QUESTION_MESSAGE,
        null,
        availableCategories.toArray(),
        availableCategories.get(0));

    if (selectedCategory != null) {
        String amountStr = JOptionPane.showInputDialog(this,
            "Enter budget amount for " + selectedCategory.getCategoryName() + ":");

        if (amountStr != null && !amountStr.trim().isEmpty()) {
            try {
                BigDecimal amount = new BigDecimal(amountStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    showError("Budget amount must be greater than zero");
                    return;
                }

                // Create new budget object
                Budget newBudget = new Budget(userId, selectedCategory.getCategoryId(),
                    monthYear, amount);

                // Add to table and current budgets map
                budgetTableModel.addRow(new Object[]{
                    selectedCategory.getCategoryName(),
                    amount,
                    BigDecimal.ZERO, // Initial spent amount
                    amount  // Initial remaining amount (same as budget since spent is 0)
                });

                currentBudgets.put(selectedCategory.getCategoryId(), newBudget);
                
                // Mark as modified and enable save button
                budgetsModified = true;
                btnSave.setEnabled(true);

            } catch (NumberFormatException ex) {
                showError("Invalid amount entered");
            }
        }
    }
}

private void saveBudgetChanges() {
    boolean success = true;

    for (int row = 0; row < budgetTableModel.getRowCount(); row++) {
        String categoryName = (String) budgetTableModel.getValueAt(row, 0);
        BigDecimal newAmount = (BigDecimal) budgetTableModel.getValueAt(row, 1);
        int categoryId = dbConnection.getCategoryIdByName(categoryName);

        Budget currentBudget = currentBudgets.get(categoryId);
        if (currentBudget != null) {
            // Check if this is a new budget or modified budget
            boolean isNewBudget = currentBudget.getBudgetId() == 0; // Assuming 0 means new budget
            currentBudget.setAmount(newAmount);

            boolean updated;
            if (isNewBudget) {
                updated = dbConnection.addBudget(currentBudget);
            } else {
                updated = dbConnection.updateBudget(currentBudget);
            }

            if (!updated) {
                success = false;
            }
        }
    }

    if (success) {
        budgetsModified = false;
        btnSave.setEnabled(false);
        loadBudgetData(); // Reload to get fresh data
        JOptionPane.showMessageDialog(this,
            "Budgets saved successfully",
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
    } else {
        showError("Some budgets failed to update");
    }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }

    public boolean isBudgetsModified() {
        return budgetsModified;
    }

       
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        gradient2 = new com.moneymentor.Models.Gradient();
        btnAdd = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        gradient1 = new com.moneymentor.Models.Gradient();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblBudgets = new javax.swing.JTable();
        jSeparator1 = new javax.swing.JSeparator();
        jPanel1 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        cmbMonth = new javax.swing.JComboBox<>();
        cmbYear = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Budget Management");
        setModal(true);
        setUndecorated(true);
        setSize(new java.awt.Dimension(500, 400));

        gradient2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        btnAdd.setBackground(new java.awt.Color(88, 166, 255));
        btnAdd.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnAdd.setForeground(new java.awt.Color(255, 255, 255));
        btnAdd.setText("Add Budget");
        gradient2.add(btnAdd);

        btnSave.setBackground(new java.awt.Color(88, 166, 255));
        btnSave.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnSave.setForeground(new java.awt.Color(255, 255, 255));
        btnSave.setText("Save Changes");
        gradient2.add(btnSave);

        btnClose.setBackground(new java.awt.Color(88, 166, 255));
        btnClose.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnClose.setForeground(new java.awt.Color(255, 255, 255));
        btnClose.setText("Close");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });
        gradient2.add(btnClose);

        getContentPane().add(gradient2, java.awt.BorderLayout.SOUTH);

        jLabel5.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("BUDGET MANAGEMENT");

        jLabel7.setFont(new java.awt.Font("Segoe UI Black", 1, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("Welcome to the budget board, feel free to manage budget");

        javax.swing.GroupLayout gradient1Layout = new javax.swing.GroupLayout(gradient1);
        gradient1.setLayout(gradient1Layout);
        gradient1Layout.setHorizontalGroup(
            gradient1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(gradient1Layout.createSequentialGroup()
                .addGap(307, 307, 307)
                .addComponent(jLabel5))
            .addGroup(gradient1Layout.createSequentialGroup()
                .addGap(215, 215, 215)
                .addComponent(jLabel7))
        );
        gradient1Layout.setVerticalGroup(
            gradient1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(gradient1Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7)
                .addGap(16, 16, 16))
        );

        getContentPane().add(gradient1, java.awt.BorderLayout.NORTH);

        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel3.setLayout(new java.awt.BorderLayout());

        tblBudgets.setBackground(new java.awt.Color(51, 51, 51));
        tblBudgets.setForeground(new java.awt.Color(255, 255, 255));
        tblBudgets.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(tblBudgets);

        jPanel3.add(jScrollPane1, java.awt.BorderLayout.CENTER);
        jPanel3.add(jSeparator1, java.awt.BorderLayout.PAGE_START);

        jPanel5.add(jPanel3, java.awt.BorderLayout.CENTER);

        jPanel1.setBackground(new java.awt.Color(51, 51, 51));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255), 2));
        jPanel1.setForeground(new java.awt.Color(255, 255, 255));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Budget Planning");
        jPanel1.add(jLabel4);

        jPanel4.setBackground(new java.awt.Color(51, 51, 51));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Period:");
        jPanel4.add(jLabel3);

        cmbMonth.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jPanel4.add(cmbMonth);

        cmbYear.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jPanel4.add(cmbYear);

        jPanel1.add(jPanel4);

        jPanel5.add(jPanel1, java.awt.BorderLayout.PAGE_START);

        getContentPane().add(jPanel5, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnCloseActionPerformed

   
    public static void main(String args[]) {

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                BudgetDialog dialog = new BudgetDialog(new javax.swing.JFrame(), true, 1);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnSave;
    private javax.swing.JComboBox<String> cmbMonth;
    private javax.swing.JComboBox<String> cmbYear;
    private com.moneymentor.Models.Gradient gradient1;
    private com.moneymentor.Models.Gradient gradient2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable tblBudgets;
    // End of variables declaration//GEN-END:variables
}
