package com.example.chatter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chatter.ProfileState
import com.example.chatter.SearchState
import com.example.chatter.model.UserProfile

@Composable
fun SearchScreen(
    profileState: ProfileState,
    searchState: SearchState,
    onSearchChange: (String) -> Unit,
    onSelectUser: (UserProfile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Welcome ${profileState.profile?.displayName ?: ""}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Search for a friend by username",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(modifier = Modifier.padding(4.dp))
        OutlinedTextField(
            value = searchState.query,
            onValueChange = onSearchChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(searchState.results) { user ->
                UserRow(user = user, onSelectUser = onSelectUser)
            }
        }
    }
}

@Composable
private fun UserRow(user: UserProfile, onSelectUser: (UserProfile) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelectUser(user) }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = user.displayName, fontWeight = FontWeight.Bold)
            Text(text = "@${user.username}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
