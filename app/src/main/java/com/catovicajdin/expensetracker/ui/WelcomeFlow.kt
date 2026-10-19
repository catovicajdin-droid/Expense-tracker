package com.catovicajdin.expensetracker.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.backup.DataTransfer
import com.catovicajdin.expensetracker.notifications.Bank
import com.catovicajdin.expensetracker.notifications.BankPreferences
import com.catovicajdin.expensetracker.notifications.TrackingMode
import com.catovicajdin.expensetracker.notifications.TrackingPreferences
import com.catovicajdin.expensetracker.ui.components.CategoryIconBadge
import kotlinx.coroutines.launch

private enum class Step { WELCOME, MODE, BANK, ACCESS, CATEGORIES }

/**
 * First run: what the app does, automatic or manual tracking, which bank, notification access, and
 * the starting categories. Manual users skip the bank and access steps entirely, since without
 * notification reading neither does anything.
 *
 * The automatic choice is presented as a 7-day trial with one bank, but nothing enforces the trial
 * yet - that arrives with Play Billing.
 */
@Composable
fun WelcomeFlow(
    isNotificationAccessGranted: Boolean,
    onRequestNotificationAccess: () -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(Step.WELCOME) }
    var mode by remember { mutableStateOf(TrackingMode.AUTOMATIC) }
    var bank by remember { mutableStateOf(Bank.INTESA) }

    fun finish() {
        TrackingPreferences.finish(context, mode)
        onFinished()
    }

    // Coming back from the system screen with access granted moves straight on.
    LaunchedEffect(step, isNotificationAccessGranted) {
        if (step == Step.ACCESS && isNotificationAccessGranted) step = Step.CATEGORIES
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        when (step) {
            Step.WELCOME -> WelcomeStep(onNext = { step = Step.MODE }, onRestored = { finish() })
            Step.MODE -> ModeStep(
                selected = mode,
                onSelect = { mode = it },
                onBack = { step = Step.WELCOME },
                onNext = { step = if (mode == TrackingMode.AUTOMATIC) Step.BANK else Step.CATEGORIES },
            )
            Step.BANK -> BankStep(
                selected = bank,
                onSelect = { bank = it },
                onBack = { step = Step.MODE },
                onNext = {
                    BankPreferences.setEnabled(context, setOf(bank))
                    step = if (isNotificationAccessGranted) Step.CATEGORIES else Step.ACCESS
                },
            )
            Step.ACCESS -> AccessStep(
                bank = bank,
                onGrant = onRequestNotificationAccess,
                onBack = { step = Step.BANK },
            )
            Step.CATEGORIES -> CategoriesStep(
                onBack = {
                    step = when {
                        mode == TrackingMode.MANUAL -> Step.MODE
                        isNotificationAccessGranted -> Step.BANK
                        else -> Step.ACCESS
                    }
                },
                onNext = { finish() },
            )
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit, onRestored: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingRestore by remember { mutableStateOf<DataTransfer.RestoreCheck.Ok?>(null) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            when (val check = DataTransfer.checkBackup(context, uri)) {
                is DataTransfer.RestoreCheck.Ok -> pendingRestore = check
                is DataTransfer.RestoreCheck.Rejected -> Toast.makeText(context, check.reason, Toast.LENGTH_LONG).show()
            }
        }
    }

    Text("💸", fontSize = 40.sp)
    Heading("Know where your money goes")
    Body(
        "Expense Tracker turns your bank's payment notifications into a spending log, sorted into " +
            "categories and checked against your monthly budget.",
    )
    Bullet("🔔", "Every card payment is picked up as it happens, and one tap files it.")
    Bullet("📊", "Budgets per category, with a heads-up at 50% and 75%.")
    Bullet("🔒", "Everything stays on this phone. No bank login, no account, no cloud.")
    PrimaryButton("Get started", onNext)
    TextButton(onClick = { restore.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
        Text("I have a backup to restore", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    pendingRestore?.let { check ->
        AlertDialog(
            onDismissRequest = {
                check.staged.delete()
                pendingRestore = null
            },
            title = { Text("Restore this backup?") },
            text = { Text("It holds ${check.transactionCount} transactions. The app restarts with them.") },
            confirmButton = {
                TextButton(onClick = {
                    // Marked done first: the restore restarts the app, which would otherwise land
                    // back here.
                    onRestored()
                    DataTransfer.restore(context, check.staged)
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = {
                    check.staged.delete()
                    pendingRestore = null
                }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ModeStep(selected: TrackingMode, onSelect: (TrackingMode) -> Unit, onBack: () -> Unit, onNext: () -> Unit) {
    Heading("How do you want to track?")
    ChoiceCard(
        title = "Automatic",
        badge = "Free for 7 days",
        text = "Payments are read from your bank app's notifications. After the trial it's part of the subscription.",
        selected = selected == TrackingMode.AUTOMATIC,
        onClick = { onSelect(TrackingMode.AUTOMATIC) },
    )
    ChoiceCard(
        title = "Manual",
        badge = "Free",
        text = "You add each transaction yourself. Budgets, categories and charts all work the same.",
        selected = selected == TrackingMode.MANUAL,
        onClick = { onSelect(TrackingMode.MANUAL) },
    )
    Body("You can switch later in Settings.")
    PrimaryButton("Continue", onNext)
    BackButton(onBack)
}

@Composable
private fun BankStep(selected: Bank, onSelect: (Bank) -> Unit, onBack: () -> Unit, onNext: () -> Unit) {
    Heading("Which bank do you use?")
    Body("The trial reads one bank. With a subscription you can add the others in Settings.")
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Bank.entries.forEach { bank ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onSelect(bank) }.padding(vertical = 6.dp),
            ) {
                RadioButton(
                    selected = bank == selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.onBackground),
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(bank.displayName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${bank.appName} app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    PrimaryButton("Continue", onNext)
    BackButton(onBack)
}

@Composable
private fun AccessStep(bank: Bank, onGrant: () -> Unit, onBack: () -> Unit) {
    Text("🔔", fontSize = 40.sp)
    Heading("Allow notification access")
    Body(
        "Android asks you to allow this on a system screen. Find Expense Tracker in the list and turn " +
            "it on, then come back.",
    )
    Body(
        "Only notifications from the ${bank.appName} app are read. Messages, emails and every other app " +
            "are ignored, and nothing is sent anywhere.",
    )
    PrimaryButton("Open settings", onGrant)
    BackButton(onBack)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoriesStep(onBack: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    val categories by AppDatabase.get(context).categoryDao().all().collectAsState(initial = emptyList())

    Heading("Your categories")
    Body("You start with these. Rename them, add your own or remove any in Settings → Categories.")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 16.dp),
    ) {
        categories.forEach { category ->
            Surface(
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(6.dp, 4.dp, 12.dp, 4.dp)) {
                    CategoryIconBadge(category, size = 24.dp)
                    Text(category.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
    PrimaryButton("Start tracking", onNext)
    BackButton(onBack)
}

@Composable
private fun ChoiceCard(title: String, badge: String, text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(badge, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
}

@Composable
private fun Body(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun Bullet(icon: String, text: String) {
    Row(modifier = Modifier.padding(top = 14.dp)) {
        Text(icon, fontSize = 20.sp)
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onBackground,
            contentColor = MaterialTheme.colorScheme.background,
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
    ) { Text(text) }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
