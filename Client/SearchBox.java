import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;
import static javax.swing.KeyStroke.getKeyStroke;

class SearchBox extends JTextArea {
    public SearchBox(int rows, int columns) {
        super(rows, columns);
        setLineWrap(true);
        setWrapStyleWord(true);
        disableTab(this);
    }
    public SearchBox(int rows, int columns, int chars) {
        super(rows, columns);
        setOpaque(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        disableTab(this);
        setBorder(new RoundBorder(true));
        ((AbstractDocument)getDocument()).setDocumentFilter(new DocumentLengthFilter(chars));
    }
    
    // Color Search Bar
    @Override
    public void paintComponent(Graphics g) {
        g.setColor(new Color(255, 255, 255));
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
        super.paintComponent(g);
    }
    // Make Tab Change Focus
    public static void disableTab(JTextComponent comp) {
        comp.getInputMap(JTextComponent.WHEN_FOCUSED).put(getKeyStroke("TAB"), "focusForward");
        comp.getActionMap().put("focusForward", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                comp.transferFocus();
            }
        });
        comp.getInputMap(JTextComponent.WHEN_FOCUSED).put(getKeyStroke("shift TAB"), "focusBackward");
        comp.getActionMap().put("focusBackward", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                comp.transferFocusBackward();
            }
        });
    }
    
    // Implement Character Limits
    public static class DocumentLengthFilter extends DocumentFilter {
        private int limit;
        public DocumentLengthFilter(int limit) {
            this.limit = limit;
        }
        
        @Override
        public void replace(FilterBypass fb, int offs, int length, String str, AttributeSet a) throws BadLocationException {
            try {
                if (fb.getDocument().getLength() + str.length() - length <= limit && !str.equals("\n"))
                    super.replace(fb, offs, length, str.replaceAll("[^A-Za-z0-9]", ""), a);
            } catch (BadLocationException e) { e.printStackTrace(); }
        }
        public void insertUpdate(DocumentEvent e) { filter(); }
        public void removeUpdate(DocumentEvent e) { filter(); }
        public void changedUpdate(DocumentEvent e) { filter(); }
        
        private void filter() {
            JScrollPane pane;
        }
    }
}
