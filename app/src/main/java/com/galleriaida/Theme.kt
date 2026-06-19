package com.galleriaida.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

val SkyBlue = Color(0xFFB3E5FC)
val SoftPurple = Color(0xFFE1BEE7)
val LemonYellow = Color(0xFFFFF9C4)
val MintGreen = Color(0xFFC8E6C9)
val PeachOrange = Color(0xFFFFCCBC)
val StarGold = Color(0xFFFFD700)
val DeepPurple = Color(0xFF6A1B9A)
val DarkText = Color(0xFF212121)
val MedText = Color(0xFF616161)
val White = Color(0xFFFFFFFF)
val ErrorRed = Color(0xFFE53935)
val SuccessGreen = Color(0xFF43A047)
val CardBg = Color(0xFFF3E5F5)
val ButtonPrimary = Color(0xFF7B1FA2)
val ButtonSecondary = Color(0xFF00897B)
val DisabledGray = Color(0xFFBDBDBD)
val TournamentAccent = Color(0xFFFB8C00)  // warm orange — distinct from other game accents

private val KidsColorScheme = lightColorScheme(
    primary = ButtonPrimary,
    onPrimary = White,
    secondary = ButtonSecondary,
    onSecondary = White,
    background = Color(0xFFFCF4FF),
    onBackground = DarkText,
    surface = White,
    onSurface = DarkText,
    error = ErrorRed,
    onError = White
)

val KidsTypography = Typography(
    displayLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = DarkText),
    titleLarge = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = DarkText),
    titleMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = DarkText),
    bodyLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Normal, color = DarkText),
    bodyMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal, color = MedText),
    labelLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White),
)

@Composable
fun KidsAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KidsColorScheme,
        typography = KidsTypography,
        content = content
    )
}
