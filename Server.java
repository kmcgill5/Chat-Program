import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.stream.Collectors;

class Server {
    private static ArrayList<Servlet> clients = new ArrayList<>();
    public static List<String> users = Collections.synchronizedList(new ArrayList<>());
    public static List<String> logins = Collections.synchronizedList(new ArrayList<>());
    private static List<String[]> messages = Collections.synchronizedList(new ArrayList<>());
    private static boolean running = true;
    private static Random rand = new Random();
    
    public static String register(String name, String password) {
        MessageDigest md = null;
        boolean found = false;
        try {
            md = MessageDigest.getInstance("SHA-256");
            for (int i = 0; i < logins.size(); i++)
                if (logins.get(i).equals(Base64.getEncoder().encodeToString(md.digest((name + password).getBytes()))))
                    found = true;
        } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }
        if (!found && users.contains(name))
            return "";
        else if (!found && !users.contains(name)) {
            try {
                if (md != null) {
                    Files.writeString(Paths.get("passwords.txt"), Base64.getEncoder().encodeToString(md.digest((name + password).getBytes())) + "\n", StandardOpenOption.APPEND);
                    logins.add(Base64.getEncoder().encodeToString(md.digest((name + password).getBytes())));
                }
            } catch (IOException e) { e.printStackTrace(); }
            users.add(name);
            try (PrintWriter pw = new PrintWriter(name + ".txt")) {
                pw.println("Requested:");
                pw.println("Requests:");
                pw.println("Friends:");
                pw.println("Chats:");
            } catch(IOException e) { e.printStackTrace(); }
        }
        return name;
    }
    public static boolean hasMessage(String name) {
        synchronized (messages) {
            if (messages.size() > 0)
                for (int i = 0; i < messages.size(); i++)
                    if (messages.get(i)[1].equals(name))
                        return true;
        }
        return false;
    }
    public static String getMessage(String name) {
        synchronized (messages) {
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i)[1].equals(name)) {
                    // Handle if message is in updates.txt
                    try {
                        List<String> updates = Files.readAllLines(Paths.get("updates.txt"));
                        for (int k = 0; k < updates.size(); k++) {
                            if (updates.get(k).equals(messages.get(k)[0] + " " + messages.get(k)[1] + " " + messages.get(k)[2])) {
                                updates.remove(k);
                                break;
                            }
                        }
                        Files.write(Paths.get("updates.txt"), updates);
                    } catch (IOException e) { e.printStackTrace(); }
                    
                    // Sends message
                    if (messages.get(i)[2].equals("USERS"))
                        return messages.remove(i)[2] + " " + String.join(" ", String.join(" ", users));
                    else if ("REQUEST ACCEPT REJECT".contains(messages.get(i)[2]))
                        return messages.get(i)[2] + " " + messages.remove(i)[0];
                    else
                        return messages.get(i)[0] + " " + messages.remove(i)[2];
                }
            }
        }
        return "";
    }
    public static void checkRequest(String client, String recipient) throws IOException {
        synchronized (messages) {
            if (messages.size() > 0) {
                for (int i = 0; i < messages.size(); i++) {
                    if (messages.get(i)[0].equals(recipient) && messages.get(i)[1].equals(client) && messages.get(i)[2].equals("REQUEST")) {
                        sendMessage(client, recipient, "ACCEPT");
                        sendMessage(recipient, client, "ACCEPT");
                        
                        // Handle if message is in updates.txt
                        try {
                            List<String> updates = Files.readAllLines(Paths.get("updates.txt"));
                            for (int k = 0; k < updates.size(); k++) {
                                if (updates.get(k).equals(recipient + " " + client + " REQUEST")) {
                                    updates.remove(k);
                                    break;
                                }
                            }
                            Files.write(Paths.get("updates.txt"), updates);
                        } catch (IOException e) { e.printStackTrace(); }
                        
                        messages.remove(i);
                        break;
                    }
                    else if (i + 1 == messages.size())
                        sendMessage(client, recipient, "REQUEST");
                }
            }
        }
    }
    public static void sendMessage(String client, String recipient, String message) throws IOException {
        synchronized (messages) {
            if (users.contains(recipient)) {
                List<String> sender = Files.readAllLines(Paths.get(client + ".txt"));
                List<String> receiver = Files.readAllLines(Paths.get(recipient + ".txt"));
                if (message.equals("REQUEST")) {
                    sender.set(0, sender.get(0) + " " + recipient);
                    receiver.set(1, receiver.get(1) + " " + client);
                }
                else if (message.equals("ACCEPT")) {
                    sender.set(1, sender.get(1).substring(0, sender.get(1).indexOf(recipient) - 1) + sender.get(1).substring(sender.get(1).indexOf(recipient) + recipient.length()));
                    sender.set(2, sender.get(2) + " " + recipient);
                    receiver.set(0, receiver.get(0).substring(0, receiver.get(0).indexOf(client) - 1) + receiver.get(0).substring(receiver.get(0).indexOf(client) + client.length()));
                    receiver.set(2, receiver.get(2) + " " + client);
                }
                else if (message.equals("REJECT")) {
                    sender.set(1, sender.get(1).substring(0, sender.get(1).indexOf(recipient) - 1) + sender.get(1).substring(sender.get(1).indexOf(recipient) + recipient.length()));
                    receiver.set(0, receiver.get(0).substring(0, receiver.get(0).indexOf(client) - 1) + receiver.get(0).substring(receiver.get(0).indexOf(client) + client.length()));
                }
                else if (!message.equals("USERS")) {
                    sender.add(client + " " + recipient + " " + message);
                    receiver.add(client + " " + message);
                }
                Files.write(Paths.get(client + ".txt"), sender);
                Files.write(Paths.get(recipient + ".txt"), receiver);
                
                messages.add(new String[] {client, recipient, message});
                
                // Handle if recipient is offline
                for (Servlet servlet : clients) {
                    if (servlet.name.equals(recipient) && servlet.client.isClosed()) {
                        List<String> updates = Files.readAllLines(Paths.get("updates.txt"));
                        updates.add(client + " " + recipient + " " + message);
                        Files.write(Paths.get("updates.txt"), updates);
                    }
                }
            }
        }
    }
    public static boolean accessing(ArrayList<Servlet> clients) {
        for (Servlet servlet : clients)
            if (!servlet.client.isClosed())
                return true;
        return false;
    }
    public static void main(String[] args) throws IOException {
        // Create Updates File
        if (!Files.exists(Paths.get("updates.txt")) || !Files.isRegularFile(Paths.get("updates.txt")))
            Files.createFile(Paths.get("updates.txt"));
        
        // Read in Current Users
        users = Files.list(Paths.get(".\\")).filter(Files::isRegularFile).map(Path::getFileName).map(Path::toString).filter(user -> user.endsWith(".txt")).filter(user -> !user.equals("passwords.txt") && !user.equals("updates.txt")).map(user -> user.substring(0, user.length() - 4)).collect(Collectors.toList());
        
        // Create Password File and Read Hashes
        if (!Files.exists(Paths.get("passwords.txt")) || !Files.isRegularFile(Paths.get("passwords.txt")))
            Files.createFile(Paths.get("passwords.txt"));
        else
            logins = Files.readAllLines(Paths.get("passwords.txt"));
        
        // Start Server
        ServerSocket ss = new ServerSocket(8080);
        new Thread(() -> {
            Scanner in = new Scanner(System.in);
            String line = "";
            while (line == null || !line.equalsIgnoreCase("exit") || accessing(clients)) {
                line = in.nextLine();
                if (line != null && line.equalsIgnoreCase("exit") && !accessing(clients)) {
                    running = false;
                    in.close();
                    try { ss.close(); }
                    catch (IOException e) { e.printStackTrace(); }
                }
            }
        }).start();
        try {
            while (running) {
                clients.add(new Servlet(ss.accept()));
                clients.getLast().start();
            }
        } catch (IOException e) {
            if (running)
                e.printStackTrace();
        }
    }
}