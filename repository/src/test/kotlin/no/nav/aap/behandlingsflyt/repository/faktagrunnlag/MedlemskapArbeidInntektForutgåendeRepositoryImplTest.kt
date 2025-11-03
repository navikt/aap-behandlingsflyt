package no.nav.aap.behandlingsflyt.repository.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.lovvalg.EnhetGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektInformasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Inntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Virksomhet
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektForutgåendeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

internal class MedlemskapArbeidInntektForutgåendeRepositoryImplTest {
    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }


    @Test
    fun mapperOrgnavnKorrektTilForutgåendeInntekt() {
        dataSource.transaction { connection ->
            val personOgSakService = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            )
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val medlemskapArbeidInntektForutgåendeRepo = MedlemskapArbeidInntektForutgåendeRepositoryImpl(connection)

            val sak =
                personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
            val behandling =
                behandlingRepo.opprettBehandling(
                    sak.id,
                    TypeBehandling.Førstegangsbehandling,
                    null,
                    VurderingsbehovOgÅrsak(
                        listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                        ÅrsakTilOpprettelse.SØKNAD
                    )
                )
            lagNyFullVurdering(behandling.id, medlemskapArbeidInntektForutgåendeRepo, "Første begrunnelse", connection)

            val lagretInntekt = medlemskapArbeidInntektForutgåendeRepo.hentHvisEksisterer(behandling.id)!!

            val inntekt1 = lagretInntekt.inntekterINorgeGrunnlag.first { it.identifikator == "1234" }
            val inntekt2 = lagretInntekt.inntekterINorgeGrunnlag.first { it.identifikator == "4321" }

            assertEquals(inntekt1.organisasjonsNavn, "Bepis AS")
            assertEquals(inntekt1.identifikator, "1234")
            assertEquals(inntekt2.organisasjonsNavn, "Rotte AS")
            assertEquals(inntekt2.identifikator, "4321")
        }
    }

    @Test
    fun `kan hente siste relevante utenlandsopplysning`() {
        val sak = dataSource.transaction { connection ->
            val personOgSakService =
                PersonOgSakService(
                    FakePdlGateway,
                    PersonRepositoryImpl(connection),
                    SakRepositoryImpl(connection)
                )
            personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
        }

        val sak2 = dataSource.transaction { connection ->
            val personOgSakService =
                PersonOgSakService(
                    FakePdlGateway,
                    PersonRepositoryImpl(connection),
                    SakRepositoryImpl(connection)
                )
            personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
        }

        dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val arbeidInntektRepo = MedlemskapArbeidInntektRepositoryImpl(connection)

            val førstegangsBehandling =
                opprettBehandlingMedVurdering(TypeBehandling.Førstegangsbehandling, sak.id, null, emptyList(), null)
            val revurdering = opprettBehandlingMedVurdering(
                TypeBehandling.Revurdering,
                sak.id,
                førstegangsBehandling.id,
                emptyList(),
                UtenlandsOppholdData(
                    harBoddINorgeSiste5År = true,
                    harArbeidetINorgeSiste5År = false,
                    arbeidetUtenforNorgeFørSykdom = false,
                    iTilleggArbeidUtenforNorge = false,
                    utenlandsOpphold = null
                )
            )
            opprettBehandlingMedVurdering(
                TypeBehandling.Revurdering,
                sak.id,
                revurdering.id,
                listOf(VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_MEDLEMSKAP)),
                null
            )

            // Random ny behandling uten kobling
            behandlingRepo.opprettBehandling(
                sak2.id,
                TypeBehandling.Førstegangsbehandling,
                null,
                VurderingsbehovOgÅrsak(
                    listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    ÅrsakTilOpprettelse.SØKNAD
                )
            )

            val sisteUtenlandsOppholdData =
                arbeidInntektRepo.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sak.id)
            assertEquals(sisteUtenlandsOppholdData?.harBoddINorgeSiste5År, true)
        }
    }

    @Test
    fun `henter relaterte historiske vurderinger`() {
        val sak = dataSource.transaction { connection ->
            val personOgSakService =
                PersonOgSakService(
                    FakePdlGateway,
                    PersonRepositoryImpl(connection),
                    SakRepositoryImpl(connection)
                )
            personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
        }

        val sak2 = dataSource.transaction { connection ->
            val personOgSakService =
                PersonOgSakService(
                    FakePdlGateway,
                    PersonRepositoryImpl(connection),
                    SakRepositoryImpl(connection)
                )
            personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
        }

        dataSource.transaction { connection ->
            val forutgåendeRepo = MedlemskapArbeidInntektForutgåendeRepositoryImpl(connection)

            val førstegangsBehandling =
                opprettBehandlingMedVurdering(TypeBehandling.Førstegangsbehandling, sak.id, null, emptyList(), null)
            val revurdering = opprettBehandlingMedVurdering(
                TypeBehandling.Revurdering,
                sak.id,
                førstegangsBehandling.id,
                emptyList(),
                null
            )

            val historikk = forutgåendeRepo.hentHistoriskeVurderinger(sak.id, revurdering.id)
            assertEquals(1, historikk.size)
            opprettBehandlingMedVurdering(TypeBehandling.Førstegangsbehandling, sak2.id, null, emptyList(), null)

            val nyHistorikk = forutgåendeRepo.hentHistoriskeVurderinger(sak.id, revurdering.id)
            assertEquals(1, nyHistorikk.size)
        }
    }

    private fun opprettBehandlingMedVurdering(
        typeBehandling: TypeBehandling,
        sakId: SakId,
        forrigeBehandlingId: BehandlingId?,
        årsaker: List<VurderingsbehovMedPeriode>,
        utenlandsOppholdData: UtenlandsOppholdData?
    )
            : Behandling {
        return dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val forutgåendeRepo = MedlemskapArbeidInntektForutgåendeRepositoryImpl(connection)
            val behandling = behandlingRepo.opprettBehandling(
                sakId,
                typeBehandling,
                forrigeBehandlingId,
                VurderingsbehovOgÅrsak(årsaker, ÅrsakTilOpprettelse.SØKNAD)
            )
            lagNyFullVurdering(behandling.id, forutgåendeRepo, "Heftig vurdering", connection, utenlandsOppholdData)
            behandling
        }
    }

    private fun lagNyFullVurdering(
        behandlingId: BehandlingId,
        forutgåendeRepository: MedlemskapArbeidInntektForutgåendeRepositoryImpl,
        begrunnelse: String,
        connection: DBConnection,
        utenlandsOppholdData: UtenlandsOppholdData? = UtenlandsOppholdData(
            harBoddINorgeSiste5År = false,
            harArbeidetINorgeSiste5År = false,
            arbeidetUtenforNorgeFørSykdom = false,
            iTilleggArbeidUtenforNorge = false,
            utenlandsOpphold = null
        )
    ) {
        val lovvalgRepository = MedlemskapArbeidInntektRepositoryImpl(connection)
        if (utenlandsOppholdData != null) lovvalgRepository.lagreOppgittUtenlandsOppplysninger(
            behandlingId,
            JournalpostId("1"),
            utenlandsOppholdData
        )
        forutgåendeRepository.lagreArbeidsforholdOgInntektINorge(
            behandlingId, emptyList(),
            listOf(
                ArbeidsInntektMaaned(
                    aarMaaned = YearMonth.now(),
                    arbeidsInntektInformasjon = ArbeidsInntektInformasjon(
                        listOf(
                            Inntekt(
                                beloep = 1.0,
                                opptjeningsland = null,
                                skattemessigBosattLand = null,
                                opptjeningsperiodeFom = null,
                                opptjeningsperiodeTom = null,
                                virksomhet = Virksomhet(
                                    identifikator = "1234"
                                ),
                                beskrivelse = null
                            ),
                            Inntekt(
                                beloep = 1.0,
                                opptjeningsland = null,
                                skattemessigBosattLand = null,
                                opptjeningsperiodeFom = null,
                                opptjeningsperiodeTom = null,
                                virksomhet = Virksomhet(
                                    identifikator = "4321"
                                ),
                                beskrivelse = null
                            ),
                        )
                    )
                ),
            ),
            null,
            enhetGrunnlag = listOf(
                EnhetGrunnlag("1234", "Bepis AS"),
                EnhetGrunnlag("4321", "Rotte AS")
            )
        )
        forutgåendeRepository.lagreManuellVurdering(
            behandlingId,
            ManuellVurderingForForutgåendeMedlemskap(
                begrunnelse = begrunnelse,
                harForutgåendeMedlemskap = true,
                varMedlemMedNedsattArbeidsevne = false,
                medlemMedUnntakAvMaksFemAar = false,
                vurdertAv = "NavIdent",
                vurdertTidspunkt = LocalDateTime.now()
            )
        )
    }
}