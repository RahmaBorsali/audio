package com.example.audio.ui.navigation

sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "Home")
    object Search : Screen("search", "Search")
    object Library : Screen("library", "Library")
    object LocalMusic : Screen("local", "Ma Musique")
}
