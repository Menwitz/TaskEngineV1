package com.buzbuz.smartautoclicker.core.agent.brain

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * A basic OpenAI Client implementation.
 * Requires an API Key to be passed in constructor.
 */
class OpenAIClient(private val apiKey: String) : LLMClient {

    override suspend fun generateResponse(messages: List<Message>, jsonSchema: String?): LLMResponse {
        return withContext(Dispatchers.IO) {
            try {
                // TODO: use a proper HTTP library (OkHttp/Retrofit) if available. 
                // Using HttpURLConnection for zero-dependency drop-in.
                
                val url = URL("https://api.openai.com/v1/chat/completions")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.doOutput = true

                val messagesJson = JSONArray()
                messages.forEach { msg ->
                    val m = JSONObject()
                    m.put("role", msg.role.name.lowercase())
                    m.put("content", msg.content)
                    messagesJson.put(m)
                }

                val body = JSONObject()
                body.put("model", "gpt-4o") // Default to GPT-4o
                body.put("messages", messagesJson)
                body.put("temperature", 0.0)
                
                if (jsonSchema != null) {
                   // Add structured output format if needed
                   body.put("response_format", JSONObject().put("type", "json_object"))
                }

                val os = OutputStreamWriter(connection.outputStream)
                os.write(body.toString())
                os.flush()
                os.close()

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val br = BufferedReader(InputStreamReader(connection.inputStream))
                    val responseString = br.readText()
                    br.close()
                    
                    val responseJson = JSONObject(responseString)
                    val content = responseJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        
                    LLMResponse(content = content, originalResponse = responseString)
                } else {
                     val br = BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream))
                     val errorString = br.readText()
                     Log.e("OpenAIClient", "Error $responseCode: $errorString")
                     LLMResponse("Error: $errorString")
                }
            } catch (e: Exception) {
                Log.e("OpenAIClient", "Exception calling OpenAI", e)
                LLMResponse("Exception: ${e.message}")
            }
        }
    }
}
