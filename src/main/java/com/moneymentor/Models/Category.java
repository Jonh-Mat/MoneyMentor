/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.moneymentor.Models;

public class Category {
    private int categoryId;
    private String categoryName;
    private String categoryType; // "Income" or "Expense"
    private String description;
    
    // Constructors
    public Category() {}
    
    public Category(String categoryName, String categoryType, String description) {
        this.categoryName = categoryName;
        this.categoryType = categoryType;
        this.description = description;
    }
    
    // Getters and Setters
    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(String categoryType) {
        this.categoryType = categoryType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
     @Override
    public String toString() {
        // This is important for the JComboBox to display the category name
        return categoryName;
    }
}
