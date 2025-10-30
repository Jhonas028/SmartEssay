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

// Sends OTP emails using Bulk Email and OTP Email API (RapidAPI)
public class RapidEmailAPI {

    private static final OkHttpClient client = new OkHttpClient();

    public static void sendOtpEmail(String otp, String toEmail) throws JSONException {
        MediaType mediaType = MediaType.parse("application/json");

        JSONObject json = new JSONObject();
        json.put("subject", "SmartEssay OTP Verification");
        json.put("from", "eczstudytool@gmail.com");
        json.put("to", toEmail);
        json.put("smtp_server", "smtp.gmail.com");
        json.put("smtp_port", "587");
        json.put("smtp_username", "eczstudytool@gmail.com");
        json.put("smtp_password", "vmbnwzadiuzsanpi"); // ⚠️ Use secure storage or backend proxy
        json.put("body", "Your SmartEssay OTP is: " + otp + "\n\nDo not share this code with anyone.");

        String jsonString = json.toString();
        Log.i("RapidMail Request", jsonString);

        RequestBody requestBody = RequestBody.create(mediaType, jsonString);

        Request request = new Request.Builder()
                .url("https://bulk-email-and-otp-email-api.p.rapidapi.com/api/send/otp")
                .post(requestBody)
                .addHeader("x-rapidapi-key", "5d6b0c84c3msh8935cfeb2995b5fp15496djsnac76059af8ce")
                .addHeader("x-rapidapi-host", "bulk-email-and-otp-email-api.p.rapidapi.com")
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("RapidMail", "Failed to send OTP email", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resBody = response.body().string();
                if (response.isSuccessful()) {
                    Log.d("RapidMail", "OTP email sent successfully: " + resBody);
                } else {
                    Log.e("RapidMail", "Error sending OTP email: " + response.code() + " - " + resBody);
                }
            }
        });
    }
}
