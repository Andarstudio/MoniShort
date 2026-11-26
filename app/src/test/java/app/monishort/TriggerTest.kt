package app.monishort

import app.monishort.triggers.BatteryLevelTrigger
import app.monishort.triggers.ChargerConnectedTrigger
import app.monishort.triggers.TriggerEvent
import app.monishort.triggers.TriggerTypes
import app.monishort.triggers.defaultTriggerRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerTest {
    private fun level(cur: Int, prev: Int, initial: Boolean = false) =
        TriggerEvent(
            TriggerTypes.BATTERY_LEVEL,
            buildMap {
                put("level", "$cur")
                put("previous", "$prev")
                if (initial) put("initial", "true")
            },
        )

    @Test fun belowFiresOnlyWhenCrossingDown() {
        val cfg = mapOf("level" to "20", "direction" to "below")
        assertTrue(BatteryLevelTrigger.matches(level(20, 21), cfg))
        assertFalse(BatteryLevelTrigger.matches(level(19, 20), cfg)) // already below before
        assertFalse(BatteryLevelTrigger.matches(level(50, 49), cfg))
    }

    @Test fun aboveFiresOnlyWhenCrossingUp() {
        val cfg = mapOf("level" to "80", "direction" to "above")
        assertTrue(BatteryLevelTrigger.matches(level(80, 79), cfg))
        assertFalse(BatteryLevelTrigger.matches(level(81, 80), cfg))
    }

    @Test fun belowFiresOnInitialReadingWhenAlreadyBelowThreshold() {
        val cfg = mapOf("level" to "20", "direction" to "below")
        assertTrue(BatteryLevelTrigger.matches(level(18, 18, initial = true), cfg))
        assertFalse(BatteryLevelTrigger.matches(level(25, 25, initial = true), cfg))
    }

    @Test fun aboveFiresOnInitialReadingWhenAlreadyAboveThreshold() {
        val cfg = mapOf("level" to "80", "direction" to "above")
        assertTrue(BatteryLevelTrigger.matches(level(85, 85, initial = true), cfg))
        assertFalse(BatteryLevelTrigger.matches(level(75, 75, initial = true), cfg))
    }

    @Test fun configValidation() {
        assertTrue(BatteryLevelTrigger.validate(mapOf("level" to "20", "direction" to "below")).isEmpty())
        assertEquals(2, BatteryLevelTrigger.validate(mapOf("level" to "0", "direction" to "sideways")).size)
    }

    @Test fun simpleTriggersMatchByType() {
        assertTrue(ChargerConnectedTrigger.matches(TriggerEvent(TriggerTypes.CHARGER_CONNECTED), emptyMap()))
        assertFalse(ChargerConnectedTrigger.matches(TriggerEvent(TriggerTypes.CHARGER_DISCONNECTED), emptyMap()))
    }

    @Test fun registryKnowsAllTriggers() {
        val r = defaultTriggerRegistry()
        assertEquals(5, r.all().size)
        assertTrue(r.get(TriggerTypes.DEVICE_BOOT) != null)
    }
}
