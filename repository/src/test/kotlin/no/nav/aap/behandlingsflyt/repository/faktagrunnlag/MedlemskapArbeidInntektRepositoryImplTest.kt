package no.nav.aap.behandlingsflyt.repository.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.lovvalg.EnhetGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLand
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektInformasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Inntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Virksomhet
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

internal class MedlemskapArbeidInntektRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun mapperOrgnavnKorrektTilInntekt() {
        dataSource.transaction { connection ->
            val personOgSakService = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            )
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val repo = MedlemskapArbeidInntektRepositoryImpl(connection)

            val sak = personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
            val behandling = behandlingRepo.opprettBehandling(sak.id, listOf(), TypeBehandling.Førstegangsbehandling, null)
            lagNyFullVurdering(behandling.id, repo, "Første begrunnelse")

            val lagretInntekt = repo.hentHvisEksisterer(behandling.id)!!

            val inntekt1 = lagretInntekt.inntekterINorgeGrunnlag.first{it.identifikator == "1234"}
            val inntekt2 = lagretInntekt.inntekterINorgeGrunnlag.first{it.identifikator == "4321"}

            assertEquals(inntekt1.organisasjonsNavn, "Bepis AS")
            assertEquals(inntekt1.identifikator, "1234")
            assertEquals(inntekt2.organisasjonsNavn, "Rotte AS")
            assertEquals(inntekt2.identifikator, "4321")
        }
    }

    @Test
    fun henterRelaterteHistoriskeVurderinger() {
        // Førstegangsbehandling
        val behandling = dataSource.transaction { connection ->
            val personOgSakService = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            )
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val repo = MedlemskapArbeidInntektRepositoryImpl(connection)

            val sak =
                personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
            val behandling =
                behandlingRepo.opprettBehandling(sak.id, listOf(), TypeBehandling.Førstegangsbehandling, null)
            lagNyFullVurdering(behandling.id, repo, "Første begrunnelse")

            val historikk = repo.hentHistoriskeVurderinger(sak.id, behandling.id)
            assertEquals(0, historikk.size)

            behandling
        }

        // Revurdering
        dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val repo = MedlemskapArbeidInntektRepositoryImpl(connection)

            val revurdering =
                behandlingRepo.opprettBehandling(behandling.sakId, listOf(), TypeBehandling.Revurdering, behandling.id)

            val historikk = repo.hentHistoriskeVurderinger(revurdering.sakId, revurdering.id)
            lagNyFullVurdering(revurdering.id, repo, "Andre begrunnelse")
            assertEquals(1, historikk.size)
        }
    }

    private fun lagNyFullVurdering(
        behandlingId: BehandlingId,
        repo: MedlemskapArbeidInntektRepositoryImpl,
        begrunnelse: String
    ) {
        repo.lagreArbeidsforholdOgInntektINorge(behandlingId, listOf(),
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
        repo.lagreManuellVurdering(
            behandlingId,
            ManuellVurderingForLovvalgMedlemskap(
                LovvalgVedSøknadsTidspunktDto(begrunnelse, EØSLand.NOR),
                MedlemskapVedSøknadsTidspunktDto(begrunnelse, true),
                "SAKSBEHANDLER",
                LocalDateTime.now()
            )
        )
        repo.lagreOppgittUtenlandsOppplysninger(
            behandlingId,
            JournalpostId("1"),
            UtenlandsOppholdData(true, false, false, false, null)
        )
    }
}