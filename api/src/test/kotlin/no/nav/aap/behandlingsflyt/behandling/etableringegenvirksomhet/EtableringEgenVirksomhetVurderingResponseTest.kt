package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.tilNyttFormat
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.test.FakeAnsattInfoGateway
import no.nav.aap.behandlingsflyt.test.FakeEnhetGateway
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class EtableringEgenVirksomhetVurderingResponseTest {
    @Test
    fun `beholder perioder i API for gamle og migrerte vurderinger`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val periode = Periode(LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 6))
        val vurdertAvService = VurdertAvService(
            InMemoryAvklaringsbehovRepository,
            AnsattInfoService(FakeAnsattInfoGateway(), FakeEnhetGateway()),
        )

        EtableringFase.entries.forEach { fase ->
            val gammel = EtableringEgenVirksomhetVurdering(
                begrunnelse = "Historisk vurdering",
                virksomhetNavn = "Virksomhet",
                foreliggerFagligVurdering = true,
                virksomhetErNy = true,
                brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                kanFøreTilSelvforsørget = true,
                utviklingsPerioder = if (fase == EtableringFase.UTVIKLING) listOf(periode) else emptyList(),
                oppstartsPerioder = if (fase == EtableringFase.OPPSTART) listOf(periode) else emptyList(),
                vurdertAv = Bruker("Z123456"),
                opprettet = Instant.parse("2026-01-01T00:00:00Z"),
                vurdertIBehandling = behandling.id,
                fom = periode.fom,
                tom = periode.tom,
                fase = null,
                erRegistrertINødvendigeOffentligeRegister = null,
                jobberBrukerAktivMedVirksomheten = null,
            )
            val migrert = listOf(gammel).tilNyttFormat().single()
            val forventet = EtableringEgenVirksomhetVurderingResponse.fraDomene(gammel, vurdertAvService)
            assertThat(forventet.utviklingsPeriode).isEqualTo(gammel.utviklingsPerioder)
            assertThat(forventet.oppstartsPeriode).isEqualTo(gammel.oppstartsPerioder)

            assertThat(EtableringEgenVirksomhetVurderingResponse.fraDomene(migrert, vurdertAvService))
                .usingRecursiveComparison().isEqualTo(forventet.copy(fase = fase))
            assertThat(
                EtableringEgenVirksomhetVurderingResponse.fraDomene(
                    tidslinjeOf(periode to migrert), vurdertAvService
                )
            ).usingRecursiveComparison().isEqualTo(listOf(forventet.copy(fase = fase)))
            assertThat(migrert.utviklingsPerioder).isEmpty()
            assertThat(migrert.oppstartsPerioder).isEmpty()
        }
    }
}
