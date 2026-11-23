package com.phoneguard.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoneguard.app.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhoneGuardTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    PhoneGuardApp()
                }
            }
        }
    }
}

@Composable
fun PhoneGuardApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var report by remember { mutableStateOf<ScanReport?>(null) }
    var scanning by remember { mutableStateOf(false) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val result = BackupHelper.backupContacts(context)
            result.onSuccess {
                Toast.makeText(context, "Contacts saved to $it", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(context, "Backup failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Contacts permission is needed to back them up", Toast.LENGTH_SHORT).show()
        }
    }

    fun runScan() {
        scanning = true
        report = null
    }

    LaunchedEffect(scanning) {
        if (scanning) {
            delay(900) // lets the scanning animation play
            report = ScanEngine.run(context)
            scanning = false
        }
    }

    LaunchedEffect(Unit) { runScan() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(BgDeep, Color(0xFF0A0E1A)))
            )
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        Text("PhoneGuard", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        Text(
            "A quick, honest check of your phone's security",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(24.dp))

        ScoreRing(scanning = scanning, score = report?.score ?: 0)

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { runScan() },
            enabled = !scanning,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = BgDeep)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (scanning) "Scanning..." else "Scan again", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentSoft)
        ) {
            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Back up contacts now")
        }

        Spacer(Modifier.height(20.dp))

        AnimatedVisibility(visible = report != null, enter = fadeIn(), exit = fadeOut()) {
            report?.let { r ->
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(r.findings) { finding -> FindingCard(finding) }
                }
            }
        }
    }
}

@Composable
fun ScoreRing(scanning: Boolean, score: Int) {
    val animatedScore by animateFloatAsState(
        targetValue = if (scanning) 0f else score.toFloat(),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "score"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val spin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "spinAngle"
    )

    val ringColor = when {
        scanning -> Accent
        score >= 80 -> Safe
        score >= 50 -> Warn
        else -> Danger
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(170.dp)) {
            val stroke = 14.dp.toPx()
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (scanning) {
                drawArc(
                    color = ringColor,
                    startAngle = spin,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            } else {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * (animatedScore / 100f),
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (scanning) {
                Text("Scanning", color = TextSecondary, style = MaterialTheme.typography.titleMedium)
            } else {
                Text(
                    "${animatedScore.toInt()}",
                    color = TextPrimary,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Safety score", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun FindingCard(finding: Finding) {
    var expanded by remember { mutableStateOf(finding.severity != Severity.OK) }

    val (icon, color) = when (finding.severity) {
        Severity.OK -> Icons.Filled.CheckCircle to Safe
        Severity.WARN -> Icons.Filled.Warning to Warn
        Severity.DANGER -> Icons.Filled.Error to Danger
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                finding.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = TextSecondary
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(250)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
        ) {
            Text(
                finding.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 10.dp, start = 46.dp)
            )
        }
    }
}
