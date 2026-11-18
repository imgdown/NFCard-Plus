package com.nfcard.plus

import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nfcard.plus.ui.MainScreen
import com.nfcard.plus.ui.theme.NFCardTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ReaderViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null

    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        viewModel.onTagDiscovered(tag)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setContent {
            NFCardTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                MainScreen(
                    state = state,
                    nfcAvailable = nfcAdapter != null,
                    onModeChange = viewModel::setMode,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this,
            readerCallback,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null,
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }
}
