package org.jlleitschuh.gradle.ktlint.sample.mppandroid

interface LocationProvider {
    fun startLocationFix()
    fun stopLocationFix()
    fun getCurrentLocation(): Location

    data class Location(
        val latitude: Long,
        val longitude: Long
    )
}
