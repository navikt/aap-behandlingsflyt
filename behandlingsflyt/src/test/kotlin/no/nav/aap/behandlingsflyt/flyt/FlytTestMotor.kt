package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.testutil.ManuellMotorImpl
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import javax.sql.DataSource

/**
 * [ManuellMotorImpl] plukker jobber med `neste_kjoring < LocalDateTime.now()`. Stilles veggklokka tilbake (WSL2 gjør
 * det med ~20 ms omtrent hvert 30. sekund), ser en jobb som nettopp ble planlagt ut til å ligge i fremtiden. Da kjøres
 * den ikke, og testen feiler et par steg senere fordi et avklaringsbehov mangler. Denne kjører motoren på nytt så lenge
 * det finnes klare jobber planlagt innenfor 100 ms fra nå, i begge retninger (klokka kan ha tatt seg inn igjen før vi
 * sjekker). Jobber som er utsatt med vilje (f.eks. til kl. 19) ligger mye lenger frem og berøres ikke. En jobb som står
 * fast bak en feilet jobb gir høyst 100 ms venting før den faller utenfor vinduet.
 */
class FlytTestMotor(private val dataSource: DataSource, private val motor: ManuellMotorImpl) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun kjørJobber() {
        motor.kjørJobber()
        repeat(MAKS_NYE_FORSØK) {
            if (!harJobbPlanlagtAkkuratNå()) return
            log.info("Fant klar jobb planlagt innenfor 100 ms (klokka er trolig stilt tilbake), kjører motoren på nytt")
            Thread.sleep(10)
            motor.kjørJobber()
        }
    }

    private fun harJobbPlanlagtAkkuratNå(): Boolean {
        val nå = LocalDateTime.now()
        return dataSource.transaction(readOnly = true) { connection ->
            connection.queryFirst(
                "SELECT EXISTS (SELECT 1 FROM jobb WHERE status = 'KLAR' AND neste_kjoring > ? AND neste_kjoring < ?) AS finnes"
            ) {
                setParams {
                    setLocalDateTime(1, nå.minus(VINDU))
                    setLocalDateTime(2, nå.plus(VINDU))
                }
                setRowMapper { it.getBoolean("finnes") }
            }
        }
    }

    private companion object {
        const val MAKS_NYE_FORSØK = 20
        val VINDU: Duration = Duration.ofMillis(100)
    }
}
