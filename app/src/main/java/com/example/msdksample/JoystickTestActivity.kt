package com.example.msdksample

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.v5.et.create
import dji.v5.et.listen
import dji.v5.manager.KeyManager
import dji.v5.manager.SDKManager
import dji.v5.utils.common.LogUtils

class JoystickTestActivity : AppCompatActivity() {

    private val tag = LogUtils.getTag(this)
    private val handler = Handler(Looper.getMainLooper())
    private var checkCount = 0
    private var listenersStarted = false

    private lateinit var tvLeftHorizontal: TextView
    private lateinit var tvLeftVertical: TextView
    private lateinit var tvRightHorizontal: TextView
    private lateinit var tvRightVertical: TextView

    private lateinit var tvShutterButton: TextView
    private lateinit var tvShutterLongPress: TextView
    private lateinit var tvRcButtonEvent: TextView
    private lateinit var tvRecordButton: TextView
    private lateinit var tvGoHomeButton: TextView
    private lateinit var tvPauseButton: TextView
    private lateinit var tvCustomButton1: TextView
    private lateinit var tvCustomButton2: TextView
    private lateinit var tvCustomButton3: TextView
    private lateinit var tvAuthLedButton: TextView
    private lateinit var tvFlightModeSwitch: TextView

    private lateinit var tvLeftDial: TextView
    private lateinit var tvRightDial: TextView
    private lateinit var tvScrollWheel: TextView
    private lateinit var tvFiveDimension: TextView

    private lateinit var tvConnectionStatus: TextView
    private lateinit var tvPhysicalLastEvent: TextView
    private lateinit var tvPhysicalHistory: TextView
    private lateinit var btnTab: Button
    private lateinit var physicalSlots: List<TextView>

    private val defaultPhysicalKeyCodes = listOf(
        KeyEvent.KEYCODE_F1,
        KeyEvent.KEYCODE_F2,
        KeyEvent.KEYCODE_F3,
        KeyEvent.KEYCODE_F4,
        KeyEvent.KEYCODE_F5,
        KeyEvent.KEYCODE_F6,
        KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_FOCUS,
        KeyEvent.KEYCODE_CAMERA,
        KeyEvent.KEYCODE_MEDIA_RECORD
    )
    private val physicalKeyCodes = defaultPhysicalKeyCodes.toMutableList()
    private val physicalKeyStates = mutableMapOf<Int, Boolean>()
    private val recentPhysicalEvents = ArrayDeque<String>()

    private val keyCodeToName = mapOf(
        KeyEvent.KEYCODE_F1 to "L1 / F1",
        KeyEvent.KEYCODE_F2 to "L2 / F2",
        KeyEvent.KEYCODE_F3 to "L3 / F3",
        KeyEvent.KEYCODE_F4 to "R1 / F4",
        KeyEvent.KEYCODE_F5 to "R2 / F5",
        KeyEvent.KEYCODE_F6 to "R3 / F6",
        KeyEvent.KEYCODE_F7 to "F7",
        KeyEvent.KEYCODE_F8 to "F8",
        KeyEvent.KEYCODE_F9 to "F9",
        KeyEvent.KEYCODE_F10 to "F10",
        KeyEvent.KEYCODE_F11 to "F11",
        KeyEvent.KEYCODE_F12 to "F12",
        KeyEvent.KEYCODE_BUTTON_L1 to "Record / BUTTON_L1",
        KeyEvent.KEYCODE_BUTTON_R1 to "Shutter full / BUTTON_R1",
        KeyEvent.KEYCODE_BUTTON_L2 to "BUTTON_L2",
        KeyEvent.KEYCODE_BUTTON_R2 to "BUTTON_R2",
        KeyEvent.KEYCODE_BUTTON_THUMBL to "BUTTON_THUMBL",
        KeyEvent.KEYCODE_BUTTON_THUMBR to "BUTTON_THUMBR",
        KeyEvent.KEYCODE_BUTTON_SELECT to "BUTTON_SELECT",
        KeyEvent.KEYCODE_BUTTON_START to "BUTTON_START",
        KeyEvent.KEYCODE_CAMERA to "Camera / CAMERA",
        KeyEvent.KEYCODE_FOCUS to "Shutter half / FOCUS",
        KeyEvent.KEYCODE_MEDIA_RECORD to "Record / MEDIA_RECORD",
        KeyEvent.KEYCODE_VOLUME_UP to "VOLUME_UP",
        KeyEvent.KEYCODE_VOLUME_DOWN to "VOLUME_DOWN",
        KeyEvent.KEYCODE_DPAD_UP to "DPAD_UP",
        KeyEvent.KEYCODE_DPAD_DOWN to "DPAD_DOWN",
        KeyEvent.KEYCODE_DPAD_LEFT to "DPAD_LEFT",
        KeyEvent.KEYCODE_DPAD_RIGHT to "DPAD_RIGHT",
        KeyEvent.KEYCODE_DPAD_CENTER to "DPAD_CENTER"
    )

    private var leftHorizontal = 0
    private var leftVertical = 0
    private var rightHorizontal = 0
    private var rightVertical = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_joystick_test)

        initViews()
        updatePhysicalButtonsDisplay()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        checkSDKRegistration()
    }

    override fun onResume() {
        super.onResume()
        window.decorView.isFocusable = true
        window.decorView.isFocusableInTouchMode = true
        window.decorView.requestFocus()
    }

    private fun initViews() {
        tvConnectionStatus = findViewById(R.id.tv_connection_status)
        btnTab = findViewById(R.id.btn_tab)
        btnTab.setOnClickListener {
            startActivity(Intent(this, TabPageActivity::class.java))
        }

        tvLeftHorizontal = findViewById(R.id.tv_left_horizontal)
        tvLeftVertical = findViewById(R.id.tv_left_vertical)
        tvRightHorizontal = findViewById(R.id.tv_right_horizontal)
        tvRightVertical = findViewById(R.id.tv_right_vertical)

        tvShutterButton = findViewById(R.id.tv_shutter_button)
        tvShutterLongPress = findViewById(R.id.tv_shutter_long_press)
        tvRcButtonEvent = findViewById(R.id.tv_rc_button_event)
        tvRecordButton = findViewById(R.id.tv_record_button)
        tvGoHomeButton = findViewById(R.id.tv_gohome_button)
        tvPauseButton = findViewById(R.id.tv_pause_button)
        tvCustomButton1 = findViewById(R.id.tv_custom_button1)
        tvCustomButton2 = findViewById(R.id.tv_custom_button2)
        tvCustomButton3 = findViewById(R.id.tv_custom_button3)
        tvAuthLedButton = findViewById(R.id.tv_auth_led_button)
        tvFlightModeSwitch = findViewById(R.id.tv_flight_mode_switch)

        tvLeftDial = findViewById(R.id.tv_left_dial)
        tvRightDial = findViewById(R.id.tv_right_dial)
        tvScrollWheel = findViewById(R.id.tv_scroll_wheel)
        tvFiveDimension = findViewById(R.id.tv_five_dimension)

        tvPhysicalLastEvent = findViewById(R.id.tv_physical_last_event)
        tvPhysicalHistory = findViewById(R.id.tv_physical_history)
        physicalSlots = listOf(
            findViewById(R.id.tv_physical_button1),
            findViewById(R.id.tv_physical_button2),
            findViewById(R.id.tv_physical_button3),
            findViewById(R.id.tv_physical_button4),
            findViewById(R.id.tv_physical_button5),
            findViewById(R.id.tv_physical_button6),
            findViewById(R.id.tv_physical_button7),
            findViewById(R.id.tv_physical_button8),
            findViewById(R.id.tv_physical_button9),
            findViewById(R.id.tv_physical_button10),
            findViewById(R.id.tv_physical_button11),
            findViewById(R.id.tv_physical_button12)
        )
    }

    private fun checkSDKRegistration() {
        if (SDKManager.getInstance().isRegistered) {
            tvConnectionStatus.text = "MSDK: registered"
            tvConnectionStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            startListeningAll()
            return
        }

        checkCount += 1
        if (checkCount <= MAX_SDK_CHECK_COUNT) {
            tvConnectionStatus.text = "MSDK: registering... ($checkCount/$MAX_SDK_CHECK_COUNT)"
            tvConnectionStatus.setTextColor(getColor(android.R.color.darker_gray))
            handler.postDelayed({ checkSDKRegistration() }, SDK_CHECK_INTERVAL_MS)
        } else {
            tvConnectionStatus.text = "MSDK: registration timeout"
            tvConnectionStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            LogUtils.e(tag, "MSDK registration timeout")
        }
    }

    private fun startListeningAll() {
        if (listenersStarted) return
        listenersStarted = true

        startListeningSticks()
        startListeningButtons()
        startListeningDials()
        startListeningFiveDimension()
        startListeningFlightModeSwitch()
        checkConnectionStatus()
    }

    private fun startListeningSticks() {
        RemoteControllerKey.KeyStickLeftHorizontal.create().listen(this) { value ->
            value?.let {
                leftHorizontal = it
                runOnUiThread { tvLeftHorizontal.text = "Horizontal: $leftHorizontal" }
            }
        }

        RemoteControllerKey.KeyStickLeftVertical.create().listen(this) { value ->
            value?.let {
                leftVertical = it
                runOnUiThread { tvLeftVertical.text = "Vertical: $leftVertical" }
            }
        }

        RemoteControllerKey.KeyStickRightHorizontal.create().listen(this) { value ->
            value?.let {
                rightHorizontal = it
                runOnUiThread { tvRightHorizontal.text = "Horizontal: $rightHorizontal" }
            }
        }

        RemoteControllerKey.KeyStickRightVertical.create().listen(this) { value ->
            value?.let {
                rightVertical = it
                runOnUiThread { tvRightVertical.text = "Vertical: $rightVertical" }
            }
        }
    }

    private fun startListeningButtons() {
        RemoteControllerKey.KeyShutterButtonDown.create().listen(this) { value ->
            updateBooleanStatus(tvShutterButton, "Shutter", value)
        }

        try {
            RemoteControllerKey.KeyRCShutterButtonLongPress.create().listen(this) { value ->
                updateBooleanStatus(tvShutterLongPress, "Shutter long", value)
            }
        } catch (e: Exception) {
            LogUtils.e(tag, "Shutter long press listener failed: ${e.message}")
            runOnUiThread { tvShutterLongPress.text = "Shutter long: unsupported" }
        }

        try {
            RemoteControllerKey.KeyRcButtonEventPro.create().listen(this) { value ->
                updateRcButtonEventStatus(value)
            }
        } catch (e: Exception) {
            LogUtils.e(tag, "RC button event listener failed: ${e.message}")
            runOnUiThread { tvRcButtonEvent.text = "RC event pro: unsupported" }
        }

        RemoteControllerKey.KeyRecordButtonDown.create().listen(this) { value ->
            updateBooleanStatus(tvRecordButton, "Record", value)
        }

        RemoteControllerKey.KeyGoHomeButtonDown.create().listen(this) { value ->
            updateBooleanStatus(tvGoHomeButton, "Go Home", value)
        }

        RemoteControllerKey.KeyPauseButtonDown.create().listen(this) { value ->
            updateBooleanStatus(tvPauseButton, "Pause", value)
        }

        RemoteControllerKey.KeyCustomButton1Down.create().listen(this) { value ->
            updateBooleanStatus(tvCustomButton1, "C1", value)
        }

        RemoteControllerKey.KeyCustomButton2Down.create().listen(this) { value ->
            updateBooleanStatus(tvCustomButton2, "C2", value)
        }

        RemoteControllerKey.KeyCustomButton3Down.create().listen(this) { value ->
            updateBooleanStatus(tvCustomButton3, "C3", value)
        }

        try {
            RemoteControllerKey.KeyRCAuthLedButtonDown.create().listen(this) { value ->
                updateBooleanStatus(tvAuthLedButton, "Auth LED", value)
            }
        } catch (e: Exception) {
            LogUtils.e(tag, "Auth LED listener failed: ${e.message}")
            runOnUiThread { tvAuthLedButton.text = "Auth LED: unsupported" }
        }
    }

    private fun startListeningFlightModeSwitch() {
        try {
            RemoteControllerKey.KeyFlightModeSwitchState.create().listen(this) { value ->
                val modeText = when (value?.toString()) {
                    "SWITCH_ONE" -> "position 1"
                    "SWITCH_TWO" -> "position 2"
                    "SWITCH_THREE" -> "position 3"
                    else -> value?.toString() ?: "unknown"
                }
                runOnUiThread {
                    tvFlightModeSwitch.text = "Flight mode switch: $modeText"
                }
            }
        } catch (e: Exception) {
            LogUtils.e(tag, "Flight mode switch listener failed: ${e.message}")
            runOnUiThread { tvFlightModeSwitch.text = "Flight mode switch: unsupported" }
        }
    }

    private fun startListeningDials() {
        RemoteControllerKey.KeyLeftDial.create().listen(this) { value ->
            value?.let { runOnUiThread { tvLeftDial.text = "Left dial: $it" } }
        }

        RemoteControllerKey.KeyRightDial.create().listen(this) { value ->
            value?.let { runOnUiThread { tvRightDial.text = "Right dial: $it" } }
        }

        RemoteControllerKey.KeyScrollWheel.create().listen(this) { value ->
            value?.let { runOnUiThread { tvScrollWheel.text = "Scroll wheel: $it" } }
        }
    }

    private fun startListeningFiveDimension() {
        RemoteControllerKey.KeyFiveDimensionPressedStatus.create().listen(this) { value ->
            value?.let {
                val statusList = mutableListOf<String>()
                if (it.getUpwards() == true) statusList.add("up")
                if (it.getDownwards() == true) statusList.add("down")
                if (it.getLeftwards() == true) statusList.add("left")
                if (it.getRightwards() == true) statusList.add("right")
                if (it.getMiddlePressed() == true) statusList.add("center")
                val status = if (statusList.isEmpty()) "idle" else statusList.joinToString(", ")
                runOnUiThread { tvFiveDimension.text = "Five-way: $status" }
            }
        }
    }

    private fun checkConnectionStatus() {
        RemoteControllerKey.KeyConnection.create().listen(this) { isConnected ->
            runOnUiThread {
                if (isConnected == true) {
                    tvConnectionStatus.text = "MSDK: registered, RC connected"
                    tvConnectionStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                } else {
                    tvConnectionStatus.text = "MSDK: registered, RC disconnected"
                    tvConnectionStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
                }
            }
        }
    }

    private fun updateBooleanStatus(view: TextView, label: String, value: Boolean?) {
        runOnUiThread {
            val pressed = value == true
            view.text = "$label: ${if (pressed) "DOWN" else "UP"}"
            view.setTextColor(
                getColor(if (pressed) android.R.color.holo_green_dark else android.R.color.black)
            )
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
            handlePhysicalKeyEvent(event)
            if (event.keyCode != KeyEvent.KEYCODE_BACK) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun handlePhysicalKeyEvent(event: KeyEvent) {
        val isPressed = event.action == KeyEvent.ACTION_DOWN
        val keyCode = event.keyCode
        ensurePhysicalKeyVisible(keyCode)
        physicalKeyStates[keyCode] = isPressed

        val eventText = buildString {
            append(if (isPressed) "DOWN" else "UP")
            append(" ")
            append(keyLabel(keyCode))
            append(" keyCode=")
            append(keyCode)
            append(" scanCode=")
            append(event.scanCode)
            append(" repeat=")
            append(event.repeatCount)
            append(" source=0x")
            append(event.source.toString(16))
        }

        LogUtils.d(tag, "Physical key event: $eventText")
        runOnUiThread {
            tvPhysicalLastEvent.text = eventText
            pushPhysicalEvent(eventText)
            updatePhysicalButtonsDisplay()
        }
    }

    private fun ensurePhysicalKeyVisible(keyCode: Int) {
        if (physicalKeyCodes.contains(keyCode)) return

        if (physicalKeyCodes.size >= MAX_PHYSICAL_KEY_SLOTS) {
            val removable = physicalKeyCodes.firstOrNull {
                it !in defaultPhysicalKeyCodes && physicalKeyStates[it] != true
            } ?: physicalKeyCodes.first()
            physicalKeyCodes.remove(removable)
            physicalKeyStates.remove(removable)
        }
        physicalKeyCodes.add(keyCode)
    }

    private fun updatePhysicalButtonsDisplay() {
        if (!::physicalSlots.isInitialized) return

        physicalSlots.forEachIndexed { index, view ->
            val keyCode = physicalKeyCodes.getOrNull(index)
            if (keyCode == null) {
                view.text = "--"
                view.setTextColor(getColor(android.R.color.darker_gray))
                return@forEachIndexed
            }

            val isPressed = physicalKeyStates[keyCode] == true
            view.text = "${keyLabel(keyCode)} ($keyCode): ${if (isPressed) "DOWN" else "UP"}"
            view.setTextColor(
                getColor(if (isPressed) android.R.color.holo_green_dark else android.R.color.black)
            )
        }
    }

    private fun pushPhysicalEvent(eventText: String) {
        recentPhysicalEvents.addFirst(eventText)
        while (recentPhysicalEvents.size > MAX_RECENT_EVENTS) {
            recentPhysicalEvents.removeLast()
        }
        tvPhysicalHistory.text = recentPhysicalEvents.joinToString(separator = "\n")
    }

    private fun updateRcButtonEventStatus(value: Any?) {
        runOnUiThread {
            tvRcButtonEvent.text = "RC event pro: ${formatRcButtonEvent(value)}"
            tvRcButtonEvent.setTextColor(getColor(android.R.color.holo_green_dark))
        }
    }

    private fun formatRcButtonEvent(value: Any?): String {
        if (value == null) return "null"
        val parts = mutableListOf<String>()
        for (getter in listOf(
            "getValue",
            "getButtonActionValue",
            "getIsC1Click",
            "getIsC2Click",
            "getIsC3Click",
            "getIsC4Click"
        )) {
            runCatching {
                val method = value.javaClass.getMethod(getter)
                val result = method.invoke(value)
                parts.add("${getter.removePrefix("get")}=$result")
            }
        }
        if (parts.isEmpty()) {
            parts.add(value.toString())
        }
        return parts.joinToString(separator = " ")
    }

    private fun keyLabel(keyCode: Int): String {
        keyCodeToName[keyCode]?.let { return it }
        val androidName = KeyEvent.keyCodeToString(keyCode)
        return androidName.removePrefix("KEYCODE_")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        KeyManager.getInstance().cancelListen(this)
    }

    companion object {
        private const val MAX_SDK_CHECK_COUNT = 10
        private const val SDK_CHECK_INTERVAL_MS = 2000L
        private const val MAX_PHYSICAL_KEY_SLOTS = 12
        private const val MAX_RECENT_EVENTS = 8
    }
}
