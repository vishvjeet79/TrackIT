package com.example.trackit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trackit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel? = null
) {
    if (LocalInspectionMode.current && viewModel == null) {
        SettingsContent(
            isDarkModeEnabled = false,
            onDarkModeToggle = {},
            modifier = modifier
        )
    } else {
        val actualViewModel: SettingsViewModel = viewModel ?: viewModel(factory = AppViewModelProvider.Factory)
        val isDarkModeEnabled by actualViewModel.isDarkModeEnabled.collectAsStateWithLifecycle()
        
        SettingsContent(
            isDarkModeEnabled = isDarkModeEnabled,
            onDarkModeToggle = { actualViewModel.toggleDarkMode(it) },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    isDarkModeEnabled: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.dark_mode),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isDarkModeEnabled,
                    onCheckedChange = onDarkModeToggle
                )
            }
        }
    }
}
