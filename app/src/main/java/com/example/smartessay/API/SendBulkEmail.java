//jhonas API
package com.example.smartessay.API;

import android.util.Log;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

// 5 request per month (RapidAPI free tier)
public class SendBulkEmail {

    private static final OkHttpClient client = new OkHttpClient();

    public static void sendOtpEmail(String otp, String toEmail) throws JSONException {
        MediaType mediaType = MediaType.parse("application/json");

        String subject = "Account OTP";
        String senderEmail = "gateway.smtp587@gmail.com";
        String senderName = "Company Name";
        String body = "Hello! Your OTP is: " + otp + "\nPlease use it to continue signing in.";

        JSONObject json = new JSONObject();
        json.put("subject", subject);
        json.put("from", senderEmail);
        json.put("to", toEmail);
        json.put("senders_name", senderName);
        json.put("body", body);

        String jsonString = json.toString();

        Log.i("json request: ", jsonString);

        RequestBody requestBody = RequestBody.create(mediaType, jsonString);

        Log.i("requestBody: ", requestBody.toString());

        Request request = new Request.Builder()
                .url("https://send-bulk-emails.p.rapidapi.com/api/send/otp/mail")
                .post(requestBody)
                .addHeader("x-rapidapi-key", "5d6b0c84c3msh8935cfeb2995b5fp15496djsnac76059af8ce")
                .addHeader("x-rapidapi-host", "send-bulk-emails.p.rapidapi.com")
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SendBulkEmail", "Failed to send OTP email", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("SendBulkEmail", "Email sent successfully: " + response.body().string());
                } else {
                    Log.e("SendBulkEmail", "Error sending email: " + response.code());
                }
            }
        });
    }
}
