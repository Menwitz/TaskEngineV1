package com.buzbuz.smartautoclicker.core.agent.brain

import kotlinx.serialization.Serializable

/**
 * Interface for interacting with a Large Language Model (LLM).
 */
interface LLMClient {
    /**
     * Generate a response for the given messages.
     *
     * @param messages The conversation history.
     * @param jsonSchema Optional JSON schema to enforce structured output.
     */
    suspend fun generateResponse(
        messages: List<Message>, 
        jsonSchema: String? = null
    ): LLMResponse
}

@Serializable
data class Message(
    val role: Role,
    val content: String,
    val imageUrl: String? = null // For vision models
)

enum class Role {
    SYSTEM, USER, ASSISTANT
}

data class LLMResponse(
    val content: String,
    val originalResponse: Any? = null
)
