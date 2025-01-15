package com.moneymentor.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.border.EmptyBorder;

public class ModernPasswordField extends JPasswordField {
    private Color borderColor = new Color(88, 166, 255);
    private boolean focused = false;
    
    public ModernPasswordField() {
        super();
        setupStyle();
    }
    
    private void setupStyle() {
        setOpaque(false);
        setBackground(new Color(45, 45, 45));
        setForeground(Color.WHITE);
        setCaretColor(Color.WHITE);
        setFont(new Font("Segoe UI", Font.PLAIN, 14));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 60)),
            new EmptyBorder(8, 10, 8, 10)
        ));
        
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focused = true;
                repaint();
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                focused = false;
                repaint();
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Paint background
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        
        // Paint border
        if (focused) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(2));
        } else {
            g2.setColor(new Color(60, 60, 60));
            g2.setStroke(new BasicStroke(1));
        }
        g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
        
        super.paintComponent(g);
        g2.dispose();
    }
}