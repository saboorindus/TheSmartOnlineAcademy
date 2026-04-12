package com.echologics.thesmartonlineacademy.ui.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun TeacherBottomNav(
    navController: NavHostController,
    currentRoute: String,
    content: @Composable (Modifier) -> Unit
) {
    val items = listOf(
        BottomNavItem("Bookings", Icons.Default.Home, "teacher_home"),
        BottomNavItem("Messages", Icons.AutoMirrored.Filled.Chat, "teacher_messages")
    )
    AppBottomNavScaffold(navController, currentRoute, items, content)
}

@Composable
fun StudentBottomNav(
    navController: NavHostController,
    currentRoute: String,
    content: @Composable (Modifier) -> Unit
) {
    val items = listOf(
        BottomNavItem("Discover", Icons.Default.Search, "student_home"),
        BottomNavItem("Messages", Icons.AutoMirrored.Filled.Chat, "student_messages")
    )
    AppBottomNavScaffold(navController, currentRoute, items, content)
}

@Composable
private fun AppBottomNavScaffold(
    navController: NavHostController,
    currentRoute: String,
    items: List<BottomNavItem>,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(item.route) {
                                    popUpTo(items.first().route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(item.icon, contentDescription = item.label)
                        },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Purple,
                            selectedTextColor = Purple,
                            indicatorColor = Purple.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        content(Modifier.padding(padding))
    }
}