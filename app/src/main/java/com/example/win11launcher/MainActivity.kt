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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.OptIn
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
    val offsetX: Float = 90f,
    val offsetY: Float = 45f,
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
        enableImmersiveMode()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Win11LauncherRoot(
                        onExitLauncher = { finish() }
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }

    private fun enableImmersiveMode() {
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

@Composable
fun drawableBitmapOrNull(context: Context, name: String): ImageBitmap? {
    return remember(context, name) {
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (resId != 0) {
            runCatching {
                val d = androidx.core.content.ContextCompat.getDrawable(context, resId)
                d?.toBitmap()?.asImageBitmap()
            }.getOrNull()
        } else {
            null
        }
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
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1F2937))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackText.take(2),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun Win11LauncherRoot(onExitLauncher: () -> Unit) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val wallpapers = remember {
        listOf(
            "wall_win11" to "ويندوز 11 الرسمي",
            "wall_win11_dark" to "ويندوز 11 داكن",
            "wall_win10" to "ويندوز 10 كلاسيك",
            "wall_ascent" to "قمة الجبل",
            "wall_autumn" to "الخريف",
            "wall_azul" to "الأزرق"
        )
    }
    var selectedWallpaper by remember { mutableStateOf("wall_win11") }
    var searchQuery by remember { mutableStateOf("") }
    var showStartMenu by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf<Offset?>(null) }
    var showQuickSettings by remember { mutableStateOf(false) }
    var showCalendarPopup by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var zCounter by remember { mutableFloatStateOf(20f) }

    val windows = remember { mutableStateListOf<DesktopWindow>() }

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
            val offsetShift = (windows.size % 5) * 26f
            windows.add(
                DesktopWindow(
                    id = id,
                    title = title,
                    iconName = iconName,
                    kind = kind,
                    url = url,
                    offsetX = 70f + offsetShift,
                    offsetY = 36f + offsetShift,
                    isMaximized = false,
                    isMinimized = false,
                    zIndex = zCounter
                )
            )
        }
        showStartMenu = false
        showContextMenu = null
        showQuickSettings = false
        showCalendarPopup = false
    }

    // حالة الصوت
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var currentVolume by remember {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    LaunchedEffect(Unit) {
        while (true) {
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            delay(600L)
        }
    }
    val volumePercent = ((currentVolume.toFloat() / maxVolume.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
    val volumeIconName = when {
        currentVolume == 0 -> "icon_vol_mute"
        volumePercent <= 25 -> "icon_vol_low"
        volumePercent <= 66 -> "icon_vol_mid"
        else -> "icon_vol_high"
    }

    // حالة البطارية
    var batteryLevel by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
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
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    // الوقت والتاريخ
    var nowText by remember { mutableStateOf(formatCurrentTimeDate()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowText = formatCurrentTimeDate()
            delay(15_000L)
        }
    }

    // التطبيقات المثبتة على الهاتف
    val installedApps = remember(refreshTick) {
        loadInstalledApps(context)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A121D))) {
        // خلفية سطح المكتب
        val wallBmp = drawableBitmapOrNull(context, selectedWallpaper)
        if (wallBmp != null) {
            Image(
                bitmap = wallBmp,
                contentDescription = "خلفية سطح المكتب",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // طبقة سطح المكتب التفاعلية (ضغط عادي لإغلاق القوائم، ضغط مطوّل لقائمة الخيارات)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 52.dp)
                .combinedClickable(
                    onClick = {
                        showStartMenu = false
                        showContextMenu = null
                        showQuickSettings = false
                        showCalendarPopup = false
                        showVolumeSlider = false
                    },
                    onLongClick = {
                        showStartMenu = false
                        showQuickSettings = false
                        showCalendarPopup = false
                        showContextMenu = Offset(420f, 180f)
                    }
                )
        ) {
            // أيقونات سطح المكتب الأساسية
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
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
                        openOrFocusWindow(
                            "win_browser",
                            "متصفح الويب",
                            "icon_chrome",
                            WindowKind.BROWSER,
                            "https://www.google.com"
                        )
                    }
                )
                DesktopIconItem(
                    iconName = "icon_youtube",
                    label = "يوتيوب",
                    onClick = {
                        openOrFocusWindow(
                            "win_yt",
                            "يوتيوب",
                            "icon_youtube",
                            WindowKind.BROWSER,
                            "https://m.youtube.com"
                        )
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

            // النوافذ الحرة داخل اللانشر (تحريك + تصغير + تكبير/ملء شاشة + إغلاق)
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
                                    offsetX = (win.offsetX + dx).coerceIn(0f, 900f),
                                    offsetY = (win.offsetY + dy).coerceIn(0f, 450f)
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
                        onSelectWallpaper = { selectedWallpaper = it },
                        installedApps = installedApps,
                        onLaunchExternalApp = { app ->
                            launchExternalApp(context, app.packageName)
                        }
                    )
                }
            }

            // قائمة السياق (ضغط مطوّل على سطح المكتب)
            showContextMenu?.let { pos ->
                Surface(
                    modifier = Modifier
                        .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                        .width(210.dp)
                        .zIndex(90f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xE620242C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        ContextMenuRow("تحديث") {
                            refreshTick += 1
                            showContextMenu = null
                        }
                        ContextMenuRow("فتح هذا الكمبيوتر") {
                            openOrFocusWindow("win_pc", "هذا الكمبيوتر", "icon_computer", WindowKind.THIS_PC)
                        }
                        ContextMenuRow("متصفح داخل نافذة") {
                            openOrFocusWindow("win_browser", "متصفح الويب", "icon_chrome", WindowKind.BROWSER)
                        }
                        HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        ContextMenuRow("تخصيص سطح المكتب") {
                            openOrFocusWindow("win_settings", "الإعدادات والتخصيص", "icon_settings", WindowKind.SETTINGS)
                        }
                    }
                }
            }

            // قائمة Start
            if (showStartMenu) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .width(560.dp)
                        .height(430.dp)
                        .zIndex(95f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xF01B1F27),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "التطبيقات المثبتة (${installedApps.size})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "الإعدادات",
                                    color = Color(0xFF60A5FA),
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            openOrFocusWindow(
                                                "win_settings",
                                                "الإعدادات والتخصيص",
                                                "icon_settings",
                                                WindowKind.SETTINGS
                                            )
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            val filteredApps = remember(installedApps, searchQuery) {
                                if (searchQuery.isBlank()) installedApps
                                else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
                            }
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredApps, key = { it.packageName }) { app ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x14FFFFFF))
                                            .clickable {
                                                showStartMenu = false
                                                launchExternalApp(context, app.packageName)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val appBmp = remember(app.packageName) {
                                            app.icon?.toBitmap(64, 64)?.asImageBitmap()
                                        }
                                        if (appBmp != null) {
                                            Image(
                                                bitmap = appBmp,
                                                contentDescription = app.label,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = app.label,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Win11AssetIcon(
                                    name = "icon_account",
                                    fallbackText = "PC",
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("This PC", color = Color.White, fontSize = 13.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "إغلاق اللانشر",
                                    color = Color(0xFFF87171),
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onExitLauncher() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // سلايدر الصوت السريع
            if (showVolumeSlider) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 8.dp)
                        .width(240.dp)
                        .zIndex(96f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xF21E232C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("مستوى الصوت", color = Color.White, fontSize = 12.sp)
                            Text("$volumePercent%", color = Color(0xFF60A5FA), fontSize = 12.sp)
                        }
                        Slider(
                            value = currentVolume.toFloat(),
                            onValueChange = { newVal ->
                                val target = newVal.roundToInt().coerceIn(0, maxVolume)
                                currentVolume = target
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                            },
                            valueRange = 0f..maxVolume.toFloat()
                        )
                    }
                }
            }

            // نافذة المركز السريع (صوت + بطارية + شبكة)
            if (showQuickSettings) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 8.dp)
                        .width(310.dp)
                        .zIndex(96f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xF21E232C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("المركز السريع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuickTile("الواي فاي", "icon_network", active = true)
                            QuickTile("الإعدادات", "icon_settings", active = false) {
                                openOrFocusWindow("win_settings", "الإعدادات", "icon_settings", WindowKind.SETTINGS)
                            }
                        }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الصوت ($volumePercent%)", color = Color.White, fontSize = 12.sp)
                                Text(
                                    if (isCharging) "البطارية: $batteryLevel% (يشحن)" else "البطارية: $batteryLevel%",
                                    color = if (isCharging) Color(0xFF4ADE80) else Color(0xFFE5E7EB),
                                    fontSize = 12.sp
                                )
                            }
                            Slider(
                                value = currentVolume.toFloat(),
                                onValueChange = { newVal ->
                                    val target = newVal.roundToInt().coerceIn(0, maxVolume)
                                    currentVolume = target
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                                },
                                valueRange = 0f..maxVolume.toFloat()
                            )
                        }
                    }
                }
            }

            // نافذة الوقت والتاريخ
            if (showCalendarPopup) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 8.dp)
                        .width(260.dp)
                        .zIndex(96f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xF21E232C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("التاريخ والوقت", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(nowText, color = Color(0xFF93C5FD), fontSize = 13.sp)
                    }
                }
            }
        }

        // شريط المهام السفلي (Taskbar)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(52.dp)
                .zIndex(100f),
            color = Color(0xEB141820),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // يسار شريط المهام (في RTL: الوقت، البطارية، الصوت، الشبكة)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // التاريخ والوقت
                    Text(
                        text = nowText,
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                showCalendarPopup = !showCalendarPopup
                                showQuickSettings = false
                                showVolumeSlider = false
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )

                    // مؤشر البطارية الديناميكي (نسبة + حالة شحن)
                    DynamicBatteryIndicator(
                        level = batteryLevel,
                        isCharging = isCharging,
                        modifier = Modifier.clickable {
                            showQuickSettings = !showQuickSettings
                            showCalendarPopup = false
                            showVolumeSlider = false
                        }
                    )

                    // أيقونة الصوت الديناميكية المتغيرة حسب المستوى
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                showVolumeSlider = !showVolumeSlider
                                showQuickSettings = false
                                showCalendarPopup = false
                            }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Win11AssetIcon(
                            name = volumeIconName,
                            fallbackText = "VOL",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // أيقونة الشبكة
                    Win11AssetIcon(
                        name = "icon_network",
                        fallbackText = "NET",
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                showQuickSettings = !showQuickSettings
                                showCalendarPopup = false
                                showVolumeSlider = false
                            }
                    )
                }

                // وسط شريط المهام: زر Start + بحث + نوافذ مفتوحة/مصغّرة
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (showStartMenu) Color(0x3360A5FA) else Color(0x14FFFFFF))
                            .clickable {
                                showStartMenu = !showStartMenu
                                showContextMenu = null
                                showQuickSettings = false
                                showCalendarPopup = false
                                showVolumeSlider = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Win11AssetIcon(
                            name = "app_icon",
                            fallbackText = "W11",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.isNotBlank()) showStartMenu = true
                        },
                        placeholder = { Text("بحث", fontSize = 12.sp, color = Color(0xFF9CA3AF)) },
                        singleLine = true,
                        modifier = Modifier.width(170.dp).height(38.dp),
                        shape = RoundedCornerShape(18.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                    )

                    // عرض النوافذ المفتوحة أو المصغّرة للتبديل السريع
                    windows.forEachIndexed { index, w ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!w.isMinimized) Color(0x2860A5FA) else Color(0x14FFFFFF))
                                .clickable {
                                    zCounter += 1f
                                    windows[index] = w.copy(isMinimized = false, zIndex = zCounter)
                                }
                                .padding(horizontal = 8.dp)
                        ) {
                            Win11AssetIcon(
                                name = w.iconName,
                                fallbackText = w.title,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = w.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // يمين شريط المهام: زر إظهار سطح المكتب (تصغير الكل)
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .fillMaxHeight()
                        .background(Color(0x5593C5FD))
                        .clickable {
                            for (i in windows.indices) {
                                windows[i] = windows[i].copy(isMinimized = true)
                            }
                            showStartMenu = false
                        }
                )
            }
        }
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
            .width(78.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Win11AssetIcon(
            name = iconName,
            fallbackText = label,
            modifier = Modifier.size(38.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
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
            .clip(RoundedCornerShape(6.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (isCharging) "$level%⚡" else "$level%",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Canvas(modifier = Modifier.width(22.dp).height(12.dp)) {
            val bodyWidth = size.width - 3.dp.toPx()
            val bodyHeight = size.height
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset.Zero,
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.3.dp.toPx())
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(bodyWidth, bodyHeight * 0.3f),
                size = Size(2.dp.toPx(), bodyHeight * 0.4f),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            val pad = 2.dp.toPx()
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
fun ContextMenuRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 13.sp)
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
            .width(135.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (active) Color(0xFF2563EB) else Color(0x22FFFFFF),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Win11AssetIcon(name = iconName, fallbackText = title, modifier = Modifier.size(18.dp))
            Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalLayoutApi::class)
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
    onLaunchExternalApp: (InstalledAppItem) -> Unit
) {
    val modifier = if (window.isMaximized) {
        Modifier
            .fillMaxSize()
            .zIndex(window.zIndex)
    } else {
        Modifier
            .width(640.dp)
            .height(360.dp)
            .offset { IntOffset(window.offsetX.roundToInt(), window.offsetY.roundToInt()) }
            .zIndex(window.zIndex)
    }

    Surface(
        modifier = modifier.clickable(onClick = onFocus),
        shape = if (window.isMaximized) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
        color = Color(0xF5181C24),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x38FFFFFF))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // شريط العنوان القابل للسحب والتحكم (تصغير / تكبير / إغلاق)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
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
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Win11AssetIcon(
                        name = window.iconName,
                        fallbackText = window.title,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = window.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WindowControlBtn("—", onClick = onMinimize)
                    WindowControlBtn(if (window.isMaximized) "❐" else "□", onClick = onToggleMaximize)
                    WindowControlBtn("✕", isClose = true, onClick = onClose)
                }
            }

            // محتوى النافذة حسب النوع
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                when (window.kind) {
                    WindowKind.THIS_PC -> ThisPcWindowContent(
                        installedApps = installedApps,
                        onLaunchExternalApp = onLaunchExternalApp
                    )
                    WindowKind.SETTINGS -> SettingsWindowContent(
                        selectedWallpaper = selectedWallpaper,
                        onSelectWallpaper = onSelectWallpaper
                    )
                    WindowKind.BROWSER -> {
                        var address by remember(window.url) { mutableStateOf(window.url) }
                        var loadUrl by remember(window.url) { mutableStateOf(window.url) }
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                                )
                                Button(
                                    onClick = {
                                        val clean = if (address.startsWith("http")) address else "https://$address"
                                        loadUrl = clean
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    Text("ذهاب", fontSize = 12.sp, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            AndroidView(
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        webViewClient = WebViewClient()
                                        loadUrl(loadUrl)
                                    }
                                },
                                update = { wv ->
                                    if (wv.url != loadUrl) wv.loadUrl(loadUrl)
                                }
                            )
                        }
                    }
                    WindowKind.NOTEPAD -> {
                        var noteText by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = { Text("اكتب ملاحظاتك هنا...", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp)
                        )
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
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isClose) Color(0x33EF4444) else Color(0x18FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThisPcWindowContent(
    installedApps: List<InstalledAppItem>,
    onLaunchExternalApp: (InstalledAppItem) -> Unit
) {
    val storageInfo = remember {
        runCatching {
            val path: File = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val totalGb = stat.totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            val availGb = stat.availableBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            String.format(Locale.US, "%.1f جيجابايت متاحة من %.1f جيجابايت", availGb, totalGb)
        }.getOrDefault("التخزين الداخلي (:C)")
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x14FFFFFF))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Win11AssetIcon(name = "icon_disk", fallbackText = "C:", modifier = Modifier.size(34.dp))
            Column {
                Text("القرص المحلي (:C)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(storageInfo, color = Color(0xFF9CA3AF), fontSize = 12.sp)
            }
        }

        Text("التطبيقات المثبتة", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            installedApps.take(12).forEach { app ->
                Column(
                    modifier = Modifier
                        .width(84.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x12FFFFFF))
                        .clickable { onLaunchExternalApp(app) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val bmp = remember(app.packageName) {
                        app.icon?.toBitmap(64, 64)?.asImageBitmap()
                    }
                    if (bmp != null) {
                        Image(bitmap = bmp, contentDescription = app.label, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = app.label,
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsWindowContent(
    selectedWallpaper: String,
    onSelectWallpaper: (String) -> Unit
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("تخصيص الخلفية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { (key, title) ->
                val isSelected = selectedWallpaper == key
                Column(
                    modifier = Modifier
                        .width(135.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0x332563EB) else Color(0x14FFFFFF))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF60A5FA) else Color(0x22FFFFFF),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectWallpaper(key) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Win11AssetIcon(
                        name = key,
                        fallbackText = title,
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = title, color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun formatCurrentTimeDate(): String {
    val timeFmt = SimpleDateFormat("hh:mm a", Locale("ar"))
    val dateFmt = SimpleDateFormat("yyyy/MM/dd", Locale.US)
    val now = Date()
    return "${timeFmt.format(now)}\n${dateFmt.format(now)}"
}

private fun loadInstalledApps(context: Context): List<InstalledAppItem> {
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

    return resolves
        .filter { it.activityInfo?.packageName != context.packageName }
        .map { ri ->
            InstalledAppItem(
                label = ri.loadLabel(pm).toString(),
                packageName = ri.activityInfo.packageName,
                icon = runCatching { ri.loadIcon(pm) }.getOrNull()
            )
        }
        .sortedBy { it.label.lowercase(Locale.getDefault()) }
}

private fun launchExternalApp(context: Context, packageName: String) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(launchIntent) }
}
