import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

class CustomScroll extends BasicScrollBarUI {
    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        g.setColor(Color.WHITE);
        g.fillRect(r.x, r.y, r.width, r.height);
    }
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        g.setColor(new Color(240, 240, 240));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 20, 20);
    }
    protected JButton createDecreaseButton(int orientation) {
        return new JButton() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(0, 0);
            }
        };
    }
    protected JButton createIncreaseButton(int orientation) {
        return new JButton() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(0, 0);
            }
        };
    }
    public Dimension getPreferredSize(JComponent c) {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL)
            return new Dimension(10, super.getPreferredSize(c).height);
        return new Dimension(super.getPreferredSize(c).width, 10);
    }
}