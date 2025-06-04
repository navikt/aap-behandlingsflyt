package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.samordning.AvklaringsType
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningVurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningYtelseRepository
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemorySakOgBehandlingService
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
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

    @ParameterizedTest
    @MethodSource("manuelleYtelserProvider")
    fun `om det finnes tilfeller av samordning med Sykepenger, Svangerskapspenger, Pleiepenger, skal det opprettes et avklaringsbehov`(
        ytelse: Ytelse
    ) {
        val behandling = opprettBehandling(nySak())
        val steg = settOppRessurser(ytelse, behandling.id)

        val res = steg.utfør(
            kontekst = FlytKontekstMedPerioder(
                sakId = behandling.sakId,
                behandlingId = behandling.id,
                forrigeBehandlingId = behandling.forrigeBehandlingId,
                behandlingType = behandling.typeBehandling(),
                vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
                årsakerTilBehandling = setOf(ÅrsakTilBehandling.MOTTATT_SØKNAD),
                rettighetsperiode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
            )
        )

        assertThat(res).isEqualTo(FantAvklaringsbehov(Definisjon.AVKLAR_SAMORDNING_GRADERING))

        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandling.id, SamordningVurderingGrunnlag(
                begrunnelse = "En god begrunnelse",
                maksDatoEndelig = false,
                maksDato = LocalDate.now().plusYears(1),
                vurderinger = listOf(
                    SamordningVurdering(
                        ytelseType = ytelse,
                        vurderingPerioder = listOf(
                            SamordningVurderingPeriode(
                                periode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                                gradering = Prosent(50),
                                manuell = false,
                            )
                        )
                    )
                )
            )
        )

        val res2 = steg.utfør(
            kontekst = FlytKontekstMedPerioder(
                sakId = behandling.sakId,
                behandlingId = behandling.id,
                forrigeBehandlingId = behandling.forrigeBehandlingId,
                behandlingType = behandling.typeBehandling(),
                vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
                årsakerTilBehandling = setOf(ÅrsakTilBehandling.MOTTATT_SØKNAD),
                rettighetsperiode = Periode(LocalDate.now().minusYears(1), LocalDate.now())
            )
        )

        assertThat(res2).isEqualTo(Fullført)
    }

    @Disabled("Inntil vi samordner ytelser automatisk")
    @ParameterizedTest
    @MethodSource("automatiskBehandledeYtelserProvider")
    fun `foreldrepenger, omsorgspenger, opplæringspenger avklares automatisk`(ytelse: Ytelse) {
        val behandling = opprettBehandling(nySak())
        val steg = settOppRessurser(ytelse, behandling.id)

        val res = steg.utfør(
            kontekst = FlytKontekstMedPerioder(
                sakId = behandling.sakId,
                behandlingId = behandling.id,
                forrigeBehandlingId = behandling.forrigeBehandlingId,
                behandlingType = behandling.typeBehandling(),
                vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
                årsakerTilBehandling = setOf(ÅrsakTilBehandling.MOTTATT_SØKNAD),
                rettighetsperiode = Periode(LocalDate.now().minusYears(1), LocalDate.now())
            )
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
                maksDatoEndelig = false,
                maksDato = LocalDate.now().plusYears(1),
                vurderinger = listOf(
                    SamordningVurdering(
                        ytelseType = Ytelse.PLEIEPENGER,
                        vurderingPerioder = listOf(
                            SamordningVurderingPeriode(
                                periode = pleiepengerPeriode,
                                gradering = Prosent(50),
                                manuell = false
                            )
                        )
                    ),
                    SamordningVurdering(
                        ytelseType = Ytelse.SYKEPENGER,
                        vurderingPerioder = listOf(
                            SamordningVurderingPeriode(
                                periode = periodeMedSykepenger,
                                gradering = Prosent(90),
                                manuell = false
                            )
                        )
                    )
                )
            )
        )


        val steg = settOppRessurser(
            Ytelse.SYKEPENGER,
            behandling.id,
            periode = periodeMedSykepenger
        )

        steg.utfør(
            FlytKontekstMedPerioder(
                sakId = behandling.sakId,
                behandlingId = behandling.id,
                forrigeBehandlingId = behandling.forrigeBehandlingId,
                behandlingType = behandling.typeBehandling(),
                vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
                årsakerTilBehandling = setOf(ÅrsakTilBehandling.MOTTATT_SØKNAD),
                rettighetsperiode = Periode(LocalDate.now().minusYears(1), LocalDate.now())
            )
        )

        val perioderMedSamordning = InMemorySamordningRepository.hentHvisEksisterer(behandling.id)!!

        assertThat(perioderMedSamordning.samordningPerioder).hasSize(2)
        assertThat(perioderMedSamordning.samordningPerioder.first().periode).isEqualTo(periodeMedSykepenger)
        assertThat(perioderMedSamordning.samordningPerioder.first().gradering).isEqualTo(Prosent(90))
        assertThat(perioderMedSamordning.samordningPerioder.last().periode).isEqualTo(pleiepengerPeriode)
        assertThat(perioderMedSamordning.samordningPerioder.last().gradering).isEqualTo(Prosent(50))
    }

    @Test
    fun `en fra register og en manuell, ikke overlappende pdddderioder`() {
        val behandling = opprettBehandling(nySak())
        val periodeMedSykepenger = Periode(LocalDate.now().minusYears(1), LocalDate.now())

        val pleiepengerPeriode = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusWeeks(2))
        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandling.id, SamordningVurderingGrunnlag(
                begrunnelse = "En god begrunnelse",
                maksDatoEndelig = false,
                maksDato = LocalDate.now().plusYears(1),
                vurderinger = listOf(
                    SamordningVurdering(
                        ytelseType = Ytelse.PLEIEPENGER,
                        vurderingPerioder = listOf(
                            SamordningVurderingPeriode(
                                periode = pleiepengerPeriode,
                                gradering = Prosent(50),
                                manuell = false
                            )
                        )
                    ),
                    SamordningVurdering(
                        ytelseType = Ytelse.SYKEPENGER,
                        vurderingPerioder = listOf(
                            SamordningVurderingPeriode(
                                periode = periodeMedSykepenger,
                                gradering = Prosent(90),
                                manuell = false
                            )
                        )
                    )
                )
            )
        )


        val steg = settOppRessurser(
            Ytelse.SYKEPENGER,
            behandling.id,
            periode = periodeMedSykepenger
        )

        steg.utfør(
            FlytKontekstMedPerioder(
                sakId = behandling.sakId,
                behandlingId = behandling.id,
                forrigeBehandlingId = behandling.forrigeBehandlingId,
                behandlingType = behandling.typeBehandling(),
                vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
                årsakerTilBehandling = setOf(ÅrsakTilBehandling.MOTTATT_SØKNAD),
                rettighetsperiode = Periode(LocalDate.now().minusYears(1), LocalDate.now())
            )
        )

        val perioderMedSamordning = InMemorySamordningRepository.hentHvisEksisterer(behandling.id)!!

        assertThat(perioderMedSamordning.samordningPerioder).hasSize(2)
        assertThat(perioderMedSamordning.samordningPerioder.first().periode).isEqualTo(periodeMedSykepenger)
        assertThat(perioderMedSamordning.samordningPerioder.first().gradering).isEqualTo(Prosent(90))
        assertThat(perioderMedSamordning.samordningPerioder.last().periode).isEqualTo(pleiepengerPeriode)
        assertThat(perioderMedSamordning.samordningPerioder.last().gradering).isEqualTo(Prosent(50))
    }

    @Test
    fun `saksbehandler kan lagre flere perioder enn det vi får fra registre`() {
        val behandling = opprettBehandling(nySak())
        val steg = settOppRessurser(Ytelse.SYKEPENGER, behandling.id)

        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandling.id, SamordningVurderingGrunnlag(
                begrunnelse = "En god begrunnelse",
                maksDatoEndelig = false,
                maksDato = LocalDate.now().plusYears(1),
                vurderinger = listOf(
                    SamordningVurdering(
                        ytelseType = Ytelse.SYKEPENGER,
                        vurderingPerioder = listOf(
                            SamordningVurderingPeriode(
                                periode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                                gradering = Prosent(50),
                                manuell = false,
                            )
                        )
                    )
                )
            )
        )

        val res2 = steg.utfør(
            kontekst = FlytKontekstMedPerioder(
                sakId = behandling.sakId,
                behandlingId = behandling.id,
                forrigeBehandlingId = behandling.forrigeBehandlingId,
                behandlingType = behandling.typeBehandling(),
                vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
                årsakerTilBehandling = setOf(ÅrsakTilBehandling.MOTTATT_SØKNAD),
                rettighetsperiode = Periode(LocalDate.now().minusYears(1), LocalDate.now())
            )
        )

        assertThat(res2).isEqualTo(Fullført)

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
        val kontekst = FlytKontekstMedPerioder(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            forrigeBehandlingId = behandling.forrigeBehandlingId,
            behandlingType = behandling.typeBehandling(),
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            årsakerTilBehandling = setOf(ÅrsakTilBehandling.MOTTATT_SØKNAD),
            rettighetsperiode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
        )

        val res = steg.utfør(kontekst = kontekst)

        assertThat(res).isEqualTo(FantAvklaringsbehov(Definisjon.AVKLAR_SAMORDNING_GRADERING))

        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandling.id, SamordningVurderingGrunnlag(
                begrunnelse = "En god begrunnelse",
                maksDatoEndelig = false,
                maksDato = LocalDate.now().plusYears(1),
                vurderinger = listOf(
                    SamordningVurdering(
                        ytelseType = Ytelse.SYKEPENGER,

                        vurderingPerioder = listOf(
                            SamordningVurderingPeriode(
                                periode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                                gradering = Prosent(50),
                                manuell = false,
                            )
                        )
                    )
                )
            )
        )

        val res2 = steg.utfør(kontekst = kontekst)

        assertThat(res2).isEqualTo(Fullført)

        lagreYtelseGrunnlag(behandling.id, Ytelse.SYKEPENGER, Periode(LocalDate.now().minusYears(2), LocalDate.now()))

        val res3 = steg.utfør(kontekst = kontekst)

        assertThat(res3).isEqualTo(FantAvklaringsbehov(Definisjon.AVKLAR_SAMORDNING_GRADERING))
    }

    @Test
    fun `skal kunne regne ut samordninggrad også uten registeropplysninger, kun vurderinger`() {
        val behandling = opprettBehandling(nySak())
        val steg = SamordningSteg(
            samordningService = SamordningService(
                samordningVurderingRepository = InMemorySamordningVurderingRepository,
                samordningYtelseRepository = InMemorySamordningYtelseRepository,
            ),
            samordningRepository = InMemorySamordningRepository,
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            tidligereVurderinger = FakeTidligereVurderinger(),
        )

        InMemorySamordningVurderingRepository.lagreVurderinger(
            behandlingId = behandling.id,
            samordningVurderinger = SamordningVurderingGrunnlag(
                begrunnelse = "bla bla",
                maksDatoEndelig = false,
                maksDato = LocalDate.now().plusWeeks(1),
                vurderinger = listOf(
                    SamordningVurdering(
                        Ytelse.SYKEPENGER, listOf(
                            SamordningVurderingPeriode(
                                periode = Periode(LocalDate.now().minusWeeks(2), LocalDate.now().plusWeeks(2)),
                                gradering = Prosent.`100_PROSENT`,
                                manuell = false,
                            )
                        )
                    )
                )
            )
        )

        val kontekst = FlytKontekstMedPerioder(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            forrigeBehandlingId = behandling.forrigeBehandlingId,
            behandlingType = behandling.typeBehandling(),
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            årsakerTilBehandling = setOf(ÅrsakTilBehandling.MOTTATT_SØKNAD),
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        )

        val res = steg.utfør(kontekst)

        assertThat(res).isEqualTo(Fullført)

        val uthentet = InMemorySamordningRepository.hentHvisEksisterer(behandling.id)

        assertThat(uthentet!!.samordningPerioder).hasSize(1)
        assertThat(uthentet.samordningPerioder.first().gradering).isEqualTo(Prosent.`100_PROSENT`)
    }

    private fun settOppRessurser(
        ytelse: Ytelse,
        behandlingId: BehandlingId,
        periode: Periode = Periode(LocalDate.now().minusYears(1), LocalDate.now())
    ): SamordningSteg {
        val steg = SamordningSteg(
            samordningService = SamordningService(
                samordningVurderingRepository = InMemorySamordningVurderingRepository,
                samordningYtelseRepository = InMemorySamordningYtelseRepository,
            ),
            samordningRepository = InMemorySamordningRepository,
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            tidligereVurderinger = FakeTidligereVurderinger(),
        )

        lagreYtelseGrunnlag(behandlingId, ytelse, periode)
        return steg
    }

    private fun lagreYtelseGrunnlag(
        behandlingId: BehandlingId,
        ytelse: Ytelse,
        periode: Periode
    ) {
        InMemorySamordningYtelseRepository.lagre(
            behandlingId, listOf(
                SamordningYtelse(
                    ytelseType = ytelse,
                    ytelsePerioder = listOf(
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
            .finnEllerOpprettBehandling(sak.saksnummer, listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD)))
            .behandling
    }
}
