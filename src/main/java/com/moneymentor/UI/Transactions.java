package com.moneymentor.UI;

import com.moneymentor.Database.DatabaseConnection;
import com.moneymentor.Models.Transaction;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import java.time.format.DateTimeFormatter;


public class Transactions extends javax.swing.JDialog {        
        private final int userId;
        private boolean expenseAdded = false;
        private DatabaseConnection dbConnection;
        private int selectedTransactionId = -1;
        private String selectedTransactionType;
        
    public Transactions(java.awt.Frame parent, boolean modal, int userId) {
        super(parent, modal);
        this.userId = userId;
        this.dbConnection = DatabaseConnection.getInstance();
        initComponents();
        customizeComponents();
        
        setPreferredSize(new java.awt.Dimension(800, 400));
        setSize(new java.awt.Dimension(800, 500));
        setResizable(false);
    
        // Center the dialog on the screen
        setLocationRelativeTo(parent);
    }
    
     private void customizeComponents() {
        // Set up category combo box
        loadCategories();
        loadBudgets(); 
        updateTransactionsTable();
        // Set current date
        dateChooser.setDate(new java.util.Date());
        
        // Add button listeners
        btnUpdate.addActionListener(evt -> UpdateExpense());
        btnDelete.addActionListener(evt -> DeleteExpense());
        btnCancel.addActionListener(evt -> dispose());
    }    
   
 private void loadCategories() {
        List<String> categories = dbConnection.getCategoriesByType("Expense");
        
        if (categories.isEmpty()) {
             JOptionPane.showMessageDialog(this, "No expense categories found in database");
            // Add a default category to prevent errors
            categories.add("Other");
        }
        
        DefaultComboBoxModel<String> categoryModel = new DefaultComboBoxModel<>(
            categories.toArray(new String[0])
        );
        cmbCategory.setModel(categoryModel);
    }  
 
    private void loadBudgets() {
    List<String> budgets = dbConnection.getBudgetsByUser(userId);
    if (budgets.isEmpty()) {
        JOptionPane.showMessageDialog(this, "No budgets found");
        budgets.add("Default");
    }
    DefaultComboBoxModel<String> budgetModel = new DefaultComboBoxModel<>(
        budgets.toArray(new String[0])
    );
    cmbBudget.setModel(budgetModel);
    }
    
    private void updateTransactionsTable() {
 try {
        // Create the table model with column names
        String[] columnNames = {"ID", "Date", "Type", "Category", "Budget", "Description", "Amount"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Get all transactions for the user
        List<Transaction> transactions = dbConnection.getRecentTransactions(userId, 100);
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
        
        for (Transaction transaction : transactions) {
            String categoryName = dbConnection.getCategoryName(transaction.getCategoryId());
            // Fix the budget name retrieval
            String budgetName = getBudgetDisplayName(transaction.getBudgetId());
            String amountStr = transaction.getTransactionType().equals("Expense") ? 
                              "-$" + transaction.getAmount().toString() :
                              "+$" + transaction.getAmount().toString();
            
            model.addRow(new Object[]{
                transaction.getTransactionId(),
                transaction.getTransactionDate().format(dateFormatter),
                transaction.getTransactionType(),
                categoryName,
                budgetName,  // Now displays the correct budget name
                transaction.getDescription(),
                amountStr
            });
        }
        
        tblRecentTransactions.setModel(model);
        
        // Hide the ID column
        tblRecentTransactions.getColumnModel().getColumn(0).setMinWidth(0);
        tblRecentTransactions.getColumnModel().getColumn(0).setMaxWidth(0);
        tblRecentTransactions.getColumnModel().getColumn(0).setWidth(0);
        
        // Add selection listener
        tblRecentTransactions.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = tblRecentTransactions.getSelectedRow();
                if (selectedRow >= 0) {
                    selectedTransactionId = (int) tblRecentTransactions.getValueAt(selectedRow, 0);
                    selectedTransactionType = (String) tblRecentTransactions.getValueAt(selectedRow, 2);
                    
                    // Fill the form with selected transaction data
                    txtAmount.setText(tblRecentTransactions.getValueAt(selectedRow, 6).toString()
                            .replace("$", "").replace("+", "").replace("-", ""));
                    txtDescription.setText((String) tblRecentTransactions.getValueAt(selectedRow, 5));
                    cmbCategory.setSelectedItem(tblRecentTransactions.getValueAt(selectedRow, 3));
                    cmbBudget.setSelectedItem(tblRecentTransactions.getValueAt(selectedRow, 4));
                    
                    try {
                        Date transactionDate = Date.valueOf(tblRecentTransactions.getValueAt(selectedRow, 1)
                                .toString().split(" ")[0]);
                        dateChooser.setDate(transactionDate);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this,
            "Error updating transactions table: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }
}
    private String getBudgetDisplayName(int budgetId) {
    try {
        // Query to get budget details based on budgetId
        String query = "SELECT c.CategoryName, b.MonthYear " +
                      "FROM Budgets b " +
                      "JOIN Categories c ON b.CategoryID = c.CategoryID " +
                      "WHERE b.BudgetID = ?";
        
        Connection conn = dbConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setInt(1, budgetId);
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            String categoryName = rs.getString("CategoryName");
            Timestamp monthYear = rs.getTimestamp("MonthYear");
            LocalDateTime dateTime = monthYear.toLocalDateTime();
            String monthYearStr = dateTime.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            return categoryName + " - " + monthYearStr;
        }
        return "N/A";
    } catch (SQLException e) {
        e.printStackTrace();
        return "N/A";
    }
}
    
    private void UpdateExpense() {
    if (selectedTransactionId == -1) {
        JOptionPane.showMessageDialog(this, "Please select a transaction to update");
        return;
    }
    
    try {
        BigDecimal amount = new BigDecimal(txtAmount.getText().trim());
        String categoryName = (String) cmbCategory.getSelectedItem();
        String budgetName = (String) cmbBudget.getSelectedItem();
        String description = txtDescription.getText().trim();
        Date date = new Date(dateChooser.getDate().getTime());
        
        int categoryId = dbConnection.getCategoryIdByName(categoryName);
        int budgetId = dbConnection.getBudgetIdFromDisplay(budgetName, userId);
        
        if (dbConnection.updateTransaction(selectedTransactionId, userId, amount, 
                                         categoryId, budgetId, description, date)) {
            JOptionPane.showMessageDialog(this, "Transaction updated successfully");
            updateTransactionsTable();
            clearForm();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update transaction");
        }
        
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this,
            "Error updating transaction: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }
}

    // Implement the Delete action
    private void DeleteExpense() {
        if (selectedTransactionId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a transaction to delete");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this transaction?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (dbConnection.deleteTransaction(selectedTransactionId, userId)) {
                JOptionPane.showMessageDialog(this, "Transaction deleted successfully");
                updateTransactionsTable();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete transaction");
            }
        }
    } 

    // Add helper method to clear the form
    private void clearForm() {
        selectedTransactionId = -1;
        selectedTransactionType = null;
        txtAmount.setText("");
        txtDescription.setText("");
        dateChooser.setDate(new java.util.Date());
        cmbCategory.setSelectedIndex(0);
        cmbBudget.setSelectedIndex(0);
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        gradient2 = new com.moneymentor.Models.Gradient();
        btnUpdate = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        gradient1 = new com.moneymentor.Models.Gradient();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        cmbBudget = new javax.swing.JComboBox<>();
        txtDescription = new javax.swing.JTextField();
        txtAmount = new javax.swing.JTextField();
        dateChooser = new com.toedter.calendar.JDateChooser();
        cmbCategory = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblRecentTransactions = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setUndecorated(true);
        setResizable(false);
        setSize(new java.awt.Dimension(700, 300));

        jPanel1.setLayout(new java.awt.BorderLayout());

        gradient2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        btnUpdate.setBackground(new java.awt.Color(102, 204, 0));
        btnUpdate.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnUpdate.setForeground(new java.awt.Color(255, 255, 255));
        btnUpdate.setText("Update");
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });
        gradient2.add(btnUpdate);

        btnDelete.setBackground(new java.awt.Color(255, 0, 1));
        btnDelete.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnDelete.setForeground(new java.awt.Color(255, 255, 255));
        btnDelete.setText("Delete");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });
        gradient2.add(btnDelete);

        btnCancel.setBackground(new java.awt.Color(88, 166, 255));
        btnCancel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnCancel.setForeground(new java.awt.Color(255, 255, 255));
        btnCancel.setText("Cancel");
        gradient2.add(btnCancel);

        jPanel1.add(gradient2, java.awt.BorderLayout.NORTH);

        getContentPane().add(jPanel1, java.awt.BorderLayout.PAGE_END);

        jLabel5.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("TRANSACTIONS");

        jLabel7.setFont(new java.awt.Font("Segoe UI Black", 1, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("Welcome to the transaction, feel free to manage your transactions");

        javax.swing.GroupLayout gradient1Layout = new javax.swing.GroupLayout(gradient1);
        gradient1.setLayout(gradient1Layout);
        gradient1Layout.setHorizontalGroup(
            gradient1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(gradient1Layout.createSequentialGroup()
                .addGroup(gradient1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(gradient1Layout.createSequentialGroup()
                        .addGap(329, 329, 329)
                        .addComponent(jLabel5))
                    .addGroup(gradient1Layout.createSequentialGroup()
                        .addGap(177, 177, 177)
                        .addComponent(jLabel7)))
                .addContainerGap(523, Short.MAX_VALUE))
        );
        gradient1Layout.setVerticalGroup(
            gradient1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(gradient1Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel7)
                .addGap(31, 31, 31))
        );

        getContentPane().add(gradient1, java.awt.BorderLayout.PAGE_START);

        jPanel2.setLayout(new java.awt.GridLayout(2, 1));

        jPanel3.setBackground(new java.awt.Color(51, 51, 51));
        jPanel3.setPreferredSize(new java.awt.Dimension(789, 300));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Amount:");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Description:");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("Budget:");

        cmbBudget.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        cmbCategory.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Category:");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Date:");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(132, 132, 132)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel6)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(txtDescription, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(32, 32, 32)
                        .addComponent(jLabel4)
                        .addGap(30, 30, 30)
                        .addComponent(dateChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(txtAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20)
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(cmbCategory, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(cmbBudget, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(47, 47, 47)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel1))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(cmbCategory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(16, 16, 16)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtDescription, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dateChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(3, 3, 3)
                                .addComponent(jLabel4))))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jLabel3)))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(cmbBudget, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(280, Short.MAX_VALUE))
        );

        jPanel2.add(jPanel3);

        jPanel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(51, 51, 51)));
        jPanel4.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2));
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        tblRecentTransactions.setBackground(new java.awt.Color(51, 51, 51));
        tblRecentTransactions.setForeground(new java.awt.Color(255, 255, 255));
        tblRecentTransactions.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(tblRecentTransactions);

        jPanel4.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel2.add(jPanel4);

        getContentPane().add(jPanel2, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed

    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnDeleteActionPerformed

    public static void main(String args[]) {
       
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Transactions dialog = new Transactions(new javax.swing.JFrame(), true, 1);
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
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JComboBox<String> cmbBudget;
    private javax.swing.JComboBox<String> cmbCategory;
    private com.toedter.calendar.JDateChooser dateChooser;
    private com.moneymentor.Models.Gradient gradient1;
    private com.moneymentor.Models.Gradient gradient2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblRecentTransactions;
    private javax.swing.JTextField txtAmount;
    private javax.swing.JTextField txtDescription;
    // End of variables declaration//GEN-END:variables
}
