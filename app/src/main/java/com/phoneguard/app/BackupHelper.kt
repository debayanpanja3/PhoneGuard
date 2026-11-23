package com.phoneguard.app

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports all contacts to a single .vcf file so they survive a factory reset
 * even if the Google account sync is interrupted or disabled by an attacker.
 */
object BackupHelper {

    fun backupContacts(context: Context): Result<String> {
        return try {
            val resolver = context.contentResolver
            val cursor = resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.LOOKUP_KEY),
                null, null, null
            ) ?: return Result.failure(Exception("Could not read contacts"))

            val builder = StringBuilder()
            cursor.use {
                while (it.moveToNext()) {
                    val lookupKey = it.getString(0)
                    val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey)
                    resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                        afd.createInputStream().use { input ->
                            builder.append(String(input.readBytes()))
                        }
                    }
                }
            }

            val fileName = "phoneguard_contacts_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.vcf"

            val savedPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/x-vcard")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(Exception("Could not create backup file"))
                resolver.openOutputStream(uri)?.use { it.write(builder.toString().toByteArray()) }
                "Downloads/$fileName"
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, fileName)
                FileOutputStream(file).use { it.write(builder.toString().toByteArray()) }
                file.absolutePath
            }

            Result.success(savedPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
