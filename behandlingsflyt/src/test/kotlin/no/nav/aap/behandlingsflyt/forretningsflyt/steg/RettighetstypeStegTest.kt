package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plusHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.genererVilkårsresultat
import no.nav.aap.behandlingsflyt.help.inmemory.genererPerson
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryRettighetstypeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class RettighetstypeStegTest {
    private val sakRepository = InMemorySakRepository
    private val behandlingRepository = InMemoryBehandlingRepository
    private val rettighetstypeRepository = InMemoryRettighetstypeRepository
    private val vilkårsresultatRepository = InMemoryVilkårsresultatRepository

    private val kvoteService = KvoteService()
    private val steg = RettighetstypeSteg(
        rettighetstypeRepository = rettighetstypeRepository,
        vilkårsresultatRepository = vilkårsresultatRepository,
        kvoteService = kvoteService,
    )

    @Test
    fun `Skal utlede og lagre ned riktig rettighetstype`() {
        val rettighetsperiode = Periode(1 januar 2020, Tid.MAKS)
        val person = genererPerson(rettighetsperiode.fom.minusYears(25))

        val sak = sakRepository.finnEllerOpprett(person, rettighetsperiode)
        val behandling = behandlingRepository.opprettBehandling(
            sak.id,
            TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            )
        )
        val behandlingId = behandling.id

        vilkårsresultatRepository.lagre(behandlingId, genererVilkårsresultat(rettighetsperiode))

        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            this.rettighetsperiode = rettighetsperiode
        }

        steg.utfør(kontekst)

        val forventetStartDatoOrdinær = rettighetsperiode.fom
        val forventetSluttdatoOrdinær =
            forventetStartDatoOrdinær.plusHverdager(kvoteService.beregn().ordinærkvote).minusDays(1)

        assertTidslinje(
            rettighetstypeRepository.hent(behandlingId).rettighetstypeTidslinje,
            Periode(forventetStartDatoOrdinær, forventetSluttdatoOrdinær) to {
                assertThat(it).isEqualTo(RettighetsType.BISTANDSBEHOV)
            }
        )

    }

}