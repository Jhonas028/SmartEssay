// Jhonas API
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

public class Teacher_OpenAiAPI {

    public interface GradeCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    /*JhonasGmail
    private static final String API_URL = "https://open-ai21.p.rapidapi.com/conversationllama";
    private static final String API_KEY = "5d6b0c84c3msh8935cfeb2995b5fp15496djsnac76059af8ce";
    private static final String API_HOST = "open-ai21.p.rapidapi.com";
    */

    private static final String API_URL = "https://open-ai21.p.rapidapi.com/conversationllama";
    private static final String API_KEY = "f44f1619fcmsh442a09b23a4c5fcp1950b5jsn98559570e89b";
    private static final String API_HOST = "open-ai21.p.rapidapi.com";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON = MediaType.parse("application/json");

    public static void gradeEssay(String essayText,
                                  String content, String organization,
                                  String grammar, String critical, String otherCriteria, String others,
                                  GradeCallback callback) {

        String prompt = "Grade the following essay ONLY on the rubrics provided by the teacher. " +
                "Do NOT generate feedback for rubrics that are blank or not included.\n\n" +
                "Provide the result EXACTLY in this format:\n\n" +
                "Score: [overall_score]%\n\n";

        // ✅ Dynamically include sections based on provided rubrics
        if (!content.isEmpty()) {
            prompt += "Content / Ideas, [score]\n\t→ [feedback]\n\n";
        }
        if (!organization.isEmpty()) {
            prompt += "Organization / Structure, [score]\n\t→ [feedback]\n\n";
        }
        if (!grammar.isEmpty()) {
            prompt += "Grammar, Mechanics, and Formatting, [score]\n\t→ [feedback]\n\n";
        }
        if (!critical.isEmpty()) {
            prompt += "Critical Thinking, [score]\n\t→ [feedback]\n\n";
        }
        if (!otherCriteria.isEmpty()) {
            prompt += "Other Criteria, [score]\n\t→ [feedback]\n\n";
        }
        if (!others.isEmpty()) {
            prompt += "Teacher Notes: " + others + "\n\n";
        }

        // ✅ Add essay and rubric weight percentages
        prompt += "\nEssay:\n" + essayText + "\n\n" +
                "Rubric Percentages:\n" +
                "Content: " + content + "%\n" +
                "Organization: " + organization + "%\n" +
                "Grammar: " + grammar + "%\n" +
                "Critical Thinking: " + critical + "%\n";

        if (!otherCriteria.isEmpty()) {
            prompt += "Other Criteria: " + otherCriteria + "%\n";
        }

        prompt += "Teacher Notes: " + others;

        try {
            // ✅ Construct request JSON payload
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

            // ✅ Async API call with error handling
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("TeacherOpenAI", "Failure details", e);
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
