# iBeacon Broadcaster for Android

<p align="center">
  <img src="docs/images/icon.png" width="108" height="108" alt="iBeacon Icon" />
</p>

<p align="center">
  <strong>Надёжный автономный iBeacon-передатчик с современным дизайном Material 3 и многоуровневой защитой от выгрузки системой.</strong>
</p>

<p align="center">
  <a href="https://developer.android.com/about/versions/15"><img src="https://img.shields.io/badge/Android-8.0%20..%2015%20(API%2026--35)-3DDC84?logo=android&logoColor=white" alt="Android Support" /></a>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose M3" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" /></a>
</p>

<p align="center">
  <strong>Русский</strong> | <a href="README.en.md">English</a>
</p>

---

## 📱 О проекте

**iBeacon Broadcaster** превращает ваш Android-смартфон в полноценный Bluetooth LE маяк стандарта Apple iBeacon с настраиваемыми идентификаторами **UUID**, **Major**, **Minor** и мощностью калибровки (**Measured Power**).

Главная цель приложения — **бесперебойная и непрерывная трансляция**. Большинство существующих аналогов прекращают вещание при выключении экрана, переходе в режим энергосбережения Doze или при закрытии приложения из списка недавних задач. iBeacon Broadcaster спроектирован для максимальной живучести в агрессивных фоновых условиях современных версий Android и фирменных оболочек (MagicOS, EMUI, MIUI/HyperOS, OneUI).

---

## ✨ Скриншоты

<p align="center">
  <img src="docs/images/hero_dark.png" width="340" alt="Главный экран вещания" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="docs/images/settings_dark.png" width="340" alt="Параметры радиосигнала и чек-лист" />
</p>

---

## 🚀 Ключевые возможности

- **Стандартный протокол iBeacon**: формирование корректного 30-байтового пакета BLE Advertising (Apple Company ID `0x004C`, тип `0x02`, длина `0x15`).
- **Современный интерфейс Material 3**:
  - Поддержка **Material You Dynamic Color** (Android 12+) — цвета адаптируются под системную тему и обои.
  - Полноценная тёмная и светлая темы.
  - **Hero-карточка статуса**: анимированный радар, живой таймер Uptime, краткая сводка параметров и счётчик автовосстановлений службы.
  - Сегментированные переключатели частоты и мощности радиосигнала.
  - Генератор случайного UUID и удобное копирование идентификатора в буфер обмена.
  - Валидация входных данных с подсветкой некорректных диапазонов.
- **Интерактивный чек-лист «Живучесть»**:
  - Шкала прогресса настройки системы (например, `5 из 5 настроено`).
  - Быстрый переход в один клик к системным экранам отключения оптимизации батареи, управления автозапуском и выдачи разрешений.
- **Интеграция со шторкой уведомлений**:
  - Постоянное Foreground Service уведомление с актуальным статусом и идентификатором маяка.
  - Кнопка быстрой остановки («Остановить») прямо из уведомления.
  - Утончённая векторная иконка маяка в строке состояния Android.

---

## 🛡️ Архитектура живучести (Keep-Alive)

Для обеспечения непрерывной трансляции приложение реализует эшелонированную систему защиты службы от остановки:

1. **Foreground Service (`connectedDevice`)**: служба зарегистрирована со специальным типом для подключённых радиоустройств в Android 14+ и приоритетным флагом `START_STICKY`.
2. **Перехват свайпа (`onTaskRemoved`)**: при сбросе приложения из списка недавних задач планируется мгновенный точный перезапуск службы через `AlarmManager`.
3. **Partial WakeLock**: процессор не засыпает глубоким сном во время трансляции, поддерживая работу BLE-стека.
4. **Двойной сторожевой таймер (Watchdogs)**:
   - **Точный будильник (`WatchdogAlarm`)**: каждые ~5 минут проверяет состояние и «будит» службу даже в глубоком Doze-режиме (`setExactAndAllowWhileIdle`).
   - **Фоновый воркер (`WatchdogWorker`)**: периодический `WorkManager` (каждые 15 минут) обеспечивает устойчивость к длительному простою и перезагрузкам.
5. **Внутренний самоконтроль (Self-Check)**: каждые 30 секунд корутина внутри службы проверяет, не сбросил ли стек Bluetooth рекламу втихую, и при необходимости переинициализирует её.
6. **Слежение за Bluetooth**: BroadcastReceiver мгновенно поднимает трансляцию при выключении и повторном включении Bluetooth пользователем.
7. **Автозапуск при старте системы**: поддержка `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED` и обновления пакета `MY_PACKAGE_REPLACED`.
8. **Device-Protected Storage**: настройки маяка хранятся в защищённом хранилище устройства, что позволяет поднять вещание сразу после перезагрузки телефона ещё **до ввода пароля разблокировки экрана**.

---

## 📡 Формат пакета iBeacon (Wire Format)

Пакет BLE Non-connectable Undirected Advertising формируется в соответствии со спецификацией Apple:

| Байт | Поле | Значение | Описание |
| :--- | :--- | :--- | :--- |
| `0..1` | Flags AD Type | `0x02, 0x01` | Длина 2, тип Flags |
| `2` | Flags Value | `0x06` | LE General Discoverable + BR/EDR Not Supported |
| `3..4` | Manufacturer Data Header | `0x1A, 0xFF` | Длина 26 байт (0x1A), тип Manufacturer Specific (0xFF) |
| `5..6` | Company Identifier | `0x4C, 0x00` | Apple Inc. (Little-Endian: `0x004C`) |
| `7` | Beacon Type | `0x02` | Идентификатор спецификации iBeacon |
| `8` | Data Length | `0x15` | Длина полезной нагрузки (21 байт) |
| `9..24` | Proximity UUID | 16 байт | 128-битный идентификатор (Big-Endian) |
| `25..26` | Major | 2 байта | Идентификатор группы `0..65535` (Big-Endian) |
| `27..28` | Minor | 2 байта | Идентификатор маяка `0..65535` (Big-Endian) |
| `29` | Measured Power | 1 байт | Калибровочный RSSI на расстоянии 1 метра (int8, обычно `-59 dBm`) |

---

## ⚙️ Параметры вещания

### Периодичность трансляции (Advertise Mode)
Android регулирует частоту пакетов через системные пресеты:
- **`~1000 мс`** (`ADVERTISE_MODE_LOW_POWER`): максимальное энергосбережение, подходит для стационарных сценариев.
- **`~250 мс`** (`ADVERTISE_MODE_BALANCED`): сбалансированный режим, рекомендуемый для большинства задач.
- **`~100 мс`** (`ADVERTISE_MODE_LOW_LATENCY`): высокая частота для точной и быстрой детекции в движении.

### Мощность передатчика (Tx Power)
Влияет на физический радиус действия антенны:
- **Мин.** (`TX_POWER_ULTRA_LOW`): наименьший радиус, минимальное энергопотребление.
- **Низк.** (`TX_POWER_LOW`): для работы в пределах одной комнаты.
- **Сред.** (`TX_POWER_MEDIUM`): сбалансированный охват.
- **Макс.** (`TX_POWER_HIGH`): наибольшая дальность радиосигнала.

---

## 🛠️ Сборка и установка

### Требования
- **Android Studio** Ladybug (2024.2+) или новее.
- **JDK 17+** (встроенный в Android Studio JBR).
- **Android SDK Platform 35**.

### Сборка Debug APK
```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
```
Готовый файл: `app/build/outputs/apk/debug/app-debug.apk`

### Запуск тестов
```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew test
```

### Сборка оптимизированного Release APK
Для релизной сборки создайте файл `keystore.properties` в корне проекта (он добавлен в `.gitignore`):
```properties
storeFile=keystore/release.jks
storePassword=ВАШ_ПАРОЛЬ_ХРАНИЛИЩА
keyAlias=ВАШ_АЛИАС
keyPassword=ВАШ_ПАРОЛЬ_КЛЮЧА
```

Сборка с оптимизацией R8 и сжатием ресурсов:
```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleRelease
```
Готовый APK: `app/build/outputs/apk/release/app-release.apk` (размер ~1.2 МБ).

### Установка на телефон через ADB
```bash
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 📋 Рекомендации для различных прошивок (OEM)

На некоторых прошивках со строгим контролем фоновой активности необходимо выполнить ручную настройку (для удобства в приложении предусмотрен чек-лист с прямыми переходами в настройки):

- **Honor (MagicOS) / Huawei (EMUI)**:
  1. Перейдите в *Настройки -> Батарея -> Запуск приложений*.
  2. Найдите *iBeacon*, отключите «Автоматическое управление» и включите все три тумблера вручную: «Автозапуск», «Косвенный запуск» и «Работа в фоновом режиме».
  3. В меню *Недавние приложения* заблокируйте приложение замочком (свайп вниз по карточке).
- **Xiaomi / POCO (MIUI / HyperOS)**:
  1. Включите *Автозапуск* для приложения в свойствах программы.
  2. В разделе *Контроль активности* выберите «Нет ограничений».
- **Samsung (OneUI)**:
  1. Перейдите в *Сведения о приложении -> Батарея* и выберите режим «Не ограничено».

> [!NOTE]
> Принудительная остановка приложения пользователем из системных настроек («Остановить принудительно») блокирует все будильники и фоновые задачи приложения до следующего ручного запуска. Это ограничение безопасности Android, общее для всех приложений.

---

## 📄 Лицензия

Проект распространяется под свободной лицензией **Apache License 2.0**. Подробности в файле [LICENSE](LICENSE).
