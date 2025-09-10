// Jhonas API
package com.example.smartessay.API;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Teacher_OpenAiAPI {

    // This interface is used as a "callback".
    // Instead of returning immediately, the API will call onSuccess or onError later.
    public interface GradeCallback {
        void onSuccess(String result);   // called when API works
        void onError(String error);      // called when API fails
    }

    // API endpoint, key, and host (needed by RapidAPI)
    private static final String API_URL = "https://open-ai21.p.rapidapi.com/conversationllama";
    private static final String API_KEY = "5d6b0c84c3msh8935cfeb2995b5fp15496djsnac76059af8ce";
    private static final String API_HOST = "open-ai21.p.rapidapi.com";

    // Default HTTP client
    private static final OkHttpClient client = new OkHttpClient();

    // Another HTTP client with longer timeouts (in case requests take longer)
    private static final OkHttpClient client2 = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)  // wait up to 60s for connection
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)   // wait up to 60s to send body
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)   // wait up to 120s for server reply
            .build();

    // Tell API we are sending JSON type
    private static final MediaType JSON = MediaType.parse("application/json");

    // Main function to grade the essay
    public static void gradeEssay(String essayText,
                                  String content, String organization,
                                  String grammar, String critical, String others,
                                  GradeCallback callback) {

        // First, create a "prompt" message that explains how the AI should grade
        String prompt = "Grade the following essay ONLY on the rubrics provided by the teacher. " +
                "Do NOT generate feedback for rubrics that are blank or not included.\n\n" +
                "Provide the result EXACTLY in this format:\n\n" +
                "Score: [overall_score]%\n\n";

        // Dynamically add grading sections ONLY if they are not empty
        if (!content.isEmpty()) {
            prompt += "Content / Ideas, [score]\n\t* [feedback]\n";
        }
        if (!organization.isEmpty()) {
            prompt += "Organization / Structure, [score]\n\t* [feedback]\n";
        }
        if (!grammar.isEmpty()) {
            prompt += "Grammar, Mechanics, and Formatting, [score]\n\t* [feedback]\n";
        }
        if (!critical.isEmpty()) {
            prompt += "Critical Thinking, [score]\n\t* [feedback]\n";
        }
        if (!others.isEmpty()) {
            prompt += "Teacher Notes: " + others + "\n";
        }

        // Append the actual essay text and rubric percentages
        prompt += "\nEssay:\n" + essayText + "\n\n" +
                "Rubric Percentages:\n" +
                "Content: " + content + "%\n" +
                "Organization: " + organization + "%\n" +
                "Grammar: " + grammar + "%\n" +
                "Critical Thinking: " + critical + "%\n" +
                "Teacher Notes: " + others;

        try {
            // Build JSON request for the API
            JSONObject messageObj = new JSONObject();
            messageObj.put("role", "user");      // who is speaking
            messageObj.put("content", prompt);   // the actual message

            JSONArray messagesArray = new JSONArray();
            messagesArray.put(messageObj);       // put the message into an array

            JSONObject requestObj = new JSONObject();
            requestObj.put("messages", messagesArray); // send messages array
            requestObj.put("web_access", false);       // no web search

            String requestJson = requestObj.toString(); // convert to string

            // Create request body with JSON data
            RequestBody body = RequestBody.create(JSON, requestJson);

            // Create the HTTP request with headers
            Request request = new Request.Builder()
                    .url(API_URL)                        // where to send
                    .post(body)                          // attach body
                    .addHeader("x-rapidapi-key", API_KEY) // API key
                    .addHeader("x-rapidapi-host", API_HOST) // API host
                    .addHeader("Content-Type", "application/json") // JSON type
                    .build();

            // Send the request asynchronously (non-blocking)
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    // IF request fails (no internet, timeout, etc.)
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("API request failed: " + e.getMessage())
                    );
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    // IF response is not successful (status code not 200 OK)
                    if (!response.isSuccessful()) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onError("Unexpected code: " + response.code())
                        );
                        return; // stop here if failed
                    }

                    // ELSE: response is successful
                    String responseBody = response.body().string(); // get body as string

                    // Pass the result back to the main thread (UI)
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onSuccess(responseBody)
                    );
                }
            });

        } catch (Exception e) {
            // IF something goes wrong when building JSON
            callback.onError("JSON build error: " + e.getMessage());
        }
    }
}
