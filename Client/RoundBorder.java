import java.awt.*;
import javax.swing.border.AbstractBorder;

class RoundBorder extends AbstractBorder {
    private boolean opaque;
    
    public RoundBorder(boolean opaque) {
        this.opaque = opaque;
    }
    
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if (!opaque) {
            g.setColor(Color.WHITE);
            g.fillRoundRect(x, y, width - 1, height - 1, 20, 20);
        }
    }
    public Insets getBorderInsets(Component c) {
        return new Insets(5, 5, 5, 5);
    }
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(5, 5, 5, 5);
        return insets;
    }
}
