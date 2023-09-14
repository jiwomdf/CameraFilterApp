package com.katilijiwoadiwiyono.filterrecord.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * This extension can be called by Activity to handle callback for request single permission
 */
fun AppCompatActivity.requestPermissionLauncher(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
): ActivityResultLauncher<String> {
    return registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            onPermissionGranted.invoke()
        } else {
            onPermissionDenied.invoke()
        }
    }
}

/**
 * This extension can be called by Activity to handle callback for request multiple permission
 * If permission is denied, it will return the name of permission that denied by user
 */
fun AppCompatActivity.requestMultiplePermissionLauncher(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: (permission: String) -> Unit
): ActivityResultLauncher<Array<String>> {
    return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

        var isAllPermissionGranted = true
        permissions.forEach {
            if (!it.value) {
                isAllPermissionGranted = false
                onPermissionDenied.invoke(it.key)
            }
        }

        if (isAllPermissionGranted) {
            onPermissionGranted.invoke()
        }
    }
}

/**
 * This extension can be called by Activity to check single permission
 * onShowRequestPermissionRationale by default will return dialog to go to setting, or you can override with custom UI
 */
fun AppCompatActivity.checkPermission(
    permission: String,
    launcher: ActivityResultLauncher<String>,
    onPermissionGranted: () -> Unit,
    onShowRequestPermissionRationale: (() -> Unit) = { showRequestPermissionRationaleDialog(this) }
) {
    if (hasPermission(this, permission)) {
        onPermissionGranted.invoke()
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(permission)) {
                onShowRequestPermissionRationale.invoke()
            } else {
                launcher.launch(permission)
            }
        } else {
            launcher.launch(permission)
        }
    }
}

/**
 * This extension can be called by Activity to check multiple permission
 * onShowRequestPermissionRationale by default will return dialog to go to setting, or you can override with custom UI
 */
fun AppCompatActivity.checkMultiplePermissions(
    permissions: Array<String>,
    launcher: ActivityResultLauncher<Array<String>>,
    onPermissionGranted: () -> Unit,
    onShowRequestPermissionRationale: (() -> Unit) = { showRequestPermissionRationaleDialog(this) }
) {
    if (hasPermissions(this, permissions)) {
        onPermissionGranted.invoke()
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissions.all { shouldShowRequestPermissionRationale(it) }) {
                onShowRequestPermissionRationale.invoke()
            } else {
                launcher.launch(permissions)
            }
        } else {
            launcher.launch(permissions)
        }
    }
}

/**
 * This extension can be called by Fragment to request single permission
 */
fun Fragment.requestPermissionLauncher(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
): ActivityResultLauncher<String> {
    return registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            onPermissionGranted.invoke()
        } else {
            onPermissionDenied.invoke()
        }
    }
}

/**
 * This extension can be called by Fragment to handle callback for request multiple permission
 * If permission is denied, it will return the name of permission that denied by user
 */
fun Fragment.requestMultiplePermissionLauncher(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: (permission: String) -> Unit
): ActivityResultLauncher<Array<String>> {
    return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        var isAllPermissionGranted = true
        permissions.forEach {
            if (!it.value) {
                isAllPermissionGranted = false
                onPermissionDenied.invoke(it.key)
            }
        }

        if (isAllPermissionGranted) {
            onPermissionGranted.invoke()
        }
    }
}

/**
 * This extension can be called by Fragment to check single permission
 * onShowRequestPermissionRationale by default will return dialog to go to setting, or you can override with custom UI
 */
fun Fragment.checkPermission(
    permission: String,
    launcher: ActivityResultLauncher<String>,
    onPermissionGranted: () -> Unit,
    onShowRequestPermissionRationale: (() -> Unit) = { showRequestPermissionRationaleDialog(this.requireContext()) }
) {
    if (hasPermission(this.requireContext(), permission)) {
        onPermissionGranted.invoke()
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(permission)) {
                onShowRequestPermissionRationale.invoke()
            } else {
                launcher.launch(permission)
            }
        } else {
            launcher.launch(permission)
        }
    }
}

/**
 * This extension can be called by Fragment to check multiple permission
 * onShowRequestPermissionRationale by default will return dialog to go to setting, or you can override with custom UI
 */
fun Fragment.checkMultiplePermissions(
    permissions: Array<String>,
    launcher: ActivityResultLauncher<Array<String>>,
    onPermissionGranted: () -> Unit,
    onShowRequestPermissionRationale: (() -> Unit) = { showRequestPermissionRationaleDialog(this.requireContext()) }
) {
    if (hasPermissions(this.requireContext(), permissions)) {
        onPermissionGranted.invoke()
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissions.all { shouldShowRequestPermissionRationale(it) }) {
                onShowRequestPermissionRationale.invoke()
            } else {
                launcher.launch(permissions)
            }
        } else {
            launcher.launch(permissions)
        }
    }
}

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun hasPermissions(context: Context, permissions: Array<String>): Boolean =
    permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/**
 * Default request permission rationale dialog
 */
private fun showRequestPermissionRationaleDialog(context: Context) {
    val builder = AlertDialog.Builder(context)
    builder.setTitle("Permissions required")
        .setMessage("Some permissions are needed to be allowed to use this app without any problems.")
        .setPositiveButton("Ok") { dialog, _ ->
            dialog.cancel()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            context.startActivity(intent)
        }.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }.show()
}