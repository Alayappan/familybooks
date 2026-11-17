package com.example.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    fun formatMonthYear(timestamp: Long): String {
        return monthYearFormat.format(Date(timestamp))
    }

    fun formatPercentage(value: Double): String {
        val sign = if (value > 0) "+" else ""
        return "$sign${String.format(Locale.US, "%.1f", value)}%"
    }

    fun getCategoryIcon(category: String): ImageVector {
        return when (category.lowercase(Locale.ROOT)) {
            "groceries", "supermarket", "shopping" -> Icons.Default.ShoppingCart
            "utilities", "electricity", "water", "internet" -> Icons.Default.WaterDamage
            "dining", "food", "restaurants" -> Icons.Default.Fastfood
            "education", "school", "books", "tuition" -> Icons.Default.School
            "healthcare", "medicine", "doctor" -> Icons.Default.LocalHospital
            "transport", "fuel", "car", "travel" -> Icons.Default.DirectionsCar
            "salary", "bonus", "income" -> Icons.Default.Work
            "rent", "mortgage" -> Icons.Default.AccountBalance
            else -> Icons.Default.Category
        }
    }

    fun getCategoryColor(category: String): Color {
        return when (category.lowercase(Locale.ROOT)) {
            "groceries", "shopping" -> Color(0xFF43A047)
            "utilities", "internet" -> Color(0xFF1E88E5)
            "dining", "food" -> Color(0xFFFB8C00)
            "education", "books" -> Color(0xFF8E24AA)
            "healthcare", "medicine" -> Color(0xFFE53935)
            "transport", "fuel" -> Color(0xFF00ACC1)
            "salary", "income" -> Color(0xFF2E7D32)
            "rent" -> Color(0xFF3949AB)
            else -> Color(0xFF546E7A)
        }
    }

    fun getMemberAvatarColor(name: String): Color {
        val colors = listOf(
            Color(0xFF0A2540),
            Color(0xFF00A88D),
            Color(0xFF1E5B94),
            Color(0xFF8E24AA),
            Color(0xFFD81B60),
            Color(0xFFFB8C00),
            Color(0xFF00897B),
            Color(0xFF5C6BC0)
        )
        val hash = kotlin.math.abs(name.hashCode())
        return colors[hash % colors.size]
    }

    fun parseAvatarColor(hex: String, fallbackName: String): Color {
        if (hex.isBlank()) return getMemberAvatarColor(fallbackName)
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {
            getMemberAvatarColor(fallbackName)
        }
    }
}
