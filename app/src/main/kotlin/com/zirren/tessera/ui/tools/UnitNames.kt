package com.zirren.tessera.ui.tools

import com.zirren.tessera.R
import com.zirren.tessera.core.model.AreaUnit
import com.zirren.tessera.core.model.LengthUnit
import com.zirren.tessera.core.model.MassUnit
import com.zirren.tessera.core.model.MeasureUnit
import com.zirren.tessera.core.model.SpeedUnit
import com.zirren.tessera.core.model.TempUnit
import com.zirren.tessera.core.model.UnitCategory
import com.zirren.tessera.core.model.VolumeUnit

object UnitNames {

    fun categoryTitle(category: UnitCategory): Int = when (category) {
        UnitCategory.LENGTH -> R.string.converter_cat_length
        UnitCategory.MASS -> R.string.converter_cat_mass
        UnitCategory.VOLUME -> R.string.converter_cat_volume
        UnitCategory.TEMPERATURE -> R.string.converter_cat_temperature
        UnitCategory.SPEED -> R.string.converter_cat_speed
        UnitCategory.AREA -> R.string.converter_cat_area
    }

    fun unitName(unit: MeasureUnit): Int = when (unit) {
        LengthUnit.MILLIMETER -> R.string.unit_mm
        LengthUnit.CENTIMETER -> R.string.unit_cm
        LengthUnit.METER -> R.string.unit_m
        LengthUnit.KILOMETER -> R.string.unit_km
        LengthUnit.INCH -> R.string.unit_in
        LengthUnit.FOOT -> R.string.unit_ft
        LengthUnit.YARD -> R.string.unit_yd
        LengthUnit.MILE -> R.string.unit_mi

        MassUnit.MILLIGRAM -> R.string.unit_mg
        MassUnit.GRAM -> R.string.unit_g
        MassUnit.KILOGRAM -> R.string.unit_kg
        MassUnit.METRIC_TONNE -> R.string.unit_t
        MassUnit.OUNCE -> R.string.unit_oz
        MassUnit.POUND -> R.string.unit_lb
        MassUnit.STONE -> R.string.unit_st

        VolumeUnit.MILLILITER -> R.string.unit_ml
        VolumeUnit.LITER -> R.string.unit_l
        VolumeUnit.CUBIC_METER -> R.string.unit_m3
        VolumeUnit.US_FLUID_OUNCE -> R.string.unit_fl_oz
        VolumeUnit.US_CUP -> R.string.unit_cup
        VolumeUnit.US_PINT -> R.string.unit_pt
        VolumeUnit.US_GALLON -> R.string.unit_gal
        VolumeUnit.IMP_PINT -> R.string.unit_imp_pt
        VolumeUnit.IMP_GALLON -> R.string.unit_imp_gal

        TempUnit.CELSIUS -> R.string.unit_celsius
        TempUnit.FAHRENHEIT -> R.string.unit_fahrenheit
        TempUnit.KELVIN -> R.string.unit_kelvin

        SpeedUnit.METERS_PER_SECOND -> R.string.unit_mps
        SpeedUnit.KILOMETERS_PER_HOUR -> R.string.unit_kmh
        SpeedUnit.MILES_PER_HOUR -> R.string.unit_mph
        SpeedUnit.KNOTS -> R.string.unit_kn

        AreaUnit.SQ_CENTIMETER -> R.string.unit_cm2
        AreaUnit.SQ_METER -> R.string.unit_m2
        AreaUnit.HECTARE -> R.string.unit_ha
        AreaUnit.SQ_KILOMETER -> R.string.unit_km2
        AreaUnit.SQ_INCH -> R.string.unit_in2
        AreaUnit.SQ_FOOT -> R.string.unit_ft2
        AreaUnit.ACRE -> R.string.unit_ac
        AreaUnit.SQ_MILE -> R.string.unit_mi2
    }
}
