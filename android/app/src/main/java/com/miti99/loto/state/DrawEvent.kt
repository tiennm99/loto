package com.miti99.loto.state

/** A single number-drawn event broadcast from MasterPanel to PlayerBoard. */
data class DrawEvent(val num: Int, val at: Long, val id: Long)
