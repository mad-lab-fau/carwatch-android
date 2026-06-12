# CARWatch App

![Downloads](https://PlayBadges.pavi2410.me/badge/downloads?id=de.fau.cs.mad.carwatch&pretty)
![Rating](https://PlayBadges.pavi2410.me/badge/ratings?id=de.fau.cs.mad.carwatch&pretty)

**Get CARWatch in the Play Store**:  
[![Google Play](https://img.shields.io/badge/Google%20Play-CARWatch-3DDC84?logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=de.fau.cs.mad.carwatch)


CARWatch is an open-source Android research application designed to support accurate and
standardized saliva sampling in psychoneuroendocrinological studies. The app enables objective
recording of sampling times using barcode scans and automated reminders, reducing reliance on
self-reports and improving data validity.

Originally developed for assessing the cortisol awakening response (CAR), CARWatch has evolved into
a flexible framework for a range of saliva sampling protocols, including wake-up-based sampling,
fixed clock-time sampling, multi-day protocols, optional evening samples, and full diurnal cortisol
profiling.

CARWatch is a research-study companion app for invited participants. It is not a general consumer
app and cannot be used without a valid study configuration QR code provided by a study team.


## Objective
The CAR is the natural increase in cortisol levels that occurs within 30-45 minutes after waking up 
and can be an important indicator for overall health and well-being.

However, many CAR studies suffer from low adherence to the study protocol as well as from the 
lack of precise and objective methods for assessing the awakening and saliva sampling times which 
leads to measurement bias on CAR quantification. This issue can be solved by *CARWatch*, an Android
application that allows for objective saliva sampling times during CAR studies.


## Features
The main purpose of the application is to reduce saliva sampling inaccuracies by
objectively assessing saliva sampling times during CAR assessment.

CARWatch includes the following features:
* Awakening time assessment: Awakening can be assessed either by setting an alarm by the user 
  or by pressing a button in the application in the case of spontaneous awakening.
* Saliva sampling time assessment: Saliva sampling times are recorded by setting alarms for 
  each saliva sample which can only be turned off by scanning a barcode on the saliva sampling tube.
* Data export: All data collected by CARWatch can be exported which can be used for 
  further analysis.
* QR code-based study configuration for participant setup.
* Flexible alarm scheduling relative to awakening or at fixed clock times.
* Support for multi-day and multi-sample study designs.
* Daily overview of completed, pending, and missed samples.
* Optional evening sample reminders and bedtime sampling workflow.
* Built-in tutorial for participant guidance.
* Privacy-preserving local data handling with no backend data storage.

CARWatch is intended for use in scientific studies. It is not a medical device and does not provide
medical advice, diagnostics, or treatment recommendations.

## Getting Started
The easiest way to use CARWatch is to install it from the
[Google Play Store](https://play.google.com/store/apps/details?id=de.fau.cs.mad.carwatch).

To build the app from source:

1. Clone this repository.
2. Open the project in Android Studio.
3. Let Android Studio sync the Gradle project.
4. Connect an Android device or start an emulator.
5. Build and install the debug variant, or run:

```bash
./gradlew assembleDebug
```

CARWatch requires camera access for QR and tube barcode scanning. On newer Android versions it also
requires notification permissions so that wake-up, saliva sample, and evening sample reminders can
be shown reliably.

## Usage
CARWatch is designed for studies in which invited participants collect saliva samples according to a
configured sampling schedule. The app guides participants through setup, wake-up reporting, sample
reminders, barcode scanning, bedtime reporting, and log export.

CARWatch itself is not the sponsor, operator, or investigator of any specific study. It is a
research-enablement tool used by external research teams and institutions to support the collection
of saliva sampling data.

### Study setup
Before a participant starts using the app, the study team should prepare:

1. A study configuration QR code.
2. Barcode labels for all saliva tubes used in the study.
3. Participant instructions that explain when to use the Wakeup, Alarm, and Bedtime screens.
4. A contact email address for log export, if logs should be sent by email.

Study configurations can be created with the
[CARWatch Study Manager](https://carwatch-tools.github.io/study-manager/). The Study Manager helps
researchers define the study metadata, sampling schedule, participant settings, evening sample
requirement, and log-sharing contact email, then provides the QR code that participants scan during
onboarding.

On first launch, CARWatch opens the setup flow:

1. Welcome screen and study information.
2. Camera and notification permission request.
3. Study QR code scan.
4. Participant ID entry, if the participant ID is not already included in the QR code.
5. Study configuration confirmation.
6. Short tutorial.
7. Start of the active study.

The study QR code configures the app for one specific study. Without this QR code, the app cannot be
used because the QR code provides the study schedule and study metadata.

### Daily participant workflow
The main app has three primary screens:

1. `Wakeup`: report spontaneous awakening and start the morning sampling sequence.
2. `Alarm`: set the wake-up alarm, review scheduled saliva sample alarms, and set the evening sample reminder if required.
3. `Bedtime`: report going to bed and record the evening sample if the study requires one.

Recommended daily workflow:

1. Before going to sleep, open the `Alarm` screen and set the desired wake-up alarm time for the next morning.
2. If the study includes an evening sample, use the `Bedtime` screen before sleep and scan the evening sample tube when prompted.
3. If the study includes an evening reminder, configure the reminder in the `Alarm` screen. The default suggested time is 21:00 unless the participant has selected another time before.
4. In the morning, either wake up through the CARWatch alarm or open the `Wakeup` screen and confirm that you just woke up.
5. If the wake-up sample is required immediately, scan the wake-up sample tube. The wake-up event is only recorded after the barcode scan succeeds.
6. CARWatch schedules the remaining saliva sample alarms according to the study configuration.
7. When a saliva sample alarm fires, take the sample and scan the matching tube barcode to record it.
8. Use the `Alarm` screen to see which samples are scheduled, due, overdue, or already recorded.
9. After the last required sample for the day, follow the in-app message. If the study is finished, export the logs and send them to the study team.

### Menu actions
The floating menu in the main app provides additional actions:

* `Share Logs`: creates a ZIP archive of the collected log files and opens Android's share sheet.
* `Study Information`: shows the loaded study configuration, including participant ID, study days, sampling plan, and evening sample setting.
* `Reregister`: clears the current study configuration and returns to QR setup. Existing log files are kept.
* `Show Tutorial`: opens the tutorial screens again.
* `Finish Study Day`: cancels remaining samples for the current day and marks the day as finished.
* `Delete Logs`: deletes locally stored log files after confirmation. This cannot be undone.
* `Kill Alarms`: cancels scheduled alarms and timers after confirmation. This should only be used for troubleshooting.
* `App Info`: shows the installed app version.

### Study configuration QR code
The setup QR code contains semicolon-separated key-value pairs. It must start with `CARWATCH`.
Values are separated from keys with `:`.

Researchers can generate this QR code with the
[CARWatch Study Manager](https://carwatch-tools.github.io/study-manager/).

Supported fields:

| Field | Meaning | Example |
| --- | --- | --- |
| `N` | Study name | `CARWatchTest` |
| `D` | Number of study days | `3` |
| `NP` | Number of participants covered by the tube barcode set | `20` |
| `T` | Relative saliva sample offsets in minutes after wake-up | `0,15,15,15` |
| `A` | Fixed saliva sample times in `HHmm` format, comma-separated; can be empty | `0800,1200` |
| `SS` | First sample label prefix/index | `S0` |
| `E` | Evening sample required, `1` for yes and `0` for no | `1` |
| `M` | Contact email address for log export | `study@example.org` |
| `FD` | Duplicate barcode checking, `1` for enabled and `0` for disabled | `1` |
| `PID` | Optional participant ID stored directly from the QR code | `VP_001` |
| `V` | Optional web/config generator version | `1.0.0` |

Example:

```text
CARWATCH;N:CARWatchTest;D:3;NP:20;T:0,15,15,15;A:;SS:S0;E:1;M:study@example.org;FD:1;PID:VP_001;V:1.0.0
```

Notes:

* A first `T` value of `0` means that the wake-up sample is due immediately after wake-up.
* Further `T` values schedule relative samples after the previous sample time.
* `A` can be used for fixed-time samples in addition to relative samples.
* If `E` is `1`, CARWatch shows evening sample controls and prompts the participant to record the evening sample before bed.
* The QR code is generated and distributed by the study team, for example through the [CARWatch Study Manager](https://carwatch-tools.github.io/study-manager/) or another study-specific configuration workflow.

### Saliva tube barcode scanning
CARWatch uses the camera to scan the barcode printed on each saliva tube. The scanned barcode is
validated against the configured participant count, study day count, sample count, and expected
sample. If duplicate checking is enabled in the study configuration, scanning the same tube barcode
again is rejected.

When a barcode scan succeeds, CARWatch:

* logs the scan event,
* marks the corresponding sample as recorded,
* cancels the related reminder or alarm,
* updates the sample status in the `Alarm` screen.

### Example study flow
For a study configured with one immediate wake-up sample, three additional wake-up-based samples,
two fixed-time samples, and one evening sample, a participant completes seven samples per study day:

* wake-up sample at 0 minutes,
* wake-up-based samples at +15, +30, and +45 minutes,
* fixed-time samples at 12:00 and 15:00,
* evening sample before going to bed.

Example review or demonstration flow:

1. Launch the app.
2. Read the onboarding information.
3. Grant camera and notification permissions.
4. Scan the study QR code.
5. Confirm the displayed study configuration.
6. Proceed through the tutorial.
7. Use the `Wakeup` tab to report waking up.
8. Scan the wake-up sample barcode when prompted.
9. Use the `Alarm` tab to monitor scheduled and fixed-time samples.
10. Scan each saliva tube barcode when the corresponding sample is collected.
11. Use the `Bedtime` tab to record the evening sample, if required.
12. Open the menu and use `Share Logs` to export the locally stored study logs.

### Log export
CARWatch stores study logs locally on the device. Use `Share Logs` from the menu to create and share
a ZIP archive. The share dialog is prefilled with the configured study contact email address when
one was provided in the QR code.

Use `Delete Logs` only after the logs have been exported and received by the study team.

### Privacy and data handling
CARWatch stores study configuration, participant ID, scanned barcode history, and log files locally
on the device so the participant can complete the study. The app does not upload study data to a
backend server.

Camera access is used only for QR code and saliva tube barcode scanning. Notification permission is
used only to remind the participant when samples are due. Logs are shared only when the participant
explicitly selects `Share Logs`.

Important:

* The app cannot be used without a valid study QR code.
* Camera permission is required for QR code and barcode scanning.
* Notification permission is required for sample reminders.
* Data stays on the device unless the user explicitly shares logs.


## How to cite
If you use CARWatch in your research, please cite our Psychoneuroendocrinology 
[article](https://www.sciencedirect.com/science/article/pii/S0306453023000513):

```
Richer, R., Abel, L., Küderle, A., Eskofier, B. M., & Rohleder, N. (2023). 
CARWatch – A smartphone application for improving the accuracy of cortisol awakening response 
sampling. Psychoneuroendocrinology, 151, 106073. https://doi.org/10.1016/j.psyneuen.2023.106073
```

## Studies using CARWatch
The following papers used CARWatch in their research:
* Richer, R., Abel, L., Küderle, A., Eskofier, B. M., & Rohleder, N. (2023).
  CARWatch – A smartphone application for improving the accuracy of cortisol awakening response
  sampling. Psychoneuroendocrinology, 151, 106073. https://doi.org/10.1016/j.psyneuen.2023.106073
* Richer, R., Küderle, A., Dörr, J., Rohleder, N., & Eskofier, B. M. (2021). 
  Assessing the Influence of the Inner Clock on the Cortisol Awakening Response and 
  Pre-Awakening Movement. 2021 IEEE EMBS International Conference on Biomedical and Health 
  Informatics (BHI), 1–4. https://doi.org/10.1109/BHI50953.2021.9508529
* *further papers to be added - let us know if you used CARWatch in your research!*


## Compatibility
CARWatch requires an Android device running Android 6.0 (Marshmallow) or higher.

## Contributing
We welcome contributions to CARWatch! To contribute, please fork this repository 
and submit a pull request with your changes.

## License
CARWatch is licensed under the MIT License. See the LICENSE file for more information.

## Contact
If you have any questions or feedback about CARWatch, please contact 
[Robert Richer](mailto:robert.richer@fau.de).
