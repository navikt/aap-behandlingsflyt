package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMeldeperiodeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class FastsettMeldeperiodeStegTest {
    private val behandlingId = BehandlingId(Random().nextLong())

    @Test
    fun `samme fastsatt dag ved endring av rettighetsperioden`() {
        val steg = FastsettMeldeperiodeSteg(
            meldeperiodeRepository = InMemoryMeldeperiodeRepository,
            tidligereVurderinger = FakeTidligereVurderinger(),
            sakRepository = InMemorySakRepository,
        )


        var aktuellPeriode = Periode(10 mars 2025, 10 april 2025)
        steg.oppdaterFørsteMeldeperiode(behandlingId, aktuellPeriode)
        InMemoryMeldeperiodeRepository.hentMeldeperioder(behandlingId, aktuellPeriode).also {
            assertEquals(
                listOf(
                    Periode(10 mars 2025, 23 mars 2025),
                    Periode(24 mars 2025, 6 april 2025),
                    Periode(7 april 2025, 20 april 2025),
                ), it
            )
        }

        /* flytt "søknadstidspunktet" bakover */
        aktuellPeriode = Periode(
            aktuellPeriode.fom.minusDays(7),
            aktuellPeriode.tom,
        )
        steg.oppdaterFørsteMeldeperiode(behandlingId, aktuellPeriode)
        InMemoryMeldeperiodeRepository.hentMeldeperioder(behandlingId, aktuellPeriode).also {
            assertEquals(
                listOf(
                    Periode(24 februar 2025, 9 mars 2025),
                    Periode(10 mars 2025, 23 mars 2025),
                    Periode(24 mars 2025, 6 april 2025),
                    Periode(7 april 2025, 20 april 2025),
                ), it
            )
        }

        /* utvid med en meldeperiode */
        aktuellPeriode = Periode(
            aktuellPeriode.fom,
            aktuellPeriode.tom.plusDays(14),
        )
        steg.oppdaterFørsteMeldeperiode(behandlingId, aktuellPeriode)
        InMemoryMeldeperiodeRepository.hentMeldeperioder(behandlingId, aktuellPeriode).also {
            assertEquals(
                listOf(
                    Periode(24 februar 2025, 9 mars 2025),
                    Periode(10 mars 2025, 23 mars 2025),
                    Periode(24 mars 2025, 6 april 2025),
                    Periode(7 april 2025, 20 april 2025),
                    Periode(21 april 2025, 4 mai 2025),
                ), it
            )
        }
    }

    @Test
    fun `skal ikke oppdatere perioden dersom den har samme startdato`() {
        val steg = FastsettMeldeperiodeSteg(
            meldeperiodeRepository = InMemoryMeldeperiodeRepository,
            tidligereVurderinger = FakeTidligereVurderinger(),
            sakRepository = InMemorySakRepository,
        )

        val startMeldeperiode = LocalDate.of(2024, 1, 1)
        val behandlingId = BehandlingId(Random().nextLong())
        val meldeperiodeFraStart = Periode(startMeldeperiode, startMeldeperiode)
        val nyMeldeperiodeMedUlikSluttdato = Periode(startMeldeperiode, startMeldeperiode.plusDays(14))
        InMemoryMeldeperiodeRepository.lagreFørsteMeldeperiode(behandlingId, meldeperiodeFraStart)
        steg.oppdaterFørsteMeldeperiode(behandlingId, nyMeldeperiodeMedUlikSluttdato)
        val persistertMeldeperiode = InMemoryMeldeperiodeRepository.hentFørsteMeldeperiode(behandlingId)
        assertThat(persistertMeldeperiode).isEqualTo(meldeperiodeFraStart)
    }
}