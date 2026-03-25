package com.snozzz.link.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.snozzz.link.ui.navigation.LinkNavHost
import com.snozzz.link.ui.theme.LinkTheme

@Composable
fun LinkApp() {
    LinkTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val navController = rememberNavController()
            LinkNavHost(navController = navController)
        }
    }
}
