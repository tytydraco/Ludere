package com.draco.libretrowrapper.fragments

import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.draco.libretrowrapper.utils.GamePad
import com.draco.libretrowrapper.utils.GamePadConfig
import com.draco.libretrowrapper.utils.PrivateData
import com.draco.libretrowrapper.R
import com.swordfish.libretrodroid.GLRetroView

class GamePadFragment : Fragment() {
    /* Essential objects */
    private lateinit var privateData: PrivateData

    /* UI components */
    private lateinit var leftGamePadContainer: FrameLayout
    private lateinit var rightGamePadContainer: FrameLayout

    /* Emulator objects */
    var retroView: GLRetroView? = null
    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        privateData = PrivateData(context)

        /* Initialize the GamePads */
        val gamePadConfig = GamePadConfig(context, resources)
        leftGamePad = GamePad(context, gamePadConfig.left, privateData)
        rightGamePad = GamePad(context, gamePadConfig.right, privateData)

        /* Configure GamePad size and position */
        val density = resources.displayMetrics.density
        val gamePadSize = resources.getDimension(R.dimen.config_gamepad_size) / density
        leftGamePad!!.pad.offsetX = -1f
        leftGamePad!!.pad.offsetY = 1f
        rightGamePad!!.pad.offsetX = 1f
        rightGamePad!!.pad.offsetY = 1f
        leftGamePad!!.pad.primaryDialMaxSizeDp = gamePadSize
        rightGamePad!!.pad.primaryDialMaxSizeDp = gamePadSize

        /*
         * Subscribe the input handler observables, which might be null
         * if the activity is recreated. In which case, the subscription will
         * occur later in the lifecycle.
         */
        if (retroView != null) {
            leftGamePad!!.subscribe(retroView!!)
            rightGamePad!!.subscribe(retroView!!)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gamepad, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Initialize UI components */
        leftGamePadContainer = view.findViewById(R.id.left_container)
        rightGamePadContainer = view.findViewById(R.id.right_container)

        /* Check if we should show or hide controls */
        val visibility = if (shouldShowGamePads())
            View.VISIBLE
        else
            View.GONE

        /* Apply the new visibility state to the containers */
        leftGamePadContainer.visibility = visibility
        rightGamePadContainer.visibility = visibility

        /* Add to layout */
        leftGamePadContainer.addView(leftGamePad!!.pad)
        rightGamePadContainer.addView(rightGamePad!!.pad)
    }

    private fun shouldShowGamePads(): Boolean {
        /* Do not show if the device lacks a touch screen */
        if (!requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN))
            return false

        /* Do not show if the current display is external (i.e. wireless cast) */
        val dm = context?.getSystemService(Service.DISPLAY_SERVICE) as DisplayManager
        if (dm.getDisplay(getCurrentDisplayId()).flags and Display.FLAG_PRESENTATION == Display.FLAG_PRESENTATION)
            return false

        /* Do not show if the device has a controller connected */
        for (id in InputDevice.getDeviceIds()) {
            InputDevice.getDevice(id).apply {
                if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                    sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
                    return false
            }
        }

        /* Otherwise, show */
        return true
    }

    private fun getCurrentDisplayId(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            context?.display!!.displayId
        else {
            val wm = context?.getSystemService(AppCompatActivity.WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.displayId
        }
    }

    override fun onDestroy() {
        leftGamePad?.unsubscribe()
        rightGamePad?.unsubscribe()
        super.onDestroy()
    }
}