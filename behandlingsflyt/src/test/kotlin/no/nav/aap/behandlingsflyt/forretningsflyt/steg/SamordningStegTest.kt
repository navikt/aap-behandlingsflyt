package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingRepository
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.samordning.AvklaringsType
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningVurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningYtelseRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemorySakOgBehandlingService
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.stream.Stream

class SamordningStegTest {
    companion object {
        @JvmStatic
        fun manuelleYtelserProvider(): Stream<Ytelse> {
            return Ytelse.entries.filter { it.type == AvklaringsType.MANUELL }.stream()
        }

        @JvmStatic
        fun automatiskBehandledeYtelserProvider(): Stream<Ytelse> {
            return Ytelse.entries.filter { it.type == AvklaringsType.AUTOMATISK }.stream()
        }
    }

    private val tidligereVurderinger = mockk<TidligereVurderinger>()
    private val avbrytRevurderingRepository = mockk<AvbrytRevurderingRepository>()

    private val steg = SamordningSteg(
        samordningService = SamordningService(
            samordningVurderingRepository = InMemorySamordningVurderingRepository,
            samordningYtelseRepository = InMemorySamordningYtelseRepository,
        ),
        samordningRepository = InMemorySamordningRepository,
        avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
        tidligereVurderinger = tidligereVurderinger,
        vilkårsresultatRepository = InMemoryVilkårsresultatRepository,
        behandlingRepository = InMemoryBehandlingRepository,
        avklaringsbehovService = AvklaringsbehovService(
            InMemoryAvklaringsbehovRepository, AvbrytRevurderingService(
                avbrytRevurderingRepository
            )
        ),
    )

    @BeforeEach
    fun beforeEach() {
        every { tidligereVurderinger.girAvslag(any(), any()) } returns false
        every {
            tidligereVurderinger.behandlingsutfall(
                any(),
                any()
            )
        } answers {
            tidslinjeOf(
                firstArg<FlytKontekstMedPerioder>().rettighetsperiode to TidligereVurderinger.Behandlingsutfall.UKJENT
            )
        }
        every { avbrytRevurderingRepository.hentHvisEksisterer(any()) } returns null
    }

    @ParameterizedTest
    @MethodSource("manuelleYtelserProvider")
    fun `om det finnes tilfeller av samordning med Sykepenger, Svangerskapspenger, Pleiepenger, skal det opprettes et avklaringsbehov`(
        ytelse: Ytelse
    ) {
        val behandling = opprettBehandling(nySak())
        val steg = settOppRessurser(ytelse, behandling.id)

        steg.utfør(flytKontekstMedPerioder(behandling))

        verifiserAvklaringsbehov(behandling, Status.OPPRETTET)

        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandling.id, SamordningVurderingGrunnlag(
                begrunnelse = "En god begrunnelse",
                maksDatoEndelig = false,
                fristNyRevurdering = LocalDate.now().plusYears(1),
                vurderinger = setOf(
                    SamordningVurdering(
                        ytelseType = ytelse,
                        vurderingPerioder = setOf(
                            SamordningVurderingPeriode(
                                periode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                                gradering = Prosent(50),
                                manuell = false,
                            )
                        )
                    )
                ),
                vurdertAv = "ident"
            )
        )
        løsBehovet(behandling)

        steg.utfør(
            kontekst = flytKontekstMedPerioder(behandling)
        )

        verifiserAvklaringsbehov(behandling, Status.AVSLUTTET)
    }

    @Disabled("Inntil vi samordner ytelser automatisk")
    @ParameterizedTest
    @MethodSource("automatiskBehandledeYtelserProvider")
    fun `foreldrepenger, omsorgspenger, opplæringspenger avklares automatisk`(ytelse: Ytelse) {
        val behandling = opprettBehandling(nySak())
        val steg = settOppRessurser(ytelse, behandling.id)

        val res = steg.utfør(
            kontekst = flytKontekstMedPerioder(behandling)
        )

        assertThat(res).isEqualTo(Fullført)
    }

    @Test
    fun `en fra register og en manuell, ikke overlappende perioder`() {
        val behandling = opprettBehandling(nySak())
        val periodeMedSykepenger = Periode(LocalDate.now().minusYears(1), LocalDate.now())

        val pleiepengerPeriode = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusWeeks(2))
        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandling.id, SamordningVurderingGrunnlag(
                begrunnelse = "En god begrunnelse",
                maksDatoEndelig = true,
                fristNyRevurdering = null,
                vurderinger = setOf(
                    SamordningVurdering(
                        ytelseType = Ytelse.PLEIEPENGER,
                        vurderingPerioder = setOf(
                            SamordningVurderingPeriode(
                                periode = pleiepengerPeriode,
                                gradering = Prosent(50),
                                manuell = false
                            )
                        )
                    ),
                    SamordningVurdering(
                        ytelseType = Ytelse.SYKEPENGER,
                        vurderingPerioder = setOf(
                            SamordningVurderingPeriode(
                                periode = periodeMedSykepenger,
                                gradering = Prosent(90),
                                manuell = false
                            )
                        )
                    )
                ),
                vurdertAv = "ident"
            )
        )


        val steg = settOppRessurser(
            Ytelse.SYKEPENGER,
            behandling.id,
            periode = periodeMedSykepenger
        )

        steg.utfør(flytKontekstMedPerioder(behandling))

        val perioderMedSamordning = InMemorySamordningRepository.hentHvisEksisterer(behandling.id)!!

        assertThat(perioderMedSamordning.samordningPerioder).hasSize(2)
        assertThat(perioderMedSamordning.samordningPerioder.first().periode).isEqualTo(periodeMedSykepenger)
        assertThat(perioderMedSamordning.samordningPerioder.first().gradering).isEqualTo(Prosent(90))
        assertThat(perioderMedSamordning.samordningPerioder.last().periode).isEqualTo(pleiepengerPeriode)
        assertThat(perioderMedSamordning.samordningPerioder.last().gradering).isEqualTo(Prosent(50))
    }

    @Test
    fun `tilbakeføring skal slette vurderinger`() {
        val behandling = opprettBehandling(nySak())
        val steg = settOppRessurser(Ytelse.SYKEPENGER, behandling.id)

        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandling.id, SamordningVurderingGrunnlag(
                begrunnelse = "",
                maksDatoEndelig = true,
                fristNyRevurdering = null,
                vurderinger = setOf(
                    SamordningVurdering(
                        ytelseType = Ytelse.SYKEPENGER,
                        vurderingPerioder = setOf(
                            SamordningVurderingPeriode(
                                periode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                                gradering = Prosent(50),
                                manuell = false,
                            )
                        )
                    )
                ),
                vurdertAv = "ident"
            )
        )

        steg.utfør(flytKontekstMedPerioder(behandling))
        verifiserAvklaringsbehov(behandling, Status.OPPRETTET)
        løsBehovet(behandling)

        // Simler trekk av søknad
        simulerTrekkAvSøknad()

        // skal tilbakefølre
        steg.utfør(flytKontekstMedPerioder(behandling))

        val vurderinger = InMemorySamordningVurderingRepository.hentHvisEksisterer(behandling.id)
        assertThat(vurderinger).isNull()
    }

    private fun simulerTrekkAvSøknad() {
        every {
            tidligereVurderinger.behandlingsutfall(
                any(),
                any()
            )
        } answers {
            tidslinjeOf(
                firstArg<FlytKontekstMedPerioder>().rettighetsperiode to TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG
            )
        }
    }

    @Test
    fun `saksbehandler kan lagre flere perioder enn det vi får fra registre`() {
        val behandling = opprettBehandling(nySak())
        val steg = settOppRessurser(Ytelse.SYKEPENGER, behandling.id)

        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandling.id, SamordningVurderingGrunnlag(
                begrunnelse = "",
                maksDatoEndelig = true,
                fristNyRevurdering = null,
                vurderinger = setOf(
                    SamordningVurdering(
                        ytelseType = Ytelse.SYKEPENGER,
                        vurderingPerioder = setOf(
                            SamordningVurderingPeriode(
                                periode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                                gradering = Prosent(50),
                                manuell = false,
                            )
                        )
                    )
                ),
                vurdertAv = "ident"
            )
        )

        val res2 = steg.utfør(
            kontekst = flytKontekstMedPerioder(behandling)
        )

        assertThat(res2).isEqualTo(Fullført)
        verifiserAvklaringsbehov(behandling, Status.OPPRETTET)

        val uthentetGrunnlag = InMemorySamordningRepository.hentHvisEksisterer(behandling.id)

        assertThat(uthentetGrunnlag!!.samordningPerioder).hasSize(1)
        assertThat(uthentetGrunnlag.samordningPerioder.first()).isEqualTo(
            SamordningPeriode(
                periode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                gradering = Prosent(50),
            )
        )
    }

    @Test
    fun `om det kommer ny informasjon, avklaringsbehov opprettes igjen`() {
        val behandling = opprettBehandling(nySak())
        val steg = settOppRessurser(Ytelse.SYKEPENGER, behandling.id)
        val kontekst = flytKontekstMedPerioder(behandling)

        steg.utfør(kontekst = kontekst)
        verifiserAvklaringsbehov(behandling, Status.OPPRETTET)

        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandling.id, SamordningVurderingGrunnlag(
                begrunnelse = "",
                maksDatoEndelig = true,
                fristNyRevurdering = null,
                vurderinger = setOf(
                    SamordningVurdering(
                        ytelseType = Ytelse.SYKEPENGER,

                        vurderingPerioder = setOf(
                            SamordningVurderingPeriode(
                                periode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                                gradering = Prosent(50),
                                manuell = false,
                            )
                        )
                    )
                ),
                vurdertAv = "ident"
            )
        )
        løsBehovet(behandling)

        val res2 = steg.utfør(kontekst = kontekst)

        assertThat(res2).isEqualTo(Fullført)
        verifiserAvklaringsbehov(behandling, Status.AVSLUTTET)

        lagreYtelseGrunnlag(behandling.id, Ytelse.SYKEPENGER, Periode(LocalDate.now().minusYears(2), LocalDate.now()))
        løsBehovet(behandling)

        steg.utfør(kontekst = kontekst)

        verifiserAvklaringsbehov(behandling, Status.OPPRETTET)
    }

    private fun flytKontekstMedPerioder(behandling: Behandling): FlytKontekstMedPerioder = FlytKontekstMedPerioder(
        sakId = behandling.sakId,
        behandlingId = behandling.id,
        forrigeBehandlingId = behandling.forrigeBehandlingId,
        behandlingType = behandling.typeBehandling(),
        vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
        vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_SØKNAD),
        rettighetsperiode = Periode(LocalDate.now().minusYears(1), LocalDate.now())
    )

    @Test
    fun `skal kunne regne ut samordninggrad også uten registeropplysninger, kun vurderinger`() {
        val behandling = opprettBehandling(nySak())


        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandlingId = behandling.id,
            samordningVurderinger = SamordningVurderingGrunnlag(
                begrunnelse = "",
                maksDatoEndelig = true,
                fristNyRevurdering = null,
                vurderinger = setOf(
                    SamordningVurdering(
                        Ytelse.SYKEPENGER, setOf(
                            SamordningVurderingPeriode(
                                periode = Periode(LocalDate.now().minusWeeks(2), LocalDate.now().plusWeeks(2)),
                                gradering = Prosent.`100_PROSENT`,
                                manuell = false,
                            )
                        )
                    )
                ),
                vurdertAv = "ident"
            )
        )

        val kontekst = FlytKontekstMedPerioder(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            forrigeBehandlingId = behandling.forrigeBehandlingId,
            behandlingType = behandling.typeBehandling(),
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_SØKNAD),
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        )

        val res = steg.utfør(kontekst)

        assertThat(res).isEqualTo(Fullført)

        val uthentet = InMemorySamordningRepository.hentHvisEksisterer(behandling.id)

        assertThat(uthentet!!.samordningPerioder).hasSize(1)
        assertThat(uthentet.samordningPerioder.first().gradering).isEqualTo(Prosent.`100_PROSENT`)
    }


    @Test
    fun `skal ikke gjøre noe spesielt dersom maksdato (deprekert) ikke er bekreftet`() {
        val behandling = opprettBehandling(nySak())

        val sykepengePeriode = Periode(LocalDate.now().minusWeeks(2), LocalDate.now().plusWeeks(2))
        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandlingId = behandling.id,
            samordningVurderinger = SamordningVurderingGrunnlag(
                begrunnelse = "bla bla",
                maksDatoEndelig = false,
                fristNyRevurdering = LocalDate.now().plusWeeks(1),
                vurderinger = setOf(
                    SamordningVurdering(
                        Ytelse.SYKEPENGER, setOf(
                            SamordningVurderingPeriode(
                                periode = sykepengePeriode,
                                gradering = Prosent.`100_PROSENT`,
                                manuell = false,
                            )
                        )
                    )
                ),
                vurdertAv = "ident"
            )
        )

        val kontekst = FlytKontekstMedPerioder(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            forrigeBehandlingId = behandling.forrigeBehandlingId,
            behandlingType = behandling.typeBehandling(),
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_SØKNAD),
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        )

        val res = steg.utfør(kontekst)

        assertThat(res).isEqualTo(Fullført)

        val uthentet = InMemorySamordningRepository.hentHvisEksisterer(behandling.id)

        val samordninger = uthentet?.samordningPerioder.orEmpty()
        assertThat(samordninger).hasSize(1)
        assertThat(samordninger.first().gradering).isEqualTo(Prosent.`100_PROSENT`)
        assertThat(samordninger.first().periode).isEqualTo(sykepengePeriode)
    }

    private fun løsBehovet(behandling: Behandling) {
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id).løsAvklaringsbehov(
            Definisjon.AVKLAR_SAMORDNING_GRADERING,
            begrunnelse = "...",
            endretAv = "meg"
        )
    }

    private fun verifiserAvklaringsbehov(behandling: Behandling, ønsketStatus: Status) {
        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        assertThat(
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SAMORDNING_GRADERING)?.status()
        ).isEqualTo(ønsketStatus)
    }

    private fun settOppRessurser(
        ytelse: Ytelse,
        behandlingId: BehandlingId,
        periode: Periode = Periode(LocalDate.now().minusYears(1), LocalDate.now())
    ): SamordningSteg {
        lagreYtelseGrunnlag(behandlingId, ytelse, periode)
        return steg
    }

    private fun lagreYtelseGrunnlag(
        behandlingId: BehandlingId,
        ytelse: Ytelse,
        periode: Periode
    ) {
        InMemorySamordningYtelseRepository.lagre(
            behandlingId, setOf(
                SamordningYtelse(
                    ytelseType = ytelse,
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = periode,
                            gradering = Prosent(50),
                            kronesum = 1234,
                        )
                    ),
                    kilde = "xxxx",
                )
            )
        )
    }

    private fun nySak(): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            InMemoryPersonRepository,
            InMemorySakRepository
        ).finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
    }

    private fun opprettBehandling(sak: Sak): Behandling {
        return InMemorySakOgBehandlingService
            .finnEllerOpprettOrdinærBehandling(
                sak.saksnummer,
                VurderingsbehovOgÅrsak(
                    listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    ÅrsakTilOpprettelse.SØKNAD
                )
            )
    }
}
