package com.balookrd.ibeacon.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Tech / BLE Teal & Sapphire Material 3 Palette
val PrimaryLight = Color(0xFF006879)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFA6EEFF)
val OnPrimaryContainerLight = Color(0xFF001F26)

val SecondaryLight = Color(0xFF4A6268)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFCCE7ED)
val OnSecondaryContainerLight = Color(0xFF051F23)

val TertiaryLight = Color(0xFF535D7E)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFDAE1FF)
val OnTertiaryContainerLight = Color(0xFF0F1A37)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val BackgroundLight = Color(0xFFF6FAFB)
val OnBackgroundLight = Color(0xFF171D1E)
val SurfaceLight = Color(0xFFF6FAFB)
val OnSurfaceLight = Color(0xFF171D1E)
val SurfaceVariantLight = Color(0xFFDBE4E6)
val OnSurfaceVariantLight = Color(0xFF3F484A)
val OutlineLight = Color(0xFF6F797B)
val OutlineVariantLight = Color(0xFFBFC8CA)

// Dark Theme Colors
val PrimaryDark = Color(0xFF53D7F1)
val OnPrimaryDark = Color(0xFF00363F)
val PrimaryContainerDark = Color(0xFF004E5B)
val OnPrimaryContainerDark = Color(0xFFA6EEFF)

val SecondaryDark = Color(0xFFB1CBD1)
val OnSecondaryDark = Color(0xFF1C3439)
val SecondaryContainerDark = Color(0xFF334B50)
val OnSecondaryContainerDark = Color(0xFFCCE7ED)

val TertiaryDark = Color(0xFFBCC5EB)
val OnTertiaryDark = Color(0xFF252F4E)
val TertiaryContainerDark = Color(0xFF3C4565)
val OnTertiaryContainerDark = Color(0xFFDAE1FF)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val BackgroundDark = Color(0xFF0F1415)
val OnBackgroundDark = Color(0xFFDEE3E4)
val SurfaceDark = Color(0xFF0F1415)
val OnSurfaceDark = Color(0xFFDEE3E4)
val SurfaceVariantDark = Color(0xFF3F484A)
val OnSurfaceVariantDark = Color(0xFFBFC8CA)
val OutlineDark = Color(0xFF899294)
val OutlineVariantDark = Color(0xFF3F484A)

// Status colors for Beacon active / inactive
val BeaconActiveGreen = Color(0xFF00A859)
val BeaconActiveContainerLight = Color(0xFFC7F3D6)
val OnBeaconActiveContainerLight = Color(0xFF003916)
val BeaconActiveContainerDark = Color(0xFF005322)
val OnBeaconActiveContainerDark = Color(0xFF74F9A0)

val IBeaconShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
)

@Composable
fun IBeaconTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = IBeaconShapes,
        content = content,
    )
}
