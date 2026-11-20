package io.github.arcm111.dhikra.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSoundTest {

    @Test
    fun keysRoundTrip() {
        val sounds = listOf(
            ReminderSound.Default,
            ReminderSound.Silent,
            ReminderSound.Azan,
            ReminderSound.Device("content://media/internal/audio/media/42?title=Chime|x"),
        )
        sounds.forEach { assertEquals(it, ReminderSound.fromKey(it.key)) }
    }

    @Test
    fun unknownOrMissingIsTheDefault() {
        assertEquals(ReminderSound.Default, ReminderSound.fromKey(null))
        assertEquals(ReminderSound.Default, ReminderSound.fromKey("bells"))
        assertEquals(ReminderSound.Default, ReminderSound.fromKey("device:"))
    }

    @Test
    fun prayerAlertsRoundTrip() {
        val alerts = listOf(
            PrayerAlert(),
            PrayerAlert(enabled = true, sound = ReminderSound.Azan),
            PrayerAlert(enabled = false, sound = ReminderSound.Device("content://media/1|2")),
        )
        alerts.forEach { assertEquals(it, PrayerAlert.decode(it.encode())) }
    }

    @Test
    fun aMissingAlertIsOffWithTheDefaultSound() {
        assertEquals(PrayerAlert(enabled = false, sound = ReminderSound.Default), PrayerAlert.decode(null))
        assertEquals(PrayerAlert(enabled = true, sound = ReminderSound.Default), PrayerAlert.decode("on"))
    }
}
