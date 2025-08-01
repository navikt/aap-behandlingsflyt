package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.ÅrsakTilReturKode
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarManuellInntektVurderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOverstyrtForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOverstyrtLovvalgMedlemskapLøsning
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
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettYrkesskadeInntektLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FullmektigLøsningDto
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.HåndterSvarFraAndreinstansLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.HåndterSvarFraAndreinstansLøsningDto
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.RefusjonkravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivForhåndsvarselKlageFormkravBrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SykdomsvurderingForBrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.TrekkKlageLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.TrekkSøknadLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VentePåFristForhåndsvarselKlageFormkravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderFormkravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageKontorLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageNayLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderRettighetsperiodeLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.trekkklage.flate.TrekkKlageVurderingDto
import no.nav.aap.behandlingsflyt.behandling.trekkklage.flate.TrekkKlageÅrsakDto
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLand
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
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
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
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.RegisterBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvarDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingerForBarnetillegg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarnDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningYrkeskaderBeløpVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.SamordningVurderingData
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.VurderingerForSamordning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykepengerVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansKonsekvens
import no.nav.aap.behandlingsflyt.flyt.internals.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.flyt.internals.NyÅrsakTilBehandlingHendelse
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.integrasjon.kabal.Fagsystem
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlingDetaljer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlingEventType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageUtfall
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlagebehandlingAvsluttetDetaljer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UtenlandsPeriodeDto
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.FormkravRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.barn.BarnRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.PersonNavn
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.*
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus

@Tag("motor")
class FlytOrkestratorTest() : AbstraktFlytOrkestratorTest() {
    @Test
    fun `happy case førstegangsbehandling + revurder førstegangssøknad, gi sykepengererstatning hele perioden`() {
        val sak = happyCaseFørstegangsbehandling()
        val behandling = revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom)

        behandling.løsAvklaringsBehov(
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
        ).medKontekst {
            assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                .describedAs("Siden vurderingenGjelderFra er lik kravdato (rettighetsperiode.fom), så kan man revurdere 11-13")
                .containsExactlyInAnyOrder(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
        }
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "...",
                        dokumenterBruktIVurdering = listOf(),
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
            .fattVedtakEllerSendRetur()
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

        behandling.løsAvklaringsBehov(
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
        ).medKontekst {
            assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                .describedAs("Siden vurderingenGjelderFra er lik kravdato (rettighetsperiode.fom), så kan man revurdere 11-13")
                .containsExactlyInAnyOrder(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
        }
            .løsAvklaringsBehov(
                // Vi svarer nei på rett til sykepengererstatning
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "HAR IKKE RETT",
                        dokumenterBruktIVurdering = listOf(),
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
            .fattVedtakEllerSendRetur()
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
    fun `revurdere sykepengeerstatning - skal ikke trigge 11-13 om gjelderFra ikke er kravdato`() {
        val sak = happyCaseFørstegangsbehandling()
        val gjelderFra = sak.rettighetsperiode.fom.plusMonths(1)

        revurdereFramTilOgMedSykdom(sak, gjelderFra)
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
            ).medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .describedAs("Siden vurderingenGjelderFra ikke er lik kravdato (rettighetsperiode.fom), så skal man ikke vurdere 11-13")
                    .containsExactlyInAnyOrder(Definisjon.FATTE_VEDTAK)  // ingen avklaringsbehov løst av NAY, gå rett til fatte vedtak
            }
            .fattVedtakEllerSendRetur()
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

    fun revurdereFramTilOgMedSykdom(sak: Sak, gjelderFra: LocalDate, vissVarighet: Boolean? = null): Behandling {
        val ident = sak.person.aktivIdent()
        val periode = sak.rettighetsperiode

        return sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("29"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
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
            .medKontekst {
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
                            erNedsettelseIArbeidsevneAvEnVissVarighet = vissVarighet,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            // Ny revurdering
                            vurderingenGjelderFra = gjelderFra
                        )
                    )
                ),
            ).medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.AVKLAR_BISTANDSBEHOV)
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
    }

    @Test
    fun `hopper over foreslå vedtak-steg når revurdering ikke skal innom NAY`() {
        val sak = happyCaseFørstegangsbehandling()
        // Revurdering av sykdom uten 11-13
        val behandling = revurdereFramTilOgMedSykdom(
            sak = sak,
            gjelderFra = sak.rettighetsperiode.fom,
            vissVarighet = true
        )

        behandling.løsAvklaringsBehov(
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
            ))
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
        val behandling = revurdereFramTilOgMedSykdom(
            sak = sak,
            gjelderFra = sak.rettighetsperiode.fom,
            vissVarighet = false)

        behandling.løsAvklaringsBehov(
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
            )).løsAvklaringsBehov(
            AvklarSykepengerErstatningLøsning(
                sykepengeerstatningVurdering = SykepengerVurderingDto(
                    begrunnelse = "test",
                    dokumenterBruktIVurdering = emptyList(),
                    harRettPå = true,
                    grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR
                ),
                behovstype = Definisjon.AVKLAR_SYKEPENGEERSTATNING.kode
            )).medKontekst {
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
        person.barn.forEach { FakePersoner.leggTil(it) }

        val ident = person.aktivIdent()

        val behandling = sendInnSøknad(
            ident, periode, TestSøknader.STANDARD_SØKNAD
        )
            .løsSykdom()
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()

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
                    registerbarn = RegisterBarn(id = -1, barn = listOf(Barn(barnIdent, Fødselsdato(barnfødseldato)))),
                    oppgitteBarn = null,
                    vurderteBarn = null
                )
            )

        val uthentetTilkjentYtelse =
            requireNotNull(dataSource.transaction { TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id) })
            { "Tilkjent ytelse skal være beregnet her." }

        val barnetillegg = uthentetTilkjentYtelse.map { Segment(it.periode, it.tilkjent.barnetillegg) }.let(::Tidslinje)

        val barnBlirAttenPå = barnfødseldato.plusYears(18)

        val periodeBarnUnderAtten = Periode(periode.fom, barnBlirAttenPå.minusDays(1))
        val barnErAtten = barnetillegg.begrensetTil(periodeBarnUnderAtten)

        assertThat(barnErAtten).isNotEmpty
        // Verifiser at barnetillegg kun gis fram til barnet er 18 år
        assertTidslinje(barnErAtten, periodeBarnUnderAtten to {
            assertThat(it).isEqualTo(Beløp(37))
        })

        val periodeBarnOverAtten = Periode(barnBlirAttenPå, periode.tom)
        val barnErOverAtten = barnetillegg.begrensetTil(periodeBarnOverAtten)
        assertThat(barnErOverAtten).isNotEmpty
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
        person.barn.forEach { FakePersoner.leggTil(it) }

        val ident = person.aktivIdent()

        val behandling = sendInnSøknad(
            ident, periode, TestSøknader.STANDARD_SØKNAD
        )
            .løsSykdom()
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()

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
                        barn = person.barn.map { Barn(it.aktivIdent(), it.fødselsdato) }),
                    oppgitteBarn = null,
                    vurderteBarn = null
                )
            )

        val uthentetTilkjentYtelse =
            requireNotNull(dataSource.transaction { TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id) })
            { "Tilkjent ytelse skal være beregnet her." }

        val barnetillegg = uthentetTilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

        val begrensetTilRettighetsperioden = barnetillegg.begrensetTil(periode)
        assertThat(begrensetTilRettighetsperioden).isNotEmpty
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
        val ident = person.aktivIdent()

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

        sendInnSøknad(
            ident, periode, søknad
        )
            .løsSykdom()
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
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
                        )
                    ),
                ),
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).describedAs("Vi avklarte bare ett barn, behovet skal fortsatt være åpent")
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
                        )
                    ),
                ),
            )
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

                println(tilkjentYtelse)
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

        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnSøknad(ident, periode, TestSøknader.STANDARD_SØKNAD)
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
                assertThat(åpneAvklaringsbehov).isNotEmpty()
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsSykdom()
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()

        val sak = hentSak(ident, periode)

        // Sender inn meldekort
        behandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("220"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    MeldekortV0(
                        harDuArbeidet = false,
                        timerArbeidPerPeriode = listOf(
                            ArbeidIPeriodeV0(
                                fraOgMedDato = LocalDate.now().minusMonths(3),
                                tilOgMedDato = LocalDate.now().plusMonths(3),
                                timerArbeid = 0.0,
                            )
                        )
                    ),
                ),
                periode = periode
            )
        )
            .kvalitetssikreOk()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Ikke årsakssammenheng",
                        relevanteSaker = listOf(),
                        andelAvNedsettelsen = null,
                        erÅrsakssammenheng = false
                    )
                ),
            )
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
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
                // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtakEllerSendRetur(returVed = Definisjon.AVKLAR_SYKDOM)
            .løsSykdom()
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Ikke årsakssammenheng",
                        relevanteSaker = listOf(),
                        andelAvNedsettelsen = null,
                        erÅrsakssammenheng = false
                    )
                )
            )
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
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
            .fattVedtakEllerSendRetur()
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

        var brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)

        // Det er bestilt vedtaksbrev som er klar for forhåndsvisning og editering
        assertThat(brevbestilling.status).isEqualTo(
            no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR
        )

        behandling = behandling.medKontekst {
            // Venter på at brevet skal fullføres
            assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.SKRIV_VEDTAKSBREV) }
        }
            .løsVedtaksbrev()

        brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        // Brevet er fullført
        assertThat(brevbestilling.status).isEqualTo(
            no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT
        )
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

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

        sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("299"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                strukturertDokument = StrukturertDokument(TestSøknader.STANDARD_SØKNAD),
                periode = periode
            )
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
    fun `trukket søknad blokkerer nye ytelsesbehandlinger`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnSøknad(ident, periode, TestSøknader.STANDARD_SØKNAD)

        løsSykdom(behandling)
        leggTilÅrsakForBehandling(behandling, listOf(VurderingsbehovMedPeriode(Vurderingsbehov.SØKNAD_TRUKKET)))
        assertThat(hentAlleAvklaringsbehov(behandling)).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.erÅpent() && avklaringsbehov.definisjon == Definisjon.VURDER_TREKK_AV_SØKNAD).isTrue() }

        behandling = løsAvklaringsBehov(
            behandling,
            TrekkSøknadLøsning(begrunnelse = "trekker søknaden"),
        )

        assertThat(hentAlleAvklaringsbehov(behandling)).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.erAvsluttet()).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        behandling = sendInnSøknad(
            ident, periode,
            SøknadV0(
                student = SøknadStudentDto("NEI"),
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

        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnSøknad(ident, periode, TestSøknader.STANDARD_SØKNAD)

        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        løsSykdom(behandling)

        behandling = løsAvklaringsBehov(
            behandling,
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
        )

        behandling = løsAvklaringsBehov(
            behandling,
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

        behandling = løsSykdomsvurderingBrev(behandling)

        behandling = kvalitetssikreOk(behandling)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarYrkesskadeLøsning(
                yrkesskadesvurdering = YrkesskadevurderingDto(
                    begrunnelse = "Veldig relevante",
                    relevanteSaker = person.yrkesskade.map { it.saksreferanse },
                    andelAvNedsettelsen = 50,
                    erÅrsakssammenheng = true
                )
            ),
        )

        behandling = løsAvklaringsBehov(
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

        behandling = løsAvklaringsBehov(
            behandling,
            FastsettYrkesskadeInntektLøsning(
                yrkesskadeInntektVurdering = BeregningYrkeskaderBeløpVurderingDTO(
                    vurderinger = person.yrkesskade.map {
                        YrkesskadeBeløpVurderingDTO(
                            antattÅrligInntekt = Beløp(5000000),
                            referanse = it.saksreferanse,
                            begrunnelse = "Trenger hjelp fra Nav",
                        )
                    },
                )
            )
        )

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())

        // Saken står til To-trinnskontroll hos beslutter
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = fattVedtakEllerSendRetur(behandling)
        var brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)

        assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)
        // Det er bestilt vedtaksbrev som er klar for forhåndsvisning og editering
        assertThat(brevBestilling.status).isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR)

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        // Venter på at brevet skal fullføres
        assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.SKRIV_VEDTAKSBREV) }

        brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        behandling =
            løsAvklaringsBehov(behandling, vedtaksbrevLøsning(brevBestilling.referanse.brevbestillingReferanse))

        brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        // Brevet er fullført
        assertThat(brevBestilling.status).isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT)
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

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
        val person = TestPersoner.STANDARD_PERSON()

        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnSøknad(ident, periode, TestSøknader.STANDARD_SØKNAD)

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(
            behandling,
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

        behandling = løsAvklaringsBehov(
            behandling,
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

        behandling = løsSykdomsvurderingBrev(behandling)

        behandling = kvalitetssikreOk(behandling)

        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKEPENGEERSTATNING) }

        behandling = løsAvklaringsBehov(
            behandling, AvklarSykepengerErstatningLøsning(
                sykepengeerstatningVurdering = SykepengerVurderingDto(
                    begrunnelse = "...",
                    dokumenterBruktIVurdering = listOf(),
                    harRettPå = true,
                    grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR
                ),
            )
        )

        behandling = løsAvklaringsBehov(
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

        behandling = løsForutgåendeMedlemskap(behandling)
        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())

        // Saken står til To-trinnskontroll hos beslutter
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = fattVedtakEllerSendRetur(behandling)

        assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)

        var resultat =
            dataSource.transaction { ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id) }
        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)

        behandling = behandling.løsVedtaksbrev()

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder()).hasSize(1)
            .first()
            .extracting(Vilkårsperiode::erOppfylt, Vilkårsperiode::innvilgelsesårsak)
            .containsExactly(true, Innvilgelsesårsak.SYKEPENGEERSTATNING)

        resultat =
            dataSource.transaction { ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id) }
        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)

        assertTidslinje(
            vilkårsresultat.rettighetstypeTidslinje(),
            periode to {
                assertThat(it).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
            })

        // Verifisere at det går an å kun 1 mnd med sykepengeerstatning
        val revurdering = sendInnDokument(
            ident,
            DokumentMottattPersonHendelse(
                journalpost = JournalpostId("123123"),
                mottattTidspunkt = LocalDateTime.now(),
                innsendingType = InnsendingType.MANUELL_REVURDERING,
                strukturertDokument = StrukturertDokument(
                    ManuellRevurderingV0(
                        årsakerTilBehandling = listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
                        beskrivelse = "..."
                    )
                ),
                periode = periode
            ),
        )
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    sykdomsvurderinger = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            harSkadeSykdomEllerLyte = false,
                            erSkadeSykdomEllerLyteVesentligdel = false,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = null,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = null,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            vurderingenGjelderFra = periode.fom.plusMonths(1),
                        )
                    )
                )
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov.map { it.definisjon }).containsOnly(Definisjon.FATTE_VEDTAK)

                val underveisTidslinje = dataSource.transaction {
                    UnderveisRepositoryImpl(it).hent(this.behandling.id).perioder
                }.map { Segment(it.periode, it) }.let(::Tidslinje)

                val oppfyltPeriode = underveisTidslinje.filter { it.verdi.rettighetsType != null }.helePerioden()

                assertThat(oppfyltPeriode.fom).isEqualTo(periode.fom)
                assertThat(oppfyltPeriode.tom).isEqualTo(periode.fom.plusMonths(1).minusDays(1))
            }
    }

    @Test
    fun `avslag på 11-6 er også inngang til 11-13`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val person = TestPersoner.STANDARD_PERSON()

        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnSøknad(ident, periode, TestSøknader.STANDARD_SØKNAD)
            .medKontekst {
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
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
                        begrunnelse = "Trenger  hjelp fra nav",
                        erBehovForAktivBehandling = false,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = false,
                        skalVurdereAapIOvergangTilUføre = false,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = "Nope"
                    ),
                ),
            )
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKEPENGEERSTATNING) }
            }
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "...",
                        dokumenterBruktIVurdering = listOf(),
                        harRettPå = true,
                        grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR
                    ),
                )
            )
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtakEllerSendRetur()
            .medKontekst {
                assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)
            }

        var resultat =
            dataSource.transaction { ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id) }
        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)

        behandling = behandling.løsVedtaksbrev()

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
            periode to {
                assertThat(it).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
            })
    }

    @Test
    fun `ved avslag på 11-5 hoppes det rett til beslutter-steget`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        val person = TestPersoner.STANDARD_PERSON()

        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnSøknad(ident, periode, TestSøknader.STANDARD_SØKNAD)

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

        behandling = løsSykdomsvurderingBrev(behandling)

        behandling = kvalitetssikreOk(behandling)

        // Saken står til To-trinnskontroll hos beslutter
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.FATTE_VEDTAK) }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = fattVedtakEllerSendRetur(behandling)
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
    fun `to-trinn og ingen endring i gruppe etter sendt tilbake fra beslutter`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val person = TestPersoner.PERSON_MED_YRKESSKADE()

        val ident = person.aktivIdent()

        // Sender inn en søknad
        sendInnSøknad(
            ident, periode,
            TestSøknader.SØKNAD_STUDENT
        ).medKontekst {
            assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
            assertThat(åpneAvklaringsbehov).isNotEmpty()
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }.løsAvklaringsBehov(
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
        ).løsAvklaringsBehov(
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
        )
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
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "",
                        relevanteSaker = listOf(),
                        andelAvNedsettelsen = null,
                        erÅrsakssammenheng = false
                    )
                ),
            )
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
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                // Saken står til To-trinnskontroll hos beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtakEllerSendRetur(returVed = Definisjon.AVKLAR_SYKDOM)
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
            .fattVedtakEllerSendRetur()
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
        val person = TestPersoner.STANDARD_PERSON()
        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnSøknad(ident, periode, TestSøknader.STANDARD_SØKNAD)

        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(
            behandling, AvklarSykdomLøsning(
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
        )

        behandling = løsAvklaringsBehov(
            behandling,
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
        )

        behandling = løsAvklaringsBehov(
            behandling,
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

        behandling = løsSykdomsvurderingBrev(behandling)

        behandling = kvalitetssikreOk(behandling)

        behandling = løsAvklaringsBehov(
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

        behandling = løsForutgåendeMedlemskap(behandling)
        behandling = løsAvklaringsBehov(
            behandling, AvklarSamordningGraderingLøsning(
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

        behandling = løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())

        // Beslutter godkjenner ikke samordning gradering
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        behandling = løsAvklaringsBehov(
            behandling, FatteVedtakLøsning(
                alleAvklaringsbehov
                    .filter { behov -> behov.erTotrinn() }
                    .map { behov ->
                        TotrinnsVurdering(
                            behov.definisjon.kode,
                            behov.definisjon != Definisjon.AVKLAR_SAMORDNING_GRADERING,
                            "begrunnelse",
                            emptyList()
                        )
                    }), Bruker("BESLUTTER")
        )
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        // Avklar samordning gradering gjenåpnes, behandlingen står i samordning-steget
        assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.AVKLAR_SAMORDNING_GRADERING).isTrue() }
        assertThat(behandling.aktivtSteg()).isEqualTo(StegType.SAMORDNING_GRADERING)
    }

    @Test
    fun `Ikke oppfylt på grunn av alder på søknadstidspunkt`(hendelser: List<StoppetBehandling>) {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val person = TestPersoner.PERSON_FOR_UNG()

        val ident = person.aktivIdent()

        sendInnSøknad(
            ident, periode, TestSøknader.STANDARD_SØKNAD
        )

        val sak = hentSak(ident, periode)
        var behandling = hentNyesteBehandlingForSak(sak.id)
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
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "JA", "NEI", "NEI", null)
            )
        ).medKontekst {
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(Definisjon.AVKLAR_SYKDOM)
        }

        hendelsesMottak.håndtere(
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

        sendInnSøknad(
            ident, periode,
            SøknadV0(
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
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
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        val behandling = sendInnSøknad(ident, periode, TestSøknader.STANDARD_SØKNAD)
            .medKontekst {
                // Validér avklaring
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }
            }

        // Oppretter bestilling av legeerklæring
        hendelsesMottak.bestillLegeerklæring(behandling.id)
        util.ventPåSvar(behandling.id.toLong())

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
        util.ventPåSvar()

        // Validér avklaring
        behandling.medKontekst {
            val legeerklæringBestillingVenteBehov =
                åpneAvklaringsbehov.filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
            assertThat(legeerklæringBestillingVenteBehov.isEmpty()).isTrue()
        }
    }

    @Test
    fun `Fjerner legeerklæring ventebehov ved mottak av legeerklæring`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        sendInnSøknad(
            ident, periode,
            SøknadV0(
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "JA", "NEI", "NEI", listOf())
            ),
        )

        val sak = hentSak(ident, periode)
        val behandling = hentNyesteBehandlingForSak(sak.id)

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }

        // Oppretter bestilling av legeerklæring
        hendelsesMottak.bestillLegeerklæring(behandling.id)
        util.ventPåSvar(behandling.id.toLong())

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING).isTrue() }

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
        util.ventPåSvar(sak.id.toLong())

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)

        val legeerklæringBestillingVenteBehov =
            åpneAvklaringsbehov.filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
        assertThat(legeerklæringBestillingVenteBehov.isEmpty()).isTrue()

    }

    @Test
    fun `Fjerner legeerklæring ventebehov ved mottak av dialogmelding`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        sendInnSøknad(
            ident, periode, TestSøknader.STANDARD_SØKNAD
        )

        val sak = hentSak(ident, periode)
        val behandling = hentNyesteBehandlingForSak(sak.id)

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }

        // Oppretter bestilling av legeerklæring
        hendelsesMottak.bestillLegeerklæring(behandling.id)

        assertThat(hentÅpneAvklaringsbehov(behandling)).anySatisfy { assertThat(it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING).isTrue() }

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
        util.ventPåSvar()

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        val legeerklæringBestillingVenteBehov =
            åpneAvklaringsbehov.filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
        assertThat(legeerklæringBestillingVenteBehov.isEmpty()).isTrue()
    }

    @Test
    fun `Lager avklaringsbehov i medlemskap når kravene til manuell avklaring oppfylles`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        val behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("NEI", null, "JA", null, null)
            )
        )

        // Validér avklaring
        val åpenAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpenAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })
    }

    @Test
    fun `Går automatisk forbi medlemskap når kravene til manuell avklaring ikke oppfylles`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        val behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
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
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
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
        val ident = nyPerson(harYrkesskade = false, harUtenlandskOpphold = true)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        var behandling = sendInnSøknad(
            ident, periode,
            SøknadV0(
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null),
            ),
        )

        løsFramTilForutgåendeMedlemskap(behandling, harYrkesskade = false)

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

        behandling
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtakEllerSendRetur()
            .løsVedtaksbrev()

        val revurdering = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("12344932122"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                strukturertDokument = StrukturertDokument(
                    ManuellRevurderingV0(
                        årsakerTilBehandling = listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP), ""
                    ),
                ),
                innsendingType = InnsendingType.MANUELL_REVURDERING,
                periode = periode
            )
        ).medKontekst {
            assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
            assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
        }

        val revurderingÅpneAvklaringsbehov = hentÅpneAvklaringsbehov(revurdering.id)
        assertTrue(revurderingÅpneAvklaringsbehov.any { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
    }

    @Test
    fun `Oppfyller ikke forutgående medlemskap når unntak ikke oppfylles og ikke medlem i folketrygden`() {
        val ident = nyPerson(harYrkesskade = false, harUtenlandskOpphold = true)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        var behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto(
                    "JA", null, "NEI", null, null
                )
            )
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
                    "begrunnelseforutgående", false, varMedlemMedNedsattArbeidsevne = false, medlemMedUnntakAvMaksFemAar = null
                )
            )
        )

        // Validér riktig resultat
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder()
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
        assertTrue(vilkårsResultat.none { it.erOppfylt() })
    }

    @Test
    fun `Oppfyller forutgående medlemskap når unntak finnes`() {
        val ident = nyPerson(harYrkesskade = false, harUtenlandskOpphold = true)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        var behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto(
                    "JA", null, "NEI", null, null
                ),
            )
        )

        behandling = løsFramTilForutgåendeMedlemskap(behandling, harYrkesskade = false)

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })

        // Trigger manuell vurdering
        behandling = løsAvklaringsBehov(
            behandling, AvklarForutgåendeMedlemskapLøsning(
                manuellVurderingForForutgåendeMedlemskap = ManuellVurderingForForutgåendeMedlemskapDto(
                    "begrunnelse", true, true, null
                ),
                behovstype = AvklaringsbehovKode.`5020`
            )
        )

        // Validér riktig resultat
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder()
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
        assertTrue(vilkårsResultat.all { it.erOppfylt() })

        // Teste å trekke søknad
        leggTilÅrsakForBehandling(behandling, listOf(VurderingsbehovMedPeriode(Vurderingsbehov.SØKNAD_TRUKKET)))
    }

    @Test
    fun `Går forbi forutgåendemedlemskapsteget når yrkesskade eksisterer`() {
        val ident = nyPerson(harYrkesskade = true, harUtenlandskOpphold = false)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        var behandling = sendInnSøknad(
            ident, periode,
            SøknadV0(
                student = SøknadStudentDto("NEI"), yrkesskade = "JA", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null),
            ),
        )

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
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
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
        behandling = løsAvklaringsBehov(
            behandling,
            AvklarLovvalgMedlemskapLøsning(
                manuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskapDto(
                    LovvalgVedSøknadsTidspunktDto("crazy lovvalgsland vurdering", null),
                    MedlemskapVedSøknadsTidspunktDto("crazy medlemskap vurdering", true)
                ),
                behovstype = AvklaringsbehovKode.`5017`
            )
        )

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
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
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
        behandling = løsAvklaringsBehov(
            behandling, AvklarLovvalgMedlemskapLøsning(
                manuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskapDto(
                    LovvalgVedSøknadsTidspunktDto("crazy lovvalgsland vurdering", EØSLand.DNK),
                    MedlemskapVedSøknadsTidspunktDto(null, null)
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
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
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
        behandling = løsAvklaringsBehov(
            behandling, AvklarLovvalgMedlemskapLøsning(
                manuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskapDto(
                    LovvalgVedSøknadsTidspunktDto("crazy lovvalgsland vurdering", EØSLand.NOR),
                    MedlemskapVedSøknadsTidspunktDto("crazy medlemskap vurdering", false)
                )
            )
        )

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
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
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
        var behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        )

        behandling = løsAvklaringsBehov(
            behandling, AvklarOverstyrtLovvalgMedlemskapLøsning(
                manuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskapDto(
                    LovvalgVedSøknadsTidspunktDto("crazy lovvalgsland vurdering", EØSLand.NOR),
                    MedlemskapVedSøknadsTidspunktDto("crazy medlemskap vurdering", false)
                )
            )
        )

        // Validér avklaring
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { Definisjon.MANUELL_OVERSTYRING_LOVVALG == it.definisjon })

        // Validér riktig resultat
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        assertTrue(vilkårsResultat.none { it.erOppfylt() })
        assertTrue(Avslagsårsak.IKKE_MEDLEM == vilkårsResultat.first().avslagsårsak)
    }

    @Test
    fun `Kan løse overstyringsbehov til oppfylt`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        var behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        )

        behandling = løsAvklaringsBehov(
            behandling, AvklarOverstyrtLovvalgMedlemskapLøsning(
                manuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskapDto(
                    LovvalgVedSøknadsTidspunktDto("crazy lovvalgsland vurdering", EØSLand.NOR),
                    MedlemskapVedSøknadsTidspunktDto("crazy medlemskap vurdering", true)
                )
            )
        )

        // Validér avklaring
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { Definisjon.MANUELL_OVERSTYRING_LOVVALG == it.definisjon })

        // Validér riktig resultat
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        val overstyrtManuellVurdering = dataSource.transaction {
            MedlemskapArbeidInntektRepositoryImpl(it).hentHvisEksisterer(behandling.id)?.manuellVurdering?.overstyrt
        }
        assertTrue(vilkårsResultat.all { it.erOppfylt() })
        assertTrue(overstyrtManuellVurdering == true)
    }

    @Test
    fun `kan hente inn manuell inntektsdata i grunnlag og benytte i beregning`() {
        val ident = nyPerson(false, false, mutableListOf())
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val nedsattDato = LocalDate.now()

        // Oppretter vanlig søknad
        val behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        )
        løsFramTilGrunnlag(behandling)

        løsAvklaringsBehov(
            behandling,
            FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurderingDto(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = nedsattDato,
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            ),
        )
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { Definisjon.FASTSETT_MANUELL_INNTEKT == it.definisjon })

        løsAvklaringsBehov(
            behandling,
            AvklarManuellInntektVurderingLøsning(
                manuellVurderingForManglendeInntekt = ManuellInntektVurderingDto(
                    begrunnelse = "Mangler ligning",
                    belop = BigDecimal(300000),
                )
            )
        )

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).noneMatch { it.definisjon == Definisjon.FASTSETT_MANUELL_INNTEKT }

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
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        )

        løsFramTilGrunnlag(behandling)

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
    fun `kan ikke sende inn negativ manuell inntekt`() {
        val ident = nyPerson(false, false, mutableListOf())
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val nedsattDato = LocalDate.now()

        // Oppretter vanlig søknad
        val behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        )

        løsFramTilGrunnlag(behandling)

        løsAvklaringsBehov(
            behandling,
            FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurderingDto(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = nedsattDato,
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            ),
        )
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { Definisjon.FASTSETT_MANUELL_INNTEKT == it.definisjon })

        assertThrows<UgyldigForespørselException> {
            løsAvklaringsBehov(
                behandling,
                AvklarManuellInntektVurderingLøsning(
                    manuellVurderingForManglendeInntekt = ManuellInntektVurderingDto(
                        begrunnelse = "Mangler ligning",
                        belop = BigDecimal(-1),
                    )
                )
            )
        }

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).anyMatch { it.definisjon == Definisjon.FASTSETT_MANUELL_INNTEKT }
    }

    @Test
    fun `kan tilbakeføre behandling til start`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        val behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
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

        val behandlingId = behandling.id

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandlingId)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })

        // Trigger manuell vurdering
        løsAvklaringsBehov(
            behandling, AvklarLovvalgMedlemskapLøsning(
                manuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskapDto(
                    LovvalgVedSøknadsTidspunktDto("crazy lovvalgsland vurdering", EØSLand.NOR),
                    MedlemskapVedSøknadsTidspunktDto(null, true)
                )
            )
        )

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandlingId)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_SYKDOM })

        dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            assertThat(behandlingRepo.hent(behandlingId).aktivtSteg()).isEqualTo(StegType.AVKLAR_SYKDOM)

            // Tilbakefør med hjelpefunksjon
            Driftfunksjoner(postgresRepositoryRegistry.provider(connection)).flyttBehandlingTilStart(
                behandlingId,
                connection
            )

            // Validér avklaring
            assertThat(behandlingRepo.hent(behandlingId).aktivtSteg()).isEqualTo(StegType.START_BEHANDLING)
        }

        util.ventPåSvar()
        val b = hentNyesteBehandlingForSak(behandling.sakId)
        assertThat(b.aktivtSteg()).isEqualTo(StegType.AVKLAR_SYKDOM)
    }

    @Test
    fun `Teste Klageflyt`() {
        val person = TestPersoner.PERSON_FOR_UNG()
        val ident = person.aktivIdent()

        val periode = Periode(LocalDate.now().minusMonths(3), LocalDate.now().plusYears(3))

        // Avslås pga. alder
        val avslåttFørstegang = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto("NEI"),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            )
        )
        assertThat(avslåttFørstegang)
            .describedAs("Førstegangsbehandlingen skal være satt som avsluttet")
            .extracting { b -> b.status().erAvsluttet() }.isEqualTo(true)
        val kravMottatt = LocalDate.now().minusMonths(1)
        val klagebehandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("4002"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                InnsendingType.KLAGE,
                strukturertDokument = StrukturertDokument(KlageV0(kravMottatt = kravMottatt)),
                periode
            )
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

        util.ventPåSvar(klagebehandling.sakId.id)

        // OmgjøringSteg
        dataSource.transaction { connection ->
            val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)

            val manuellRevurdering = mottattDokumentRepository.hentDokumenterAvType(
                klagebehandling.sakId,
                InnsendingType.MANUELL_REVURDERING
            )

            assertThat(manuellRevurdering).hasSize(1).first()
                .extracting(MottattDokument::strukturertDokument)
                .isNotNull
            assertThat(
                manuellRevurdering.first().strukturerteData<ManuellRevurderingV0>()?.data?.beskrivelse
            ).isEqualTo("Revurdering etter klage som tas til følge. Følgende vilkår omgjøres: § 11-5")
        }

        val revurdering = hentNyesteBehandlingForSak(klagebehandling.sakId, listOf(TypeBehandling.Revurdering))
        assertThat(revurdering.vurderingsbehov()).containsExactly(VurderingsbehovMedPeriode(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND))

        // OpprettholdelseSteg
        val steghistorikk = hentStegHistorikk(klagebehandling.id)
        assertThat(steghistorikk)
            .anySatisfy { it -> assertThat(it.steg() == StegType.OPPRETTHOLDELSE && it.status() == StegStatus.AVSLUTTER).isTrue }

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
                student = SøknadStudentDto("NEI"),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            )
        )

        assertThat(avslåttFørstegang)
            .describedAs("Førstegangsbehandlingen skal være satt som avsluttet")
            .extracting { b -> b.status().erAvsluttet() }.isEqualTo(true)
        val kravMottatt = LocalDate.now().minusMonths(1)
        val klagebehandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("4001"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                InnsendingType.KLAGE,
                strukturertDokument = StrukturertDokument(KlageV0(kravMottatt = kravMottatt)),
                periode
            )
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
                        "21049599999",
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
                student = SøknadStudentDto("NEI"),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            ),
        )

        assertThat(avslåttFørstegang)
            .describedAs("Førstegangsbehandlingen skal være satt som avsluttet")
            .extracting { b -> b.status().erAvsluttet() }.isEqualTo(true)
        val kravMottatt = LocalDate.now().minusMonths(1)
        val klagebehandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("4006"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                InnsendingType.KLAGE,
                strukturertDokument = StrukturertDokument(KlageV0(kravMottatt = kravMottatt)),
                periode
            )
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
                    likevelBehandles = true,
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
                    innstilling = KlageInnstilling.OPPRETTHOLD,
                    vilkårSomOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5),
                    vilkårSomOmgjøres = emptyList()
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
        assertThat(åpneAvklaringsbehov).hasSize(4)
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
                student = SøknadStudentDto("NEI"),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            )
        )

        assertThat(avslåttFørstegang)
            .describedAs("Førstegangsbehandlingen skal være satt som avsluttet")
            .extracting { b -> b.status() }.isIn(Status.IVERKSETTES, Status.AVSLUTTET)

        val kravMottatt = LocalDate.now().minusMonths(1)
        val klagebehandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("4005"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                InnsendingType.KLAGE,
                strukturertDokument = StrukturertDokument(KlageV0(kravMottatt = kravMottatt)),
                periode
            )
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

        val trekkKlageBehandling = sendInnDokument(
            ident, NyÅrsakTilBehandlingHendelse(
                referanse = InnsendingReferanse(
                    type = InnsendingReferanse.Type.BEHANDLING_REFERANSE,
                    verdi = klagebehandling.referanse.referanse.toString()
                ),
                mottattTidspunkt = LocalDateTime.now().minusMonths(4),
                innsendingType = InnsendingType.NY_ÅRSAK_TIL_BEHANDLING,
                strukturertDokument = StrukturertDokument(
                    NyÅrsakTilBehandlingV0(
                        årsakerTilBehandling = listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.KLAGE_TRUKKET)
                    ),
                ),
                periode = periode
            )
        )

        // Sjekk at Klagen nå har fått "KLAGE_TRUKKET" som årsak til behandling (og derfor er i riktig tilstand)
        util.ventPåSvar()
        assertThat(trekkKlageBehandling.id).isEqualTo(klagebehandling.id)
        assertThat(trekkKlageBehandling.vurderingsbehov().map { it.type }).contains(Vurderingsbehov.KLAGE_TRUKKET)

        // Løs avklaringsbehovet som trekker klagen og trigger sletting - skal og sette klagen til avsluttet
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(trekkKlageBehandling.id)
        assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(Definisjon.VURDER_TREKK_AV_KLAGE)

        løsAvklaringsBehov(
            trekkKlageBehandling,
            avklaringsBehovLøsning = TrekkKlageLøsning(
                vurdering = TrekkKlageVurderingDto(
                    begrunnelse = "Begrunnelse",
                    skalTrekkes = true,
                    hvorforTrekkes = TrekkKlageÅrsakDto.FEILREGISTRERING
                )
            )
        )

        val avsluttetBehandling = hentBehandling(trekkKlageBehandling.referanse)
        assertThat(avsluttetBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Håndtere svar fra kabal - valg omgjøring skal opprette en revurdering`() {
        val person = TestPersoner.PERSON_FOR_UNG()
        val ident = person.aktivIdent()

        val periode = Periode(LocalDate.now().minusMonths(3), LocalDate.now().plusYears(3))

        // Avslås pga. alder
        val avslåttFørstegang = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto("NEI"),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            )
        )
        assertThat(avslåttFørstegang)
            .describedAs("Førstegangsbehandlingen skal være satt som avsluttet")
            .extracting { b -> b.status().erAvsluttet() }.isEqualTo(true)
        val kravMottatt = LocalDate.now().minusMonths(1)
        val klagebehandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("401"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                InnsendingType.KLAGE,
                strukturertDokument = StrukturertDokument(KlageV0(kravMottatt = kravMottatt)),
                periode
            )
        )

        assertThat(klagebehandling.referanse).isNotEqualTo(avslåttFørstegang.referanse)
        assertThat(klagebehandling.typeBehandling()).isEqualTo(TypeBehandling.Klage)

        var svarFraAndreinstansBehandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("402"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                InnsendingType.KABAL_HENDELSE,
                strukturertDokument = StrukturertDokument(
                    KabalHendelseV0(
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
                ),
                periode
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

        util.ventPåSvar(sakId = svarFraAndreinstansBehandling.sakId.id)

        val revurdering = hentNyesteBehandlingForSak(svarFraAndreinstansBehandling.sakId)
        assertThat(revurdering).isNotNull
        assertThat(revurdering.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
        assertThat(revurdering.vurderingsbehov().map { it.type }).contains(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)
    }

    @Test
    fun `Skal kunne overstyre rettighetsperioden`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        val nyStartDato = periode.fom.minusDays(7)

        val behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto(
                    "JA", "JA", "NEI", null, emptyList()
                ),
            )
        )

        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.AVKLAR_SYKDOM)
        løsAvklaringsBehov(
            behandling = behandling,
            avklaringsBehovLøsning = VurderRettighetsperiodeLøsning(
                rettighetsperiodeVurdering = RettighetsperiodeVurderingDTO(
                    startDato = nyStartDato,
                    begrunnelse = "En begrunnelse",
                    harRettUtoverSøknadsdato = true,
                    harKravPåRenter = false,
                )
            )
        )

        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.AVKLAR_SYKDOM)

        val oppdatertSak = hentSak(ident, periode)

        assertThat(oppdatertSak.rettighetsperiode).isNotEqualTo(periode)
        assertThat(oppdatertSak.rettighetsperiode).isEqualTo(
            Periode(
                nyStartDato,
                nyStartDato.plusYears(1).minusDays(1)
            )
        )
    }
}
