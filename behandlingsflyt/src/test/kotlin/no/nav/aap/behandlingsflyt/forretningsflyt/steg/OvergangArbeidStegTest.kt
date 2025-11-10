package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.Random
import java.util.UUID

class OvergangArbeidStegTest {
    private val random = Random(1235123)

    private val sakRepository = InMemorySakRepository
    private val behandlingRepository = InMemoryBehandlingRepository

    @Test
    fun `Overgang arbeid skal vurderes ved forutgående ordinær aap`() {
        val rettighetsperiode = Periode(
            1 januar 2020,
            1 januar 2021,
        )
        val person = person()
        val sak = sak(person, rettighetsperiode)
        val behandling = behandling(sak, typeBehandling = TypeBehandling.Førstegangsbehandling)
        val kontekstMedPerioder = flytKontekstMedPerioder(sak, behandling, VurderingType.FØRSTEGANGSBEHANDLING)


        val bistandMock: BistandRepository = mockk(relaxed = true) {
            every { hentHvisEksisterer(any()) } returns BistandGrunnlag(
                vurderinger = listOf(bistand(erBehov = true, vurderingenGjelderFra = rettighetsperiode.fom))
            )
        }
        val sykdomMock: SykdomRepository = mockk(relaxed = true) {
            every { hentHvisEksisterer(any()) } returns SykdomGrunnlag(
                yrkesskadevurdering = null,
                sykdomsvurderinger = listOf(
                    sykdom(erSyk = true, vurderingenGjelderFra = rettighetsperiode.fom),
                    sykdom(erSyk = false, vurderingenGjelderFra = rettighetsperiode.fom.plusMonths(2))
                )
            )
        }

        val steg = OvergangArbeidSteg(
            vilkårsresultatRepository = InMemoryVilkårsresultatRepository,
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            overgangArbeidRepository = mockk<OvergangArbeidRepository> {
                every { hentHvisEksisterer(any()) } returns null
            },
            sykdomRepository = sykdomMock,
            tidligereVurderinger = FakeTidligereVurderinger(),
            bistandRepository = bistandMock,
            behandlingRepository = behandlingRepository,
            avklaringsbehovService = AvklaringsbehovService(inMemoryRepositoryProvider),
            studentRepository = mockk<StudentRepository> {
                every { hentHvisEksisterer(any()) } returns null
            },
            overgangUføreRepository = mockk<OvergangUføreRepository> {
                every { hentHvisEksisterer(any()) } returns null
            },
            unleashGateway = mockk<UnleashGateway> {
                every { isDisabled(BehandlingsflytFeature.OvergangArbeid) } returns false
            }
        )
        
        steg.utfør(kontekstMedPerioder)
        val behov = hentOvergangArbeidBehov(behandling)
        assertThat(behov).isNotNull
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

    private fun hentOvergangArbeidBehov(behandling: Behandling): Avklaringsbehov? =
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .hentBehovForDefinisjon(Definisjon.AVKLAR_OVERGANG_ARBEID)


    private fun flytKontekstMedPerioder(
        sak: Sak,
        behandling: Behandling,
        vurderingType: VurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
        vurderingsbehov: Set<Vurderingsbehov> = setOf(Vurderingsbehov.MOTTATT_SØKNAD)
    ): FlytKontekstMedPerioder = FlytKontekstMedPerioder(
        sak.id, behandling.id, behandling.forrigeBehandlingId, behandling.typeBehandling(),
        vurderingType = vurderingType,
        vurderingsbehovRelevanteForSteg = vurderingsbehov,
        rettighetsperiode = sak.rettighetsperiode
    )

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

    private fun sak(person: Person, periode: Periode): Sak =
        sakRepository.finnEllerOpprett(person, periode)

    private fun person(): Person =
        Person(PersonId(random.nextLong()), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

    private fun bistand(
        vurderingenGjelderFra: LocalDate,
        erBehov: Boolean
    ) = BistandVurdering(
        begrunnelse = "Begrunnelse",
        erBehovForAktivBehandling = erBehov,
        erBehovForArbeidsrettetTiltak = erBehov,
        erBehovForAnnenOppfølging = false,
        vurderingenGjelderFra = vurderingenGjelderFra,
        vurdertAv = "Z00000",
        skalVurdereAapIOvergangTilUføre = null,
        skalVurdereAapIOvergangTilArbeid = null,
        overgangBegrunnelse = null,
    )

    private fun sykdom(erSyk: Boolean, vurderingenGjelderFra: LocalDate) = Sykdomsvurdering(
        begrunnelse = "",
        dokumenterBruktIVurdering = emptyList(),
        harSkadeSykdomEllerLyte = erSyk,
        erSkadeSykdomEllerLyteVesentligdel = erSyk,
        erNedsettelseIArbeidsevneMerEnnHalvparten = erSyk,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
        erArbeidsevnenNedsatt = true,
        yrkesskadeBegrunnelse = null,
        erNedsettelseIArbeidsevneAvEnVissVarighet = true,
        vurderingenGjelderFra = vurderingenGjelderFra,
        vurderingenGjelderTil = null,
        vurdertAv = Bruker("Z00000"),
        opprettet = Instant.now(),
        vurdertIBehandling = BehandlingId(1L),
    )
}
