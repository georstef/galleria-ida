package com.galleriaida.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel
import java.util.Locale
import androidx.compose.ui.focus.onFocusChanged

data class LanguageOption(
    val displayName: String,
    val code: String,
    val isKeyboard: Boolean
)

fun getLanguageOptions(context: android.content.Context): List<LanguageOption?> {
    val keyboardLocales = mutableListOf<LanguageOption>()
    val seen = mutableSetOf<String>()

    try {
        val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager

        imm.enabledInputMethodList.forEach { imi ->
            imm.getEnabledInputMethodSubtypeList(imi, true).forEach { subtype ->
                if (subtype.mode == "keyboard") {
                    val locale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        android.icu.util.ULocale.forLanguageTag(subtype.languageTag).toLocale()
                    } else {
                        Locale(subtype.locale.substringBefore("_"))
                    }
                    val code = locale.language
                    if (code.isNotBlank() && seen.add(code)) {
                        keyboardLocales.add(
                            LanguageOption(
                                displayName = locale.getDisplayLanguage(locale)
                                    .replaceFirstChar { it.uppercase() },
                                code = code,
                                isKeyboard = true
                            )
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("Languages", "Failed to get keyboard languages: ${e.message}")
    }

    val allLanguages = listOf(
        "en" to "English", "el" to "Greek", "es" to "Spanish",
        "fr" to "French", "de" to "German", "it" to "Italian",
        "pt" to "Portuguese", "nl" to "Dutch", "ru" to "Russian",
        "zh" to "Chinese", "ar" to "Arabic", "tr" to "Turkish",
        "pl" to "Polish", "sv" to "Swedish", "da" to "Danish"
    ).filter { it.first !in seen }
        .map { LanguageOption(it.second, it.first, false) }

    val result = mutableListOf<LanguageOption?>()
    result.addAll(keyboardLocales)
    if (keyboardLocales.isNotEmpty() && allLanguages.isNotEmpty()) {
        result.add(null)
    }
    result.addAll(allLanguages)

    if (result.filterNotNull().isEmpty()) {
        result.add(LanguageOption("English", "en", true))
    }

    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBasicSetupScreen(
    viewModel: AppViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val languageOptions = remember { getLanguageOptions(context) }
    val selectableOptions = languageOptions.filterNotNull()

    var name by remember { mutableStateOf("") }
    var selectedLanguage by remember {
        mutableStateOf(selectableOptions.firstOrNull() ?: LanguageOption("English", "en", true))
    }
    var expanded by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar with back arrow
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp)
                    .padding(top = 24.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "🌟 Welcome!",
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Let's create your profile",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MedText,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(48.dp))

                var nameTouched by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Your name", style = MaterialTheme.typography.bodyLarge) },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && nameTouched && name.isNotBlank()) {
                                nameError = viewModel.isNameTaken(name.trim())
                            }
                            if (focusState.isFocused) nameTouched = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                if (nameError) {
                    Text(
                        "This name is already taken",
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedLanguage.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Language", style = MaterialTheme.typography.bodyLarge) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        languageOptions.forEach { option ->
                            if (option == null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = DisabledGray
                                )
                            } else {
                                DropdownMenuItem(
                                    text = {
                                        Text(option.displayName, style = MaterialTheme.typography.bodyLarge)
                                    },
                                    onClick = {
                                        selectedLanguage = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))

                Button(
                    onClick = {
                        when {
                            name.isBlank() -> { nameError = true }
                            viewModel.isNameTaken(name.trim()) -> { nameError = true }
                            else -> {
                                viewModel.createPlayerBasic(name, selectedLanguage.code)
                                onContinue()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
                ) {
                    Text("Continue ➡️", style = MaterialTheme.typography.labelLarge)
                }
            }
        } // end outer Column
    }
}