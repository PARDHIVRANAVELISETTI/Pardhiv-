package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini API Support Data Classes (Moshi Adapters) ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

// --- Retrofit Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Retrofit Client Holder ---

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

// --- High-Level Call Helper ---

object GeminiHelper {
    suspend fun getSavingsInsight(
        restaurantName: String,
        basketItemsInfo: String,
        swiggyPriceString: String,
        zomatoPriceString: String,
        swiggyCoupons: String,
        zomatoCoupons: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Please configure your GEMINI_API_KEY in the AI Studio Secrets Panel to generate intelligent saving insights!"
        }

        val prompt = """
            Provide a smart food cart optimization recommendation for the Indian food-delivery user based on the details below.
            
            Restaurant Name: $restaurantName
            User's Current Cart Items:
            $basketItemsInfo
            
            Current Cost Comparison:
            - Swiggy Total Cost: $swiggyPriceString
            - Zomato Total Cost: $zomatoPriceString
            
            Available Mock Active Coupons:
            - Swiggy: $swiggyCoupons
            - Zomato: $zomatoCoupons
            
            Instructions:
            - Be extremely concise (under 120 words).
            - Recommend the absolute cheapest way to get their food.
            - Provide a concrete "Pro Tip": e.g., adding an item to unlock a high-tier coupon, switching to standard coupons, or toggling Swiggy One/Zomato Gold if available.
            - Use attractive bullet points if helpful. Keep it fun, helpful, and highly contextual.
            - Currency symbol to use: ₹ (Rupee).
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(temperature = 0.7f),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are a smart, friendly, and witty AI food budget advisor named BiteSaver. You analyze restaurant orders and coupon structures on Swiggy and Zomato to help users minimize their spending."))
            )
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No saving tips generated this time. Select another combination!"
        } catch (e: Exception) {
            "BiteSaver Tip: Both Swiggy and Zomato are great. Try toggling active coupon codes like TRYNEW or WELCOME50 manually in the bill sheet to dynamically find your sweet spot of savings! Error: ${e.localizedMessage}"
        }
    }
}
