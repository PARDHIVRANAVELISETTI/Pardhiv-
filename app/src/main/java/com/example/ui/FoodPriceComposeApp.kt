package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FoodCompareViewModel
import com.example.ui.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPriceComposeApp(
    viewModel: FoodCompareViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    var showActiveCompareSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            val titleText = when (currentScreen) {
                is Screen.Home -> "Food Price Compare"
                is Screen.RestaurantDetail -> "Restaurant Menu"
            }
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = titleText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Fastfood,
                            contentDescription = "Food logo",
                            tint = SwiggyOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                navigationIcon = {
                    if (currentScreen !is Screen.Home) {
                        IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                actions = {
                    if (currentScreen is Screen.RestaurantDetail) {
                        val isFavorite by viewModel.isCurrentRestaurantFavorite.collectAsState()
                        IconButton(onClick = { viewModel.toggleCurrentFavorite() }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (isFavorite) AmberWarning else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.Home -> HomeScreen(
                    viewModel = viewModel,
                    onRestaurantClick = { viewModel.navigateTo(Screen.RestaurantDetail(it)) }
                )
                is Screen.RestaurantDetail -> RestaurantDetailScreen(
                    viewModel = viewModel,
                    restaurant = screen.restaurant,
                    onCompareClick = { showActiveCompareSheet = true }
                )
            }

            // Compare breakdown dialog (Acts as a full-bleed bottom sheet comparison card)
            if (showActiveCompareSheet) {
                CompareBreakdownSheet(
                    viewModel = viewModel,
                    onDismiss = { showActiveCompareSheet = false }
                )
            }
        }
    }
}

// ==========================================
// HOME SCREEN
// ==========================================
@Composable
fun HomeScreen(
    viewModel: FoodCompareViewModel,
    onRestaurantClick: (Restaurant) -> Unit
) {
    val restaurants = SampleData.restaurants
    val searchQuery by viewModel.searchQuery.collectAsState()
    val favorites by viewModel.favoriteRestaurants.collectAsState()
    val totalSavings by viewModel.totalLifetimeSavings.collectAsState()
    val savingsRecords by viewModel.savingsRecords.collectAsState()

    var activeCategoryFilter by remember { mutableStateOf("All") }
    var activeSortFilter by remember { mutableStateOf("Default") } // "Default", "Distance", "Swiggy Delivery", "Zomato Delivery"
    var showRecordsDialog by remember { mutableStateOf(false) }

    val filteredAndSortedRestaurants = remember(searchQuery, activeCategoryFilter, activeSortFilter) {
        val filtered = restaurants.filter { rest ->
            // Search filter
            val matchesSearch = rest.name.contains(searchQuery, ignoreCase = true) || 
                                rest.cuisine.contains(searchQuery, ignoreCase = true)
            
            // Category filter
            val matchesCategory = if (activeCategoryFilter == "All") true else {
                val menu = SampleData.menuItems[rest.id] ?: emptyList()
                menu.any { it.category.contains(activeCategoryFilter, ignoreCase = true) }
            }

            matchesSearch && matchesCategory
        }
        
        when (activeSortFilter) {
            "Distance" -> filtered.sortedBy { it.distanceKm }
            "Swiggy Delivery" -> filtered.sortedByDescending { it.swiggyDeliveryRating }
            "Zomato Delivery" -> filtered.sortedByDescending { it.zomatoDeliveryRating }
            else -> filtered
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- GAMIFIED LIFETIME SAVINGS COMPONENT ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.5.dp, PrimaryMint.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LIFETIME SAVINGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryMint,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "₹${"%.2f".format(totalSavings ?: 0.0)}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Saved",
                                fontSize = 14.sp,
                                color = SavingsGreen,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Text(
                            text = "Compare meals before checkout to see this grow!",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 14.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SavingsGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Savings growth",
                                tint = SavingsGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showRecordsDialog = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Savings History",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryMint
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowRight,
                                contentDescription = "View History",
                                tint = PrimaryMint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- SIMULATION SETTINGS SECTION (SURGES & MEMBERSHIPS) ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Delivery Surges & Subscriptions Simulator",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Swiggy One Switch
                    val swiggyOneActive by viewModel.isSwiggyOne.collectAsState()
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.isSwiggyOne.value = !swiggyOneActive },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (swiggyOneActive) SwiggyOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (swiggyOneActive) SwiggyOrange else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = swiggyOneActive,
                                onCheckedChange = { viewModel.isSwiggyOne.value = it },
                                colors = CheckboxDefaults.colors(checkedColor = SwiggyOrange)
                            )
                            Column {
                                Text("Swiggy One", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Free Deliveries", fontSize = 9.sp, color = TextSecondary)
                            }
                        }
                    }

                    // Zomato Gold Switch
                    val zomatoGoldActive by viewModel.isZomatoGold.collectAsState()
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.isZomatoGold.value = !zomatoGoldActive },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (zomatoGoldActive) ZomatoRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (zomatoGoldActive) ZomatoRed else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = zomatoGoldActive,
                                onCheckedChange = { viewModel.isZomatoGold.value = it },
                                colors = CheckboxDefaults.colors(checkedColor = ZomatoRed)
                            )
                            Column {
                                Text("Zomato Gold", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Free Deliveries", fontSize = 9.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Surge simulations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rainSurgeActive by viewModel.isRainSurge.collectAsState()
                    val festivalSurgeActive by viewModel.isFestivalSurge.collectAsState()

                    FilterChip(
                        selected = rainSurgeActive,
                        onClick = { viewModel.isRainSurge.value = !rainSurgeActive },
                        label = { Text("🌧️ Monsoon Surge (+₹25)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberWarning.copy(alpha = 0.2f),
                            selectedLabelColor = AmberWarning
                        )
                    )

                    FilterChip(
                        selected = festivalSurgeActive,
                        onClick = { viewModel.isFestivalSurge.value = !festivalSurgeActive },
                        label = { Text("💥 Peak Hours (+₹15)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ZomatoRed.copy(alpha = 0.2f),
                            selectedLabelColor = ZomatoRed
                        )
                    )
                }
            }
        }

        // --- SEARCH BAR COMPONENT ---
        item {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search dishes, chains, cuisines...", color = TextSecondary) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon", tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_bar")
            )
        }

        // --- CUISINE/CATEGORY QUICK HORIZONTAL SELECTOR ---
        item {
            val categories = listOf("All", "Pizza", "Biryani", "Burger", "Sweets", "Subs", "Sides")
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isActive = activeCategoryFilter == category
                    FilterChip(
                        selected = isActive,
                        onClick = { activeCategoryFilter = category },
                        label = { Text(category, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryMint,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }

        // --- SORT SELECTOR COMPONENT ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort Icon",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sort by Distance & Delivery:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val sortOptions = listOf(
                        "Default" to "🔥 Recommended",
                        "Distance" to "📍 Nearest First",
                        "Swiggy Delivery" to "🍊 Swiggy Riders (High to Low)",
                        "Zomato Delivery" to "🍎 Zomato Riders (High to Low)"
                    )
                    
                    items(sortOptions) { (key, label) ->
                        val isSelected = activeSortFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { activeSortFilter = key },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryMint.copy(alpha = 0.15f),
                                selectedLabelColor = PrimaryMint,
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
        }

        // --- FAVORITE RESTAURANTS SEGMENT (IF ANY) ---
        if (favorites.isNotEmpty() && searchQuery.isEmpty() && activeCategoryFilter == "All") {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "⭐ Your Saved Favorites",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(favorites) { fav ->
                            val fullRest = restaurants.find { it.id == fav.restaurantId }
                            if (fullRest != null) {
                                Card(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .clickable { onRestaurantClick(fullRest) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(fullRest.logoUrl, fontSize = 28.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = fullRest.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${fullRest.distanceKm} km",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- GENERAL RESTAURANT DIRECTORY ---
        item {
            Text(
                text = if (activeCategoryFilter == "All") "Popular Restaurants Near You" else "Restaurants with $activeCategoryFilter",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (filteredAndSortedRestaurants.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🍜", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No restaurants found matching filter rules.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredAndSortedRestaurants) { restaurant ->
                val isFav = favorites.any { it.restaurantId == restaurant.id }
                RestaurantListCard(
                    viewModel = viewModel,
                    restaurant = restaurant,
                    isFavorite = isFav,
                    onFavoriteToggle = { viewModel.toggleFavorite(restaurant.id, restaurant.name) },
                    onCardClick = { onRestaurantClick(restaurant) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Savings history Dialog
    if (showRecordsDialog) {
        Dialog(
            onDismissRequest = { showRecordsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.7f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📜 Direct Savings History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        IconButton(onClick = { showRecordsDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close history")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (savingsRecords.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("💸", fontSize = 54.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Zero simulated orders recorded yet.\nGo to a restaurant, add dishes, compare, and click 'Record Savings'!",
                                style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.clearSavedSavings() },
                            colors = ButtonDefaults.buttonColors(containerColor = ZomatoRed),
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Clear History", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(savingsRecords) { record ->
                                val dateStr = remember(record.timestamp) {
                                    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(record.timestamp))
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = record.restaurantName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Saved on ${record.cheaperPlatform} • $dateStr",
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Text(
                                            text = "+₹${"%.2f".format(record.savedAmount)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = SavingsGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestaurantListCard(
    viewModel: FoodCompareViewModel,
    restaurant: Restaurant,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onCardClick: () -> Unit
) {
    val swiggyOne by viewModel.isSwiggyOne.collectAsState()
    val zomatoGold by viewModel.isZomatoGold.collectAsState()
    val rainSurge by viewModel.isRainSurge.collectAsState()
    val festivalSurge by viewModel.isFestivalSurge.collectAsState()

    val typicalSavings = remember(restaurant, swiggyOne, zomatoGold, rainSurge, festivalSurge) {
        viewModel.calculateTypicalSavings(restaurant)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("restaurant_card_${restaurant.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simulated Logo Emoji
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(text = restaurant.logoUrl, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = restaurant.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (restaurant.isPopular) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(PrimaryMint.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Popular", color = PrimaryMint, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = restaurant.cuisine,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Star, contentDescription = "rating", tint = AmberWarning, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = restaurant.rating.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Text("•", fontSize = 11.sp, color = TextSecondary)
                    Text(text = restaurant.deliveryTime, fontSize = 11.sp, color = TextSecondary)
                    Text("•", fontSize = 11.sp, color = TextSecondary)
                    Text(text = "${restaurant.distanceKm} km", fontSize = 11.sp, color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🚴 Riders:", fontSize = 10.sp, color = TextSecondary)
                    
                    Box(
                        modifier = Modifier
                            .background(SwiggyOrange.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Swiggy", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SwiggyOrange)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = SwiggyOrange, modifier = Modifier.size(8.dp))
                            Text(text = restaurant.swiggyDeliveryRating.toString(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SwiggyOrange)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(ZomatoRed.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Zomato", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = ZomatoRed)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = ZomatoRed, modifier = Modifier.size(8.dp))
                            Text(text = restaurant.zomatoDeliveryRating.toString(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = ZomatoRed)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Best Deal Badging highlight
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (typicalSavings.cheaperPlatform == "Swiggy") SwiggyOrange.copy(alpha = 0.08f) else ZomatoRed.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    val icon = if (typicalSavings.cheaperPlatform == "Swiggy") "🍊" else "🍎"
                    val highlightColor = if (typicalSavings.cheaperPlatform == "Swiggy") SwiggyOrange else ZomatoRed
                    val couponCode = if (typicalSavings.cheaperPlatform == "Swiggy") typicalSavings.swiggyCouponCode else typicalSavings.zomatoCouponCode
                    
                    Text(
                        text = "$icon Save ~₹${"%.0f".format(typicalSavings.savingsAmount)} on ${typicalSavings.cheaperPlatform}! (with $couponCode)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = highlightColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Favorite + CTA Column
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = { onFavoriteToggle() }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite Toggle",
                        tint = if (isFavorite) AmberWarning else TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(PrimaryMint, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Menu", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


// ==========================================
// RESTAURANT DETAILS & MENU BUILDER SCREEN
// ==========================================
@Composable
fun RestaurantDetailScreen(
    viewModel: FoodCompareViewModel,
    restaurant: Restaurant,
    onCompareClick: () -> Unit
) {
    val cart by viewModel.cart.collectAsState()
    val menuItems = SampleData.menuItems[restaurant.id] ?: emptyList()
    val distanceKm by viewModel.customDistanceKm.collectAsState()

    // Group items by category
    val groupedMenu = menuItems.groupBy { it.category }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Dynamic Distance Slider simulation (allows user to preview distance effects in real-time!)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Simulated Distance to Kitchen",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${"%.1f".format(distanceKm)} km",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryMint
                    )
                }
                Text(
                    "Affects delivery fares on both systems dynamically",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
                Slider(
                    value = distanceKm.toFloat(),
                    onValueChange = { viewModel.updateDistance(it.toDouble()) },
                    valueRange = 0.5f..15f,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryMint,
                        activeTrackColor = PrimaryMint
                    )
                )
            }
        }

        // Menu list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 🎟️ Best Coupons Banner / Promotion Row
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalActivity,
                                contentDescription = "Coupons",
                                tint = PrimaryMint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Coupons & Best Deals Preview",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Swiggy best deal
                            val bestSwiggy = remember { viewModel.findBestCouponForSubtotal("Swiggy", 400.0) }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(SwiggyOrange.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .border(1.dp, SwiggyOrange.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .background(SwiggyOrange, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(bestSwiggy?.code ?: "WELCOME50", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Swiggy Deal", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SwiggyOrange)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = bestSwiggy?.description ?: "50% off. Min order ₹149.",
                                        fontSize = 9.sp,
                                        color = TextSecondary,
                                        lineHeight = 11.sp
                                    )
                                }
                            }
                            
                            // Zomato best deal
                            val bestZomato = remember { viewModel.findBestCouponForSubtotal("Zomato", 400.0) }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(ZomatoRed.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .border(1.dp, ZomatoRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .background(ZomatoRed, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(bestZomato?.code ?: "CRAVINGS", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Zomato Deal", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ZomatoRed)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = bestZomato?.description ?: "50% off. Min order ₹159.",
                                        fontSize = 9.sp,
                                        color = TextSecondary,
                                        lineHeight = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            groupedMenu.forEach { (category, items) ->
                item {
                    Text(
                        text = category,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                items(items) { item ->
                    val quantity = cart[item.id] ?: 0
                    MenuItemRow(
                        item = item,
                        quantity = quantity,
                        onAdd = { viewModel.addToCart(item) },
                        onRemove = { viewModel.removeFromCart(item) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp)) // Extra Padding for persistent sticky compare bar
            }
        }

        // --- STICKY BOTTOM COMPARE BAR ---
        val cartItems = viewModel.getCartItems()
        val cartSubtotal = viewModel.getCartSubtotal()

        if (cartItems.isNotEmpty()) {
            val swiggyOneActive by viewModel.isSwiggyOne.collectAsState()
            val zomatoGoldActive by viewModel.isZomatoGold.collectAsState()
            val rainSurgeActive by viewModel.isRainSurge.collectAsState()
            val festivalSurgeActive by viewModel.isFestivalSurge.collectAsState()
            val selectedSwiggyCoupon by viewModel.selectedSwiggyCoupon.collectAsState()
            val selectedZomatoCoupon by viewModel.selectedZomatoCoupon.collectAsState()

            val comparison = remember(cartItems, distanceKm, swiggyOneActive, zomatoGoldActive, rainSurgeActive, festivalSurgeActive, selectedSwiggyCoupon, selectedZomatoCoupon) {
                viewModel.calculateComparison()
            }
            val swiggyTotal = comparison.swiggyBill.totalBill
            val zomatoTotal = comparison.zomatoBill.totalBill

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    // Savings summary live box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SavingsGreen.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .border(1.dp, SavingsGreen, RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💸", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (comparison.cheaperPlatform == "Equal") {
                                        "Pricing is matching on both platforms!"
                                    } else {
                                        "${comparison.cheaperPlatform} saves you ₹${"%.2f".format(comparison.savingsAmount)}!"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SavingsGreen
                                )
                                Text(
                                    text = "Best coupons are automatically matched and applied.",
                                    fontSize = 8.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(SavingsGreen, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "SAVE ₹${"%.0f".format(comparison.savingsAmount)}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${cartItems.sumOf { it.quantity }} items selected",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column {
                                    Text("Swiggy Total", fontSize = 9.sp, color = SwiggyOrange, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "₹${"%.2f".format(swiggyTotal)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (comparison.cheaperPlatform == "Swiggy") SavingsGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                VerticalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                Column {
                                    Text("Zomato Total", fontSize = 9.sp, color = ZomatoRed, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "₹${"%.2f".format(zomatoTotal)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (comparison.cheaperPlatform == "Zomato") SavingsGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { onCompareClick() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("compare_cart_cta")
                        ) {
                            Icon(imageVector = Icons.Default.CompareArrows, contentDescription = "compare", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Check Bill Details",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuItemRow(
    item: MenuItem,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Food emoji badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.iconEmoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (item.isVeg) Color(0xFF2E7D32) else Color(0xFFC62828))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${"%.2f".format(item.price)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Interactive Selector Controls (at least 48dp target)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                if (quantity > 0) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.background, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "minus", tint = PrimaryMint, modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = quantity.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.width(16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                IconButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .size(32.dp)
                        .background(PrimaryMint, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "add", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}


// ==========================================
// SIDE-BY-SIDE PLATFORM COMPARISON SHEET
// ==========================================
@Composable
fun CompareBreakdownSheet(
    viewModel: FoodCompareViewModel,
    onDismiss: () -> Unit
) {
    val comparison = viewModel.calculateComparison()
    val swiggyBill = comparison.swiggyBill
    val zomatoBill = comparison.zomatoBill

    val selectedSwiggyCoupon by viewModel.selectedSwiggyCoupon.collectAsState()
    val selectedZomatoCoupon by viewModel.selectedZomatoCoupon.collectAsState()

    val swiggyBestSuggested = viewModel.findBestCouponForPlatform("Swiggy")
    val zomatoBestSuggested = viewModel.findBestCouponForPlatform("Zomato")

    val aiInsight by viewModel.aiInsight.collectAsState()
    val isInsightLoading by viewModel.isInsightLoading.collectAsState()

    var showSwiggyDropdown by remember { mutableStateOf(false) }
    var showZomatoDropdown by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Swipe line & titles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.QueryStats, contentDescription = "compare header icon", tint = PrimaryMint)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Platform Price Match",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "dismiss compare")
                    }
                }

                // Divider line
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Main body containing comparisons
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // --- THE WINNER HIGHLIGHT HEADER COALITION ---
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (comparison.cheaperPlatform == "Equal") {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    SavingsGreen.copy(alpha = 0.12f)
                                }
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (comparison.cheaperPlatform == "Equal") {
                                    MaterialTheme.colorScheme.outlineVariant
                                } else {
                                    SavingsGreen
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                when (comparison.cheaperPlatform) {
                                    "Swiggy" -> {
                                        Text(
                                            "🎉 Swiggy is Cheaper! Save ₹${"%.2f".format(comparison.savingsAmount)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = SavingsGreen
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Calculated including menu markup, promotions, and delivery fares.",
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                viewModel.recordSavingsOutcome("Swiggy", comparison.savingsAmount)
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SwiggyOrange),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Simulate Checkout & Claim Savings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    "Zomato" -> {
                                        Text(
                                            "🎉 Zomato is Cheaper! Save ₹${"%.2f".format(comparison.savingsAmount)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = SavingsGreen
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Calculated including menu markup, promotions, and delivery fares.",
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                viewModel.recordSavingsOutcome("Zomato", comparison.savingsAmount)
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ZomatoRed),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Simulate Checkout & Claim Savings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    else -> {
                                        Text(
                                            "🤝 Pricing is Identical!",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Prices are perfectly matched on Swiggy and Zomato.",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- SIDE-BY-SIDE DENSE COLUMNS ---
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // --- Swiggy Column ---
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        if (comparison.cheaperPlatform == "Swiggy") SwiggyOrange else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                // Column Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(SwiggyOrange)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Swiggy", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SwiggyOrange)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Bill Breakdown
                                BillBreakdownItem("Menu Subtotal (+3%)", swiggyBill.subtotal)
                                BillBreakdownItem("Packaging Fee", swiggyBill.packagingCharge)
                                BillBreakdownItem("Delivery Fare", swiggyBill.deliveryFee)
                                BillBreakdownItem("Platform Levy", swiggyBill.platformFee)
                                BillBreakdownItem("GST & Taxes (5%)", swiggyBill.gstAndTaxes)

                                if (swiggyBill.discountAmount > 0) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Coupon saved", fontSize = 10.sp, color = SavingsGreen, fontWeight = FontWeight.Bold)
                                        Text("-₹${"%.1f".format(swiggyBill.discountAmount)}", fontSize = 10.sp, color = SavingsGreen, fontWeight = FontWeight.Bold)
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                // Total Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("TOTAL BILL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "₹${"%.2f".format(swiggyBill.totalBill)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (comparison.cheaperPlatform == "Swiggy") SavingsGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // SWIGGY COUPON PICKER DROPDOWN BUTTON
                                Box {
                                    Button(
                                        onClick = { showSwiggyDropdown = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = SwiggyOrange),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = selectedSwiggyCoupon?.code ?: "Select Coupon",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown", modifier = Modifier.size(12.dp))
                                    }

                                    DropdownMenu(
                                        expanded = showSwiggyDropdown,
                                        onDismissRequest = { showSwiggyDropdown = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("None (Reset)", fontSize = 12.sp) },
                                            onClick = {
                                                viewModel.selectSwiggyCoupon(null)
                                                showSwiggyDropdown = false
                                            }
                                        )
                                        SampleData.swiggyCoupons.forEach { coupon ->
                                            val currentSubtotal = viewModel.getCartSubtotal() * 1.03
                                            val meetsMin = currentSubtotal >= coupon.minOrderValue
                                            DropdownMenuItem(
                                                text = { 
                                                    Column {
                                                        Text(
                                                            text = coupon.code, 
                                                            fontWeight = FontWeight.Bold, 
                                                            color = if (meetsMin) SwiggyOrange else TextSecondary,
                                                            fontSize = 12.sp
                                                        )
                                                        Text(
                                                            text = coupon.description, 
                                                            fontSize = 9.sp, 
                                                            color = TextSecondary,
                                                            lineHeight = 11.sp
                                                        )
                                                        if (!meetsMin) {
                                                            Text(
                                                                "Locks at ₹${coupon.minOrderValue} subtotal", 
                                                                fontSize = 8.sp, 
                                                                color = ZomatoRed,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    if (meetsMin) {
                                                        viewModel.selectSwiggyCoupon(coupon)
                                                    }
                                                    showSwiggyDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Quick Auto Recommendation tip
                                if (swiggyBestSuggested != null && swiggyBestSuggested.code != selectedSwiggyCoupon?.code) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "💡 Try '${swiggyBestSuggested.code}' for optimal savings!",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SwiggyOrange,
                                        lineHeight = 10.sp
                                    )
                                }
                            }

                            // --- Zomato Column ---
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        if (comparison.cheaperPlatform == "Zomato") ZomatoRed else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                // Column Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(ZomatoRed)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Zomato", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ZomatoRed)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Bill Breakdown
                                BillBreakdownItem("Menu Subtotal (+5%)", zomatoBill.subtotal)
                                BillBreakdownItem("Packaging Fee", zomatoBill.packagingCharge)
                                BillBreakdownItem("Delivery Fare", zomatoBill.deliveryFee)
                                BillBreakdownItem("Platform Levy", zomatoBill.platformFee)
                                BillBreakdownItem("GST & Taxes (5%)", zomatoBill.gstAndTaxes)

                                if (zomatoBill.discountAmount > 0) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Coupon saved", fontSize = 10.sp, color = SavingsGreen, fontWeight = FontWeight.Bold)
                                        Text("-₹${"%.1f".format(zomatoBill.discountAmount)}", fontSize = 10.sp, color = SavingsGreen, fontWeight = FontWeight.Bold)
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                // Total Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("TOTAL BILL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "₹${"%.2f".format(zomatoBill.totalBill)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (comparison.cheaperPlatform == "Zomato") SavingsGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // ZOMATO COUPON PICKER DROPDOWN BUTTON
                                Box {
                                    Button(
                                        onClick = { showZomatoDropdown = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = ZomatoRed),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = selectedZomatoCoupon?.code ?: "Select Coupon",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown", modifier = Modifier.size(12.dp))
                                    }

                                    DropdownMenu(
                                        expanded = showZomatoDropdown,
                                        onDismissRequest = { showZomatoDropdown = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("None (Reset)", fontSize = 12.sp) },
                                            onClick = {
                                                viewModel.selectZomatoCoupon(null)
                                                showZomatoDropdown = false
                                            }
                                        )
                                        SampleData.zomatoCoupons.forEach { coupon ->
                                            val currentSubtotal = viewModel.getCartSubtotal() * 1.05
                                            val meetsMin = currentSubtotal >= coupon.minOrderValue
                                            DropdownMenuItem(
                                                text = { 
                                                    Column {
                                                        Text(
                                                            text = coupon.code, 
                                                            fontWeight = FontWeight.Bold, 
                                                            color = if (meetsMin) ZomatoRed else TextSecondary,
                                                            fontSize = 12.sp
                                                        )
                                                        Text(
                                                            text = coupon.description, 
                                                            fontSize = 9.sp, 
                                                            color = TextSecondary,
                                                            lineHeight = 11.sp
                                                        )
                                                        if (!meetsMin) {
                                                            Text(
                                                                "Locks at ₹${coupon.minOrderValue} subtotal", 
                                                                fontSize = 8.sp, 
                                                                color = ZomatoRed,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    if (meetsMin) {
                                                        viewModel.selectZomatoCoupon(coupon)
                                                    }
                                                    showZomatoDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Quick Auto Recommendation tip
                                if (zomatoBestSuggested != null && zomatoBestSuggested.code != selectedZomatoCoupon?.code) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "💡 Try '${zomatoBestSuggested.code}' for optimal savings!",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ZomatoRed,
                                        lineHeight = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    // --- INTELLIGENT GEMINI AI PORTAL ---
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, PrimaryMint.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                Brush.linearGradient(listOf(SwiggyOrange, ZomatoRed)),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "AI Genie",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "BiteSaver AI Genie Helper",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Our model will analyze your current basket and coupon tier requirements to devise optimized food shopping hacks.",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    lineHeight = 13.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                if (isInsightLoading) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = PrimaryMint,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "BiteSaver AI is engineering discounts...",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = PrimaryMint
                                        )
                                    }
                                } else if (aiInsight.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = aiInsight,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { viewModel.generateAISavingsTips() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isInsightLoading,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⚡ Generate AI Optimization Insights", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BillBreakdownItem(
    label: String,
    amount: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 10.sp, color = TextSecondary)
        Text(
            text = "₹${"%.2f".format(amount)}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
