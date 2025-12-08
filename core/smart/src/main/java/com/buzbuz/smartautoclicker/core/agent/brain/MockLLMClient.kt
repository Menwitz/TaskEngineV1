package com.buzbuz.smartautoclicker.core.agent.brain

import android.util.Log

/**
 * A mock LLM client that returns predefined actions for testing.
 */
class MockLLMClient : LLMClient {

    override suspend fun generateResponse(messages: List<Message>, jsonSchema: String?): LLMResponse {
        val userContent = messages.lastOrNull { it.role == Role.USER }?.content ?: ""
        Log.d("MockLLM", "Received prompt: $userContent")

        // Simple mock logic based on "GOAL" in prompt
        val responseText = when {
            userContent.contains("GOAL: Open Settings") -> """{ "action": "tap", "id": 4 }""" // Assume 4 is settings
            userContent.contains("GOAL: Scroll Down") -> """{ "action": "scroll", "direction": "down" }"""
            else -> """{ "action": "stop" }"""
        }

        return LLMResponse(content = responseText)
    }
}
