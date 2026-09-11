package no.nav.aap.behandlingsflyt.behandling.vilkår

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Grunnlag
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Repository
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Vurdering
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.komponenter.tidslinje.Tidslinje
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId

class TidligereVurderingerImplTest {

    private val trukketSøknadService = mockk<TrukketSøknadService>()
    private val vilkårsresultatRepository = mockk<VilkårsresultatRepository>()
    private val vilkårService = VilkårService(vilkårsresultatRepository)
    private val avbrytRevurderingService = mockk<AvbrytRevurderingService>()
    private val sykdomRepository = mockk<SykdomRepository>()
    private val bistandRepository = mockk<BistandRepository>()
    private val avslag11_27repository = mockk<Avslag11_27Repository>()
    private val kravRepository = mockk<KravRepository>()
    private val unleashGateway = mockk<UnleashGateway>()

    private val søknadsdato: LocalDate = LocalDate.of(2026, 1, 1)
    private val rettighetsperiode = Periode(søknadsdato, søknadsdato.plusYears(3))
    private val behandlingId = BehandlingId(1)
    private val sakId = SakId(1)

    private lateinit var tidligereVurderinger: TidligereVurderingerImpl

    @BeforeEach
    fun setup() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.Avslag11_27) } returns false
        every { avbrytRevurderingService.revurderingErAvbrutt(any()) } returns false
        every { trukketSøknadService.søknadErTrukket(any()) } returns false
        every { bistandRepository.hentHvisEksisterer(any()) } returns null
        every { avslag11_27repository.hentHvisEksisterer(any()) } returns null
        every { kravRepository.hentHvisEksisterer(any()) } returns null
    }

    private fun opprettTidligereVurderinger(): TidligereVurderingerImpl = TidligereVurderingerImpl(
        trukketSøknadService = trukketSøknadService,
        vilkårsresultatRepository = vilkårsresultatRepository,
        avbrytRevurderingService = avbrytRevurderingService,
        sykdomRepository = sykdomRepository,
        bistandRepository = bistandRepository,
        avslag11_27repository = avslag11_27repository,
        kravRepository = kravRepository,
        unleashGateway = unleashGateway,
    )

    @Test
    fun `AVBRYT_REVURDERING gir IkkeBehandlingsgrunnlag hvis revurdering er avbrutt`() {
        every { avbrytRevurderingService.revurderingErAvbrutt(behandlingId) } returns true
        every { vilkårsresultatRepository.hent(behandlingId) } returns tomtVilkårsresultat()

        val tidligereVurderinger = opprettTidligereVurderinger()
        val resultat = tidligereVurderinger.behandlingsutfall(kontekst(), StegType.VURDER_ALDER)

        assertThat(resultat.segmenter()).allSatisfy {
            assertThat(it.verdi).isEqualTo(TidligereVurderinger.IkkeBehandlingsgrunnlag)
        }
    }

    @Test
    fun `SØKNAD gir IkkeBehandlingsgrunnlag hvis søknad er trukket`() {
        every { trukketSøknadService.søknadErTrukket(behandlingId) } returns true
        every { vilkårsresultatRepository.hent(behandlingId) } returns tomtVilkårsresultat()

        val tidligereVurderinger = opprettTidligereVurderinger()
        val resultat = tidligereVurderinger.behandlingsutfall(kontekst(), StegType.VURDER_ALDER)

        assertThat(resultat.segmenter()).allSatisfy {
            assertThat(it.verdi).isEqualTo(TidligereVurderinger.IkkeBehandlingsgrunnlag)
        }
    }

    @Test
    fun `VURDER_ALDER gir UunngåeligAvslag hvis aldersvilkåret ikke er oppfylt`() {
        val resultatEtterVurdering = vilkårMedPeriode(
            Vilkårtype.ALDERSVILKÅRET,
            rettighetsperiode,
            Utfall.IKKE_OPPFYLT,
            Avslagsårsak.BRUKER_OVER_67
        )
        every { vilkårsresultatRepository.hent(behandlingId) } returns resultatEtterVurdering

        val tidligereVurderinger = opprettTidligereVurderinger()
        val resultat = tidligereVurderinger.behandlingsutfall(kontekst(), StegType.AVKLAR_SYKDOM)

        assertThat(resultat.segmenter()).allSatisfy {
            assertThat(it.verdi).isInstanceOf(TidligereVurderinger.UunngåeligAvslag::class.java)
            assertThat((it.verdi as TidligereVurderinger.UunngåeligAvslag).vilkårtype).isEqualTo(Vilkårtype.ALDERSVILKÅRET)
        }
    }

    @Test
    fun `AVKLAR_SYKDOM gir UunngåeligAvslag når sykdom ikke er oppfylt og ingen tidligere innvilget vurdering`() {
        every { vilkårsresultatRepository.hent(behandlingId) } returns tomtVilkårsresultat()
        every { sykdomRepository.hentHvisEksisterer(behandlingId) } returns SykdomGrunnlag(
            yrkesskadevurdering = null,
            sykdomsvurderinger = listOf(sykdomsvurdering(søknadsdato, erOppfylt = false)),
        )

        val tidligereVurderinger = opprettTidligereVurderinger()
        val resultat = tidligereVurderinger.behandlingsutfall(kontekst(), StegType.VURDER_BISTANDSBEHOV)

        assertThat(resultat.segmenter()).allSatisfy {
            assertThat(it.verdi).isInstanceOf(TidligereVurderinger.UunngåeligAvslag::class.java)
            assertThat((it.verdi as TidligereVurderinger.UunngåeligAvslag).vilkårtype).isEqualTo(Vilkårtype.SYKDOMSVILKÅRET)
        }
    }

    @Test
    fun `AVKLAR_SYKDOM gir ikke avslag hvis det er potensielt oppfylt overgang arbeid pga tidligere innvilget sykdomsvurdering`() {
        every { vilkårsresultatRepository.hent(behandlingId) } returns tomtVilkårsresultat()

        val overgang = søknadsdato.plusMonths(6)
        every { sykdomRepository.hentHvisEksisterer(behandlingId) } returns SykdomGrunnlag(
            yrkesskadevurdering = null,
            sykdomsvurderinger = listOf(
                sykdomsvurdering(søknadsdato, overgang.minusDays(1), erOppfylt = true),
                sykdomsvurdering(overgang, erOppfylt = false),
            ),
        )

        val tidligereVurderinger = opprettTidligereVurderinger()
        val resultat = tidligereVurderinger.behandlingsutfall(kontekst(), StegType.VURDER_BISTANDSBEHOV)

        assertThat(resultat.segmenter()).noneMatch { it.verdi is TidligereVurderinger.UunngåeligAvslag }
    }

    @Test
    fun `OVERGANG_UFORE gir PotensieltOppfylt med VURDERES_FOR_UFORETRYGD hvis vilkåret er oppfylt`() {
        every { vilkårsresultatRepository.hent(behandlingId) } returns vilkårMedPeriode(
            Vilkårtype.OVERGANGUFØREVILKÅRET,
            rettighetsperiode,
            Utfall.OPPFYLT,
        )
        every { sykdomRepository.hentHvisEksisterer(behandlingId) } returns null

        val tidligereVurderinger = opprettTidligereVurderinger()
        val resultat = tidligereVurderinger.behandlingsutfall(kontekst(), StegType.OVERGANG_ARBEID)

        assertThat(resultat.segmenter()).allSatisfy {
            val utfall = it.verdi as TidligereVurderinger.PotensieltOppfylt
            assertThat(utfall.rettighetstype).isEqualTo(RettighetsType.VURDERES_FOR_UFØRETRYGD)
        }
    }

    @Test
    fun `OVERGANG_UFORE gir UunngåeligAvslag når vilkåret ikke er oppfylt og ingen tidligere rettighetstype er satt`() {
        every { vilkårsresultatRepository.hent(behandlingId) } returns vilkårMedPeriode(
            Vilkårtype.OVERGANGUFØREVILKÅRET,
            rettighetsperiode,
            Utfall.IKKE_OPPFYLT,
            Avslagsårsak.VARIGHET_OVERSKREDET_OVERGANG_UFORE
        )
        every { sykdomRepository.hentHvisEksisterer(behandlingId) } returns SykdomGrunnlag(
            yrkesskadevurdering = null,
            sykdomsvurderinger = listOf(sykdomsvurdering(søknadsdato, erOppfylt = true)),
        )

        val tidligereVurderinger = opprettTidligereVurderinger()
        val resultat = tidligereVurderinger.behandlingsutfall(kontekst(), StegType.OVERGANG_ARBEID)

        assertThat(resultat.segmenter()).allSatisfy {
            assertThat(it.verdi).isInstanceOf(TidligereVurderinger.UunngåeligAvslag::class.java)
            assertThat((it.verdi as TidligereVurderinger.UunngåeligAvslag).vilkårtype).isEqualTo(Vilkårtype.OVERGANGUFØREVILKÅRET)
        }
    }

    @Test
    fun `OVERGANG_UFORE gir ikke avslag for periode etter tidligere innvilget sykdomsvurdering pga potensielt overgang arbeid`() {
        val overgang = søknadsdato.plusMonths(6)
        every { vilkårsresultatRepository.hent(behandlingId) } returns vilkårMedPeriode(
            Vilkårtype.OVERGANGUFØREVILKÅRET,
            rettighetsperiode,
            Utfall.IKKE_OPPFYLT,
            Avslagsårsak.VARIGHET_OVERSKREDET_OVERGANG_UFORE
        )
        every { sykdomRepository.hentHvisEksisterer(behandlingId) } returns SykdomGrunnlag(
            yrkesskadevurdering = null,
            sykdomsvurderinger = listOf(
                sykdomsvurdering(søknadsdato, overgang.minusDays(1), erOppfylt = true),
                sykdomsvurdering(overgang, erOppfylt = false),
            ),
        )

        val tidligereVurderinger = opprettTidligereVurderinger()
        val resultat = tidligereVurderinger.behandlingsutfall(kontekst(), StegType.OVERGANG_ARBEID)

        val segmentEtterOvergang = resultat.segmenter().first { !it.periode.fom.isBefore(overgang) }
        assertThat(segmentEtterOvergang.verdi)
            .describedAs("Perioden etter tidligere innvilget sykdomsvurdering skal ikke gi UunngåeligAvslag")
            .isNotInstanceOf(TidligereVurderinger.UunngåeligAvslag::class.java)
    }

    @Test
    fun `girAvslag returnerer true når hele perioden gir UunngåeligAvslag`() {
        every { vilkårsresultatRepository.hent(behandlingId) } returns vilkårMedPeriode(
            Vilkårtype.ALDERSVILKÅRET,
            rettighetsperiode,
            Utfall.IKKE_OPPFYLT,
            Avslagsårsak.BRUKER_OVER_67
        )

        val tidligereVurderinger = opprettTidligereVurderinger()
        assertThat(tidligereVurderinger.girAvslag(kontekst(), StegType.AVKLAR_SYKDOM)).isTrue()
    }

    @Test
    fun `girAvslag returnerer false hvis kun deler av perioden er avslag`() {
        every { vilkårsresultatRepository.hent(behandlingId) } returns vilkårMedPeriode(
            Vilkårtype.ALDERSVILKÅRET,
            Periode(søknadsdato, søknadsdato.plusMonths(1)),
            Utfall.IKKE_OPPFYLT,
            Avslagsårsak.BRUKER_OVER_67
        )

        val tidligereVurderinger = opprettTidligereVurderinger()
        assertThat(tidligereVurderinger.girAvslag(kontekst(), StegType.AVKLAR_SYKDOM)).isFalse()
    }

    @Test
    fun `girIngenBehandlingsgrunnlag returnerer true hvis søknad er trukket`() {
        every { trukketSøknadService.søknadErTrukket(behandlingId) } returns true
        every { vilkårsresultatRepository.hent(behandlingId) } returns tomtVilkårsresultat()

        val tidligereVurderinger = opprettTidligereVurderinger()
        assertThat(tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst(), StegType.VURDER_LOVVALG)).isTrue()
    }

    // -------------------------------------------------------------------------
    // Avslag § 11-27
    // -------------------------------------------------------------------------
    @Test
    fun `VURDER_AVSLAG_11_27 gir UunngåeligAvslag når krav skal avslås etter 11-27`() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.Avslag11_27) } returns true

        val referanse = Kravreferanse.ny()
        every { vilkårsresultatRepository.hent(behandlingId) } returns tomtVilkårsresultat()
        every { kravRepository.hentHvisEksisterer(behandlingId) } returns KravGrunnlag(
            vurderinger = setOf(relevantKrav(referanse, søknadsdato)),
        )
        every { avslag11_27repository.hentHvisEksisterer(behandlingId) } returns Avslag11_27Grunnlag(
            vurderinger = setOf(avslag1127Vurdering(referanse, skalAvslås = true)),
        )

        val tidligereVurderinger = opprettTidligereVurderinger()
        val resultat = tidligereVurderinger.behandlingsutfall(kontekst(), StegType.AVKLAR_SYKDOM)

        assertThat(resultat.segmenter()).allSatisfy {
            assertThat(it.verdi).isInstanceOf(TidligereVurderinger.UunngåeligAvslag::class.java)
            assertThat((it.verdi as TidligereVurderinger.UunngåeligAvslag).vilkårtype).isEqualTo(Vilkårtype.SAMORDNING)
        }
    }

    @Test
    fun `VURDER_AVSLAG_11_27 gir ikke avslag når krav ikke skal avslås etter 11-27`() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.Avslag11_27) } returns true

        val referanse = Kravreferanse.ny()
        every { vilkårsresultatRepository.hent(behandlingId) } returns tomtVilkårsresultat()
        every { kravRepository.hentHvisEksisterer(behandlingId) } returns KravGrunnlag(
            vurderinger = setOf(relevantKrav(referanse, søknadsdato)),
        )
        every { avslag11_27repository.hentHvisEksisterer(behandlingId) } returns Avslag11_27Grunnlag(
            vurderinger = setOf(avslag1127Vurdering(referanse, skalAvslås = false)),
        )

        val tidligereVurderinger = opprettTidligereVurderinger()
        val resultat = tidligereVurderinger.behandlingsutfall(kontekst(), StegType.AVKLAR_SYKDOM)

        assertThat(resultat.segmenter()).noneMatch { it.verdi is TidligereVurderinger.UunngåeligAvslag }
    }

    @Test
    fun `VURDER_AVSLAG_11_27 gir ikke avslag hvis krav mangler grunnlag`() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.Avslag11_27) } returns true

        every { vilkårsresultatRepository.hent(behandlingId) } returns tomtVilkårsresultat()
        every { kravRepository.hentHvisEksisterer(behandlingId) } returns null
        every { avslag11_27repository.hentHvisEksisterer(behandlingId) } returns null

        val tidligereVurderinger = opprettTidligereVurderinger()
        val resultat = tidligereVurderinger.behandlingsutfall(kontekst(), StegType.AVKLAR_SYKDOM)

        assertThat(resultat.segmenter()).noneMatch { it.verdi is TidligereVurderinger.UunngåeligAvslag }
    }

    private fun kontekst(): FlytKontekstMedPerioder = FlytKontekstMedPerioder(
        sakId = sakId,
        behandlingId = behandlingId,
        forrigeBehandlingId = null,
        behandlingType = TypeBehandling.Førstegangsbehandling,
        vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
        rettighetsperiode = rettighetsperiode,
        vurderingsbehovRelevanteForStegMedPerioder = emptySet(),
    )

    private fun tomtVilkårsresultat() = Vilkårsresultat()

    private fun sykdomsvurdering(
        vurderingenGjelderFra: LocalDate,
        vurderingenGjelderTil: LocalDate? = null,
        erOppfylt: Boolean = true,
    ) = Sykdomsvurdering(
        begrunnelse = "begrunnelse",
        vurderingenGjelderFra = vurderingenGjelderFra,
        vurderingenGjelderTil = vurderingenGjelderTil,
        harSkadeSykdomEllerLyte = erOppfylt,
        erSkadeSykdomEllerLyteVesentligdel = erOppfylt,
        erNedsettelseIArbeidsevneMerEnnHalvparten = erOppfylt,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
        yrkesskadeBegrunnelse = null,
        harNedsattArbeidsevne = if (erOppfylt) ArbeidsevneNedsattValg.JA else ArbeidsevneNedsattValg.NEI,
        diagnose = null,
        vurdertAv = Bruker("Z000000"),
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
    )

    private object TestGrunnlag : Faktagrunnlag

    private fun vilkårMedPeriode(
        type: Vilkårtype,
        periode: Periode,
        utfall: Utfall,
        avslagsårsak: Avslagsårsak? = null,
    ): Vilkårsresultat {
        val vurdering = Vilkårsvurdering(
            Vilkårsperiode(
                periode = periode,
                utfall = utfall,
                manuellVurdering = true,
                begrunnelse = null,
                avslagsårsak = avslagsårsak,
            )
        )

        val vurderer = object : Vilkårsvurderer<TestGrunnlag> {
            override val vilkårtype: Vilkårtype = type
            override fun vurder(faktagrunnlag: TestGrunnlag): Tidslinje<Vilkårsvurdering> {
                return Tidslinje(periode, vurdering)
            }
        }

        val vilkårsresultatSlot = slot<Vilkårsresultat>()
        every { vilkårsresultatRepository.hent(behandlingId) } returns Vilkårsresultat()
        every { vilkårsresultatRepository.lagre(behandlingId, capture(vilkårsresultatSlot)) } just Runs

        vilkårService.vurderVilkår(behandlingId, TestGrunnlag, vurderer)

        return vilkårsresultatSlot.captured
    }

    private fun relevantKrav(
        referanse: Kravreferanse,
        muligRettFra: LocalDate,
    ) = RelevantKrav(
        referanse = referanse,
        journalpostId = JournalpostId("jp-${referanse.verdi}"),
        vurdertAv = Bruker("Z000000"),
        begrunnelse = "begrunnelse",
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        søknadsdato = Søknadsdato(muligRettFra, SøknadsdatoÅrsak.SøknadMottatt, begrunnelse = "Test"),
        overstyrMuligRettFra = null,
        muligRettFra = muligRettFra,
    )

    private fun avslag1127Vurdering(
        referanse: Kravreferanse,
        skalAvslås: Boolean,
    ) = Avslag11_27Vurdering(
        referanse = referanse,
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        vurdertAv = Bruker("Z000000"),
        begrunnelse = "begrunnelse",
        harAnnenFullYtelse = skalAvslås,
        skalAvslås1127 = skalAvslås,
    )
}