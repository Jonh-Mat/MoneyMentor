package com.moneymentor.UI;

import com.moneymentor.Database.DatabaseConnection;
import com.moneymentor.Models.Category;
import com.moneymentor.Models.Transaction;
import com.moneymentor.Models.User;
import com.moneymentor.Util.GradientSidePanel;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.JOptionPane;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class DashboardFrame extends javax.swing.JFrame {
    private final User currentUser;
    private final DatabaseConnection dbConnection;
    private CardLayout cardLayout;
    private JPanel contentPanel;
   
    
    public DashboardFrame(User user) {
        this.currentUser = user;
        this.dbConnection = DatabaseConnection.getInstance();
        initComponents();
        setupDashboard();          
        initializeTransactionsTable();
        applySidebarGradient();
        updateRecentTransactions();
    }
    
    private void setupDashboard() {
        // Set welcome message
    lblWelcome.setText("Welcome, " + currentUser.getUsername());
    
    // Setup card layout
    cardLayout = new CardLayout();
    contentPanel = new JPanel(cardLayout);
    
    // Add main dashboard panel to content panel
    contentPanel.add(Main, "dashboard");
    
    // Replace Main panel with content panel in BorderLayout
    getContentPane().remove(Main);
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    
    // Initialize dashboard data
    updateDashboardData();
    }  
    
    private void initButtonEvent(){
        //Add income
        btnIncome.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            showAddIncomeDialog();
        }
    });
        
     // Add Expense Button
    btnExpenses.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            showAddExpenseDialog();
        }
    });

    // View Reports Button
    btnReports.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            showReportsPanel();
        }
    });

    // Manage Budget Button
    btnBudget.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            showBudgetPanel();
        }
    });
    
    btnVieawAll.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            showTransactionPanel();
        }
    });
    
}
    // Dialog methods
private void showAddIncomeDialog() {
    try {
        // Create and show the add income dialog
        AddIncomeDialog dialog = new AddIncomeDialog(this, true, currentUser.getUserId());
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        
        // After dialog closes, refresh dashboard if income was added
        if (dialog.isIncomeAdded()) {
           updateDashboardData();
        }
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this,
            "Error showing add income dialog: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }
}

private void showAddExpenseDialog() {
    AddExpenDialogue dialog = new AddExpenDialogue(this, true, currentUser.getUserId());
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);
    
    if (dialog.isExpenseAdded()) {
        updateDashboardData(); // Refresh your dashboard
    }
}

private void showBudgetPanel() {
    BudgetDialog dialog = new BudgetDialog(this, true, currentUser.getUserId());
    dialog.setVisible(true);
    
    if (dialog.isBudgetsModified()) {
        // Refresh dashboard if needed
        updateDashboardData();
    }
}

private void showReportsPanel() {
    Report reportDialog = new Report(this, true, currentUser.getUserId());
    reportDialog.setLocationRelativeTo(this); // Center the dialog relative to the dashboard
    reportDialog.setVisible(true);
    updateDashboardData();
}

private void showTransactionPanel(){
    Transactions transac = new Transactions(this, true, currentUser.getUserId());
    transac.setLocationRelativeTo(this);
    transac.setVisible(true);
    updateDashboardData();
}

private void updateDashboardData() {
    try {
        // Get current month and year
        LocalDateTime currentDate = LocalDateTime.now();
        int currentMonth = currentDate.getMonthValue();
        int currentYear = currentDate.getYear();

        // Update Income, Expenses, and Balance cards
        updateSummaryCards(currentMonth, currentYear);
        
        // Update Charts
        updateCharts(currentMonth, currentYear);
        
        // Update Recent Transactions table
        updateRecentTransactions();

    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this,
            "Error updating dashboard: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }
}

private void updateSummaryCards(int month, int year) {
    try {
        // Get monthly summary
        BigDecimal monthlyIncome = dbConnection.getMonthlyIncome(currentUser.getUserId(), month, year);
        BigDecimal monthlyExpenses = dbConnection.getMonthlyExpenses(currentUser.getUserId(), month, year);
        BigDecimal balance = monthlyIncome.subtract(monthlyExpenses);

        // Update labels with formatted values
        lblMonthlyIncome.setText(String.format("$%,.2f", monthlyIncome));
        lblMonthlyExpenses.setText(String.format("$%,.2f", monthlyExpenses));
        lblTotalBalance.setText(String.format("$%,.2f", balance));

        // Update colors based on values
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            lblTotalBalance.setForeground(new Color(192, 0, 0)); // Red for negative
        } else {
            lblTotalBalance.setForeground(new Color(0, 128, 0)); // Green for positive
        }

    } catch (Exception ex) {
        throw new RuntimeException("Error updating summary cards: " + ex.getMessage());
    }
}

private void updateCharts(int month, int year) {
    try {
        // Clear existing charts
        incomeVsExpensesChart.removeAll();
        expenseCategoriesChart.removeAll();

        // Create Income vs Expenses Bar Chart
        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
        BigDecimal income = dbConnection.getMonthlyIncome(currentUser.getUserId(), month, year);
        BigDecimal expenses = dbConnection.getMonthlyExpenses(currentUser.getUserId(), month, year);
        
        barDataset.addValue(income.doubleValue(), "Amount", "Income");
        barDataset.addValue(expenses.doubleValue(), "Amount", "Expenses");

        JFreeChart barChart = ChartFactory.createBarChart(
            "Income vs Expenses",
            "Type",
            "Amount ($)",
            barDataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        // Create Expense Categories Pie Chart
        DefaultPieDataset pieDataset = new DefaultPieDataset();
        List<Category> categories = dbConnection.getExpenseCategories();
        
        for (Category category : categories) {
            BigDecimal spent = dbConnection.getSpentAmount(currentUser.getUserId(), 
                                                         category.getCategoryId(), 
                                                         LocalDateTime.of(year, month, 1, 0, 0));
            if (spent.compareTo(BigDecimal.ZERO) > 0) {
                pieDataset.setValue(category.getCategoryName(), spent.doubleValue());
            }
        }

        JFreeChart pieChart = ChartFactory.createPieChart(
            "Expense Distribution",
            pieDataset,
            true,
            true,
            false
        );

        // Add charts to panels
        ChartPanel barChartPanel = new ChartPanel(barChart);
        ChartPanel pieChartPanel = new ChartPanel(pieChart);

        incomeVsExpensesChart.setLayout(new BorderLayout());
        expenseCategoriesChart.setLayout(new BorderLayout());

        incomeVsExpensesChart.add(barChartPanel, BorderLayout.CENTER);
        expenseCategoriesChart.add(pieChartPanel, BorderLayout.CENTER);

        // Refresh the panels
        incomeVsExpensesChart.revalidate();
        incomeVsExpensesChart.repaint();
        expenseCategoriesChart.revalidate();
        expenseCategoriesChart.repaint();

    } catch (Exception ex) {
        throw new RuntimeException("Error updating charts: " + ex.getMessage());
    }
}

private void initializeTransactionsTable() {
    // Set up the table model with column names
    String[] columnNames = {"Date", "Type", "Category", "Description", "Amount"};
    DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // Make table read-only
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 4) { // Amount column
                return BigDecimal.class;
            }
            return String.class;
        }
    };
    
    tblRecentTransactions.setModel(model);
    
    // Set column widths
    tblRecentTransactions.getColumnModel().getColumn(0).setPreferredWidth(100); // Date
    tblRecentTransactions.getColumnModel().getColumn(1).setPreferredWidth(80);  // Type
    tblRecentTransactions.getColumnModel().getColumn(2).setPreferredWidth(100); // Category
    tblRecentTransactions.getColumnModel().getColumn(3).setPreferredWidth(200); // Description
    tblRecentTransactions.getColumnModel().getColumn(4).setPreferredWidth(100); // Amount
}

private void updateRecentTransactions() {
try {
        // Get the table model        
        DefaultTableModel model = (DefaultTableModel) tblRecentTransactions.getModel();
        model.setRowCount(0); // Clear existing rows
        
        // Get recent transactions (last 5)
        List<Transaction> recentTransactions = dbConnection.getRecentTransactions(currentUser.getUserId(), 10);
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
        
        for (Transaction transaction : recentTransactions) {
            String categoryName = dbConnection.getCategoryName(transaction.getCategoryId());
            String amountStr = transaction.getTransactionType().equals("Expense") ? 
                              "-$" + transaction.getAmount().toString() :
                              "+$" + transaction.getAmount().toString();
            
            model.addRow(new Object[]{
                transaction.getTransactionDate().format(dateFormatter),
                transaction.getTransactionType(),
                categoryName,
                transaction.getDescription(),
                amountStr
            });
        }                                      
        
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this,
            "Error updating recent transactions: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }

}

 private void applySidebarGradient() {
        // Replace the existing sidebar panel with our gradient panel
        GradientSidePanel gradientSide = new GradientSidePanel();
        gradientSide.setLayout(new BoxLayout(gradientSide, BoxLayout.Y_AXIS));
               
        jPanel2.setOpaque(false);        
        jLabel1.setForeground(Color.WHITE);
        jLabel1.setFont(new Font("Segoe UI", Font.BOLD, 24));   
        
        jPanel3.setOpaque(false);        
        lblWelcome.setForeground(Color.WHITE);
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 24));                   
        
        // Create a container for menu buttons        
        jPanel5.setOpaque(false);
        
        
        // Style and add each menu button
        JButton[] menuButtons = {btnDashboard, btnExpenses, btnIncome, btnBudget, btnReports};
        for (JButton btn : menuButtons) {
            styleMenuButton(btn);           
        }                
        
        // Replace the old sidebar with the new gradient sidebar
        Component[] components = side.getComponents();
        for (Component cp : components){
            gradientSide.add(cp);
        }
        side.removeAll();
        Container parent = side.getParent();
        parent.remove(side);
        parent.add(gradientSide, "West");
        side = gradientSide;
        
        // Revalidate and repaint
        parent.revalidate();
        parent.repaint();
        
    }
    
    private void styleMenuButton(JButton button) {
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(200, 40));
                
    }
    

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        side = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        lblWelcome = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        btnExpenses = new javax.swing.JButton();
        btnIncome = new javax.swing.JButton();
        btnBudget = new javax.swing.JButton();
        btnReports = new javax.swing.JButton();
        btnDashboard = new javax.swing.JButton();
        Main = new javax.swing.JPanel();
        summary = new javax.swing.JPanel();
        incomeCard = new javax.swing.JPanel();
        income2 = new javax.swing.JLabel();
        lblMonthlyIncome = new javax.swing.JLabel();
        expenseCard = new javax.swing.JPanel();
        income3 = new javax.swing.JLabel();
        lblMonthlyExpenses = new javax.swing.JLabel();
        balanceCard = new javax.swing.JPanel();
        income4 = new javax.swing.JLabel();
        lblTotalBalance = new javax.swing.JLabel();
        Center = new javax.swing.JPanel();
        charts = new javax.swing.JPanel();
        incomeVsExpensesChart = new javax.swing.JPanel();
        expenseCategoriesChart = new javax.swing.JPanel();
        transactions = new javax.swing.JPanel();
        income5 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblRecentTransactions = new javax.swing.JTable();
        btnVieawAll = new javax.swing.JButton();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(jTable1);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Money Mentor - Dashboard");
        setPreferredSize(new java.awt.Dimension(800, 600));

        side.setBackground(new java.awt.Color(255, 255, 255));
        side.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 20));
        side.setPreferredSize(new java.awt.Dimension(200, 250));
        side.setLayout(new javax.swing.BoxLayout(side, javax.swing.BoxLayout.Y_AXIS));

        jPanel2.setToolTipText("");

        jLabel1.setText("MoneyMentor");
        jPanel2.add(jLabel1);

        side.add(jPanel2);

        jPanel3.setPreferredSize(new java.awt.Dimension(50, 100));

        lblWelcome.setText("Welcome,");
        jPanel3.add(lblWelcome);

        side.add(jPanel3);

        jPanel5.setPreferredSize(new java.awt.Dimension(250, 310));
        jPanel5.setLayout(new java.awt.GridLayout(5, 1, 0, 10));

        btnExpenses.setText("Expenses");
        btnExpenses.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExpensesActionPerformed(evt);
            }
        });
        jPanel5.add(btnExpenses);

        btnIncome.setText("Income");
        btnIncome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIncomeActionPerformed(evt);
            }
        });
        jPanel5.add(btnIncome);

        btnBudget.setText("Budget");
        btnBudget.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBudgetActionPerformed(evt);
            }
        });
        jPanel5.add(btnBudget);

        btnReports.setText("Reports");
        btnReports.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReportsActionPerformed(evt);
            }
        });
        jPanel5.add(btnReports);

        btnDashboard.setText("Logout");
        btnDashboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDashboardActionPerformed(evt);
            }
        });
        jPanel5.add(btnDashboard);

        side.add(jPanel5);

        getContentPane().add(side, java.awt.BorderLayout.WEST);

        Main.setBackground(new java.awt.Color(255, 255, 255));
        Main.setLayout(new java.awt.BorderLayout());

        summary.setBackground(new java.awt.Color(255, 255, 255));
        summary.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255), 2));
        summary.setLayout(new java.awt.GridLayout(1, 3, 3, 0));

        incomeCard.setBackground(new java.awt.Color(51, 51, 51));
        incomeCard.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(74, 0, 224), 3));
        incomeCard.setForeground(new java.awt.Color(255, 255, 255));
        incomeCard.setPreferredSize(new java.awt.Dimension(82, 70));
        incomeCard.setLayout(new javax.swing.BoxLayout(incomeCard, javax.swing.BoxLayout.Y_AXIS));

        income2.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        income2.setForeground(new java.awt.Color(255, 255, 255));
        income2.setText("Total Income:");
        incomeCard.add(income2);

        lblMonthlyIncome.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        lblMonthlyIncome.setForeground(new java.awt.Color(255, 255, 255));
        lblMonthlyIncome.setText("$0.00");
        incomeCard.add(lblMonthlyIncome);

        summary.add(incomeCard);

        expenseCard.setBackground(new java.awt.Color(51, 51, 51));
        expenseCard.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 0, 0), 3));
        expenseCard.setForeground(new java.awt.Color(255, 255, 255));
        expenseCard.setPreferredSize(new java.awt.Dimension(85, 50));
        expenseCard.setLayout(new javax.swing.BoxLayout(expenseCard, javax.swing.BoxLayout.Y_AXIS));

        income3.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        income3.setForeground(new java.awt.Color(255, 255, 255));
        income3.setText("Total Expense:");
        expenseCard.add(income3);

        lblMonthlyExpenses.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        lblMonthlyExpenses.setForeground(new java.awt.Color(255, 255, 255));
        lblMonthlyExpenses.setText("$0.00");
        expenseCard.add(lblMonthlyExpenses);

        summary.add(expenseCard);

        balanceCard.setBackground(new java.awt.Color(51, 51, 51));
        balanceCard.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 255, 51), 3));
        balanceCard.setForeground(new java.awt.Color(255, 255, 255));
        balanceCard.setLayout(new javax.swing.BoxLayout(balanceCard, javax.swing.BoxLayout.Y_AXIS));

        income4.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        income4.setForeground(new java.awt.Color(255, 255, 255));
        income4.setText("Balance");
        balanceCard.add(income4);

        lblTotalBalance.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        lblTotalBalance.setForeground(new java.awt.Color(255, 255, 255));
        lblTotalBalance.setText("$0.00");
        balanceCard.add(lblTotalBalance);

        summary.add(balanceCard);

        Main.add(summary, java.awt.BorderLayout.NORTH);

        Center.setBackground(new java.awt.Color(51, 51, 51));
        Center.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255), 2));
        Center.setLayout(new java.awt.GridLayout(2, 1, 0, 10));

        charts.setBackground(new java.awt.Color(51, 51, 51));
        charts.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255), 2));
        charts.setLayout(new java.awt.GridLayout(0, 2, 10, 0));

        incomeVsExpensesChart.setBackground(new java.awt.Color(51, 51, 51));
        incomeVsExpensesChart.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2));
        charts.add(incomeVsExpensesChart);

        expenseCategoriesChart.setBackground(new java.awt.Color(51, 51, 51));
        expenseCategoriesChart.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2));
        charts.add(expenseCategoriesChart);

        Center.add(charts);

        transactions.setBackground(new java.awt.Color(51, 51, 51));
        transactions.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255), 2));
        transactions.setLayout(new java.awt.BorderLayout());

        income5.setBackground(new java.awt.Color(255, 255, 255));
        income5.setFont(new java.awt.Font("Segoe UI Black", 0, 14)); // NOI18N
        income5.setForeground(new java.awt.Color(255, 255, 255));
        income5.setText("Recent Transactions:");
        transactions.add(income5, java.awt.BorderLayout.PAGE_START);

        jScrollPane2.setBackground(new java.awt.Color(255, 255, 255));
        jScrollPane2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(51, 51, 51), 2));

        tblRecentTransactions.setBackground(new java.awt.Color(51, 51, 51));
        tblRecentTransactions.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255), 2));
        tblRecentTransactions.setForeground(new java.awt.Color(255, 255, 255));
        tblRecentTransactions.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(tblRecentTransactions);

        transactions.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        btnVieawAll.setBackground(new java.awt.Color(88, 166, 255));
        btnVieawAll.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnVieawAll.setForeground(new java.awt.Color(255, 255, 255));
        btnVieawAll.setText("View All Transactions");
        btnVieawAll.setActionCommand("View All Transactions");
        btnVieawAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVieawAllActionPerformed(evt);
            }
        });
        transactions.add(btnVieawAll, java.awt.BorderLayout.PAGE_END);

        Center.add(transactions);

        Main.add(Center, java.awt.BorderLayout.CENTER);

        getContentPane().add(Main, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnIncomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIncomeActionPerformed
        showAddIncomeDialog();
    }//GEN-LAST:event_btnIncomeActionPerformed

    private void btnReportsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReportsActionPerformed
        showReportsPanel();
    }//GEN-LAST:event_btnReportsActionPerformed

    private void btnExpensesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExpensesActionPerformed
        showAddExpenseDialog();
    }//GEN-LAST:event_btnExpensesActionPerformed

    private void btnDashboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDashboardActionPerformed
        LoginFrame login = new LoginFrame();
        this.dispose();
        login.setVisible(true);
    }//GEN-LAST:event_btnDashboardActionPerformed

    private void btnBudgetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBudgetActionPerformed
        showBudgetPanel();
    }//GEN-LAST:event_btnBudgetActionPerformed

    private void btnVieawAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVieawAllActionPerformed
        showTransactionPanel();
        
    }//GEN-LAST:event_btnVieawAllActionPerformed
   
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {                
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            }
        });
    }
       
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Center;
    private javax.swing.JPanel Main;
    private javax.swing.JPanel balanceCard;
    private javax.swing.JButton btnBudget;
    private javax.swing.JButton btnDashboard;
    private javax.swing.JButton btnExpenses;
    private javax.swing.JButton btnIncome;
    private javax.swing.JButton btnReports;
    private javax.swing.JButton btnVieawAll;
    private javax.swing.JPanel charts;
    private javax.swing.JPanel expenseCard;
    private javax.swing.JPanel expenseCategoriesChart;
    private javax.swing.JLabel income2;
    private javax.swing.JLabel income3;
    private javax.swing.JLabel income4;
    private javax.swing.JLabel income5;
    private javax.swing.JPanel incomeCard;
    private javax.swing.JPanel incomeVsExpensesChart;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel lblMonthlyExpenses;
    private javax.swing.JLabel lblMonthlyIncome;
    private javax.swing.JLabel lblTotalBalance;
    private javax.swing.JLabel lblWelcome;
    private javax.swing.JPanel side;
    private javax.swing.JPanel summary;
    private javax.swing.JTable tblRecentTransactions;
    private javax.swing.JPanel transactions;
    // End of variables declaration//GEN-END:variables
}
