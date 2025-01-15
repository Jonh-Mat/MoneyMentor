package com.moneymentor.Database;
import com.moneymentor.Models.Budget;
import com.moneymentor.Models.Category;
import com.moneymentor.Models.Transaction;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.moneymentor.Models.User;
import java.sql.Date;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class DatabaseConnection {
    //Database connection parameters 
    private static final String DATABASE_URL = "jdbc:sqlserver://CMJ\\SQLEXPRESS;databaseName=Money;user=moneymentor;password=YourStrongPassword123;trustServerCertificate=true";
    private static final String DATABASE_USER = "moneymentor";
    private static final String DATABASE_PASSWORD = "12345";
    
    private static DatabaseConnection instance;
    private Connection connection;
    
    //Private constructor for singleton pattern
    private DatabaseConnection(){
        try {
            //Load the SQL server jdbc driver
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            
        }catch (ClassNotFoundException e){
            System.out.println("SQL Server JDBC Driver not found.");
            e.printStackTrace();        
            
        }       
    }
    // Get singleton instance
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }
    
     // Get database connection
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
            }
        } catch (SQLException e) {
            System.out.println("Error connecting to database.");
            e.printStackTrace();
        }
        return connection;
    }
    
    public void closeConnection(){
        try{
            if(connection != null && !connection.isClosed()){
                connection.close();
            }            
        }
        catch (SQLException e) {
            System.out.println("Error closing database connection.");
            e.printStackTrace();
        }
    }
    
    // Databse connection for users
public User validateLogin(String username, String password) {
    String query = "SELECT * FROM Users WHERE Username = ? AND Password = ?";
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
            
        pstmt.setString(1, username);
        pstmt.setString(2, password);
            
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            User user = new User();
            user.setUserId(rs.getInt("UserID"));
            user.setUsername(rs.getString("Username"));
            user.setEmail(rs.getString("Email"));
            user.setFullName(rs.getString("FullName"));
            user.setTotalBalance(rs.getBigDecimal("TotalBalance"));
            updateLastLogin(user.getUserId());
            return user;
            
        }
    } catch (SQLException e) {
        System.out.println("Error in validateLogin: " + e.getMessage());
        e.printStackTrace();
    }
    return null;
}
    private void updateLastLogin(int userId) {
        String query = "UPDATE Users SET LastLogin = GETDATE() WHERE UserID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
     // Register new user
    public boolean registerUser(String username, String password, String email, String fullName) {
        String query = "INSERT INTO Users (Username, Password, Email, FullName) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password); // Note: In production, use password hashing
            pstmt.setString(3, email);
            pstmt.setString(4, fullName);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("Error registering user.");
            e.printStackTrace();
            return false;
        }
    }
    
    // Get user by username
    public User getUser(String username) {
        String query = "SELECT * FROM Users WHERE Username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("UserID"));
                user.setUsername(rs.getString("Username"));
                user.setEmail(rs.getString("Email"));
                user.setFullName(rs.getString("FullName"));
                user.setTotalBalance(rs.getBigDecimal("TotalBalance"));
                return user;
            }
        } catch (SQLException e) {
            System.out.println("Error getting user details.");
            e.printStackTrace();
        }
        return null;
    }
    
    
    // Update user's total balance
    public boolean updateUserBalance(int userId, BigDecimal newBalance) {
        String query = "UPDATE Users SET TotalBalance = ? WHERE UserID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setBigDecimal(1, newBalance);
            pstmt.setInt(2, userId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("Error updating user balance.");
            e.printStackTrace();
            return false;
        }
    }         

    // Get budget summary for current month
    public List<Budget> getCurrentMonthBudgets(int userId) {
        List<Budget> budgets = new ArrayList<>();
        String query = "SELECT b.*, c.CategoryName FROM Budgets b " +
                      "JOIN Categories c ON b.CategoryID = c.CategoryID " +
                      "WHERE b.UserID = ? AND MONTH(b.MonthYear) = MONTH(GETDATE()) " +
                      "AND YEAR(b.MonthYear) = YEAR(GETDATE())";
                      
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Budget budget = new Budget();
                budget.setBudgetId(rs.getInt("BudgetID"));
                budget.setAmount(rs.getBigDecimal("Amount"));
                budget.setMonthYear(rs.getTimestamp("MonthYear").toLocalDateTime());
                budgets.add(budget);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return budgets;
    }

    // Get total income and expenses for current month
    public BigDecimal[] getMonthlyTotals(int userId) {
        BigDecimal[] totals = new BigDecimal[2]; // [income, expenses]
        String query = "SELECT TransactionType, SUM(Amount) as Total " +
                      "FROM Transactions WHERE UserID = ? " +
                      "AND MONTH(TransactionDate) = MONTH(GETDATE()) " +
                      "AND YEAR(TransactionDate) = YEAR(GETDATE()) " +
                      "GROUP BY TransactionType";
                      
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                if ("Income".equals(rs.getString("TransactionType"))) {
                    totals[0] = rs.getBigDecimal("Total");
                } else {
                    totals[1] = rs.getBigDecimal("Total");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return totals;
    }
    
    public boolean addIncome(int userId, BigDecimal amount, int categoryId, 
                        int budgetId, String description, Date date) {
    Connection conn = null;
    try {
        conn = getConnection();
        conn.setAutoCommit(false);  // Start transaction

        // Insert the income transaction
        String transactionQuery = "INSERT INTO Transactions (UserID, Amount, CategoryID, " +
                                "BudgetID, Description, TransactionDate, TransactionType) " +
                                "VALUES (?, ?, ?, ?, ?, ?, 'Income')";
        
        try (PreparedStatement pstmt = conn.prepareStatement(transactionQuery)) {
            pstmt.setInt(1, userId);
            pstmt.setBigDecimal(2, amount);
            pstmt.setInt(3, categoryId);
            pstmt.setInt(4, budgetId);
            pstmt.setString(5, description);
            pstmt.setDate(6, date);
            pstmt.executeUpdate();
        }

        // Update the budget amount
        String budgetQuery = "UPDATE Budgets SET Amount = Amount + ? " +
                           "WHERE BudgetID = ? AND UserID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(budgetQuery)) {
            pstmt.setBigDecimal(1, amount);
            pstmt.setInt(2, budgetId);
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();
        }

        conn.commit();  // Commit transaction
        return true;
        
    } catch (SQLException e) {
        if (conn != null) {
            try {
                conn.rollback();  // Rollback on error
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        e.printStackTrace();
        return false;
    } finally {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
    
     public BigDecimal getMonthlyIncome(int userId) {
        String query = "SELECT COALESCE(SUM(Amount), 0) as TotalIncome " +
                      "FROM Transactions " +
                      "WHERE UserID = ? " +
                      "AND TransactionType = 'Income' " +
                      "AND MONTH(TransactionDate) = MONTH(GETDATE()) " +
                      "AND YEAR(TransactionDate) = YEAR(GETDATE())";
                      
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBigDecimal("TotalIncome");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return BigDecimal.ZERO;
    }
          
     public boolean deleteIncome(int transactionId, int userId) {
        String query = "DELETE FROM Transactions " +
                      "WHERE TransactionID = ? AND UserID = ? " +
                      "AND TransactionType = 'Income'";
                      
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, transactionId);
            pstmt.setInt(2, userId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
     
     public boolean updateIncome(int transactionId, int userId, BigDecimal amount,
                              String category, String description, Date date) {
        String query = "UPDATE Transactions " +
                      "SET Amount = ?, Category = ?, Description = ?, " +
                      "TransactionDate = ? " +
                      "WHERE TransactionID = ? AND UserID = ? " +
                      "AND TransactionType = 'Income'";
                      
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setBigDecimal(1, amount);
            pstmt.setString(2, category);
            pstmt.setString(3, description);
            pstmt.setDate(4, date);
            pstmt.setInt(5, transactionId);
            pstmt.setInt(6, userId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
public boolean addExpense(int userId, BigDecimal amount, int categoryId, 
                         int budgetId, String description, Date date) {
    Connection conn = null;
    try {
        conn = getConnection();
        conn.setAutoCommit(false);  // Start transaction

        // Insert the expense transaction
        String transactionQuery = "INSERT INTO Transactions (UserID, Amount, CategoryID, " +
                                "BudgetID, Description, TransactionDate, TransactionType) " +
                                "VALUES (?, ?, ?, ?, ?, ?, 'Expense')";
        
        try (PreparedStatement pstmt = conn.prepareStatement(transactionQuery)) {
            pstmt.setInt(1, userId);
            pstmt.setBigDecimal(2, amount);
            pstmt.setInt(3, categoryId);
            pstmt.setInt(4, budgetId);
            pstmt.setString(5, description);
            pstmt.setDate(6, date);
            pstmt.executeUpdate();
        }

        // Update the budget amount
        String budgetQuery = "UPDATE Budgets SET Amount = Amount - ? " +
                           "WHERE BudgetID = ? AND UserID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(budgetQuery)) {
            pstmt.setBigDecimal(1, amount);
            pstmt.setInt(2, budgetId);
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();
        }

        conn.commit();  // Commit transaction
        return true;
        
    } catch (SQLException e) {
        if (conn != null) {
            try {
                conn.rollback();  // Rollback on error
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        e.printStackTrace();
        return false;
    } finally {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
    
    public BigDecimal getMonthlyExpenses(int userId) {
    String query = "SELECT COALESCE(SUM(Amount), 0) as TotalExpenses " +
                  "FROM Transactions " +
                  "WHERE UserID = ? " +
                  "AND TransactionType = 'Expense' " +
                  "AND MONTH(TransactionDate) = MONTH(GETDATE()) " +
                  "AND YEAR(TransactionDate) = YEAR(GETDATE())";
                  
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
        
        pstmt.setInt(1, userId);
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            return rs.getBigDecimal("TotalExpenses");
        }
        
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    return BigDecimal.ZERO;
    }
    
    public List<String> getCategoriesByType(String type) {
        List<String> categories = new ArrayList<>();
        String query = "SELECT CategoryName FROM Categories WHERE CategoryType = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, type);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                categories.add(rs.getString("CategoryName"));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error fetching categories: " + e.getMessage());
        }
        
        return categories;
    }
    
    public int getCategoryIdByName(String categoryName) {
        String query = "SELECT CategoryID FROM Categories WHERE CategoryName = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, categoryName);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("CategoryID");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error fetching category ID: " + e.getMessage());
        }
        
        return -1;
    }
    
    public List<Budget> getBudgets(int userId, LocalDateTime monthYear) {
        List<Budget> budgets = new ArrayList<>();
        String query = "SELECT b.*, c.CategoryName FROM Budgets b " +
                      "JOIN Categories c ON b.CategoryID = c.CategoryID " +
                      "WHERE b.UserID = ? AND " +
                      "MONTH(b.MonthYear) = ? AND YEAR(b.MonthYear) = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, monthYear.getMonthValue());
            pstmt.setInt(3, monthYear.getYear());
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Budget budget = new Budget();
                budget.setBudgetId(rs.getInt("BudgetID"));
                budget.setUserId(rs.getInt("UserID"));
                budget.setCategoryId(rs.getInt("CategoryID"));
                budget.setMonthYear(rs.getTimestamp("MonthYear").toLocalDateTime());
                budget.setAmount(rs.getBigDecimal("Amount"));
                budget.setCreatedDate(rs.getTimestamp("CreatedDate").toLocalDateTime());
                budgets.add(budget);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return budgets;
    }

    public boolean addBudget(Budget budget) {
        String query = "INSERT INTO Budgets (UserID, CategoryID, MonthYear, " +
                      "Amount, CreatedDate) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, 
                 Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, budget.getUserId());
            pstmt.setInt(2, budget.getCategoryId());
            pstmt.setTimestamp(3, Timestamp.valueOf(budget.getMonthYear()));
            pstmt.setBigDecimal(4, budget.getAmount());
            pstmt.setTimestamp(5, Timestamp.valueOf(budget.getCreatedDate()));
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    budget.setBudgetId(rs.getInt(1));
                    return true;
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }

    public boolean updateBudget(Budget budget) {
        String query = "UPDATE Budgets SET Amount = ? WHERE BudgetID = ? " +
                      "AND UserID = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setBigDecimal(1, budget.getAmount());
            pstmt.setInt(2, budget.getBudgetId());
            pstmt.setInt(3, budget.getUserId());
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public BigDecimal getSpentAmount(int userId, int categoryId, LocalDateTime monthYear) {
        String query = "SELECT COALESCE(SUM(Amount), 0) as SpentAmount " +
                      "FROM Transactions " +
                      "WHERE UserID = ? AND CategoryID = ? " +
                      "AND MONTH(TransactionDate) = ? " +
                      "AND YEAR(TransactionDate) = ? " +
                      "AND TransactionType = 'Expense'";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, categoryId);
            pstmt.setInt(3, monthYear.getMonthValue());
            pstmt.setInt(4, monthYear.getYear());
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("SpentAmount");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return BigDecimal.ZERO;
    }
    
     public String getCategoryName(int categoryId) {
        String query = "SELECT CategoryName FROM Categories WHERE CategoryID = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("CategoryName");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error getting category name: " + e.getMessage());
        }
        
        return null;
    }
     
     public List<Category> getUnbudgetedCategories(int userId, LocalDateTime monthYear) {
        List<Category> categories = new ArrayList<>();
        
        String query = "SELECT c.CategoryID, c.CategoryName, c.CategoryType, c.Description " +
                      "FROM Categories c " +
                      "LEFT JOIN Budgets b ON c.CategoryID = b.CategoryID " +
                      "AND b.UserID = ? " +
                      "AND MONTH(b.MonthYear) = ? " +
                      "AND YEAR(b.MonthYear) = ? " +
                      "WHERE c.CategoryType = 'Expense' " +
                      "AND b.BudgetID IS NULL";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, monthYear.getMonthValue());
            pstmt.setInt(3, monthYear.getYear());
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Category category = new Category();
                category.setCategoryId(rs.getInt("CategoryID"));
                category.setCategoryName(rs.getString("CategoryName"));
                category.setCategoryType(rs.getString("CategoryType"));
                category.setDescription(rs.getString("Description"));
                categories.add(category);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error getting unbudgeted categories: " + e.getMessage());
        }
        
        return categories;
    }
     
    public List<Category> getExpenseCategories() {
    List<Category> categories = new ArrayList<>();
    String query = "SELECT * FROM Categories WHERE CategoryType = 'Expense'";
    
    try (Connection conn = getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {
        
        while (rs.next()) {
            Category category = new Category();
            category.setCategoryId(rs.getInt("CategoryID"));
            category.setCategoryName(rs.getString("CategoryName"));
            category.setCategoryType(rs.getString("CategoryType"));
            category.setDescription(rs.getString("Description"));
            categories.add(category);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    return categories;
}

    public BigDecimal getMonthlyIncome(int userId, int month, int year) {
    String query = "SELECT COALESCE(SUM(Amount), 0) as TotalIncome FROM Transactions t " +
                  "JOIN Categories c ON t.CategoryID = c.CategoryID " +
                  "WHERE t.UserID = ? AND c.CategoryType = 'Income' " +
                  "AND MONTH(TransactionDate) = ? AND YEAR(TransactionDate) = ?";
    
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
        
        pstmt.setInt(1, userId);
        pstmt.setInt(2, month);
        pstmt.setInt(3, year);
        
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getBigDecimal("TotalIncome");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return BigDecimal.ZERO;
}

public BigDecimal getMonthlyExpenses(int userId, int month, int year) {
    String query = "SELECT COALESCE(SUM(Amount), 0) as TotalExpenses FROM Transactions t " +
                  "JOIN Categories c ON t.CategoryID = c.CategoryID " +
                  "WHERE t.UserID = ? AND c.CategoryType = 'Expense' " +
                  "AND MONTH(TransactionDate) = ? AND YEAR(TransactionDate) = ?";
    
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
        
        pstmt.setInt(1, userId);
        pstmt.setInt(2, month);
        pstmt.setInt(3, year);
        
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getBigDecimal("TotalExpenses");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return BigDecimal.ZERO;
}

public List<String> getBudgetsByUser(int userId) {
    List<String> budgets = new ArrayList<>();
    String query = "SELECT b.BudgetID, b.CategoryID, b.MonthYear, b.Amount, c.CategoryName " +
                  "FROM Budgets b " +
                  "JOIN Categories c ON b.CategoryID = c.CategoryID " +
                  "WHERE b.UserID = ?";
    
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
        
        pstmt.setInt(1, userId);
        ResultSet rs = pstmt.executeQuery();
        
        while (rs.next()) {
            int budgetId = rs.getInt("BudgetID");
            String categoryName = rs.getString("CategoryName");
            java.sql.Timestamp monthYear = rs.getTimestamp("MonthYear");
            BigDecimal amount = rs.getBigDecimal("Amount");
            
            // Format the date to show month and year
            LocalDateTime dateTime = monthYear.toLocalDateTime();
            String monthYearStr = dateTime.getMonth().toString() + " " + dateTime.getYear();
            
            // Create a display string that combines category and month/year
            String budgetDisplay = String.format("%s - %s (%.2f)", 
                                               categoryName, 
                                               monthYearStr, 
                                               amount);
            
            budgets.add(budgetDisplay);
            System.out.println("Debug - Found budget: " + budgetDisplay);
        }
        
    } catch (SQLException e) {
        System.err.println("Error loading budgets: " + e.getMessage());
        e.printStackTrace();
    }
    return budgets;
}

public int getBudgetIdByName(String budgetName) {
    String query = "SELECT BudgetID FROM Budgets WHERE BudgetName = ?";
    
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
        
        pstmt.setString(1, budgetName);
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            return rs.getInt("BudgetID");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return -1;
}

public int getBudgetIdFromDisplay(String budgetDisplay, int userId) {
    // Extract category name from the display string (everything before the first " - ")
    String categoryName = budgetDisplay.split(" - ")[0];
    
    String query = "SELECT b.BudgetID FROM Budgets b " +
                  "JOIN Categories c ON b.CategoryID = c.CategoryID " +
                  "WHERE b.UserID = ? AND c.CategoryName = ?";
    
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
        
        pstmt.setInt(1, userId);
        pstmt.setString(2, categoryName);
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            return rs.getInt("BudgetID");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return -1;
}

public List<Transaction> getRecentTransactions(int userId, int limit) {
    List<Transaction> transactions = new ArrayList<>();
    String query = "SELECT TOP (?) t.TransactionID, t.UserID, t.CategoryID, " +
                  "t.BudgetID, t.Amount, t.TransactionType, t.Description, " +
                  "t.TransactionDate, c.CategoryName " +
                  "FROM Transactions t " +
                  "JOIN Categories c ON t.CategoryID = c.CategoryID " +
                  "WHERE t.UserID = ? " +
                  "ORDER BY t.TransactionDate DESC";
    
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
        
        pstmt.setInt(1, limit);
        pstmt.setInt(2, userId);
        ResultSet rs = pstmt.executeQuery();
        
        while (rs.next()) {
            Transaction transaction = new Transaction();
            transaction.setTransactionId(rs.getInt("TransactionID"));
            transaction.setUserId(rs.getInt("UserID"));
            transaction.setCategoryId(rs.getInt("CategoryID"));
            transaction.setBudgetId(rs.getInt("BudgetID"));
            transaction.setAmount(rs.getBigDecimal("Amount"));
            transaction.setTransactionType(rs.getString("TransactionType"));
            transaction.setDescription(rs.getString("Description"));
            
            // Convert SQL Timestamp to LocalDateTime
            Timestamp transactionDate = rs.getTimestamp("TransactionDate");
            if (transactionDate != null) {
                transaction.setTransactionDate(transactionDate.toLocalDateTime());
            }
            
            transactions.add(transaction);
        }
    } catch (SQLException e) {
        System.err.println("Error getting recent transactions: " + e.getMessage());
        e.printStackTrace();
    }
    return transactions;
}

public boolean deleteTransaction(int transactionId, int userId) {
    String query = "DELETE FROM Transactions WHERE TransactionID = ? AND UserID = ?";
    
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
        
        pstmt.setInt(1, transactionId);
        pstmt.setInt(2, userId);
        
        int rowsAffected = pstmt.executeUpdate();
        return rowsAffected > 0;
        
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}

public boolean updateTransaction(int transactionId, int userId, BigDecimal amount, 
                               int categoryId, int budgetId, String description, 
                               Date date) {
    String query = "UPDATE Transactions SET Amount = ?, CategoryID = ?, " +
                  "BudgetID = ?, Description = ?, TransactionDate = ? " +
                  "WHERE TransactionID = ? AND UserID = ?";
    
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
        
        pstmt.setBigDecimal(1, amount);
        pstmt.setInt(2, categoryId);
        pstmt.setInt(3, budgetId);
        pstmt.setString(4, description);
        pstmt.setDate(5, date);
        pstmt.setInt(6, transactionId);
        pstmt.setInt(7, userId);
        
        int rowsAffected = pstmt.executeUpdate();
        return rowsAffected > 0;
        
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}

}
