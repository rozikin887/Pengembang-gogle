package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Part(
    @Json(name = "text") val text: String? = null
)

data class Content(
    @Json(name = "parts") val parts: List<Part>
)

data class GenerationConfig(
    @Json(name = "temperature") val temperature: Double? = null,
    @Json(name = "topP") val topP: Double? = null,
    @Json(name = "topK") val topK: Int? = null
)

data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

data class Candidate(
    @Json(name = "content") val content: Content? = null
)

data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
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

class GeminiRepository {
    private val tag = "GeminiRepository"

    /**
     * Checks if the Gemini API key is valid and configured correctly.
     */
    fun isApiKeyAvailable(): Boolean {
        return try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "YOUR_API_KEY"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Calls Gemini API to get a response. If the key is unavailable, or the API fails, 
     * returns a charming, educational offline response.
     */
    suspend fun generateStoryOrAnswer(prompt: String, systemInstruction: String): String = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            Log.w(tag, "Gemini API key is not configured or using standard example placeholder. Using offline fallback response.")
            return@withContext getOfflineResponse(prompt)
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.8),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        try {
            val response = GeminiRetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
            val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!result.isNullOrBlank()) {
                result
            } else {
                Log.e(tag, "Received empty response from Gemini API")
                getOfflineResponse(prompt)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error summoning Gemini API: ", e)
            getOfflineResponse(prompt)
        }
    }

    private fun getOfflineResponse(prompt: String): String {
        val query = prompt.lowercase()
        return when {
            // FAQ Fallbacks
            query.contains("langit") && query.contains("biru") -> {
                "*tersenyum manis* Hai Sahabat! Langit berwarna biru karena sinar matahari menyinari atmosfer bumi. Atmosfer kita menyebarkan warna biru ke segala arah lebih banyak dari warna lainnya karena cahaya biru berjalan sebagai gelombang yang lebih pendek dan lebih kecil! Hebat sekali kan alam raya ciptaan Tuhan?"
            }
            query.contains("dinosaurus") && query.contains("punah") -> {
                "*berlagak seperti dinosaurus* Auumm! Jutaan tahun lalu, sebuah batu raksasa dari luar angkasa bernama meteorit menabrak bumi kita dengan sangat keras. Ini membuat iklim bumi tertutup debu sangat tebal dan cuaca mendadak menjadi sangat dingin, sehingga dinosaurus yang berukuran besar kesulitan mendapatkan makanan dan akhirnya punah."
            }
            query.contains("kucing") && query.contains("meong") -> {
                "*mengelus kepala kucing virtual* Meowww! Kucing mengeong terutama untuk berbicara dengan manusia, lho! Saat masih kecil, mereka mengeong pada induknya untuk minta susu atau perhatian. Tapi setelah dewasa, mereka tahu bahwa mengeong adalah cara terbaik untuk memanggil kita ketika lapar atau ingin diajak bermain."
            }
            query.contains("ikan") && query.contains("bernapas") -> {
                "*menirukan gerakan berenang* Blub blub blub! Ikan bernapas dalam air memakai organ khusus bernama Insang. Saat ikan membuka mulutnya, air masuk dan mengalir melewati insang. Insang lalu menyaring oksigen yang terlarut di dalam air, sehingga ikan bisa terus berenang dengan ceria tanpa takut kehabisan napas!"
            }
            
            // Story fallback
            query.contains("kancil") -> {
                "**Kancil yang Cerdik dan Pak Tani**\n\nSuatu hari yang cerah di pinggir hutan, Kancil sedang berjalan-jalan mencari buah segar. Di perjalanan, perut Kancil berbunyi kruuuuk... Oh, Kancil lapar! Ia pun melihat sebuah kebun mentimun yang sangat subur milik Pak Tani.\n\n*mengetuk dagu berpikir* Kancil tahu mengambil mentimun tanpa izin itu tidak baik. Tapi ia sangat lapar. Akhirnya, setelah memakan beberapa timun, Kancil meninggalkan pesan dari daun kering untuk Pak Tani berisi janji akan membantu membersihkan kebun besok sebagai ganti mentimun itu. Ketika Pak Tani datang dan melihat kebunnya rapi karena dibantu Kancil, ia tersenyum geli. Kancil pun belajar bahwa kejujuran dan tanggung jawab selalu berbuah manis! *tersenyum ceria*"
            }
            query.contains("angkasa") || query.contains("bintang") -> {
                "**Petualangan Riko di Angkasa Luar**\n\nRiko memakai helm astronot mainannya dan memejamkan mata di tempat tidur. *wussshh!* Tiba-tiba kasurnya berubah menjadi roket super cepat yang terbang menembus awan dan meluncur ke langit yang penuh bintang-bintang bersinar.\n\nDi angkasa luar, Riko melihat planet Mars yang berwarna merah cerah dan cincin planet Saturnus yang menyala indah dari es dan debu angkasa. Tiba-tiba ia bertemu dengan robot ramah bernama Pip-Pop yang memberinya sebuah kancing ajaib. Saat ditekan, Riko terbangun dan menyadari bahwa belajar sains sangatlah seru karena suatu hari ia bisa benar-benar jadi astronot sungguhan! *bertepuk tangan gembira*"
            }
            @Suppress("Pivot")
            query.contains("menabung") || query.contains("budi") -> {
                "**Celengan Ayam Baru Budi**\n\nBudi mendapat kado ulang tahun celengan ayam dari ibunya. Ibu berkata, \"Budi, setiap kali kamu punya uang kembalian atau sisa jajan, taruhlah di dalam perut si ayam ini ya!\"\n\n*memasukkan uang koin* Budi mulai menyisihkan lima ratus rupiah setiap sore. Kadang ia tergoda ingin membeli mainan gelembung, tapi ia ingat ingin membeli buku mewarnai dinosaurus. Setelah berjalan beberapa bulan, celengan ayam itu menjadi sangat berat! Saat dibuka bersama ibu, jumlah uangnya cukup untuk membeli buku, bahkan ada sisanya untuk ditabung kembali di bank. Budi sangat bangga belajar hidup hemat sejak dini! *tersenyum bangga*"
            }
            
            // Custom or fallback story generator outline
            else -> {
                "**Persahabatan Indah di Hutan Pintar**\n\nDi sebuah hutan yang dinamai Hutan Pintar, hiduplah seekor gajah ramah bernama Gafi yang sangat gemar membaca, dan seekor kelinci lincah bernama Kimi yang ceria.\n\nSuatu sore, Kimi kesulitan menyusun menara balok karena selalu roboh. Gafi lewat dan berkata, \"Kimi, ayo kita buat fondasi bawahnya lebih lebar agar menara kita berdiri kokoh!\" Keduanya bekerjasama memindahkan balok besar ke bawah dan balok kecil ke atas. Wah! Menara balok mereka menjulang tinggi dan tidak roboh lagi.\n\nMerekapun bersenang-senang merayakan keberhasilan mereka. Pesan moral cerita ini adalah hal yang sulit akan menjadi sangat mudah jika kita bekerjasama dengan rukun! *melompat gembira*"
            }
        }
    }
}
