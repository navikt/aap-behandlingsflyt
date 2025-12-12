package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.ÅrsakTilReturKode
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BekreftTotalvurderingKlageLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBehandlendeEnhetLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettFullmektigLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettPåklagetBehandlingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FullmektigLøsningDto
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.HåndterSvarFraAndreinstansLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.HåndterSvarFraAndreinstansLøsningDto
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivForhåndsvarselKlageFormkravBrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.TrekkKlageLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VentePåFristForhåndsvarselKlageFormkravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderFormkravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageKontorLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageNayLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.behandling.trekkklage.flate.TrekkKlageVurderingDto
import no.nav.aap.behandlingsflyt.behandling.trekkklage.flate.TrekkKlageÅrsakDto
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
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansKonsekvens
import no.nav.aap.behandlingsflyt.integrasjon.kabal.Fagsystem
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlingDetaljer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlingEventType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageUtfall
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlagebehandlingAvsluttetDetaljer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OmgjøringKlageRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.AVBRYT_REVURDERING
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.SEND_FORVALTNINGSMELDING
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.START_BEHANDLING
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.SØKNAD
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.VURDER_RETTIGHETSPERIODE
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.FormkravRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus

class KlageFlytTest: AbstraktFlytOrkestratorTest(FakeUnleash::class) {
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

        val behandling = hentBehandling(klagebehandling.referanse)
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
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

}