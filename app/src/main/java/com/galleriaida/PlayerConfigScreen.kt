package com.galleriaida.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerConfigScreen(
    viewModel: AppViewModel,
    onPlayerCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var schoolClass by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("English") }
    var expanded by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    val languages = listOf("English", "Greek", "Spanish", "French", "German", "Italian", "Portuguese", "Dutch")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create your profile! 🎉",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            label = { Text("Your name", style = MaterialTheme.typography.bodyLarge) },
            isError = nameError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyLarge
        )
        if (nameError) {
            Text("Please enter a name", color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = schoolClass,
            onValueChange = { schoolClass = it },
            label = { Text("School class (e.g. 3B)", style = MaterialTheme.typography.bodyLarge) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(20.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = language,
                onValueChange = {},
                readOnly = true,
                label = { Text("Language", style = MaterialTheme.typography.bodyLarge) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                languages.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang, style = MaterialTheme.typography.bodyLarge) },
                        onClick = { language = lang; expanded = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = {
                if (name.isBlank()) { nameError = true; return@Button }
                viewModel.createPlayer(name, schoolClass.ifBlank { "—" }, language)
                onPlayerCreated()
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
        ) {
            Text("Let's Go! 🚀", style = MaterialTheme.typography.labelLarge)
        }
    }
}
