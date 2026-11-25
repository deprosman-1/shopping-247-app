package com.example

import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun verify_all_categories_are_defined() {
    val categories = ProductCategory.values().map { it.displayName }
    assertTrue(categories.contains("Clothes"))
    assertTrue(categories.contains("Shoes"))
    assertTrue(categories.contains("Bags"))
    assertTrue(categories.contains("Beauty Product"))
    assertTrue(categories.contains("Electronics"))
    assertTrue(categories.contains("Food"))
    assertTrue(categories.contains("Beverages"))
    assertTrue(categories.contains("Fruits/Vegetables"))
    assertTrue(categories.contains("Processed Foods/Pastries"))
    assertTrue(categories.contains("Frozen Foods/Meat"))
    org.junit.Assert.assertFalse(categories.contains("Kids"))
    org.junit.Assert.assertFalse(categories.contains("Pharmaceuticals"))
    assertEquals(24, categories.size)
  }

  @Test
  fun verify_dynamic_variant_price_calculation() {
    val product = Product(
      id = "test-1",
      title = "Sneaker Test",
      category = "Shoes",
      brand = "Standard",
      availableBrands = listOf("Standard", "Nike Tech", "Luxury Pro"),
      availableSizes = listOf("40", "45", "46"),
      availableColors = listOf(ProductColor("Red", "#FF0000")),
      basePrice = 10000.0,
      discountPercent = 10,
      stockQuantity = 5,
      imageUrl = "",
      description = "Test"
    )

    // Base discount calculation
    val basePriceWithDiscount = product.calculatePrice("Standard", "40")
    assertEquals(9000.0, basePriceWithDiscount, 0.01)

    // With luxury brand multiplier
    val proPrice = product.calculatePrice("Luxury Pro", "40")
    assertTrue(proPrice > basePriceWithDiscount)
  }

  @Test
  fun verify_customer_wishlist_storage() {
    val customer = com.example.data.model.Customer(
      id = "c-123",
      fullName = "Test User",
      email = "test@example.com",
      phoneNumber = "1234567890",
      deliveryAddress = "Test Address",
      wishlistProductIds = listOf("prod-shoes-01", "prod-elec-01")
    )

    assertEquals(2, customer.wishlistProductIds.size)
    assertTrue(customer.wishlistProductIds.contains("prod-shoes-01"))

    // Toggle item out
    val toggled = customer.wishlistProductIds.toMutableList()
    toggled.remove("prod-shoes-01")
    assertEquals(1, toggled.size)
    assertEquals("prod-elec-01", toggled.first())
  }
}
