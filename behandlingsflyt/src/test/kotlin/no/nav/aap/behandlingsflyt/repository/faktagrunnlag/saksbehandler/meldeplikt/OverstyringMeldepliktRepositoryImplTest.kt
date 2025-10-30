package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.meldeplikt


import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktOverstyringStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktVurderingPeriode
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.september
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.dbtest.TestDataSource.Companion.invoke
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OverstyringMeldepliktRepositoryImplTest {
    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    @AutoClose
    private val dataSource = TestDataSource()

    @Test
    fun `Finner ikke rimeligGrunnVurderinger hvis ikke lagret`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val meldepliktOverstyringRepository = OverstyringMeldepliktRepositoryImpl(connection)
            val meldepliktOverstyringGrunnlag = meldepliktOverstyringRepository.hentHvisEksisterer(behandling.id)
            assertThat(meldepliktOverstyringGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter rimeligGrunnVurderinger`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val meldepliktOverstyringRepository = OverstyringMeldepliktRepositoryImpl(connection)
            val periode = OverstyringMeldepliktVurderingPeriode(fom = 13 august 2023, tom = 26 august 2023, begrunnelse = "en begrunnelse", meldepliktOverstyringStatus = MeldepliktOverstyringStatus.RIMELIG_GRUNN)
            val overstyringMeldepliktVurdering = OverstyringMeldepliktVurdering(
                vurdertAv = "veileder",
                opprettetTid = null,
                vurdertIBehandling = behandling.referanse,
                perioder = listOf(periode)
            )

            meldepliktOverstyringRepository.lagre(behandling.id, overstyringMeldepliktVurdering)
            val meldepliktOverstyringGrunnlag = meldepliktOverstyringRepository.hentHvisEksisterer(behandling.id)
            assertThat(meldepliktOverstyringGrunnlag?.vurderinger).hasSize(1)

            val vurdering = meldepliktOverstyringGrunnlag?.vurderinger?.first()
            assertThat(vurdering?.vurdertAv).isEqualTo("veileder")
            assertThat(vurdering?.perioder).hasSize(1)
            assertThat(vurdering?.perioder?.first()).isEqualTo(periode)
        }
    }

    @Test
    fun `Lagre 2 ganger på samme behandling skal føre til at man henter grunn som kun inneholder den siste lagrede vurderingen`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val meldepliktOverstyringRepository = OverstyringMeldepliktRepositoryImpl(connection)
            val periode1 = OverstyringMeldepliktVurderingPeriode(fom = 13 august 2023, tom = 26 august 2023, begrunnelse = "en begrunnelse", meldepliktOverstyringStatus = MeldepliktOverstyringStatus.RIMELIG_GRUNN)
            val periode2 = OverstyringMeldepliktVurderingPeriode(fom = 27 august 2023, tom = 7 september 2023, begrunnelse = "en til befunnelse", meldepliktOverstyringStatus = MeldepliktOverstyringStatus.IKKE_MELDT_SEG)
            val periode3 = OverstyringMeldepliktVurderingPeriode(fom = 9 september  2023, tom = 26 september 2023, begrunnelse = "mer begrunnelse", meldepliktOverstyringStatus = MeldepliktOverstyringStatus.HAR_MELDT_SEG)

            val overstyringMeldepliktVurdering1 = OverstyringMeldepliktVurdering(
                vurdertAv = "veileder1",
                opprettetTid = null,
                vurdertIBehandling = behandling.referanse,
                perioder = listOf(periode1, periode2)
            )

            val overstyringMeldepliktVurdering2 = OverstyringMeldepliktVurdering(
                vurdertAv = "veileder2",
                opprettetTid = null,
                vurdertIBehandling = behandling.referanse,
                perioder = listOf(periode3)
            )

            meldepliktOverstyringRepository.lagre(behandling.id, overstyringMeldepliktVurdering1)
            meldepliktOverstyringRepository.lagre(behandling.id, overstyringMeldepliktVurdering2)
            val meldepliktOverstyringGrunnlag = meldepliktOverstyringRepository.hentHvisEksisterer(behandling.id)
            assertThat(meldepliktOverstyringGrunnlag?.vurderinger).hasSize(1)

            val vurdering = meldepliktOverstyringGrunnlag?.vurderinger?.first()
            assertThat(vurdering?.vurdertAv).isEqualTo("veileder2")
            assertThat(vurdering?.perioder).hasSize(1)
            assertThat(vurdering?.perioder).contains(periode3)
        }
    }

    @Test
    fun `Kopier skal lage ey nytt grunnlag med de samme vurderingene`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val revurdering = behandlingRepository.opprettBehandling(sak.id, TypeBehandling.Revurdering, behandling.id, VurderingsbehovOgÅrsak(emptyList(), ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE))


            val meldepliktOverstyringRepository = OverstyringMeldepliktRepositoryImpl(connection)
            val periode1 = OverstyringMeldepliktVurderingPeriode(fom = 13 august 2023, tom = 26 august 2023, begrunnelse = "en begrunnelse", meldepliktOverstyringStatus = MeldepliktOverstyringStatus.RIMELIG_GRUNN)
            val periode2 = OverstyringMeldepliktVurderingPeriode(fom = 27 august 2023, tom = 7 september 2023, begrunnelse = "en til befunnelse", meldepliktOverstyringStatus = MeldepliktOverstyringStatus.IKKE_MELDT_SEG)
            val periode3 = OverstyringMeldepliktVurderingPeriode(fom = 9 september  2023, tom = 26 september 2023, begrunnelse = "mer begrunnelse", meldepliktOverstyringStatus = MeldepliktOverstyringStatus.HAR_MELDT_SEG)

            val overstyringMeldepliktVurdering1 = OverstyringMeldepliktVurdering(
                vurdertAv = "veileder1",
                opprettetTid = null,
                vurdertIBehandling = behandling.referanse,
                perioder = listOf(periode1, periode2)
            )

            val overstyringMeldepliktVurdering2 = OverstyringMeldepliktVurdering(
                vurdertAv = "veileder2",
                opprettetTid = null,
                vurdertIBehandling = behandling.referanse,
                perioder = listOf(periode3)
            )

            meldepliktOverstyringRepository.lagre(behandling.id, overstyringMeldepliktVurdering1)
            meldepliktOverstyringRepository.lagre(behandling.id, overstyringMeldepliktVurdering2)
            meldepliktOverstyringRepository.kopier(behandling.id, revurdering.id)
            val meldepliktOverstyringGrunnlag = meldepliktOverstyringRepository.hentHvisEksisterer(revurdering.id)
            assertThat(meldepliktOverstyringGrunnlag?.vurderinger).hasSize(1)

            val vurdering = meldepliktOverstyringGrunnlag?.vurderinger?.first()
            assertThat(vurdering?.vurdertAv).isEqualTo("veileder2")
            assertThat(vurdering?.perioder).hasSize(1)
            assertThat(vurdering?.perioder).contains(periode3)
        }
    }

    @Test
    fun `Lagre 2 ganger på samme behandling skal føre til at man henter grunn som kun inneholder den siste lagrede vurderingen samt vurderinger fra vedtatte grunnlag`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val revurdering = behandlingRepository.opprettBehandling(
                sak.id,
                TypeBehandling.Revurdering,
                behandling.id,
                VurderingsbehovOgÅrsak(emptyList(), ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE)
            )

            val meldepliktOverstyringRepository = OverstyringMeldepliktRepositoryImpl(connection)
            
            val origPeriode1 = OverstyringMeldepliktVurderingPeriode(
                fom = 13 august 2023,
                tom = 26 august 2023,
                begrunnelse = "original begrunnelse 1",
                meldepliktOverstyringStatus = MeldepliktOverstyringStatus.RIMELIG_GRUNN
            )
            val origPeriode2 = OverstyringMeldepliktVurderingPeriode(
                fom = 27 august 2023,
                tom = 7 september 2023,
                begrunnelse = "original begrunnelse 2",
                meldepliktOverstyringStatus = MeldepliktOverstyringStatus.IKKE_MELDT_SEG
            )

            val originalVurdering = OverstyringMeldepliktVurdering(
                vurdertAv = "veilederOriginal",
                opprettetTid = null,
                vurdertIBehandling = behandling.referanse,
                perioder = listOf(origPeriode1, origPeriode2)
            )

            meldepliktOverstyringRepository.lagre(behandling.id, originalVurdering)
            meldepliktOverstyringRepository.kopier(behandling.id, revurdering.id)

            // Første lagring på revurdering
            val revPeriodeA1 = OverstyringMeldepliktVurderingPeriode(
                fom = 9 september  2023,
                tom = 26 september 2023,
                begrunnelse = "rev A",
                meldepliktOverstyringStatus = MeldepliktOverstyringStatus.HAR_MELDT_SEG
            )
            val firstRevVurdering = OverstyringMeldepliktVurdering(
                vurdertAv = "veilederRevurdering1",
                opprettetTid = null,
                vurdertIBehandling = revurdering.referanse,
                perioder = listOf(revPeriodeA1)
            )
            meldepliktOverstyringRepository.lagre(revurdering.id, firstRevVurdering)

            // Andre lagring på revurdering (skal erstatte første)
            val revPeriodeB1 = OverstyringMeldepliktVurderingPeriode(
                fom = 1 september 2023,
                tom = 8 september 2023,
                begrunnelse = "rev B",
                meldepliktOverstyringStatus = MeldepliktOverstyringStatus.RIMELIG_GRUNN
            )
            val secondRevVurdering = OverstyringMeldepliktVurdering(
                vurdertAv = "veilederRevurdering2",
                opprettetTid = null,
                vurdertIBehandling = revurdering.referanse,
                perioder = listOf(revPeriodeB1)
            )
            meldepliktOverstyringRepository.lagre(revurdering.id, secondRevVurdering)
            
            val grunnlagForRevurdering = meldepliktOverstyringRepository.hentHvisEksisterer(revurdering.id)
            assertThat(grunnlagForRevurdering).isNotNull()

            // Forvent at original vurderinger fortsatt er med
            assertThat(grunnlagForRevurdering!!.vurderinger.any { it.vurdertAv == "veilederOriginal" && it.perioder.containsAll(listOf(origPeriode1, origPeriode2)) }).isTrue()

            // Forvent at første lagring på revurdering IKKE er med lenger 
            assertThat(grunnlagForRevurdering.vurderinger.any { it.vurdertAv == "veilederRevurdering1" || it.perioder.contains(revPeriodeA1) }).isFalse()

            // Forvent at siste lagring på revurdering er med
            assertThat(grunnlagForRevurdering.vurderinger.any { it.vurdertAv == "veilederRevurdering2" && it.perioder.contains(revPeriodeB1) }).isTrue()
        }
    }

    @Test
    fun `etter å ha slettet grunnlaget for en behandling skal den være borte fra databasen`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val meldepliktOverstyringRepository = OverstyringMeldepliktRepositoryImpl(connection)
            val periode1 = OverstyringMeldepliktVurderingPeriode(fom = 13 august 2023, tom = 26 august 2023, begrunnelse = "en begrunnelse", meldepliktOverstyringStatus = MeldepliktOverstyringStatus.RIMELIG_GRUNN)
            val periode2 = OverstyringMeldepliktVurderingPeriode(fom = 27 august 2023, tom = 7 september 2023, begrunnelse = "en til befunnelse", meldepliktOverstyringStatus = MeldepliktOverstyringStatus.IKKE_MELDT_SEG)
            val periode3 = OverstyringMeldepliktVurderingPeriode(fom = 9 september  2023, tom = 26 september 2023, begrunnelse = "mer begrunnelse", meldepliktOverstyringStatus = MeldepliktOverstyringStatus.HAR_MELDT_SEG)

            val overstyringMeldepliktVurdering1 = OverstyringMeldepliktVurdering(
                vurdertAv = "veileder1",
                opprettetTid = null,
                vurdertIBehandling = behandling.referanse,
                perioder = listOf(periode1, periode2)
            )

            val overstyringMeldepliktVurdering2 = OverstyringMeldepliktVurdering(
                vurdertAv = "veileder2",
                opprettetTid = null,
                vurdertIBehandling = behandling.referanse,
                perioder = listOf(periode3)
            )

            meldepliktOverstyringRepository.lagre(behandling.id, overstyringMeldepliktVurdering1)
            meldepliktOverstyringRepository.lagre(behandling.id, overstyringMeldepliktVurdering2)

            meldepliktOverstyringRepository.slett(behandling.id)

            val meldepliktOverstyringGrunnlag = meldepliktOverstyringRepository.hentHvisEksisterer(behandling.id)
            assertThat(meldepliktOverstyringGrunnlag).isNull()
        }
    }

    @Test
    fun `om man lager et grunnlag som kopieres til en ny behandling og sletter det originale grunnlaget så skal kopien fortsatt eksistere`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val revurdering = behandlingRepository.opprettBehandling(sak.id, TypeBehandling.Revurdering, behandling.id, VurderingsbehovOgÅrsak(emptyList(), ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE))

            val meldepliktOverstyringRepository = OverstyringMeldepliktRepositoryImpl(connection)
            val periode1 = OverstyringMeldepliktVurderingPeriode(fom = 13 august 2023, tom = 26 august 2023, begrunnelse = "en begrunnelse", meldepliktOverstyringStatus = MeldepliktOverstyringStatus.RIMELIG_GRUNN)
            val periode2 = OverstyringMeldepliktVurderingPeriode(fom = 27 august 2023, tom = 7 september 2023, begrunnelse = "en til befunnelse", meldepliktOverstyringStatus = MeldepliktOverstyringStatus.IKKE_MELDT_SEG)

            val overstyringMeldepliktVurdering1 = OverstyringMeldepliktVurdering(
                vurdertAv = "veileder1",
                opprettetTid = null,
                vurdertIBehandling = behandling.referanse,
                perioder = listOf(periode1, periode2)
            )

            meldepliktOverstyringRepository.lagre(behandling.id, overstyringMeldepliktVurdering1)
            meldepliktOverstyringRepository.kopier(behandling.id, revurdering.id)

            meldepliktOverstyringRepository.slett(behandling.id)

            val grunnlagOriginal = meldepliktOverstyringRepository.hentHvisEksisterer(behandling.id)
            val grunnlagKopi = meldepliktOverstyringRepository.hentHvisEksisterer(revurdering.id)
            assertThat(grunnlagOriginal).isNull()
            assertThat(grunnlagKopi).isNotNull()
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }
}