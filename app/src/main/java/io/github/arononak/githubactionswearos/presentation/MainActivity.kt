package io.github.arononak.githubactionswearos.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import io.github.arononak.githubactionswearos.GithubViewModel
import io.github.arononak.githubactionswearos.presentation.theme.GithubActionsWearOSTheme
import kotlinx.coroutines.*
import java.lang.Integer.max
import java.lang.Integer.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GithubActionsApp()
        }
    }
}

@Composable
fun GithubActionsApp() {
    val githubViewModel: GithubViewModel = viewModel()
    val navController = rememberSwipeDismissableNavController()

    GithubActionsWearOSTheme {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = "Landing",
            modifier = Modifier.background(MaterialTheme.colors.background)
        ) {
            composable("Landing") {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background),
                ) {
                    GithubActionsStatus(text = githubViewModel.state.collectAsState().value.status)
                    Spacer(modifier = Modifier.height(32.dp))
                    Chip(
                        icon = {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_preferences),
                                contentDescription = "Settings",
                                modifier = Modifier.wrapContentSize(align = Alignment.Center),
                            )
                        },
                        label = { Text(text = "Settings") },
                        onClick = { navController.navigate("Settings") },
                    )
                }
            }
            composable("Settings") {
                val state = githubViewModel.state.collectAsState().value

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        GithubActionsNumberPicker(
                            min = 5,
                            max = 60,
                            value = state.settings?.refreshTime ?: 60,
                            onValueChanged = githubViewModel::onRefreshTimeChanged,
                        )
                        GithubActionsTextField(
                            hint = "Owner",
                            text = state.settings?.owner ?: "",
                            onTextChanged = githubViewModel::onChangedOwner,
                        )
                        GithubActionsTextField(
                            hint = "Repo",
                            text = state.settings?.repo ?: "",
                            onTextChanged = githubViewModel::onChangedRepo,
                        )
                        GithubActionsTextField(
                            hint = "Token (private repo)",
                            text = state.settings?.token ?: "",
                            onTextChanged = githubViewModel::onChangedToken,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                        ) {
                            Text("Back")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GithubActionsNumberPicker(
    min: Int = 5,
    max: Int = 60,
    value: Int = 5,
    onValueChanged: (Int) -> Unit,
) {
    var number by remember { mutableStateOf(value) }
    var text by remember { mutableStateOf(number.toString()) }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(text = "Refresh time")
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    number = max(min, number - 1)
                    text = number.toString()
                    onValueChanged(number)
                }, modifier = Modifier.weight(1f)
            ) {
                Text(text = "-")
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f), contentAlignment = Alignment.Center
            ) {
                Text(text = text)
            }
            Button(
                onClick = {
                    number = min(max, number + 1)
                    text = number.toString()
                    onValueChanged(number)
                }, modifier = Modifier.weight(1f)
            ) {
                Text(text = "+")
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GithubActionsTextField(
    hint: String,
    text: String,
    onTextChanged: (String) -> Unit,
) {
    var value by remember { mutableStateOf(text) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = hint)
        Spacer(modifier = Modifier.height(2.dp))
        BasicTextField(
            value = value,
            onValueChange = {
                if (value.length == 1 && it.isEmpty()) {
                    value = ""
                    onTextChanged("")
                } else {
                    if (it.isNotEmpty()) {
                        value = it
                        onTextChanged(it)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            textStyle = TextStyle(color = Color.White),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White)
                .padding(4.dp)
                .onFocusChanged {})
    }
}

@Composable
fun GithubActionsStatus(text: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = text,
    )
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    GithubActionsApp()
}