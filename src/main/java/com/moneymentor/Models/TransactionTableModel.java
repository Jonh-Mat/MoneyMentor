package com.moneymentor.Models;

import com.moneymentor.Models.Transaction;
import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.time.format.DateTimeFormatter;

public class TransactionTableModel extends AbstractTableModel {
    private final String[] columnNames = {"Date", "Type", "Category", "Description", "Amount"};
    private List<Transaction> transactions;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public TransactionTableModel(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @Override
    public int getRowCount() {
        return transactions.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Transaction transaction = transactions.get(rowIndex);
        
        switch (columnIndex) {
            case 0: return transaction.getTransactionDate().format(dateFormatter);
            case 1: return transaction.getTransactionType();
            case 2: return ""; // We'll need to add category name
            case 3: return transaction.getDescription();
            case 4: return String.format("$%.2f", transaction.getAmount());
            default: return null;
        }
    }
}