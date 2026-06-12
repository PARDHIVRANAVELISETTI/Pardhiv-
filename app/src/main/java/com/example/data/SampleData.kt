package com.example.data

object SampleData {
    val restaurants = listOf(
        Restaurant(
            id = 1,
            name = "Domino's Pizza",
            cuisine = "Pizzas, Fast Food, Italian",
            rating = 4.3,
            deliveryTime = "25 mins",
            logoUrl = "🍕",
            distanceKm = 2.4,
            isPopular = true,
            swiggyDeliveryRating = 4.7,
            zomatoDeliveryRating = 4.4
        ),
        Restaurant(
            id = 2,
            name = "Biryani By Kilo",
            cuisine = "Biryani, Mughlai, Kebab",
            rating = 4.5,
            deliveryTime = "40 mins",
            logoUrl = "🍲",
            distanceKm = 4.2,
            isPopular = true,
            swiggyDeliveryRating = 4.2,
            zomatoDeliveryRating = 4.8
        ),
        Restaurant(
            id = 3,
            name = "Burger King",
            cuisine = "Burgers, Fast Food, Drinks",
            rating = 4.1,
            deliveryTime = "18 mins",
            logoUrl = "🍔",
            distanceKm = 1.8,
            isPopular = true,
            swiggyDeliveryRating = 4.9,
            zomatoDeliveryRating = 4.6
        ),
        Restaurant(
            id = 4,
            name = "Haldiram's Sweets",
            cuisine = "Mithai, North Indian, Street Food",
            rating = 4.4,
            deliveryTime = "30 mins",
            logoUrl = "🧇",
            distanceKm = 3.5,
            isPopular = false,
            swiggyDeliveryRating = 4.1,
            zomatoDeliveryRating = 4.2
        ),
        Restaurant(
            id = 5,
            name = "Subway",
            cuisine = "Healthy Subs, Salads, Cookies",
            rating = 4.2,
            deliveryTime = "22 mins",
            logoUrl = "🥪",
            distanceKm = 2.1,
            isPopular = false,
            swiggyDeliveryRating = 4.5,
            zomatoDeliveryRating = 4.7
        ),
        Restaurant(
            id = 6,
            name = "KFC",
            cuisine = "Fried Chicken, Burgers, Snacks",
            rating = 4.0,
            deliveryTime = "20 mins",
            logoUrl = "🍗",
            distanceKm = 3.0,
            isPopular = true,
            swiggyDeliveryRating = 4.4,
            zomatoDeliveryRating = 4.3
        )
    )

    val menuItems = mapOf(
        1 to listOf( // Domino's
            MenuItem(101, "Margherita Pizza (Regular)", "Classic cheese and tomato sauce", 239.0, "Pizza", false, "🍕"),
            MenuItem(102, "Farmhouse Pizza (Medium)", "Delightful combination of onion, capsicum, tomato & mushroom", 459.0, "Pizza", false, "🍕"),
            MenuItem(103, "Classic Garlic Bread", "Baked garlic bread with seasoning", 109.0, "Sides", false, "🥖"),
            MenuItem(104, "Stuffed Garlic Bread", "Garlic bread stuffed with cheese, sweet corn & jalapeno", 179.0, "Sides", false, "🥖"),
            MenuItem(105, "Choco Lava Cake", "Warm chocolate cake with filled lava chocolate", 119.0, "Dessert", false, "🧁"),
            MenuItem(106, "Veg Extravaganza Pizza (Regular)", "Black olives, capsicum, onion, grilled mushroom, corn, jalapeño & extra cheese", 329.0, "Pizza", false, "🍕")
        ),
        2 to listOf( // Biryani By Kilo
            MenuItem(201, "Veg Hyderabadi Biryani (1/2 kg)", "Basmati rice cooked cooked with green veggies & aromatic spices", 349.0, "Biryani", false, "🍲"),
            MenuItem(202, "Chicken Dum Biryani (1/2 kg)", "Authentic chicken dum biryani cooked in an earthen pot", 459.0, "Biryani", true, "🍲"),
            MenuItem(203, "Mutton Hyderabadi Biryani (1/2 kg)", "Mouth-watering pieces of mutton layered with long basmati rice", 599.0, "Biryani", true, "🍲"),
            MenuItem(204, "Galouti Kebab (4 pcs)", "Melt-in-mouth minced mutton kebabs served with mint chutney", 399.0, "Starters", true, "🍢"),
            MenuItem(205, "Double Ka Meetha", "Traditional Hyderabadi bread pudding dessert", 149.0, "Dessert", false, "🍰"),
            MenuItem(206, "Extra Salan & Raita", "Traditional spicy curry and refreshing curd side", 49.0, "Sides", false, "🥣")
        ),
        3 to listOf( // Burger King
            MenuItem(301, "Crispy Veg Burger", "Crispy veg patty, onion, mayo & liquid cheese", 79.0, "Burgers", false, "🍔"),
            MenuItem(302, "Veg Whopper", "Our signature double-stacked juicy veg burger", 179.0, "Burgers", false, "🍔"),
            MenuItem(303, "Chicken Whopper", "Standard grilled chicken whopper signature size", 219.0, "Burgers", true, "🍔"),
            MenuItem(304, "Medium Crispy French Fries", "Perfectly salted crispy fried potatoes", 119.0, "Sides", false, "🍟"),
            MenuItem(305, "Cheesy Fries", "Crispy French Fries loaded with warm liquid cheese sauce", 159.0, "Sides", false, "🍟"),
            MenuItem(306, "Chocolate Thick Shake", "Creamy, dense chocolate milkshake", 139.0, "Beverages", false, "🥤")
        ),
        4 to listOf( // Haldiram's
            MenuItem(401, "Special Chole Bhature (2 pcs)", "Delicious spicy chickpeas served with fluffy golden bhaturas", 160.0, "Thalis & Platter", false, "🍛"),
            MenuItem(402, "Special Pav Bhaji", "Rich butter-toasted pav served with spicy mashed vegetable bhaji", 140.0, "Street Food", false, "🍞"),
            MenuItem(403, "Raj Kachori", "Crisp puri stuffed with yogurt, potatoes, sprouts, chutneys & sev", 120.0, "Street Food", false, "🥙"),
            MenuItem(404, "Dry Petha (250g)", "Traditional sweet treat made with ash gourd pieces", 90.0, "Sweets", false, "🍬"),
            MenuItem(405, "Kaju Katli (250g)", "Premium diamond-shaped melt-in-mouth cashew sweet fudge", 300.0, "Sweets", false, "💎"),
            MenuItem(406, "Deluxe Veg Thali", "Rice, Paneer Butter Masala, Dal Makhani, 2 Roti, Sweet, Raita, Papad", 280.0, "Thalis & Platter", false, "🍱")
        ),
        5 to listOf( // Subway
            MenuItem(501, "Paneer Tikka Sub (15cm/6')", "Succulent pieces of marinated paneer tikka with veggies & sauces", 229.0, "Subs", false, "🥪"),
            MenuItem(502, "Veg Shammi Sub (15cm/6')", "Healthy spiced lentil shammi kebab patty with salads", 199.0, "Subs", false, "🥪"),
            MenuItem(503, "Chicken Kofta Sub (15cm/6')", "Tender seasoned chicken meatballs with choice of bread and sauces", 249.0, "Subs", true, "🥪"),
            MenuItem(504, "Double Chocolate Cookie", "Warm baked cookie packed with rich chocolate chunks", 69.0, "Sides", false, "🍪"),
            MenuItem(505, "Subway Veg Salad Bowl", "A hearty bowl of crisp lettuce, toppings, paneer tikka, and dressings", 259.0, "Healthy Options", false, "🥗"),
            MenuItem(506, "Lipton Iced Tea (Peach)", "Refreshing sweetened peach iced tea", 89.0, "Beverages", false, "🥤")
        ),
        6 to listOf( // KFC
            MenuItem(601, "2 PC Hot & Crispy Chicken", "Signature crunchy golden fried chicken pieces", 249.0, "Fried Chicken", true, "🍗"),
            MenuItem(602, "Zinger Veg Burger", "Crunchy vegetable patty with cool lettuce and signature mayo", 159.0, "Burgers", false, "🍔"),
            MenuItem(603, "Zinger Chicken Burger", "Crispy juicy chicken breast fillet with lettuce and mayo", 199.0, "Burgers", true, "🍔"),
            MenuItem(604, "Popcorn Chicken (Medium)", "Bite-size tender boneless chicken pieces inside", 189.0, "Snacks", true, "🍿"),
            MenuItem(605, "Chicken Longer Burger", "Longer bun topped with single crispy chicken strip", 119.0, "Burgers", true, "🍔"),
            MenuItem(606, "7 PC Chicken Bucket Deal", "Shareable bucket containing 4 pc Hot & Crispy, 3 pc Hot Wings", 649.0, "Fried Chicken", true, "Bucket")
        )
    )

    val swiggyCoupons = listOf(
        Coupon(
            code = "WELCOME50",
            platform = "Swiggy",
            description = "50% off on your first few orders. Min order ₹149. Max saving ₹100.",
            minOrderValue = 149.0,
            discountPercent = 50.0,
            maxDiscount = 100.0
        ),
        Coupon(
            code = "SWIGGYIT",
            platform = "Swiggy",
            description = "Get 40% off on standard restaurants. Min order ₹199. Max saving ₹80.",
            minOrderValue = 199.0,
            discountPercent = 40.0,
            maxDiscount = 80.0
        ),
        Coupon(
            code = "JUMBO120",
            platform = "Swiggy",
            description = "Save more on high value orders. Flat ₹120 off. Min order ₹499.",
            minOrderValue = 499.0,
            flatDiscount = 120.0
        ),
        Coupon(
            code = "EATON",
            platform = "Swiggy",
            description = "Flat ₹30 off on orders above ₹249 on selected cafes.",
            minOrderValue = 249.0,
            flatDiscount = 30.0
        )
    )

    val zomatoCoupons = listOf(
        Coupon(
            code = "CRAVINGS",
            platform = "Zomato",
            description = "Satisfy cravings with 50% off. Min order ₹159. Max saving ₹120.",
            minOrderValue = 159.0,
            discountPercent = 50.0,
            maxDiscount = 120.0
        ),
        Coupon(
            code = "TRYNEW",
            platform = "Zomato",
            description = "Try new hidden gems. Get 30% off. Min order ₹149. Max saving ₹75.",
            minOrderValue = 149.0,
            discountPercent = 30.0,
            maxDiscount = 75.0
        ),
        Coupon(
            code = "ZOMPAYTM",
            platform = "Zomato",
            description = "Flat ₹100 off when ordering with Paytm wallet. Min order ₹500.",
            minOrderValue = 500.0,
            flatDiscount = 100.0
        ),
        Coupon(
            code = "SAVEMORE",
            platform = "Zomato",
            description = "Save flat ₹40 on orders above ₹299.",
            minOrderValue = 299.0,
            flatDiscount = 40.0
        )
    )
}
