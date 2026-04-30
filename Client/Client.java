import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

class Client {
    public static String name;
    public static String recipient = "";
    public static volatile boolean running = true;
    public static Socket ss;
    public static OutputStream os;
    public static InputStream is;
    public static PrintWriter pw;
    public static BufferedReader br;
    public static JFrame frame = new JFrame("Chat");
    public static CardLayout layout = new CardLayout();
    
    public static void main(String[] args) throws IOException {
        // Establishing Connection
        try {
            ss = new Socket("localhost", 8080);
            os = ss.getOutputStream();
            is = ss.getInputStream();
            pw = new PrintWriter(ss.getOutputStream(), true);
            br = new BufferedReader(new InputStreamReader(ss.getInputStream()));
        } catch (ConnectException e) { }
        
        // Input Loop Thread
        new Thread(() -> {
            try { while (running && ss != null)
                if (frame.isShowing())
                    ServerCom.getData();
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
        
        // Setting up GUI
        SwingUtilities.invokeLater(() -> {
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setLayout(layout);
            
            // Exiting the JFrame
            WindowAdapter adapter = new WindowAdapter() {
                public void windowClosing(WindowEvent event) {
                    running = false;
                    frame.dispose();
                    // Closing Connections
                    if (ss != null) {
                        pw.println("exit");
                        try {
                            br.close();
                            pw.close();
                            ss.close();
                        } catch(IOException e) {}
                    }
                }
            };
            frame.addWindowListener(adapter);
            
            // Login
            login();
            if (!running) {
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                return;
            }
            frame.setVisible(true);
            
            // Setting Up Resizeable Split Panes
            GUI.chatPanel();
            GUI.friendsPanel();
            frame.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    for (int i = 0; i < GUI.splitPanes.length; i++) {
                        GUI.splitPanes[i].getLeftComponent().setMinimumSize(new Dimension(frame.getContentPane().getWidth() / 3, 0));
                        GUI.splitPanes[i].getRightComponent().setMinimumSize(new Dimension(frame.getContentPane().getWidth() / 3, 0));
                    }
                }
            });
            frame.revalidate();
            frame.repaint();
            
            // Sending Updates to Server
            if (ss != null) {
                if (Files.exists(Paths.get(name + "_updates.txt")) && Files.isRegularFile(Paths.get(name + "_updates.txt"))) {
                    try {
                        java.util.List<String> lines = Files.readAllLines(Paths.get(name + "_updates.txt"));
                        while (lines.size() > 0)
                            pw.println(lines.remove(0));
                        Files.delete(Paths.get(name + "_updates.txt"));
                        pw.println("END");
                    } catch (IOException e) { e.printStackTrace(); }
                }
                else
                    pw.println("END");
            }
            
            // Setting Up Components When Offline
            if (ss == null) {
                try {
                    java.util.List<String> file = Files.readAllLines(Paths.get(name + ".txt"));
                    if (file.get(0).indexOf(' ') > -1)
                        ServerCom.users("USERS " + file.get(0).substring(file.get(0).indexOf(' ') + 1), false);
                    if (file.get(1).indexOf(' ') > -1)
                        ServerCom.users("USERS " + file.get(1).substring(file.get(1).indexOf(' ') + 1), false);
                    if (file.get(2).indexOf(' ') > -1)
                        for (String user : Arrays.asList(file.get(2).substring(file.get(2).indexOf(' ') + 1).split(" ")))
                            ServerCom.request("REQUEST " + user, false);
                    if (file.get(3).indexOf(' ') > -1)
                        for (String user : Arrays.asList(file.get(3).substring(file.get(3).indexOf(' ') + 1).split(" ")))
                            ServerCom.accept("ACCEPT " + user, false);
                } catch (IOException e) { e.printStackTrace(); }
            }
        });
    }
    
    private static void login() {
        // Setup Stuff
        JDialog dialog = new JDialog(frame, "Login", true);
        dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        dialog.setLayout(new FlowLayout());
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(null);
        dialog.setResizable(false);
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                running = false;
                dialog.dispose();
            }
        });
        
        // Username
        dialog.add(new JLabel("Username"));
        JTextArea username = new JTextArea(1, 25);
        ((AbstractDocument)username.getDocument()).setDocumentFilter(new SearchBox.DocumentLengthFilter(14));
        SearchBox.disableTab(username);
        dialog.add(username);
        
        // Password
        dialog.add(new JLabel("Password"));
        JPasswordField password = new JPasswordField(25);
        ((AbstractDocument)password.getDocument()).setDocumentFilter(new SearchBox.DocumentLengthFilter(14));
        SearchBox.disableTab(password);
        password.setBorder(null);
        dialog.add(password);
        
        // Incorrect Login
        JLabel label = new JLabel("username or password is incorrect");
        label.setForeground(Color.RED);
        
        // Submit Button
        JButton submit = new JButton("Log In");
        submit.setPreferredSize(new Dimension(dialog.getWidth() / 2, 30));
        submit.addActionListener((event) -> {
            if (pw != null) {
                pw.println(username.getText());
                pw.println(new String(password.getPassword()));
            }
            try {
                MessageDigest md = null;
                try { md = MessageDigest.getInstance("SHA-256"); }
                catch (NoSuchAlgorithmException f) { }
                if (br == null && Files.exists(Paths.get("passwords.txt")) && Files.isRegularFile(Paths.get("passwords.txt"))) {
                    java.util.List<String> passwords = Files.readAllLines(Paths.get("passwords.txt"));
                    if (md != null) {
                        for (String hash : passwords) {
                            if (hash.equals(Base64.getEncoder().encodeToString(md.digest((username.getText() + new String(password.getPassword())).getBytes())))) {
                                name = username.getText();
                                dialog.dispose();
                            }
                        }
                    }
                    if (label.getParent() == null) {
                        dialog.add(label);
                        dialog.revalidate();
                        dialog.repaint();
                    }
                }
                else if (br != null && (name = br.readLine()).equals(username.getText())) {
                    boolean found = false;
                    dialog.dispose();
                    if (!Files.exists(Paths.get(name + ".txt")) || !Files.isRegularFile(Paths.get(name + ".txt")))
                        Files.write(Paths.get(name + ".txt"), java.util.List.of("Users:", "Requested:", "Requests:", "Friends:", "Chats:"), StandardOpenOption.CREATE);
                    if (md != null && Files.exists(Paths.get("passwords.txt")) && Files.isRegularFile(Paths.get("passwords.txt")))
                        for (String hash : Files.readAllLines(Paths.get("passwords.txt")))
                            if (hash.equals(Base64.getEncoder().encodeToString(md.digest((name + new String(password.getPassword())).getBytes()))))
                                found = true;
                    if (!found)
                        Files.writeString(Paths.get("passwords.txt"), Base64.getEncoder().encodeToString(md.digest((name + new String(password.getPassword())).getBytes())) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
                else if (label.getParent() == null) {
                    dialog.add(label);
                    dialog.revalidate();
                    dialog.repaint();
                }
            } catch (IOException e) { e.printStackTrace(); }
        });
        dialog.add(submit);
        
        // Finished
        dialog.setVisible(true);
    }
}
