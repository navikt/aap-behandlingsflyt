package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Year

class ManuellInntektGrunnlagRepositoryImplTest {

    @Test
    fun `lagre og hente ut igjen`() {
        val dataSource = InitTestDatabase.freshDatabase()

        val behandling = dataSource.transaction {
            val sak = sak(it, Periode(1 januar 2023, 31 desember 2023))
            finnEllerOpprettBehandling(it, sak)
        }

        val manuellVurdering = ManuellInntektVurdering(
            år = Year.of(2024),
            begrunnelse = "hipp som happ",
            belop = BigDecimal(123.40).let(::Beløp),
            vurdertAv = "Kungen"
        )
        dataSource.transaction {
            val repo = ManuellInntektGrunnlagRepositoryImpl(it)

            repo.lagre(behandling.id, manuellVurdering)
        }

        dataSource.transaction {
            val repo = ManuellInntektGrunnlagRepositoryImpl(it)

            val uthentet = repo.hentHvisEksisterer(behandling.id)

            assertThat(uthentet).isNotNull.extracting { it!!.manuelleInntekter }
                .usingRecursiveComparison().withEqualsForType(
                    { a, b -> a.minus(b).abs().toDouble() < 0.0001 },
                    BigDecimal::class.java
                )
                .ignoringFields("opprettet")
                .isEqualTo(setOf(manuellVurdering))
        }

        // Sett inn på samme år, nytt beløp
        dataSource.transaction {
            val repo = ManuellInntektGrunnlagRepositoryImpl(it)

            repo.lagre(behandling.id, manuellVurdering.copy(belop = BigDecimal(123.41).let(::Beløp)))
            val uthentet = repo.hentHvisEksisterer(behandling.id)
            assertThat(uthentet).isNotNull.extracting { it!!.manuelleInntekter }
                .usingRecursiveComparison().withEqualsForType(
                    { a, b -> a.minus(b).abs().toDouble() < 0.0001 },
                    BigDecimal::class.java
                )
                .ignoringFields("opprettet")
                .isEqualTo(setOf(manuellVurdering.copy(belop = BigDecimal(123.41).let(::Beløp))))
        }

        // Test sletting
        dataSource.transaction {
            val repo = ManuellInntektGrunnlagRepositoryImpl(it)
            repo.slett(behandling.id)
            val uthentet = repo.hentHvisEksisterer(behandling.id)
            assertThat(uthentet).isNull()
        }
    }

    @Test
    fun `lagre for flere år og hente ut igjen`() {
        val dataSource = InitTestDatabase.freshDatabase()

        val behandling = dataSource.transaction {
            val sak = sak(it, Periode(1 januar 2023, 31 desember 2023))
            finnEllerOpprettBehandling(it, sak)
        }

        val manuellVurdering2024 = ManuellInntektVurdering(
            år = Year.of(2024),
            begrunnelse = "begrunnelse 2024",
            belop = BigDecimal(100).let(::Beløp),
            vurdertAv = "saksbehandler"
        )

        val manuellVurdering2025 = ManuellInntektVurdering(
            år = Year.of(2025),
            begrunnelse = "begrunnelse 2025",
            belop = BigDecimal(200).let(::Beløp),
            vurdertAv = "saksbehandler"
        )

        dataSource.transaction {
            val repo = ManuellInntektGrunnlagRepositoryImpl(it)

            repo.lagre(behandling.id, setOf(manuellVurdering2024, manuellVurdering2025))
            val uthentet = repo.hentHvisEksisterer(behandling.id)

            assertThat(uthentet?.manuelleInntekter).hasSize(2)
        }
    }

    @Test
    fun `lagre for flere år og så fjerne et år`() {
        val dataSource = InitTestDatabase.freshDatabase()

        val behandling = dataSource.transaction {
            val sak = sak(it, Periode(1 januar 2023, 31 desember 2023))
            finnEllerOpprettBehandling(it, sak)
        }

        val manuellVurdering2024 = ManuellInntektVurdering(
            år = Year.of(2024),
            begrunnelse = "begrunnelse 2024",
            belop = BigDecimal(100).let(::Beløp),
            vurdertAv = "saksbehandler"
        )

        val manuellVurdering2025 = ManuellInntektVurdering(
            år = Year.of(2025),
            begrunnelse = "begrunnelse 2025",
            belop = BigDecimal(200).let(::Beløp),
            vurdertAv = "saksbehandler"
        )

        dataSource.transaction {
            val repo = ManuellInntektGrunnlagRepositoryImpl(it)

            repo.lagre(behandling.id, setOf(manuellVurdering2024, manuellVurdering2025))
            val uthentet = repo.hentHvisEksisterer(behandling.id)

            assertThat(uthentet?.manuelleInntekter).hasSize(2)

            repo.lagre(behandling.id, setOf(manuellVurdering2024))
            val uthentetEtterSletting = repo.hentHvisEksisterer(behandling.id)

            assertThat(uthentetEtterSletting?.manuelleInntekter).hasSize(1)
        }
    }

    private fun sak(connection: DBConnection, periode: Periode): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }
}