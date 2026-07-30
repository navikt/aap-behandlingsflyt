package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.samordning.AvklaringsType
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningVurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningYtelseRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Stream

class SamordningStegTest {
    companion object {
        @JvmStatic
        fun manuelleYtelserProvider(): Stream<Ytelse> {
            return Ytelse.entries.filter { it.type == AvklaringsType.MANUELL }.stream()
        }
    }

    @ParameterizedTest
    @MethodSource("manuelleYtelserProvider")
    fun `om det finnes tilfeller av samordning med Sykepenger, Svangerskapspenger, Pleiepenger, skal det opprettes et avklaringsbehov`(
        ytelse: Ytelse
    ) {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        settOppRessurser(ytelse, behandling.id)

        steg().utfør(flytKontekstMedPerioder(behandling))

        verifiserAvklaringsbehov(behandling, Status.OPPRETTET)

        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandling.id, SamordningVurderingGrunnlag(
                begrunnelse = "En god begrunnelse",
                vurderinger = setOf(
                    SamordningVurdering(
                        ytelseType = ytelse,
                        vurderingPerioder = setOf(
                            SamordningVurderingPeriode(
                                periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1)),
                                gradering = Prosent(50),
                                manuell = false,
                            )
                        )
                    )
                ),
                vurdertAv = Bruker("ident"),
                vurdertTidspunkt = LocalDateTime.now()
            )
        )
        løsBehovet(behandling)

        steg().utfør(flytKontekstMedPerioder(behandling))

        verifiserAvklaringsbehov(behandling, Status.AVSLUTTET)
    }

    @Test
    fun `en fra register og en manuell, ikke overlappende perioder`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val periodeMedSykepenger = Periode(LocalDate.now(), LocalDate.now().plusYears(1))

        val pleiepengerPeriode =
            Periode(LocalDate.now().plusYears(1).plusDays(1), LocalDate.now().plusYears(1).plusWeeks(2))
        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandling.id, SamordningVurderingGrunnlag(
                begrunnelse = "En god begrunnelse",
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
                vurdertAv = Bruker("ident"),
                vurdertTidspunkt = LocalDateTime.now()
            )
        )

        settOppRessurser(
            Ytelse.SYKEPENGER,
            behandling.id,
            periode = periodeMedSykepenger
        )

        steg().utfør(flytKontekstMedPerioder(behandling))

        val perioderMedSamordning = InMemorySamordningRepository.hentHvisEksisterer(behandling.id)!!

        assertThat(perioderMedSamordning.samordningPerioder).hasSize(2)
        assertThat(perioderMedSamordning.samordningPerioder.first().periode).isEqualTo(periodeMedSykepenger)
        assertThat(perioderMedSamordning.samordningPerioder.first().gradering).isEqualTo(Prosent(90))
        assertThat(perioderMedSamordning.samordningPerioder.last().periode).isEqualTo(pleiepengerPeriode)
        assertThat(perioderMedSamordning.samordningPerioder.last().gradering).isEqualTo(Prosent(50))
    }

    @Test
    fun `tilbakeføring skal slette vurderinger`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()
        settOppRessurser(Ytelse.SYKEPENGER, behandling.id)

        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandling.id, SamordningVurderingGrunnlag(
                begrunnelse = "",
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
                vurdertAv = Bruker("ident"),
                vurdertTidspunkt = LocalDateTime.now()
            )
        )

        steg().utfør(flytKontekstMedPerioder(behandling))
        verifiserAvklaringsbehov(behandling, Status.OPPRETTET)
        løsBehovet(behandling)

        // Simuler trekk av søknad
        InMemoryTrukketSøknadRepository.lagreTrukketSøknadVurdering(
            behandling.id,
            TrukketSøknadVurdering(
                journalpostId = JournalpostId("12344321"),
                begrunnelse = "en grunn",
                vurdertAv = Bruker("Z00000"),
                skalTrekkes = true,
                vurdert = Instant.parse("2020-01-01T12:12:12Z"),
            )
        )

        // skal tilbakeføre
        steg(
            FakeTidligereVurderinger(
                Tidslinje(
                    sak.rettighetsperiode,
                    TidligereVurderinger.UunngåeligAvslag
                )
            ).apply {
                avslagEllerIngenBehandlingsgrunnlag = true
                ingenBehandlingsgrunnlag = true
            })
            .utfør(flytKontekstMedPerioder(behandling))

        val vurderinger = InMemorySamordningVurderingRepository.hentHvisEksisterer(behandling.id)
        assertThat(vurderinger).isNull()
    }

    @Test
    fun `saksbehandler kan lagre flere perioder enn det vi får fra registre`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        settOppRessurser(Ytelse.SYKEPENGER, behandling.id)

        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandling.id, SamordningVurderingGrunnlag(
                begrunnelse = "",
                vurderinger = setOf(
                    SamordningVurdering(
                        ytelseType = Ytelse.SYKEPENGER,
                        vurderingPerioder = setOf(
                            SamordningVurderingPeriode(
                                periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1)),
                                gradering = Prosent(50),
                                manuell = false,
                            )
                        )
                    )
                ),
                vurdertAv = Bruker("ident"),
                vurdertTidspunkt = LocalDateTime.now()
            )
        )

        val res2 = steg().utfør(
            kontekst = flytKontekstMedPerioder(behandling)
        )

        assertThat(res2).isEqualTo(Fullført)
        verifiserAvklaringsbehov(behandling, Status.OPPRETTET)

        val uthentetGrunnlag = InMemorySamordningRepository.hentHvisEksisterer(behandling.id)

        assertThat(uthentetGrunnlag!!.samordningPerioder).hasSize(1)
        assertThat(uthentetGrunnlag.samordningPerioder.first()).isEqualTo(
            SamordningPeriode(
                periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1)),
                gradering = Prosent(50),
            )
        )
    }

    @Test
    fun `om det kommer ny informasjon, avklaringsbehov opprettes igjen`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        settOppRessurser(Ytelse.SYKEPENGER, behandling.id)
        val kontekst = flytKontekstMedPerioder(behandling)

        steg().utfør(kontekst = kontekst)
        verifiserAvklaringsbehov(behandling, Status.OPPRETTET)

        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandling.id, SamordningVurderingGrunnlag(
                begrunnelse = "",
                vurderinger = setOf(
                    SamordningVurdering(
                        ytelseType = Ytelse.SYKEPENGER,

                        vurderingPerioder = setOf(
                            SamordningVurderingPeriode(
                                periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1)),
                                gradering = Prosent(50),
                                manuell = false,
                            )
                        )
                    )
                ),
                vurdertAv = Bruker("ident"),
                vurdertTidspunkt = LocalDateTime.now()
            )
        )
        løsBehovet(behandling)

        val res2 = steg().utfør(kontekst = kontekst)

        assertThat(res2).isEqualTo(Fullført)
        verifiserAvklaringsbehov(behandling, Status.AVSLUTTET)

        lagreYtelseGrunnlag(
            behandling.id,
            Ytelse.SYKEPENGER,
            Periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
        )
        løsBehovet(behandling)

        steg().utfør(kontekst = kontekst)

        verifiserAvklaringsbehov(behandling, Status.OPPRETTET)
    }

    private fun flytKontekstMedPerioder(behandling: Behandling): FlytKontekstMedPerioder =
        no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder {
            this.behandling = behandling
        }

    @Test
    fun `skal kunne regne ut samordninggrad også uten registeropplysninger, kun vurderinger`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()


        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandlingId = behandling.id,
            samordningVurderinger = SamordningVurderingGrunnlag(
                begrunnelse = "",
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
                vurdertAv = Bruker("ident"),
                vurdertTidspunkt = LocalDateTime.now()
            )
        )

        val kontekst =
            no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder {
                this.behandling = behandling
                this.rettighetsperiode = Periode(LocalDate.now().minusYears(1), LocalDate.now())
            }

        val res = steg().utfør(kontekst)

        assertThat(res).isEqualTo(Fullført)

        val uthentet = InMemorySamordningRepository.hentHvisEksisterer(behandling.id)

        assertThat(uthentet!!.samordningPerioder).hasSize(1)
        assertThat(uthentet.samordningPerioder.first().gradering).isEqualTo(Prosent.`100_PROSENT`)
    }


    @Test
    fun `skal ikke gjøre noe spesielt dersom maksdato (deprekert) ikke er bekreftet`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        val sykepengePeriode = Periode(LocalDate.now().minusWeeks(2), LocalDate.now().plusWeeks(2))
        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandlingId = behandling.id,
            samordningVurderinger = SamordningVurderingGrunnlag(
                begrunnelse = "bla bla",
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
                vurdertAv = Bruker("ident"),
                vurdertTidspunkt = LocalDateTime.now()
            )
        )

        val kontekst = no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder {
            this.behandling = behandling
            this.vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_SØKNAD)
            this.rettighetsperiode = Periode(LocalDate.now().minusYears(1), LocalDate.now())
        }

        val res = steg().utfør(kontekst)

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
            endretAv = Bruker("meg")
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
        periode: Periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
    ) {
        lagreYtelseGrunnlag(behandlingId, ytelse, periode)
    }

    private fun steg(tidligereVurderinger: TidligereVurderinger = FakeTidligereVurderinger()): SamordningSteg {
        return SamordningSteg(
            samordningService = SamordningService(inMemoryRepositoryProvider),
            samordningRepository = inMemoryRepositoryProvider.provide(),
            tidligereVurderinger = tidligereVurderinger,
            avklaringsbehovService = AvklaringsbehovService(inMemoryRepositoryProvider, minimalGatewayProvider())
        )
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
}
