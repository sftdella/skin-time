package com.della.skintime.ui

import com.della.skintime.R

fun getStepIcon(stepName: String): Int = when {
    stepName.contains("facial wash", ignoreCase = true) ||
    stepName.contains("sabun muka", ignoreCase = true)  -> R.drawable.ic_facial_wash

    stepName.contains("serum", ignoreCase = true)        -> R.drawable.ic_serum
    stepName.contains("retinol", ignoreCase = true)      -> R.drawable.ic_retinol
    stepName.contains("moisturizer", ignoreCase = true) ||
    stepName.contains("pelembap", ignoreCase = true)     -> R.drawable.ic_moisturizer

    stepName.contains("sunscreen", ignoreCase = true) ||
    stepName.contains("spf", ignoreCase = true)          -> R.drawable.ic_sunscreen

    stepName.contains("sleeping mask", ignoreCase = true) -> R.drawable.ic_sleeping_mask
    stepName.contains("sheet mask", ignoreCase = true)   -> R.drawable.ic_sheet_mask

    stepName.contains("mask", ignoreCase = true) ||
    stepName.contains("masker", ignoreCase = true)       -> R.drawable.ic_mask

    stepName.contains("exfoliat", ignoreCase = true) ||
    stepName.contains("scrub", ignoreCase = true) ||
    stepName.contains("peeling", ignoreCase = true)      -> R.drawable.ic_exfoliating_gel

    else -> R.drawable.ic_ceklis_routine
}
