package no.nav.aap.behandlingsflyt.repository.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLand
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
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

class RefusjonkravRepositoryImplTest {
    @Test
    fun henterRelaterteHistoriskeVurderinger() {
        InitTestDatabase.dataSource.transaction { connection ->
            val personOgSakService = PersonOgSakService(FakePdlGateway, PersonRepositoryImpl(connection), SakRepositoryImpl(connection))
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val repo = MedlemskapArbeidInntektRepositoryImpl(connection)

            val sak = personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))

            val behandling1 = behandlingRepo.opprettBehandling(sak.id, listOf(), TypeBehandling.Førstegangsbehandling, null)
            val behandlingId1 = behandling1.id

            lagNyFullVurdering(behandlingId1, repo, "Første begrunnelse")

            val behandling2 = behandlingRepo.opprettBehandling(sak.id, listOf(), TypeBehandling.Revurdering, null)
            val behandlingId2 = behandling2.id

            lagNyFullVurdering(behandlingId2, repo, "Andre begrunnelse")

            val historikk = repo.hentHistoriskeVurderinger(sak.id)
            assertEquals(2, historikk.size)

            val sak2 = personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
            val behandling1sak1 = behandlingRepo.opprettBehandling(sak2.id, listOf(), TypeBehandling.Førstegangsbehandling, null)
            val behandlingId1sak1 = behandling1sak1.id
            lagNyFullVurdering(behandlingId1sak1, repo, "Random begrunnelse")

            val nyHistorikk = repo.hentHistoriskeVurderinger(sak.id)
            assertEquals(2, nyHistorikk.size)
        }
    }

    private fun lagNyFullVurdering(behandlingId: BehandlingId, repo: MedlemskapArbeidInntektRepositoryImpl, begrunnelse: String) {
        repo.lagreArbeidsforholdOgInntektINorge(behandlingId, listOf(), listOf(), null)
        repo.lagreManuellVurdering(behandlingId,
            ManuellVurderingForLovvalgMedlemskap(
                LovvalgVedSøknadsTidspunktDto(begrunnelse, EØSLand.NOR),
                MedlemskapVedSøknadsTidspunktDto(begrunnelse, true),
                "SAKSBEHANDLER"
            )
        )
        repo.lagreOppgittUtenlandsOppplysninger(behandlingId, JournalpostId("1"), UtenlandsOppholdData(true, false, false, false, null))
    }
}