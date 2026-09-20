# Jev Phish Guard

Watches incoming SMS, sends each one to Jev as a batch of 5 typed questions
(1 Choice verdict, 3 Noul signals, 1 Score for severity), and flags the ones
that look like phishing — with a notification for high-confidence hits and a
quiet log entry for everything else.

## What's in here

| File | Role |
|---|---|
| `SmsReceiver.kt` | Listens for `SMS_RECEIVED`, hands the message off, never blocks the real SMS app |
| `PhishingClassifier.kt` | The 5 questions, and the pipeline that turns an answer into a stored verdict |
| `JevClient.kt` | Minimal OkHttp client for `POST /v1/systemone` |
| `PrivacyFilter.kt` | Masks OTP-shaped codes before anything leaves the device |
| `MessageStore.kt` | In-memory log + running cost/latency stats |
| `NotificationHelper.kt` | Heads-up alert for high-confidence phishing |
| `ui/PhishGuardScreen.kt` | The Compose screen itself — status band, message list, empty state |
| `MainActivity.kt` | Wires `MessageStore` into the screen, permissions, and the API key dialog |
| `SettingsStore.kt` | Where the API key lives (plain SharedPreferences — see caveats) |

## Setup

1. Open the project root in Android Studio (Koala/2024.1 or newer). The Gradle
   wrapper is included and pinned to Gradle 8.7 (the confirmed minimum for
   AGP 8.5.x) — Android Studio should sync without prompting you to generate one.
2. Run on an emulator or a device running Android 8.0+ (minSdk 26).
3. On first launch, tap the settings icon in the top-right of the status band,
   paste your TypeSafe API key in, and hit **Save**.
4. Grant the SMS and notification permissions when prompted.

## Testing without touching your real SIM

You don't need to risk your actual number to test this. The Android emulator
can fake an incoming SMS:

**Easiest — via adb:**
```
adb emu sms send +911234567890 "Your account will be suspended in 1 hour. Verify now: bit.ly/xyz123"
```

**Or via the emulator's telnet console:**
```
telnet localhost 5554
sms send +911234567890 Your account will be suspended in 1 hour. Verify now.
```
(Port is usually `5554` for the first running emulator instance — check
`adb devices` if you have more than one running.)

Either way, this fires the same `SMS_RECEIVED` broadcast a real text would,
so it exercises the whole pipeline. Send a mix on purpose:
- an obvious fake ("URGENT: KYC will be blocked, verify at [link]")
- a real-looking OTP message ("Your OTP is 483920, valid for 10 minutes")
- something genuinely ambiguous

That third category is the one worth screen-recording for your post — a
tool that only ever confidently flags the obvious cases isn't proving much.

**For a real device:** ask a friend to text you something phishing-shaped,
or use any of the free SMS-testing gateways. Don't test with real account
numbers, real OTPs tied to your accounts, or anything you wouldn't want
logged.

If a real-device test silently does nothing, check whether the message
actually arrived as RCS instead of SMS — two Google Messages users chatting
will often fall back to RCS chat features automatically, and RCS never
triggers `SMS_RECEIVED` (there's no public broadcast for it at all). Turn
off RCS chat features on one side (Google Messages → profile icon →
Messages settings → RCS chats → turn off) to force plain SMS.

## Design decisions worth knowing about (and worth mentioning if you post this)

**OTP masking.** `PrivacyFilter` strips numeric codes before the message
ever reaches the API, but only when the text also contains an OTP-ish
keyword — narrow on purpose, so it doesn't eat phone numbers or prices in
unrelated messages. The surrounding language is what actually determines
phishing intent; the code itself adds nothing to that judgment, so there's
no reason to let it leave the device.

**Confidence gating, not a single threshold.** `likely_phishing` at ≥70%
confidence fires a notification. Below that, or `uncertain`, it's logged but
silent. This is deliberate — a tool that's loud about everything gets
ignored. The middle band is also the most interesting part of a demo:
screen-record the ones it wasn't sure about, not just the obvious catches.

**`goAsync()`, not WorkManager.** `SmsReceiver` uses `goAsync()` plus a
receiver-scoped coroutine to let the suspend network call finish after
`onReceive()` returns. That's enough for a weekend build running in the
foreground. It won't survive the OS killing the process mid-request. If you
want that guarantee, move `classifyAndStore()` into a `WorkManager` one-off
job instead — same function, just enqueued rather than launched directly.
Worth saying out loud in an interview, not worth building today.

**Plain SharedPreferences for the key.** Fine for your own test device today.
Swap in `androidx.security`'s `EncryptedSharedPreferences` before this holds
a key you'd mind losing — same API shape, five-minute change.

**Fail open, always.** If the API call throws, the exception is caught and
logged — the receiver never crashes and the real SMS is never blocked. A
false negative from an outage is fine; breaking someone's actual messaging
is not.

## A note on the TypeSafe response shape

Choice answers carry `choice`, `confidence`, and a `probabilities` map; Noul
answers carry their single probability under `noul`. Both were confirmed
against a live API response — if TypeSafe changes this shape later,
`JevClient.parseAnswer()` is the only place that needs to change. There's a
`Log.d` under the `JevClient` tag on every request if you want to check the
raw JSON yourself.

## Rotate your key

If the API key you paste in here is one you've ever typed into a chat
window before, rotate it first.
