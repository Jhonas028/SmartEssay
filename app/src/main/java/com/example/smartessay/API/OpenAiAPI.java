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

public class OpenAiAPI {

    public interface GradeCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    private static final String API_URL = "https://open-ai21.p.rapidapi.com/conversationllama";
    private static final String API_KEY = "5d6b0c84c3msh8935cfeb2995b5fp15496djsnac76059af8ce";
    private static final String API_HOST = "open-ai21.p.rapidapi.com";

    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json");

    public static void gradeEssay(String essayText,
                                  String content, String organization, String development,
                                  String grammar, String critical, String others,
                                  GradeCallback callback) {

        // Prepare prompt
        String prompt = "Grade the following essay based on these rubrics. " +
                "Provide the result EXACTLY in the following format with proper spacing and indentation:\n\n" +
                "Score: [overall_score]%\n\n" +
                "Content / Ideas, [score]\n" +
                "\t* [feedback]\n" +
                "Organization / Structure, [score]\n" +
                "\t* [feedback]\n" +
                "Development & Support, [score]\n" +
                "\t* [feedback]\n" +
                "Language Use / Style, [score]\n" +
                "\t* [feedback]\n" +
                "Grammar, Mechanics, and Formatting, [score]\n" +
                "\t* [feedback]\n\n" +
                "Essay:\n" + essayText + "\n\n" +
                "Rubric Percentages:\n" +
                "Content: " + content + "%\n" +
                "Organization: " + organization + "%\n" +
                "Development: " + development + "%\n" +
                "Grammar: " + grammar + "%\n" +
                "Critical Thinking: " + critical + "%\n" +
                "Teacher Notes: " + others;


        try {
            // Build JSON safely
            JSONObject messageObj = new JSONObject();
            messageObj.put("role", "user");
            messageObj.put("content", prompt);

            JSONArray messagesArray = new JSONArray();
            messagesArray.put(messageObj);

            JSONObject requestObj = new JSONObject();
            requestObj.put("messages", messagesArray);
            requestObj.put("web_access", false);

            String requestJson = requestObj.toString();

            RequestBody body = RequestBody.create(JSON, requestJson);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("x-rapidapi-key", API_KEY)
                    .addHeader("x-rapidapi-host", API_HOST)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("API request failed: " + e.getMessage())
                    );
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onError("Unexpected code: " + response.code())
                        );
                        return;
                    }
                    String responseBody = response.body().string();
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onSuccess(responseBody)
                    );
                }
            });

        } catch (Exception e) {
            callback.onError("JSON build error: " + e.getMessage());
        }
    }

}
