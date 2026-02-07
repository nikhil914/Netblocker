package com.example.netblockerpro.ui.screen.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.netblockerpro.ui.screen.home.SortOption

@Composable
fun SortFilterBar(
    currentSort: SortOption,
    hideSystemApps: Boolean,
    onSortChange: (SortOption) -> Unit,
    onHideSystemChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort chips
        FilterChip(
            selected = currentSort == SortOption.NAME_ASC,
            onClick = { onSortChange(SortOption.NAME_ASC) },
            label = { Text("A-Z") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.SortByAlpha,
                    contentDescription = null,
                    modifier = Modifier
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        
        FilterChip(
            selected = currentSort == SortOption.BLOCKED_FIRST,
            onClick = { onSortChange(SortOption.BLOCKED_FIRST) },
            label = { Text("Blocked First") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        
        FilterChip(
            selected = currentSort == SortOption.SYSTEM_LAST,
            onClick = { onSortChange(SortOption.SYSTEM_LAST) },
            label = { Text("System Last") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        
        // Filter chip
        FilterChip(
            selected = hideSystemApps,
            onClick = { onHideSystemChange(!hideSystemApps) },
            label = { Text("Hide System") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )
    }
}
