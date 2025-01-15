package com.moneymentor.Util;

import javax.swing.*;
import java.awt.*;

public class GradientSidePanel extends JPanel {
    private final Color startColor = Color.decode("#8E2DE2");
    private final Color endColor = Color.decode("#4A00E0");

    public GradientSidePanel() {
        setOpaque(false); // Make panel non-opaque to show gradient
    }

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
        
        // Add glass effect
        addGlassEffect(g2d);
        
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