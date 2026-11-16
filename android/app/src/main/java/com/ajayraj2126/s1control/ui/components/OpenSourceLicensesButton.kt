package com.ajayraj2126.s1control.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajayraj2126.s1control.ui.theme.AccentMint
import com.ajayraj2126.s1control.ui.theme.SurfaceDark
import com.ajayraj2126.s1control.ui.theme.TextPrimary
import com.ajayraj2126.s1control.ui.theme.TextSecondary

/** Keeps the original authorship and full license accessible apart from app branding. */
@Composable
fun OpenSourceLicensesButton(modifier: Modifier = Modifier) {
    var showLicenses by remember { mutableStateOf(false) }

    TextButton(onClick = { showLicenses = true }, modifier = modifier) {
        Text("Open-source licenses", color = AccentMint)
    }

    if (showLicenses) {
        val context = LocalContext.current
        val originalLicense = remember(context) {
            context.assets.open("licenses/S1Mods-LICENSE.txt")
                .bufferedReader().use { it.readText() }
        }
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text("Open-source licenses", color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(
                            text = "S1 Control by AJ includes S1Mods code. " +
                                "The original authorship and license notices follow.\n\n" +
                                originalLicense,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicenses = false }) {
                    Text("Close", color = TextPrimary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
