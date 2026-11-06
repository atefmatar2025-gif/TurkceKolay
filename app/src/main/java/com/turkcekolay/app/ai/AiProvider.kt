package com.turkcekolay.app.ai

import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import kotlinx.serialization.json.Json

enum class TutorMode {
    FREE_CHAT,
    EXPLAIN_WORD,
    EXPLAIN_SENTENCE,
    CORRECT_SENTENCE,
    QUIZ,
    LISTENING_PRACTICE,
    SUGGEST_REVIEW,
}

@Serializable
data class TutorWireMessage(val role: String, val text: String)

@Serializable
data class TutorRequest(
    val messages: List<TutorWireMessage>,
    val mode: String,
    /** e.g. "L0", "A1": the backend keeps Turkish at this level. */
    val learnerLevel: String,
    /** Words the learner already knows, so the tutor reuses them instead of introducing many new ones. */
    val knownWords: List<String>,
    val interfaceLanguage: String = "ar",
)

@Serializable
data class TutorResponse(val reply: String)

/**
 * Abstraction over any AI provider. The app never talks to an AI vendor directly:
 * it calls our own backend, which holds the secret key (see backend/ and docs/ARCHITECTURE.md).
 */
interface AiProvider {
    val isConfigured: Boolean
    suspend fun reply(request: TutorRequest): Result<String>
}

/** Used when no backend URL was configured at build time. The tutor shows its offline state. */
class NotConfiguredAiProvider : AiProvider {
    override val isConfigured: Boolean = false
    override suspend fun reply(request: TutorRequest): Result<String> =
        Result.failure(IllegalStateException("AI tutor backend is not configured"))
}

internal interface TutorApi {
    @POST("tutor")
    suspend fun chat(@Body request: TutorRequest): TutorResponse
}

class BackendAiProvider(baseUrl: String, client: OkHttpClient, json: Json) : AiProvider {
    private val api: TutorApi = Retrofit.Builder()
        .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(TutorApi::class.java)

    override val isConfigured: Boolean = true

    override suspend fun reply(request: TutorRequest): Result<String> =
        runCatching { api.chat(request).reply.trim() }
            .mapCatching { if (it.isEmpty()) error("Empty reply") else it }
}
