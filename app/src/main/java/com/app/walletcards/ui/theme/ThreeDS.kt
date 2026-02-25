package com.app.walletcards.ui.theme

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.app.walletcards.model.ThreeDSResponse
import com.app.walletcards.network.CardApiService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreeDSBottomSheet(response: ThreeDSResponse, cardId: String, userEmail: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("3DS Authentication")
            Spacer(modifier = Modifier.height(16.dp))
            response.data?.let {
                Text("Merchant: ${it.merchantName}")
                Text("Amount: ${it.merchantAmount} ${it.merchantCurrency}")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { 
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion { 
                        if (!sheetState.isVisible) {
                            onDismiss()
                        }
                    }
                 }) {
                    Text("Reject")
                }
                Button(onClick = { 
                    scope.launch {
                        response.data?.let {
                            val apiResponse = CardApiService.approve3ds(userEmail, cardId, it.eventId)
                            if (apiResponse?.isSuccessful == true) {
                                Toast.makeText(context, "Transaction approved", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Approval failed. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        sheetState.hide()
                    }.invokeOnCompletion { 
                        if (!sheetState.isVisible) {
                            onDismiss()
                        }
                    }
                 }) {
                    Text("Approve")
                }
            }
        }
    }
}