package com.snozzz.link.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    data object Home : Destination(
        route = "home",
        title = "Today",
        icon = Icons.Rounded.FavoriteBorder,
    )

    data object Activity : Destination(
        route = "activity",
        title = "Moments",
        icon = Icons.Rounded.QueryStats,
    )

    data object Chat : Destination(
        route = "chat",
        title = "Messages",
        icon = Icons.Rounded.ChatBubbleOutline,
    )
}

val bottomDestinations = listOf(
    Destination.Home,
    Destination.Activity,
    Destination.Chat,
)
