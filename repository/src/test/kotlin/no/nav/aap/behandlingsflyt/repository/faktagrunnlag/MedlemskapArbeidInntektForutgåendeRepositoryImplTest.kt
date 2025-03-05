package no.nav.aap.behandlingsflyt.repository.faktagrunnlag

import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektForutgåendeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import java.time.LocalDate

class MedlemskapArbeidInntektForutgåendeRepositoryImplTest {
    @Test
    fun henterRelaterteHistoriskeVurderinger() {
        InitTestDatabase.dataSource.transaction { connection ->
            val personOgSakService = PersonOgSakService(FakePdlGateway, PersonRepositoryImpl(connection), SakRepositoryImpl(connection))
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val forutgåendeRepo = MedlemskapArbeidInntektForutgåendeRepositoryImpl(connection)

            val sak = personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))

            val behandling1 = behandlingRepo.opprettBehandling(sak.id, listOf(), TypeBehandling.Førstegangsbehandling, null)
            val behandlingId1 = behandling1.id

            lagNyFullVurdering(behandlingId1, forutgåendeRepo, "Første begrunnelse", connection)

            val behandling2 = behandlingRepo.opprettBehandling(sak.id, listOf(), TypeBehandling.Revurdering, null)
            val behandlingId2 = behandling2.id

            lagNyFullVurdering(behandlingId2, forutgåendeRepo, "Andre begrunnelse", connection)

            val historikk = forutgåendeRepo.hentHistoriskeVurderinger(sak.id)
            assertEquals(2, historikk.size)

            val sak2 = personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
            val behandling1sak1 = behandlingRepo.opprettBehandling(sak2.id, listOf(), TypeBehandling.Førstegangsbehandling, null)
            val behandlingId1sak1 = behandling1sak1.id
            lagNyFullVurdering(behandlingId1sak1, forutgåendeRepo, "Random begrunnelse", connection)

            val nyHistorikk = forutgåendeRepo.hentHistoriskeVurderinger(sak.id)
            assertEquals(2, nyHistorikk.size)
        }
    }

    private fun lagNyFullVurdering(behandlingId: BehandlingId, forutgåendeRepository: MedlemskapArbeidInntektForutgåendeRepositoryImpl, begrunnelse: String, connection: DBConnection) {
        val lovvalgRepository = MedlemskapArbeidInntektRepositoryImpl(connection)
        lovvalgRepository.lagreOppgittUtenlandsOppplysninger(behandlingId, JournalpostId("1"), UtenlandsOppholdData(true, false, false, false, null))

        forutgåendeRepository.lagreArbeidsforholdOgInntektINorge(behandlingId, listOf(), listOf(), null)
        forutgåendeRepository.lagreManuellVurdering(behandlingId,
            ManuellVurderingForForutgåendeMedlemskap(
                begrunnelse, true, false, false
            )
        )
    }
}