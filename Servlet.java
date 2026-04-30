import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Collections;

class Servlet extends Thread {
    public Socket client;
    public String name;
    private OutputStream os;
    private InputStream is;
    private PrintWriter pw;
    private BufferedReader br;
    private boolean running = true;
    
    Servlet(Socket client) throws IOException {
        this.client = client;
        name = "";
        os = client.getOutputStream();
        is = client.getInputStream();
        pw = new PrintWriter(client.getOutputStream(), true);
        br = new BufferedReader(new InputStreamReader(client.getInputStream()));
    }
    
    @Override
    public void run() {
        try {
            // Register Name
            while (name.equals("")) {
                name = br.readLine();
                if (!name.equals("exit"))
                    pw.println((name = Server.register(name, br.readLine())));
            }
            // Get Updates from Client
            String input = "";
            while ((input = br.readLine()) == null || !input.equals("END")) {
                if (input == null)
                    input = "";
                else if (input.indexOf(' ') > -1 && input.substring(0, input.indexOf(' ')).equals("REQUEST"))
                    Server.checkRequest(name, input.substring(input.indexOf(' ') + 1));
                else if (input.indexOf(' ') > -1 && input.substring(0, input.indexOf(' ')).equals("ACCEPT"))
                    Server.sendMessage(name, input.substring(input.indexOf(' ') + 1), "ACCEPT");
                else if (input.indexOf(' ') > -1 && input.substring(0, input.indexOf(' ')).equals("REJECT"))
                    Server.sendMessage(name, input.substring(input.indexOf(' ') + 1), "REJECT");
                else if (input.indexOf(' ') > 0 && !input.equals("exit"))
                    Server.sendMessage(name, input.substring(0, input.indexOf(' ')), input.substring(input.indexOf(' ') + 1));
            }
            // Sending Server Updates
            new Thread(() -> {
                // Variables and Initial Updates
                int users = 0;
                
                while (running) {
                    if (Server.hasMessage(name)) {   // Messages
                        String message = Server.getMessage(name);
                        pw.println(message);
                        if (message.indexOf(' ', message.indexOf(' ') + 1) > -1 && message.substring(message.indexOf(' ') + 1, message.indexOf(' ', message.indexOf(' ') + 1)).equals("FILE"))
                            sendFile(message);
                    }
                    else if (users < Server.users.size()) {
                        pw.println("USERS " + String.join(" ", Server.users));
                        users = Server.users.size();
                    }
                }
            }).start();
            // Client Input
            while (!input.equals("exit")) {
                if ((input = br.readLine()) == null)
                    input = " ";
                else if (input.equals("USERS"))
                    Server.sendMessage(name, name, "USERS");
                else if (input.indexOf(' ') > -1 && input.substring(0, input.indexOf(' ')).equals("REQUEST"))
                    Server.sendMessage(name, input.substring(input.indexOf(' ') + 1), "REQUEST");
                else if (input.indexOf(' ') > -1 && input.substring(0, input.indexOf(' ')).equals("ACCEPT"))
                    Server.sendMessage(name, input.substring(input.indexOf(' ') + 1), "ACCEPT");
                else if (input.indexOf(' ') > -1 && input.substring(0, input.indexOf(' ')).equals("REJECT"))
                    Server.sendMessage(name, input.substring(input.indexOf(' ') + 1), "REJECT");
                else if (input.indexOf(' ') > -1 && input.substring(0, input.indexOf(' ')).equals("FILE"))
                    retrieveFile(input.split(" ", 4)[1], Integer.parseInt(input.split(" ", 4)[2]), input.split(" ", 4)[3]);
                else if (input.indexOf(' ') > 0 && !input.equals("exit"))
                    Server.sendMessage(name, input.substring(0, input.indexOf(' ')), input.substring(input.indexOf(' ') + 1));
            }
            pw.println();
            running = false;
            pw.close();
            br.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void retrieveFile(String recipient, int remaining, String file) {
        byte[] buffer = new byte[4096];
        
        try (FileOutputStream fos = new FileOutputStream(recipient + "_" + file)) {
            int read;
            
            while (remaining > 0 && (read = is.read(buffer, 0, Math.min(buffer.length, remaining))) != -1) {
                fos.write(buffer, 0, read);
                remaining -= read;
            }
            
            Server.sendMessage(name, recipient, "FILE " + file);
        } catch (IOException e) { e.printStackTrace(); }
    }
    public void sendFile(String from) {
        String file = "";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("."), name + "*")) {
            for (Path entry : stream)
                if (!entry.getFileName().equals(".\\" + name + ".txt"))
                    file = entry.toString();
        } catch (IOException e) { e.printStackTrace(); }
        byte[] buffer = new byte[4096];
        int read;
        
        pw.println((new File(file)).length());
        pw.println(file.substring(file.indexOf('_') + 1));
        
        try (FileInputStream fis = new FileInputStream(file)) {
            while ((read = fis.read(buffer)) != -1)
                os.write(buffer, 0, read);
            os.flush();
        } catch (IOException e) { e.printStackTrace(); }
    }
}