package com.prajwal.guajr.randomstringgenerator.DataClass

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class RandomString(
    val value: String,
    val length: Int,
    val created: Date
) : Parcelable

data class RandomStringResponse(
    val randomText: RandomTextData
)

data class RandomTextData(
    val value: String,
    val length: Int,
    val created: String
)