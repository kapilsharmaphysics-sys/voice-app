package com.app.voiceapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.voiceapp.presentation.feed.FeedScreen
import com.app.voiceapp.presentation.record.RecordScreen

private const val ROUTE_FEED = "feed"
private const val ROUTE_RECORD = "record"

/**
 * Two-screen nav graph: feed is the start destination, record is pushed on top when the FAB is tapped.
 * Uses plain string routes — a sealed class would be overkill for two screens.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_FEED) {
        composable(ROUTE_FEED) {
            FeedScreen(onRecordTapped = { navController.navigate(ROUTE_RECORD) })
        }
        composable(ROUTE_RECORD) {
            RecordScreen(onBack = { navController.popBackStack() })
        }
    }
}
