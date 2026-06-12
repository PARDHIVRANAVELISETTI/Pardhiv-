package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CartItem
import com.example.data.ComparisonResult
import com.example.data.Coupon
import com.example.data.MenuItem
import com.example.data.PlatformBill
import com.example.data.Restaurant
import com.example.data.SampleData
import com.example.data.db.AppDatabase
import com.example.data.db.FoodCompareRepository
import com.example.network.GeminiHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    data class RestaurantDetail(val restaurant: Restaurant) : Screen()
}

class FoodCompareViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FoodCompareRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FoodCompareRepository(database.dao())
    }

    // Database flows
    val favoriteRestaurants = repository.favoriteRestaurants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsRecords = repository.savingsRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalLifetimeSavings = repository.totalLifetimeSavings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // UI Navigation State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Memberships & Surges State
    val isSwiggyOne = MutableStateFlow(false)
    val isZomatoGold = MutableStateFlow(false)
    val isRainSurge = MutableStateFlow(false)
    val isFestivalSurge = MutableStateFlow(false)

    // Current Active Cart (Menu Item ID -> Quantity)
    private val _cart = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val cart: StateFlow<Map<Int, Int>> = _cart.asStateFlow()

    // Current Selected Restaurant
    private val _selectedRestaurant = MutableStateFlow<Restaurant?>(null)
    val selectedRestaurant: StateFlow<Restaurant?> = _selectedRestaurant.asStateFlow()

    // Custom Delivery Distance Slider override (defaults to restaurant base distance)
    private val _customDistanceKm = MutableStateFlow(3.0)
    val customDistanceKm: StateFlow<Double> = _customDistanceKm.asStateFlow()

    // Currently Selected Custom Coupons
    private val _selectedSwiggyCoupon = MutableStateFlow<Coupon?>(null)
    val selectedSwiggyCoupon: StateFlow<Coupon?> = _selectedSwiggyCoupon.asStateFlow()

    private val _selectedZomatoCoupon = MutableStateFlow<Coupon?>(null)
    val selectedZomatoCoupon: StateFlow<Coupon?> = _selectedZomatoCoupon.asStateFlow()

    // AI Insight state
    private val _aiInsight = MutableStateFlow<String>("")
    val aiInsight: StateFlow<String> = _aiInsight.asStateFlow()

    private val _isInsightLoading = MutableStateFlow(false)
    val isInsightLoading: StateFlow<Boolean> = _isInsightLoading.asStateFlow()

    // Favorite status of current selected restaurant
    private val _isCurrentRestaurantFavorite = MutableStateFlow(false)
    val isCurrentRestaurantFavorite: StateFlow<Boolean> = _isCurrentRestaurantFavorite.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.RestaurantDetail) {
            _selectedRestaurant.value = screen.restaurant
            _customDistanceKm.value = screen.restaurant.distanceKm
            _cart.value = emptyMap() // Clear cart for new restaurant
            _selectedSwiggyCoupon.value = null
            _selectedZomatoCoupon.value = null
            _aiInsight.value = ""
            checkFavoriteStatus(screen.restaurant.id)
            autoApplyBestCoupons()
        } else {
            _selectedRestaurant.value = null
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun checkFavoriteStatus(restaurantId: Int) {
        viewModelScope.launch {
            _isCurrentRestaurantFavorite.value = repository.isFavorite(restaurantId)
        }
    }

    fun toggleCurrentFavorite() {
        val restaurant = _selectedRestaurant.value ?: return
        viewModelScope.launch {
            repository.toggleFavorite(restaurant.id, restaurant.name)
            checkFavoriteStatus(restaurant.id)
        }
    }

    fun toggleFavorite(id: Int, name: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id, name)
        }
    }

    fun updateDistance(distance: Double) {
        _customDistanceKm.value = distance
        autoApplyBestCoupons()
    }

    // Add to cart operations
    fun addToCart(item: MenuItem) {
        val currentMap = _cart.value.toMutableMap()
        currentMap[item.id] = (currentMap[item.id] ?: 0) + 1
        _cart.value = currentMap
        autoApplyBestCoupons()
    }

    fun removeFromCart(item: MenuItem) {
        val currentMap = _cart.value.toMutableMap()
        val currentQty = currentMap[item.id] ?: 0
        if (currentQty > 1) {
            currentMap[item.id] = currentQty - 1
        } else {
            currentMap.remove(item.id)
        }
        _cart.value = currentMap
        autoApplyBestCoupons()
    }

    fun getCartItems(): List<CartItem> {
        val restaurantId = _selectedRestaurant.value?.id ?: return emptyList()
        val itemsList = SampleData.menuItems[restaurantId] ?: return emptyList()
        return _cart.value.mapNotNull { entry ->
            val menuItem = itemsList.find { it.id == entry.key }
            if (menuItem != null) CartItem(menuItem, entry.value) else null
        }
    }

    fun getCartSubtotal(): Double {
        return getCartItems().sumOf { it.item.price * it.quantity }
    }

    fun selectSwiggyCoupon(coupon: Coupon?) {
        _selectedSwiggyCoupon.value = coupon
        _aiInsight.value = "" // clear outdated insights
    }

    fun selectZomatoCoupon(coupon: Coupon?) {
        _selectedZomatoCoupon.value = coupon
        _aiInsight.value = "" // clear outdated insights
    }

    fun autoApplyBestCoupons() {
        _aiInsight.value = ""
        val subtotal = getCartSubtotal()
        if (subtotal > 0.0) {
            _selectedSwiggyCoupon.value = findBestCouponForPlatform("Swiggy")
            _selectedZomatoCoupon.value = findBestCouponForPlatform("Zomato")
        } else {
            _selectedSwiggyCoupon.value = null
            _selectedZomatoCoupon.value = null
        }
    }

    // Performs live calculation of complete bill comparison
    fun calculateComparison(): ComparisonResult {
        val subtotal = getCartSubtotal()
        val distance = _customDistanceKm.value
        val rain = isRainSurge.value
        val festival = isFestivalSurge.value

        val swiggyBill = calculateBillForPlatform(
            "Swiggy",
            subtotal,
            distance,
            isSwiggyOne.value,
            _selectedSwiggyCoupon.value,
            rain,
            festival
        )
        val zomatoBill = calculateBillForPlatform(
            "Zomato",
            subtotal,
            distance,
            isZomatoGold.value,
            _selectedZomatoCoupon.value,
            rain,
            festival
        )

        val cheaperPlatform = when {
            subtotal == 0.0 -> "Equal"
            swiggyBill.totalBill < zomatoBill.totalBill -> "Swiggy"
            zomatoBill.totalBill < swiggyBill.totalBill -> "Zomato"
            else -> "Equal"
        }

        val savings = if (subtotal > 0.0) {
            Math.abs(swiggyBill.totalBill - zomatoBill.totalBill)
        } else 0.0

        return ComparisonResult(swiggyBill, zomatoBill, cheaperPlatform, savings)
    }

    private fun calculateBillForPlatform(
        platformName: String,
        subtotal: Double,
        distanceKm: Double,
        isMember: Boolean,
        selectedCoupon: Coupon?,
        isRainSurge: Boolean,
        isFestivalSurge: Boolean
    ): PlatformBill {
        if (subtotal == 0.0) {
            return PlatformBill(
                platformName = platformName,
                subtotal = 0.0,
                packagingCharge = 0.0,
                deliveryFee = 0.0,
                platformFee = 0.0,
                gstAndTaxes = 0.0,
                selectedCoupon = null,
                discountAmount = 0.0,
                totalBill = 0.0
            )
        }

        // Realistic markups: Zomato has 5% markup, Swiggy has 3% markup on base restaurants
        val markup = if (platformName == "Swiggy") 0.03 else 0.05
        val platformSubtotal = subtotal * (1.0 + markup)

        // Flat Packaging charge: Swiggy ₹20, Zomato ₹15
        val packagingCharge = if (platformName == "Swiggy") 20.0 else 15.0

        // Delivery calculation: Base ₹35 up to 3km, ₹9 per extra km
        val baseDel = 35.0
        val extraDistCharge = if (distanceKm > 3.0) (distanceKm - 3.0) * 9.0 else 0.0
        var deliveryFee = baseDel + extraDistCharge

        if (isRainSurge) deliveryFee += 25.0
        if (isFestivalSurge) deliveryFee += 15.0

        // Membership discount applied up to 10km free delivery limit
        if (isMember && distanceKm <= 10.0) {
            deliveryFee = 0.0
        }

        // Platform fee: Swiggy charges ₹6, Zomato charges ₹5
        var platformFee = if (platformName == "Swiggy") 6.0 else 5.0
        if (isFestivalSurge) platformFee += 2.0

        // Discount calculation
        var discount = 0.0
        if (selectedCoupon != null && platformSubtotal >= selectedCoupon.minOrderValue) {
            if (selectedCoupon.flatDiscount > 0.0) {
                discount = selectedCoupon.flatDiscount
            } else if (selectedCoupon.discountPercent > 0.0) {
                val calculated = platformSubtotal * (selectedCoupon.discountPercent / 100.0)
                discount = if (selectedCoupon.maxDiscount > 0.0) {
                    Math.min(calculated, selectedCoupon.maxDiscount)
                } else {
                    calculated
                }
            }
        }

        // GST & Taxes -> 5% of food total + packaging charge
        val gstAndTaxes = (platformSubtotal + packagingCharge) * 0.05

        val total = Math.max(0.0, platformSubtotal + packagingCharge + deliveryFee + platformFee + gstAndTaxes - discount)

        return PlatformBill(
            platformName = platformName,
            subtotal = platformSubtotal,
            packagingCharge = packagingCharge,
            deliveryFee = deliveryFee,
            platformFee = platformFee,
            gstAndTaxes = gstAndTaxes,
            selectedCoupon = selectedCoupon,
            discountAmount = discount,
            totalBill = total
        )
    }

    // Automatically finds and suggests the coupon that yields the lowest cost on a platform for a given subtotal
    fun findBestCouponForSubtotal(platform: String, subtotal: Double): Coupon? {
        val coupons = if (platform == "Swiggy") SampleData.swiggyCoupons else SampleData.zomatoCoupons
        val markup = if (platform == "Swiggy") 1.03 else 1.05
        val platformSubtotal = subtotal * markup

        var bestCoupon: Coupon? = null
        var maxDiscount = 0.0

        for (coupon in coupons) {
            if (platformSubtotal >= coupon.minOrderValue) {
                val discount = if (coupon.flatDiscount > 0.0) {
                    coupon.flatDiscount
                } else {
                    val calc = platformSubtotal * (coupon.discountPercent / 100.0)
                    if (coupon.maxDiscount > 0.0) Math.min(calc, coupon.maxDiscount) else calc
                }

                if (discount > maxDiscount) {
                    maxDiscount = discount
                    bestCoupon = coupon
                }
            }
        }
        return bestCoupon
    }

    // Automatically finds and suggests the coupon that yields the lowest cost on a platform
    fun findBestCouponForPlatform(platform: String): Coupon? {
        return findBestCouponForSubtotal(platform, getCartSubtotal())
    }

    data class TypicalSavings(
        val cheaperPlatform: String,
        val savingsAmount: Double,
        val swiggyCouponCode: String,
        val zomatoCouponCode: String,
        val swiggyCouponDesc: String,
        val zomatoCouponDesc: String
    )

    fun calculateTypicalSavings(restaurant: Restaurant): TypicalSavings {
        val typicalSubtotal = 400.0
        val swiggyBest = findBestCouponForSubtotal("Swiggy", typicalSubtotal)
        val zomatoBest = findBestCouponForSubtotal("Zomato", typicalSubtotal)

        val swiggyBill = calculateBillForPlatform(
            platformName = "Swiggy",
            subtotal = typicalSubtotal,
            distanceKm = restaurant.distanceKm,
            isMember = isSwiggyOne.value,
            selectedCoupon = swiggyBest,
            isRainSurge = isRainSurge.value,
            isFestivalSurge = isFestivalSurge.value
        )

        val zomatoBill = calculateBillForPlatform(
            platformName = "Zomato",
            subtotal = typicalSubtotal,
            distanceKm = restaurant.distanceKm,
            isMember = isZomatoGold.value,
            selectedCoupon = zomatoBest,
            isRainSurge = isRainSurge.value,
            isFestivalSurge = isFestivalSurge.value
        )

        val cheaperPlatform = when {
            swiggyBill.totalBill < zomatoBill.totalBill -> "Swiggy"
            zomatoBill.totalBill < swiggyBill.totalBill -> "Zomato"
            else -> "Equal"
        }
        val savingsAmount = Math.abs(swiggyBill.totalBill - zomatoBill.totalBill)

        return TypicalSavings(
            cheaperPlatform = cheaperPlatform,
            savingsAmount = savingsAmount,
            swiggyCouponCode = swiggyBest?.code ?: "No coupon found",
            zomatoCouponCode = zomatoBest?.code ?: "No coupon found",
            swiggyCouponDesc = swiggyBest?.description ?: "No coupon found",
            zomatoCouponDesc = zomatoBest?.description ?: "No coupon found"
        )
    }

    // Call Gemini API to generate professional insights
    fun generateAISavingsTips() {
        val restaurant = _selectedRestaurant.value ?: return
        val comparison = calculateComparison()
        
        val basketItemsInfo = getCartItems().joinToString("\n") { 
            "- ${it.item.name} (Qty: ${it.quantity}) @ ₹${it.item.price} each" 
        }

        val swiggyCouponsStr = SampleData.swiggyCoupons.joinToString(", ") { 
            "${it.code} (Min order ₹${it.minOrderValue})" 
        }
        val zomatoCouponsStr = SampleData.zomatoCoupons.joinToString(", ") { 
            "${it.code} (Min order ₹${it.minOrderValue})" 
        }

        val swiggyPriceString = "₹${"%.2f".format(comparison.swiggyBill.totalBill)} (Food subtotal with 3% menu markup: ₹${"%.2f".format(comparison.swiggyBill.subtotal)}, Delivery: ₹${"%.2f".format(comparison.swiggyBill.deliveryFee)}, Coupon used: ${comparison.swiggyBill.selectedCoupon?.code ?: "None"})"
        val zomatoPriceString = "₹${"%.2f".format(comparison.zomatoBill.totalBill)} (Food subtotal with 5% menu markup: ₹${"%.2f".format(comparison.zomatoBill.subtotal)}, Delivery: ₹${"%.2f".format(comparison.zomatoBill.deliveryFee)}, Coupon used: ${comparison.zomatoBill.selectedCoupon?.code ?: "None"})"

        _isInsightLoading.value = true
        _aiInsight.value = ""

        viewModelScope.launch {
            val result = GeminiHelper.getSavingsInsight(
                restaurantName = restaurant.name,
                basketItemsInfo = basketItemsInfo,
                swiggyPriceString = swiggyPriceString,
                zomatoPriceString = zomatoPriceString,
                swiggyCoupons = swiggyCouponsStr,
                zomatoCoupons = zomatoCouponsStr
            )
            _aiInsight.value = result
            _isInsightLoading.value = false
        }
    }

    // Confirm that the user locked in these savings! This updates their lifetime database score.
    fun recordSavingsOutcome(cheaperPlatform: String, amount: Double) {
        val restaurant = _selectedRestaurant.value ?: return
        viewModelScope.launch {
            repository.saveSavingsRecord(
                restaurantName = restaurant.name,
                cheaperPlatform = cheaperPlatform,
                amount = amount
            )
        }
    }

    fun clearSavedSavings() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
