package no.nav.aap.behandlingsflyt.integrasjon.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Opprettholdes
import no.nav.aap.behandlingsflyt.integrasjon.kabal.KabalGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.util.*

@Fakes
class KabalGatewayTest {
    private val random = Random(1235123)

    @Test
    fun kanOversendeTilKabal() {
        val person = Person(random.nextLong(), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

        assertDoesNotThrow {
            KabalGateway().oversendTilAndreinstans(
                klagenGjelder = person,
                behandlingsreferanse = BehandlingReferanse(UUID.randomUUID()),
                saksbehandlersEnhet = "0301",
                kravDato = LocalDate.now(),
                saksnummer = Saksnummer("1"),
                klageresultat = Opprettholdes(
                    vilkårSomSkalOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5)
                )
            )
        }

    }
}