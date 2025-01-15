package com.moneymentor.Models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Budget {
    private int budgetId =0;
    private int userId;
    private int categoryId;
    private LocalDateTime monthYear;
    private BigDecimal amount;
    private LocalDateTime createdDate;
    
    // Constructors
    public Budget() {}
    
    public Budget(int userId, int categoryId, LocalDateTime monthYear, BigDecimal amount) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.monthYear = monthYear;
        this.amount = amount;
        this.createdDate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public int getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(int budgetId) {
        this.budgetId = budgetId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public LocalDateTime getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(LocalDateTime monthYear) {
        this.monthYear = monthYear;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}