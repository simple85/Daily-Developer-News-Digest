import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    private static final String DEV_TO_API = "https://dev.to/api/articles?top=1";
    private static final String HN_API = "https://hacker-news.firebaseio.com/v0/topstories.json";

    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable dailyTask = Main::sendDailyDigest;
        long initialDelay = computeInitialDelay();
        long period = TimeUnit.DAYS.toSeconds(1);

        scheduler.scheduleAtFixedRate(dailyTask, initialDelay, period, TimeUnit.SECONDS);
    }

    private static long computeInitialDelay() {
        return 5; // For testing, run after 5 seconds. Replace with real-time scheduling.
    }

    public static void sendDailyDigest() {
        String devToNews = fetchDevToNews();
        String hnNews = fetchHackerNews();

        StringBuilder emailBody = new StringBuilder();
        emailBody.append("<h1>Daily Developer Digest - ").append(LocalDate.now()).append("</h1>");
        emailBody.append("<h2>Dev.to:</h2>").append(devToNews);
        emailBody.append("<h2>Hacker News:</h2>").append(hnNews);

        sendEmail("email to send to", "Daily Developer Digest", emailBody.toString());
    }

    private static String fetchDevToNews() {
        try {
            URL url = new URL(DEV_TO_API);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            return "<p>" + response.substring(0, Math.min(response.length(), 500)) + "...</p>";
        } catch (IOException e) {
            return "<p>Error fetching Dev.to news</p>";
        }
    }

    private static String fetchHackerNews() {
        try {
            URL url = new URL(HN_API);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            return "<p>Top story IDs: " + response.substring(0, 100) + "...</p>";
        } catch (IOException e) {
            return "<p>Error fetching Hacker News</p>";
        }
    }

    private static void sendEmail(String to, String subject, String body) {
        final String username = "your email";
        final String password = "your app pass key";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(body, "text/html");

            Transport.send(message);
            System.out.println("Digest sent successfully!");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
