package no.nav.aap.barnetillegg

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovValidering
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvbrytRevurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBarnRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBarnetilleggRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class BarnetilleggStegTest {
    private val barnRepository: BarnRepository = InMemoryBarnRepository

    @Test
    fun `vedtakBehøverVurdering returnerer false for meldekort`() {
        val kontekst = opprettKontekst(
            vurderingType = VurderingType.MELDEKORT,
            vurderingsbehov = listOf(Vurderingsbehov.BARNETILLEGG)
        )

        assertFalse(steg().vedtakBehøverVurdering(kontekst))
    }

    @Test
    fun `vedtakBehøverVurdering returnerer true ved førstegangsbehandling med dødsfall barn`() {
        val kontekst = opprettKontekst(
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            vurderingsbehov = listOf(Vurderingsbehov.DØDSFALL_BARN)
        )

        assertTrue(steg().vedtakBehøverVurdering(kontekst))
    }

    @Test
    fun `vedtakBehøverVurdering returnerer true ved revurdering med barnetillegg vurderingsbehov`() {
        val kontekst = opprettKontekst(
            vurderingType = VurderingType.REVURDERING,
            vurderingsbehov = listOf(Vurderingsbehov.BARNETILLEGG)
        )

        assertTrue(steg().vedtakBehøverVurdering(kontekst))
    }

    @Test
    fun `vedtakBehøverVurdering returnerer true når barn er oppgitt`() {
        val kontekst = opprettKontekst(
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            vurderingsbehov = listOf(Vurderingsbehov.HELHETLIG_VURDERING)
        )

        barnRepository.lagreOppgitteBarn(
            kontekst.behandlingId, OppgitteBarn(
                null, listOf(
                    OppgitteBarn.OppgittBarn(
                        Ident("08427832180"), "Mille Larsen",
                        Fødselsdato(LocalDate.now().minusYears(3)), Relasjon.FORELDER
                    )
                )
            )
        )

        assertTrue(steg().vedtakBehøverVurdering(kontekst))
    }

    @Test
    fun `vedtakBehøverVurdering returnerer false når ikke mulig med rett til AAP`() {
        val kontekst = opprettKontekst(
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            vurderingsbehov = listOf(Vurderingsbehov.BARNETILLEGG)
        )
        val tidligereVurderinger = FakeTidligereVurderinger().apply {
            avslagEllerIngenBehandlingsgrunnlag = true
        }

        val steg = steg(tidligereVurderinger)

        assertFalse(steg.vedtakBehøverVurdering(kontekst))
    }

    private fun steg(tidligereVurderinger: TidligereVurderinger = FakeTidligereVurderinger()): BarnetilleggSteg {
        val avklaringsbehovRepository: AvklaringsbehovRepository = InMemoryAvklaringsbehovRepository
        val vilkårsresultatRepository: VilkårsresultatRepository = InMemoryVilkårsresultatRepository
        val behandlingRepository: BehandlingRepository = InMemoryBehandlingRepository
        val barnRepository: BarnRepository = InMemoryBarnRepository
        val avbrytRevurderingService = AvbrytRevurderingService(InMemoryAvbrytRevurderingRepository)
        val trukketSøknadService = TrukketSøknadService(InMemoryTrukketSøknadRepository)
        val kravRepository: KravRepository = InMemoryKravRepository
        val sakRepository: SakRepository = InMemorySakRepository
        val avklaringsbehovValidering = AvklaringsbehovValidering(
            inMemoryRepositoryProvider,
            minimalGatewayProvider()
        )

        return BarnetilleggSteg(
            barnetilleggService = BarnetilleggService(barnRepository, SakService(sakRepository, behandlingRepository)),
            barnetilleggRepository = InMemoryBarnetilleggRepository,
            barnRepository = barnRepository,
            tidligereVurderinger = tidligereVurderinger,
            avklaringsbehovService = AvklaringsbehovService(
                avbrytRevurderingService,
                avklaringsbehovRepository,
                behandlingRepository,
                vilkårsresultatRepository,
                trukketSøknadService,
                kravRepository,
                sakRepository,
                AlleAvskruddUnleash,
                avklaringsbehovValidering
            )
        )
    }

    private fun opprettKontekst(
        vurderingType: VurderingType,
        vurderingsbehov: List<Vurderingsbehov>
    ): FlytKontekstMedPerioder {
        val behandlingId = BehandlingId(Random().nextLong())
        return flytKontekstMedPerioder {
            this.behandlingId = behandlingId
            this.vurderingType = vurderingType
            this.rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(6))
            this.vurderingsbehovRelevanteForSteg = vurderingsbehov.toSet()
        }
    }
}