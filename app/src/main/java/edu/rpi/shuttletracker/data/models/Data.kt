package edu.rpi.shuttletracker.data.models

data class Data<T>(
    // It's a generic wrap class for scalar type T
    var value: T,
)
