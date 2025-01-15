package com.moneymentor.UI;

import com.moneymentor.Database.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;


public class AddExpenDialogue extends javax.swing.JDialog {
        private final int userId;
        private boolean expenseAdded = false;
        private DatabaseConnection dbConnection;
        
    public AddExpenDialogue(java.awt.Frame parent, boolean modal, int userId) {
        super(parent, modal);
        this.userId = userId;
        this.dbConnection = DatabaseConnection.getInstance();
        initComponents();
        customizeComponents();
    }
    
        private void customizeComponents() {
        // Set up category combo box
         loadCategories();
         loadBudgets();  
        // Set current date
        dateChooser.setDate(new java.util.Date());
        
        // Add button listeners
        btnSave.addActionListener(evt -> saveExpense());
        btnCancel.addActionListener(evt -> dispose());
    }
    
    private void loadCategories() {
        List<String> categories = dbConnection.getCategoriesByType("Expense");
        
        if (categories.isEmpty()) {
            showError("No expense categories found in database");
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
        showError("No budgets found");
        budgets.add("Default");
    }
    DefaultComboBoxModel<String> budgetModel = new DefaultComboBoxModel<>(
        budgets.toArray(new String[0])
    );
    cmbBudget.setModel(budgetModel);
    }
    
    private void saveExpense() {
        try {
            // Validate amount
            String amountStr = txtAmount.getText().trim();
            if (amountStr.isEmpty()) {
                showError("Please enter an amount");
                txtAmount.requestFocus();
                return;
            }

            // Validate description
            String description = txtDescription.getText().trim();
            if (description.isEmpty()) {
                showError("Please enter a description");
                txtDescription.requestFocus();
                return;
            }

            // Get values
        BigDecimal amount = new BigDecimal(amountStr);
        String categoryName = (String) cmbCategory.getSelectedItem();
        String budgetDisplay = (String) cmbBudget.getSelectedItem();
        Date date = new Date(dateChooser.getDate().getTime());
        
        int categoryId = dbConnection.getCategoryIdByName(categoryName);
        int budgetId = dbConnection.getBudgetIdFromDisplay(budgetDisplay, userId);
        
        if (categoryId == -1) {
            showError("Selected category is invalid");
            return;
        }
        
        if (budgetId == -1) {
            showError("Selected budget is invalid");
            return;
        }
        
        boolean success = dbConnection.addExpense(userId, amount, categoryId, 
                                                budgetId, description, date);

        if (success) {
            expenseAdded = true;
            dispose();
        } else {
            showError("Failed to save expense");
        }
    } catch (Exception ex) {
        showError("Error: " + ex.getMessage());
    }
    }
     
    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }

    public boolean isExpenseAdded() {
        return expenseAdded;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        gradient1 = new com.moneymentor.Models.Gradient();
        btnSave = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        Main = new java.awt.Panel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtDescription = new javax.swing.JTextField();
        txtAmount = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        dateChooser = new com.toedter.calendar.JDateChooser();
        cmbCategory = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        cmbBudget = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Add Expense");
        setModal(true);
        setUndecorated(true);
        setPreferredSize(new java.awt.Dimension(897, 300));
        setResizable(false);
        setSize(new java.awt.Dimension(400, 300));

        gradient1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        btnSave.setBackground(new java.awt.Color(88, 166, 255));
        btnSave.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnSave.setForeground(new java.awt.Color(255, 255, 255));
        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        gradient1.add(btnSave);

        btnCancel.setBackground(new java.awt.Color(88, 166, 255));
        btnCancel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnCancel.setForeground(new java.awt.Color(255, 255, 255));
        btnCancel.setText("Cancel");
        gradient1.add(btnCancel);

        getContentPane().add(gradient1, java.awt.BorderLayout.SOUTH);

        Main.setBackground(new java.awt.Color(51, 51, 51));
        Main.setPreferredSize(new java.awt.Dimension(500, 100));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Amount:");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Description:");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Category:");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Date:");

        cmbCategory.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("Budget");

        cmbBudget.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout MainLayout = new javax.swing.GroupLayout(Main);
        Main.setLayout(MainLayout);
        MainLayout.setHorizontalGroup(
            MainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MainLayout.createSequentialGroup()
                .addGap(153, 153, 153)
                .addGroup(MainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel6)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(MainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(MainLayout.createSequentialGroup()
                        .addComponent(txtDescription, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(32, 32, 32)
                        .addComponent(jLabel4)
                        .addGap(30, 30, 30)
                        .addComponent(dateChooser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(MainLayout.createSequentialGroup()
                        .addComponent(txtAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20)
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(cmbCategory, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(cmbBudget, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(69, Short.MAX_VALUE))
        );
        MainLayout.setVerticalGroup(
            MainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MainLayout.createSequentialGroup()
                .addGap(63, 63, 63)
                .addGroup(MainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(MainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel1))
                    .addGroup(MainLayout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addGroup(MainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(cmbCategory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGroup(MainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(MainLayout.createSequentialGroup()
                        .addGap(16, 16, 16)
                        .addGroup(MainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtDescription, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dateChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(MainLayout.createSequentialGroup()
                                .addGap(3, 3, 3)
                                .addComponent(jLabel4))))
                    .addGroup(MainLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jLabel3)))
                .addGap(18, 18, 18)
                .addGroup(MainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(cmbBudget, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(112, 112, 112))
        );

        getContentPane().add(Main, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        
    }//GEN-LAST:event_btnSaveActionPerformed
    
    public static void main(String args[]) {
        
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                AddExpenDialogue dialog = new AddExpenDialogue(new javax.swing.JFrame(), true, 1);
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
    private java.awt.Panel Main;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnSave;
    private javax.swing.JComboBox<String> cmbBudget;
    private javax.swing.JComboBox<String> cmbCategory;
    private com.toedter.calendar.JDateChooser dateChooser;
    private com.moneymentor.Models.Gradient gradient1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JTextField txtAmount;
    private javax.swing.JTextField txtDescription;
    // End of variables declaration//GEN-END:variables
}
