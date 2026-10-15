package com.zirren.tessera.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ConverterTest {

    private fun check(expected: Double, actual: BigDecimal?, delta: Double = 1e-4) {
        assertNotNull(actual)
        assertEquals(expected, actual!!.toDouble(), delta)
    }

    // ---- Length --------------------------------------------------------------------------

    @Test fun lengthConversions() {
        // 1 inch = 2.54 cm
        check(2.54, UnitConverter.convert(BigDecimal.ONE, LengthUnit.INCH, LengthUnit.CENTIMETER))
        // 1 foot = 12 inches
        check(12.0, UnitConverter.convert(BigDecimal.ONE, LengthUnit.FOOT, LengthUnit.INCH))
        // 1 meter = 1000 mm
        check(1000.0, UnitConverter.convert(BigDecimal.ONE, LengthUnit.METER, LengthUnit.MILLIMETER))
        // 1 mile = 1.609344 km
        check(1.609344, UnitConverter.convert(BigDecimal.ONE, LengthUnit.MILE, LengthUnit.KILOMETER))
        // 100 meters to feet (~328.084)
        check(328.084, UnitConverter.convert(BigDecimal(100), LengthUnit.METER, LengthUnit.FOOT))
    }

    // ---- Mass ----------------------------------------------------------------------------

    @Test fun massConversions() {
        // 1 lb = 453.59237 g
        check(453.59237, UnitConverter.convert(BigDecimal.ONE, MassUnit.POUND, MassUnit.GRAM))
        // 1 kg = ~2.20462 lb
        check(2.20462, UnitConverter.convert(BigDecimal.ONE, MassUnit.KILOGRAM, MassUnit.POUND))
        // 1 stone = 14 lb
        check(14.0, UnitConverter.convert(BigDecimal.ONE, MassUnit.STONE, MassUnit.POUND))
        // 1 tonne = 1000 kg
        check(1000.0, UnitConverter.convert(BigDecimal.ONE, MassUnit.METRIC_TONNE, MassUnit.KILOGRAM))
    }

    // ---- Volume --------------------------------------------------------------------------

    @Test fun volumeConversions() {
        // 1 US gallon = ~3.78541 liters
        check(3.78541, UnitConverter.convert(BigDecimal.ONE, VolumeUnit.US_GALLON, VolumeUnit.LITER))
        // 1 Imperial gallon = 4.54609 liters
        check(4.54609, UnitConverter.convert(BigDecimal.ONE, VolumeUnit.IMP_GALLON, VolumeUnit.LITER))
        // 1 US cup = 8 fl oz
        check(8.0, UnitConverter.convert(BigDecimal.ONE, VolumeUnit.US_CUP, VolumeUnit.US_FLUID_OUNCE))
        // 1 liter = 1000 ml
        check(1000.0, UnitConverter.convert(BigDecimal.ONE, VolumeUnit.LITER, VolumeUnit.MILLILITER))
    }

    // ---- Temperature ---------------------------------------------------------------------

    @Test fun temperatureConversions() {
        // 0 °C = 32 °F
        check(32.0, UnitConverter.convert(BigDecimal.ZERO, TempUnit.CELSIUS, TempUnit.FAHRENHEIT))
        // 100 °C = 212 °F
        check(212.0, UnitConverter.convert(BigDecimal(100), TempUnit.CELSIUS, TempUnit.FAHRENHEIT))
        // -40 °C = -40 °F
        check(-40.0, UnitConverter.convert(BigDecimal(-40), TempUnit.CELSIUS, TempUnit.FAHRENHEIT))
        // 0 °C = 273.15 K
        check(273.15, UnitConverter.convert(BigDecimal.ZERO, TempUnit.CELSIUS, TempUnit.KELVIN))
        // 72 °F to °C (~22.222 °C)
        check(22.2222, UnitConverter.convert(BigDecimal(72), TempUnit.FAHRENHEIT, TempUnit.CELSIUS))
    }

    // ---- Speed ---------------------------------------------------------------------------

    @Test fun speedConversions() {
        // 100 km/h = ~62.1371 mph
        check(62.1371, UnitConverter.convert(BigDecimal(100), SpeedUnit.KILOMETERS_PER_HOUR, SpeedUnit.MILES_PER_HOUR))
        // 1 m/s = 3.6 km/h
        check(3.6, UnitConverter.convert(BigDecimal.ONE, SpeedUnit.METERS_PER_SECOND, SpeedUnit.KILOMETERS_PER_HOUR))
        // 1 knot = ~1.15078 mph
        check(1.15078, UnitConverter.convert(BigDecimal.ONE, SpeedUnit.KNOTS, SpeedUnit.MILES_PER_HOUR))
    }

    // ---- Area ----------------------------------------------------------------------------

    @Test fun areaConversions() {
        // 1 hectare = 10,000 sq m
        check(10000.0, UnitConverter.convert(BigDecimal.ONE, AreaUnit.HECTARE, AreaUnit.SQ_METER))
        // 1 acre = ~4046.86 sq m
        check(4046.85642, UnitConverter.convert(BigDecimal.ONE, AreaUnit.ACRE, AreaUnit.SQ_METER))
        // 1 sq mile = 640 acres
        check(640.0, UnitConverter.convert(BigDecimal.ONE, AreaUnit.SQ_MILE, AreaUnit.ACRE))
        // 1 sq meter = ~10.7639 sq ft
        check(10.7639, UnitConverter.convert(BigDecimal.ONE, AreaUnit.SQ_METER, AreaUnit.SQ_FOOT))
    }

    // ---- Incompatible categories & identity ----------------------------------------------

    @Test fun identityConversionReturnsExactValue() {
        val v = BigDecimal("42.123")
        assertEquals(v, UnitConverter.convert(v, LengthUnit.METER, LengthUnit.METER))
        assertEquals(v, UnitConverter.convert(v, TempUnit.FAHRENHEIT, TempUnit.FAHRENHEIT))
    }

    @Test fun crossCategoryReturnsNull() {
        assertNull(UnitConverter.convert(BigDecimal.ONE, LengthUnit.METER, MassUnit.KILOGRAM))
        assertNull(UnitConverter.convert(BigDecimal.ONE, TempUnit.CELSIUS, SpeedUnit.KILOMETERS_PER_HOUR))
    }

    // ---- Region-aware defaults -----------------------------------------------------------

    @Test fun defaultsReflectRegion() {
        val usLen = UnitConverter.defaultPair(UnitCategory.LENGTH, "US")
        assertEquals(LengthUnit.INCH, usLen.first)
        assertEquals(LengthUnit.CENTIMETER, usLen.second)

        val seLen = UnitConverter.defaultPair(UnitCategory.LENGTH, "SE")
        assertEquals(LengthUnit.METER, seLen.first)
        assertEquals(LengthUnit.FOOT, seLen.second)

        val usTemp = UnitConverter.defaultPair(UnitCategory.TEMPERATURE, "US")
        assertEquals(TempUnit.FAHRENHEIT, usTemp.first)
        assertEquals(TempUnit.CELSIUS, usTemp.second)

        val seTemp = UnitConverter.defaultPair(UnitCategory.TEMPERATURE, "SE")
        assertEquals(TempUnit.CELSIUS, seTemp.first)
        assertEquals(TempUnit.FAHRENHEIT, seTemp.second)

        val ukMass = UnitConverter.defaultPair(UnitCategory.MASS, "GB")
        assertEquals(MassUnit.STONE, ukMass.first)
        assertEquals(MassUnit.KILOGRAM, ukMass.second)
    }
}
