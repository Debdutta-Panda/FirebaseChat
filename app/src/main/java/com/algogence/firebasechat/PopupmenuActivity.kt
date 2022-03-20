package com.algogence.firebasechat

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

class PopupmenuActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContent {
            OverFlowMenuDemo()
        }
    }
}

@Composable
fun OverFlowMenuDemo() {
    val context = LocalContext.current
    val expanded = remember { mutableStateOf(false) }
    val menuItems = listOf("Cancel", "Delete", "View")
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        content = {

        },
        topBar = {
            TopAppBar(title = { Text("Overflow Menu") },
                backgroundColor = Color.Blue,
                contentColor = Color.White,
                actions = {
                    IconButton(
                        onClick = {
                            expanded.value = true
                        }

                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "image",
                            tint = Color.White,
                        )
                    }
                    DropdownMenu(
                        expanded = expanded.value,
                        offset = DpOffset((-40).dp, (-40).dp),
                        onDismissRequest = { expanded.value = false }) {
                        menuItems.forEach {
                            DropdownMenuItem(onClick = {
                                Toast.makeText(
                                    context,
                                    "You clicked $it menu",
                                    Toast.LENGTH_LONG
                                ).show()
                                expanded.value = false
                            }) {
                                Text(text = it)
                            }
                        }
                    }
                })
        },
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    OverFlowMenuDemo()
}