package com.example.win11launcher

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class WindowKind {
    THIS_PC,
    SETTINGS,
    BROWSER,
    NOTEPAD
}

data class DesktopWindow(
    val id: String,
    val title: String,
    val iconName: String,
    val kind: WindowKind,
    val url: String = "https://www.google.com",
    val offsetX: Float = 24f,
    val offsetY: Float = 16f,
    val isMaximized: Boolean = false,
    val isMinimized: Boolean = false,
    val zIndex: Float = 10f
)

data class InstalledAppItem(
    val label: String,
    val packageName: String,
    val icon: Drawable?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveBars()

        runCatching {
            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        AppEntryPoint(onExitLauncher = { finish() })
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveBars()
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveBars()
    }

    private fun applyImmersiveBars() {
        runCatching {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.let { controller ->
                    controller.hide(
                        android.view.WindowInsets.Type.statusBars() or
                            android.view.WindowInsets.Type.navigationBars()
                    )
                    controller.systemBarsBehavior =
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
            }
        }
    }
}

@Composable
fun AppEntryPoint(onExitLauncher: () -> Unit) {
    var bootCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2200L)
        bootCompleted = true
    }

    if (!bootCompleted) {
        Win11BootScreen(onSkip = { bootCompleted = true })
    } else {
        Win11LauncherRoot(onExitLauncher = onExitLauncher)
    }
}

@Composable
fun Win11BootScreen(onSkip: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "boot")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinner"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF04060A))
            .clickable { onSkip() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            StartLogoMark(sizeDp = 52)

            Canvas(modifier = Modifier.size(24.dp)) {
                drawArc(
                    color = Color(0xFF38BDF8),
                    startAngle = angle,
                    sweepAngle = 240f,
                    useCenter = false,
                    style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Text(
                text = "Windows 11",
                color = Color(0xFFE2E8F0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StartLogoMark(sizeDp: Int = 18) {
    val half = (sizeDp / 2).coerceAtLeast(6)
    val gap = 2.dp
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Box(
                modifier = Modifier
                    .size(half.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color(0xFF00A4EF))
            )
            Box(
                modifier = Modifier
                    .size(half.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color(0xFF00A4EF))
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Box(
                modifier = Modifier
                    .size(half.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color(0xFF00A4EF))
            )
            Box(
                modifier = Modifier
                    .size(half.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color(0xFF00A4EF))
            )
        }
    }
}

@Composable
fun drawableBitmapOrNull(context: Context, name: String): ImageBitmap? {
    return remember(name) {
        runCatching {
            val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (resId != 0) {
                val d = ContextCompat.getDrawable(context, resId)
                val w = d?.intrinsicWidth?.coerceAtLeast(1)?.coerceAtMost(900) ?: 1
                val h = d?.intrinsicHeight?.coerceAtLeast(1)?.coerceAtMost(900) ?: 1
                d?.toBitmap(w, h)?.asImageBitmap()
            } else {
                null
            }
        }.getOrNull()
    }
}

@Composable
fun Win11AssetIcon(
    name: String,
    fallbackText: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val bmp = drawableBitmapOrNull(context, name)
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = fallbackText,
            modifier = modifier,
            contentScale = contentScale
        )
    } else if (name == "icon_youtube") {
        // أيقونة يوتيوب مدمجة واضحة إذا لم يوجد ملف خارجي
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFFDC2626)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "▶",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1F2937))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackText.take(2),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Win11LauncherRoot(onExitLauncher: () -> Unit) {
    val context = LocalContext.current
    val audioManager = remember {
        runCatching { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }.getOrNull()
    }
    val prefs = remember {
        context.getSharedPreferences("win11_launcher_prefs", Context.MODE_PRIVATE)
    }

    var selectedWallpaper by remember {
        mutableStateOf(prefs.getString("wallpaper", "wall_win11") ?: "wall_win11")
    }
    var searchQuery by remember { mutableStateOf("") }
    var showStartMenu by remember { mutableStateOf(false) }
    var showQuickSettings by remember { mutableStateOf(false) }
    var showCalendarPopup by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var zCounter by remember { mutableFloatStateOf(20f) }

    val windows = remember { mutableStateListOf<DesktopWindow>() }

    fun closeAllPopups() {
        showStartMenu = false
        showQuickSettings = false
        showCalendarPopup = false
        showVolumeSlider = false
        showContextMenu = false
    }

    fun openOrFocusWindow(
        id: String,
        title: String,
        iconName: String,
        kind: WindowKind,
        url: String = "https://www.google.com"
    ) {
        zCounter += 1f
        val existingIndex = windows.indexOfFirst { it.id == id }
        if (existingIndex >= 0) {
            val cur = windows[existingIndex]
            windows[existingIndex] = cur.copy(isMinimized = false, zIndex = zCounter)
        } else {
            val shift = (windows.size % 4) * 14f
            windows.add(
                DesktopWindow(
                    id = id,
                    title = title,
                    iconName = iconName,
                    kind = kind,
                    url = url,
                    offsetX = 16f + shift,
                    offsetY = 12f + shift,
                    isMaximized = false,
                    isMinimized = false,
                    zIndex = zCounter
                )
            )
        }
        closeAllPopups()
    }

    val maxVolume = remember(audioManager) {
        runCatching { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15 }.getOrDefault(15)
    }
    var currentVolume by remember(audioManager) {
        mutableIntStateOf(
            runCatching { audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 5 }.getOrDefault(5)
        )
    }
    LaunchedEffect(audioManager) {
        while (true) {
            runCatching {
                audioManager?.let {
                    val v = it.getStreamVolume(AudioManager.STREAM_MUSIC)
                    if (v != currentVolume) currentVolume = v
                }
            }
            delay(1500L)
        }
    }
    val volumePercent by remember(currentVolume, maxVolume) {
        derivedStateOf { ((currentVolume.toFloat() / maxVolume.toFloat()) * 100f).roundToInt().coerceIn(0, 100) }
    }
    val volumeIconName by remember(currentVolume, volumePercent) {
        derivedStateOf {
            when {
                currentVolume == 0 -> "icon_vol_mute"
                volumePercent <= 25 -> "icon_vol_low"
                volumePercent <= 66 -> "icon_vol_mid"
                else -> "icon_vol_high"
            }
        }
    }

    var batteryLevel by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                runCatching {
                    if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                        if (level >= 0) {
                            batteryLevel = ((level.toFloat() / scale.toFloat()) * 100f).roundToInt()
                        }
                        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                    }
                }
            }
        }
        runCatching {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val initialIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
            initialIntent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                if (level >= 0) batteryLevel = ((level.toFloat() / scale.toFloat()) * 100f).roundToInt()
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    var nowText by remember { mutableStateOf(formatCurrentTimeDate()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowText = formatCurrentTimeDate()
            delay(15_000L)
        }
    }

    val installedApps = remember {
        loadInstalledAppsSafely(context)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF070B12))) {
        val wallBmp = drawableBitmapOrNull(context, selectedWallpaper)
        if (wallBmp != null) {
            Image(
                bitmap = wallBmp,
                contentDescription = "خلفية سطح المكتب",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // مساحة سطح المكتب الكاملة مع دعم الضغط المطوّل (بديل الزر الأيمن)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 38.dp)
                .combinedClickable(
                    onClick = { closeAllPopups() },
                    onLongClick = {
                        closeAllPopups()
                        showContextMenu = true
                    }
                )
        ) {
            // أيقونات سطح المكتب
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DesktopIconItem(
                    iconName = "icon_computer",
                    label = "هذا الكمبيوتر",
                    onClick = {
                        openOrFocusWindow("win_pc", "هذا الكمبيوتر", "icon_computer", WindowKind.THIS_PC)
                    }
                )
                DesktopIconItem(
                    iconName = "icon_chrome",
                    label = "متصفح ويب",
                    onClick = {
                        openOrFocusWindow("win_browser", "متصفح الويب", "icon_chrome", WindowKind.BROWSER, "https://www.google.com")
                    }
                )
                DesktopIconItem(
                    iconName = "icon_youtube",
                    label = "يوتيوب",
                    onClick = {
                        openOrFocusWindow("win_yt", "يوتيوب", "icon_youtube", WindowKind.BROWSER, "https://m.youtube.com")
                    }
                )
                DesktopIconItem(
                    iconName = "icon_notepad",
                    label = "المفكرة",
                    onClick = {
                        openOrFocusWindow("win_notes", "المفكرة", "icon_notepad", WindowKind.NOTEPAD)
                    }
                )
            }

            // قائمة السياق (الزر الأيمن / ضغط مطوّل)
            if (showContextMenu) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(185.dp)
                        .zIndex(97f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFA1C212B),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        ContextMenuRow("تحديث سطح المكتب") { closeAllPopups() }
                        ContextMenuRow("فتح المفكرة") {
                            openOrFocusWindow("win_notes", "المفكرة", "icon_notepad", WindowKind.NOTEPAD)
                        }
                        ContextMenuRow("تغيير الخلفية") {
                            openOrFocusWindow("win_settings", "الإعدادات", "icon_settings", WindowKind.SETTINGS)
                        }
                        ContextMenuRow("تصغير كل النوافذ") {
                            for (i in windows.indices) {
                                windows[i] = windows[i].copy(isMinimized = true)
                            }
                            closeAllPopups()
                        }
                    }
                }
            }

            // النوافذ المفتوحة بأبعاد متناسبة مع شاشة الهاتف
            windows.forEachIndexed { idx, win ->
                if (!win.isMinimized) {
                    FloatingDesktopWindow(
                        window = win,
                        onFocus = {
                            zCounter += 1f
                            windows[idx] = win.copy(zIndex = zCounter)
                        },
                        onMove = { dx, dy ->
                            if (!win.isMaximized) {
                                windows[idx] = win.copy(
                                    offsetX = (win.offsetX + dx).coerceIn(0f, 340f),
                                    offsetY = (win.offsetY + dy).coerceIn(0f, 220f)
                                )
                            }
                        },
                        onToggleMaximize = {
                            zCounter += 1f
                            windows[idx] = win.copy(isMaximized = !win.isMaximized, zIndex = zCounter)
                        },
                        onMinimize = {
                            windows[idx] = win.copy(isMinimized = true)
                        },
                        onClose = {
                            windows.removeAt(idx)
                        },
                        selectedWallpaper = selectedWallpaper,
                        onSelectWallpaper = { newWall ->
                            selectedWallpaper = newWall
                            prefs.edit().putString("wallpaper", newWall).apply()
                        },
                        installedApps = installedApps,
                        onLaunchExternalApp = { app ->
                            launchExternalAppSafely(context, app.packageName)
                        },
                        onOpenWindow = { id, title, icon, kind, url ->
                            openOrFocusWindow(id, title, icon, kind, url)
                        }
                    )
                }
            }

            // قائمة Start
            if (showStartMenu) {
                val filteredApps = remember(installedApps, searchQuery) {
                    if (searchQuery.isBlank()) installedApps
                    else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .width(310.dp)
                        .height(235.dp)
                        .zIndex(95f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xF8181C24),
                    border = BorderStroke(1.dp, Color(0x26FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            StartShortcutChip("هذا الكمبيوتر") {
                                openOrFocusWindow("win_pc", "هذا الكمبيوتر", "icon_computer", WindowKind.THIS_PC)
                            }
                            StartShortcutChip("الإعدادات") {
                                openOrFocusWindow("win_settings", "الإعدادات", "icon_settings", WindowKind.SETTINGS)
                            }
                            StartShortcutChip("المفكرة") {
                                openOrFocusWindow("win_notes", "المفكرة", "icon_notepad", WindowKind.NOTEPAD)
                            }
                        }

                        Text(
                            text = "التطبيقات (${filteredApps.size})",
                            color = Color(0xFF93C5FD),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 3.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x12FFFFFF))
                                        .clickable {
                                            closeAllPopups()
                                            launchExternalAppSafely(context, app.packageName)
                                        }
                                        .padding(horizontal = 7.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val appBmp = remember(app.packageName) {
                                        runCatching {
                                            val d = app.icon
                                            val w = d?.intrinsicWidth?.coerceIn(16, 48) ?: 32
                                            val h = d?.intrinsicHeight?.coerceIn(16, 48) ?: 32
                                            d?.toBitmap(w, h)?.asImageBitmap()
                                        }.getOrNull()
                                    }
                                    if (appBmp != null) {
                                        Image(
                                            bitmap = appBmp,
                                            contentDescription = app.label,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = app.label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0x22FFFFFF))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Windows 11", color = Color(0xFF9CA3AF), fontSize = 10.sp)
                            Text(
                                text = "إغلاق اللانشر",
                                color = Color(0xFFF87171),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .clickable { onExitLauncher() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (showVolumeSlider) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 4.dp)
                        .width(195.dp)
                        .zIndex(96f),
                    shape = RoundedCornerShape(9.dp),
                    color = Color(0xF81E232C),
                    border = BorderStroke(1.dp, Color(0x26FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الصوت", color = Color.White, fontSize = 10.sp)
                            Text("$volumePercent%", color = Color(0xFF60A5FA), fontSize = 10.sp)
                        }
                        Slider(
                            value = currentVolume.toFloat(),
                            onValueChange = { newVal ->
                                val target = newVal.roundToInt().coerceIn(0, maxVolume)
                                currentVolume = target
                                runCatching { audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0) }
                            },
                            valueRange = 0f..maxVolume.toFloat()
                        )
                    }
                }
            }

            if (showQuickSettings) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 4.dp)
                        .width(225.dp)
                        .zIndex(96f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xF81E232C),
                    border = BorderStroke(1.dp, Color(0x26FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("المركز السريع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            QuickTile("الواي فاي", "icon_network", active = true)
                            QuickTile("الإعدادات", "icon_settings", active = false) {
                                openOrFocusWindow("win_settings", "الإعدادات", "icon_settings", WindowKind.SETTINGS)
                            }
                        }
                        Text(
                            text = if (isCharging) "البطارية: $batteryLevel% (يشحن)" else "البطارية: $batteryLevel%",
                            color = if (isCharging) Color(0xFF4ADE80) else Color(0xFFE5E7EB),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (showCalendarPopup) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 4.dp)
                        .width(175.dp)
                        .zIndex(96f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xF81E232C),
                    border = BorderStroke(1.dp, Color(0x26FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("التاريخ والوقت", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(nowText, color = Color(0xFF93C5FD), fontSize = 10.sp)
                    }
                }
            }
        }

        // شريط المهام السفلي (38.dp) مع علامة Start الواضحة والتاريخ والوقت المرتب
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(38.dp)
                .zIndex(100f),
            color = Color(0xF212161E),
            border = BorderStroke(1.dp, Color(0x1AFFFFFF))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // التاريخ والوقت والبطارية والصوت
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = nowText,
                        color = Color.White,
                        fontSize = 8.5.sp,
                        lineHeight = 10.sp,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                val next = !showCalendarPopup
                                closeAllPopups()
                                showCalendarPopup = next
                            }
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    )

                    DynamicBatteryIndicator(
                        level = batteryLevel,
                        isCharging = isCharging,
                        modifier = Modifier.clickable {
                            val next = !showQuickSettings
                            closeAllPopups()
                            showQuickSettings = next
                        }
                    )

                    Win11AssetIcon(
                        name = volumeIconName,
                        fallbackText = "VOL",
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                val next = !showVolumeSlider
                                closeAllPopups()
                                showVolumeSlider = next
                            }
                    )

                    Win11AssetIcon(
                        name = "icon_network",
                        fallbackText = "NET",
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                val next = !showQuickSettings
                                closeAllPopups()
                                showQuickSettings = next
                            }
                    )
                }

                // علامة Start + البحث + النوافذ المفتوحة
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (showStartMenu) Color(0x3860A5FA) else Color(0x18FFFFFF))
                            .clickable {
                                val next = !showStartMenu
                                closeAllPopups()
                                showStartMenu = next
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        StartLogoMark(sizeDp = 16)
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { newText ->
                            searchQuery = newText
                            if (newText.isNotBlank()) showStartMenu = true
                        },
                        placeholder = { Text("بحث", fontSize = 9.sp, color = Color(0xFF9CA3AF)) },
                        singleLine = true,
                        modifier = Modifier.width(88.dp).height(30.dp),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 10.sp)
                    )

                    windows.forEachIndexed { index, w ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!w.isMinimized) Color(0x2860A5FA) else Color(0x14FFFFFF))
                                .clickable {
                                    zCounter += 1f
                                    windows[index] = w.copy(isMinimized = false, zIndex = zCounter)
                                }
                                .padding(horizontal = 5.dp)
                        ) {
                            Win11AssetIcon(
                                name = w.iconName,
                                fallbackText = w.title,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = w.title,
                                color = Color.White,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // زر تصغير الكل
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .background(Color(0x6693C5FD))
                        .clickable {
                            for (i in windows.indices) {
                                windows[i] = windows[i].copy(isMinimized = true)
                            }
                            closeAllPopups()
                        }
                )
            }
        }
    }
}

@Composable
fun ContextMenuRow(title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(text = title, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
fun StartShortcutChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF2563EB))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DesktopIconItem(
    iconName: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Win11AssetIcon(
            name = iconName,
            fallbackText = label,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 9.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DynamicBatteryIndicator(
    level: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val fillColor = when {
        isCharging -> Color(0xFF22C55E)
        level <= 20 -> Color(0xFFEF4444)
        else -> Color(0xFFE5E7EB)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .padding(horizontal = 2.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = if (isCharging) "$level%⚡" else "$level%",
            color = Color.White,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Medium
        )
        Canvas(modifier = Modifier.width(16.dp).height(9.dp)) {
            val bodyWidth = size.width - 2.dp.toPx()
            val bodyHeight = size.height
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset.Zero,
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
                style = Stroke(width = 1.1.dp.toPx())
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(bodyWidth, bodyHeight * 0.3f),
                size = Size(1.8.dp.toPx(), bodyHeight * 0.4f),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            val pad = 1.5.dp.toPx()
            val innerMaxW = (bodyWidth - pad * 2).coerceAtLeast(0f)
            val fillW = innerMaxW * (level.coerceIn(0, 100) / 100f)
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(pad, pad),
                size = Size(fillW, (bodyHeight - pad * 2).coerceAtLeast(0f)),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

@Composable
fun QuickTile(
    title: String,
    iconName: String,
    active: Boolean,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .width(98.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(7.dp))
            .clickable(onClick = onClick),
        color = if (active) Color(0xFF2563EB) else Color(0x22FFFFFF),
        shape = RoundedCornerShape(7.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Win11AssetIcon(name = iconName, fallbackText = title, modifier = Modifier.size(14.dp))
            Text(text = title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FloatingDesktopWindow(
    window: DesktopWindow,
    onFocus: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onToggleMaximize: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    selectedWallpaper: String,
    onSelectWallpaper: (String) -> Unit,
    installedApps: List<InstalledAppItem>,
    onLaunchExternalApp: (InstalledAppItem) -> Unit,
    onOpenWindow: (String, String, String, WindowKind, String) -> Unit
) {
    val context = LocalContext.current
    val modifier = if (window.isMaximized) {
        Modifier
            .fillMaxSize()
            .zIndex(window.zIndex)
    } else {
        Modifier
            .width(310.dp)
            .height(215.dp)
            .offset { IntOffset(window.offsetX.roundToInt(), window.offsetY.roundToInt()) }
            .zIndex(window.zIndex)
    }

    Surface(
        modifier = modifier.clickable(onClick = onFocus),
        shape = if (window.isMaximized) RoundedCornerShape(0.dp) else RoundedCornerShape(9.dp),
        color = Color(0xFA181C24),
        border = BorderStroke(1.dp, Color(0x28FFFFFF))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // شريط العنوان القابل للسحب
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .background(Color(0xFF222733))
                    .pointerInput(window.id, window.isMaximized) {
                        detectDragGestures(
                            onDragStart = { onFocus() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onMove(dragAmount.x, dragAmount.y)
                            }
                        )
                    }
                    .padding(horizontal = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Win11AssetIcon(
                        name = window.iconName,
                        fallbackText = window.title,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = window.title,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    WindowControlBtn("—", onClick = onMinimize)
                    WindowControlBtn(if (window.isMaximized) "❐" else "□", onClick = onToggleMaximize)
                    WindowControlBtn("✕", isClose = true, onClick = onClose)
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                when (window.kind) {
                    WindowKind.THIS_PC -> FileManagerWindowContent(
                        installedApps = installedApps,
                        onLaunchExternalApp = onLaunchExternalApp
                    )
                    WindowKind.SETTINGS -> SettingsWindowContent(
                        selectedWallpaper = selectedWallpaper,
                        onSelectWallpaper = onSelectWallpaper,
                        onOpenNotepad = {
                            onOpenWindow("win_notes", "المفكرة", "icon_notepad", WindowKind.NOTEPAD, "")
                        }
                    )
                    WindowKind.BROWSER -> {
                        var address by remember(window.url) { mutableStateOf(window.url) }
                        var loadUrl by remember(window.url) { mutableStateOf(window.url) }
                        Column(modifier = Modifier.fillMaxSize()) {
                            // شريط رابط مدمج وصغير حتى لا يأخذ مساحة كبيرة
                            Row(
                                modifier = Modifier.fillMaxWidth().height(28.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { updatedAddr -> address = updatedAddr },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    textStyle = TextStyle(color = Color.White, fontSize = 9.5.sp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFF2563EB))
                                        .clickable {
                                            val clean = if (address.startsWith("http")) address else "https://$address"
                                            loadUrl = clean
                                        }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("ذهاب", fontSize = 9.5.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            AndroidView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp)),
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        runCatching {
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.loadWithOverviewMode = true
                                            settings.useWideViewPort = true
                                            webViewClient = WebViewClient()
                                            loadUrl(loadUrl)
                                        }
                                    }
                                },
                                update = { wv ->
                                    runCatching {
                                        if (wv.url != loadUrl && loadUrl.isNotBlank()) wv.loadUrl(loadUrl)
                                    }
                                }
                            )
                        }
                    }
                    WindowKind.NOTEPAD -> {
                        val prefs = remember {
                            context.getSharedPreferences("win11_notepad_prefs", Context.MODE_PRIVATE)
                        }
                        var noteText by remember {
                            mutableStateOf(prefs.getString("saved_note", "") ?: "")
                        }
                        var savedBadge by remember { mutableStateOf("محفوظ") }

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(savedBadge, color = Color(0xFF4ADE80), fontSize = 9.5.sp)
                                Button(
                                    onClick = {
                                        prefs.edit().putString("saved_note", noteText).apply()
                                        savedBadge = "تم الحفظ ✓"
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    Text("حفظ", fontSize = 9.5.sp, color = Color.White)
                                }
                            }
                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { updatedNote ->
                                    noteText = updatedNote
                                    prefs.edit().putString("saved_note", updatedNote).apply()
                                    savedBadge = "محفوظ تلقائياً"
                                },
                                placeholder = { Text("اكتب ملاحظاتك هنا...", color = Color(0xFF9CA3AF), fontSize = 10.sp) },
                                modifier = Modifier.fillMaxSize(),
                                textStyle = TextStyle(color = Color.White, fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WindowControlBtn(
    symbol: String,
    isClose: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isClose) Color(0x33EF4444) else Color(0x18FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FileManagerWindowContent(
    installedApps: List<InstalledAppItem>,
    onLaunchExternalApp: (InstalledAppItem) -> Unit
) {
    val rootDir = remember {
        runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
    }
    var currentDir by remember { mutableStateOf(rootDir) }
    var activeTab by remember { mutableStateOf("files") }

    val storageInfo = remember {
        runCatching {
            val path: File = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val totalGb = stat.totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            val availGb = stat.availableBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            String.format(Locale.US, "%.1f GB متاح من %.1f GB", availGb, totalGb)
        }.getOrDefault("التخزين الداخلي (:C)")
    }

    val entries = remember(currentDir) {
        runCatching {
            currentDir?.listFiles()
                ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.getDefault()) })
                ?.take(80)
                ?: emptyList()
        }.getOrDefault(emptyList())
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (activeTab == "files") Color(0xFF2563EB) else Color(0x18FFFFFF))
                        .clickable { activeTab = "files" }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text("الملفات", color = Color.White, fontSize = 9.5.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (activeTab == "apps") Color(0xFF2563EB) else Color(0x18FFFFFF))
                        .clickable { activeTab = "apps" }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text("التطبيقات", color = Color.White, fontSize = 9.5.sp)
                }
            }

            if (activeTab == "files" && currentDir != null && currentDir?.parentFile != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0x18FFFFFF))
                        .clickable {
                            val parent = currentDir?.parentFile
                            if (parent != null && parent.canRead()) {
                                currentDir = parent
                            }
                        }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text("↑ رجوع", color = Color(0xFF93C5FD), fontSize = 9.5.sp)
                }
            }
        }

        Text(
            text = if (activeTab == "files") (currentDir?.path ?: storageInfo) else storageInfo,
            color = Color(0xFF9CA3AF),
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (activeTab == "files") {
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "لا توجد ملفات مقروءة هنا أو يلزم منح صلاحية التخزين من إعدادات النظام",
                        color = Color(0xFF9CA3AF),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(entries, key = { it.absolutePath }) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0x12FFFFFF))
                                .clickable {
                                    if (file.isDirectory && file.canRead()) {
                                        currentDir = file
                                    }
                                }
                                .padding(horizontal = 7.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (file.isDirectory) "📁 ${file.name}" else "📄 ${file.name}",
                                color = if (file.isDirectory) Color(0xFFFBBF24) else Color.White,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (file.isDirectory) "مجلد" else "${(file.length() / 1024).coerceAtLeast(1)} KB",
                                color = Color(0xFF9CA3AF),
                                fontSize = 8.5.sp
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(installedApps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x12FFFFFF))
                            .clickable { onLaunchExternalApp(app) }
                            .padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsWindowContent(
    selectedWallpaper: String,
    onSelectWallpaper: (String) -> Unit,
    onOpenNotepad: () -> Unit
) {
    val options = remember {
        listOf(
            "wall_win11" to "ويندوز 11",
            "wall_win11_dark" to "ويندوز 11 داكن",
            "wall_win10" to "ويندوز 10",
            "wall_ascent" to "الجبل",
            "wall_autumn" to "الخريف",
            "wall_azul" to "الأزرق"
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تخصيص الخلفية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0x2260A5FA))
                    .clickable { onOpenNotepad() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("المفكرة", color = Color(0xFF93C5FD), fontSize = 9.5.sp)
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            options.forEach { (key, title) ->
                val isSelected = selectedWallpaper == key
                Column(
                    modifier = Modifier
                        .width(88.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isSelected) Color(0x332563EB) else Color(0x14FFFFFF))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF60A5FA) else Color(0x22FFFFFF),
                            shape = RoundedCornerShape(7.dp)
                        )
                        .clickable { onSelectWallpaper(key) }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Win11AssetIcon(
                        name = key,
                        fallbackText = title,
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = title, color = Color.White, fontSize = 9.sp, maxLines = 1)
                }
            }
        }
    }
}

private fun formatCurrentTimeDate(): String {
    return runCatching {
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.US)
        val dateFmt = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        val now = Date()
        "${timeFmt.format(now)}\n${dateFmt.format(now)}"
    }.getOrDefault("12:00 PM\n2026/11/27")
}

private fun loadInstalledAppsSafely(context: Context): List<InstalledAppItem> {
    return runCatching {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolves: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        resolves
            .asSequence()
            .mapNotNull { ri ->
                val pkg = ri.activityInfo?.packageName
                if (pkg.isNullOrBlank() || pkg == context.packageName) return@mapNotNull null
                val label = runCatching { ri.loadLabel(pm).toString() }.getOrDefault(pkg)
                val icon = runCatching { ri.loadIcon(pm) }.getOrNull()
                InstalledAppItem(label = label, packageName = pkg, icon = icon)
            }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .take(80)
            .toList()
    }.getOrDefault(emptyList())
}

private fun launchExternalAppSafely(context: Context, packageName: String) {
    runCatching {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }
}
