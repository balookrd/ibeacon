package com.balookrd.ibeacon.beacon

import android.bluetooth.le.AdvertiseCallback

/** Human-readable reason, shared by the notification and the UI. */
fun BeaconFailure.describe(): String = when (this) {
    BeaconFailure.BleUnsupported -> "Устройство не поддерживает Bluetooth LE"
    BeaconFailure.AdvertisingUnsupported -> "Устройство не умеет BLE-рекламу (нет режима периферии)"
    BeaconFailure.BluetoothOff -> "Bluetooth выключен"
    BeaconFailure.PermissionMissing -> "Нет разрешения на BLE-рекламу"
    BeaconFailure.InvalidConfig -> "Некорректные параметры маяка"
    is BeaconFailure.SystemError -> "Ошибка BLE-стека: ${systemErrorText(code)} (код $code)"
}

private fun systemErrorText(code: Int): String = when (code) {
    AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "пакет не помещается в 31 байт"
    AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "слишком много рекламодателей"
    AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "реклама уже запущена"
    AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "внутренняя ошибка стека"
    AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "функция не поддерживается"
    else -> "неизвестная ошибка"
}
