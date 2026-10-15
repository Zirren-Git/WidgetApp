package com.zirren.tessera.core.model

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Categories of physical quantities Tessera converts. */
enum class UnitCategory(val label: String) {
    LENGTH("Length"),
    MASS("Mass"),
    VOLUME("Volume"),
    TEMPERATURE("Temperature"),
    SPEED("Speed"),
    AREA("Area");

    val units: List<MeasureUnit> get() = when (this) {
        LENGTH -> LengthUnit.entries
        MASS -> MassUnit.entries
        VOLUME -> VolumeUnit.entries
        TEMPERATURE -> TempUnit.entries
        SPEED -> SpeedUnit.entries
        AREA -> AreaUnit.entries
    }
}

/** Common interface for every unit in Tessera. */
sealed interface MeasureUnit {
    val symbol: String
    val category: UnitCategory
}

// ---- Length (base unit: meter) -------------------------------------------------------------

enum class LengthUnit(
    override val symbol: String,
    val factorToMeter: BigDecimal,
) : MeasureUnit {
    MILLIMETER("mm", BigDecimal("0.001")),
    CENTIMETER("cm", BigDecimal("0.01")),
    METER("m", BigDecimal.ONE),
    KILOMETER("km", BigDecimal("1000")),
    INCH("in", BigDecimal("0.0254")),
    FOOT("ft", BigDecimal("0.3048")),
    YARD("yd", BigDecimal("0.9144")),
    MILE("mi", BigDecimal("1609.344"));

    override val category: UnitCategory get() = UnitCategory.LENGTH
}

// ---- Mass (base unit: kilogram) ------------------------------------------------------------

enum class MassUnit(
    override val symbol: String,
    val factorToKg: BigDecimal,
) : MeasureUnit {
    MILLIGRAM("mg", BigDecimal("0.000001")),
    GRAM("g", BigDecimal("0.001")),
    KILOGRAM("kg", BigDecimal.ONE),
    METRIC_TONNE("t", BigDecimal("1000")),
    OUNCE("oz", BigDecimal("0.028349523125")),
    POUND("lb", BigDecimal("0.45359237")),
    STONE("st", BigDecimal("6.35029318"));

    override val category: UnitCategory get() = UnitCategory.MASS
}

// ---- Volume (base unit: liter) -------------------------------------------------------------

enum class VolumeUnit(
    override val symbol: String,
    val factorToLiter: BigDecimal,
) : MeasureUnit {
    MILLILITER("ml", BigDecimal("0.001")),
    LITER("l", BigDecimal.ONE),
    CUBIC_METER("m\u00B3", BigDecimal("1000")),
    US_FLUID_OUNCE("fl oz", BigDecimal("0.0295735295625")),
    US_CUP("cup", BigDecimal("0.2365882365")),
    US_PINT("pt", BigDecimal("0.473176473")),
    US_GALLON("gal", BigDecimal("3.785411784")),
    IMP_PINT("imp pt", BigDecimal("0.56826125")),
    IMP_GALLON("imp gal", BigDecimal("4.54609"));

    override val category: UnitCategory get() = UnitCategory.VOLUME
}

// ---- Temperature (special affine conversion) -----------------------------------------------

enum class TempUnit(override val symbol: String) : MeasureUnit {
    CELSIUS("\u00B0C"),
    FAHRENHEIT("\u00B0F"),
    KELVIN("K");

    override val category: UnitCategory get() = UnitCategory.TEMPERATURE

    fun toCelsius(v: BigDecimal): BigDecimal = when (this) {
        CELSIUS -> v
        FAHRENHEIT -> v.subtract(BigDecimal(32)).multiply(BigDecimal(5)).divide(BigDecimal(9), MathContext.DECIMAL128)
        KELVIN -> v.subtract(BigDecimal("273.15"))
    }

    fun fromCelsius(c: BigDecimal): BigDecimal = when (this) {
        CELSIUS -> c
        FAHRENHEIT -> c.multiply(BigDecimal(9)).divide(BigDecimal(5), MathContext.DECIMAL128).add(BigDecimal(32))
        KELVIN -> c.add(BigDecimal("273.15"))
    }
}

// ---- Speed (base unit: meters per second) -------------------------------------------------

enum class SpeedUnit(
    override val symbol: String,
    val factorToMps: BigDecimal,
) : MeasureUnit {
    METERS_PER_SECOND("m/s", BigDecimal.ONE),
    KILOMETERS_PER_HOUR("km/h", BigDecimal.ONE.divide(BigDecimal("3.6"), MathContext.DECIMAL128)),
    MILES_PER_HOUR("mph", BigDecimal("0.44704")),
    KNOTS("kn", BigDecimal("0.5144444444444444"));

    override val category: UnitCategory get() = UnitCategory.SPEED
}

// ---- Area (base unit: square meter) --------------------------------------------------------

enum class AreaUnit(
    override val symbol: String,
    val factorToSqMeter: BigDecimal,
) : MeasureUnit {
    SQ_CENTIMETER("cm\u00B2", BigDecimal("0.0001")),
    SQ_METER("m\u00B2", BigDecimal.ONE),
    HECTARE("ha", BigDecimal("10000")),
    SQ_KILOMETER("km\u00B2", BigDecimal("1000000")),
    SQ_INCH("sq in", BigDecimal("0.00064516")),
    SQ_FOOT("sq ft", BigDecimal("0.09290304")),
    ACRE("ac", BigDecimal("4046.8564224")),
    SQ_MILE("sq mi", BigDecimal("2589988.110336"));

    override val category: UnitCategory get() = UnitCategory.AREA
}

/** Pure-Kotlin unit converter with region-aware defaults. */
object UnitConverter {

    /** Converts [value] from [from] to [to]. Returns null if units belong to different categories. */
    fun convert(value: BigDecimal, from: MeasureUnit, to: MeasureUnit): BigDecimal? {
        if (from.category != to.category) return null
        if (from == to) return value

        return when {
            from is TempUnit && to is TempUnit -> {
                val celsius = from.toCelsius(value)
                to.fromCelsius(celsius)
            }
            from is LengthUnit && to is LengthUnit -> {
                val meters = value.multiply(from.factorToMeter)
                meters.divide(to.factorToMeter, MathContext.DECIMAL128)
            }
            from is MassUnit && to is MassUnit -> {
                val kg = value.multiply(from.factorToKg)
                kg.divide(to.factorToKg, MathContext.DECIMAL128)
            }
            from is VolumeUnit && to is VolumeUnit -> {
                val liters = value.multiply(from.factorToLiter)
                liters.divide(to.factorToLiter, MathContext.DECIMAL128)
            }
            from is SpeedUnit && to is SpeedUnit -> {
                val mps = value.multiply(from.factorToMps)
                mps.divide(to.factorToMps, MathContext.DECIMAL128)
            }
            from is AreaUnit && to is AreaUnit -> {
                val sqMeters = value.multiply(from.factorToSqMeter)
                sqMeters.divide(to.factorToSqMeter, MathContext.DECIMAL128)
            }
            else -> null
        }
    }

    /** Sensible default unit pairs for a category based on the phone's country code. */
    fun defaultPair(category: UnitCategory, countryCode: String?): Pair<MeasureUnit, MeasureUnit> {
        val region = countryCode?.trim()?.uppercase().orEmpty()
        val isUs = region in setOf("US", "PR", "GU", "VI", "AS", "MP", "UM", "LR", "MM")
        val isUk = region in setOf("GB", "IM", "JE", "GG")

        return when (category) {
            UnitCategory.LENGTH -> if (isUs) LengthUnit.INCH to LengthUnit.CENTIMETER else LengthUnit.METER to LengthUnit.FOOT
            UnitCategory.MASS -> if (isUs) MassUnit.POUND to MassUnit.KILOGRAM else if (isUk) MassUnit.STONE to MassUnit.KILOGRAM else MassUnit.KILOGRAM to MassUnit.POUND
            UnitCategory.VOLUME -> if (isUs) VolumeUnit.US_GALLON to VolumeUnit.LITER else if (isUk) VolumeUnit.IMP_PINT to VolumeUnit.LITER else VolumeUnit.LITER to VolumeUnit.US_GALLON
            UnitCategory.TEMPERATURE -> if (isUs) TempUnit.FAHRENHEIT to TempUnit.CELSIUS else TempUnit.CELSIUS to TempUnit.FAHRENHEIT
            UnitCategory.SPEED -> if (isUs || isUk) SpeedUnit.MILES_PER_HOUR to SpeedUnit.KILOMETERS_PER_HOUR else SpeedUnit.KILOMETERS_PER_HOUR to SpeedUnit.MILES_PER_HOUR
            UnitCategory.AREA -> if (isUs) AreaUnit.ACRE to AreaUnit.HECTARE else AreaUnit.SQ_METER to AreaUnit.SQ_FOOT
        }
    }
}
