package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertRettighetstype
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import org.assertj.core.api.Assertions.assertThat
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test

class KvoteServiceTest {

    private val behandlingRepository = InMemoryBehandlingRepository
    private val kravRepository = InMemoryKravRepository

    private val kvoteService = KvoteService(inMemoryRepositoryProvider, minimalGatewayProvider { })

    @Test
    fun `uten migrert krav-vurdering returneres standardkvoter`() {
        val behandling = opprettBehandling()

        val kvoter = kvoteService.gjeldendeKvoter(behandling.id)

        assertThat(kvoter).isEqualTo(KvoteService.standardKvoter)
    }

    @Test
    fun `med migrert krav-vurdering overstyrer resterendeKvoteOrdinaer ordinærkvoten`() {
        val behandling = opprettBehandling()
        val resterendeKvoteOrdinaer = 42
        kravRepository.lagre(behandling.id, setOf(lagMigrertKrav(behandling.id, resterendeKvoteOrdinaer)))

        val kvoter = kvoteService.gjeldendeKvoter(behandling.id)

        assertThat(kvoter.ordinærkvote).isEqualTo(Hverdager(resterendeKvoteOrdinaer))
        assertThat(kvoter.sykepengeerstatningkvote).isEqualTo(KvoteService.standardKvoter.sykepengeerstatningkvote)
    }

    private fun opprettBehandling() =
        InMemoryBehandlingRepository.opprettBehandling(
            sakId = opprettInMemorySak().id,
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
            ),
        )

    private fun lagMigrertKrav(behandlingId: BehandlingId, resterendeKvoteOrdinaer: Int) =
        MigrertKrav(
            referanse = Kravreferanse.ny(),
            vurdertAv = SYSTEMBRUKER,
            begrunnelse = "Migrert fra Arena",
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
            virkningstidspunktArena = LocalDate.of(2020, 1, 1),
            muligRettFra = LocalDate.of(2020, 1, 1),
            arenaSaksnummer = "ARENA-4711",
            rettighetstype = MigrertRettighetstype.ORDINÆR,
            resterendeKvoteOrdinær = resterendeKvoteOrdinaer,
        )
}
