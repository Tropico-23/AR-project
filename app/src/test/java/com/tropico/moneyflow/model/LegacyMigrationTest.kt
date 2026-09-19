package com.tropico.moneyflow.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyMigrationTest {
    @Test
    fun newCategoriesAreAvailable() {
        assertEquals("Coffee", ExpenseCategory.Coffee.label)
        assertEquals("Subscriptions", ExpenseCategory.Subscriptions.label)
    }
}
