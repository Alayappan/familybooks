package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TealAccent,
    onPrimary = Navy900,
    primaryContainer = Navy700,
    onPrimaryContainer = Color.White,
    secondary = TealAccent,
    onSecondary = Navy900,
    background = NavyDarkBackground,
    onBackground = TextPrimaryDark,
    surface = NavyDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavyDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = Navy600
)

private val LightColorScheme = lightColorScheme(
    primary = Navy900,
    onPrimary = SurfaceLight,
    primaryContainer = Navy100,
    onPrimaryContainer = Navy900,
    secondary = TealDark,
    onSecondary = SurfaceLight,
    secondaryContainer = TealLight,
    onSecondaryContainer = Navy900,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Navy50,
    onSurfaceVariant = TextSecondaryLight,
    outline = InputBorderUnfocused,
    outlineVariant = CardBorderLight,
    error = ExpenseRed,
    onError = Color.White
)

@Composable
fun familyOutlinedTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimaryLight,
    unfocusedTextColor = TextPrimaryLight,
    disabledTextColor = TextSecondaryLight,
    errorTextColor = TextPrimaryLight,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = InputContainerLight,
    disabledContainerColor = Navy50,
    errorContainerColor = Color.White,
    cursorColor = Navy900,
    errorCursorColor = ExpenseRed,
    focusedBorderColor = Navy900,
    unfocusedBorderColor = InputBorderUnfocused,
    errorBorderColor = ExpenseRed,
    focusedLabelColor = Navy900,
    unfocusedLabelColor = TextSecondaryLight,
    errorLabelColor = ExpenseRed,
    focusedPlaceholderColor = TextTertiaryLight,
    unfocusedPlaceholderColor = TextTertiaryLight,
    focusedLeadingIconColor = Navy900,
    unfocusedLeadingIconColor = TextSecondaryLight,
    focusedTrailingIconColor = Navy900,
    unfocusedTrailingIconColor = TextSecondaryLight
)

@Composable
fun familySwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Navy900,
    checkedBorderColor = Navy900,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = Color(0xFFCBD5E1),
    uncheckedBorderColor = InputBorderUnfocused
)

@Composable
fun familyCheckboxColors(): CheckboxColors = CheckboxDefaults.colors(
    checkedColor = Navy900,
    uncheckedColor = TextSecondaryLight,
    checkmarkColor = Color.White
)

@Composable
fun FamilyBooksTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
