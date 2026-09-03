package kz.qlms.app.util

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * Minimal standard geohash encoder plus a bounding-box helper for "nearby"
 * Firestore queries (Firestore has no native geo-radius query, so incidents are
 * indexed by geohash prefix and re-filtered client-side by exact distance).
 */
object GeoHash {
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    fun encode(latitude: Double, longitude: Double, precision: Int = 7): String {
        var latInterval = -90.0 to 90.0
        var lonInterval = -180.0 to 180.0
        val hash = StringBuilder()
        var isEven = true
        var bit = 0
        var ch = 0

        while (hash.length < precision) {
            if (isEven) {
                val mid = (lonInterval.first + lonInterval.second) / 2
                if (longitude > mid) {
                    ch = ch or (1 shl (4 - bit))
                    lonInterval = mid to lonInterval.second
                } else {
                    lonInterval = lonInterval.first to mid
                }
            } else {
                val mid = (latInterval.first + latInterval.second) / 2
                if (latitude > mid) {
                    ch = ch or (1 shl (4 - bit))
                    latInterval = mid to latInterval.second
                } else {
                    latInterval = latInterval.first to mid
                }
            }
            isEven = !isEven
            if (bit < 4) {
                bit++
            } else {
                hash.append(BASE32[ch])
                bit = 0
                ch = 0
            }
        }
        return hash.toString()
    }

    /** Great-circle distance in kilometers (haversine). */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadiusKm * c
    }

    /**
     * Geohash prefixes covering a radius search box, short enough to over-fetch
     * a little and precise enough that we're not scanning the whole city.
     * Callers must still filter results with [distanceKm] since a prefix match
     * is a rectangle, not a circle.
     */
    fun neighborsForRadius(latitude: Double, longitude: Double, radiusKm: Double): List<String> {
        val precision = when {
            radiusKm <= 1.2 -> 6
            radiusKm <= 5.0 -> 5
            radiusKm <= 20.0 -> 4
            else -> 3
        }
        val center = encode(latitude, longitude, precision)
        val latStep = radiusKm / 111.0
        val lonStep = radiusKm / (111.0 * max(0.2, cos(Math.toRadians(latitude))))
        val hashes = linkedSetOf(center)
        for (dLat in -1..1) {
            for (dLon in -1..1) {
                val lat = (latitude + dLat * latStep).coerceIn(-90.0, 90.0)
                val lon = min(180.0, max(-180.0, longitude + dLon * lonStep))
                hashes += encode(lat, lon, precision)
            }
        }
        return hashes.toList()
    }
}
