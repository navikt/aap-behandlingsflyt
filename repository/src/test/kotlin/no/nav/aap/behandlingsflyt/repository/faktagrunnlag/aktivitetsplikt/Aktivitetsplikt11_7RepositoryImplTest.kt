package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.dbtest.TestDataSource.Companion.invoke
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.*

internal class Aktivitetsplikt11_7RepositoryImplTest {

    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    @AutoClose
    private val dataSource = TestDataSource()

    @Test
    fun `Lagrer ned og henter vurdering av aktivitetsplikt § 11-7`() {
        dataSource.transaction { connection ->
            val sak = opprettSak(connection, periode)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val aktivitetspliktRepository = Aktivitetsplikt11_7RepositoryImpl(connection)
            val vurdering = Aktivitetsplikt11_7Vurdering(
                begrunnelse = "Begrunnelse",
                erOppfylt = true,
                vurdertAv = "ident",
                gjelderFra = 1 januar 2023,
                opprettet = Instant.parse("2023-01-01T12:00:00Z"),
                vurdertIBehandling = behandling.id,
                skalIgnorereVarselFrist = false
            )

            aktivitetspliktRepository.lagre(behandling.id, listOf(vurdering))
            val grunnlag = aktivitetspliktRepository.hentHvisEksisterer(behandling.id)

            assertThat(grunnlag).isEqualTo(Aktivitetsplikt11_7Grunnlag(listOf(vurdering)))

            val varselBrevbestillingReferanse = BrevbestillingReferanse(UUID.randomUUID())
            val varselSendt = LocalDate.of(2023, 1, 1)
            val frist = LocalDate.of(2023, 1, 25)

            aktivitetspliktRepository.lagreVarsel(behandling.id, varselBrevbestillingReferanse)
            aktivitetspliktRepository.lagreFrist(behandling.id, varselSendt, frist)

            val vurdering2 = Aktivitetsplikt11_7Vurdering(
                begrunnelse = "Begrunnelse 2",
                erOppfylt = false,
                utfall = Utfall.STANS,
                vurdertAv = "ident2",
                gjelderFra = 1 februar 2023,
                opprettet = Instant.parse("2023-01-02T12:10:00Z"),
                vurdertIBehandling = behandling.id,
                skalIgnorereVarselFrist = false
            )
            val nyTidslinje = grunnlag!!.tidslinje()
                .kombiner(
                    Tidslinje(Periode(vurdering2.gjelderFra, LocalDate.MAX), vurdering2),
                    StandardSammenslåere.prioriterHøyreSideCrossJoin()
                )

            aktivitetspliktRepository.lagre(
                behandling.id,
                nyTidslinje.segmenter().map { it.verdi }
            )

            val grunnlag2 = aktivitetspliktRepository.hentHvisEksisterer(behandling.id)
            grunnlag2!!.tidslinje().assertTidslinje(
                Segment(Periode(1 januar 2023, 31 januar 2023)) {
                    assertThat(it).isEqualTo(vurdering)
                },
                Segment(Periode(1 februar 2023, LocalDate.MAX)) {
                    assertThat(it).isEqualTo(vurdering2)
                }
            )

            val varsel = aktivitetspliktRepository.hentVarselHvisEksisterer(behandling.id)
            assertThat(varsel).isNotNull
            assertThat(varsel?.sendtDato).isEqualTo(varselSendt)
            assertThat(varsel?.svarfrist).isEqualTo(frist)
            assertThat(varsel?.varselId).isEqualTo(varselBrevbestillingReferanse)
        }
    }
}
