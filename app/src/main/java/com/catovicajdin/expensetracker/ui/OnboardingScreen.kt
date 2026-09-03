package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onRequestNotificationAccess: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text("Expense Tracker needs Notification Access to read transaction alerts from your Intesa Mobile app.")
        Button(onClick = onRequestNotificationAccess, modifier = Modifier.padding(top = 16.dp)) {
            Text("Grant notification access")
        }
    }
}
