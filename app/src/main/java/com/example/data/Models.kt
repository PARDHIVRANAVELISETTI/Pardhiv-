package com.example.data

data class Restaurant(
    val id: Int,
    val name: String,
    val cuisine: String,
    val rating: Double,
    val deliveryTime: String,
    val logoUrl: String,
    val distanceKm: Double,
    val isPopular: Boolean = false,
    val swiggyDeliveryRating: Double = 4.0,
    val zomatoDeliveryRating: Double = 4.0
)

data class MenuItem(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double, // Base price
    val category: String,
    val isVeg: Boolean,
    val iconEmoji: String = "🍲"
)

data class CartItem(
    val item: MenuItem,
    val quantity: Int
)

data class Coupon(
    val code: String,
    val platform: String, // "Swiggy" or "Zomato" or "Both"
    val description: String,
    val minOrderValue: Double,
    val discountPercent: Double = 0.0,
    val maxDiscount: Double = 0.0,
    val flatDiscount: Double = 0.0
)

data class PlatformBill(
    val platformName: String,
    val subtotal: Double,
    val packagingCharge: Double,
    val deliveryFee: Double,
    val platformFee: Double,
    val gstAndTaxes: Double,
    val selectedCoupon: Coupon?,
    val discountAmount: Double,
    val totalBill: Double
)

data class ComparisonResult(
    val swiggyBill: PlatformBill,
    val zomatoBill: PlatformBill,
    val cheaperPlatform: String, // "Swiggy", "Zomato", "Equal"
    val savingsAmount: Double
)
