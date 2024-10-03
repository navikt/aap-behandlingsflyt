package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.verdityper.sakogbehandling.SakId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class MeldeperiodeTest {
    @Test
    fun `meldeperiode starter fra rettighetsperioden`() {
        val meldeperiode = Meldeperiode.forRettighetsperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1))
        assertEquals(Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 14)), meldeperiode.asPeriode)
    }
}