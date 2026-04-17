//for the events file
import java.io.File;
//raw data recieving
import java.io.InputStream;
//raw data sending
import java.io.OutputStream;
//socket endpoints
import java.net.Socket;
//current date and time
import java.time.LocalDate;
//dynamic lists
import java.util.ArrayList;
import java.util.List;
//text parsing
import java.util.Scanner;
//test
public class ServerForPi {

    public static void main(String[] args) {
        //Dynamic ip for pi...

        
        Scanner inputScanner = new Scanner(System.in);
        System.out.print("Enter Server IP [Press Enter for 127.0.0.1]: ");
        String serverIP = inputScanner.nextLine().trim();
        if (serverIP.isEmpty())     
        {
            serverIP = "127.0.0.1"; 
        }
        
        int port = 25; 
        String targetFile = "events.txt";

        //6 days
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(6);
        
        int targetDay = targetDate.getDayOfMonth();
        int targetMonth = targetDate.getMonthValue();
        
        System.out.println("\nSystem Date: " + today);
        System.out.println("Scanning for events on: " + targetDay + "/" + targetMonth);

        List<String> upcomingEvents = new ArrayList<>();

        // reading file
        try 
        {
            File file = new File(targetFile);
            if (!file.exists()) 
            {
                System.out.println("XXX Error: Could not find " + targetFile);
                inputScanner.close();
                return;
            }

            Scanner fileScanner = new Scanner(file);
            while (fileScanner.hasNextLine()) 
            {
                String line = fileScanner.nextLine().trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split(" ", 2);
                if (parts.length < 2) continue;
                
                String[] dateParts = parts[0].split("/");
                if (dateParts.length < 2) continue;
                
                try 
                {
                    int eventDay = Integer.parseInt(dateParts[0]);
                    int eventMonth = Integer.parseInt(dateParts[1]);
                    
                    if (eventDay == targetDay && eventMonth == targetMonth) 
                    {
                        upcomingEvents.add(parts[1]);
                    }
                } 
                catch (NumberFormatException e) 
                {}
            }
            fileScanner.close();

        } 
        catch (Exception e) 
        {
            System.out.println("File Error: " + e.getMessage());
            return;
        }
        
        if (upcomingEvents.isEmpty()) 
        {
            System.out.println("No events exactly 6 days away. Terminating.");
            inputScanner.close();
            return;
        }

        System.out.println("Found " + upcomingEvents.size() + " target event(s). Engaging SMTP Socket to " + serverIP + "...\n");

        //SMTP Section
        try {
            Socket socket = new Socket(serverIP, port);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            
            // Buffer for reading server responses
            byte[] buffer = new byte[2048];
            
            int bytesRead = in.read(buffer);
            System.out.print("Server: " + new String(buffer, 0, bytesRead));
            
            sendCommand(out, "HELO Puddle-Jumper", in, buffer);
            sendCommand(out, "MAIL FROM:<damian@puddle-jumper>", in, buffer);
            sendCommand(out, "RCPT TO:<root@localhost>", in, buffer); 
            sendCommand(out, "DATA", in, buffer);
            
            StringBuilder email = new StringBuilder();
            email.append("Subject: Upcoming Event Reminder!\r\n");
            email.append("From: ServerForPi <damian@puddle-jumper>\r\n");
            email.append("To: User <root@localhost>\r\n");
            email.append("\r\n"); 
            email.append("This is your automated 6-day early warning for the following events:\r\n\r\n");
            
            for (String event : upcomingEvents) 
            {
                email.append("- ").append(event).append("\r\n");
            }
            
            email.append("\r\n.\r\n"); 
            
            System.out.println("Clients: [Sending Payload...]");
            out.write(email.toString().getBytes());
            out.flush();
            
            bytesRead = in.read(buffer);
            if (bytesRead != -1) 
            {
                System.out.print("Server: " + new String(buffer, 0, bytesRead));
            }
            
            sendCommand(out, "QUIT", in, buffer);

            socket.close();
            System.out.println("\nSUCCESS: Early Warning Email Dispatched!");

        } 
        catch (Exception e) 
        {
            System.out.println("\nXXX SMTP Network Error: " + e.getMessage());
        }
        
        inputScanner.close();
    }

    // Helper method to send SMTP commands and read responses
    private static void sendCommand(OutputStream out, String command, InputStream in, byte[] buffer) throws Exception 
    {
        System.out.println("Clients: " + command);
        out.write((command + "\r\n").getBytes());
        out.flush();
        
        Thread.sleep(150); 
        int bytesRead = in.read(buffer);
        
        if (bytesRead == -1) 
        {
            throw new Exception("Server severed the connection.");
        }
        
        System.out.print("Server: " + new String(buffer, 0, bytesRead));
    }
}