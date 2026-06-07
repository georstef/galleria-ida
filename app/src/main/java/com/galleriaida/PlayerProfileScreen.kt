package com.galleriaida.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    viewModel: AppViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit = {}
) {
    val player by viewModel.currentPlayer.collectAsState()
    val uiStrings by viewModel.uiStrings.collectAsState()
    val context = LocalContext.current

    val languageOptions = remember { getLanguageOptions(context) }
    val selectableOptions = languageOptions.filterNotNull()

    key(player?.id) {
        var name by remember { mutableStateOf(player?.name ?: "") }
        var nameError by remember { mutableStateOf("") }
        var nameTouched by remember { mutableStateOf(false) }
        var schoolClass by remember {
            mutableStateOf(player?.schoolClass?.takeIf { it != "—" } ?: "")
        }
        var schoolYearPosition by remember {
            mutableStateOf(player?.schoolYearPosition ?: "")
        }
        var yearExpanded by remember { mutableStateOf(false) }
        var langExpanded by remember { mutableStateOf(false) }
        var classExpanded by remember { mutableStateOf(false) }

        var selectedLanguage by remember {
            mutableStateOf(
                selectableOptions.firstOrNull { it.code == player?.language }
                    ?: selectableOptions.firstOrNull()
                    ?: LanguageOption("English", "en", true)
            )
        }

        val yearOptions = listOf(
            Pair(uiStrings.profileYearBeginning, "beginning"),
            Pair(uiStrings.profileYearMiddle, "middle"),
            Pair(uiStrings.profileYearEnd, "end")
        )
        val selectedYearLabel = yearOptions.firstOrNull { it.second == schoolYearPosition }?.first ?: ""
        val classOptions = (1..12).map { "Class $it" }
        val initial = player?.name?.firstOrNull()?.uppercase() ?: "?"

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .imePadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
                    }
                    Text(uiStrings.profileTitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(48.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp)
                        .padding(top = 8.dp, bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    // Avatar circle
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(SoftPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepPurple
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Stars badge
                    Row(
                        modifier = Modifier
                            .background(LemonYellow, RoundedCornerShape(40.dp))
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⭐", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            uiStrings.profileStarsCollected.format(player?.stars ?: 0),
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepPurple
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameError = "" },
                        label = { Text(uiStrings.profileLabelName, style = MaterialTheme.typography.bodyLarge) },
                        isError = nameError.isNotBlank(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused && nameTouched && name.isNotBlank()) {
                                    if (viewModel.isNameTaken(name.trim(), excludeId = player?.id)) {
                                        nameError = uiStrings.profileErrorNameTaken
                                    }
                                }
                                if (focusState.isFocused) nameTouched = true
                            },
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                    if (nameError.isNotBlank()) {
                        Text(
                            nameError,
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Language
                    ExposedDropdownMenuBox(
                        expanded = langExpanded,
                        onExpandedChange = { langExpanded = !langExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedLanguage.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(uiStrings.profileLabelLanguage, style = MaterialTheme.typography.bodyLarge) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(16.dp),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                        ExposedDropdownMenu(
                            expanded = langExpanded,
                            onDismissRequest = { langExpanded = false }
                        ) {
                            languageOptions.forEach { option ->
                                if (option == null) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = DisabledGray
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(option.displayName, style = MaterialTheme.typography.bodyLarge) },
                                        onClick = { selectedLanguage = option; langExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // School class
                    ExposedDropdownMenuBox(
                        expanded = classExpanded,
                        onExpandedChange = { classExpanded = !classExpanded }
                    ) {
                        OutlinedTextField(
                            value = schoolClass,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(uiStrings.profileLabelSchoolClass, style = MaterialTheme.typography.bodyLarge) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(16.dp),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                        ExposedDropdownMenu(
                            expanded = classExpanded,
                            onDismissRequest = { classExpanded = false }
                        ) {
                            classOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                                    onClick = { schoolClass = option; classExpanded = false }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // School year position
                    ExposedDropdownMenuBox(
                        expanded = yearExpanded,
                        onExpandedChange = { yearExpanded = !yearExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedYearLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(uiStrings.profileLabelSchoolYear, style = MaterialTheme.typography.bodyLarge) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(16.dp),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                        ExposedDropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false }
                        ) {
                            yearOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.first, style = MaterialTheme.typography.bodyLarge) },
                                    onClick = { schoolYearPosition = option.second; yearExpanded = false }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(48.dp))

                    Button(
                        onClick = {
                            when {
                                name.isBlank() -> {
                                    nameError = uiStrings.profileErrorNameBlank
                                }
                                viewModel.isNameTaken(name.trim(), excludeId = player?.id) -> {
                                    nameError = uiStrings.profileErrorNameTaken
                                }
                                else -> {
                                    player?.let {
                                        viewModel.updatePlayer(
                                            it.copy(
                                                name = name.trim(),
                                                language = selectedLanguage.code,
                                                schoolClass = schoolClass.ifBlank { "—" },
                                                schoolYearPosition = schoolYearPosition.ifBlank { "beginning" }
                                            )
                                        )
                                    }
                                    onDone()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
                    ) {
                        Text(uiStrings.profileSaveButton, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Settings cog — bottom right floating
            IconButton(
                onClick = onSettings,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(20.dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SoftPurple)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = DeepPurple,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}