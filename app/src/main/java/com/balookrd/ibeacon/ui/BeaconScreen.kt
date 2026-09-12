package com.balookrd.ibeacon.ui

import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.balookrd.ibeacon.beacon.AdvertiseRate
import com.balookrd.ibeacon.beacon.BeaconConfig
import com.balookrd.ibeacon.beacon.ConfigProblem
import com.balookrd.ibeacon.beacon.TxPower
import com.balookrd.ibeacon.beacon.describe
import com.balookrd.ibeacon.keepalive.KeepAliveHelper
import kotlinx.coroutines.delay

@Composable
fun BeaconScreen(
    viewModel: BeaconViewModel,
    onRequestPermissions: () -> Unit,
    onOpenIntent: (Intent) -> Unit,
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val checklist by viewModel.checklist.collectAsStateWithLifecycle()
    val shouldRun by viewModel.shouldRun.collectAsStateWithLifecycle()
    val savedConfig by viewModel.savedConfig.collectAsStateWithLifecycle()

    var showProblems by remember { mutableLongStateOf(0L) }
    val problems = if (showProblems > 0L) form.problems else emptySet()
    val hasUnapplied = shouldRun && form.toConfig() != savedConfig
    val focusManager = LocalFocusManager.current

    BeaconScreenContent(
        form = form,
        statusAdvertising = status.advertising,
        shouldRun = shouldRun,
        failureText = status.failure?.describe(),
        config = status.config,
        advertisingSinceElapsedMs = status.advertisingSinceElapsedMs,
        revivals = status.revivals,
        checklist = checklist,
        problems = problems,
        hasUnapplied = hasUnapplied,
        onStartOrApply = {
            if (!checklist.advertisePermissionGranted) {
                onRequestPermissions()
            } else if (!viewModel.startOrApply()) {
                showProblems = SystemClock.elapsedRealtime()
            }
        },
        onStop = viewModel::stop,
        onUuidChange = viewModel::onUuidChange,
        onMajorChange = viewModel::onMajorChange,
        onMinorChange = viewModel::onMinorChange,
        onMeasuredPowerChange = viewModel::onMeasuredPowerChange,
        onRandomizeUuid = viewModel::randomizeUuid,
        onRateChange = viewModel::onRateChange,
        onTxPowerChange = viewModel::onTxPowerChange,
        onRequestPermissions = onRequestPermissions,
        onOpenIntent = onOpenIntent,
        onClearFocus = { focusManager.clearFocus() },
    )
}

@Composable
private fun BeaconScreenContent(
    form: FormState,
    statusAdvertising: Boolean,
    shouldRun: Boolean,
    failureText: String?,
    config: BeaconConfig?,
    advertisingSinceElapsedMs: Long?,
    revivals: Int,
    checklist: Checklist,
    problems: Set<ConfigProblem>,
    hasUnapplied: Boolean,
    onStartOrApply: () -> Unit,
    onStop: () -> Unit,
    onUuidChange: (String) -> Unit,
    onMajorChange: (String) -> Unit,
    onMinorChange: (String) -> Unit,
    onMeasuredPowerChange: (String) -> Unit,
    onRandomizeUuid: () -> Unit,
    onRateChange: (AdvertiseRate) -> Unit,
    onTxPowerChange: (TxPower) -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenIntent: (Intent) -> Unit,
    onClearFocus: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClearFocus() })
            }
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero Status Card
        HeroStatusCard(
            advertising = statusAdvertising,
            shouldRun = shouldRun,
            failureText = failureText,
            config = config,
            advertisingSinceElapsedMs = advertisingSinceElapsedMs,
            revivals = revivals,
        )

        // Action Buttons Row
        ActionButtonsRow(
            shouldRun = shouldRun,
            hasUnapplied = hasUnapplied,
            onStartOrApply = onStartOrApply,
            onStop = onStop,
        )

        // Unapplied or Problem Alert Card
        AnimatedVisibility(
            visible = problems.isNotEmpty() || hasUnapplied,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = if (problems.isNotEmpty()) {
                            "Маяк не перезапущен: проверьте подсвеченные поля ниже"
                        } else {
                            "Параметры изменены, но ещё не применены. Нажмите «Применить»."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Section: Identity
        IdentityCard(
            form = form,
            problems = problems,
            onUuidChange = onUuidChange,
            onMajorChange = onMajorChange,
            onMinorChange = onMinorChange,
            onMeasuredPowerChange = onMeasuredPowerChange,
            onRandomizeUuid = onRandomizeUuid,
        )

        // Section: Radio & Tx Power
        RadioCard(
            rate = form.rate,
            txPower = form.txPower,
            onRateChange = onRateChange,
            onTxPowerChange = onTxPowerChange,
        )

        // Section: Keep Alive Checklist
        KeepAliveCard(
            checklist = checklist,
            onRequestPermissions = onRequestPermissions,
            onOpenIntent = onOpenIntent,
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HeroStatusCard(
    advertising: Boolean,
    shouldRun: Boolean,
    failureText: String?,
    config: BeaconConfig?,
    advertisingSinceElapsedMs: Long?,
    revivals: Int,
) {
    val containerColor = when {
        advertising -> MaterialTheme.colorScheme.primaryContainer
        failureText != null || (!advertising && shouldRun) -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        advertising -> MaterialTheme.colorScheme.onPrimaryContainer
        failureText != null || (!advertising && shouldRun) -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Radar pulse or static icon
            BeaconRadarIcon(advertising = advertising, isError = failureText != null || (!advertising && shouldRun))

            // Main Status Title
            Text(
                text = when {
                    advertising -> "Маяк в эфире"
                    shouldRun -> "Запуск не удался"
                    else -> "Маяк остановлен"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            // Uptime Counter Badge
            if (advertising && advertisingSinceElapsedMs != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Время работы",
                            modifier = Modifier.size(16.dp),
                        )
                        Uptime(advertisingSinceElapsedMs)
                    }
                }
            }

            // Error or OEM warning details
            if (failureText != null) {
                Text(
                    text = failureText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            } else if (!advertising && shouldRun) {
                Text(
                    text = "Оболочка телефона не дала запустить сервис в фоне. Включите автозапуск в блоке «Живучесть» ниже.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            // Active broadcast parameters summary
            if (advertising && config != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = config.uuid,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "major: ${config.major}  •  minor: ${config.minor}  •  ${config.measuredPower} dBm",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            // Revivals Chip
            if (revivals > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "Автовосстановлений: $revivals",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun BeaconRadarIcon(advertising: Boolean, isError: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "beaconPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseScale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseAlpha",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(72.dp),
    ) {
        if (advertising) {
            // Pulsing radio wave rings
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(pulseScale)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                        shape = CircleShape,
                    )
            )
        }

        // Center Icon Container
        val iconBg = when {
            advertising -> MaterialTheme.colorScheme.primary
            isError -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
        }
        val iconTint = when {
            advertising -> MaterialTheme.colorScheme.onPrimary
            isError -> MaterialTheme.colorScheme.onError
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .background(iconBg, CircleShape),
        ) {
            Icon(
                imageVector = when {
                    advertising -> Icons.Default.Sensors
                    isError -> Icons.Default.ErrorOutline
                    else -> Icons.Default.SensorsOff
                },
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    shouldRun: Boolean,
    hasUnapplied: Boolean,
    onStartOrApply: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onStartOrApply,
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
            enabled = !shouldRun || hasUnapplied,
            shape = MaterialTheme.shapes.medium,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        ) {
            Icon(
                imageVector = if (hasUnapplied) Icons.Default.Refresh else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (hasUnapplied) "Применить" else "Старт",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        FilledTonalButton(
            onClick = onStop,
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
            enabled = shouldRun,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Стоп",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun Uptime(sinceElapsedMs: Long) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(sinceElapsedMs) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            delay(1_000)
        }
    }
    val seconds = ((now - sinceElapsedMs) / 1000).coerceAtLeast(0)
    Text(
        text = "%02d:%02d:%02d".format(seconds / 3600, (seconds / 60) % 60, seconds % 60),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
    )
}

@Composable
private fun IdentityCard(
    form: FormState,
    problems: Set<ConfigProblem>,
    onUuidChange: (String) -> Unit,
    onMajorChange: (String) -> Unit,
    onMinorChange: (String) -> Unit,
    onMeasuredPowerChange: (String) -> Unit,
    onRandomizeUuid: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val doneOptions = KeyboardOptions(imeAction = ImeAction.Done)
    val numberOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
    val doneActions = KeyboardActions(onDone = { focusManager.clearFocus() })

    ModernSectionCard(
        title = "Идентификатор маяка",
        icon = Icons.Default.Fingerprint,
    ) {
        OutlinedTextField(
            value = form.uuid,
            onValueChange = onUuidChange,
            label = { Text("Proximity UUID") },
            singleLine = true,
            isError = ConfigProblem.INVALID_UUID in problems,
            supportingText = {
                if (ConfigProblem.INVALID_UUID in problems) {
                    Text("Требуется UUID: 36 символов с дефисами или 32 hex-символа")
                } else {
                    Text("Уникальный идентификатор маяка")
                }
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(form.uuid))
                            Toast.makeText(context, "UUID скопирован", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Скопировать UUID",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onRandomizeUuid) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Сгенерировать случайный UUID",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            keyboardOptions = doneOptions,
            keyboardActions = doneActions,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = form.major,
                onValueChange = onMajorChange,
                label = { Text("major") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                singleLine = true,
                isError = ConfigProblem.MAJOR_OUT_OF_RANGE in problems,
                keyboardOptions = numberOptions,
                keyboardActions = doneActions,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = form.minor,
                onValueChange = onMinorChange,
                label = { Text("minor") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                singleLine = true,
                isError = ConfigProblem.MINOR_OUT_OF_RANGE in problems,
                keyboardOptions = numberOptions,
                keyboardActions = doneActions,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f),
            )
        }

        if (ConfigProblem.MAJOR_OUT_OF_RANGE in problems || ConfigProblem.MINOR_OUT_OF_RANGE in problems) {
            Text(
                text = "major и minor должны быть числами от 0 до 65535",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        OutlinedTextField(
            value = form.measuredPower,
            onValueChange = onMeasuredPowerChange,
            label = { Text("Measured power (1 метр)") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.SignalCellularAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            singleLine = true,
            isError = ConfigProblem.MEASURED_POWER_OUT_OF_RANGE in problems,
            supportingText = {
                Text(
                    if (ConfigProblem.MEASURED_POWER_OUT_OF_RANGE in problems) {
                        "Значение должно быть от -127 до 0 dBm"
                    } else {
                        "RSSI на расстоянии 1 м (обычно -59 dBm) для расчёта дистанции"
                    }
                )
            },
            keyboardOptions = numberOptions,
            keyboardActions = doneActions,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadioCard(
    rate: AdvertiseRate,
    txPower: TxPower,
    onRateChange: (AdvertiseRate) -> Unit,
    onTxPowerChange: (TxPower) -> Unit,
) {
    ModernSectionCard(
        title = "Параметры радиосигнала",
        icon = Icons.Default.Sensors,
    ) {
        Text(
            text = "Периодичность трансляции",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )

        // Material 3 SingleChoiceSegmentedButtonRow
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            AdvertiseRate.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = rate == option,
                    onClick = { onRateChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = AdvertiseRate.entries.size,
                    ),
                    label = {
                        Text("~${option.approxIntervalMs} мс")
                    },
                )
            }
        }

        Text(
            text = when (rate) {
                AdvertiseRate.LOW_LATENCY -> "Максимальная частота (~100 мс) — быстрый отклик, повышенный расход."
                AdvertiseRate.BALANCED -> "Сбалансированный режим (~250 мс) — компромисс энергопотребления."
                AdvertiseRate.LOW_POWER -> "Энергосберегающий режим (~1000 мс) — минимальный расход батареи."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "Мощность передатчика (Tx Power)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            TxPower.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = txPower == option,
                    onClick = { onTxPowerChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = TxPower.entries.size,
                    ),
                    icon = {},
                    label = {
                        Text(
                            text = option.label(),
                            maxLines = 1,
                            softWrap = false,
                        )
                    },
                )
            }
        }

        Text(
            text = when (txPower) {
                TxPower.ULTRA_LOW -> "Минимальная мощность — наименьший радиус и расход батареи."
                TxPower.LOW -> "Низкая мощность — для обнаружения в пределах комнаты."
                TxPower.MEDIUM -> "Средняя мощность — сбалансированная дальность сигнала."
                TxPower.HIGH -> "Максимальная мощность — наибольший охват радиуса вещания."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KeepAliveCard(
    checklist: Checklist,
    onRequestPermissions: () -> Unit,
    onOpenIntent: (Intent) -> Unit,
) {
    val context = LocalContext.current

    val totalItems = if (checklist.hasAutostartScreen) 6 else 5
    val completedItems = listOf(
        checklist.advertisePermissionGranted,
        checklist.notificationsEnabled,
        checklist.batteryUnrestricted,
        checklist.exactAlarmsAllowed,
        checklist.bluetoothOn,
    ).count { it }

    ModernSectionCard(
        title = "Живучесть и фоновый режим",
        icon = Icons.Default.Shield,
    ) {
        if (!checklist.advertisingSupported) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Это устройство не поддерживает BLE-передатчик — маяк вещать не сможет.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // Progress summary
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Готовность системы",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "$completedItems из 5 настроено",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (completedItems == 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold,
                )
            }
            LinearProgressIndicator(
                progress = { completedItems / 5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Checklist items
        ModernChecklistRow(
            ok = checklist.advertisePermissionGranted,
            icon = Icons.Default.Bluetooth,
            title = "Разрешение на BLE-рекламу",
            description = "Необходимо для передачи радиопакетов маяка",
            actionLabel = "Выдать",
            onAction = onRequestPermissions,
        )

        ModernChecklistRow(
            ok = checklist.notificationsEnabled,
            icon = Icons.Default.Notifications,
            title = "Уведомления службы",
            description = "Удерживает Foreground Service в памяти системы",
            actionLabel = "Настроить",
            onAction = { onOpenIntent(KeepAliveHelper.appSettingsIntent(context)) },
        )

        ModernChecklistRow(
            ok = checklist.batteryUnrestricted,
            icon = Icons.Default.BatteryFull,
            title = "Без ограничений батареи",
            description = "Предотвращает принудительное засыпание в режиме Doze",
            actionLabel = "Отключить",
            onAction = { onOpenIntent(KeepAliveHelper.batteryOptimizationIntent(context)) },
        )

        ModernChecklistRow(
            ok = checklist.exactAlarmsAllowed,
            icon = Icons.Default.Timer,
            title = "Точные будильники",
            description = "Обеспечивают работу регулярного сторожа маяка",
            actionLabel = "Разрешить",
            onAction = { KeepAliveHelper.exactAlarmSettingsIntent(context)?.let(onOpenIntent) },
        )

        if (checklist.hasAutostartScreen) {
            ModernChecklistRow(
                ok = false,
                neutral = true,
                icon = Icons.Default.PowerSettingsNew,
                title = "Автозапуск ${checklist.vendorLabel.orEmpty()}".trim(),
                description = "Фирменные оболочки убивают фон. Включите автозапуск вручную.",
                actionLabel = "Открыть",
                onAction = { KeepAliveHelper.autostartIntent(context)?.let(onOpenIntent) },
            )
        }

        ModernChecklistRow(
            ok = checklist.bluetoothOn,
            icon = Icons.Default.Bluetooth,
            title = "Модуль Bluetooth",
            description = "При включении Bluetooth маяк поднимется автоматически",
            actionLabel = "Включить",
            onAction = { onOpenIntent(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) },
        )
    }
}

@Composable
private fun ModernChecklistRow(
    ok: Boolean,
    neutral: Boolean = false,
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val statusBg = when {
        ok -> MaterialTheme.colorScheme.primaryContainer
        neutral -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val statusTint = when {
        ok -> MaterialTheme.colorScheme.primary
        neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.error
    }

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
        ),
        leadingContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(statusBg, CircleShape),
            ) {
                Icon(
                    imageVector = when {
                        ok -> Icons.Default.Check
                        neutral -> Icons.Default.Info
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = statusTint,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            if (!ok) {
                FilledTonalButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.height(32.dp),
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    )
}

@Composable
private fun ModernSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            content()
        }
    }
}

private fun TxPower.label(): String = when (this) {
    TxPower.ULTRA_LOW -> "Мин."
    TxPower.LOW -> "Низк."
    TxPower.MEDIUM -> "Сред."
    TxPower.HIGH -> "Макс."
}

// Previews for Android Studio and Testing
@Preview(showBackground = true, name = "Light Theme - Active")
@Composable
private fun BeaconScreenPreviewLight() {
    IBeaconTheme(darkTheme = false) {
        BeaconScreenContent(
            form = FormState(
                uuid = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0",
                major = "1",
                minor = "100",
                measuredPower = "-59",
                rate = AdvertiseRate.BALANCED,
                txPower = TxPower.MEDIUM,
            ),
            statusAdvertising = true,
            shouldRun = true,
            failureText = null,
            config = BeaconConfig(
                uuid = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0",
                major = 1,
                minor = 100,
                measuredPower = -59,
                rate = AdvertiseRate.BALANCED,
                txPower = TxPower.MEDIUM,
            ),
            advertisingSinceElapsedMs = SystemClock.elapsedRealtime() - 365_000,
            revivals = 2,
            checklist = Checklist(
                advertisePermissionGranted = true,
                notificationsEnabled = true,
                batteryUnrestricted = true,
                exactAlarmsAllowed = true,
                bluetoothOn = true,
                advertisingSupported = true,
                vendorLabel = "Samsung",
                hasAutostartScreen = false,
            ),
            problems = emptySet(),
            hasUnapplied = false,
            onStartOrApply = {},
            onStop = {},
            onUuidChange = {},
            onMajorChange = {},
            onMinorChange = {},
            onMeasuredPowerChange = {},
            onRandomizeUuid = {},
            onRateChange = {},
            onTxPowerChange = {},
            onRequestPermissions = {},
            onOpenIntent = {},
            onClearFocus = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark Theme - Stopped")
@Composable
private fun BeaconScreenPreviewDark() {
    IBeaconTheme(darkTheme = true) {
        BeaconScreenContent(
            form = FormState(
                uuid = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0",
                major = "1",
                minor = "100",
                measuredPower = "-59",
                rate = AdvertiseRate.BALANCED,
                txPower = TxPower.MEDIUM,
            ),
            statusAdvertising = false,
            shouldRun = false,
            failureText = null,
            config = null,
            advertisingSinceElapsedMs = null,
            revivals = 0,
            checklist = Checklist(
                advertisePermissionGranted = true,
                notificationsEnabled = true,
                batteryUnrestricted = false,
                exactAlarmsAllowed = true,
                bluetoothOn = true,
                advertisingSupported = true,
                vendorLabel = "Xiaomi",
                hasAutostartScreen = true,
            ),
            problems = emptySet(),
            hasUnapplied = false,
            onStartOrApply = {},
            onStop = {},
            onUuidChange = {},
            onMajorChange = {},
            onMinorChange = {},
            onMeasuredPowerChange = {},
            onRandomizeUuid = {},
            onRateChange = {},
            onTxPowerChange = {},
            onRequestPermissions = {},
            onOpenIntent = {},
            onClearFocus = {},
        )
    }
}

