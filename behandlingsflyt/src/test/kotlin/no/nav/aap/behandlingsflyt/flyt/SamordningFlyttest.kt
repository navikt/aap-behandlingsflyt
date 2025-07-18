package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.RefusjonkravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SamordningVentPaVirkningstidspunktLøsning
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.adapter.InstitusjonsoppholdJSON
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritaksvurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.SamordningVurderingData
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.VurderingerForSamordning
import no.nav.aap.behandlingsflyt.flyt.internals.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime


class SamordningFlyttest : AbstraktFlytOrkestratorTest() {

    @Test
    fun `ingen sykepenger i register, vurderer sykepenger for samordning med ukjent maksdato som fører til revurdering og ingen utbetaling etter kjent sykepengedato`() {
        val fom = LocalDate.now()
        val periode = Periode(fom, fom.plusYears(3))
        val sykePengerPeriode = Periode(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        val person = TestPersoner.STANDARD_PERSON()

        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnSøknad(
            ident, periode,
            SøknadV0(
                student = SøknadStudentDto("NEI"),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            ),
        )
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
                assertThat(åpneAvklaringsbehov).isNotEmpty()
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsSykdom()
            .løsAvklaringsBehov(
                AvklarBistandsbehovLøsning(
                    bistandsVurdering = BistandVurderingLøsningDto(
                        begrunnelse = "Trenger hjelp fra nav",
                        erBehovForAktivBehandling = true,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = null,
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null
                    ),
                )
            )
            .løsRefusjonskrav()
            .løsAvklaringsBehov(
                FritakMeldepliktLøsning(
                    fritaksvurderinger = listOf(
                        FritaksvurderingDto(
                            harFritak = true,
                            fraDato = periode.fom,
                            begrunnelse = "...",
                        )
                    ),
                ),
            )
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).containsExactly(Definisjon.FORESLÅ_VEDTAK)
            }
            .løsAvklaringsBehov(
                AvklarSamordningGraderingLøsning(
                    vurderingerForSamordning = VurderingerForSamordning(
                        vurderteSamordningerData = listOf(
                            SamordningVurderingData(
                                ytelseType = Ytelse.SYKEPENGER,
                                periode = sykePengerPeriode,
                                gradering = 100,
                                kronesum = null,
                            )
                        ),
                        begrunnelse = "",
                        maksDatoEndelig = true,
                        fristNyRevurdering = null,
                    ),
                ),
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).isEqualTo(listOf(Definisjon.FORESLÅ_VEDTAK))

                // Vilkår skal ikke være oppfylt med 100% gradert samordning
                val vilkår = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.SAMORDNING)
                assertThat(vilkår.vilkårsperioder()).hasSize(1)
                    .first()
                    .extracting(Vilkårsperiode::utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
            }
            .løsAvklaringsBehov(
                AvklarSamordningGraderingLøsning(
                    vurderingerForSamordning = VurderingerForSamordning(
                        vurderteSamordningerData = listOf(
                            SamordningVurderingData(
                                ytelseType = Ytelse.SYKEPENGER,
                                periode = sykePengerPeriode,
                                gradering = 50,
                                kronesum = null,
                            ),
                            SamordningVurderingData(
                                ytelseType = Ytelse.PLEIEPENGER,
                                periode = sykePengerPeriode,
                                gradering = 50,
                                kronesum = null,
                            )
                        ),
                        begrunnelse = "",
                        maksDatoEndelig = true,
                        fristNyRevurdering = null,
                    ),
                ),
            )

        // Vilkår skal være ikke vurdert når samordningen har mindre enn 100% gradering
        var vilkårOppdatert = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.SAMORDNING)
        assertThat(vilkårOppdatert.vilkårsperioder()).hasSize(1)
        assertThat(vilkårOppdatert.vilkårsperioder().first().utfall).isEqualTo(Utfall.IKKE_VURDERT)

        // Setter samordningen til 100 sykepenger og ikke oppfylt for å verifisere opprettelse av revurdering
        behandling = løsAvklaringsBehov(
            behandling,
            AvklarSamordningGraderingLøsning(
                vurderingerForSamordning = VurderingerForSamordning(
                    vurderteSamordningerData = listOf(
                        SamordningVurderingData(
                            ytelseType = Ytelse.SYKEPENGER,
                            periode = sykePengerPeriode,
                            gradering = 100,
                            kronesum = null,
                        )
                    ),
                    begrunnelse = "En god begrunnelse",
                    maksDatoEndelig = false,
                    fristNyRevurdering = LocalDate.now().plusMonths(1),
                ),
            ),
        )

        // Vilkår skal være ikke vurdert når samordningen har mindre enn 100% gradering
        vilkårOppdatert = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.SAMORDNING)
        assertThat(vilkårOppdatert.vilkårsperioder()).hasSize(2)
            .extracting(Vilkårsperiode::utfall)
            .containsExactly(tuple(Utfall.IKKE_OPPFYLT), tuple(Utfall.IKKE_VURDERT))

        behandling = løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())
            .fattVedtakEllerSendRetur()

        val uthentetTilkjentYtelse =
            requireNotNull(dataSource.transaction { TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id) })
            { "Tilkjent ytelse skal være beregnet her." }

        val periodeMedFullSamordning =
            uthentetTilkjentYtelse.map { Segment(it.periode, it.tilkjent.gradering.samordningGradering) }
                .let(::Tidslinje)
                .filter { it.verdi == Prosent.`100_PROSENT` }.helePerioden()

        // Verifiser at samordningen ble fanget opp
        assertThat(periodeMedFullSamordning.inneholder(sykePengerPeriode.tom)).isTrue
        // Verifiser at samordning med 100% strekker seg ut rettighetsperioden for å unngå feilaktig utbetaling fordi perioden har passert
        assertThat(periodeMedFullSamordning.tom).isEqualTo(periode.tom)

        val behandlingReferanse = behandling.referanse
        behandling = behandling.løsVedtaksbrev()

        var revurdering = hentNyesteBehandlingForSak(behandling.sakId)

        // Siden samordning overlappet, skal en revurdering opprettes med en gang
        assertThat(revurdering.referanse).isNotEqualTo(behandlingReferanse)
        assertThat(revurdering.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
        util.ventPåSvar(sakId = behandling.sakId.id)

        // Verifiser at den er satt på vent
        var åpneAvklaringsbehovPåNyBehandling = hentÅpneAvklaringsbehov(revurdering.id)

        assertThat(åpneAvklaringsbehovPåNyBehandling.map { it.definisjon }).contains(Definisjon.SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT)

        // Ta av vent
        revurdering = løsAvklaringsBehov(revurdering, SamordningVentPaVirkningstidspunktLøsning())

        åpneAvklaringsbehovPåNyBehandling = hentÅpneAvklaringsbehov(revurdering.id)
        assertThat(åpneAvklaringsbehovPåNyBehandling.map { it.definisjon }).containsExactly(Definisjon.AVKLAR_SAMORDNING_GRADERING)

        // Avklar samordning i revurdering
        revurdering = løsAvklaringsBehov(
            revurdering,
            AvklarSamordningGraderingLøsning(
                vurderingerForSamordning = VurderingerForSamordning(
                    vurderteSamordningerData = listOf(
                        SamordningVurderingData(
                            ytelseType = Ytelse.SYKEPENGER,
                            periode = sykePengerPeriode,
                            gradering = 100,
                            kronesum = null,
                        )
                    ),
                    begrunnelse = "En god begrunnelse",
                    maksDatoEndelig = true,
                    fristNyRevurdering = null,
                ),
            ),
        )

        val tilkjentYtelse =
            requireNotNull(dataSource.transaction { TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(revurdering.id) }) { "Tilkjent ytelse skal være beregnet her." }

        assertThat(tilkjentYtelse).hasSizeGreaterThan(2)
        tilkjentYtelse.forEach {
            if (it.periode.overlapper(sykePengerPeriode)) {
                assertThat(it.tilkjent.gradering.samordningGradering).isEqualTo(Prosent.`100_PROSENT`)
                assertThat(it.tilkjent.redusertDagsats()).isEqualTo(Beløp(0))
            } else {
                assertThat(it.tilkjent.gradering.samordningGradering).isEqualTo(Prosent.`0_PROSENT`)
                assertThat(it.tilkjent.redusertDagsats()).isNotEqualTo(Beløp(0))
            }
        }

        åpneAvklaringsbehovPåNyBehandling = hentÅpneAvklaringsbehov(revurdering.id)
        assertThat(åpneAvklaringsbehovPåNyBehandling.map { it.definisjon }).containsExactly(Definisjon.FORESLÅ_VEDTAK)

    }

    @Test
    fun `stopper opp ved samordning ved funn av sykepenger, og løses ved info fra saksbehandler`() {
        val fom = LocalDate.now().minusMonths(1)
        val periode = Periode(fom, fom.plusYears(3))

        val sykePengerPeriode = Periode(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))

        val person = TestPersoner.STANDARD_PERSON().medSykepenger(
            listOf(
                TestPerson.Sykepenger(
                    grad = 50, periode = sykePengerPeriode
                )
            )
        )
        val ident = person.aktivIdent()
        var revurdering = revurderingEtterVentPåSamordning(ident, periode, sykePengerPeriode)

        // Verifiser at den er satt på vent
        var åpneAvklaringsbehovPåNyBehandling = hentÅpneAvklaringsbehov(revurdering.id)

        assertThat(
            åpneAvklaringsbehovPåNyBehandling.map { it.definisjon }).contains(Definisjon.SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT)

        // Opprett manuell revurdering før ta av vent
        revurdering = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    ManuellRevurderingV0(
                        årsakerTilBehandling = listOf(SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
                        beskrivelse = "en begrunnelse",
                    ),
                ),
                journalpost = JournalpostId("121321"),
                innsendingType = InnsendingType.MANUELL_REVURDERING,
                periode = periode,
            )
        )
        assertThat(revurdering.årsaker().map { it.type }).describedAs("Ny årsak skal være lagt til")
            .contains(ÅrsakTilBehandling.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)

        // Ta av vent
        revurdering = løsAvklaringsBehov(revurdering, SamordningVentPaVirkningstidspunktLøsning())

        assertThat(revurdering.aktivtSteg()).describedAs("Forventer at behandlingen ligger på sykdom nå.")
            .isEqualTo(StegType.AVKLAR_SYKDOM)

        åpneAvklaringsbehovPåNyBehandling = hentÅpneAvklaringsbehov(revurdering.id)
        assertThat(åpneAvklaringsbehovPåNyBehandling.filter { it.erVentepunkt() }).isEmpty()

        assertThat(åpneAvklaringsbehovPåNyBehandling).describedAs("Sykdom skal være åpent avklaringsbehov.")
            .extracting(Avklaringsbehov::definisjon).contains(tuple(Definisjon.AVKLAR_SYKDOM))

        // Prøve å løse sykdomsvilkåret på nytt
        revurdering = revurdering.løsSykdom()
        åpneAvklaringsbehovPåNyBehandling = hentÅpneAvklaringsbehov(revurdering.id)
        assertThat(åpneAvklaringsbehovPåNyBehandling.map { it.definisjon }).doesNotContain(Definisjon.AVKLAR_SYKDOM)
        assertThat(
            åpneAvklaringsbehovPåNyBehandling.map { it.definisjon }).contains(Definisjon.AVKLAR_BISTANDSBEHOV)
    }

    @Test
    fun `ny informasjon i tidligere steg skal tilbakeføre behandling som er på vent pga samordning`() {
        val fom = LocalDate.now().minusYears(1)
        val periode = Periode(fom, fom.plusYears(3))

        val sykePengerPeriode = Periode(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))

        val person = TestPersoner.STANDARD_PERSON().medSykepenger(
            listOf(
                TestPerson.Sykepenger(
                    grad = 50, periode = sykePengerPeriode
                )
            )
        )
        val ident = person.aktivIdent()
        var revurdering = revurderingEtterVentPåSamordning(ident, periode, sykePengerPeriode)

        // Verifiser at den er satt på vent
        var åpneAvklaringsbehovPåNyBehandling = hentÅpneAvklaringsbehov(revurdering.id)
        assertThat(
            åpneAvklaringsbehovPåNyBehandling.map { it.definisjon }).contains(Definisjon.SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT)

        // Nytt institusjonsopphold
        person.institusjonsopphold = listOf(
            InstitusjonsoppholdJSON(
                startdato = LocalDate.now().minusMonths(5),
                forventetSluttdato = LocalDate.now().plusMonths(4),
                institusjonstype = "FO",
                institusjonsnavn = "institusjon",
                organisasjonsnummer = "2334",
                kategori = "S",
            )
        )


        // prosesser på nytt
        nullstillInformasjonskravOppdatert(InformasjonskravNavn.INSTITUSJONSOPPHOLD, revurdering.sakId)
        revurdering = prosesserBehandling(revurdering)
        åpneAvklaringsbehovPåNyBehandling = hentÅpneAvklaringsbehov(revurdering.id)

        // Behandlingen tilbakeføres til EtAnnetStedSteg
        assertThat(revurdering.aktivtSteg()).isEqualTo(StegType.DU_ER_ET_ANNET_STED)
        assertThat(åpneAvklaringsbehovPåNyBehandling.map { it.definisjon }).contains(Definisjon.AVKLAR_SONINGSFORRHOLD)
    }

    private fun revurderingEtterVentPåSamordning(
        ident: Ident,
        periode: Periode,
        sykePengerPeriode: Periode
    ): Behandling {
        // Sender inn en søknad
        var behandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("20"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(0),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)

        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = behandling.løsSykdom().løsAvklaringsBehov(
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingLøsningDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null,
                    skalVurdereAapIOvergangTilUføre = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null
                ),
            ),
        ).løsAvklaringsBehov(
            RefusjonkravLøsning(
                listOf(
                    RefusjonkravVurderingDto(
                        harKrav = true, fom = LocalDate.now(), tom = null, navKontor = "",
                    )
                )
            )
        ).løsAvklaringsBehov(
            avklaringsBehovLøsning = FritakMeldepliktLøsning(
                fritaksvurderinger = listOf(
                    FritaksvurderingDto(
                        harFritak = true,
                        fraDato = periode.fom,
                        begrunnelse = "...",
                    )
                ),
            ),
        ).kvalitetssikreOk()
            .løsAvklaringsBehov(
                FastsettBeregningstidspunktLøsning(
                    beregningVurdering = BeregningstidspunktVurderingDto(
                        begrunnelse = "Trenger hjelp fra Nav",
                        nedsattArbeidsevneDato = LocalDate.now(),
                        ytterligereNedsattArbeidsevneDato = null,
                        ytterligereNedsattBegrunnelse = null
                    ),
                ),
            ).løsForutgåendeMedlemskap()

        assertThat(hentÅpneAvklaringsbehov(behandling.id).map { it.definisjon }).containsExactly(Definisjon.AVKLAR_SAMORDNING_GRADERING)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarSamordningGraderingLøsning(
                vurderingerForSamordning = VurderingerForSamordning(
                    vurderteSamordningerData = listOf(
                        SamordningVurderingData(
                            ytelseType = Ytelse.SYKEPENGER,
                            periode = sykePengerPeriode,
                            gradering = 100,
                            kronesum = null,
                        )
                    ),
                    begrunnelse = "En god begrunnelse",
                    maksDatoEndelig = false,
                    fristNyRevurdering = LocalDate.now().plusMonths(1),
                ),
            ),
        )
        assertThat(hentÅpneAvklaringsbehov(behandling.id).map { it.definisjon }).isEqualTo(listOf(Definisjon.FORESLÅ_VEDTAK))

        behandling = løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())
        behandling = fattVedtakEllerSendRetur(behandling)

        val uthentetTilkjentYtelse =
            requireNotNull(dataSource.transaction { TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id) }) { "Tilkjent ytelse skal være beregnet her." }

        val periodeMedFullSamordning =
            uthentetTilkjentYtelse.map { Segment(it.periode, it.tilkjent.gradering.samordningGradering) }
                .let(::Tidslinje)
                .filter { it.verdi == Prosent.`100_PROSENT` }.helePerioden()

        // Verifiser at samordningen ble fanget opp
        assertThat(periodeMedFullSamordning.inneholder(sykePengerPeriode.tom)).isTrue
        // Verifiser at samordning med 100% strekker seg ut rettighetsperioden for å unngå feilaktig utbetaling fordi perioden har passert
        assertThat(periodeMedFullSamordning.tom).isEqualTo(periode.tom)
        behandling = behandling.løsVedtaksbrev()

        val nyesteBehandling = hentNyesteBehandlingForSak(behandling.sakId)
        val behandlingReferanse = behandling.referanse

        // Siden samordning overlappet, skal en revurdering opprettes med en gang
        assertThat(nyesteBehandling.referanse).isNotEqualTo(behandlingReferanse)
        assertThat(nyesteBehandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)

        util.ventPåSvar(sakId = behandling.sakId.id)
        return nyesteBehandling
    }
}