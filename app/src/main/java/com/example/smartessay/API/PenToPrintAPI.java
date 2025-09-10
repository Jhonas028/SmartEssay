// jhonas API
package com.example.smartessay.API;

import android.util.Log;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

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
    private static final OkHttpClient client = new OkHttpClient();

    // API endpoint (where the request will be sent)
    private static final String API_URL = "https://pen-to-print-handwriting-ocr.p.rapidapi.com/recognize/";

    // Your API key (used to access the OCR service)
    private static final String API_KEY = "5d6b0c84c3msh8935cfeb2995b5fp15496djsnac76059af8ce";

    // API host name (required by RapidAPI)
    private static final String API_HOST = "pen-to-print-handwriting-ocr.p.rapidapi.com";

    // Function to send the image file to the API and display the result
    public static void sendImage(File imageFile, TextView resultView) {

        // Create the request body (the data we send to API)
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM) // we are sending it as form-data
                // attach the image file (srcImg is the key required by API)
                .addFormDataPart("srcImg", imageFile.getName(),
                        RequestBody.create(MediaType.parse("application/octet-stream"), imageFile))
                // extra form data required by API
                .addFormDataPart("includeSubScan", "0")
                .addFormDataPart("Session", "string")
                .build();

        // Create the actual HTTP request with headers (API key, host)
        Request request = new Request.Builder()
                .url(API_URL) // where to send the request
                .post(body) // attach the request body
                .addHeader("x-rapidapi-key", API_KEY) // authorization header
                .addHeader("x-rapidapi-host", API_HOST) // host header
                .build();

        // Execute the request in the background (async)
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // If request fails (like no internet or server error)
                Log.e("PenToPrintAPI", "OCR request failed", e);
                // Update TextView on UI thread with error message
                resultView.post(() -> resultView.setText("OCR request failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // If request succeeds, get the response as string
                String result = response.body() != null ? response.body().string() : "Empty response";

                try {
                    // Convert the response string into a JSON object
                    JSONObject jsonObject = new JSONObject(result);

                    // IF: response has "value" key, it means OCR worked
                    if (jsonObject.has("value")) {
                        // Get the recognized text inside "value"
                        String value = jsonObject.getString("value")
                                // Replace multiple line breaks with space
                                .replaceAll("\\n+", " ")
                                // Replace multiple spaces with single space
                                .replaceAll("\\s{2,}", " ")
                                // Remove spaces from start and end
                                .trim();

                        // Update TextView with the recognized text
                        resultView.post(() -> {
                            String oldText = resultView.getText().toString();
                            // IF TextView is empty, just show the value
                            // ELSE, add new value below the old text
                            String newText = oldText.isEmpty() ? value : oldText + "\n\n" + value;
                            resultView.setText(newText);
                        });

                        // ELSE: if "value" does not exist in response, show raw response
                    } else {
                        resultView.post(() -> resultView.setText("No 'value' in response: " + result));
                    }

                    // If JSON is broken or invalid, show error
                } catch (JSONException e) {
                    resultView.post(() -> resultView.setText("Invalid OCR response: " + result));
                }
            }
        });
    }
}
