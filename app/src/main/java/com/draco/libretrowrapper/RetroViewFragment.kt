package com.draco.libretrowrapper

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.Variable
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.CountDownLatch

class RetroViewFragment : Fragment() {
    /* Essential objects */
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var privateData: PrivateData

    /* Emulator objects */
    var retroView: GLRetroView? = null

    /* UI components */
    private lateinit var progress: ProgressBar

    /* Latch that gets decremented when the GLRetroView renders a frame */
    val retroViewReadyLatch = CountDownLatch(1)

    /* Store all observable subscriptions */
    private val compositeDisposable = CompositeDisposable()

    /* Shared preference keys */
    private val fastForwardEnabledString = "fast_forward_enabled"
    private val audioEnabledString = "audio_enabled"

    override fun onAttach(context: Context) {
        super.onAttach(context)

        /* Setup essential objects */
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        privateData = PrivateData(context)

        /* Prepare the SRAM bytes if the file exists */
        var saveBytes = byteArrayOf()
        if (privateData.save.exists()) {
            val saveInputStream = privateData.save.inputStream()
            saveBytes = saveInputStream.readBytes()
            saveInputStream.close()
        }

        /* Create the GLRetroView */
        retroView = GLRetroView(
            requireContext(),
            privateData.core.absolutePath,
            privateData.rom.absolutePath,
            saveRAMState = saveBytes,
            shader = GLRetroView.SHADER_SHARP,
            variables = getCoreVariables()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_retroview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Initialize UI components */
        progress = view.findViewById(R.id.progress)

        /* Hook the GLRetroView to the fragment lifecycle */
        lifecycle.addObserver(retroView!!)

        /* Center the view in the parent container */
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        )
        params.gravity = Gravity.CENTER
        retroView!!.layoutParams = params

        (view as FrameLayout).addView(retroView)
        progress.visibility = View.GONE

        /* Start tracking the frame state of the GLRetroView */
        val renderDisposable = retroView!!
            .getGLRetroEvents()
            .subscribe {
                if (it == GLRetroView.GLRetroEvents.FrameRendered)
                    retroViewReadyLatch.countDown()
            }
        compositeDisposable.add(renderDisposable)

        Thread {
            retroViewReadyLatch.await()

            /* Restore emulator settings from last launch */
            restoreSettings()
        }.start()
    }

    private fun getCoreVariables(): Array<Variable> {
        /* Parse the variables string into a Variable array */
        val variables = arrayListOf<Variable>()
        val rawVariablesString = getString(R.string.config_variables)
        val rawVariables = rawVariablesString.split(",")

        for (rawVariable in rawVariables) {
            val rawVariableSplit = rawVariable.split("=")
            if (rawVariableSplit.size != 2)
                continue

            variables.add(Variable(rawVariableSplit[0], rawVariableSplit[1]))
        }

        return variables.toTypedArray()
    }

    private fun restoreSettings() {
        retroView!!.fastForwardEnabled = sharedPreferences.getBoolean(fastForwardEnabledString, false)
        retroView!!.audioEnabled = sharedPreferences.getBoolean(audioEnabledString, true)
    }

    private fun saveSettings() {
        with (sharedPreferences.edit()) {
            putBoolean(fastForwardEnabledString, retroView!!.fastForwardEnabled)
            putBoolean(audioEnabledString, retroView!!.audioEnabled)
            apply()
        }
    }

    fun saveTempState() {
        /* Save a temporary state since Android killed the activity */
        val savedInstanceStateBytes = retroView?.serializeState()
        if (savedInstanceStateBytes != null) {
            with(privateData.savedInstanceState.outputStream()) {
                write(savedInstanceStateBytes)
                close()
            }
        }
    }

    fun restoreTempState() {
        /* Don't bother restoring a temporary state if it doesn't exist */
        if (!privateData.savedInstanceState.exists())
            return

        Thread {
            /* Wait for the GLRetroView to become usable */
            retroViewReadyLatch.await()

            /* Fetch the state bytes */
            val stateInputStream = privateData.savedInstanceState.inputStream()
            val stateBytes = stateInputStream.readBytes()
            stateInputStream.close()

            /* Invalidate the temporary state so we cannot restore it twice */
            privateData.savedInstanceState.delete()

            /* Restore the temporary state */
            retroView!!.unserializeState(stateBytes)
        }.start()
    }

    override fun onStop() {
        /* Save emulator settings for next launch */
        saveSettings()

        /* Save SRAM to the disk only if the emulator was able to render a frame */
        if (retroViewReadyLatch.count == 0L) {
            with(privateData.save.outputStream()) {
                write(retroView?.serializeSRAM())
                close()
            }
        }

        super.onStop()
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }
}