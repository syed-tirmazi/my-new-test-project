package com.example.chatter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chatter.notifications.NotificationHelper
import com.example.chatter.ui.screens.ChatScreen
import com.example.chatter.ui.screens.ProfileSetupScreen
import com.example.chatter.ui.screens.SearchScreen
import com.example.chatter.ui.theme.ChatterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)

        setContent {
            ChatterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatterNavHost(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ChatterNavHost(viewModel: ChatterViewModel, navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = "profile"
    ) {
        composable("profile") {
            ProfileSetupScreen(
                state = viewModel.profileState.value,
                onSubmit = { username, displayName ->
                    viewModel.updateProfile(username, displayName)
                    navController.navigate("search") {
                        popUpTo("profile") { inclusive = true }
                    }
                },
                onProfileReady = {
                    navController.navigate("search") {
                        popUpTo("profile") { inclusive = true }
                    }
                }
            )
        }

        composable("search") {
            SearchScreen(
                profileState = viewModel.profileState.value,
                searchState = viewModel.searchState.value,
                onSearchChange = viewModel::updateSearch,
                onSelectUser = { target ->
                    viewModel.setActiveChat(target.uid)
                    navController.navigate("chat/${target.uid}")
                }
            )
        }

        composable("chat/{peerId}") { backStackEntry ->
            val peerId = backStackEntry.arguments?.getString("peerId") ?: return@composable
            val peerProfile = viewModel.activeUserProfile.value
            ChatScreen(
                currentUser = viewModel.profileState.value.profile,
                peerProfile = peerProfile,
                chatState = viewModel.chatState.value,
                onBack = { navController.popBackStack() },
                onSend = viewModel::sendMessage
            )
            viewModel.observePeer(peerId)
        }
    }
}
