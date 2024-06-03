package no.nav.aap.motor

import no.nav.aap.motor.retry.RekjørFeiledeJobb

object JobbType {
    private val jobber = HashMap<String, Jobb>()

    init {
        jobber[RekjørFeiledeJobb.type()] = RekjørFeiledeJobb
    }

    fun leggTil(jobb: Jobb) {
        jobber[jobb.type()] = jobb
    }

    fun parse(type: String): Jobb {
        return jobber.getValue(type)
    }

    fun cronTypes(): List<String> {
        return jobber.filter { it.value.cron() != null }.keys.toList()
    }

}
