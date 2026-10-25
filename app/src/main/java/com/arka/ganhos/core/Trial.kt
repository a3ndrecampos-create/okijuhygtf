package com.arka.ganhos.core

const val TRIAL_DAYS = 60
private const val DAY_MS = 24L * 60 * 60 * 1000

/** Dias que ainda restam do teste grátis (0 = acabou). [start] e [now] em milissegundos. */
fun trialDaysLeft(start: Long, now: Long, totalDays: Int = TRIAL_DAYS): Int {
    val elapsedDays = ((now - start).coerceAtLeast(0L) / DAY_MS).toInt()
    return (totalDays - elapsedDays).coerceAtLeast(0)
}
