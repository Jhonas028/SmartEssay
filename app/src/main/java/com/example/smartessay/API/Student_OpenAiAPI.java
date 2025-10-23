package com.example.smartessay.API;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

public class Student_OpenAiAPI {

    public interface GradeCallback {
        void onSuccess(String result);
        void onError(String error);
    }
    /*JhonasGmail
    private static final String API_URL = "https://open-ai21.p.rapidapi.com/conversationllama";
    private static final String API_KEY = "5d6b0c84c3msh8935cfeb2995b5fp15496djsnac76059af8ce";
    private static final String API_HOST = "open-ai21.p.rapidapi.com"; */

    // Ana Gmail
    //    private static final String API_KEY = "f44f1619fcmsh442a09b23a4c5fcp1950b5jsn98559570e89b";
    private static final String API_URL = "https://open-ai21.p.rapidapi.com/conversationllama";
    private static final String API_KEY = "f44f1619fcmsh442a09b23a4c5fcp1950b5jsn98559570e89b";
    private static final String API_HOST = "open-ai21.p.rapidapi.com";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)  // wait up to 60s for connection
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)   // up to 60s to send body
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)   // up to 120s for server to respond
            .build();

    private static final MediaType JSON = MediaType.parse("application/json");

    public static void gradeEssay(String topic,String essayText,
                                  String content, String organization,
                                  String grammar, String relevance, String otherCriteria, String others,
                                  GradeCallback callback) {

        String prompt = "Grade the following essay ONLY on the rubrics provided by the teacher. " +
                "Do NOT generate feedback for rubrics that are blank or not included.\n\n" +
                "Provide the result EXACTLY in this format:\n\n" +
                "Score: [overall_score]%\n\n";

        // Dynamically build sections
        if (!content.isEmpty()) {
            prompt += "Content / Ideas\n\t→ [feedback]\n";
            prompt += "\n\tPoints for Improvement \n\t→ [feedback]\n\n";
        }
        if (!organization.isEmpty()) {
            prompt += "Organization / Structure\n\t→ [feedback]\n\n ";
            prompt += "\n\tPoints for Improvement \n\t→ [feedback]\n\n";
        }
        if (!grammar.isEmpty()) {
            prompt += "Grammar, Mechanics, and Formatting\n\t→ [feedback]\n\n ";
            prompt += "\n\tPoints for Improvement \n\t→ [feedback]\n\n";
        }
        if (!relevance.isEmpty()) {
            prompt += "Subject Relevance\n\t→ [feedback]\n\n ";
            prompt += "\n\tPoints for Improvement \n\t→ [feedback]\n\n";
        }
        /*if (!otherCriteria.isEmpty()) {
            prompt += "Other Criteria\n\t→ [feedback]\n\n ";

        }*/

        if (!others.isEmpty()) {
            prompt += "Teacher Notes: " + others + "\n\n";
        }

        prompt += "\nEssay Topic: " + topic + "\n\nEssay:\n" + essayText + "\n\n" +
                "Rubric Percentages:\n" +
                "Content: " + content + "%\n" +
                "Organization: " + organization + "%\n" +
                "Grammar: " + grammar + "%\n" +
                "Subject Relevance: " + relevance + "%\n";


        if (!otherCriteria.isEmpty()) {
            prompt += "Other Criteria: " + otherCriteria + "%\n";
        }

        prompt += "Teacher Notes: " + others;

        try {
            // ✅ Build JSON exactly like raw API example
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

            // ✅ Make async call
            client.newCall(request).enqueue(new Callback() {
                @Override

                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("OpenAI", "Failure details", e);  // <-- full stacktrace
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

                    // ✅ This returns the raw JSON response
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
