package de.simon.dankelmann.bluetoothlespam.ui.swiftPair

import android.R.attr.text
import android.R.id.checkbox
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.SwiftPairAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Constants.LogLevel
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBleAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.LogEntryModel
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.Services.AdvertisementLoopService
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeAdvertisementService
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentSwiftpairBinding


class SwiftPairFragment: Fragment(), IBleAdvertisementServiceCallback {
    private var _binding: FragmentSwiftpairBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var _viewModel: SwiftPairViewModel? = null
    private var _bluetoothLeAdvertisementService: BluetoothLeAdvertisementService = BluetoothLeAdvertisementService(
        AppContext.getContext().bluetoothAdapter()!!)
    private var _advertisementLoopService: AdvertisementLoopService = AdvertisementLoopService(_bluetoothLeAdvertisementService)
    private val _logTag = "SwiftPairFragment"

    private lateinit var _toggleButton:Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(SwiftPairViewModel::class.java)
        _viewModel = viewModel
        _binding = FragmentSwiftpairBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // setup callbacks
        _bluetoothLeAdvertisementService.addBleAdvertisementServiceCallback(this)
        _advertisementLoopService.addBleAdvertisementServiceCallback(this)

        // Add advertisement sets to the Loop Service:
        val _advertisementSetGenerator = SwiftPairAdvertisementSetGenerator()
        val _advertisementSets = _advertisementSetGenerator.getAdvertisementSets()
        _advertisementSets.map {
            _advertisementLoopService.addAdvertisementSet(it)
        }

        setupUi()

        return root
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        if(_advertisementLoopService.advertising){
            stopAdvertising()
        }
    }

    fun startAdvertising(){
        _advertisementLoopService.startAdvertising()

        val logEntry = LogEntryModel()
        logEntry.level = LogLevel.Info
        logEntry.message = "Started Advertising"
        _viewModel!!.addLogEntry(logEntry)

        _viewModel!!.isTransmitting.postValue(true)

        _toggleButton.text = "Stop Advertising"
    }

    fun stopAdvertising(){
        _advertisementLoopService.stopAdvertising()

        val logEntry = LogEntryModel()
        logEntry.level = LogLevel.Info
        logEntry.message = "Stopped Advertising"
        _viewModel!!.addLogEntry(logEntry)

        _viewModel!!.isTransmitting.postValue(false)

        _toggleButton.text = "Start Advertising"
    }

    fun setupUi(){
        if(_viewModel != null){

            // toggle button
            val toggleBtn: Button = binding.advertiseButton
            _toggleButton = toggleBtn
            //animation view
            val animationView: LottieAnimationView = binding.swiftPairAnimation

            val toggleOnClickListener = View.OnClickListener { view ->
                if (!_advertisementLoopService.advertising) {
                  startAdvertising()
                } else {
                    stopAdvertising()
                }
            }

            toggleBtn.setOnClickListener(toggleOnClickListener)
            animationView.setOnClickListener(toggleOnClickListener)

            _viewModel!!.isTransmitting.observe(viewLifecycleOwner) {
                if(it == true){
                    animationView.repeatCount = LottieDrawable.INFINITE
                    animationView.playAnimation()
                } else {
                    animationView.cancelAnimation()
                }
            }

            // include device name switch
            val includeDeviceNameSwitch: Switch = binding.swiftPairIncludeNameSwitch
            includeDeviceNameSwitch.setOnClickListener { view ->
                _bluetoothLeAdvertisementService.includeDeviceName = includeDeviceNameSwitch.isChecked
            }

            // txPower
            val fastPairingTxPowerSeekbar = binding.swiftPairTxPowerSeekbar
            val fastPairingTxPowerSeekbarLabel: TextView = binding.swiftPairTxPowerSeekbarLabel
            fastPairingTxPowerSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

                    var newTxPowerLevel = progress
                    var newTxPowerLabel = "High"

                    when (progress) {
                        0 -> {
                            newTxPowerLabel = "Ultra Low"
                        }
                        1 -> {
                            newTxPowerLabel = "Low"
                        }
                        2 -> {
                            newTxPowerLabel = "Medium"
                        }
                        3 -> {
                            newTxPowerLabel = "High"
                        } else -> {
                        newTxPowerLevel = 3
                        newTxPowerLabel = "High"
                    }
                    }

                    fastPairingTxPowerSeekbarLabel.text = "TX Power: ${newTxPowerLabel}"
                    _bluetoothLeAdvertisementService.txPowerLevel = newTxPowerLevel
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    // you can probably leave this empty
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    // you can probably leave this empty
                }
            })

            // seekbar
            val fastPairingRepeatitionSeekbar: SeekBar = binding.swiftPairRepeatitionSeekbar
            val fastPairingRepeatitionLabel: TextView = binding.swiftPairRepeatitionSeekbarLabel
            fastPairingRepeatitionSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    fastPairingRepeatitionLabel.text = "Advertise every ${progress} Seconds"
                    _advertisementLoopService.setIntervalSeconds(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    // currently not in use
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    // currently not in use
                }
            })

            // status label
            val statusLabelFastPairing: TextView = binding.statusLabelswiftPair
            _viewModel!!.statusText.observe(viewLifecycleOwner) {
                statusLabelFastPairing.text = it
            }

            // log scroll view
            val logView: LinearLayout = binding.swiftPairLogLinearView
            _viewModel!!.logEntries.observe(viewLifecycleOwner) {
                logView.removeAllViews()
                it.reversed().map { logEntryModel ->
                    val logEntryTextView: TextView = TextView(logView.context)
                    logEntryTextView.text = logEntryModel.message

                    when (logEntryModel.level){
                        LogLevel.Info -> {
                            logEntryTextView.setTextColor(ContextCompat.getColor(logView.context, R.color.log_info))
                        }
                        LogLevel.Warning -> {
                            logEntryTextView.setTextColor(ContextCompat.getColor(logView.context, R.color.log_warning))
                        }
                        LogLevel.Error -> {
                            logEntryTextView.setTextColor(ContextCompat.getColor(logView.context, R.color.log_error))
                        }
                        LogLevel.Success -> {
                            logEntryTextView.setTextColor(ContextCompat.getColor(logView.context, R.color.log_success))
                        }
                    }

                    logView.addView(logEntryTextView)
                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onAdvertisementStarted() {
        _viewModel!!.setStatusText("Started Advertising")
    }
    override fun onAdvertisementStopped() {
        _viewModel!!.setStatusText("Stopped Advertising")
    }

    override fun onAdvertisementSetStarted(advertisementSet: AdvertisementSet) {
        var message = "Advertising: ${advertisementSet.deviceName}"
        _viewModel!!.setStatusText(message)

        var logEntry = LogEntryModel()
        logEntry.level = LogLevel.Info
        logEntry.message = message
        _viewModel!!.addLogEntry(logEntry)
    }

    override fun onAdvertisementSetStopped(advertisementSet: AdvertisementSet) {
        // currently not in use
    }

    override fun onStartFailure(errorCode: Int) {
        var message = ""
        message = if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
            "ADVERTISE_FAILED_FEATURE_UNSUPPORTED"
        } else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
            "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
        } else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED) {
            "ADVERTISE_FAILED_ALREADY_STARTED"
        } else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE) {
            "ADVERTISE_FAILED_DATA_TOO_LARGE"
        } else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR) {
            "ADVERTISE_FAILED_INTERNAL_ERROR"
        } else {
            "unknown"
        }

        if (errorCode != AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED){
            var logEntry = LogEntryModel()
            logEntry.level = LogLevel.Error
            logEntry.message = message
            _viewModel!!.addLogEntry(logEntry)
        }
    }

    override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
        var logEntry = LogEntryModel()
        logEntry.level = LogLevel.Success
        logEntry.message = "Started advertising successfully"
        _viewModel!!.addLogEntry(logEntry)
    }

    override fun onAdvertisingSetStarted(
        advertisingSet: AdvertisingSet?,
        txPower: Int,
        status: Int
    ) {
        var logEntry = LogEntryModel()
        logEntry.level = LogLevel.Success
        logEntry.message = "Advertised successfully"
        _viewModel!!.addLogEntry(logEntry)
    }

    override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int) {
        // currently not in use
    }

    override fun onScanResponseDataSet(advertisingSet: AdvertisingSet, status: Int) {
        // currently not in use
    }

    override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
        // currently not in use
    }
}