package com.example

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Native TextToSpeech for children pronunciation learning
        try {
            tts = TextToSpeech(this, this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to construct TextToSpeech. ", e)
        }

        setContent {
            MyApplicationTheme {
                MainAppScreen(
                    speak = { text -> speakOut(text) }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("id", "ID")) // Indonesian voice mapping
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TTS", "Language ID of Indo is not supported, falling back to US locale.")
                tts?.setLanguage(Locale.US)
            }
            isTtsReady = true
        } else {
            Log.e("TTS", "Initialization of TextToSpeech failed.")
        }
    }

    private fun speakOut(text: String) {
        if (isTtsReady && tts != null) {
            // Remove stars or emoji tags in TTS for cleaner voice synthesis
            val speakableText = text.replace(Regex("[*|⭐|🍎|🍒|🍋|🐶|🦒|🦆|🍊|🦖|✨|💡|🐻|🐼|🎋|🌈]"), "")
            tts?.speak(speakableText, TextToSpeech.QUEUE_FLUSH, null, "SahabatPintarSpeech")
        } else {
            Toast.makeText(this, "Suara belum siap. Silahkan tunggu sebentar!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    speak: (String) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val starsEarned by viewModel.starsEarned.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Face,
                            contentDescription = "Face",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Sahabat Pintar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    if (currentScreen != Screen.Dashboard) {
                        IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali Ke Utama"
                            )
                        }
                    }
                },
                actions = {
                    // Star Counter displays globally in Top Bar to incentivize children's learning success
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Bintang Diperoleh",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "$starsEarned ⭐",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { fullWidth -> fullWidth }
                    ) + fadeIn() togetherWith slideOutHorizontally(
                        animationSpec = tween(300),
                        targetOffsetX = { fullWidth -> -fullWidth }
                    ) + fadeOut()
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.Dashboard -> DashboardScreen(
                        starsEarned = starsEarned,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                    Screen.Abjad -> AbjadScreen(
                        viewModel = viewModel,
                        speak = speak
                    )
                    Screen.Berhitung -> BerhitungScreen(
                        viewModel = viewModel,
                        speak = speak
                    )
                    Screen.Kuis -> KuisScreen(
                        viewModel = viewModel,
                        speak = speak
                    )
                    Screen.Dongeng -> DongengScreen(
                        viewModel = viewModel,
                        speak = speak
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD SCREEN (MAIN MENU)
// ==========================================
@Composable
fun DashboardScreen(
    starsEarned: Int,
    onNavigate: (Screen) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Mascot Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable { /* Fun bounce logic */ },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // High contrast mascot emoji
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🐼",
                        fontSize = 38.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hai Sahabat Pintar!",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (starsEarned == 0) "Ayo pilih permainan seru di bawah dan dapatkan bintang pertamamu hari ini! ✨"
                        else "Luar biasa! Kamu sudah mengumpulkan $starsEarned Bintang emas hari ini! Tingkatkan belajarmu ya! ✨",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Text(
            text = "Pilih Ruang Belajar Seru:",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Menu items
        DashboardMenuCard(
            title = "Belajar Abjad (ABC)",
            desc = "Mari mengenal huruf A sampai Z dengan mengeja nama benda lucu disertai papan tulis sentuh!",
            icon = Icons.Default.School,
            iconEmoji = "🅰️",
            color = Color(0xFFFF8A80), // Soft Red
            onClick = { onNavigate(Screen.Abjad) }
        )

        DashboardMenuCard(
            title = "Belajar Berhitung (123)",
            desc = "Membilang apel lezat 1 hingga 10 dan bermain tantangan penjumlahan bergambar yang asyik!",
            icon = Icons.Default.Calculate,
            iconEmoji = "🧮",
            color = Color(0xFF64B5F6), // Soft Blue
            onClick = { onNavigate(Screen.Berhitung) }
        )

        DashboardMenuCard(
            title = "Game Kuis Cerdas",
            desc = "Flashcard kuis menebak nama hewan, mencocokkan ejaan kata kosong dan meraup poin bintang!",
            icon = Icons.Default.Star,
            iconEmoji = "🏆",
            color = Color(0xFFFFB74D), // Soft Orange
            onClick = { onNavigate(Screen.Kuis) }
        )

        DashboardMenuCard(
            title = "Dongeng Pendidikan AI",
            desc = "Mendengar cerita moral mendidik atau menanyakan hal seru kepada robot panda lucu bertenaga AI!",
            icon = Icons.Default.MenuBook,
            iconEmoji = "🦖",
            color = Color(0xFFBA68C8), // Soft Purple
            onClick = { onNavigate(Screen.Dongeng) }
        )
    }
}

@Composable
fun DashboardMenuCard(
    title: String,
    desc: String,
    icon: ImageVector,
    iconEmoji: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconEmoji,
                    fontSize = 28.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Masuk",
                tint = Color(0xFF94A3B8)
            )
        }
    }
}

// ==========================================
// 2. BELAJAR ABJAD (ALPHABET WITH CHALK BOARD)
// ==========================================
@Composable
fun AbjadScreen(
    viewModel: MainViewModel,
    speak: (String) -> Unit
) {
    val selectedLetter by viewModel.selectedLetter.collectAsStateWithLifecycle()
    val alphabet = ('A'..'Z').toList()

    // Example word definitions for Indonesian children with custom beautiful graphics (emojis)
    val exampleWords = mapOf(
        'A' to "Apel 🍎", 'B' to "Bebek 🦆", 'C' to "Ceri 🍒", 'D' to "Domba 🐑", 
        'E' to "Ember 🪣", 'F' to "Fajar 🌅", 'G' to "Gajah 🐘", 'H' to "Harimau 🐅", 
        'I' to "Ikan 🐟", 'J' to "Jeruk 🍊", 'K' to "Kera 🐒", 'L' to "Lobak 🥕", 
        'M' to "Mangga 🥭", 'N' to "Nanas 🍍", 'O' to "Obor 🪵", 'P' to "Panda 🐼", 
        'Q' to "Qori 🕌", 'R' to "Rusa 🦌", 'S' to "Sapi 🐄", 'T' to "Tupai 🐿️", 
        'U' to "Unta 🐪", 'V' to "Vas bunga 🏺", 'W' to "Wortel 🥕", 'X' to "Xilofon 🪘", 
        'Y' to "Yo-yo 🪀", 'Z' to "Zebra 🦓"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active display of selected letter
        val word = exampleWords[selectedLetter] ?: "Apel"
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = selectedLetter.toString(),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(84.dp)
                    )
                    Text(
                        text = "$selectedLetter untuk $word",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pronounce button
                    Button(
                        onClick = { speak("$selectedLetter ... untuk ... $word") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Putar Suara Ejaan"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Suara", fontWeight = FontWeight.Bold)
                    }

                    // Simple Chalk drawing instruction text
                    Text(
                        text = "Ayo jiplak di bawah!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // CHALKBOARD / CANVAS PRACTICE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .border(6.dp, Color(0xFF8D6E63), RoundedCornerShape(20.dp)) // Wood frame
        ) {
            // Draw state
            val paths = remember { mutableStateListOf<Offset>() }
            var triggerRedraw by remember { mutableStateOf(0) } // Forces recompose boundary if needed

            // Black board base with background faint guideline letter to trace
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF263238)) // Dark Slate Chalk Board
                    .pointerInput(triggerRedraw) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            paths.add(change.position)
                        }
                    }
            ) {
                // Render faint letter tracing line in center of blackboard
                // Kids will draw on top of this!
                drawContext.canvas.save()
                
                // Tracing helper drawing (drawn within center viewport)
                // We draw it easily using TextPaint or just simple dashed representations, or simply draw lines.
                // Let's rely on standard compose text rendering or we can show standard large card behind.
                // Since this is custom rendering, let's draw paths of tracing lines in custom chalk-white:
                paths.forEach { point ->
                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(),
                        center = point
                    )
                }
                drawContext.canvas.restore()
            }

            // Central watermark of the letter
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedLetter.toString(),
                    fontSize = 160.sp,
                    fontWeight = FontWeight.Thin,
                    color = Color.White.copy(alpha = 0.12f),
                    textAlign = TextAlign.Center
                )
            }

            // Canvas action overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { 
                        paths.clear()
                        triggerRedraw++
                    },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Bersihkan Papan",
                        tint = Color.White
                    )
                }
            }

            Text(
                text = "Papan Tulis Sentuh",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            )
        }

        // SCROLLABLE ALPHABET GRID
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "Ketuk huruf belajar lainnya:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6), // 6 letters per row
                    modifier = Modifier.height(130.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(alphabet) { letter ->
                        val isSelected = letter == selectedLetter
                        Box(
                            modifier = Modifier
                                .aspectRatio(1.1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                )
                                .clickable { 
                                    speak(letter.toString())
                                    viewModel.selectLetter(letter)
                                    // Also auto-clears tracing canvas when changing letter for fresh sketch
                                    // Handled because paths keys on selectedLetter if we bind the lists, 
                                    // let's keep trace lists clean inside selection
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter.toString(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                        else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. BELAJAR BERHITUNG (123 & MATH GAME)
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BerhitungScreen(
    viewModel: MainViewModel,
    speak: (String) -> Unit
) {
    val selectedNumber by viewModel.selectedNumber.collectAsStateWithLifecycle()
    val mathState by viewModel.mathState.collectAsStateWithLifecycle()
    
    val numbers = (1..10).toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Title
        Text(
            text = "Belajar Membilang angka 1 sampai 10",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.secondary
        )

        // Selected Number Showcase Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedNumber.toString(),
                        fontSize = 62.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    // Speak count
                    Button(
                        onClick = { 
                            val countPhrasing = (1..selectedNumber).joinToString(" ... ")
                            speak("Mari berhitung! $countPhrasing. Totalnya ada $selectedNumber apel!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Eja Angka")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Membilang")
                    }
                }

                // Apple/Visual items count alignment
                Text(
                    text = "Hitung apel di bawah ini:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )

                // Layout rendering dynamic number of apples
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..selectedNumber) {
                        Surface(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(46.dp),
                            shape = CircleShape,
                            color = Color(0xFFFFEBEE)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "🍎",
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Horizontal Number Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Pilih Angka Lainnya:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                numbers.forEach { number ->
                    val isSelected = number == selectedNumber
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            )
                            .clickable {
                                speak(number.toString())
                                viewModel.selectNumber(number)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = number.toString(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onTertiary
                                    else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // MINI GAME PRACTICE: Addition / Subtraction
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🏆 Tantangan Berhitung!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Ayo pecahkan tantangan matematika mini di bawah ini!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                // Math Equation Representation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text(
                            text = mathState.num1.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = mathState.op,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text(
                            text = mathState.num2.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = "=",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text(
                            text = "?",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Emojis hint for visual learners
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val opColorText = if (mathState.op == "+") "DITAMBAH" else "DIKURANG"
                    Text(
                        text = "${(1..mathState.num1).joinToString("") { "⭐" }} ${mathState.op} ${(1..mathState.num2).joinToString("") { "⭐" }}",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Interactive Multiple Options buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mathState.options.forEach { option ->
                        val isSelected = option == mathState.selectedAnswer
                        val isCorrectSelection = option == mathState.correctAnswerString
                        
                        val buttonColor = when {
                            isSelected && mathState.isCorrect == true -> Color(0xFF81C784) // green
                            isSelected && mathState.isCorrect == false -> Color(0xFFE57373) // red
                            mathState.selectedAnswer != null && isCorrectSelection -> Color(0xFF81C784).copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Button(
                            onClick = { 
                                if (mathState.selectedAnswer == null) {
                                    viewModel.answerMathQuestion(option)
                                    val feedback = if (option == mathState.correctAnswerString) {
                                        "Hebat! Jawabanmu benar! Dapat satu bintang!"
                                    } else {
                                        "Kurang pas, ayo coba lagi!"
                                    }
                                    speak(feedback)
                                }
                            },
                            enabled = mathState.selectedAnswer == null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor,
                                disabledContainerColor = buttonColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = option,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Verification Feedback
                mathState.isCorrect?.let { answerResult ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (answerResult) {
                            Text(
                                text = "Hore! Benar sekali! ⭐ (+1 Bintang)",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = "Opsi kurang pas. Hasil aslinya adalah ${mathState.correctAnswerString}",
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.generateNewMathQuestion() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Tantangan Baru")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tantangan Lain", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. KUIS CERDAS SCREEN (FLASHCARD GAME)
// ==========================================
@Composable
fun KuisScreen(
    viewModel: MainViewModel,
    speak: (String) -> Unit
) {
    val kuisState by viewModel.kuisState.collectAsStateWithLifecycle()
    val totalQuestions = viewModel.questionsList.size

    if (kuisState.quizCompleted) {
        // Quiz Completion View
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🏆 Kuis Selesai! 🎉",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(Color(0xFFFFD54F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⭐",
                        fontSize = 62.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Luar biasa! Skor-mu: ${kuisState.score} poin!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Kamu mendapatkan tambahan ${kuisState.totalStars} bintang emas! Teruskan belajar ya sayang!",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )

                Button(
                    onClick = { 
                        viewModel.restartQuiz() 
                        speak("Mari bermain kuis lagi! Ayo mulai!")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Text("Main Lagi!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    } else {
        // Standard interactive quiz question view
        val currentQuestion = viewModel.questionsList[kuisState.currentQuestionIndex]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Progress Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pertanyaan ${kuisState.currentQuestionIndex + 1} dari $totalQuestions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Skor: ${kuisState.score}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                LinearProgressIndicator(
                    progress = { (kuisState.currentQuestionIndex + 1).toFloat() / totalQuestions },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            // Flashcard Question Area
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Question text (Kids friendly)
                    Text(
                        text = currentQuestion.question,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Big visual showcase box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentQuestion.imageHint,
                            fontSize = 46.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Options list
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentQuestion.options.forEach { option ->
                            val isSelected = option == kuisState.selectedOption
                            val isCorrectAnswer = option == currentQuestion.correctAnswer

                            val containerColor = when {
                                isSelected && kuisState.isCorrect == true -> Color(0xFFC8E6C9) // Green light
                                isSelected && kuisState.isCorrect == false -> Color(0xFFFFCDD2) // Red light
                                kuisState.selectedOption != null && isCorrectAnswer -> Color(0xFFC8E6C9).copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.surface
                            }

                            val tContentColor = when {
                                isSelected && kuisState.isCorrect == true -> Color(0xFF2E7D32)
                                isSelected && kuisState.isCorrect == false -> Color(0xFFC62828)
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = kuisState.selectedOption == null) {
                                        viewModel.answerQuizQuestion(option)
                                        if (option == currentQuestion.correctAnswer) {
                                            speak("Hebat! Jawabanmu benar!")
                                        } else {
                                            speak("Wah, belum tepat! Jawaban aslinya adalah ${currentQuestion.correctAnswer}")
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = containerColor),
                                border = BorderStroke(
                                    width = 1.dp, 
                                    color = if (isSelected) tContentColor else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = option,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tContentColor
                                    )
                                }
                            }
                        }
                    }

                    // Feedbacks and Navigation to next item
                    kuisState.isCorrect?.let { correct ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (correct) {
                                Text(
                                    text = "Benar! Dapat +1 Bintang Emas! ⭐",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 14.sp
                                )
                            } else {
                                Text(
                                    text = "Tidak apa-apa! Jawaban yang benar: ${currentQuestion.correctAnswer}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFC62828),
                                    fontSize = 13.sp
                                )
                            }

                                IconButton(
                                    onClick = { viewModel.nextQuizQuestion() },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Selesai",
                                        tint = Color.White
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. EDUTAINMENT AI STORY & FAQ (GEMINI POWERED)
// ==========================================
@Composable
fun DongengScreen(
    viewModel: MainViewModel,
    speak: (String) -> Unit
) {
    val dongengState by viewModel.dongengState.collectAsStateWithLifecycle()
    val isGeminiApiOnline = viewModel.isGeminiOnline()

    // Interactive Prebuilt Curious Questions list
    val prebuiltQuestions = listOf(
        "Kenapa langit berwarna biru? 💙",
        "Kenapa dinosaurus punah? 🦖",
        "Kenapa kucing mengeong? 🐱",
        "Bagaimana ikan bernapas di air berpangkal insang? 🐟"
    )

    // Prebuilt Story templates
    val storyPremises = listOf(
        Pair("Kancil Jujur 🦊", "Ceritakan dongeng Kancil yang cerdik belajar berbuat jujur setelah mengambil timun Pak Tani."),
        Pair("Petualangan Angkasa 🚀", "Cerika dongeng petualangan anak bernama Riko menembus planet Saturnus mengendarai kasur roket."),
        Pair("Menabung Hemat 🐖", "Ceritakan dongeng Budi menyisihkan koin jajan ke celengan ayam kesayangannya.")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome mascot bubble
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🤖",
                    fontSize = 42.sp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ruang Dongeng Si Pintar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (isGeminiApiOnline) "Bertanyalah apa saja atau ketuk dongeng di bawah, AI cerdas akan membuatkan cerita spesial untukmu!"
                        else "Mode Belajar Mandiri: Pilih petualangan dongeng atau tanya rasa ingin tahu di bawah!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Section A: Choose Story Dongeng
        Text(
            text = "📖 Pilih Cerita Dongeng:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            storyPremises.forEach { story ->
                Button(
                    onClick = { 
                        viewModel.loadStory(story.first, story.second)
                        speak("Mempersiapkan petualangan ${story.first}. Si Pintar siap bercerita!")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = story.first,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Section B: Tanya Si Pintar (FAQ)
        Text(
            text = "💡 Tanya Mengapa? (Rasa Ingin Tahu):",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyColumn(
            modifier = Modifier.height(130.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(prebuiltQuestions) { query ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { 
                            viewModel.askSiPintar(query)
                            speak("Aku sedang memikirkan jawaban hebat untukmu tentang $query!")
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = query,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.QuestionAnswer,
                            contentDescription = "Tanya",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // SECTION C: DISPLAY STORY / GENERATED ANSWER SCREEN
        if (dongengState.isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Si Pintar sedang memikirkan cerita ajaib baru... Tunggu sebentar ya ✨",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (dongengState.generatedContent.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📖 ${dongengState.storyTitle}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Synthesize story / Answer Speech Button
                            Button(
                                onClick = { speak(dongengState.generatedContent) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A))
                            ) {
                                Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Dengarkan dongeng")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dengar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Markdown-style structured printout suitable for kids
                    Text(
                        text = dongengState.generatedContent,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium
                    )

                    // Notice if offline fallback was loaded or Gemini with key
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isGeminiApiOnline) "✨ Cerita baru dibuat cerdas oleh AI Anda." 
                                   else "💡 Menampilkan modul belajar tersemat.",
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
