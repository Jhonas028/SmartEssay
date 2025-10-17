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

//5 request per month.

public class EmailAPI {

    private static final OkHttpClient client = new OkHttpClient();
    public static void sendOtpEmail(String otp, String toEmail) throws JSONException {
        MediaType mediaType = MediaType.parse("application/json");

        String subject = "Your OTP Code";
        String body = "Hello! Your OTP is: " + otp + "\nPlease use it to continue signing in.";

        JSONObject json = new JSONObject();
        json.put("sendTo", toEmail);
        json.put("replyTo", "no-reply@smartessay.com");
        json.put("isHtml", false);
        json.put("title", subject);
        json.put("body", body);

        String jsonString = json.toString();

        Log.i("json request: ", jsonString);

        RequestBody requestBody = RequestBody.create(mediaType, jsonString);
        Log.i("requestBody: ", requestBody.toString());

        Request request = new Request.Builder()
                .url("https://sendmail-ultimate-email-sender.p.rapidapi.com/send-email")
                .post(requestBody)
                .addHeader("x-rapidapi-key", "5d6b0c84c3msh8935cfeb2995b5fp15496djsnac76059af8ce")
                .addHeader("x-rapidapi-host", "sendmail-ultimate-email-sender.p.rapidapi.com")
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("OTP Email", "Failed to send OTP email", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("OTP Email", "Email sent: " + response.body().string());
                } else {
                    Log.e("OTP Email", "Error: " + response.code());
                }
            }
        });
    }
}
