package com.phoneguard.app

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Severity { OK, WARN, DANGER }

data class Finding(
    val title: String,
    val detail: String,
    val severity: Severity
)

data class ScanReport(
    val findings: List<Finding>,
    val score: Int // 0-100, higher is safer
)

/**
 * Sensitive permissions worth flagging when an app holds several of them at once.
 */
private val SENSITIVE_PERMS = listOf(
    android.Manifest.permission.READ_SMS,
    android.Manifest.permission.RECEIVE_SMS,
    android.Manifest.permission.RECORD_AUDIO,
    android.Manifest.permission.CAMERA,
    android.Manifest.permission.ACCESS_FINE_LOCATION,
    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    android.Manifest.permission.READ_CONTACTS,
    android.Manifest.permission.READ_CALL_LOG,
    android.Manifest.permission.PROCESS_OUTGOING_CALLS,
    android.Manifest.permission.SYSTEM_ALERT_WINDOW
)

/** Common, well-known packages we don't want to scare the user about. */
private val TRUSTED_PREFIXES = listOf(
    "com.google.", "com.android.", "com.samsung.", "com.xiaomi.", "com.miui.",
    "com.whatsapp", "org.telegram", "com.facebook", "com.instagram"
)

object ScanEngine {

    fun run(context: Context): ScanReport {
        val findings = mutableListOf<Finding>()

        findings += checkRiskyApps(context)
        findings += checkDeviceAdmins(context)
        findings += checkAccessibilityServices(context)
        findings += checkUnknownSources(context)
        findings += checkSecurityPatch()

        val danger = findings.count { it.severity == Severity.DANGER }
        val warn = findings.count { it.severity == Severity.WARN }
        val score = (100 - danger * 25 - warn * 10).coerceIn(0, 100)

        return ScanReport(findings, score)
    }

    private fun checkRiskyApps(context: Context): List<Finding> {
        val pm = context.packageManager
        val apps = try {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            emptyList()
        }

        val risky = mutableListOf<Pair<String, Int>>()
        for (pkg in apps) {
            val name = pkg.packageName
            if (TRUSTED_PREFIXES.any { name.startsWith(it) }) continue
            val granted = pkg.requestedPermissions
                ?.filterIndexed { i, perm ->
                    perm in SENSITIVE_PERMS &&
                        (pkg.requestedPermissionsFlags?.getOrNull(i)?.and(PackageManager.REQUESTED_PERMISSION_GRANTED) ?: 0) != 0
                } ?: emptyList()
            if (granted.size >= 3) {
                val label = pm.getApplicationLabel(pkg.applicationInfo).toString()
                risky += label to granted.size
            }
        }

        return if (risky.isEmpty()) {
            listOf(Finding("App permissions look normal", "No third-party app is holding an unusually large bundle of sensitive permissions.", Severity.OK))
        } else {
            risky.sortedByDescending { it.second }.take(5).map { (label, count) ->
                Finding(
                    "$label holds $count sensitive permissions",
                    "This app can access things like camera, microphone, SMS or location together — worth reviewing in Settings > Apps if you don't recognize or rarely use it.",
                    Severity.WARN
                )
            }
        }
    }

    private fun checkDeviceAdmins(context: Context): List<Finding> {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return listOf(Finding("Device admin check unavailable", "Could not read device admin status on this device.", Severity.WARN))

        val admins = dpm.activeAdmins ?: emptyList()
        val suspicious = admins.filterNot { admin ->
            val pkg = admin.packageName
            TRUSTED_PREFIXES.any { pkg.startsWith(it) }
        }

        return if (suspicious.isEmpty()) {
            listOf(Finding("No unfamiliar device admin apps", "Device administrator access — a common way attackers keep control of a phone — is not granted to any unrecognized app.", Severity.OK))
        } else {
            suspicious.map { admin ->
                Finding(
                    "Device admin: ${admin.packageName}",
                    "This app has device administrator rights, which can block uninstalling it and allow remote control. If you don't recognize it, remove its admin access in Settings > Security > Device admin apps, then uninstall it.",
                    Severity.DANGER
                )
            }
        }
    }

    private fun checkAccessibilityServices(context: Context): List<Finding> {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (enabled.isNullOrBlank()) {
            return listOf(Finding("No accessibility services enabled", "Accessibility access — often abused by stalkerware to read your screen — is currently off for every app.", Severity.OK))
        }
        val services = enabled.split(":").filter { it.isNotBlank() }
        val suspicious = services.filterNot { svc -> TRUSTED_PREFIXES.any { svc.startsWith(it) } }
        return if (suspicious.isEmpty()) {
            listOf(Finding("Accessibility services look familiar", "Only recognized apps are using accessibility access.", Severity.OK))
        } else {
            suspicious.map { svc ->
                Finding(
                    "Accessibility service: $svc",
                    "This service can read everything on your screen and simulate taps. If you didn't turn this on yourself, disable it in Settings > Accessibility immediately.",
                    Severity.DANGER
                )
            }
        }
    }

    private fun checkUnknownSources(context: Context): List<Finding> {
        return try {
            val canInstall = context.packageManager.canRequestPackageInstalls()
            if (canInstall) {
                listOf(Finding(
                    "Installing from unknown sources is allowed",
                    "Your phone currently allows apps to be installed from outside the Play Store. Turn this off in Settings > Apps > Special access > Install unknown apps unless you're using it right now.",
                    Severity.WARN
                ))
            } else {
                listOf(Finding("Unknown-source installs are blocked", "Apps can't silently install themselves from outside the Play Store.", Severity.OK))
            }
        } catch (e: Exception) {
            listOf(Finding("Could not check install sources", "This check needs a newer Android version to run.", Severity.WARN))
        }
    }

    private fun checkSecurityPatch(): List<Finding> {
        val patch = Build.VERSION.SECURITY_PATCH ?: return listOf(
            Finding("Security patch date unknown", "Couldn't read the security patch level on this device.", Severity.WARN)
        )
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val patchDate = fmt.parse(patch) ?: return listOf(Finding("Security patch: $patch", "Could not compare this date.", Severity.WARN))
            val ageDays = ((Date().time - patchDate.time) / (1000L * 60 * 60 * 24)).toInt()
            when {
                ageDays > 180 -> listOf(Finding(
                    "Security patch is $ageDays days old",
                    "Your phone hasn't received a security update in a long time. Check Settings > System > Software update.",
                    Severity.DANGER
                ))
                ageDays > 90 -> listOf(Finding(
                    "Security patch is $ageDays days old",
                    "Consider checking for a software update soon.",
                    Severity.WARN
                ))
                else -> listOf(Finding(
                    "Security patch is current ($patch)",
                    "Your device has received a security update within the last 3 months.",
                    Severity.OK
                ))
            }
        } catch (e: Exception) {
            listOf(Finding("Security patch: $patch", "Could not parse this date.", Severity.WARN))
        }
    }
}
