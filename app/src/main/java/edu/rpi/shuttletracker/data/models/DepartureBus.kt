package edu.rpi.shuttletracker.data.models

data class DepartureBus(
    val id: Int,
    val secondsTillArrival: Long,
) {
    private fun formatTime(): String {
        val minutes = secondsTillArrival / (60 * 1000)
        val seconds = (secondsTillArrival / 1000) % 60
        return String.format("%dm:%02ds", minutes, seconds)
    }

    @Override
    override fun toString(): String {
        return "Bus $id is arriving in ${formatTime()}"
    }
}
