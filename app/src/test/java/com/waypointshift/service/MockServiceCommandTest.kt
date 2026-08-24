package com.waypointshift.service

import com.waypointshift.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MockServiceCommandTest {
    @Test
    fun `decodes validated start and update coordinates`() {
        assertEquals(
            MockServiceCommand.Start(GeoPoint.of(41.0082, 28.9784)),
            MockServiceCommand.decode(MockServiceCommand.ACTION_START, 41.0082, 28.9784),
        )
        assertEquals(
            MockServiceCommand.Update(GeoPoint.of(-90.0, 180.0)),
            MockServiceCommand.decode(MockServiceCommand.ACTION_UPDATE, -90.0, 180.0),
        )
        assertEquals(
            MockServiceCommand.Stop,
            MockServiceCommand.decode(MockServiceCommand.ACTION_STOP, null, null),
        )
    }

    @Test
    fun `rejects missing non-finite and out-of-range coordinates`() {
        assertNull(MockServiceCommand.decode(MockServiceCommand.ACTION_START, null, 28.0))
        assertNull(MockServiceCommand.decode(MockServiceCommand.ACTION_START, Double.NaN, 28.0))
        assertNull(MockServiceCommand.decode(MockServiceCommand.ACTION_START, 91.0, 28.0))
        assertNull(MockServiceCommand.decode(MockServiceCommand.ACTION_UPDATE, 41.0, Double.POSITIVE_INFINITY))
        assertNull(MockServiceCommand.decode("unknown", 41.0, 28.0))
    }
}
