import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

class ServerCom {
    private static boolean first = true;
    
    public static void getData() throws IOException {
        try {
            while (GUI.splitPanes[1] == null) Thread.sleep(500);
        } catch (InterruptedException e) {}
        
        String input = Client.br.readLine();
        
        if (input.indexOf(" ") != -1 && input.substring(0, input.indexOf(' ')).equals("USERS")) {
            users(input, true);
            if (first) {
                postFileData();
                first = false;
            }
        }
        else if (input.indexOf(' ') != -1 && input.substring(0, input.indexOf(' ')).equals("REQUEST"))
            request(input, true);
        else if (input.indexOf(' ') != -1 && input.substring(0, input.indexOf(' ')).equals("ACCEPT"))
            accept(input, true);
        else if (input.indexOf(' ') != -1 && input.substring(0, input.indexOf(' ')).equals("REJECT"))
            reject(input, true);
        else if (input.indexOf(' ') != -1  && input.indexOf(' ', input.indexOf(' ') + 1) != -1 && input.substring(input.indexOf(' ') + 1, input.indexOf(' ', input.indexOf(' ') + 1)).equals("FILE"))
            file(input, true);
        else if (input != null && input.contains(" "))
            message(input, true);
        
        Client.frame.revalidate();
        Client.frame.repaint();
    }
    
    public static void postFileData() {
        try {
            java.util.List<String> lines = Files.readAllLines(Paths.get(Client.name + ".txt"));
            if (lines.get(2).contains(" "))
                for (String user : Arrays.asList(lines.get(2).substring(lines.get(2).indexOf(' ') + 1).split(" ")))
                    request("REQUEST " + user, false);
            if (lines.get(3).contains(" "))
                for (String user : Arrays.asList(lines.get(3).substring(lines.get(3).indexOf(' ') + 1).split(" ")))
                    accept("ACCEPT " + user, false);
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    public static void users(String input, boolean write) {
        // Get List of Current Known Users
        ArrayList<String> users = new ArrayList<>();
        for (JPanel panel : Arrays.stream(GUI.users.getComponents()).toArray(JPanel[]::new))
            for (Component comp : panel.getComponents())
                if (comp instanceof JLabel)
                    users.add(((JLabel)comp).getText());
        try {
            java.util.List<String> file = Files.readAllLines(Paths.get(Client.name + ".txt"));
            users.addAll(Arrays.asList(file.get(2).substring(file.get(2).indexOf(' ') + 1).split(" ")));
            users.addAll(Arrays.asList(file.get(3).substring(file.get(3).indexOf(' ') + 1).split(" ")));
        }
        catch (IOException e) { e.printStackTrace(); }
        
        // Add Unknown Users
        for (String user : input.substring(input.indexOf(' ') + 1).split("\\s+")) {
            if (!user.equals(Client.name) && !users.contains(user)) {
                JPanel profile = new JPanel(new BorderLayout());
                profile.setPreferredSize(new Dimension(GUI.userScroll.getViewport().getWidth() - 15, 30));
                profile.setMaximumSize(new Dimension(GUI.userScroll.getViewport().getWidth() - 15, 30));
                profile.add(new JLabel(user), BorderLayout.WEST);
                JButton request = new JButton("Add Friend");
                request.addActionListener((ActionEvent event) -> {
                    if (Client.pw != null)
                        Client.pw.println("REQUEST " + user);
                    request.setText("Requested");
                    request.setEnabled(false);
                    
                    // Edit File
                    try {
                        java.util.List<String> lines = Files.readAllLines(Paths.get(Client.name + ".txt"));
                        lines.set(0, lines.get(0).substring(0, lines.get(0).indexOf(user) - 1) + lines.get(0).substring(lines.get(0).indexOf(user) + user.length()));
                        lines.set(1, lines.get(1) + " " + user);
                        Files.write(Paths.get(Client.name + ".txt"), lines);
                    } catch (IOException e) { e.printStackTrace(); }
                    
                    // Updates File if Necessary
                    if (Client.ss == null) {
                        try {
                            Files.writeString(Paths.get(Client.name + "_updates.txt"), "REQUEST " + user + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e) { e.printStackTrace(); }
                    }
                });
                try {
                    if (Files.readAllLines(Paths.get(Client.name + ".txt")).get(1).contains(user)) {
                        request.setEnabled(false);
                        request.setText("Requested");
                    }
                } catch (IOException e) { }
                profile.add(request, BorderLayout.EAST);
                GUI.users.add(profile);
                Client.frame.revalidate();
                Client.frame.repaint();
        
                // Edit File
                if (write) {
                    try {
                        java.util.List<String> lines = Files.readAllLines(Paths.get(Client.name + ".txt"));
                        if (!lines.get(1).contains(user))
                            lines.set(0, lines.get(0) + " " + user);
                        Files.write(Paths.get(Client.name + ".txt"), lines);
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }
        }
    }
    
    public static void request(String input, boolean write) {
        // Remove User from Users List
        if (write)
            for (JPanel panel : Arrays.stream(GUI.users.getComponents()).filter(c -> c instanceof JPanel).toArray(JPanel[]::new))
                for (int i = 0; i < panel.getComponents().length; i++)
                    if (panel.getComponents()[i] instanceof JLabel && ((JLabel) panel.getComponents()[i]).getText().equals(input.substring(input.indexOf(' ') + 1).split("\\s+")[0]))
                        GUI.users.remove(panel);
        
        // Add User to Friend Requests List
        JPanel profile = new JPanel(new BorderLayout());
        profile.setPreferredSize(new Dimension(GUI.splitPanes[1].getRightComponent().getWidth() - 15, 30));
        profile.setMaximumSize(new Dimension(GUI.splitPanes[1].getRightComponent().getWidth() - 15, 30));
        String user = input.substring(input.indexOf(' ') + 1);
        profile.add(new JLabel(user), BorderLayout.WEST);
        
        // Add User to Friend List if Accepted
        JButton accept = new JButton("Accept");
        accept.addActionListener((ActionEvent e) -> {
            if (Client.pw != null)
                Client.pw.println("ACCEPT " + user);
            GUI.friendRequests.remove(profile);
            JButton button = new JButton(user);
            button.setPreferredSize(new Dimension(GUI.friendList.getWidth() - 15, 50));
            button.setMaximumSize(new Dimension(GUI.friendList.getWidth() - 15, 50));
            button.addActionListener((ActionEvent event) -> {
                if (Client.ss != null)
                    GUI.files.setEnabled(true);
                Client.recipient = user;
                button.setBackground(null);
                GUI.history.removeAll();
                GUI.history.revalidate();
                GUI.history.repaint();
                java.util.List<String> messages = null;
                try {
                    messages = Files.readAllLines(Paths.get(Client.name + ".txt"));
                } catch (IOException f) { messages = new ArrayList<>(); }
                for (int i = 0; i < 5; i++)
                    messages.remove(0);
                for (String message : messages) {
                    if (message.substring(0, message.indexOf(' ')).equals(Client.recipient) || message.indexOf(' ', message.indexOf(' ') + 1) > -1 && message.substring(0, message.indexOf(' ', message.indexOf(' ') + 1)).equals(Client.name + " " + Client.recipient)) {
                        JPanel back = new JPanel();
                        SearchBox thing = new SearchBox(1, 30);
                        thing.setText(message.substring(message.indexOf(' ') + 1).replaceAll("\\\\n", "\n"));
                        thing.setEditable(false);
                        if (message.substring(0, message.indexOf(' ')).equals(Client.name)) {
                            thing.setBackground(Color.BLUE);
                            thing.setText(thing.getText().substring(thing.getText().indexOf(' ') + 1));
                        }
                        back.add(thing);
                        GUI.history.add(back);
                    }
                }
                Client.frame.revalidate();
                Client.frame.repaint();
            });
            GUI.friendList.add(button);
            Client.frame.revalidate();
            Client.frame.repaint();
            
            // Edit File
            try {
                java.util.List<String> lines = Files.readAllLines(Paths.get(Client.name + ".txt"));
                lines.set(2, lines.get(2).substring(0, lines.get(2).indexOf(user) - 1) + lines.get(2).substring(lines.get(2).indexOf(user) + user.length()));
                lines.set(3, lines.get(3) + " " + user);
                Files.write(Paths.get(Client.name + ".txt"), lines);
            } catch (IOException f) { f.printStackTrace(); }
            
            // Updates File if Necessary
            if (Client.ss == null) {
                try {
                    Files.writeString(Paths.get(Client.name + "_updates.txt"), "ACCEPT " + user + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException f) { f.printStackTrace(); }
            }
        });
        
        // Add User to Users List if not Accepted
        JButton reject = new JButton("Reject");
        reject.addActionListener((ActionEvent event) -> {
            if (Client.pw != null) {
                Client.pw.println("REJECT " + user);
                Client.pw.println("USERS");
            }
            else
                users("USERS " + user, false);
            GUI.friendRequests.remove(profile);
            
            // Edit File
            try {
                java.util.List<String> lines = Files.readAllLines(Paths.get(Client.name + ".txt"));
                lines.set(0, lines.get(0) + " " + user);
                lines.set(2, lines.get(2).substring(0, lines.get(2).indexOf(user) - 1) + lines.get(2).substring(lines.get(2).indexOf(user) + user.length()));
                Files.write(Paths.get(Client.name + ".txt"), lines);
            } catch (IOException e) { e.printStackTrace(); }
            
            // Updates File if Necessary
            if (Client.ss == null) {
                try {
                    Files.writeString(Paths.get(Client.name + "_updates.txt"), "REJECT " + user + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) { e.printStackTrace(); }
            }
        });
        
        JPanel temp = new JPanel();
        temp.add(accept);
        temp.add(reject);
        profile.add(temp, BorderLayout.EAST);
        GUI.friendRequests.add(profile);
        Client.frame.revalidate();
        Client.frame.repaint();
        
        // Edit File
        if (write) {
            try {
                java.util.List<String> lines = Files.readAllLines(Paths.get(Client.name + ".txt"));
                lines.set(0, lines.get(0).substring(0, lines.get(0).indexOf(user) - 1) + lines.get(0).substring(lines.get(0).indexOf(user) + user.length()));
                lines.set(2, lines.get(2) + input.substring(input.indexOf(' ')));
                Files.write(Paths.get(Client.name + ".txt"), lines);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
    public static void accept(String input, boolean write) {
        // Remove User from Users List
        if (write)
            for (JPanel panel : Arrays.stream(GUI.users.getComponents()).filter(c -> c instanceof JPanel).toArray(JPanel[]::new))
                for (int i = 0; i < panel.getComponents().length; i++)
                    if (panel.getComponent(i) instanceof JLabel && ((JLabel)panel.getComponent(i)).getText().equals(input.substring(input.indexOf(' ') + 1).split("\\s+")[0]))
                        GUI.users.remove(panel);
        
        // Add User to Friend List
        JButton profile = new JButton(input.substring(input.indexOf(' ') + 1));
        profile.setPreferredSize(new Dimension(GUI.friendList.getWidth() - 15, 50));
        profile.setMaximumSize(new Dimension(GUI.friendList.getWidth() - 15, 50));
        profile.addActionListener((ActionEvent event) -> {
            if (Client.ss != null)
                GUI.files.setEnabled(true);
            Client.recipient = profile.getText();
            profile.setBackground(null);
            GUI.history.removeAll();
            GUI.history.revalidate();
            GUI.history.repaint();
            java.util.List<String> messages = null;
            try {
                messages = Files.readAllLines(Paths.get(Client.name + ".txt"));
            } catch (IOException e) { messages = new ArrayList<>(); }
            for (int i = 0; i < 5; i++)
                messages.remove(0);
            for (String message : messages) {
                if (message.substring(0, message.indexOf(' ')).equals(Client.recipient) || message.indexOf(' ', message.indexOf(' ') + 1) > -1 && message.substring(0, message.indexOf(' ', message.indexOf(' ') + 1)).equals(Client.name + " " + Client.recipient)) {
                    JPanel back = new JPanel();
                    SearchBox thing = new SearchBox(1, 30);
                    thing.setText(message.substring(message.indexOf(' ') + 1).replaceAll("\\\\n", "\n"));
                    thing.setEditable(false);
                    if (message.substring(0, message.indexOf(' ')).equals(Client.name)) {
                        thing.setBackground(Color.BLUE);
                        thing.setText(thing.getText().substring(thing.getText().indexOf(' ') + 1));
                    }
                    back.add(thing);
                    GUI.history.add(back);
                }
            }
            Client.frame.revalidate();
            Client.frame.repaint();
        });
        GUI.friendList.add(profile);
        Client.frame.revalidate();
        Client.frame.repaint();
        
        // Edit File
        if (write) {
            try {
                java.util.List<String> lines = Files.readAllLines(Paths.get(Client.name + ".txt"));
                lines.set(1, lines.get(1).substring(0, lines.get(1).indexOf(input.substring(input.indexOf(' ') + 1))) + lines.get(1).substring(lines.get(1).indexOf(input.substring(input.indexOf(' ') + 1)) + input.substring(input.indexOf(' ') + 1).length()));
                lines.set(3, lines.get(3) + input.substring(input.indexOf(' ')));
                Files.write(Paths.get(Client.name + ".txt"), lines);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
    
    public static void reject(String input, boolean write) {
        for (JPanel panel : Arrays.stream(GUI.users.getComponents()).filter(c -> c instanceof JPanel).toArray(JPanel[]::new)) {
            for (int i = 0; i < panel.getComponents().length; i++) {
                if (panel.getComponent(i) instanceof JLabel && ((JLabel)panel.getComponent(i)).getText().equals(input.substring(input.indexOf(' ') + 1).split("\\s+")[0])) {
                    ((JButton)panel.getComponent(i + 1)).setEnabled(true);
                    ((JButton)panel.getComponent(i + 1)).setText("Add Friend");
                }
            }
        }
        
        // Edit File
        if (write) {
            try {
                java.util.List<String> lines = Files.readAllLines(Paths.get(Client.name + ".txt"));
                lines.set(0, lines.get(0) + " " + input.substring(input.indexOf(' ') + 1));
                lines.set(1, lines.get(1).substring(0, lines.get(1).indexOf(input.substring(input.indexOf(' ') + 1)) - 1) + lines.get(1).substring(lines.get(1).indexOf(input.substring(input.indexOf(' ') + 1)) + input.substring(input.indexOf(' ') + 1).length()));
                Files.write(Paths.get(Client.name + ".txt"), lines);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
    
    public static void file(String input, boolean write) throws IOException {
        int remaining = Integer.parseInt(Client.br.readLine());
        String file = Client.br.readLine();
        byte[] buffer = new byte[4096];
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            int read;
            
            while (remaining > 0 && (read = Client.is.read(buffer, 0, Math.min(buffer.length, remaining))) != -1) {
                fos.write(buffer, 0, read);
                remaining -= read;
            }
        } catch (IOException e) { e.printStackTrace(); }
        
        message(input, write);
    }
    
    public static void message(String input, boolean write) {
        if (!input.substring(0, input.indexOf(' ')).equals(Client.recipient)) {
            for (Component chat : GUI.friendList.getComponents())
                if (((JButton)chat).getText().equals(input.substring(0, input.indexOf(' '))))
                    chat.setBackground(Color.MAGENTA);
        }
        else {
            JPanel message = new JPanel(new FlowLayout(FlowLayout.LEFT));
            SearchBox area = new SearchBox(1, 30);
            area.setText(input.substring(input.indexOf(' ') + 1).replaceAll("\\\\n", "\n"));
            if (area.getText().contains("FILE"))
                area.setText(area.getText().substring(area.getText().indexOf(' ') + 1));
            area.setEditable(false);
            message.add(area);
            GUI.history.add(message);
            GUI.history.revalidate();
        }
        
        // Edit File
        if (write) {
            try {
                java.util.List<String> lines = Files.readAllLines(Paths.get(Client.name + ".txt"));
                lines.add(input);
                Files.write(Paths.get(Client.name + ".txt"), lines);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}
