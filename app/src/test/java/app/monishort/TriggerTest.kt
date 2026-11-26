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
    private fun level(cur: Int, prev: Int) =
        TriggerEvent(TriggerTypes.BATTERY_LEVEL, mapOf("level" to "$cur", "previous" to "$prev"))

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
