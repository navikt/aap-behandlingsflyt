package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryOvergangUføreRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class VurderBistandsbehovStegTest {
    private val behandlingRepository = InMemoryBehandlingRepository

    @Test
    fun `Gjeldende vurdering er ikke god nok dersom den ikke dekker hele rettighetsperioden`() {
        val søknadsdato = 1 januar 2020
        
        val bistandMock: BistandRepository = mockk(relaxed = true) {
            every { hentHvisEksisterer(any()) } returns BistandGrunnlag(
                vurderinger = listOf(
                    Bistandsvurdering(
                        vurdertIBehandling = BehandlingId(1),
                        begrunnelse = "Begrunnelse",
                        erBehovForAktivBehandling = true,
                        erBehovForArbeidsrettetTiltak = true,
                        erBehovForAnnenOppfølging = false,
                        vurderingenGjelderFra = søknadsdato.plusDays(10),
                        vurdertAv = "Z00000",
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                        opprettet = Instant.now(),
                        tom = null
                    )
                )
            )
        }

        val sak = opprettInMemorySak(søknadsdato)
        val behandling = behandling(sak, typeBehandling = TypeBehandling.Førstegangsbehandling)
        val kontekstMedPerioder = flytKontekstMedPerioder(sak, behandling)


        val steg = VurderBistandsbehovSteg(
            unleashGateway = AlleAvskruddUnleash,
            bistandRepository = bistandMock,
            sykdomsRepository = mockk {
                every { hentHvisEksisterer(any()) } returns SykdomGrunnlag(
                    yrkesskadevurdering = null,
                    sykdomsvurderinger = listOf(
                        Sykdomsvurdering(
                            begrunnelse = "",
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                            yrkesskadeBegrunnelse = null,
                            vurderingenGjelderFra = søknadsdato,
                            vurderingenGjelderTil = null,
                            vurdertAv = Bruker("Z00000"),
                            opprettet = Instant.now(),
                            vurdertIBehandling = behandling.id,
                            diagnose = null
                        )
                    )
                )
            },
            vilkårsresultatRepository = InMemoryVilkårsresultatRepository,
            overgangUføreRepository = InMemoryOvergangUføreRepository,
            tidligereVurderinger = FakeTidligereVurderinger(),
            avklaringsbehovService = AvklaringsbehovService(inMemoryRepositoryProvider),
        )

        opprettOgLøsBistandsbehov(behandling)
        var behov = hentBistandsbehov(behandling)
        assertThat(behov!!.erÅpent()).isFalse
        steg.utfør(kontekstMedPerioder)
        behov = hentBistandsbehov(behandling)
        assertThat(behov!!.erÅpent()).isTrue
    }

    private fun opprettOgLøsBistandsbehov(behandling: Behandling) {
        InMemoryAvklaringsbehovRepository.opprett(
            behandling.id,
            Definisjon.AVKLAR_BISTANDSBEHOV,
            Definisjon.AVKLAR_BISTANDSBEHOV.løsesISteg,
            null,
            "..."
        )
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .løsAvklaringsbehov(Definisjon.AVKLAR_BISTANDSBEHOV, "...", "meg")
    }

    private fun hentBistandsbehov(behandling: Behandling): Avklaringsbehov? =
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .hentBehovForDefinisjon(Definisjon.AVKLAR_BISTANDSBEHOV)


    private fun flytKontekstMedPerioder(
        sak: Sak,
        behandling: Behandling
    ): FlytKontekstMedPerioder = no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder {
        this.behandling = behandling
        this.rettighetsperiode = sak.rettighetsperiode
    }

    private fun behandling(sak: Sak, typeBehandling: TypeBehandling): Behandling =
        behandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = typeBehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            )
        )
}