import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import javax.swing.*;
import javax.swing.event.*;

class GUI {
    public static JSplitPane[] splitPanes = new JSplitPane[2];
    public static JPanel friendList;        // Friends List
    public static JPanel history;           // Chat History
    public static JPanel friendRequests;    // Friend Requests
    public static JPanel users;             // User List (not Friends)
    public static JScrollPane userScroll;   // Scroll Pane for users
    public static JButton files;            // Button for Sending Files
    
    public static void chatPanel() {
        // Setting Up Sides
        JPanel chat = new JPanel(new BorderLayout());
        JPanel friends = new JPanel(new BorderLayout());
        chat.setMinimumSize(new Dimension(Client.frame.getContentPane().getWidth() / 3, 0));
        friends.setMinimumSize(new Dimension(Client.frame.getContentPane().getWidth() / 3, 0));
        chat.setPreferredSize(new Dimension(Client.frame.getContentPane().getWidth() / 2, Client.frame.getContentPane().getHeight()));
        friends.setPreferredSize(new Dimension(Client.frame.getContentPane().getWidth() / 2, Client.frame.getContentPane().getHeight()));
        
        // Setting Up Split Pane
        splitPanes[0] = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chat, friends);
        splitPanes[0].setResizeWeight(0.5);
        splitPanes[0].setDividerSize(2);
        Client.frame.add(splitPanes[0], "Chat");
        
        // Searching for Chat
        JPanel search = new JPanel(new FlowLayout(FlowLayout.CENTER));
        search.setPreferredSize(new Dimension(friends.getWidth(), 35));
        search.add(new SearchBox(1, 15, 14));
        friends.add(search, BorderLayout.NORTH);
        
        // Chat Menu
        friendList = new JPanel();
        friendList.setLayout(new BoxLayout(friendList, BoxLayout.Y_AXIS));
        JScrollPane menuScroll = new JScrollPane(friendList);
        menuScroll.setBorder(null);
        menuScroll.setPreferredSize(new Dimension(friends.getWidth(), friends.getHeight() - 35));
        friends.add(menuScroll, BorderLayout.CENTER);
        
        // Conversation Panel
        history = new JPanel();
        history.setLayout(new BoxLayout(history, BoxLayout.Y_AXIS));
        JScrollPane messages = new JScrollPane(history);
        messages.setMinimumSize(new Dimension(0, 0));
        messages.setBorder(null);
        chat.add(messages, BorderLayout.CENTER);
        
        // File Input Box
        JPanel typing = new JPanel(new FlowLayout(FlowLayout.CENTER));
        files = new JButton("File");
        files.setPreferredSize(new Dimension(60, 20));
        files.setEnabled(false);
        files.addActionListener((ActionEvent event) -> {
            FileDialog fd = new FileDialog(Client.frame, "Files", FileDialog.LOAD);
            fd.setVisible(true);
            String filename = fd.getFile();
            if (filename != null && !Client.recipient.equals("")) {
                File file = new File(fd.getDirectory() + filename);
                byte[] buffer = new byte[4096];
                int read;
                
                if (Client.pw != null) {
                    Client.pw.println("FILE " + Client.recipient + " " + (int)file.length() + " " + filename);
                    try (FileInputStream fis = new FileInputStream(fd.getDirectory() + filename)) {
                        while ((read = fis.read(buffer)) != -1)
                            Client.os.write(buffer, 0, read);
                        Client.os.flush();
                    } catch (IOException e) { e.printStackTrace(); }
                }
                
                JPanel message = new JPanel(new FlowLayout(FlowLayout.LEFT));
                SearchBox area = new SearchBox(1, 30);
                area.setText(filename);
                area.setEditable(false);
                area.setBackground(Color.BLUE);
                message.add(area);
                history.add(message);
                history.revalidate();
                history.repaint();
                
                // Edits File
                try {
                    Files.writeString(Paths.get(Client.name + ".txt"), Client.name + " " + Client.recipient + " FILE " + filename, StandardOpenOption.APPEND);
                } catch (IOException e) { e.printStackTrace(); }
            }
        });
        typing.add(files);
        
        // Conversation Input Box
        SearchBox textbox = new SearchBox(1, 25);
        textbox.getDocument().addDocumentListener(new DocumentListener() {
            private void updateRows() {
                textbox.setRows(Math.min(textbox.getLineCount(), 5));
                typing.revalidate();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) { updateRows(); }
            public void insertUpdate(DocumentEvent e) { updateRows(); }
            public void removeUpdate(DocumentEvent e) { updateRows(); }
        });
        JScrollPane scrollWindow = new JScrollPane(textbox);
        scrollWindow.setBorder(new RoundBorder(false));
        scrollWindow.getVerticalScrollBar().setUI(new CustomScroll());
        typing.add(scrollWindow);
        chat.add(typing, BorderLayout.SOUTH);
        
        // Send Message Button
        SendButton send = new SendButton();
        send.setPreferredSize(new Dimension(20, 20));
        send.setContentAreaFilled(false);
        send.addActionListener((event) -> {
            if (Client.recipient != "" && textbox.getText() != null && !textbox.getText().equals("")) {
                JPanel message = new JPanel(new FlowLayout(FlowLayout.LEFT));
                SearchBox area = new SearchBox(1, 30);
                area.setText(textbox.getText());
                area.setEditable(false);
                area.setBackground(Color.BLUE);
                message.add(area);
                history.add(message);
                if (textbox.getText().contains("\n"))
                    textbox.setText(textbox.getText().replace("\n", "\\n"));
                if (Client.pw != null)
                    Client.pw.println(Client.recipient + " " + textbox.getText());
                
                // Edit File
                try {
                    java.util.List<String> lines = Files.readAllLines(Paths.get(Client.name + ".txt"));
                    lines.add(Client.name + " " + Client.recipient + " " + textbox.getText());
                    Files.write(Paths.get(Client.name + ".txt"), lines);
                } catch (IOException e) { e.printStackTrace(); }
                
                // Updates File if Necessary
                if (Client.ss == null) {
                    try {
                        Files.writeString(Paths.get(Client.name + "_updates.txt"), Client.recipient + " " + textbox.getText() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }
            textbox.setText("");
        });
        typing.add(send);
        
        // Switching Panels
        JButton button = new JButton("Friends");
        button.setPreferredSize(new Dimension(friends.getWidth(), 35));
        button.addActionListener((ActionEvent e) -> Client.layout.show(Client.frame.getContentPane(), "Friends"));
        button.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) button.doClick();
            }
        });
        friends.add(button, BorderLayout.SOUTH);
    }
    
    public static void friendsPanel() {
        // Setting Up Sides
        JPanel suggestions = new JPanel(new BorderLayout());
        JPanel requests = new JPanel(new BorderLayout());
        suggestions.setMinimumSize(new Dimension(Client.frame.getContentPane().getWidth() / 3, 0));
        requests.setMinimumSize(new Dimension(Client.frame.getContentPane().getWidth() / 3, 0));
        suggestions.setPreferredSize(new Dimension(Client.frame.getContentPane().getWidth() / 2, Client.frame.getContentPane().getHeight()));
        requests.setPreferredSize(new Dimension(Client.frame.getContentPane().getWidth() / 2, Client.frame.getContentPane().getHeight()));
        
        // Setting Up Split Pane
        splitPanes[1] = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, suggestions, requests);
        splitPanes[1].setResizeWeight(0.5);
        splitPanes[1].setDividerSize(2);
        Client.frame.add(splitPanes[1], "Friends");
        
        // Searching Friend Requests
        JPanel searchRequests = new JPanel(new FlowLayout(FlowLayout.CENTER));
        searchRequests.setPreferredSize(new Dimension(requests.getWidth(), 35));
        searchRequests.add(new SearchBox(1, 15, 14));
        requests.add(searchRequests, BorderLayout.NORTH);
        
        // Display Friend Requests
        friendRequests = new JPanel();
        friendRequests.setLayout(new BoxLayout(friendRequests, BoxLayout.Y_AXIS));
        JScrollPane scrollWindow = new JScrollPane(friendRequests);
        scrollWindow.setBorder(null);
        scrollWindow.setPreferredSize(new Dimension(requests.getWidth(), requests.getHeight() - 70));
        requests.add(scrollWindow, BorderLayout.CENTER);
        
        // Searching for Friends
        JPanel searchFriends = new JPanel(new FlowLayout(FlowLayout.CENTER));
        searchFriends.setPreferredSize(new Dimension(suggestions.getWidth(), 35));
        searchFriends.add(new SearchBox(1, 15, 14));
        suggestions.add(searchFriends, BorderLayout.NORTH);
        
        // Display Searchable Friends
        users = new JPanel();
        users.setLayout(new BoxLayout(users, BoxLayout.Y_AXIS));
        userScroll = new JScrollPane(users);
        userScroll.setBorder(null);
        userScroll.setPreferredSize(new Dimension(suggestions.getWidth(), suggestions.getHeight() - 35));
        suggestions.add(userScroll, BorderLayout.CENTER);
        
        // Switching Panels
        JButton button = new JButton("Chats");
        button.setPreferredSize(new Dimension(requests.getWidth(), 35));
        button.addActionListener((ActionEvent e) -> Client.layout.show(Client.frame.getContentPane(), "Chat"));
        button.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) button.doClick();
             }
        });
        requests.add(button, BorderLayout.SOUTH);
    }
    
    private static class SendButton extends JButton {
        protected void paintComponent(Graphics g) {
            g.setColor(Color.GREEN);
            g.fillPolygon(new int[] {0, 0, getSize().height - 1}, new int[] {0, getSize().height - 1, getSize().height / 2}, 3);
        }
        protected void paintBorder(Graphics g) {}
    }
}
