package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusForeldrepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusSykepengerGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.periodisering.VurderingTilBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering.SamordningVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class SamordningYtelseVurderingServiceTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            GatewayRegistry
                .register<AbakusForeldrepengerGateway>()
                .register<AbakusSykepengerGateway>()
        }
    }

    @Test
    fun `krever avklaring når endringer kommer`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val ytelseRepo = SamordningYtelseRepositoryImpl(connection)
            val repo = SamordningVurderingRepositoryImpl(connection)
            val sakRepository = SakRepositoryImpl(connection)
            val service = SamordningYtelseVurderingService(
                SamordningYtelseRepositoryImpl(connection),
                SakService(sakRepository),
                FakeTidligereVurderinger(),
            )
            val kontekst = opprettSakdata(connection)

            // Når det ikke finnes data
            val ingenData = service.oppdater(kontekst)
            assertEquals(Informasjonskrav.Endret.IKKE_ENDRET, ingenData)

            // Data er uforandret
            val sammeData = service.oppdater(kontekst)
            assertEquals(Informasjonskrav.Endret.IKKE_ENDRET, sammeData)

            // Ny data har kommet inn
            opprettYtelseData(ytelseRepo, kontekst.behandlingId)
            opprettVurderingData(repo, kontekst.behandlingId)
            val nyData = service.oppdater(kontekst)
            assertEquals(Informasjonskrav.Endret.ENDRET, nyData)
        }
    }

    private fun opprettVurderingData(repo: SamordningVurderingRepositoryImpl, behandlingId: BehandlingId) {
        repo.lagreVurderinger(
            behandlingId,
            SamordningVurderingGrunnlag(
                begrunnelse = "En god begrunnelse",
                maksDatoEndelig = false,
                maksDato = LocalDate.now().plusYears(1),
                vurderinger = listOf(
                    SamordningVurdering(
                        Ytelse.SYKEPENGER,

                        listOf(
                            SamordningVurderingPeriode(
                                Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
                                Prosent(50),
                                0,
                                false
                            )
                        )
                    )
                )
            )
        )
    }

    private fun opprettYtelseData(repo: SamordningYtelseRepositoryImpl, behandlingId: BehandlingId) {
        repo.lagre(
            behandlingId,
            listOf(
                SamordningYtelse(
                    Ytelse.SYKEPENGER,
                    listOf(
                        SamordningYtelsePeriode(
                            Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
                            Prosent(50),
                            0
                        )
                    ),
                    "kilde",
                    "ref"
                )
            )
        )
    }

    private fun opprettSakdata(connection: DBConnection): FlytKontekstMedPerioder {
        val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("ident", true)))
        val rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusDays(5))
        val sakId = SakRepositoryImpl(connection).finnEllerOpprett(
            person,
            rettighetsperiode
        ).id
        val behandlingId = BehandlingRepositoryImpl(connection).opprettBehandling(
            sakId,
            listOf(),
            TypeBehandling.Førstegangsbehandling,
            null
        ).id
        return FlytKontekstMedPerioder(
            sakId, behandlingId, null, TypeBehandling.Førstegangsbehandling,
            VurderingTilBehandling(VurderingType.FØRSTEGANGSBEHANDLING, rettighetsperiode, setOf())
        )
    }
}