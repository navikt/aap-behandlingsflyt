package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.komponenter.dbtest.TestDataSource
import org.slf4j.LoggerFactory

/**
 * Singleton som deler én TestDataSource (og dermed én Postgres testcontainer)
 * på tvers av alle test-klasser som arver fra AbstraktFlytOrkestratorTest.
 *
 * Dette unngår at hver @ParameterizedClass-variant (toggles on/off) starter
 * sin egen container.
 */
object SharedTestDataSource {
    private val log = LoggerFactory.getLogger(javaClass)
    val instance: TestDataSource by lazy {
        log.info("Initialiserer datasource")
        TestDataSource()
    }
}

