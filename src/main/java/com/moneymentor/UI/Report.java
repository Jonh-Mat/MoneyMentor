package com.moneymentor.UI;

import com.moneymentor.Database.DatabaseConnection;
import com.moneymentor.Models.Budget;
import com.moneymentor.Models.Category;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import java.util.List;

public class Report extends javax.swing.JDialog {       
    private final int userId;
    private DatabaseConnection dbConnection;
    private DefaultTableModel reportTableModel;
    private ChartPanel chartPanel;
    
    public Report(java.awt.Frame parent, boolean modal, int userId) {
        super(parent, modal);
        this.userId = userId;
        this.dbConnection = DatabaseConnection.getInstance();
        initComponents();
        setupComponents();
        
        setPreferredSize(new java.awt.Dimension(800, 400));
        setSize(new java.awt.Dimension(900, 700));
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
            years[i] = String.valueOf(currentYear - i);
        }
        cmbYear.setModel(new DefaultComboBoxModel<>(years));

        // Set current month and year
        cmbMonth.setSelectedIndex(LocalDateTime.now().getMonthValue() - 1);
        cmbYear.setSelectedItem(String.valueOf(currentYear));

        // Setup report type combo box
        String[] reportTypes = {
            "Expense Summary",
            "Budget vs Actual",
            "Category Analysis"
        };
        cmbReportType.setModel(new DefaultComboBoxModel<>(reportTypes));

        // Setup table model
        String[] columnNames = {"Category", "Budget", "Actual", "Variance", "% Used"};
        reportTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        tblReport.setModel(reportTableModel);
    
        
        // Initialize chart panel
        chartPanel = new ChartPanel(null);
        chartPanel.setPreferredSize(new Dimension(400, 300));
        pnlChart.setLayout(new BorderLayout());
        pnlChart.add(chartPanel, BorderLayout.CENTER);

        // Add event listeners
        cmbReportType.addActionListener(e -> generateReport());
        cmbMonth.addActionListener(e -> generateReport());
        cmbYear.addActionListener(e -> generateReport());
        btnExport.addActionListener(e -> exportReport());
        btnClose.addActionListener(e -> dispose());

        // Generate initial report
        generateReport();
    }
     
      private void generateReport() {
        String reportType = (String) cmbReportType.getSelectedItem();
        int month = cmbMonth.getSelectedIndex() + 1;
        int year = Integer.parseInt((String) cmbYear.getSelectedItem());
        LocalDateTime period = LocalDateTime.of(year, month, 1, 0, 0);

        switch (reportType) {
            case "Expense Summary":
                generateExpenseSummary(period);
                break;
            case "Budget vs Actual":
                generateBudgetVsActual(period);
                break;
            case "Category Analysis":
                generateCategoryAnalysis(period);
                break;
        }
    }
     
     private void generateExpenseSummary(LocalDateTime period) {
        reportTableModel.setRowCount(0);
        DefaultPieDataset dataset = new DefaultPieDataset();

        try {
            List<Category> categories = dbConnection.getExpenseCategories();
            BigDecimal totalExpenses = BigDecimal.ZERO;

            for (Category category : categories) {
                BigDecimal spent = dbConnection.getSpentAmount(userId, category.getCategoryId(), period);
                if (spent.compareTo(BigDecimal.ZERO) > 0) {
                    reportTableModel.addRow(new Object[]{
                        category.getCategoryName(),
                        "-",
                        spent,
                        "-",
                        "-"
                    });
                    dataset.setValue(category.getCategoryName(), spent.doubleValue());
                    totalExpenses = totalExpenses.add(spent);
                }
            }

            // Add total row
            reportTableModel.addRow(new Object[]{
                "Total",
                "-",
                totalExpenses,
                "-",
                "-"
            });

            // Create pie chart
            JFreeChart chart = ChartFactory.createPieChart(
                "Expense Distribution",
                dataset,
                true,  // legend
                true,  // tooltips
                false  // urls
            );
            
            chartPanel.setChart(chart);

        } catch (Exception ex) {
            showError("Error generating expense summary: " + ex.getMessage());
        }
    }
     
     private void generateBudgetVsActual(LocalDateTime period) {
        reportTableModel.setRowCount(0);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try {
            List<Budget> budgets = dbConnection.getBudgets(userId, period);
            BigDecimal totalBudget = BigDecimal.ZERO;
            BigDecimal totalSpent = BigDecimal.ZERO;

            for (Budget budget : budgets) {
                String categoryName = dbConnection.getCategoryName(budget.getCategoryId());
                BigDecimal spent = dbConnection.getSpentAmount(userId, budget.getCategoryId(), period);
                BigDecimal variance = budget.getAmount().subtract(spent);
                double percentUsed = spent.doubleValue() / budget.getAmount().doubleValue() * 100;

                reportTableModel.addRow(new Object[]{
                    categoryName,
                    budget.getAmount(),
                    spent,
                    variance,
                    String.format("%.1f%%", percentUsed)
                });

                dataset.addValue(budget.getAmount().doubleValue(), "Budget", categoryName);
                dataset.addValue(spent.doubleValue(), "Actual", categoryName);

                totalBudget = totalBudget.add(budget.getAmount());
                totalSpent = totalSpent.add(spent);
            }

            // Create bar chart
            JFreeChart chart = ChartFactory.createBarChart(
                "Budget vs Actual Expenses",
                "Category",
                "Amount",
                dataset,
                PlotOrientation.VERTICAL,
                true,   // legend
                true,   // tooltips
                false   // urls
            );

            chartPanel.setChart(chart);

        } catch (Exception ex) {
            showError("Error generating budget vs actual report: " + ex.getMessage());
        }
    }

    private void generateCategoryAnalysis(LocalDateTime period) {
        reportTableModel.setRowCount(0);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try {
            List<Category> categories = dbConnection.getExpenseCategories();
            
            // Get previous period
            LocalDateTime previousPeriod = period.minusMonths(1);

            for (Category category : categories) {
                BigDecimal currentSpent = dbConnection.getSpentAmount(userId, category.getCategoryId(), period);
                BigDecimal previousSpent = dbConnection.getSpentAmount(userId, category.getCategoryId(), previousPeriod);
                
                if (currentSpent.compareTo(BigDecimal.ZERO) > 0 || previousSpent.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal variance = currentSpent.subtract(previousSpent);
                    double percentChange = previousSpent.compareTo(BigDecimal.ZERO) == 0 ? 100 :
                        (currentSpent.subtract(previousSpent)).doubleValue() / previousSpent.doubleValue() * 100;

                    reportTableModel.addRow(new Object[]{
                        category.getCategoryName(),
                        previousSpent,
                        currentSpent,
                        variance,
                        String.format("%.1f%%", percentChange)
                    });

                    dataset.addValue(previousSpent.doubleValue(), "Previous Month", category.getCategoryName());
                    dataset.addValue(currentSpent.doubleValue(), "Current Month", category.getCategoryName());
                }
            }

            // Create line chart
            JFreeChart chart = ChartFactory.createLineChart(
                "Category Spending Trend",
                "Category",
                "Amount",
                dataset,
                PlotOrientation.VERTICAL,
                true,   // legend
                true,   // tooltips
                false   // urls
            );

            chartPanel.setChart(chart);

        } catch (Exception ex) {
            showError("Error generating category analysis: " + ex.getMessage());
        }
    }

    private void exportReport() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Report");
            
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String filepath = fileChooser.getSelectedFile().getPath();
                if (!filepath.toLowerCase().endsWith(".csv")) {
                    filepath += ".csv";
                }

                StringBuilder csv = new StringBuilder();
                
                // Add headers
                for (int i = 0; i < reportTableModel.getColumnCount(); i++) {
                    csv.append(reportTableModel.getColumnName(i)).append(",");
                }
                csv.append("\n");

                // Add data
                for (int i = 0; i < reportTableModel.getRowCount(); i++) {
                    for (int j = 0; j < reportTableModel.getColumnCount(); j++) {
                        csv.append(reportTableModel.getValueAt(i, j)).append(",");
                    }
                    csv.append("\n");
                }

                java.nio.file.Files.writeString(java.nio.file.Path.of(filepath), csv.toString());

                JOptionPane.showMessageDialog(this,
                    "Report exported successfully to " + filepath,
                    "Export Success",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            showError("Error exporting report: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }
     
     
     
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        gradient1 = new com.moneymentor.Models.Gradient();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblReport = new javax.swing.JTable();
        pnlChart = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        cmbReportType = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        cmbMonth = new javax.swing.JComboBox<>();
        cmbYear = new javax.swing.JComboBox<>();
        gradient2 = new com.moneymentor.Models.Gradient();
        btnExport = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(800, 600));

        gradient1.setLayout(new java.awt.GridBagLayout());

        jLabel5.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("REPORTS");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(17, 290, 0, 0);
        gradient1.add(jLabel5, gridBagConstraints);

        jLabel7.setFont(new java.awt.Font("Segoe UI Black", 1, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("Welcome to the Reports board, Where you can see your transactions summary");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 12, 31, 52);
        gradient1.add(jLabel7, gridBagConstraints);

        getContentPane().add(gradient1, java.awt.BorderLayout.NORTH);

        jPanel4.setLayout(new java.awt.BorderLayout());

        jPanel3.setLayout(new java.awt.BorderLayout());

        jSplitPane2.setDividerLocation(400);
        jSplitPane2.setResizeWeight(0.5);

        jScrollPane1.setBackground(new java.awt.Color(51, 51, 51));

        tblReport.setBackground(new java.awt.Color(51, 51, 51));
        tblReport.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        tblReport.setForeground(new java.awt.Color(255, 255, 255));
        tblReport.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(tblReport);

        jSplitPane2.setLeftComponent(jScrollPane1);

        pnlChart.setLayout(new java.awt.BorderLayout());
        jSplitPane2.setRightComponent(pnlChart);

        jPanel3.add(jSplitPane2, java.awt.BorderLayout.CENTER);

        jPanel4.add(jPanel3, java.awt.BorderLayout.CENTER);

        jPanel1.setBackground(new java.awt.Color(51, 51, 51));
        jPanel1.setForeground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("ReportType:");
        jPanel1.add(jLabel1);

        cmbReportType.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        cmbReportType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jPanel1.add(cmbReportType);

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Periode:");
        jPanel1.add(jLabel2);

        cmbMonth.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        cmbMonth.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jPanel1.add(cmbMonth);

        cmbYear.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        cmbYear.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jPanel1.add(cmbYear);

        jPanel4.add(jPanel1, java.awt.BorderLayout.PAGE_START);

        getContentPane().add(jPanel4, java.awt.BorderLayout.CENTER);

        gradient2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        btnExport.setBackground(new java.awt.Color(88, 166, 255));
        btnExport.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnExport.setForeground(new java.awt.Color(255, 255, 255));
        btnExport.setText("Export");
        gradient2.add(btnExport);

        btnClose.setBackground(new java.awt.Color(88, 166, 255));
        btnClose.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnClose.setForeground(new java.awt.Color(255, 255, 255));
        btnClose.setText("Close");
        gradient2.add(btnClose);

        getContentPane().add(gradient2, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    public static void main(String args[]) {        
        
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Report dialog = new Report(new javax.swing.JFrame(), true, 1);
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
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnExport;
    private javax.swing.JComboBox<String> cmbMonth;
    private javax.swing.JComboBox<String> cmbReportType;
    private javax.swing.JComboBox<String> cmbYear;
    private com.moneymentor.Models.Gradient gradient1;
    private com.moneymentor.Models.Gradient gradient2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JPanel pnlChart;
    private javax.swing.JTable tblReport;
    // End of variables declaration//GEN-END:variables
}
