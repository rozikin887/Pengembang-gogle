package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class Screen {
    Dashboard,
    Abjad,
    Berhitung,
    Kuis,
    Dongeng
}

data class QuizQuestion(
    val id: Int,
    val question: String,
    val imageHint: String = "", // Emojis used to display
    val options: List<String>,
    val correctAnswer: String,
    val type: String // "spelling", "counting", "trivia"
)

data class KuisState(
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val totalStars: Int = 0,
    val selectedOption: String? = null,
    val isCorrect: Boolean? = null,
    val quizCompleted: Boolean = false
)

data class DongengState(
    val storyTitle: String = "Dongeng Pendidikan",
    val generatedContent: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastPrompt: String = ""
)

data class MathPracticeState(
    val num1: Int = 3,
    val num2: Int = 2,
    val op: String = "+",
    val options: List<String> = listOf("4", "5", "6"),
    val correctAnswerString: String = "5",
    val selectedAnswer: String? = null,
    val isCorrect: Boolean? = null
)

class MainViewModel : ViewModel() {
    private val geminiRepository = GeminiRepository()

    // Screen state
    private val _currentScreen = MutableStateFlow(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Star counter (reward persistence in memory during session)
    private val _starsEarned = MutableStateFlow(0)
    val starsEarned: StateFlow<Int> = _starsEarned.asStateFlow()

    // 1. Abjad State
    private val _selectedLetter = MutableStateFlow('A')
    val selectedLetter: StateFlow<Char> = _selectedLetter.asStateFlow()

    // 2. Berhitung State
    private val _selectedNumber = MutableStateFlow(1)
    val selectedNumber: StateFlow<Int> = _selectedNumber.asStateFlow()

    private val _mathState = MutableStateFlow(MathPracticeState())
    val mathState: StateFlow<MathPracticeState> = _mathState.asStateFlow()

    // 3. Kuis State
    private val _kuisState = MutableStateFlow(KuisState())
    val kuisState: StateFlow<KuisState> = _kuisState.asStateFlow()

    // 4. Dongeng AI State
    private val _dongengState = MutableStateFlow(DongengState())
    val dongengState: StateFlow<DongengState> = _dongengState.asStateFlow()

    // List of core quiz questions for kids
    val questionsList = listOf(
        QuizQuestion(
            id = 1,
            question = "Huruf apa yang hilang pada kata ini?",
            imageHint = "A _ E L  🍎",
            options = listOf("B", "P", "T"),
            correctAnswer = "P",
            type = "spelling"
        ),
        QuizQuestion(
            id = 2,
            question = "Berapa jumlah anak anjing lucu di bawah?",
            imageHint = "🐶  🐶  🐶",
            options = listOf("2", "3", "4"),
            correctAnswer = "3",
            type = "counting"
        ),
        QuizQuestion(
            id = 3,
            question = "Hewan apa yang lehernya sangat panjang?",
            imageHint = "🦒",
            options = listOf("Gajah", "Kucing", "Jerapah"),
            correctAnswer = "Jerapah",
            type = "trivia"
        ),
        QuizQuestion(
            id = 4,
            question = "Huruf apa yang hilang pada hewan ini?",
            imageHint = "B E _ E K  🦆",
            options = listOf("D", "B", "R"),
            correctAnswer = "B",
            type = "spelling"
        ),
        QuizQuestion(
            id = 5,
            question = "Berapakah hasil penjumlahan bintang berikut? ",
            imageHint = "⭐ + ⭐ ⭐",
            options = listOf("2", "3", "5"),
            correctAnswer = "3",
            type = "counting"
        ),
        QuizQuestion(
            id = 6,
            question = "Mana buah yang berawalan huruf 'C'?",
            imageHint = "🍌  🍒  🍋",
            options = listOf("Pisang", "Ceri", "Lemon"),
            correctAnswer = "Ceri",
            type = "trivia"
        ),
        QuizQuestion(
            id = 7,
            question = "Ayo bantu hitung apel lezat ini!",
            imageHint = "🍎  🍎  🍎  🍎  🍎",
            options = listOf("4", "5", "6"),
            correctAnswer = "5",
            type = "counting"
        ),
        QuizQuestion(
            id = 8,
            question = "Hasil pengurangan jeruk segar: 4 jeruk dikurang 1 jeruk?",
            imageHint = "🍊 🍊 🍊 🍊  -  🍊",
            options = listOf("2", "3", "4"),
            correctAnswer = "3",
            type = "trivia"
        )
    )

    init {
        generateNewMathQuestion()
    }

    // Navigation actions
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun addRewardStar() {
        _starsEarned.update { it + 1 }
    }

    // --- Abjad Actions ---
    fun selectLetter(letter: Char) {
        _selectedLetter.value = letter
    }

    // --- Berhitung Actions ---
    fun selectNumber(number: Int) {
        _selectedNumber.value = number
    }

    fun generateNewMathQuestion() {
        val num1 = Random.nextInt(1, 8)
        val num2 = Random.nextInt(1, 6)
        val isAddition = Random.nextBoolean()
        val op = if (isAddition) "+" else "-"
        
        // Ensure result is positive
        val actualNum1 = if (!isAddition && num1 < num2) num2 else num1
        val actualNum2 = if (!isAddition && num1 < num2) num1 else num2
        
        val correctVal = if (isAddition) actualNum1 + actualNum2 else actualNum1 - actualNum2
        val wrong1 = (correctVal + Random.nextInt(1, 3))
        val wrong2 = (correctVal - Random.nextInt(1, 3)).coerceAtLeast(0)
        
        val incorrectVal1 = if (wrong1 == correctVal) correctVal + 4 else wrong1
        val incorrectVal2 = if (wrong2 == correctVal || wrong2 == incorrectVal1) {
            correctVal + 2
        } else {
            wrong2
        }

        val options = listOf(correctVal.toString(), incorrectVal1.toString(), incorrectVal2.toString()).shuffled()

        _mathState.value = MathPracticeState(
            num1 = actualNum1,
            num2 = actualNum2,
            op = op,
            options = options,
            correctAnswerString = correctVal.toString(),
            selectedAnswer = null,
            isCorrect = null
        )
    }

    fun answerMathQuestion(answer: String) {
        val currentState = _mathState.value
        if (currentState.selectedAnswer != null) return // Already answered
        
        val correct = answer == currentState.correctAnswerString
        _mathState.value = currentState.copy(
            selectedAnswer = answer,
            isCorrect = correct
        )

        if (correct) {
            addRewardStar()
        }
    }

    // --- Kuis Actions ---
    fun answerQuizQuestion(option: String) {
        val currentState = _kuisState.value
        if (currentState.selectedOption != null) return // Answered already

        val currentQuestion = questionsList[currentState.currentQuestionIndex]
        val correct = option == currentQuestion.correctAnswer

        _kuisState.value = currentState.copy(
            selectedOption = option,
            isCorrect = correct,
            score = if (correct) currentState.score + 10 else currentState.score,
            totalStars = if (correct) currentState.totalStars + 1 else currentState.totalStars
        )

        if (correct) {
            addRewardStar()
        }
    }

    fun nextQuizQuestion() {
        val currentState = _kuisState.value
        val nextIndex = currentState.currentQuestionIndex + 1
        
        if (nextIndex < questionsList.size) {
            _kuisState.value = currentState.copy(
                currentQuestionIndex = nextIndex,
                selectedOption = null,
                isCorrect = null
            )
        } else {
            _kuisState.value = currentState.copy(
                quizCompleted = true
            )
        }
    }

    fun restartQuiz() {
        _kuisState.value = KuisState()
    }

    // --- Dongeng AI Actions ---
    fun loadStory(titleKeyword: String, promptText: String) {
        _dongengState.value = DongengState(
            storyTitle = titleKeyword,
            generatedContent = "",
            isLoading = true,
            error = null,
            lastPrompt = promptText
        )

        viewModelScope.launch {
            val systemInstruction = """
                Anda adalah Si Pintar, robot panda lucu penyayang anak-anak yang suka bercerita dongeng anak-anak yang sangat edukatif, sarat pesan moral, tertulis dalam Bahasa Indonesia yang sangat ramah anak-anak, hangat, bersemangat, jenaka, dan mudah dimengerti anak berumur 4-8 tahun.
                Panjang cerita Anda batasi cukup 2 sampai 3 paragraf pendek saja (maksimal 200 kata) agar anak tidak bosan membaca, sisipkan ekspresi emosi dalam kurung tanda bintang seperti *tertawa ceria*, *membelalakkan mata heran*, *melompat gembira*, *berbisik pelan*. Cerita harus diakhiri dengan evaluasi moral ringkas yang mendidik.
            """.trimIndent()

            val story = geminiRepository.generateStoryOrAnswer(promptText, systemInstruction)
            
            _dongengState.update { 
                it.copy(
                    generatedContent = story,
                    isLoading = false
                )
            }
        }
    }

    fun askSiPintar(question: String) {
        _dongengState.value = DongengState(
            storyTitle = "Jawaban Rasa Ingin Tahu",
            generatedContent = "",
            isLoading = true,
            error = null,
            lastPrompt = question
        )

        viewModelScope.launch {
            val systemInstruction = """
                Anda adalah Si Pintar, panda robot penjelajah dunia sains yang lucu dan cerdas sekali. Anak kecil sedang menanyakan pertanyaan rasa ingin tahu "Mengapa/Kenapa" yang sangat penting bagi mereka.
                Tugas Anda adalah menjawabnya dengan sangat gembira, sederhana, mudah ditangkap akal anak berumur 4-8 tahun, menganalogikannya dengan benda sehari-hari jika dimungkinkan, gunakan ekspresi lucu dalam tanda kurung bintang seperti *mengedipkan mata*, *tersenyum ceria*, dsb.
                Jawab dengan hangat dalam Bahasa Indonesia dan batasi hanya 1 sampai 2 paragraf ramah anak. Akhiri dengan pujian: "Kamu anak yang hebat karena suka belajar!".
            """.trimIndent()

            val answer = geminiRepository.generateStoryOrAnswer(question, systemInstruction)
            
            _dongengState.update { 
                it.copy(
                    generatedContent = answer,
                    isLoading = false
                )
            }
        }
    }

    fun isGeminiOnline(): Boolean {
        return geminiRepository.isApiKeyAvailable()
    }
}
