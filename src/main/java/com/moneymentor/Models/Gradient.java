package com.moneymentor.Models;

import javax.swing.*;
import java.awt.*;

public class Gradient extends javax.swing.JPanel {
    private final Color startColor = Color.decode("#8E2DE2");
    private final Color endColor = Color.decode("#4A00E0");
    
    public Gradient() {
        initComponents();
        setOpaque(false); // Make panel non-opaque to show gradient
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    @Override
    protected void paintComponent(Graphics g) {
       super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw main gradient
        GradientPaint gradient = new GradientPaint(
            0, 0, startColor,
            0, getHeight(), endColor
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());        
       
        // Add subtle shadow on the right edge
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillRect(getWidth() - 2, 0, 2, getHeight());
        
        g2d.dispose();
    }
    
        // Add a glass effect
    private void addGlassEffect(Graphics2D g2d) {
        // Create subtle highlight at the top
        GradientPaint glassEffect = new GradientPaint(
            0, 0, new Color(255, 255, 255, 30),
            0, 50, new Color(255, 255, 255, 0)
        );
        g2d.setPaint(glassEffect);
        g2d.fillRect(0, 0, getWidth(), 50);
    }
        
}
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

