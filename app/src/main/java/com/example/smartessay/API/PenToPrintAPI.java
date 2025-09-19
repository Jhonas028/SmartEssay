// jhonas API
package com.example.smartessay.API;

import android.util.Log;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PenToPrintAPI {

    // OkHttp client used to send HTTP requests
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    // API endpoint (where the request will be sent)
    //jhonasgmail
    /*private static final String API_URL = "https://pen-to-print-handwriting-ocr.p.rapidapi.com/recognize/";

    // Your API key (used to access the OCR service)
    private static final String API_KEY = "5d6b0c84c3msh8935cfeb2995b5fp15496djsnac76059af8ce";

    // API host name (required by RapidAPI)
    private static final String API_HOST = "pen-to-print-handwriting-ocr.p.rapidapi.com";*/

    //anagmail
    private static final String API_URL = "https://pen-to-print-handwriting-ocr.p.rapidapi.com/recognize/";

    // Your API key (used to access the OCR service)
    private static final String API_KEY = "c6ec12d84dmshbd0aeebc6972b1cp17c901jsnf23c8f080543";

    // API host name (required by RapidAPI)
    private static final String API_HOST = "pen-to-print-handwriting-ocr.p.rapidapi.com";

    // Function to send the image file to the API and display the result
    // Added Runnable onFinish → to notify when OCR is done
    public static void sendImage(File imageFile, TextView resultView, Runnable onFinish) {

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("srcImg", imageFile.getName(),
                        RequestBody.create(MediaType.parse("application/octet-stream"), imageFile))
                .addFormDataPart("includeSubScan", "0")
                .addFormDataPart("Session", "string")
                .build();

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("x-rapidapi-key", API_KEY)
                .addHeader("x-rapidapi-host", API_HOST)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("PenToPrintAPI", "OCR request failed", e);
                resultView.post(() -> {
                    resultView.setText("OCR request failed: " + e.getMessage());
                    if (onFinish != null) onFinish.run(); // hide loading
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body() != null ? response.body().string() : "Empty response";

                try {
                    JSONObject jsonObject = new JSONObject(result);

                    if (jsonObject.has("value")) {
                        String value = jsonObject.getString("value")
                                .replaceAll("\\n+", " ")
                                .replaceAll("\\s{2,}", " ")
                                .trim();

                        resultView.post(() -> {
                            String oldText = resultView.getText().toString();
                            String newText = oldText.isEmpty() ? value : oldText + "\n\n" + value;
                            resultView.setText(newText);
                            if (onFinish != null) onFinish.run(); // hide loading
                        });
                    } else {
                        resultView.post(() -> {
                            resultView.setText("No 'value' in response: " + result);
                            if (onFinish != null) onFinish.run(); // hide loading
                        });
                    }
                } catch (JSONException e) {
                    resultView.post(() -> {
                        resultView.setText("Invalid OCR response: " + result);
                        if (onFinish != null) onFinish.run(); // hide loading
                    });
                }
            }
        });
    }
}
