package com.securephoneapps.securebrowser.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ModeNight
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import com.securephoneapps.securebrowser.model.TabGroup
import com.securephoneapps.securebrowser.model.TabInstance
import com.securephoneapps.securebrowser.viewmodel.BrowserStateViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdvancedTabManagerScreen(
    viewModel: BrowserStateViewModel,
    onBack: () -> Unit
) {
    val tabs by viewModel.tabsState.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val tabGroups by viewModel.tabGroups.collectAsState()
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var selectedTabForGroup by remember { mutableStateOf<TabInstance?>(null) }
    var newGroupName by remember { mutableStateOf("") }
    var newGroupColorHex by remember { mutableStateOf("#30D158") } // Default secure green

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Engine Nests", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tab_manager_back_button")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Manager")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.createNewTab(isIncognito = true) },
                        modifier = Modifier.testTag("new_incognito_tab_button")
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = "New Incognito Tab", tint = Color(0xFF2563EB)) // Blue 600
                    }
                    IconButton(
                        onClick = { viewModel.createNewTab(isIncognito = false) },
                        modifier = Modifier.testTag("new_tab_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Standard Tab", tint = Color(0xFF059669)) // Emerald 600
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF0F172A), // Slate 900
                    navigationIconContentColor = Color(0xFF475569) // Slate 600
                ),
                modifier = Modifier.border(width = 1.dp, color = Color(0xFFE2E8F0))
            )
        },
        containerColor = Color(0xFFF8FAFC) // Slate 50 background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // -- Tab Group Matrix Section --
            if (tabGroups.isNotEmpty()) {
                Text(
                    text = "ACTIVE TAB STACKING GROUPS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF64748B), // Slate 500
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    tabGroups.forEach { group ->
                        val groupTabs = tabs.filter { it.parentGroupId == group.groupId }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    Color(android.graphics.Color.parseColor(group.hexColorBadge)),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(Color(android.graphics.Color.parseColor(group.hexColorBadge)))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = group.groupName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A), // Slate 900
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.width(70.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeGroup(group.groupId) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Group",
                                            tint = Color(0xFF94A3B8), // Slate 400
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${groupTabs.size} stacked tabs", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // -- Individual Tab Grid --
            Text(
                text = "ACTIVE TAB INSTANCES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF64748B), // Slate 500
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tabs, key = { it.tabId }) { tab ->
                    val isActive = tab.tabId == activeTabId
                    val associatedGroup = tabGroups.find { it.groupId == tab.parentGroupId }
                    val isGrouped = associatedGroup != null

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.closeTab(tab.tabId)
                                hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> Color(0xFFEF4444)
                                SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF4444)
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Tab",
                                    tint = Color.White
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TabCard(
                            tab = tab,
                            isActive = isActive,
                            isGrouped = isGrouped,
                            groupColor = associatedGroup?.hexColorBadge ?: "#000000",
                            onClick = { viewModel.selectTab(tab.tabId) },
                            onClose = { viewModel.closeTab(tab.tabId) },
                            onLongClick = {
                                selectedTabForGroup = tab
                                showCreateGroupDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // -- Dialog to Stacking Groups --
    if (showCreateGroupDialog && selectedTabForGroup != null) {
        val groupsAvailable = tabGroups

        AlertDialog(
            onDismissRequest = {
                showCreateGroupDialog = false
                selectedTabForGroup = null
            },
            title = { Text("Vivaldi Stacking Tab Group", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = {
                Column {
                    Text(
                        "Nest this tab inside an active cluster or instantiate a completely new visual color-coded tab group.",
                        color = Color(0xFF64748B), // Slate 500
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (groupsAvailable.isNotEmpty()) {
                        Text("Stack inside Existing Group:", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        groupsAvailable.forEach { grp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9)) // Slate 100
                                    .combinedClickable(
                                        onClick = {
                                            viewModel.assignTabToGroup(selectedTabForGroup!!.tabId, grp.groupId)
                                            showCreateGroupDialog = false
                                            selectedTabForGroup = null
                                        }
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(android.graphics.Color.parseColor(grp.hexColorBadge)))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(grp.groupName, color = Color(0xFF0F172A), fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (selectedTabForGroup?.parentGroupId != null) {
                        Button(
                            onClick = {
                                viewModel.assignTabToGroup(selectedTabForGroup!!.tabId, null)
                                showCreateGroupDialog = false
                                selectedTabForGroup = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Remove from Current Stack", color = Color(0xFF475569))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text("Or Create a New Tab Stack:", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        placeholder = { Text("Stack Name (e.g. Work)", color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF475569),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Stack Style Badge:", color = Color(0xFF475569), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val colorOptions = listOf("#30D158", "#FF453A", "#0A84FF", "#BF5AF2", "#FF9F0A")
                        colorOptions.forEach { col ->
                            val isChosen = newGroupColorHex == col
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(android.graphics.Color.parseColor(col)))
                                    .combinedClickable(onClick = { newGroupColorHex = col })
                                    .border(
                                        width = if (isChosen) 2.dp else 0.dp,
                                        color = if (isChosen) Color(0xFF2563EB) else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            val gid = viewModel.createTabGroup(newGroupName.trim(), newGroupColorHex)
                            viewModel.assignTabToGroup(selectedTabForGroup!!.tabId, gid)
                            newGroupName = ""
                            showCreateGroupDialog = false
                            selectedTabForGroup = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Assemble Nest", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateGroupDialog = false
                    selectedTabForGroup = null
                }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabCard(
    tab: TabInstance,
    isActive: Boolean,
    isGrouped: Boolean,
    groupColor: String,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onLongClick: () -> Unit
) {
    val borderCol = if (isActive) {
        Color(0xFF2563EB) // Blue 600
    } else if (isGrouped) {
        Color(android.graphics.Color.parseColor(groupColor))
    } else {
        Color(0xFFE2E8F0) // Slate 200
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .height(140.dp)
            .border(2.dp, borderCol, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header of the Tab Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        if (tab.isIncognito) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = "Incognito",
                                tint = Color(0xFF2563EB), // Blue 600
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        } else if (isGrouped) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = "Group",
                                tint = Color(android.graphics.Color.parseColor(groupColor)),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = tab.pageTitle,
                            color = Color(0xFF0F172A), // Slate 900
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Tab",
                            tint = Color(0xFF94A3B8), // Slate 400
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // URL text preview
                Text(
                    text = tab.currentUrl,
                    color = Color(0xFF64748B), // Slate 500
                    fontSize = 11.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp,
                    modifier = Modifier.weight(1f)
                )

                // Status indication footer
                if (isActive) {
                    Text(
                        "Active Process",
                        color = Color(0xFF059669), // Emerald 600
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (tab.isSuspendedState) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ModeNight,
                            contentDescription = "Sleeping",
                            tint = Color(0xFF2563EB), // Blue 600
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Hibernated (RAM Free)",
                            color = Color(0xFF2563EB), // Blue 600
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Sleep Overlay for Chrome Efficiency Protocol Visuals
            if (tab.isSuspendedState) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9).copy(alpha = 0.85f)), // Light slate overlay
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Hibernated",
                            tint = Color(0xFF475569), // Slate 600
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "TAP TO WAKE TAB",
                            fontSize = 9.sp,
                            color = Color(0xFF475569), // Slate 600
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
