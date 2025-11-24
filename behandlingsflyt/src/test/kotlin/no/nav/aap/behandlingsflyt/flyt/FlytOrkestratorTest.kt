package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.flate.AvbrytRevurderingVurderingDto
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.flate.AvbrytRevurderingÅrsakDto
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.ÅrsakTilReturKode
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvbrytRevurderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarManuellInntektVurderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOvergangUføreLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOverstyrtForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarPeriodisertLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningUføreLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BekreftTotalvurderingKlageLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBehandlendeEnhetLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettFullmektigLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettPåklagetBehandlingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FullmektigLøsningDto
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.HåndterSvarFraAndreinstansLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.HåndterSvarFraAndreinstansLøsningDto
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.RefusjonkravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivForhåndsvarselKlageFormkravBrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.TrekkKlageLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.TrekkSøknadLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VentePåFristForhåndsvarselKlageFormkravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderFormkravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageKontorLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageNayLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.YrkesskadeSakDto
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.trekkklage.flate.TrekkKlageVurderingDto
import no.nav.aap.behandlingsflyt.behandling.trekkklage.flate.TrekkKlageÅrsakDto
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLandEllerLandMedAvtale
import no.nav.aap.behandlingsflyt.drift.Driftfunksjoner
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.IdentMedType
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.IdentType
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagevurderingKontorLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagevurderingNayLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetVedtakType
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.PeriodisertManuellVurderingForLovvalgMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.RegisterBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvarDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingerForBarnetillegg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarnDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.flate.OvergangUføreVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.SamordningVurderingData
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.VurderingerForSamordning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykepengerVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansKonsekvens
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.integrasjon.kabal.Fagsystem
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlingDetaljer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlingEventType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageUtfall
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlagebehandlingAvsluttetDetaljer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManueltOppgittBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OmgjøringKlageRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppgitteBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UtenlandsPeriodeDto
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.AVBRYT_REVURDERING
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.SEND_FORVALTNINGSMELDING
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.START_BEHANDLING
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.SØKNAD
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.VURDER_RETTIGHETSPERIODE
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.FormkravRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.barn.BarnRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.pip.PipRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.PersonNavn
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.*
import kotlin.reflect.KClass
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus

@Tag("motor")
@ParameterizedClass
@MethodSource("testData")
class FlytOrkestratorTest(unleashGateway: KClass<UnleashGateway>) : AbstraktFlytOrkestratorTest(unleashGateway) {
    companion object {
        @Suppress("unused")
        @JvmStatic
        fun testData(): List<Arguments> {
            return listOf(
                Arguments.of(FakeUnleash::class),
            )
        }
    }

    @Test
    fun `happy case førstegangsbehandling + revurder førstegangssøknad, gi sykepengererstatning hele perioden`() {
        val sak = happyCaseFørstegangsbehandling()

        revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom)
            .løsSykdomsvurderingBrev()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .describedAs("Siden vurderingenGjelderFra er lik kravdato (rettighetsperiode.fom), så kan man revurdere 11-13")
                    .containsExactlyInAnyOrder(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "...",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = true,
                        grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR
                    ),
                )
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.FORESLÅ_VEDTAK)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                val underveisGrunnlag = dataSource.transaction { connection ->
                    UnderveisRepositoryImpl(connection).hent(this.behandling.id)
                }

                assertThat(underveisGrunnlag.perioder).isNotEmpty
                assertThat(underveisGrunnlag.perioder).extracting<RettighetsType>(Underveisperiode::rettighetsType)
                    .allSatisfy { rettighetsType ->
                        assertThat(rettighetsType).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                    }
            }
    }

    @Test
    fun `happy case førstegangsbehandling + revurder førstegangssøknad, nei på viss varighet, nei på 11-13 - avslag`() {
        val sak = happyCaseFørstegangsbehandling()
        val behandling = revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom, false)

        behandling
            .løsSykdomsvurderingBrev()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .describedAs("Siden vurderingenGjelderFra er lik kravdato (rettighetsperiode.fom), så kan man revurdere 11-13")
                    .containsExactlyInAnyOrder(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
            .løsAvklaringsBehov(
                // Vi svarer nei på rett til sykepengererstatning
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "HAR IKKE RETT",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = false,
                        grunn = null
                    ),
                )
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.FORESLÅ_VEDTAK)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                val underveisGrunnlag = dataSource.transaction { connection ->
                    UnderveisRepositoryImpl(connection).hent(this.behandling.id)
                }

                assertThat(underveisGrunnlag.perioder).isNotEmpty
                assertThat(underveisGrunnlag.perioder).extracting<RettighetsType>(Underveisperiode::rettighetsType)
                    .describedAs("Ingen perioder skal være oppfylt.")
                    .allSatisfy { rettighetsType ->
                        assertThat(rettighetsType).isEqualTo(null)
                    }
            }
    }

    @Test
    fun `innvilge som student`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        var (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            periode = periode,
            søknad = TestSøknader.SØKNAD_STUDENT
        )

        behandling = behandling
            .løsAvklaringsBehov(
                AvklarStudentLøsning(
                    studentvurdering = StudentVurderingDTO(
                        begrunnelse = "...",
                        harAvbruttStudie = true,
                        godkjentStudieAvLånekassen = true,
                        avbruttPgaSykdomEllerSkade = true,
                        harBehovForBehandling = true,
                        avbruttStudieDato = LocalDate.now().minusMonths(3),
                        avbruddMerEnn6Måneder = true
                    ),
                )
            )
            .løsRefusjonskrav()
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                val vilkår = dataSource.transaction { VilkårsresultatRepositoryImpl(it).hent(behandling.id) }
                val v = vilkår.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
                assertThat(v.harPerioderSomErOppfylt()).isTrue

                val underveisperioder =
                    dataSource.transaction { UnderveisRepositoryImpl(it).hent(behandling.id).perioder }

                assertThat(underveisperioder.map { it.rettighetsType }).allSatisfy { rettighetstype ->
                    assertThat(rettighetstype).isEqualTo(RettighetsType.STUDENT)
                }
            }
    }

    @Test
    fun `revurdere sykepengeerstatning - skal ikke trigge 11-13 om gjelderFra ikke er kravdato`() {
        val sak = happyCaseFørstegangsbehandling()
        val gjelderFra = sak.rettighetsperiode.fom.plusMonths(1)

        revurdereFramTilOgMedSykdom(sak, gjelderFra)
            .løsBistand()
            .løsSykdomsvurderingBrev()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .describedAs("Siden vurderingenGjelderFra ikke er lik kravdato (rettighetsperiode.fom), så skal man ikke vurdere 11-13")
                    .containsExactlyInAnyOrder(Definisjon.FATTE_VEDTAK)  // ingen avklaringsbehov løst av NAY, gå rett til fatte vedtak
            }
            .fattVedtak()
            .medKontekst {
                val underveisGrunnlag = dataSource.transaction { connection ->
                    UnderveisRepositoryImpl(connection).hent(this.behandling.id)
                }

                assertThat(underveisGrunnlag.perioder).isNotEmpty
                assertThat(underveisGrunnlag.perioder).extracting<RettighetsType>(Underveisperiode::rettighetsType)
                    .allSatisfy { rettighetsType ->
                        assertThat(rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)
                    }
            }
    }

    @Test
    fun `hopper over foreslå vedtak-steg når revurdering ikke skal innom NAY`() {
        val sak = happyCaseFørstegangsbehandling()
        // Revurdering av sykdom uten 11-13
        revurdereFramTilOgMedSykdom(
            sak = sak,
            gjelderFra = sak.rettighetsperiode.fom,
            vissVarighet = true
        )
            .løsBistand()
            .løsSykdomsvurderingBrev()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov.map { it.definisjon }).describedAs {
                    "Revurdering av sykdom skal gå rett til beslutter når ingen avklaringsbehov trenger å løses av NAY"
                }.containsExactly(Definisjon.FATTE_VEDTAK)
            }
    }

    @Test
    fun `revurdering skal innom foreslå vedtak-steg når NAY-saksbehandler har løst avklaringsbehov`() {
        val sak = happyCaseFørstegangsbehandling()
        // Revurdering som krever 11-13-vurdering
        revurdereFramTilOgMedSykdom(
            sak = sak,
            gjelderFra = sak.rettighetsperiode.fom,
            vissVarighet = false
        )
            .løsSykdomsvurderingBrev()
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "test",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = true,
                        grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR
                    ),
                    behovstype = Definisjon.AVKLAR_SYKEPENGEERSTATNING.kode
                )
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov.map { it.definisjon }).describedAs {
                    "Revurdering av sykdom skal innom foreslå vedtak-steg når vurdering av sykepengeerstatning er gjort av NAY"
                }.containsExactly(Definisjon.FORESLÅ_VEDTAK)
            }
    }

    @Test
    fun `barnetillegg gis fram til 18 år`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        val barnfødseldato = LocalDate.now().minusYears(17)

        val barnIdent = genererIdent(barnfødseldato)
        val person = TestPersoner.STANDARD_PERSON().medBarn(
            listOf(
                TestPerson(
                    identer = setOf(barnIdent),
                    fødselsdato = Fødselsdato(barnfødseldato),
                ),
            )
        )

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )
        behandling
            .løsSykdom(periode.fom)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(fom)
            .løsAndreStatligeYtelser()
            .løsBarnetillegg()

        val barn = dataSource.transaction {
            BarnRepositoryImpl(it).hent(behandling.id)
        }

        // Verifiser at barn faktisk blir fanget opp
        assertThat(barn)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes("[a-zA-Z]+\\.id")
            .ignoringCollectionOrder()
            .isEqualTo(
                BarnGrunnlag(
                    registerbarn = RegisterBarn(
                        id = -1,
                        barn = listOf(Barn(BarnIdentifikator.BarnIdent(barnIdent), Fødselsdato(barnfødseldato)))
                    ),
                    oppgitteBarn = null,
                    vurderteBarn = null,
                    saksbehandlerOppgitteBarn = null
                )
            )

        val uthentetTilkjentYtelse =
            requireNotNull(dataSource.transaction { TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id) })
            { "Tilkjent ytelse skal være beregnet her." }

        val barnetillegg = uthentetTilkjentYtelse.map { Segment(it.periode, it.tilkjent.barnetillegg) }.let(::Tidslinje)

        val barnBlirAttenPå = barnfødseldato.plusYears(18)

        val periodeBarnUnderAtten = Periode(periode.fom, barnBlirAttenPå.minusDays(1))
        val barnErAtten = barnetillegg.begrensetTil(periodeBarnUnderAtten)

        assertThat(barnErAtten.segmenter()).isNotEmpty
        // Verifiser at barnetillegg kun gis fram til barnet er 18 år
        assertTidslinje(barnErAtten, periodeBarnUnderAtten to {
            assertThat(it).isEqualTo(Beløp(37))
        })

        val periodeBarnOverAtten = Periode(barnBlirAttenPå, periode.tom)
        val barnErOverAtten = barnetillegg.begrensetTil(periodeBarnOverAtten)
        assertThat(barnErOverAtten.segmenter()).isNotEmpty
        // Verifiser at barnetillegg er null etter fylte 18 år
        assertTidslinje(barnErOverAtten, periodeBarnOverAtten to {
            assertThat(it).isEqualTo(Beløp(0))
        })
    }

    @Test
    fun `barnetillegg gis ikke for gamle barn`() {
        val fom = LocalDate.now()
        val periode = Periode(fom, fom.plusYears(3))

        val ungtBarnFødselsdato = LocalDate.now().minusYears(7)
        val gammeltBarnFødselsdato = LocalDate.now().minusYears(20)

        val person = TestPersoner.STANDARD_PERSON().medBarn(
            listOf(
                TestPerson(
                    identer = setOf(Ident("aaa")),
                    fødselsdato = Fødselsdato(ungtBarnFødselsdato),
                ),
                TestPerson(
                    identer = setOf(Ident("ccc")),
                    fødselsdato = Fødselsdato(gammeltBarnFødselsdato),
                ),
            )
        )

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )
        behandling
            .løsSykdom(periode.fom)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(fom)
            .løsAndreStatligeYtelser()
            .løsBarnetillegg()

        val barn = dataSource.transaction {
            BarnRepositoryImpl(it).hent(behandling.id)
        }

        // Verifiser at barn faktisk blir fanget opp
        assertThat(barn)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFieldsMatchingRegexes("[a-zA-Z]+\\.id").isEqualTo(
                BarnGrunnlag(
                    registerbarn = RegisterBarn(
                        id = -1,
                        barn = person.barn.map { Barn(BarnIdentifikator.BarnIdent(it.aktivIdent()), it.fødselsdato) }),
                    oppgitteBarn = null,
                    vurderteBarn = null,
                    saksbehandlerOppgitteBarn = null
                )
            )

        val uthentetTilkjentYtelse =
            requireNotNull(dataSource.transaction { TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id) })
            { "Tilkjent ytelse skal være beregnet her." }

        val barnetillegg = uthentetTilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

        val begrensetTilRettighetsperioden = barnetillegg.begrensetTil(periode)
        assertThat(begrensetTilRettighetsperioden.segmenter()).isNotEmpty
        assertThat(begrensetTilRettighetsperioden.helePerioden()).isEqualTo(periode)

        // Skal kun gi barnetillegg for det unge barnet
        assertTidslinje(begrensetTilRettighetsperioden, periode to {
            assertThat(it.barnetillegg).isEqualTo(Beløp(37))
            assertThat(it.antallBarn).isEqualTo(1)
        })
    }

    @Test
    fun `oppgir manuelle barn, avklarer dem`() {
        val fom = LocalDate.now()
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val manueltBarnIPDL = TestPerson(
            navn = PersonNavn("Yousef", "Yosso"),
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(12))
        )
        // Dette gjør at flyten finner barnet i PDL (via FakeServers)
        FakePersoner.leggTil(manueltBarnIPDL)

        val barnNavn = "Gregor Gorgh"
        val barnAlder = LocalDate.now().minusYears(17)
        val søknad = TestSøknader.SØKNAD_MED_BARN(
            listOf(
                Pair(
                    manueltBarnIPDL.navn.toString(),
                    manueltBarnIPDL.fødselsdato.toLocalDate()
                ),
                Pair(
                    barnNavn,
                    barnAlder
                ),
            )
        )

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
            søknad = søknad,
        )
        behandling
            .løsSykdom(periode.fom)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).containsExactly(Definisjon.AVKLAR_BARNETILLEGG)
            }
            .løsAvklaringsBehov(
                AvklarBarnetilleggLøsning(
                    vurderingerForBarnetillegg = VurderingerForBarnetillegg(
                        vurderteBarn = listOf(
                            VurdertBarnDto(
                                ident = null,
                                navn = barnNavn,
                                fødselsdato = barnAlder,
                                vurderinger = listOf(
                                    VurderingAvForeldreAnsvarDto(
                                        fraDato = periode.fom,
                                        harForeldreAnsvar = true,
                                        begrunnelse = "bra forelder"
                                    )
                                )
                            )
                        ),
                        emptyList()
                    ),
                ),
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon })
                    .describedAs("Vi avklarte bare ett barn, behovet skal fortsatt være åpent")
                    .containsExactly(Definisjon.AVKLAR_BARNETILLEGG)

            }
            .løsAvklaringsBehov(
                AvklarBarnetilleggLøsning(
                    vurderingerForBarnetillegg = VurderingerForBarnetillegg(
                        vurderteBarn = listOf(
                            VurdertBarnDto(
                                ident = null,
                                navn = barnNavn,
                                fødselsdato = barnAlder,
                                vurderinger = listOf(
                                    VurderingAvForeldreAnsvarDto(
                                        fraDato = periode.fom,
                                        harForeldreAnsvar = true,
                                        begrunnelse = "bra forelder"
                                    )
                                )
                            ),
                            VurdertBarnDto(
                                ident = null,
                                navn = manueltBarnIPDL.navn.toString(),
                                fødselsdato = manueltBarnIPDL.fødselsdato.toLocalDate(),
                                vurderinger = listOf(
                                    VurderingAvForeldreAnsvarDto(
                                        fraDato = periode.fom,
                                        harForeldreAnsvar = true,
                                        begrunnelse = "bra forelder"
                                    )
                                )
                            )
                        ),
                        emptyList()
                    ),
                ),
            )
            .løsAndreStatligeYtelser()
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).containsExactly(Definisjon.FORESLÅ_VEDTAK)

                val tilkjentYtelse =
                    dataSource.transaction { TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id) }
                        .orEmpty().map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                val periodeMedBarneTilleggForToBarn =
                    tilkjentYtelse.filter { it.verdi.barnetillegg.verdi.toDouble() > 70 }.helePerioden()
                val periodeMedBarneTilleggForEttBarn =
                    tilkjentYtelse.filter { it.verdi.barnetillegg.verdi.toDouble() < 40 }.helePerioden()

                // Barnet er 18 fram til periode.fom.plusYears(1).minusDays(1)
                assertThat(periodeMedBarneTilleggForToBarn).isEqualTo(
                    Periode(
                        periode.fom,
                        periode.fom.plusYears(1).minusDays(1)
                    )
                )
                assertThat(periodeMedBarneTilleggForEttBarn).isEqualTo(Periode(periode.fom.plusYears(1), periode.tom))

                assertTidslinje(
                    tilkjentYtelse,
                    periodeMedBarneTilleggForToBarn to {
                        assertThat(it.antallBarn).isEqualTo(2)
                    },
                    periodeMedBarneTilleggForEttBarn to {
                        assertThat(it.antallBarn).isEqualTo(1)
                    })
            }
    }

    @Test
    fun `innvilge v yrkesskadegrunnlag`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        val person = TestPersoner.PERSON_MED_YRKESSKADE().medBarn(
            listOf(
                TestPerson(
                    identer = setOf(Ident("1234123")),
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(3)),
                )
            )
        )

        var (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            periode = periode,
        )

        // Sender inn en søknad
        behandling
            .løsSykdom(sak.rettighetsperiode.fom) // erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true) // erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .leggTilVurderingsbehov(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_YRKESSKADE)
        sak.sendInnMeldekort(
            journalpostId = JournalpostId("220"),
            mottattTidspunkt = LocalDateTime.now(),
            timerArbeidet = Periode(LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(3))
                .dager()
                .associateWith { 0.0 }
        )

        behandling = behandling.kvalitetssikreOk()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Ikke årsakssammenheng",
                        relevanteSaker = emptyList(),
                        relevanteYrkesskadeSaker = emptyList(),
                        andelAvNedsettelsen = null,
                        erÅrsakssammenheng = true
                    )
                ),
            )
            .løsBeregningstidspunkt()
            .løsYrkesskadeInntekt(person.yrkesskade)
            // Skal ikke løse forutgående medlemsskap
            .løsOppholdskrav(fom)
            .løsBarnetillegg()
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                // Saken står til To-trinnskontroll hos beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.FATTE_VEDTAK) }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)
                // Venter på at brevet skal fullføres
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.SKRIV_VEDTAKSBREV) }
            }
            .løsVedtaksbrev()
            .medKontekst {
                val brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }

        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)

        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }
            .allMatch { vilkårsperiode -> vilkårsperiode.innvilgelsesårsak == Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG }
    }

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        val person = TestPersoner.PERSON_MED_YRKESSKADE().medBarn(
            listOf(
                TestPerson(
                    identer = setOf(Ident("1234123")),
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(3)),
                )
            )
        ).medUføre(Prosent(50))

        var (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            periode = periode,
        )

        // Sender inn en søknad
        behandling
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
                assertThat(åpneAvklaringsbehov).isNotEmpty()
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .leggTilVurderingsbehov(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_YRKESSKADE)
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()

        sak.sendInnMeldekort(
            journalpostId = JournalpostId("220"),
            mottattTidspunkt = LocalDateTime.now(),
            timerArbeidet = Periode(LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(3))
                .dager()
                .associateWith { 0.0 }
        )

        behandling = behandling.kvalitetssikreOk()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Ikke årsakssammenheng",
                        relevanteSaker = emptyList(),
                        relevanteYrkesskadeSaker = emptyList(),
                        andelAvNedsettelsen = null,
                        erÅrsakssammenheng = false
                    )
                ),
            )
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(fom)
            .løsBarnetillegg()
            .løsAvklaringsBehov(
                AvklarSamordningUføreLøsning(
                    samordningUføreVurdering = SamordningUføreVurderingDto(
                        begrunnelse = "Samordnet med uføre",
                        vurderingPerioder = listOf(
                            SamordningUføreVurderingPeriodeDto(
                                virkningstidspunkt = sak.rettighetsperiode.fom, uføregradTilSamordning = 45
                            )
                        )
                    )
                )
            ).løsAndreStatligeYtelser()

            .medKontekst {
                // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .beslutterGodkjennerIkke(returVed = Definisjon.AVKLAR_SYKDOM)
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Ikke årsakssammenheng",
                        relevanteSaker = emptyList(),
                        relevanteYrkesskadeSaker = emptyList(),
                        andelAvNedsettelsen = null,
                        erÅrsakssammenheng = false
                    )
                )
            )
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(fom)
            .løsAvklaringsBehov(
                AvklarSamordningUføreLøsning(
                    samordningUføreVurdering = SamordningUføreVurderingDto(
                        begrunnelse = "Samordnet med uføre",
                        vurderingPerioder = listOf(
                            SamordningUføreVurderingPeriodeDto(
                                virkningstidspunkt = sak.rettighetsperiode.fom, uføregradTilSamordning = 45
                            )
                        )
                    )
                )
            )
            .medKontekst {
                // Saken er tilbake til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.FORESLÅ_VEDTAK) }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                // Saken står til To-trinnskontroll hos beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.FATTE_VEDTAK) }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)
            }

        // Skal feile dersom man prøver å sende til beslutter etter at vedtaket er fattet
        val avklaringsbehovFeil = assertThrows<UgyldigForespørselException> {
            løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())
        }
        assertThat(avklaringsbehovFeil.message).contains("Forsøker å løse avklaringsbehov FORESLÅ_VEDTAK(kode='5098') som er definert i et steg før nåværende steg[BREV]")
        val vedtak = hentVedtak(behandling.id)
        assertThat(vedtak.vedtakstidspunkt.toLocalDate()).isToday

        val brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)

        // Det er bestilt vedtaksbrev som er klar for forhåndsvisning og editering
        assertThat(brevbestilling.status).isEqualTo(
            no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR
        )

        behandling = behandling.medKontekst {
            // Venter på at brevet skal fullføres
            assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.SKRIV_VEDTAKSBREV) }
        }
            .løsVedtaksbrev()
            .medKontekst {
                val brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
                // Brevet er fullført
                assertThat(brevbestilling.status).isEqualTo(
                    no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT
                )
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }

        // Henter vurder alder-vilkår
        // Assert utfall
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(aldersvilkår.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }

        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }

        val underveisGrunnlag = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hent(behandling.id)
        }

        assertThat(underveisGrunnlag.perioder).isNotEmpty
        assertThat(underveisGrunnlag.perioder.any { it.arbeidsgradering.gradering.prosentverdi() > 0 }).isTrue()

        // Saken er avsluttet, så det skal ikke være flere åpne avklaringsbehov
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).isEmpty()

        sak.sendInnSøknad(
            søknad = TestSøknader.STANDARD_SØKNAD,
            mottattTidspunkt = LocalDateTime.now().minusMonths(3),
            journalpostId = JournalpostId("299"),
        ).medKontekst {
            assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
            assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
        }
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    sykdomsvurderinger = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("1349532")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            vurderingenGjelderFra = null,
                        )
                    )
                ),
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).anySatisfy { it.definisjon == Definisjon.AVKLAR_BISTANDSBEHOV }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
    }

    @Test
    fun `skal ikke vise avklaringsbehov for yrkesskade ved avslag i tidligere steg`() {
        val personMedYrkesskade = TestPersoner.PERSON_MED_YRKESSKADE()
        val (_, behandling) = sendInnFørsteSøknad(
            person = personMedYrkesskade,
        )

        val oppdatertBehandling = behandling
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    sykdomsvurderinger = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er ikke syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("1231299")),
                            harSkadeSykdomEllerLyte = false,
                            vurderingenGjelderFra = null,
                            erArbeidsevnenNedsatt = null,
                            erSkadeSykdomEllerLyteVesentligdel = null,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = null,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = null,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            yrkesskadeBegrunnelse = null,
                        )
                    )
                )
            )
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_AVSLAG)

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `ved revurdering med yrkesskade og årsakssammenheng må både beregningstidspunkt og yrkesskadeinntekt avklares ved revurdering av beregning`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val person = TestPersoner.PERSON_MED_YRKESSKADE()
        // Sender inn en søknad
        val (sak, behandling) = sendInnFørsteSøknad(
            periode = periode,
            person = person,
            søknad = TestSøknader.STANDARD_SØKNAD
        )

        val oppdatertBehandling = behandling
            .løsSykdom(periode.fom)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Veldig relevante",
                        relevanteSaker = person.yrkesskade.map { it.saksreferanse },
                        relevanteYrkesskadeSaker = person.yrkesskade.map {
                            YrkesskadeSakDto(
                                it.saksreferanse,
                                null,
                            )
                        },
                        andelAvNedsettelsen = 50,
                        erÅrsakssammenheng = true
                    )
                )
            )
            .løsBeregningstidspunkt()
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsOppholdskrav(periode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_INNVILGELSE)

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)

        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_BEREGNING),
        )
            .løsBeregningstidspunkt()
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)


        assertThat(revurdering.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `ved revurdering med yrkesskade skal både årsakssammenheng og yrkesskadeinntekt avklares hvis årsak revurder yrkesskade`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val person = TestPersoner.PERSON_MED_YRKESSKADE()
        // Sender inn en søknad
        val (sak, behandling) = sendInnFørsteSøknad(
            periode = periode,
            person = person,
            søknad = TestSøknader.STANDARD_SØKNAD
        )

        val oppdatertBehandling = behandling
            .løsSykdom(periode.fom)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Veldig relevante",
                        relevanteSaker = person.yrkesskade.map { it.saksreferanse },
                        relevanteYrkesskadeSaker = person.yrkesskade.map {
                            YrkesskadeSakDto(
                                it.saksreferanse,
                                null,
                            )
                        },
                        andelAvNedsettelsen = 50,
                        erÅrsakssammenheng = true
                    )
                )
            )
            .løsBeregningstidspunkt()
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsOppholdskrav(periode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_INNVILGELSE)

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)

        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_YRKESSKADE),
        )
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Veldig relevante",
                        relevanteSaker = person.yrkesskade.map { it.saksreferanse },
                        relevanteYrkesskadeSaker = person.yrkesskade.map {
                            YrkesskadeSakDto(
                                it.saksreferanse,
                                null,
                            )
                        },
                        andelAvNedsettelsen = 50,
                        erÅrsakssammenheng = true
                    )
                )
            )
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)


        assertThat(revurdering.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `ved revurdering med yrkesskade hvor yrkesskade fjernes må forutgående medlemskap vurderes da den igjen er aktuell`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val person = TestPersoner.PERSON_MED_YRKESSKADE()
        // Sender inn en søknad
        val (sak, behandling) = sendInnFørsteSøknad(
            periode = periode,
            person = person,
            søknad = TestSøknader.SØKNAD_INGEN_MEDLEMSKAP
        )

        val oppdatertBehandling = behandling
            .løsLovvalg(periode.fom)
            .løsSykdom(periode.fom)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Ikke lenger relevant, dette var feil i fgbh",
                        relevanteSaker = person.yrkesskade.map { it.saksreferanse },
                        relevanteYrkesskadeSaker = person.yrkesskade.map {
                            YrkesskadeSakDto(
                                it.saksreferanse,
                                null,
                            )
                        },
                        andelAvNedsettelsen = 50,
                        erÅrsakssammenheng = true
                    )
                )
            )
            .løsBeregningstidspunkt()
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsOppholdskrav(periode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_INNVILGELSE)

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)

        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_YRKESSKADE),
        )
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Ikke lenger relevant, dette var feil i fgbh",
                        relevanteSaker = person.yrkesskade.map { it.saksreferanse },
                        relevanteYrkesskadeSaker = emptyList(),
                        andelAvNedsettelsen = null,
                        erÅrsakssammenheng = false
                    )
                )
            )
            .løsForutgåendeMedlemskap()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)

        assertThat(revurdering.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `kan trekke søknad som har passert manuelt vurdert lovvalg`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val (_, behandling) = sendInnFørsteSøknad(
            periode = periode,
            søknad = SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("NEI", "NEI", "NEI", null, null)
            )
        )

        behandling.medKontekst {
            assertTrue(åpneAvklaringsbehov.all { Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP == it.definisjon })
        }
            // Løs lovvalg
            .løsLovvalg(periode.fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactly(Definisjon.AVKLAR_SYKDOM)
            }
            // Trekk søknad
            .leggTilVurderingsbehov(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SØKNAD_TRUKKET)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .contains(Definisjon.VURDER_TREKK_AV_SØKNAD)
            }
            .løsAvklaringsBehov(TrekkSøknadLøsning(begrunnelse = "trekker søknaden"))
            .medKontekst {
                assertThat(åpneAvklaringsbehov).isEmpty()
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }
    }

    @Test
    fun `kan trekke søknad som har passert forutgående medlemskap`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val (_, behandling) = sendInnFørsteSøknad(
            periode = periode,
            søknad = SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("NEI", "NEI", "NEI", null, null)
            )
        )

        behandling
            .løsLovvalg(periode.fom)
            // Løs fram til forutgående
            .løsFramTilForutgåendeMedlemskap()
            .medKontekst {
                assertThat(åpneAvklaringsbehov)
                    .extracting<Definisjon> { it.definisjon }
                    .contains(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
            }
            // Løs forutgående
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(periode.fom)
            // Trekk søknad
            .leggTilVurderingsbehov(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SØKNAD_TRUKKET)
            .medKontekst {
                assertThat(åpneAvklaringsbehov)
                    .extracting<Definisjon> { it.definisjon }
                    .contains(Definisjon.VURDER_TREKK_AV_SØKNAD)
            }
            .løsAvklaringsBehov(TrekkSøknadLøsning(begrunnelse = "trekker søknaden"))
            .medKontekst {
                assertThat(åpneAvklaringsbehov).isEmpty()
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }
    }

    @Test
    fun `trukket søknad blokkerer nye ytelsesbehandlinger`() {
        // Sender inn en søknad
        var (sak, behandling) = sendInnFørsteSøknad()

        løsSykdom(behandling)
            .leggTilVurderingsbehov(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SØKNAD_TRUKKET)
            .medKontekst {
                assertThat(åpneAvklaringsbehov)
                    .extracting<Definisjon> { it.definisjon }
                    .contains(Definisjon.VURDER_TREKK_AV_SØKNAD)
            }
            .løsAvklaringsBehov(TrekkSøknadLøsning(begrunnelse = "trekker søknaden"))
            .medKontekst {
                assertThat(åpneAvklaringsbehov).isEmpty()
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }

        behandling = sak.sendInnSøknad(
            SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            ),
        )

        assertThat(behandling.forrigeBehandlingId).isNull()
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade - yrkesskade har årsakssammenheng`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val person = TestPersoner.PERSON_MED_YRKESSKADE()

        // Sender inn en søknad
        var (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )
        behandling = behandling
            .medKontekst {
                assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
            }
            .leggTilVurderingsbehov(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_YRKESSKADE)
            .løsSykdom(
                vurderingGjelderFra = sak.rettighetsperiode.fom,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true
            )
            .løsBistand()
            .løsAvklaringsBehov(
                RefusjonkravLøsning(
                    listOf(
                        RefusjonkravVurderingDto(
                            harKrav = true,
                            fom = LocalDate.now(),
                            tom = null,
                            navKontor = "",
                        )
                    )
                )
            )
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Veldig relevante",
                        relevanteSaker = person.yrkesskade.map { it.saksreferanse },
                        relevanteYrkesskadeSaker = person.yrkesskade.map {
                            YrkesskadeSakDto(
                                it.saksreferanse,
                                null,
                            )
                        },
                        andelAvNedsettelsen = 50,
                        erÅrsakssammenheng = true
                    )
                )
            )
            .løsBeregningstidspunkt()
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .medKontekst {
                // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { avklaringsbehov ->
                    assertThat(avklaringsbehov.definisjon).isEqualTo(
                        Definisjon.FORESLÅ_VEDTAK
                    )
                }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                // Saken står til To-trinnskontroll hos beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.FATTE_VEDTAK) }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .medKontekst {
                val brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)
                // Det er bestilt vedtaksbrev som er klar for forhåndsvisning og editering
                assertThat(brevBestilling.status).isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR)

                // Venter på at brevet skal fullføres
                assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.SKRIV_VEDTAKSBREV) }

                val vilkårsresultat = dataSource.transaction { VilkårsresultatRepositoryImpl(it).hent(behandling.id) }
                    .finnVilkår(Vilkårtype.SYKDOMSVILKÅRET).tidslinje().komprimer()

                assertTidslinje(vilkårsresultat, periode to {
                    assertThat(it.innvilgelsesårsak).isEqualTo(Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG)
                })
            }
            .løsVedtaksbrev()
            .medKontekst {
                // Brevet er fullført
                val brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
                assertThat(brevBestilling.status).isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT)
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(aldersvilkår.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }

        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }

        // Verifiser at beregningsgrunnlaget er av type yrkesskade
        dataSource.transaction {
            assertThat(BeregningsgrunnlagRepositoryImpl(it).hentHvisEksisterer(behandling.id)?.javaClass).isEqualTo(
                GrunnlagYrkesskade::class.java
            )
        }
    }

    @Test
    fun `ikke sykdom viss varighet, men skal få innvilget 11-13 sykepengererstatning`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Sender inn en søknad
        var (sak, behandling) = sendInnFørsteSøknad(periode = periode, mottattTidspunkt = periode.fom.atStartOfDay())

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = behandling.løsAvklaringsBehov(
            AvklarSykdomLøsning(
                sykdomsvurderinger = listOf(
                    SykdomsvurderingLøsningDto(
                        begrunnelse = "Er syk nok",
                        dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                        harSkadeSykdomEllerLyte = true,
                        erSkadeSykdomEllerLyteVesentligdel = true,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                        // Nei på denne gir mulighet til å innvilge på 11-13
                        erNedsettelseIArbeidsevneAvEnVissVarighet = false,
                        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                        erArbeidsevnenNedsatt = true,
                        yrkesskadeBegrunnelse = null,
                        vurderingenGjelderFra = null,
                    )
                )
            ),
        )
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .contains(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "...",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = true,
                        grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR
                    ),
                )
            )
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(periode.fom)
            .løsAndreStatligeYtelser()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { avklaringsbehov ->
                    assertThat(avklaringsbehov.definisjon).isEqualTo(
                        Definisjon.FORESLÅ_VEDTAK
                    )
                }
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning()).medKontekst {
                // Saken står til To-trinnskontroll hos beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)

                val resultat = dataSource.transaction {
                    ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id)
                }
                assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)
            }
            .løsVedtaksbrev()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)

                val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
                val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

                assertThat(sykdomsvilkåret.vilkårsperioder()).hasSize(1).first()
                    .extracting(Vilkårsperiode::erOppfylt, Vilkårsperiode::innvilgelsesårsak)
                    .containsExactly(true, Innvilgelsesårsak.SYKEPENGEERSTATNING)

                val resultat =
                    dataSource.transaction {
                        ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(
                            behandling.id
                        )
                    }
                assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)

                assertTidslinje(
                    vilkårsresultat.rettighetstypeTidslinje(),
                    periode to {
                        assertThat(it).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                    })
            }

        // Verifisere at det går an å kun 1 mnd med sykepengeerstatning
        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
        )
            .løsSykdom(vurderingGjelderFra = LocalDate.now().plusMonths(1))
            .løsBistand()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov.map { it.definisjon }).containsOnly(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)
            }
            .løsSykdomsvurderingBrev()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov.map { it.definisjon }).containsOnly(Definisjon.FATTE_VEDTAK)

                val underveisTidslinje = dataSource.transaction {
                    UnderveisRepositoryImpl(it).hent(this.behandling.id).perioder
                }.map { Segment(it.periode, it) }.let(::Tidslinje)

                val oppfyltPeriode = underveisTidslinje.filter { it.verdi.rettighetsType != null }.helePerioden()
                val vilkårsresultat = hentVilkårsresultat(behandlingId = this.behandling.id)

                assertThat(oppfyltPeriode.fom).isEqualTo(periode.fom)
                // Oppfylt ut rettighetsperioden
                assertThat(oppfyltPeriode.tom).isEqualTo(periode.tom)

                assertTidslinje(
                    vilkårsresultat.rettighetstypeTidslinje(),
                    Periode(periode.fom, periode.fom.plusMonths(1).minusDays(1)) to {
                        assertThat(it).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                    },
                    Periode(periode.fom.plusMonths(1), periode.tom) to {
                        assertThat(it).isEqualTo(RettighetsType.BISTANDSBEHOV)
                    }
                )
            }
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_ENDRING)

        // Revurdering nr 2, innvilger sp-erstatning på nytt
        val revurdering2 = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
        )
            .medKontekst {
                assertThat(this.behandling.id).isNotEqualTo(revurdering.id)
            }
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    sykdomsvurderinger = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = null,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            vurderingenGjelderFra = LocalDate.now().plusMonths(2),
                        )
                    )
                )
            )
            // Nei på 11-6
            .løsBistand(false)
            .løsOvergangUføre()
            .apply {
                if (gatewayProvider.provide<UnleashGateway>().isEnabled(BehandlingsflytFeature.OvergangArbeid)) {
                    løsOvergangArbeid(Utfall.IKKE_OPPFYLT)
                }
            }
            .løsSykdomsvurderingBrev()
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "...",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = true,
                        grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR,
                        gjelderFra = LocalDate.now().plusMonths(2)
                    ),
                )
            )
            .medKontekst {
                val underveisTidslinje = dataSource.transaction {
                    UnderveisRepositoryImpl(it).hent(this.behandling.id).perioder
                }.map { Segment(it.periode, it) }.let(::Tidslinje)

                val oppfyltPeriode = underveisTidslinje.filter { it.verdi.rettighetsType != null }.helePerioden()
                val vilkårsresultat = hentVilkårsresultat(behandlingId = this.behandling.id)

                assertThat(
                    underveisTidslinje.map { it.rettighetsType }.segmenter().map { it.verdi }).containsSubsequence(
                    RettighetsType.SYKEPENGEERSTATNING, RettighetsType.BISTANDSBEHOV, RettighetsType.SYKEPENGEERSTATNING
                )

                assertThat(oppfyltPeriode.fom).isEqualTo(periode.fom)
                // Oppfylt ut rettighetsperioden
                assertThat(oppfyltPeriode.tom).isEqualTo(periode.tom)

                assertTidslinje(
                    vilkårsresultat.rettighetstypeTidslinje(),
                    Periode(periode.fom, periode.fom.plusMonths(1).minusDays(1)) to {
                        assertThat(it).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                    },
                    Periode(periode.fom.plusMonths(1), periode.fom.plusMonths(2).minusDays(1)) to {
                        assertThat(it).isEqualTo(RettighetsType.BISTANDSBEHOV)
                    },
                    Periode(periode.fom.plusMonths(2), periode.tom) to {
                        assertThat(it).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                    }
                )
            }
    }

    @Test
    fun `avslag på 11-6 er også inngang til 11-13 og 11-18`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Sender inn en søknad
        var (_, behandling) = sendInnFørsteSøknad(periode = periode)
        behandling
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    sykdomsvurderinger = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123128")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            vurderingenGjelderFra = null,
                        )
                    )
                ),
            )
            // Nei på 11-6
            .løsBistand(false)
            .løsAvklaringsBehov(
                AvklarOvergangUføreLøsning(
                    OvergangUføreVurderingLøsningDto(
                        begrunnelse = "Løsning",
                        brukerHarSøktOmUføretrygd = true,
                        brukerHarFåttVedtakOmUføretrygd = "NEI",
                        brukerRettPåAAP = true,
                        virkningsdato = LocalDate.now(),
                        overgangBegrunnelse = null
                    )
                )
            )
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKEPENGEERSTATNING) }
            }
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "...",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = true,
                        grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR
                    ),
                )
            )
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(periode.fom)
            .løsAndreStatligeYtelser()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)
            }

        var resultat =
            dataSource.transaction { ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id) }
        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)


        behandling = behandling.løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_11_18)

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        // Sjekker at sykdomsvilkåret ble oppfylt med innvilgelsesårsak satt til 11-13.
        assertThat(sykdomsvilkåret.vilkårsperioder()).hasSize(1).first()
            .extracting(Vilkårsperiode::erOppfylt, Vilkårsperiode::innvilgelsesårsak)
            .containsExactly(true, Innvilgelsesårsak.SYKEPENGEERSTATNING)

        resultat =
            dataSource.transaction { ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id) }

        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)

        assertTidslinje(
            vilkårsresultat.rettighetstypeTidslinje(),
            Periode(periode.fom, periode.fom.plusMonths(8).minusDays(1)) to {
                assertThat(it).isEqualTo(RettighetsType.VURDERES_FOR_UFØRETRYGD)
            },
            Periode(periode.fom.plusMonths(8), periode.tom) to {
                assertThat(it).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
            }
        )
    }

    @Test
    fun `11-18 uføre underveis i en behandling`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val virkningsdatoOvergangUføre = periode.fom.plusDays(20)

        // Sender inn en søknad
        var (_, behandling) = sendInnFørsteSøknad(periode = periode)
        behandling
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    sykdomsvurderinger = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123128")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            vurderingenGjelderFra = null,
                        )
                    )
                ),
            )
            // Nei på 11-6
            .løsAvklaringsBehov(
                AvklarBistandsbehovLøsning(
                    bistandsVurdering = BistandVurderingLøsningDto(
                        begrunnelse = "Overgang uføre",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = "Yep"
                    ),
                ),
            )
            .løsAvklaringsBehov(
                AvklarOvergangUføreLøsning(
                    OvergangUføreVurderingLøsningDto(
                        begrunnelse = "Løsning",
                        brukerHarSøktOmUføretrygd = true,
                        brukerHarFåttVedtakOmUføretrygd = "NEI",
                        brukerRettPåAAP = true,
                        virkningsdato = virkningsdatoOvergangUføre,
                        overgangBegrunnelse = null
                    )
                )
            )
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKEPENGEERSTATNING) }
            }
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "...",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = false,
                        grunn = null
                    ),
                )
            )
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(periode.fom)
            .løsAndreStatligeYtelser()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)
            }

        var resultat =
            dataSource.transaction { ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id) }
        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)

        behandling = behandling.løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_11_18)

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val overgangUføreVilkår = vilkårsresultat.finnVilkår(Vilkårtype.OVERGANGUFØREVILKÅRET)

        // Sjekker at overgangUføreVilkår ble oppfylt med innvilgelsesårsak satt til 11-13.
        assertTidslinje(
            overgangUføreVilkår.tidslinje(), Periode(periode.fom, virkningsdatoOvergangUføre.minusDays(1)) to {
                assertThat(it.utfall).isEqualTo(Utfall.IKKE_VURDERT)
            },
            Periode(virkningsdatoOvergangUføre, virkningsdatoOvergangUføre.plusMonths(8).minusDays(1)) to {
                assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT)
            },
            Periode(virkningsdatoOvergangUføre.plusMonths(8), periode.tom) to {
                assertThat(it.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
            })

        resultat =
            dataSource.transaction { ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id) }

        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)
        assertTidslinje(
            vilkårsresultat.rettighetstypeTidslinje(),
            Periode(virkningsdatoOvergangUføre, virkningsdatoOvergangUføre.plusMonths(8).minusDays(1)) to {
                assertThat(it).isEqualTo(RettighetsType.VURDERES_FOR_UFØRETRYGD)
            },
        )
    }

    @Test
    fun `ved avslag på 11-5 hoppes det rett til beslutter-steget`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        var (_, behandling) = sendInnFørsteSøknad(periode = periode)

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarSykdomLøsning(
                sykdomsvurderinger = listOf(
                    SykdomsvurderingLøsningDto(
                        begrunnelse = "Er ikke syk nok",
                        dokumenterBruktIVurdering = listOf(JournalpostId("1231299")),
                        harSkadeSykdomEllerLyte = false,
                        vurderingenGjelderFra = null,
                        erArbeidsevnenNedsatt = null,
                        erSkadeSykdomEllerLyteVesentligdel = null,
                        erNedsettelseIArbeidsevneAvEnVissVarighet = null,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = null,
                        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                        yrkesskadeBegrunnelse = null,
                    )
                )
            ),
        )
        //behandling = løsOvergangUføre(behandling)
        behandling = løsSykdomsvurderingBrev(behandling)

        behandling = kvalitetssikreOk(behandling)

        // Saken står til To-trinnskontroll hos beslutter
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.FATTE_VEDTAK) }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsFatteVedtak(behandling)
        assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)

        val vedtak = hentVedtak(behandling.id)
        assertThat(vedtak.vedtakstidspunkt.toLocalDate()).isToday

        val resultat = dataSource.transaction {
            ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id)
        }
        assertThat(resultat).isEqualTo(Resultat.AVSLAG)
        val brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_AVSLAG)

        behandling =
            løsAvklaringsBehov(behandling, vedtaksbrevLøsning(brevbestilling.referanse.brevbestillingReferanse))
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        // Verifiserer at sykdomsvilkåret ikke er oppfylt
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder()).hasSize(1)
            .allMatch { vilkårsperiode -> !vilkårsperiode.erOppfylt() }

        // Saken er avsluttet, så det skal ikke være flere åpne avklaringsbehov
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).isEmpty()
    }

    @Test
    fun `ved førstegangsbehandling i steget for sykdom skal sykdomsvurdering for brev vises etter refusjonskrav er løst og før kvalitetsvurdering`() {
        var (sak, førstegangsbehandling) = sendInnFørsteSøknad()

        førstegangsbehandling = førstegangsbehandling
            .medKontekst {
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand()
            .løsRefusjonskrav()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy {
                    assertThat(it.definisjon).isEqualTo(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)
                }
            }
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()

        assertThat(førstegangsbehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `ved revurdering i steget for sykdom skal sykdomsvurdering for brev vises etter refusjonskrav`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val person = TestPersoner.STANDARD_PERSON()
        val sak = happyCaseFørstegangsbehandling(periode.fom, person)

        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
        )
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.SKRIV_SYKDOMSVURDERING_BREV) }
            }
            .løsSykdomsvurderingBrev() // Krever ikke kvalitetskontroll i revurdering
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)

        assertThat(revurdering.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `ved førstegangsbehandling med avslag før sykdom ved manglende medlemskap er ikke sykdomsvurdering for brev aktuelt`() {
        val (sak, behandling) = sendInnFørsteSøknad(
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3)),
            søknad = TestSøknader.SØKNAD_INGEN_MEDLEMSKAP
        )

        val oppdatertBehandling = behandling
            .løsLovvalg(sak.rettighetsperiode.fom, false)
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_AVSLAG)

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `ved førstegangsbehandling og annet lovvalgsland settes saken på vent`() {
        val (sak, behandling) = sendInnFørsteSøknad(
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3)),
            søknad = TestSøknader.SØKNAD_INGEN_MEDLEMSKAP
        )

        val oppdatertBehandling = behandling
            .løsAvklaringsBehov(
                AvklarPeriodisertLovvalgMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = sak.rettighetsperiode.fom,
                            tom = sak.rettighetsperiode.tom,
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.ESP),
                            medlemskap = null,
                        )
                    )
                )
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VENTE_PÅ_UTENLANDSK_VIDEREFØRING_AVKLARING)
            }

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.UTREDES)
    }

    @Test
    fun `ved revurdering med avslag før sykdom ved manglende medlemskap er ikke sykdomsvurdering for brev aktuelt`() {
        val sak = happyCaseFørstegangsbehandling(LocalDate.now())
        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP)
        )

        val oppdatertBehandling = revurdering
            .løsLovvalg(sak.rettighetsperiode.fom, false)
            .løsUtenSamordning()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `skal kunne gjennomføre førstegangsbehandling med periodisert lovvalg og medlemskap`() {
        var (sak, førstegangsbehandling) = sendInnFørsteSøknad(søknad = TestSøknader.SØKNAD_INGEN_MEDLEMSKAP)

        førstegangsbehandling = førstegangsbehandling
            .løsAvklaringsBehov(
                AvklarPeriodisertLovvalgMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = sak.rettighetsperiode.fom,
                            tom = sak.rettighetsperiode.fom.plusMonths(2),
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", false)
                        ),
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = sak.rettighetsperiode.fom.plusMonths(2).plusDays(1),
                            tom = null,
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", true)
                        )
                    )
                )
            )
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()

        // Virkningstidspunktet skal settes til datoen hvor medlemskap er oppfylt
        val vedtak = hentVedtak(førstegangsbehandling.id)
        assertThat(vedtak.virkningstidspunkt).isEqualTo(sak.rettighetsperiode.fom.plusMonths(2).plusDays(1))

        assertThat(førstegangsbehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `skal kunne gjennomføre førstegangsbehandling og revurdering hvor oppfylt lovvalg og medlemskap flyttes en mnd`() {
        var (sak, førstegangsbehandling) = sendInnFørsteSøknad(søknad = TestSøknader.SØKNAD_INGEN_MEDLEMSKAP)

        førstegangsbehandling = førstegangsbehandling
            .løsAvklaringsBehov(
                AvklarPeriodisertLovvalgMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = sak.rettighetsperiode.fom,
                            tom = sak.rettighetsperiode.fom.plusMonths(2),
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", false)
                        ),
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = sak.rettighetsperiode.fom.plusMonths(2).plusDays(1),
                            tom = null,
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", true)
                        )
                    )
                )
            )
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()

        assertThat(førstegangsbehandling.status()).isEqualTo(Status.AVSLUTTET)

        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP),
        )
            .løsAvklaringsBehov(
                AvklarPeriodisertLovvalgMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        // Skulle oppfylles en mnd tidligere
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = sak.rettighetsperiode.fom.plusMonths(1).plusDays(1),
                            tom = null,
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", true)
                        )
                    )
                )
            )
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand()
            .løsSykdomsvurderingBrev()
            .løsBeregningstidspunkt()
            .løsUtenSamordning()
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)

        // Virkningstidspunktet skal settes til datoen hvor medlemskap er oppfylt
        val vedtak = hentVedtak(revurdering.id)
        assertThat(vedtak.virkningstidspunkt).isEqualTo(sak.rettighetsperiode.fom.plusMonths(1).plusDays(1))

        assertThat(revurdering.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `skal kunne gjennomføre førstegangsbehandling og revurdering hvor rettighetsperiode endres og fører til avklaringsbehov for lovvalg og medlemskap`() {
        var (sak, førstegangsbehandling) = sendInnFørsteSøknad(søknad = TestSøknader.SØKNAD_INGEN_MEDLEMSKAP)

        førstegangsbehandling = førstegangsbehandling
            .løsAvklaringsBehov(
                AvklarPeriodisertLovvalgMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = sak.rettighetsperiode.fom,
                            tom = null,
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", true)
                        )
                    )
                )
            )
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()

        assertThat(førstegangsbehandling.status()).isEqualTo(Status.AVSLUTTET)

        val nyStartDato = sak.rettighetsperiode.fom.minusMonths(1)
        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.VURDER_RETTIGHETSPERIODE),
        )
            .løsRettighetsperiode(nyStartDato)
            .medKontekst {
                // Vi har ikke vurdert lovvalg og medlemskap for den utvidede perioden enda, så vi forventer et avklaringsbehov her
                assertThat(åpneAvklaringsbehov).anySatisfy { behov -> assertThat(behov.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP).isTrue() }
            }
            .løsAvklaringsBehov(
                AvklarPeriodisertLovvalgMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = nyStartDato,
                            tom = null,
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", true)
                        )
                    )
                )
            )
            .løsSykdom(nyStartDato)
            .løsBistand()
            .løsSykdomsvurderingBrev()
            .løsBeregningstidspunkt()
            .løsUtenSamordning()
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)

        assertThat(revurdering.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `to-trinn og ingen endring i gruppe etter sendt tilbake fra beslutter`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()
        // Sender inn en søknad
        val (_, behandling) = sendInnFørsteSøknad(
            periode = periode,
            person = person,
            søknad = TestSøknader.SØKNAD_STUDENT
        )
        behandling.medKontekst {
            assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
            assertThat(åpneAvklaringsbehov).isNotEmpty()
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }
            .løsAvklaringsBehov(
                AvklarStudentLøsning(
                    studentvurdering = StudentVurderingDTO(
                        begrunnelse = "Er student",
                        avbruttStudieDato = LocalDate.now(),
                        avbruddMerEnn6Måneder = true,
                        harBehovForBehandling = true,
                        harAvbruttStudie = true,
                        avbruttPgaSykdomEllerSkade = true,
                        godkjentStudieAvLånekassen = false,
                    )
                ),
            ).løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    sykdomsvurderinger = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Arbeidsevnen er nedsatt med mer enn halvparten",
                            dokumenterBruktIVurdering = listOf(JournalpostId("12312983")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            vurderingenGjelderFra = null,
                        )
                    )
                )
            ).løsBistand()

            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()

            .medKontekst {
                // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
                assertThat(åpneAvklaringsbehov).isNotEmpty()
                assertThat(åpneAvklaringsbehov).anySatisfy { behov -> assertThat(behov.definisjon == Definisjon.KVALITETSSIKRING).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .kvalitetssikreOk()
            .løsAvklaringsBehov(
                FastsettBeregningstidspunktLøsning(
                    beregningVurdering = BeregningstidspunktVurderingDto(
                        begrunnelse = "Trenger hjelp fra Nav",
                        nedsattArbeidsevneDato = LocalDate.now(),
                        ytterligereNedsattArbeidsevneDato = null,
                        ytterligereNedsattBegrunnelse = null
                    ),
                ),
            )
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(periode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                // Saken står til To-trinnskontroll hos beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .beslutterGodkjennerIkke(returVed = Definisjon.AVKLAR_SYKDOM)
            .medKontekst {
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }
            }.løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    sykdomsvurderinger = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123190923")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            vurderingenGjelderFra = null,
                        )
                    )
                ),
                ingenEndringIGruppe = true,
                bruker = Bruker("SAKSBEHANDLER")
            ).løsAvklaringsBehov(
                FastsettBeregningstidspunktLøsning(
                    beregningVurdering = BeregningstidspunktVurderingDto(
                        begrunnelse = "Trenger hjelp fra Nav",
                        nedsattArbeidsevneDato = LocalDate.now(),
                        ytterligereNedsattArbeidsevneDato = null,
                        ytterligereNedsattBegrunnelse = null
                    ),
                ),
                Bruker("SAKSBEHANDLER")
            ).løsForutgåendeMedlemskap()
            .løsOppholdskrav(periode.fom)
            .medKontekst {
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
                // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { behov -> assertThat(behov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                // Saken står til To-trinnskontroll hos beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .medKontekst {
                //Henter vurder alder-vilkår
                //Assert utfall
                val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
                val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

                assertThat(aldersvilkår.vilkårsperioder())
                    .hasSize(1)
                    .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }

                val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

                assertThat(sykdomsvilkåret.vilkårsperioder())
                    .hasSize(1)
                    .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }
            }
    }

    @Test
    fun `Når beslutter ikke godkjenner vurdering av samordning skal flyt tilbakeføres`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Sender inn en søknad
        var (sak, behandling) = sendInnFørsteSøknad(periode = periode)

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = behandling.løsAvklaringsBehov(
            AvklarSykdomLøsning(
                sykdomsvurderinger = listOf(
                    SykdomsvurderingLøsningDto(
                        begrunnelse = "Arbeidsevnen er nedsatt med mer enn halvparten",
                        dokumenterBruktIVurdering = listOf(JournalpostId("1231o9024")),
                        harSkadeSykdomEllerLyte = true,
                        erSkadeSykdomEllerLyteVesentligdel = true,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                        erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                        erArbeidsevnenNedsatt = true,
                        yrkesskadeBegrunnelse = null,
                        vurderingenGjelderFra = null,
                    )
                )
            )
        ).løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAvklaringsBehov(
                AvklarSamordningGraderingLøsning(
                    VurderingerForSamordning(
                        begrunnelse = "Sykepengervurdering",
                        maksDatoEndelig = true,
                        fristNyRevurdering = null,
                        vurderteSamordningerData = listOf(
                            SamordningVurderingData(
                                ytelseType = Ytelse.SYKEPENGER,
                                periode = Periode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusDays(5),
                                ),
                                gradering = 50,
                                manuell = true
                            )
                        ),
                    )
                )
            )
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .beslutterGodkjennerIkke(returVed = Definisjon.AVKLAR_SAMORDNING_GRADERING)
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
                // Avklar samordning gradering gjenåpnes, behandlingen står i samordning-steget
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SAMORDNING_GRADERING) }
                assertThat(this.behandling.aktivtSteg()).isEqualTo(StegType.SAMORDNING_GRADERING)
            }
    }

    @Test
    fun `Ikke oppfylt på grunn av alder på søknadstidspunkt`(hendelser: List<StoppetBehandling>) {
        var (_, behandling) = sendInnFørsteSøknad(person = TestPersoner.PERSON_FOR_UNG())

        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        val stegHistorikk = hentStegHistorikk(behandling.id)
        assertThat(stegHistorikk.map { it.steg() }).contains(StegType.BREV)
        assertThat(stegHistorikk.map { it.status() }).contains(StegStatus.AVKLARINGSPUNKT)

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(aldersvilkår.vilkårsperioder())
            .hasSize(1)
            .noneMatch { vilkårsperiodeForAlder -> vilkårsperiodeForAlder.erOppfylt() }

        val status = behandling.status()
        assertThat(status).isEqualTo(Status.IVERKSETTES)

        val brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_AVSLAG)
        behandling =
            løsAvklaringsBehov(behandling, vedtaksbrevLøsning(brevbestilling.referanse.brevbestillingReferanse))

        val behov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(behov).isEmpty()

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
        assertThat(hendelser.last().behandlingStatus).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Blir satt på vent for etterspørring av informasjon`() {
        val (sak, behandling) = sendInnFørsteSøknad()
        behandling.medKontekst {
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(Definisjon.AVKLAR_SYKDOM)
        }

        settBehandlingPåVent(
            behandling.id,
            BehandlingSattPåVent(
                frist = null,
                begrunnelse = "Avventer dokumentasjon",
                bruker = SYSTEMBRUKER,
                behandlingVersjon = behandling.versjon,
                grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER
            )
        )

        behandling.medKontekst {
            val frist = åpneAvklaringsbehov.first { it.erVentepunkt() }.frist()
            assertThat(frist).isNotNull

            assertThat(åpneAvklaringsbehov.map { it.definisjon })
                .hasSize(2)
                .containsExactlyInAnyOrder(Definisjon.MANUELT_SATT_PÅ_VENT, Definisjon.AVKLAR_SYKDOM)

        }

        sak.sendInnSøknad(
            SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "JA", "NEI", "NEI", null)
            ),
        ).medKontekst {
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            assertThat(åpneAvklaringsbehov.map { it.definisjon })
                .containsExactlyInAnyOrder(Definisjon.MANUELT_SATT_PÅ_VENT, Definisjon.AVKLAR_SYKDOM)
        }
    }

    @Test
    fun `Fjerner legeerklæring ventebehov ved mottak av avvist legeerklæring`() {
        // Oppretter vanlig søknad
        val (_, behandling) = sendInnFørsteSøknad()
        behandling.medKontekst {
            // Validér avklaring
            assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKDOM) }
        }

        // Oppretter bestilling av legeerklæring
        bestillLegeerklæring(behandling.id)
        motor.kjørJobber()

        behandling.medKontekst {
            assertThat(åpneAvklaringsbehov.all { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING })
        }

        // Send inn avvist legeerklæring
        val avvistLegeerklæringId = UUID.randomUUID().toString()
        dataSource.transaction { connection ->
            val flytJobbRepository = FlytJobbRepository(connection)
            flytJobbRepository.leggTil(
                HendelseMottattHåndteringJobbUtfører.nyJobb(
                    sakId = behandling.sakId,
                    dokumentReferanse = InnsendingReferanse(
                        InnsendingReferanse.Type.AVVIST_LEGEERKLÆRING_ID,
                        avvistLegeerklæringId
                    ),
                    brevkategori = InnsendingType.LEGEERKLÆRING_AVVIST,
                    kanal = Kanal.DIGITAL,
                    mottattTidspunkt = LocalDateTime.now()
                )
            )
        }
        motor.kjørJobber()

        // Validér avklaring
        behandling.medKontekst {
            val legeerklæringBestillingVenteBehov =
                åpneAvklaringsbehov.filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
            assertThat(legeerklæringBestillingVenteBehov.isEmpty()).isTrue()
        }
    }

    @Test
    fun `Fjerner legeerklæring ventebehov ved mottak av legeerklæring`() {
        // Oppretter vanlig søknad
        val (sak, behandling) = sendInnFørsteSøknad()

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKDOM) }

        // Oppretter bestilling av legeerklæring
        bestillLegeerklæring(behandling.id)
        motor.kjørJobber()

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.BESTILL_LEGEERKLÆRING) }

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov.all { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING })

        // Mottar legeerklæring
        val journalpostId = UUID.randomUUID().toString()
        dataSource.transaction { connection ->
            val flytJobbRepository = FlytJobbRepository(connection)
            flytJobbRepository.leggTil(
                HendelseMottattHåndteringJobbUtfører.nyJobb(
                    sakId = sak.id,
                    dokumentReferanse = InnsendingReferanse(
                        InnsendingReferanse.Type.JOURNALPOST,
                        journalpostId
                    ),
                    brevkategori = InnsendingType.LEGEERKLÆRING,
                    kanal = Kanal.DIGITAL,
                    mottattTidspunkt = LocalDateTime.now()
                )
            )
        }
        motor.kjørJobber()

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)

        val legeerklæringBestillingVenteBehov =
            åpneAvklaringsbehov.filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
        assertThat(legeerklæringBestillingVenteBehov.isEmpty()).isTrue()

    }

    @Test
    fun `Fjerner legeerklæring ventebehov ved mottak av dialogmelding`() {
        // Oppretter vanlig søknad
        val (sak, behandling) = sendInnFørsteSøknad()

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKDOM) }

        // Oppretter bestilling av legeerklæring
        bestillLegeerklæring(behandling.id)

        assertThat(hentÅpneAvklaringsbehov(behandling)).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.BESTILL_LEGEERKLÆRING) }

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov.all { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING })

        // Mottar dialogmelding
        val journalpostId = UUID.randomUUID().toString()
        dataSource.transaction { connection ->
            val flytJobbRepository = FlytJobbRepository(connection)
            flytJobbRepository.leggTil(
                HendelseMottattHåndteringJobbUtfører.nyJobb(
                    sakId = sak.id,
                    dokumentReferanse = InnsendingReferanse(
                        InnsendingReferanse.Type.JOURNALPOST,
                        journalpostId
                    ),
                    brevkategori = InnsendingType.DIALOGMELDING,
                    kanal = Kanal.DIGITAL,
                    mottattTidspunkt = LocalDateTime.now()
                )
            )
        }
        motor.kjørJobber()

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        val legeerklæringBestillingVenteBehov =
            åpneAvklaringsbehov.filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
        assertThat(legeerklæringBestillingVenteBehov.isEmpty()).isTrue()
    }

    @Test
    fun `Lager avklaringsbehov i medlemskap når kravene til manuell avklaring oppfylles`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = periode.fom.atStartOfDay(),
            periode = periode,
            søknad = SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("NEI", null, "JA", null, null)
            )
        )

        // Validér avklaring
        val åpenAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpenAvklaringsbehov)
            .extracting<Definisjon> { it.definisjon }
            .containsOnly(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)
    }

    @Test
    fun `Går automatisk forbi medlemskap når kravene til manuell avklaring ikke oppfylles`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        val behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        )

        // Validér avklaring
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })
    }

    @Test
    fun `Går videre i forutgåendemedlemskapsteget når manuell vurdering mottas`() {
        val ident = nyPerson(harYrkesskade = false, harUtenlandskOpphold = true)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        var behandling = sendInnSøknad(
            ident, periode,
            SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null),
            ),
        )

        behandling = løsFramTilForutgåendeMedlemskap(behandling, harYrkesskade = false)

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })

        // Trigger manuell vurdering
        behandling = løsAvklaringsBehov(
            behandling,
            AvklarForutgåendeMedlemskapLøsning(
                manuellVurderingForForutgåendeMedlemskap = ManuellVurderingForForutgåendeMedlemskapDto(
                    "begrunnelse", true, null, null
                ),
            ),
        )

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
    }

    @Test
    fun `Kan revurdere forutgående medlemskap med tidligere manuelle vurderinger`() {
        val person =
            FakePersoner.hentPerson(nyPerson(harYrkesskade = false, harUtenlandskOpphold = true).identifikator)!!
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        var (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
            søknad = SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null),
            ),
        )

        løsFramTilForutgåendeMedlemskap(behandling, harYrkesskade = false)

        // Validér avklaring
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })

        // Trigger manuell vurdering
        behandling = løsAvklaringsBehov(
            behandling,
            AvklarForutgåendeMedlemskapLøsning(
                manuellVurderingForForutgåendeMedlemskap = ManuellVurderingForForutgåendeMedlemskapDto(
                    "begrunnelse", true, null, null
                ),
            ),
        )
            .medKontekst { assertThat(this.åpneAvklaringsbehov).noneMatch { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP } }
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()

        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP),
        ).medKontekst {
            assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
            assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)

            assertThat(this.åpneAvklaringsbehov).anyMatch { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP }
        }
    }

    @Test
    fun `Oppfyller ikke forutgående medlemskap når unntak ikke oppfylles og ikke medlem i folketrygden`() {
        val ident = nyPerson(harYrkesskade = false, harUtenlandskOpphold = true)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        val behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto(
                    "JA", null, "NEI", null, null
                )
            )
        )
            .løsFramTilForutgåendeMedlemskap(harYrkesskade = false)
            .medKontekst {
                // Validér avklaring
                assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
            }
            // Trigger manuell vurdering
            .løsAvklaringsBehov(
                AvklarForutgåendeMedlemskapLøsning(
                    manuellVurderingForForutgåendeMedlemskap = ManuellVurderingForForutgåendeMedlemskapDto(
                        "begrunnelseforutgående",
                        false,
                        varMedlemMedNedsattArbeidsevne = false,
                        medlemMedUnntakAvMaksFemAar = null
                    )
                )
            )
            .medKontekst {
                val vilkårsResultat =
                    hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder()
                assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
                assertTrue(vilkårsResultat.none { it.erOppfylt() })
            }
    }

    @Test
    fun `Oppfyller forutgående medlemskap når unntak finnes`() {
        val ident = nyPerson(harYrkesskade = false, harUtenlandskOpphold = true)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto(
                    "JA", null, "NEI", null, null
                ),
            )
        )
            .løsFramTilForutgåendeMedlemskap(harYrkesskade = false).medKontekst {
                assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
            }
            // Trigger manuell vurdering
            .løsAvklaringsBehov(
                AvklarForutgåendeMedlemskapLøsning(
                    manuellVurderingForForutgåendeMedlemskap = ManuellVurderingForForutgåendeMedlemskapDto(
                        "begrunnelse", true, true, null
                    ),
                    behovstype = AvklaringsbehovKode.`5020`
                )
            )
            .medKontekst {
                assertTrue(this.åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
                // Validér riktig resultat
                val vilkårsResultat =
                    hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder()
                assertTrue(vilkårsResultat.all { it.erOppfylt() })
            }
    }

    @Test
    fun `Går forbi forutgåendemedlemskapsteget når yrkesskade eksisterer`() {
        val ident = nyPerson(harYrkesskade = true, harUtenlandskOpphold = false)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        var behandling = sendInnSøknad(
            ident, periode,
            SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "JA", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null),
            ),
        )
        behandling.leggTilVurderingsbehov(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_YRKESSKADE)
        behandling = løsFramTilForutgåendeMedlemskap(behandling = behandling, harYrkesskade = true)

        // Validér avklaring
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
    }

    @Test
    fun `Gir oppfylt når bruker ikke har lovvalgsland men oppfyller trygdeloven`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        var behandling = sendInnSøknad(
            ident, periode,
            SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto(
                    "JA", null, "JA", null,
                    listOf(
                        UtenlandsPeriodeDto(
                            "SWE",
                            LocalDate.now().plusMonths(1),
                            LocalDate.now().minusMonths(1),
                            "JA",
                            null,
                            LocalDate.now().plusMonths(1),
                            LocalDate.now().minusMonths(1),
                        )
                    )
                )
            )
        )

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })

        // Trigger manuell vurdering
        behandling = behandling.løsLovvalg(periode.fom)

        // Validér riktig resultat
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
        assertTrue(vilkårsResultat.all { it.erOppfylt() })
    }

    @Test
    fun `Gir avslag når bruker har annet lovvalgsland`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        var behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto(
                    "JA", null, "JA", null,
                    listOf(
                        UtenlandsPeriodeDto(
                            "SWE",
                            LocalDate.now().plusMonths(1),
                            LocalDate.now().minusMonths(1),
                            "JA",
                            null,
                            LocalDate.now().plusMonths(1),
                            LocalDate.now().minusMonths(1),
                        )
                    )
                ),
            )
        )

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })

        // Trigger manuell vurdering
        behandling = behandling
            .løsAvklaringsBehov(
                AvklarPeriodisertLovvalgMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = periode.fom,
                            tom = null,
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.DNK),
                            medlemskap = null
                        )
                    )
                )
            )

        // Validér riktig resultat
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })
        assertTrue(vilkårsResultat.none { it.erOppfylt() })
    }

    @Test
    fun `Gir avslag når bruker ikke er medlem i trygden`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        var behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto(
                    "JA", null, "JA", null,
                    listOf(
                        UtenlandsPeriodeDto(
                            "SWE",
                            LocalDate.now().plusMonths(1),
                            LocalDate.now().minusMonths(1),
                            "JA",
                            null,
                            LocalDate.now().plusMonths(1),
                            LocalDate.now().minusMonths(1),
                        )
                    )
                ),
            )
        )

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })

        // Trigger manuell vurdering
        behandling = behandling.løsLovvalg(periode.fom, false)

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov.none())

        // Validér riktig resultat
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        assertThat(vilkårsResultat).noneMatch { it.erOppfylt() }
        assertTrue(Avslagsårsak.IKKE_MEDLEM == vilkårsResultat.first().avslagsårsak)
    }

    @Test
    fun `Kan løse forutgående overstyringsbehov til ikke oppfylt`() {
        val ident = nyPerson(harYrkesskade = false, harUtenlandskOpphold = false)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        var behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        )

        behandling = løsFramTilForutgåendeMedlemskap(behandling, harYrkesskade = false)

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).noneMatch { it.definisjon == Definisjon.MANUELL_OVERSTYRING_MEDLEMSKAP }

        // Validér riktig resultat
        behandling = løsForutgåendeMedlemskap(behandling)
        var vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder()
        assertThat(vilkårsResultat).allMatch { it.erOppfylt() }

        behandling = løsAvklaringsBehov(
            behandling, AvklarOverstyrtForutgåendeMedlemskapLøsning(
                manuellVurderingForForutgåendeMedlemskap = ManuellVurderingForForutgåendeMedlemskapDto(
                    "because", false, false, false
                ),
            )
        )

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { Definisjon.MANUELL_OVERSTYRING_MEDLEMSKAP == it.definisjon })

        // Validér riktig resultat
        vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder()
        assertTrue(vilkårsResultat.none { it.erOppfylt() })
        assertThat(Avslagsårsak.IKKE_MEDLEM_FORUTGÅENDE).isEqualTo(vilkårsResultat.first().avslagsårsak)
    }

    @Test
    fun `Kan løse overstyringsbehov til ikke oppfylt`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        )
            .løsLovvalgOverstyrt(periode.fom, false)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .doesNotContain(Definisjon.MANUELL_OVERSTYRING_LOVVALG)


                // Validér riktig resultat
                val vilkårsResultat =
                    hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
                assertThat(vilkårsResultat).allMatch { !it.erOppfylt() }
                assertThat(vilkårsResultat.first().avslagsårsak).isEqualTo(Avslagsårsak.IKKE_MEDLEM)
            }
    }

    @Test
    fun `Kan løse overstyringsbehov til oppfylt`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        ).løsLovvalgOverstyrt(periode.fom, true)

        // Validér avklaring
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { Definisjon.MANUELL_OVERSTYRING_LOVVALG == it.definisjon })

        // Validér riktig resultat
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        val overstyrtManuellVurdering = dataSource.transaction {
            MedlemskapArbeidInntektRepositoryImpl(it).hentHvisEksisterer(behandling.id)?.vurderinger?.firstOrNull()?.overstyrt
        }
        assertTrue(vilkårsResultat.all { it.erOppfylt() })
        assertTrue(overstyrtManuellVurdering == true)
    }

    @Test
    fun `kan hente inn manuell inntektsdata i grunnlag og benytte i beregning`() {
        val ident = nyPerson(harYrkesskade = false, harUtenlandskOpphold = false, inntekter = mutableListOf())
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val nedsattDato = LocalDate.now()

        // Oppretter vanlig søknad
        val behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        )
            .løsFramTilGrunnlag(periode.fom)
            .løsAvklaringsBehov(
                FastsettBeregningstidspunktLøsning(
                    beregningVurdering = BeregningstidspunktVurderingDto(
                        begrunnelse = "Trenger hjelp fra Nav",
                        nedsattArbeidsevneDato = nedsattDato,
                        ytterligereNedsattArbeidsevneDato = null,
                        ytterligereNedsattBegrunnelse = null
                    ),
                ),
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov)
                    .extracting<Definisjon> { it.definisjon }
                    .containsOnly(Definisjon.FASTSETT_MANUELL_INNTEKT)

            }
            .løsAvklaringsBehov(
                AvklarManuellInntektVurderingLøsning(
                    manuellVurderingForManglendeInntekt = ManuellInntektVurderingDto(
                        begrunnelse = "Mangler ligning",
                        belop = BigDecimal(300000),
                    )
                )
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov)
                    .extracting<Definisjon> { it.definisjon }
                    .doesNotContain(Definisjon.FASTSETT_MANUELL_INNTEKT)
            }

        val beregningsGrunnlag = dataSource.transaction {
            BeregningsgrunnlagRepositoryImpl(it).hentHvisEksisterer(behandling.id) as Grunnlag11_19
        }

        val sisteInntekt =
            beregningsGrunnlag.inntekter().first { inntekt -> inntekt.år.value == nedsattDato.minusYears(1).year }

        assertThat(sisteInntekt)
            .extracting(GrunnlagInntekt::år, GrunnlagInntekt::inntektIKroner)
            .containsExactly(nedsattDato.minusYears(1).year.let { Year.of(it) }, Beløp(BigDecimal(300000)))
    }

    @Test
    fun `henter ikke inn manuell inntektsdata i grunnlag om inntektsdata eksisterer fra før`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        val behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        ).løsFramTilGrunnlag(periode.fom)

        løsAvklaringsBehov(
            behandling,
            FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurderingDto(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = LocalDate.now(),
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            ),
        )
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.FASTSETT_MANUELL_INNTEKT })
    }

    @Test
    fun `kan tilbakeføre behandling til start`() {
        // Given:
        val (_, behandling) = sendInnFørsteSøknad()

        behandling.medKontekst {
            assertThat(åpneAvklaringsbehov)
                .extracting<Definisjon> { it.definisjon }
                .containsOnly(Definisjon.AVKLAR_SYKDOM)
        }

        val antallKjøringerVurderRettighetsperiode = dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).hentStegHistorikk(behandling.id)
                .count { it.steg() == VURDER_RETTIGHETSPERIODE && it.status() == StegStatus.AVSLUTTER }
        }

        // When:
        dataSource.transaction { connection ->
            val driftfunksjoner = Driftfunksjoner(postgresRepositoryRegistry.provider(connection), gatewayProvider)
            driftfunksjoner.kjørFraSteg(behandling, VURDER_RETTIGHETSPERIODE)
        }

        // Then:
        // Har kjørt steget vi rullet tilbake til én gang til
        val antallKjøringerVurderRettighetsperiodeEtterTilbakekjøring = dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).hentStegHistorikk(behandling.id)
                .count { it.steg() == VURDER_RETTIGHETSPERIODE && it.status() == StegStatus.AVSLUTTER }
        }
        assertThat(antallKjøringerVurderRettighetsperiodeEtterTilbakekjøring)
            .isEqualTo(antallKjøringerVurderRettighetsperiode + 1)

        // Tilbake til AVKLAR_SYKDOM
        dataSource.transaction { connection ->
            assertThat(BehandlingRepositoryImpl(connection).hentAktivtSteg(behandling.id))
                .extracting { it?.steg() }
                .isEqualTo(StegType.AVKLAR_SYKDOM)
        }
    }

    @Test
    fun `Teste Klageflyt - Omgjøring av 22-13 og revurdering genereres `() {
        val periode = Periode(LocalDate.now().minusMonths(3), LocalDate.now().plusYears(3))

        // Avslås pga. alder
        val (sak, avslåttFørstegang) = sendInnFørsteSøknad(
            person = TestPersoner.PERSON_FOR_UNG(),
            periode = periode,
            mottattTidspunkt = periode.fom.atStartOfDay(),
            søknad = SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            )
        )
        assertThat(avslåttFørstegang)
            .describedAs("Førstegangsbehandlingen skal være satt som avsluttet")
            .extracting { b -> b.status().erAvsluttet() }.isEqualTo(true)

        val kravMottatt = LocalDate.now().minusMonths(1)
        val klagebehandling = sak.sendInnKlage(
            journalpostId = JournalpostId("4002"),
            mottattTidspunkt = LocalDateTime.now().minusMonths(3),
            klage = KlageV0(kravMottatt = kravMottatt),
        )
        assertThat(klagebehandling.referanse).isNotEqualTo(avslåttFørstegang.referanse)
        assertThat(klagebehandling.typeBehandling()).isEqualTo(TypeBehandling.Klage)

        dataSource.transaction { connection ->
            val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)
            val klageDokumenter =
                mottattDokumentRepository.hentDokumenterAvType(klagebehandling.id, InnsendingType.KLAGE)
            assertThat(klageDokumenter).hasSize(1)
            assertThat(klageDokumenter.first().strukturertDokument).isNotNull
            assertThat(klageDokumenter.first().strukturerteData<KlageV0>()?.data?.kravMottatt).isEqualTo(kravMottatt)
        }

        // PåklagetBehandlingSteg
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.FASTSETT_PÅKLAGET_BEHANDLING)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettPåklagetBehandlingLøsning(
                påklagetBehandlingVurdering = PåklagetBehandlingVurderingLøsningDto(
                    påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
                    påklagetBehandling = avslåttFørstegang.referanse.referanse,
                )
            )
        )

        // FullmektigSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.FASTSETT_FULLMEKTIG)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettFullmektigLøsning(
                fullmektigVurdering = FullmektigLøsningDto(
                    harFullmektig = false
                )
            )
        )

        // FormkravSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_FORMKRAV)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderFormkravLøsning(
                formkravVurdering = FormkravVurderingLøsningDto(
                    begrunnelse = "Begrunnelse",
                    erBrukerPart = true,
                    erFristOverholdt = false,
                    likevelBehandles = true,
                    erKonkret = true,
                    erSignert = true
                )
            )
        )

        // BehandlendeEnhetSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.FASTSETT_BEHANDLENDE_ENHET)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettBehandlendeEnhetLøsning(
                behandlendeEnhetVurdering = BehandlendeEnhetLøsningDto(
                    skalBehandlesAvNay = true,
                    skalBehandlesAvKontor = true
                )
            )
        )

        // KlagebehandlingKontorSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_KLAGE_KONTOR)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderKlageKontorLøsning(
                klagevurderingKontor = KlagevurderingKontorLøsningDto(
                    begrunnelse = "Begrunnelse",
                    notat = null,
                    innstilling = KlageInnstilling.OMGJØR,
                    vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_22_13),
                    vilkårSomOpprettholdes = emptyList()
                )
            )
        )

        // KvalitetssikringsSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.KVALITETSSIKRING)

        kvalitetssikreOk(klagebehandling)

        // KlagebehandlingNaySteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_KLAGE_NAY)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderKlageNayLøsning(
                klagevurderingNay = KlagevurderingNayLøsningDto(
                    begrunnelse = "Begrunnelse",
                    notat = null,
                    innstilling = KlageInnstilling.OMGJØR,
                    vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_22_13),
                    vilkårSomOpprettholdes = emptyList()
                )
            )
        )


        // Totalvurdering
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.BEKREFT_TOTALVURDERING_KLAGE)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = BekreftTotalvurderingKlageLøsning()
        )

        // FatteVedtakSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.FATTE_VEDTAK)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FatteVedtakLøsning(
                vurderinger = listOf(
                    TotrinnsVurdering(
                        begrunnelse = "Begrunnelse",
                        godkjent = true,
                        definisjon = Definisjon.VURDER_KLAGE_NAY.kode,
                        grunner = emptyList(),
                    ),
                    TotrinnsVurdering(
                        begrunnelse = "Begrunnelse",
                        godkjent = true,
                        definisjon = Definisjon.VURDER_KLAGE_KONTOR.kode,
                        grunner = emptyList(),
                    ),
                    TotrinnsVurdering(
                        begrunnelse = "Begrunnelse",
                        godkjent = true,
                        definisjon = Definisjon.VURDER_FORMKRAV.kode,
                        grunner = emptyList()
                    )
                )
            ),
            Bruker("X123456")
        )


        motor.kjørJobber()

        // OmgjøringSteg
        dataSource.transaction { connection ->
            val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)

            val omgjøringKlageRevurdering = mottattDokumentRepository.hentDokumenterAvType(
                klagebehandling.sakId,
                InnsendingType.OMGJØRING_KLAGE_REVURDERING
            )

            assertThat(omgjøringKlageRevurdering).hasSize(1).first()
                .extracting(MottattDokument::strukturertDokument)
                .isNotNull
            assertThat(
                omgjøringKlageRevurdering.first().strukturerteData<OmgjøringKlageRevurderingV0>()?.data?.beskrivelse
            ).isEqualTo("Revurdering etter klage som tas til følge. Følgende vilkår omgjøres: § 22-13")
        }

        val revurdering = hentSisteOpprettedeBehandlingForSak(klagebehandling.sakId, listOf(TypeBehandling.Revurdering))
        assertThat(revurdering.vurderingsbehov()).containsExactly(
            VurderingsbehovMedPeriode(type = Vurderingsbehov.VURDER_RETTIGHETSPERIODE, periode = null),
            VurderingsbehovMedPeriode(type = Vurderingsbehov.HELHETLIG_VURDERING, periode = null)
        )

        dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            assertThat(behandlingRepo.hent(revurdering.id).aktivtSteg()).isEqualTo(VURDER_RETTIGHETSPERIODE)

            assertThat(behandlingRepo.hentStegHistorikk(revurdering.id).map { tilstand -> tilstand.steg() }
                .distinct()).containsExactlyElementsOf(
                listOf(
                    START_BEHANDLING, SEND_FORVALTNINGSMELDING, AVBRYT_REVURDERING, SØKNAD, VURDER_RETTIGHETSPERIODE
                )
            )

        }

        // OpprettholdelseSteg
        val steghistorikk = hentStegHistorikk(klagebehandling.id)
        assertThat(steghistorikk)
            .anySatisfy { assertThat(it.steg() == StegType.OMGJØRING && it.status() == StegStatus.AVSLUTTER).isTrue }

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(0)

    }


    @Test
    fun `Teste Klageflyt - Omgjøring av kapitel 2 og revurdering genereres `() {
        val person = TestPersoner.PERSON_FOR_UNG()
        val ident = person.aktivIdent()

        val periode = Periode(LocalDate.now().minusMonths(3), LocalDate.now().plusYears(3))

        // Avslås pga. alder
        val avslåttFørstegang = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            )
        )
        assertThat(avslåttFørstegang)
            .describedAs("Førstegangsbehandlingen skal være satt som avsluttet")
            .extracting { b -> b.status().erAvsluttet() }.isEqualTo(true)
        val kravMottatt = LocalDate.now().minusMonths(1)
        val sak = hentSak(avslåttFørstegang)
        val klagebehandling = sak.sendInnKlage(
            journalpostId = JournalpostId("4002"),
            mottattTidspunkt = LocalDateTime.now().minusMonths(3),
            klage = KlageV0(kravMottatt = kravMottatt),
        )
            .medKontekst {
                val klagebehandling = this.behandling
                assertThat(this.behandling.referanse).isNotEqualTo(avslåttFørstegang.referanse)
                assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Klage)

                dataSource.transaction { connection ->
                    val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)
                    val klageDokumenter =
                        mottattDokumentRepository.hentDokumenterAvType(klagebehandling.id, InnsendingType.KLAGE)
                    assertThat(klageDokumenter).hasSize(1)
                    assertThat(klageDokumenter.first().strukturertDokument).isNotNull
                    assertThat(klageDokumenter.first().strukturerteData<KlageV0>()?.data?.kravMottatt).isEqualTo(
                        kravMottatt
                    )
                }

                // PåklagetBehandlingSteg
                assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
                    .isEqualTo(Definisjon.FASTSETT_PÅKLAGET_BEHANDLING)
            }
            .løsAvklaringsBehov(
                avklaringsBehovLøsning = FastsettPåklagetBehandlingLøsning(
                    påklagetBehandlingVurdering = PåklagetBehandlingVurderingLøsningDto(
                        påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
                        påklagetBehandling = avslåttFørstegang.referanse.referanse,
                    )
                )
            )
            .medKontekst {
                // FullmektigSteg
                assertThat(åpneAvklaringsbehov).hasSize(1)
                assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.FASTSETT_FULLMEKTIG)
            }
            .løsAvklaringsBehov(
                avklaringsBehovLøsning = FastsettFullmektigLøsning(
                    fullmektigVurdering = FullmektigLøsningDto(
                        harFullmektig = false
                    )
                )
            ).medKontekst {
                // FormkravSteg
                assertThat(åpneAvklaringsbehov).hasSize(1)
                assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_FORMKRAV)
            }
            .løsAvklaringsBehov(
                avklaringsBehovLøsning = VurderFormkravLøsning(
                    formkravVurdering = FormkravVurderingLøsningDto(
                        begrunnelse = "Begrunnelse",
                        erBrukerPart = true,
                        erFristOverholdt = false,
                        likevelBehandles = true,
                        erKonkret = true,
                        erSignert = true
                    )
                )
            )
            .medKontekst {
                // BehandlendeEnhetSteg
                assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
                    .isEqualTo(Definisjon.FASTSETT_BEHANDLENDE_ENHET)
            }
            .løsAvklaringsBehov(
                avklaringsBehovLøsning = FastsettBehandlendeEnhetLøsning(
                    behandlendeEnhetVurdering = BehandlendeEnhetLøsningDto(
                        skalBehandlesAvNay = true,
                        skalBehandlesAvKontor = true
                    )
                )
            )
            .medKontekst {
                // KlagebehandlingKontorSteg
                assertThat(åpneAvklaringsbehov).hasSize(1)
                assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_KLAGE_KONTOR)
            }
            .løsAvklaringsBehov(
                avklaringsBehovLøsning = VurderKlageKontorLøsning(
                    klagevurderingKontor = KlagevurderingKontorLøsningDto(
                        begrunnelse = "Begrunnelse",
                        notat = null,
                        innstilling = KlageInnstilling.OMGJØR,
                        vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_KAPITTEL_2),
                        vilkårSomOpprettholdes = emptyList()
                    )
                )
            )
            .medKontekst {
                // KvalitetssikringsSteg
                assertThat(åpneAvklaringsbehov).hasSize(1)
                assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.KVALITETSSIKRING)
            }
            .kvalitetssikreOk()
            .medKontekst {
                // KlagebehandlingNaySteg
                assertThat(åpneAvklaringsbehov).hasSize(1)
                assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_KLAGE_NAY)
            }
            .løsAvklaringsBehov(
                avklaringsBehovLøsning = VurderKlageNayLøsning(
                    klagevurderingNay = KlagevurderingNayLøsningDto(
                        begrunnelse = "Begrunnelse",
                        notat = null,
                        innstilling = KlageInnstilling.OMGJØR,
                        vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_KAPITTEL_2),
                        vilkårSomOpprettholdes = emptyList()
                    )
                )
            )
            .medKontekst {
                // Totalvurdering
                assertThat(åpneAvklaringsbehov).hasSize(1)
                assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.BEKREFT_TOTALVURDERING_KLAGE)
            }
            .løsAvklaringsBehov(
                avklaringsBehovLøsning = BekreftTotalvurderingKlageLøsning()
            )
            .medKontekst {
                // FatteVedtakSteg
                assertThat(åpneAvklaringsbehov).hasSize(1)
                assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.FATTE_VEDTAK)
            }
            .løsAvklaringsBehov(
                avklaringsBehovLøsning = FatteVedtakLøsning(
                    vurderinger = listOf(
                        TotrinnsVurdering(
                            begrunnelse = "Begrunnelse",
                            godkjent = true,
                            definisjon = Definisjon.VURDER_KLAGE_NAY.kode,
                            grunner = emptyList(),
                        ),
                        TotrinnsVurdering(
                            begrunnelse = "Begrunnelse",
                            godkjent = true,
                            definisjon = Definisjon.VURDER_KLAGE_KONTOR.kode,
                            grunner = emptyList(),
                        ),
                        TotrinnsVurdering(
                            begrunnelse = "Begrunnelse",
                            godkjent = true,
                            definisjon = Definisjon.VURDER_FORMKRAV.kode,
                            grunner = emptyList()
                        )
                    )
                ),
                Bruker("X123456")
            )

        // OmgjøringSteg
        dataSource.transaction { connection ->
            val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)

            val omgjøringKlageRevurdering = mottattDokumentRepository.hentDokumenterAvType(
                klagebehandling.sakId,
                InnsendingType.OMGJØRING_KLAGE_REVURDERING
            )

            assertThat(omgjøringKlageRevurdering).hasSize(1).first()
                .extracting(MottattDokument::strukturertDokument)
                .isNotNull
            assertThat(
                omgjøringKlageRevurdering.first().strukturerteData<OmgjøringKlageRevurderingV0>()?.data?.beskrivelse
            ).isEqualTo("Revurdering etter klage som tas til følge. Følgende vilkår omgjøres: Kapittel 2")
        }

        val revurdering = hentSisteOpprettedeBehandlingForSak(klagebehandling.sakId, listOf(TypeBehandling.Revurdering))
        assertThat(revurdering.vurderingsbehov()).containsExactly(VurderingsbehovMedPeriode(Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP))

        // OpprettholdelseSteg
        val steghistorikk = hentStegHistorikk(klagebehandling.id)
        assertThat(steghistorikk)
            .anySatisfy { assertThat(it.steg() == StegType.OMGJØRING && it.status() == StegStatus.AVSLUTTER).isTrue }

    }


    @Test
    fun `Teste Klageflyt`() {
        val person = TestPersoner.PERSON_FOR_UNG()
        val ident = person.aktivIdent()

        val periode = Periode(LocalDate.now().minusMonths(3), LocalDate.now().plusYears(3))

        // Avslås pga. alder
        val avslåttFørstegang = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            )
        )
        assertThat(avslåttFørstegang)
            .describedAs("Førstegangsbehandlingen skal være satt som avsluttet")
            .extracting { b -> b.status().erAvsluttet() }.isEqualTo(true)
        val kravMottatt = LocalDate.now().minusMonths(1)
        val sak = hentSak(avslåttFørstegang)
        val klagebehandling = sak.sendInnKlage(
            mottattTidspunkt = LocalDateTime.now().minusMonths(3),
            klage = KlageV0(kravMottatt = kravMottatt)
        )
        assertThat(klagebehandling.referanse).isNotEqualTo(avslåttFørstegang.referanse)
        assertThat(klagebehandling.typeBehandling()).isEqualTo(TypeBehandling.Klage)

        dataSource.transaction { connection ->
            val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)
            val klageDokumenter =
                mottattDokumentRepository.hentDokumenterAvType(klagebehandling.id, InnsendingType.KLAGE)
            assertThat(klageDokumenter).hasSize(1)
            assertThat(klageDokumenter.first().strukturertDokument).isNotNull
            assertThat(klageDokumenter.first().strukturerteData<KlageV0>()?.data?.kravMottatt).isEqualTo(kravMottatt)
        }

        // PåklagetBehandlingSteg
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.FASTSETT_PÅKLAGET_BEHANDLING)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettPåklagetBehandlingLøsning(
                påklagetBehandlingVurdering = PåklagetBehandlingVurderingLøsningDto(
                    påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
                    påklagetBehandling = avslåttFørstegang.referanse.referanse,
                )
            )
        )

        // FullmektigSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.FASTSETT_FULLMEKTIG)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettFullmektigLøsning(
                fullmektigVurdering = FullmektigLøsningDto(
                    harFullmektig = false
                )
            )
        )

        // FormkravSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_FORMKRAV)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderFormkravLøsning(
                formkravVurdering = FormkravVurderingLøsningDto(
                    begrunnelse = "Begrunnelse",
                    erBrukerPart = true,
                    erFristOverholdt = false,
                    likevelBehandles = true,
                    erKonkret = true,
                    erSignert = true
                )
            )
        )

        // BehandlendeEnhetSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.FASTSETT_BEHANDLENDE_ENHET)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettBehandlendeEnhetLøsning(
                behandlendeEnhetVurdering = BehandlendeEnhetLøsningDto(
                    skalBehandlesAvNay = true,
                    skalBehandlesAvKontor = true
                )
            )
        )

        // KlagebehandlingKontorSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_KLAGE_KONTOR)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderKlageKontorLøsning(
                klagevurderingKontor = KlagevurderingKontorLøsningDto(
                    begrunnelse = "Begrunnelse",
                    notat = null,
                    innstilling = KlageInnstilling.OPPRETTHOLD,
                    vilkårSomOmgjøres = emptyList(),
                    vilkårSomOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6)
                )
            )
        )

        // KvalitetssikringsSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.KVALITETSSIKRING)

        kvalitetssikreOk(klagebehandling)

        // KlagebehandlingNaySteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_KLAGE_NAY)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderKlageNayLøsning(
                klagevurderingNay = KlagevurderingNayLøsningDto(
                    begrunnelse = "Begrunnelse",
                    notat = null,
                    innstilling = KlageInnstilling.OMGJØR,
                    vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5),
                    vilkårSomOpprettholdes = emptyList()
                )
            )
        )


        // Totalvurdering
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.BEKREFT_TOTALVURDERING_KLAGE)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = BekreftTotalvurderingKlageLøsning()
        )

        // FatteVedtakSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.FATTE_VEDTAK)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FatteVedtakLøsning(
                vurderinger = listOf(
                    TotrinnsVurdering(
                        begrunnelse = "Begrunnelse",
                        godkjent = true,
                        definisjon = Definisjon.VURDER_KLAGE_NAY.kode,
                        grunner = emptyList(),
                    ),
                    TotrinnsVurdering(
                        begrunnelse = "Begrunnelse",
                        godkjent = true,
                        definisjon = Definisjon.VURDER_KLAGE_KONTOR.kode,
                        grunner = emptyList(),
                    ),
                    TotrinnsVurdering(
                        begrunnelse = "Begrunnelse",
                        godkjent = true,
                        definisjon = Definisjon.VURDER_FORMKRAV.kode,
                        grunner = emptyList()
                    )
                )
            ),
            Bruker("X123456")
        )

        motor.kjørJobber()

        // OmgjøringSteg
        dataSource.transaction { connection ->
            val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)

            val omgjøringKlageRevurdering = mottattDokumentRepository.hentDokumenterAvType(
                klagebehandling.sakId,
                InnsendingType.OMGJØRING_KLAGE_REVURDERING
            )

            assertThat(omgjøringKlageRevurdering).hasSize(1).first()
                .extracting(MottattDokument::strukturertDokument)
                .isNotNull
            assertThat(
                omgjøringKlageRevurdering.first().strukturerteData<OmgjøringKlageRevurderingV0>()?.data?.beskrivelse
            ).isEqualTo("Revurdering etter klage som tas til følge. Følgende vilkår omgjøres: § 11-5")
        }

        val revurdering = hentSisteOpprettedeBehandlingForSak(klagebehandling.sakId, listOf(TypeBehandling.Revurdering))
        assertThat(revurdering.vurderingsbehov()).containsExactly(VurderingsbehovMedPeriode(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND))

        // OpprettholdelseSteg
        val steghistorikk = hentStegHistorikk(klagebehandling.id)
        assertThat(steghistorikk)
            .anySatisfy { assertThat(it.steg() == StegType.OPPRETTHOLDELSE && it.status() == StegStatus.AVSLUTTER).isTrue }

        // MeldingOmVedtakBrevSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.SKRIV_VEDTAKSBREV)
    }

    @Test
    fun `Klage - Skal gå rett til beslutter ved avslag på frist`() {
        val person = TestPersoner.PERSON_FOR_UNG()
        val ident = person.aktivIdent()

        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Avslås pga. alder
        val avslåttFørstegang = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            )
        )

        assertThat(avslåttFørstegang)
            .describedAs("Førstegangsbehandlingen skal være satt som avsluttet")
            .extracting { b -> b.status().erAvsluttet() }.isEqualTo(true)
        val kravMottatt = LocalDate.now().minusMonths(1)
        val sak = hentSak(avslåttFørstegang)
        val klagebehandling = sak.sendInnKlage(
            mottattTidspunkt = LocalDateTime.now().minusMonths(3),
            klage = KlageV0(kravMottatt = kravMottatt),
        )
        assertThat(klagebehandling.referanse).isNotEqualTo(avslåttFørstegang.referanse)
        assertThat(klagebehandling.typeBehandling()).isEqualTo(TypeBehandling.Klage)

        dataSource.transaction { connection ->
            val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)
            val klageDokumenter =
                mottattDokumentRepository.hentDokumenterAvType(klagebehandling.sakId, InnsendingType.KLAGE)
            assertThat(klageDokumenter).hasSize(1)
            assertThat(klageDokumenter.first().strukturertDokument).isNotNull
            assertThat(klageDokumenter.first().strukturerteData<KlageV0>()?.data?.kravMottatt).isEqualTo(kravMottatt)
        }

        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.FASTSETT_PÅKLAGET_BEHANDLING)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettPåklagetBehandlingLøsning(
                påklagetBehandlingVurdering = PåklagetBehandlingVurderingLøsningDto(
                    påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
                    påklagetBehandling = avslåttFørstegang.referanse.referanse,
                )
            )
        )

        // FullmektigSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.FASTSETT_FULLMEKTIG)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettFullmektigLøsning(
                fullmektigVurdering = FullmektigLøsningDto(
                    harFullmektig = true,
                    fullmektigIdentMedType = IdentMedType(
                        "22128209852",
                        IdentType.FNR_DNR
                    )
                )
            )
        )

        // FormkravSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first()
            .extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.VURDER_FORMKRAV)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderFormkravLøsning(
                formkravVurdering = FormkravVurderingLøsningDto(
                    begrunnelse = "Begrunnelse",
                    erBrukerPart = true,
                    erFristOverholdt = false,
                    likevelBehandles = false,
                    erKonkret = true,
                    erSignert = true
                )
            )
        )

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.FATTE_VEDTAK)

    }

    @Test
    fun `Klage - skal sende forhåndsvarsel ved avvist på formkrav, og kunne manuelt ta av vent og fortsette ved nye opplysninger`() {
        val person = TestPersoner.PERSON_FOR_UNG()
        val ident = person.aktivIdent()

        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Avslås pga. alder
        val avslåttFørstegang = sendInnSøknad(
            ident, periode,
            SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            ),
        )

        assertThat(avslåttFørstegang)
            .describedAs("Førstegangsbehandlingen skal være satt som avsluttet")
            .extracting { b -> b.status().erAvsluttet() }.isEqualTo(true)
        val kravMottatt = LocalDate.now().minusMonths(1)
        val sak = hentSak(avslåttFørstegang)
        val klagebehandling = sak.sendInnKlage(
            mottattTidspunkt = LocalDateTime.now().minusMonths(3),
            klage = KlageV0(kravMottatt = kravMottatt),
        )
        assertThat(klagebehandling.referanse).isNotEqualTo(avslåttFørstegang.referanse)
        assertThat(klagebehandling.typeBehandling()).isEqualTo(TypeBehandling.Klage)

        dataSource.transaction { connection ->
            val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)
            val klageDokumenter =
                mottattDokumentRepository.hentDokumenterAvType(klagebehandling.sakId, InnsendingType.KLAGE)
            assertThat(klageDokumenter).hasSize(1)
            assertThat(klageDokumenter.first().strukturertDokument).isNotNull
            assertThat(klageDokumenter.first().strukturerteData<KlageV0>()?.data?.kravMottatt).isEqualTo(kravMottatt)
        }

        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.FASTSETT_PÅKLAGET_BEHANDLING)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettPåklagetBehandlingLøsning(
                påklagetBehandlingVurdering = PåklagetBehandlingVurderingLøsningDto(
                    påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
                    påklagetBehandling = avslåttFørstegang.referanse.referanse,
                )
            )
        )

        // FullmektigSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.FASTSETT_FULLMEKTIG)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettFullmektigLøsning(
                fullmektigVurdering = FullmektigLøsningDto(
                    harFullmektig = false
                )
            )
        )

        // FormkravSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_FORMKRAV)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderFormkravLøsning(
                formkravVurdering = FormkravVurderingLøsningDto(
                    begrunnelse = "Begrunnelse",
                    erBrukerPart = false,
                    erFristOverholdt = true,
                    likevelBehandles = false,
                    erKonkret = true,
                    erSignert = true
                )
            )
        )

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV)

        val formkravGrunnlag = dataSource.transaction {
            val formkravRepository = FormkravRepositoryImpl(it)
            formkravRepository.hentHvisEksisterer(klagebehandling.id)
        }

        assertNotNull(formkravGrunnlag?.varsel?.varselId)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = SkrivForhåndsvarselKlageFormkravBrevLøsning(
                brevbestillingReferanse = formkravGrunnlag.varsel.varselId.brevbestillingReferanse,
                handling = SkrivBrevAvklaringsbehovLøsning.Handling.FERDIGSTILL,
                behovstype = Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV.kode,
            )
        )

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_KLAGE_FORMKRAV)

        // Ta av vent manuelt
        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VentePåFristForhåndsvarselKlageFormkravLøsning(),
        )

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.VURDER_FORMKRAV)

        // Går manuelt tilbake til formkrav fordi nye opplysninger gir oppfylt
        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderFormkravLøsning(
                formkravVurdering = FormkravVurderingLøsningDto(
                    begrunnelse = "Ny begrunnelse",
                    erBrukerPart = true,
                    erFristOverholdt = true,
                    likevelBehandles = true,
                    erKonkret = true,
                    erSignert = true
                )
            )
        )

        // Sier at formkrav nå er oppfyllt
        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderFormkravLøsning(
                formkravVurdering = FormkravVurderingLøsningDto(
                    begrunnelse = "begrunnelse",
                    erBrukerPart = true,
                    erFristOverholdt = true,
                    erSignert = true,
                    erKonkret = true,
                    likevelBehandles = null
                )
            )
        )

        // Går inn i normal flyt
        // BehandlendeEnhetSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.FASTSETT_BEHANDLENDE_ENHET)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettBehandlendeEnhetLøsning(
                behandlendeEnhetVurdering = BehandlendeEnhetLøsningDto(
                    skalBehandlesAvNay = true,
                    skalBehandlesAvKontor = false
                )
            )
        )

        // KlagebehandlingNaySteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_KLAGE_NAY)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderKlageNayLøsning(
                klagevurderingNay = KlagevurderingNayLøsningDto(
                    begrunnelse = "Begrunnelse",
                    notat = null,
                    innstilling = KlageInnstilling.OMGJØR,
                    vilkårSomOpprettholdes = emptyList(),
                    vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5)
                )
            )
        )

        // Beslutter
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.FATTE_VEDTAK)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FatteVedtakLøsning(
                vurderinger = listOf(
                    TotrinnsVurdering(
                        begrunnelse = "Tilbakesend formkrav",
                        godkjent = false,
                        definisjon = Definisjon.VURDER_FORMKRAV.kode,
                        grunner = listOf(ÅrsakTilRetur(ÅrsakTilReturKode.ANNET, "Formkrav ikke oppfylt")),
                    ),
                    TotrinnsVurdering(
                        begrunnelse = "Begrunneøse",
                        godkjent = true,
                        definisjon = Definisjon.VURDER_KLAGE_NAY.kode,
                        grunner = emptyList(),
                    ),
                )
            ),
            Bruker("BESLUTTER")
        )

        // Sjekk at avklaringsbehov er blitt gjenåpnet
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(3)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_FORMKRAV)
        assertThat(åpneAvklaringsbehov.first().status()).isEqualTo(AvklaringsbehovStatus.SENDT_TILBAKE_FRA_BESLUTTER)
    }

    @Test
    fun `Teste TrekkKlageFlyt`() {
        val person = TestPersoner.PERSON_FOR_UNG()

        val ident = person.aktivIdent()

        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Avslås pga. alder
        val avslåttFørstegang = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            )
        )

        assertThat(avslåttFørstegang)
            .describedAs("Førstegangsbehandlingen skal være satt som avsluttet")
            .extracting { b -> b.status() }.isIn(Status.IVERKSETTES, Status.AVSLUTTET)

        val kravMottatt = LocalDate.now().minusMonths(1)
        val sak = hentSak(avslåttFørstegang)
        val klagebehandling = sak.sendInnKlage(
            mottattTidspunkt = LocalDateTime.now().minusMonths(3),
            klage = KlageV0(kravMottatt = kravMottatt),
        )
        assertThat(klagebehandling.referanse).isNotEqualTo(avslåttFørstegang.referanse)
        assertThat(klagebehandling.typeBehandling()).isEqualTo(TypeBehandling.Klage)

        dataSource.transaction { connection ->
            val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)
            val klageDokumenter =
                mottattDokumentRepository.hentDokumenterAvType(klagebehandling.id, InnsendingType.KLAGE)
            assertThat(klageDokumenter).hasSize(1)
            assertThat(klageDokumenter.first().strukturertDokument).isNotNull
            assertThat(klageDokumenter.first().strukturerteData<KlageV0>()?.data?.kravMottatt).isEqualTo(kravMottatt)
        }

        // PåklagetBehandlingSteg
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.FASTSETT_PÅKLAGET_BEHANDLING)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettPåklagetBehandlingLøsning(
                påklagetBehandlingVurdering = PåklagetBehandlingVurderingLøsningDto(
                    påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
                    påklagetBehandling = avslåttFørstegang.referanse.referanse,
                )
            )
        )

        // FullmektigSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.FASTSETT_FULLMEKTIG)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettFullmektigLøsning(
                fullmektigVurdering = FullmektigLøsningDto(
                    harFullmektig = false
                )
            )
        )

        // FormkravSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_FORMKRAV)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderFormkravLøsning(
                formkravVurdering = FormkravVurderingLøsningDto(
                    begrunnelse = "Begrunnelse",
                    erBrukerPart = true,
                    erFristOverholdt = false,
                    likevelBehandles = true,
                    erKonkret = true,
                    erSignert = true
                )
            )
        )

        // BehandlendeEnhetSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.FASTSETT_BEHANDLENDE_ENHET)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettBehandlendeEnhetLøsning(
                behandlendeEnhetVurdering = BehandlendeEnhetLøsningDto(
                    skalBehandlesAvNay = true,
                    skalBehandlesAvKontor = true
                )
            )
        )

        // KlagebehandlingKontorSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_KLAGE_KONTOR)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderKlageKontorLøsning(
                klagevurderingKontor = KlagevurderingKontorLøsningDto(
                    begrunnelse = "Begrunnelse",
                    notat = null,
                    innstilling = KlageInnstilling.OPPRETTHOLD,
                    vilkårSomOmgjøres = emptyList(),
                    vilkårSomOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6)
                )
            )
        )

        // KvalitetssikringsSteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.KVALITETSSIKRING)

        kvalitetssikreOk(klagebehandling)

        // KlagebehandlingNaySteg
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_KLAGE_NAY)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderKlageNayLøsning(
                klagevurderingNay = KlagevurderingNayLøsningDto(
                    begrunnelse = "Begrunnelse",
                    notat = null,
                    innstilling = KlageInnstilling.OMGJØR,
                    vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5),
                    vilkårSomOpprettholdes = emptyList()
                )
            )
        )

        val trekkKlageBehandling = klagebehandling.leggTilVurderingsbehov(
            no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.KLAGE_TRUKKET
        )

        // Sjekk at Klagen nå har fått "KLAGE_TRUKKET" som årsak til behandling (og derfor er i riktig tilstand)
        motor.kjørJobber()
        assertThat(trekkKlageBehandling.id).isEqualTo(klagebehandling.id)
        assertThat(trekkKlageBehandling.vurderingsbehov().map { it.type }).contains(Vurderingsbehov.KLAGE_TRUKKET)

        // Løs avklaringsbehovet som trekker klagen og trigger sletting - skal og sette klagen til avsluttet
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(Definisjon.VURDER_TREKK_AV_KLAGE)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = TrekkKlageLøsning(
                vurdering = TrekkKlageVurderingDto(
                    begrunnelse = "Begrunnelse",
                    skalTrekkes = true,
                    hvorforTrekkes = TrekkKlageÅrsakDto.FEILREGISTRERING
                )
            )
        )

        val avsluttetBehandling = hentBehandling(klagebehandling.referanse)
        assertThat(avsluttetBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Håndtere svar fra kabal - valg omgjøring av kapitel 2 skal opprette en revurdering av LOVVALG_OG_MEDLEMSKAP`() {
        val person = TestPersoner.PERSON_FOR_UNG()
        val ident = person.aktivIdent()

        val periode = Periode(LocalDate.now().minusMonths(3), LocalDate.now().plusYears(3))

        // Avslås pga. alder
        val avslåttFørstegang = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            )
        )
        assertThat(avslåttFørstegang)
            .describedAs("Førstegangsbehandlingen skal være satt som avsluttet")
            .extracting { b -> b.status().erAvsluttet() }.isEqualTo(true)
        val kravMottatt = LocalDate.now().minusMonths(1)
        val sak = hentSak(avslåttFørstegang)
        val klagebehandling = sak.sendInnKlage(
            journalpostId = JournalpostId("401"),
            mottattTidspunkt = LocalDateTime.now().minusMonths(3),
            klage = KlageV0(kravMottatt = kravMottatt),
        )

        assertThat(klagebehandling.referanse).isNotEqualTo(avslåttFørstegang.referanse)
        assertThat(klagebehandling.typeBehandling()).isEqualTo(TypeBehandling.Klage)

        var svarFraAndreinstansBehandling = sak.sendInnKabalHendelse(
            mottattTidspunkt = LocalDateTime.now().minusMonths(3),
            kabalHendelse = KabalHendelseV0(
                eventId = UUID.randomUUID(),
                kildeReferanse = klagebehandling.referanse.toString(),
                kilde = Fagsystem.KELVIN.name,
                kabalReferanse = UUID.randomUUID().toString(),
                type = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
                detaljer = BehandlingDetaljer(
                    KlagebehandlingAvsluttetDetaljer(
                        avsluttet = LocalDateTime.now().minusMinutes(2),
                        utfall = KlageUtfall.MEDHOLD,
                        journalpostReferanser = emptyList()
                    ),
                )
            ),
        )

        assertThat(svarFraAndreinstansBehandling.referanse).isNotEqualTo(klagebehandling.referanse)
        assertThat(svarFraAndreinstansBehandling.typeBehandling()).isEqualTo(TypeBehandling.SvarFraAndreinstans)

        dataSource.transaction { connection ->
            val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)
            val kabalHendelseDokumenter =
                mottattDokumentRepository.hentDokumenterAvType(
                    svarFraAndreinstansBehandling.sakId,
                    InnsendingType.KABAL_HENDELSE
                )
            assertThat(kabalHendelseDokumenter).hasSize(1)
            assertThat(kabalHendelseDokumenter.first().strukturertDokument).isNotNull
            assertThat(kabalHendelseDokumenter.first().strukturerteData<KabalHendelseV0>()?.data).isNotNull
        }

        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(svarFraAndreinstansBehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.HÅNDTER_SVAR_FRA_ANDREINSTANS)

        svarFraAndreinstansBehandling = løsAvklaringsBehov(
            svarFraAndreinstansBehandling,
            avklaringsBehovLøsning = HåndterSvarFraAndreinstansLøsning(
                svarFraAndreinstansVurdering = HåndterSvarFraAndreinstansLøsningDto(
                    begrunnelse = "Begrunnelse for håndtering",
                    konsekvens = SvarFraAndreinstansKonsekvens.OMGJØRING,
                    vilkårSomOmgjøres = listOf(
                        Hjemmel.FOLKETRYGDLOVEN_KAPITTEL_2
                    )
                )
            )
        )

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(svarFraAndreinstansBehandling.id)
        assertThat(åpneAvklaringsbehov).isEmpty()
        assertThat(svarFraAndreinstansBehandling.status()).isEqualTo(Status.AVSLUTTET)

        motor.kjørJobber()

        val revurdering = hentSisteOpprettedeBehandlingForSak(svarFraAndreinstansBehandling.sakId)
        assertThat(revurdering).isNotNull
        assertThat(revurdering.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
        assertThat(
            revurdering.vurderingsbehov().map { it.type }).contains(Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP)
    }


    @Test
    fun `Håndtere svar fra kabal - valg omgjøring skal opprette en revurdering`() {
        val person = TestPersoner.PERSON_FOR_UNG()
        val ident = person.aktivIdent()

        val periode = Periode(LocalDate.now().minusMonths(3), LocalDate.now().plusYears(3))

        // Avslås pga. alder
        val avslåttFørstegang = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            )
        )
        assertThat(avslåttFørstegang)
            .describedAs("Førstegangsbehandlingen skal være satt som avsluttet")
            .extracting { b -> b.status().erAvsluttet() }.isEqualTo(true)
        val kravMottatt = LocalDate.now().minusMonths(1)
        val sak = hentSak(avslåttFørstegang)
        val klagebehandling = sak.sendInnKlage(
            journalpostId = JournalpostId("401"),
            mottattTidspunkt = LocalDateTime.now().minusMonths(3),
            klage = KlageV0(kravMottatt = kravMottatt),
        )

        assertThat(klagebehandling.referanse).isNotEqualTo(avslåttFørstegang.referanse)
        assertThat(klagebehandling.typeBehandling()).isEqualTo(TypeBehandling.Klage)

        var svarFraAndreinstansBehandling = sak.sendInnKabalHendelse(
            mottattTidspunkt = LocalDateTime.now().minusMonths(3),
            kabalHendelse = KabalHendelseV0(
                eventId = UUID.randomUUID(),
                kildeReferanse = klagebehandling.referanse.toString(),
                kilde = Fagsystem.KELVIN.name,
                kabalReferanse = UUID.randomUUID().toString(),
                type = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
                detaljer = BehandlingDetaljer(
                    KlagebehandlingAvsluttetDetaljer(
                        avsluttet = LocalDateTime.now().minusMinutes(2),
                        utfall = KlageUtfall.MEDHOLD,
                        journalpostReferanser = emptyList()
                    ),
                )
            )
        )

        assertThat(svarFraAndreinstansBehandling.referanse).isNotEqualTo(klagebehandling.referanse)
        assertThat(svarFraAndreinstansBehandling.typeBehandling()).isEqualTo(TypeBehandling.SvarFraAndreinstans)

        dataSource.transaction { connection ->
            val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)
            val kabalHendelseDokumenter =
                mottattDokumentRepository.hentDokumenterAvType(
                    svarFraAndreinstansBehandling.sakId,
                    InnsendingType.KABAL_HENDELSE
                )
            assertThat(kabalHendelseDokumenter).hasSize(1)
            assertThat(kabalHendelseDokumenter.first().strukturertDokument).isNotNull
            assertThat(kabalHendelseDokumenter.first().strukturerteData<KabalHendelseV0>()?.data).isNotNull
        }

        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(svarFraAndreinstansBehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.HÅNDTER_SVAR_FRA_ANDREINSTANS)

        svarFraAndreinstansBehandling = løsAvklaringsBehov(
            svarFraAndreinstansBehandling,
            avklaringsBehovLøsning = HåndterSvarFraAndreinstansLøsning(
                svarFraAndreinstansVurdering = HåndterSvarFraAndreinstansLøsningDto(
                    begrunnelse = "Begrunnelse for håndtering",
                    konsekvens = SvarFraAndreinstansKonsekvens.OMGJØRING,
                    vilkårSomOmgjøres = listOf(
                        Hjemmel.FOLKETRYGDLOVEN_11_5,
                        Hjemmel.FOLKETRYGDLOVEN_11_6
                    )
                )
            )
        )

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(svarFraAndreinstansBehandling.id)
        assertThat(åpneAvklaringsbehov).isEmpty()
        assertThat(svarFraAndreinstansBehandling.status()).isEqualTo(Status.AVSLUTTET)

        motor.kjørJobber()

        val revurdering = hentSisteOpprettedeBehandlingForSak(svarFraAndreinstansBehandling.sakId)
        assertThat(revurdering).isNotNull
        assertThat(revurdering.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
        assertThat(
            revurdering.vurderingsbehov().map { it.type }).contains(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)
    }

    @Test
    fun `Skal kunne overstyre rettighetsperioden`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        val nyStartDato = periode.fom.minusDays(7)

        var (sak, behandling) = sendInnFørsteSøknad(
            mottattTidspunkt = periode.fom.atStartOfDay(),
            periode = periode,
        )

        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.AVKLAR_SYKDOM)

        behandling = sak.opprettManuellRevurdering(
            vurderingsbehov = listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.VURDER_RETTIGHETSPERIODE),
        )

        behandling = behandling.løsRettighetsperiode(nyStartDato)

        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.AVKLAR_SYKDOM)

        sak = hentSak(sak.saksnummer)

        assertThat(sak.rettighetsperiode).isNotEqualTo(periode)
        assertThat(sak.rettighetsperiode).isEqualTo(
            Periode(
                nyStartDato,
                nyStartDato.plusYears(1).minusDays(1)
            )
        )
    }

    @Test
    fun `Skal kunne overstyre rettighetsperioden hos NAY`() {
        val (sak, behandling) = sendInnFørsteSøknad()

        val ident = sak.person.aktivIdent()
        val nyStartDato = sak.rettighetsperiode.fom.minusDays(7)
        behandling
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()

        var oppdatertBehandling = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.VURDER_RETTIGHETSPERIODE),
        ).medKontekst {
            assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
            assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
        }

        oppdatertBehandling = oppdatertBehandling
            .løsRettighetsperiode(nyStartDato)
            .medKontekst {
                val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(oppdatertBehandling.id)
                assertThat(åpneAvklaringsbehov).hasSize(2)
                assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.AVKLAR_SYKDOM)

            }
            .løsSykdom(nyStartDato)
            .løsBistand()
            .løsBeregningstidspunkt(nyStartDato)
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(nyStartDato)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_INNVILGELSE)

        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(oppdatertBehandling.id)
        assertThat(åpneAvklaringsbehov).isEmpty()

        val oppdatertSak = hentSak(ident, sak.rettighetsperiode)

        assertThat(oppdatertSak.rettighetsperiode).isNotEqualTo(sak.rettighetsperiode)
        assertThat(oppdatertSak.rettighetsperiode).isEqualTo(
            Periode(
                nyStartDato,
                nyStartDato.plusYears(1).minusDays(1)
            )
        )
    }

    @Test
    fun `Skal kunne overstyre rettighetsperioden på en revurdering - øke perioden`() {
        val sak = happyCaseFørstegangsbehandling(LocalDate.now())
        val ident = sak.person.aktivIdent()
        val nyStartDato = sak.rettighetsperiode.fom.minusDays(7)
        var revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.VURDER_RETTIGHETSPERIODE),
        ).medKontekst {
            assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
            assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
        }

        revurdering = revurdering
            .løsRettighetsperiode(nyStartDato)
            .løsSykdom(nyStartDato)
            .løsBistand()
            .løsSykdomsvurderingBrev()
            .løsBeregningstidspunkt(nyStartDato)
            .løsUtenSamordning()
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_ENDRING)

        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(revurdering.id)
        assertThat(åpneAvklaringsbehov).isEmpty()

        val oppdatertSak = hentSak(ident, sak.rettighetsperiode)

        assertThat(oppdatertSak.rettighetsperiode).isNotEqualTo(sak.rettighetsperiode)
        assertThat(oppdatertSak.rettighetsperiode).isEqualTo(
            Periode(
                nyStartDato,
                sak.rettighetsperiode.tom
            )
        )
    }

    @Test
    fun `Skal kunne overstyre rettighetsperioden på en revurdering - innskrenke perioden`() {
        val sak = happyCaseFørstegangsbehandling(LocalDate.now())
        val ident = sak.person.aktivIdent()
        val førsteOverstyring = sak.rettighetsperiode.fom.minusMonths(2)
        val andreOverstyring = sak.rettighetsperiode.fom.minusMonths(1)

        /**
         * Utvid rettighetsperioden
         */
        val avklaringsbehovManuellRevurdering =
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.VURDER_RETTIGHETSPERIODE)
        sak.opprettManuellRevurdering(avklaringsbehovManuellRevurdering)
            .medKontekst {
                assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }.løsRettighetsperiode(førsteOverstyring)
            .løsSykdom(førsteOverstyring)
            .løsBistand()
            .løsSykdomsvurderingBrev()
            .løsBeregningstidspunkt(LocalDate.now())
            .løsUtenSamordning()
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)
                assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.SKRIV_VEDTAKSBREV) }
            }
            .løsVedtaksbrev(TypeBrev.VEDTAK_ENDRING)
            .medKontekst {
                val oppdatertRettighetsperiode = hentSak(ident, sak.rettighetsperiode).rettighetsperiode
                assertThat(oppdatertRettighetsperiode.fom).isEqualTo(førsteOverstyring)
            }

        /**
         * Innskrenke rettighetsperioden, men ikke etter søknadsdato
         */
        val revurderingInnskrenking = sak.opprettManuellRevurdering(avklaringsbehovManuellRevurdering)
            .medKontekst {
                assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsRettighetsperiode(andreOverstyring)
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand()
            .løsSykdomsvurderingBrev()
            .løsBeregningstidspunkt(LocalDate.now())
            .løsUtenSamordning()
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)
                assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.SKRIV_VEDTAKSBREV) }
            }
            .løsVedtaksbrev(TypeBrev.VEDTAK_ENDRING)

        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(revurderingInnskrenking.id)
        assertThat(åpneAvklaringsbehov).isEmpty()

        val oppdatertSak = hentSak(ident, sak.rettighetsperiode)

        assertThat(oppdatertSak.rettighetsperiode).isNotEqualTo(sak.rettighetsperiode)
        assertThat(oppdatertSak.rettighetsperiode).isEqualTo(
            Periode(
                andreOverstyring,
                sak.rettighetsperiode.tom
            )
        )
    }

    @Test
    fun `Skal ikke kunne overstyre rettighetsperioden på en revurdering ved å innskrenke fra søknadsdato`() {
        val sak = happyCaseFørstegangsbehandling(LocalDate.now())
        val nyStartDato = sak.rettighetsperiode.fom.plusDays(7)
        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.VURDER_RETTIGHETSPERIODE)
        )

        val feil = assertThrows<UgyldigForespørselException> {
            revurdering.løsRettighetsperiode(nyStartDato)
        }
        assertThat(feil.message).contains("Kan ikke endre starttidspunkt til å gjelde ETTER søknadstidspunkt")

    }

    @Test
    fun `Vurdering av 11-17`() {
        if (gatewayProvider.provide<UnleashGateway>().isDisabled(BehandlingsflytFeature.OvergangArbeid)) {
            return
        }

        val sak = happyCaseFørstegangsbehandling(LocalDate.now())
        val endringsdato = sak.rettighetsperiode.fom.plusDays(7)
        val sluttdato = endringsdato.plusMonths(6).minusDays(1)

        /* Gir AAP som arbeidssøker. */
        sak.opprettManuellRevurdering(
            no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
        )
            .løsSykdom(vurderingGjelderFra = endringsdato, erOppfylt = false)
            .løsBistand(erOppfylt = false)
            .løsOvergangArbeid(Utfall.OPPFYLT, fom = endringsdato)
            .løsSykdomsvurderingBrev()
            .fattVedtak()
            .also {
                assertThat(it.status()).isEqualTo(Status.IVERKSETTES)
            }
            .assertRettighetstype(
                Periode(sak.rettighetsperiode.fom, endringsdato.minusDays(1)) to RettighetsType.BISTANDSBEHOV,
                Periode(endringsdato, sluttdato) to RettighetsType.ARBEIDSSØKER,
                Periode(sluttdato.plusDays(1), sak.rettighetsperiode.tom) to null
            )

        /* Revurdering som ombestemmer seg, og ikke gir AAP som arbeidssøker. */
        sak.opprettManuellRevurdering(
            no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
        )
            .løsSykdom(vurderingGjelderFra = endringsdato, erOppfylt = false)
            .løsBistand(erOppfylt = false)
            .løsOvergangArbeid(Utfall.IKKE_OPPFYLT, fom = endringsdato)
            .løsSykdomsvurderingBrev()
            .fattVedtak()
            .also {
                assertThat(it.status()).isEqualTo(Status.IVERKSETTES)
                it.assertRettighetstype(
                    Periode(sak.rettighetsperiode.fom, endringsdato.minusDays(1)) to RettighetsType.BISTANDSBEHOV,
                    Periode(endringsdato, sak.rettighetsperiode.tom) to null,
                )
            }
    }

    @Test
    fun `Endrer sykdomsvurdering slik at 11-17-vurdering ikke lenger er nødvendig`() {
        if (gatewayProvider.provide<UnleashGateway>().isDisabled(BehandlingsflytFeature.OvergangArbeid)) {
            return
        }

        val sak = happyCaseFørstegangsbehandling(LocalDate.now())

        /* Gir AAP som arbeidssøker. */
        val endringsdato = sak.rettighetsperiode.fom.plusDays(7)
        sak.opprettManuellRevurdering(
            no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
        )
            .løsSykdom(vurderingGjelderFra = endringsdato, erOppfylt = false)
            .løsBistand(erOppfylt = true)
            .løsOvergangArbeid(Utfall.OPPFYLT, fom = endringsdato)
            /* Her hopper vi "tilbake" i flyten og endrer sykdom til oppfylt. */
            .løsSykdom(vurderingGjelderFra = endringsdato, erOppfylt = true)
            .løsSykdomsvurderingBrev()
            .fattVedtak()
            .also {
                assertThat(it.status()).isEqualTo(Status.IVERKSETTES)
            }
            .assertRettighetstype(
                sak.rettighetsperiode to RettighetsType.BISTANDSBEHOV,
            )
            .assertVilkårsutfall(
                Vilkårtype.OVERGANGARBEIDVILKÅRET,
                sak.rettighetsperiode to Utfall.IKKE_VURDERT
            )
    }

    @Test
    fun `barn lagres i pip i starten av behandlingen`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val manueltBarnIdent = genererIdent(LocalDate.now().minusYears(3))
        val person = TestPersoner.STANDARD_PERSON()

        // Oppretter søknad med manuelt barn
        val (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
            søknad = SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = OppgitteBarn(
                    barn = listOf(
                        ManueltOppgittBarn(
                            navn = "manuelt barn",
                            fødselsdato = LocalDate.now().minusYears(3),
                            ident = no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Ident(manueltBarnIdent.identifikator),
                            relasjon = ManueltOppgittBarn.Relasjon.FORELDER
                        )
                    ),
                    identer = emptySet()
                ),
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        )

        dataSource.transaction { connection ->
            val pipRepository = PipRepositoryImpl(connection)
            val pipIdenter = pipRepository.finnIdenterPåBehandling(behandling.referanse)

            // Manuelt barn finnes i pip umiddelbart etter at søknad er innsendt
            assertThat(pipIdenter.map { it.ident }).containsExactlyInAnyOrder(
                person.aktivIdent().identifikator,
                manueltBarnIdent.identifikator,
            )
        }
    }

    @Test
    fun `Teste AvbrytRevurderingFlyt`() {
        // Førstegangsbehandling
        val sak = happyCaseFørstegangsbehandling()
        val førstegangsbehandling = hentSisteOpprettedeBehandlingForSak(sak.id)

        // Revurdering 1 - skal bli avbrutt
        val revurdering1 = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)
        )
            .medKontekst {
                assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsSykdom(sak.rettighetsperiode.fom)

        assertThat(revurdering1.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
        assertThat(revurdering1.forrigeBehandlingId).isEqualTo(førstegangsbehandling.id)

        // Avbryt revurdering 1
        revurdering1.leggTilVurderingsbehov(
            no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDERING_AVBRUTT
        )
        assertThat(hentAlleAvklaringsbehov(revurdering1)).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.erÅpent() && avklaringsbehov.definisjon == Definisjon.AVBRYT_REVURDERING).isTrue() }

        løsAvklaringsBehov(
            revurdering1,
            AvbrytRevurderingLøsning(
                vurdering = AvbrytRevurderingVurderingDto(
                    årsak = AvbrytRevurderingÅrsakDto.REVURDERINGEN_BLE_OPPRETTET_VED_EN_FEIL,
                    begrunnelse = "Fordi den ikke er aktuell lenger"
                ),
            )
        )

        val avklaringsbehovene: List<Avklaringsbehov> = hentAlleAvklaringsbehov(revurdering1)
        val revurdering1FraRepo = hentBehandling(revurdering1.referanse)
        assertThat(revurdering1FraRepo.status()).isEqualTo(Status.AVSLUTTET)
        assertThat(avklaringsbehovene.none { it.erÅpent() }).isTrue()
        assertStatusForDefinisjon(avklaringsbehovene, Definisjon.AVBRYT_REVURDERING, AvklaringsbehovStatus.AVSLUTTET)
        assertStatusForDefinisjon(avklaringsbehovene, Definisjon.AVKLAR_SYKDOM, AvklaringsbehovStatus.AVBRUTT)
        assertStatusForDefinisjon(avklaringsbehovene, Definisjon.AVKLAR_BISTANDSBEHOV, AvklaringsbehovStatus.AVBRUTT)

        // Revurdering 2 - skal ikke kopiere data fra revurdering1 men fra førstegangsbehandling
        val revurdering2 = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP)
        )
            .medKontekst {
                assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }

        assertThat(revurdering2.forrigeBehandlingId).isEqualTo(førstegangsbehandling.id)

        // Verifiser at data er kopiert fra førstegangsbehandling
        val vilkårsresultat1 = hentVilkårsresultat(førstegangsbehandling.id)
        val vilkårsresultat2 = hentVilkårsresultat(revurdering2.id)

        assertThat(vilkårsresultat2).usingRecursiveComparison()
            .ignoringFields(
                "id",
                "faktagrunnlag",
                "vilkår.vilkårTidslinje",
                "vilkår.vurdertTidspunkt",
                "vilkår.faktagrunnlag",
                "vilkår.vilkårTidslinje",
                "vilkår.vurdertTidspunkt"
            )
            .isEqualTo(vilkårsresultat1)
    }

    fun assertStatusForDefinisjon(
        avklaringsbehov: List<Avklaringsbehov>,
        definisjon: Definisjon,
        forventetStatus: AvklaringsbehovStatus
    ) {
        assertThat(avklaringsbehov.filter { it.definisjon == definisjon }
            .map { it.status() })
            .containsExactly(forventetStatus)
    }

}
