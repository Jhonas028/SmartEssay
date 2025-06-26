package com.example.smartessay.API;

import android.util.Base64;
import android.util.Log;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MailGunEmail {

    public static void sendOtpEmail(String otp, String recipientEmail) {
        new Thread(() -> {
            try {
                String apiKey = "api:2c593ed9c4e235ce02852844d7e7efc2-a1dad75f-dca5493d"; // Replace with your actual key
                String credentials = Base64.encodeToString(apiKey.getBytes(), Base64.NO_WRAP);

                URL url = new URL("https://api.mailgun.net/v3/sandboxec25d2db45324e6ca3e44d90a1c73bea.mailgun.org/messages");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Authorization", "Basic " + credentials);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "from=Mailgun Sandbox <postmaster@sandboxec25d2db45324e6ca3e44d90a1c73bea.mailgun.org>" +
                        "&to=" + recipientEmail +
                        "&subject=Your OTP Code" +
                        "&text=Your verification code is: " + otp;

                OutputStream os = connection.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                int responseCode = connection.getResponseCode();
                Log.d("Mailgun", "Response Code: " + responseCode);
            } catch (Exception e) {
                Log.e("Mailgun", "Failed to send email", e);
            }
        }).start();
    }
}
