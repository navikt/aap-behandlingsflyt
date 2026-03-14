package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class VedtakslengdeGrunnlagTest {

    private val behandling1 = BehandlingId(1)
    private val behandling2 = BehandlingId(2)
    private val saksbehandler = Bruker("Z999999")

    private fun automatiskVurdering(
        sluttdato: LocalDate,
        behandlingId: BehandlingId = behandling1,
        opprettet: Instant = Instant.parse("2024-01-01T12:00:00Z"),
    ) = VedtakslengdeVurdering(
        sluttdato = sluttdato,
        utvidetMed = ÅrMedHverdager.FØRSTE_ÅR,
        vurdertAv = SYSTEMBRUKER,
        vurdertIBehandling = behandlingId,
        opprettet = opprettet,
        begrunnelse = "",
    )

    private fun manuellVurdering(
        sluttdato: LocalDate,
        behandlingId: BehandlingId = behandling1,
        opprettet: Instant = Instant.parse("2024-01-01T12:00:00Z"),
    ) = VedtakslengdeVurdering(
        sluttdato = sluttdato,
        utvidetMed = ÅrMedHverdager.FØRSTE_ÅR,
        vurdertAv = saksbehandler,
        vurdertIBehandling = behandlingId,
        opprettet = opprettet,
        begrunnelse = "manuell begrunnelse",
    )

    @Nested
    inner class GjeldendeVurderingTest {

        @Test
        fun `returnerer null når det ikke finnes vurderinger`() {
            val grunnlag = VedtakslengdeGrunnlag(emptyList())
            assertThat(grunnlag.gjeldendeVurdering()).isNull()
        }

        @Test
        fun `returnerer eneste vurdering`() {
            val vurdering = automatiskVurdering(LocalDate.of(2025, 1, 1))
            val grunnlag = VedtakslengdeGrunnlag(listOf(vurdering))
            assertThat(grunnlag.gjeldendeVurdering()).isEqualTo(vurdering)
        }

        @Test
        fun `returnerer siste vurdering fra siste behandling`() {
            val førsteVurdering = automatiskVurdering(
                sluttdato = LocalDate.of(2025, 1, 1),
                behandlingId = behandling1,
                opprettet = Instant.parse("2024-01-01T12:00:00Z"),
            )
            val sisteVurdering = automatiskVurdering(
                sluttdato = LocalDate.of(2026, 1, 1),
                behandlingId = behandling2,
                opprettet = Instant.parse("2025-01-01T12:00:00Z"),
            )
            val grunnlag = VedtakslengdeGrunnlag(listOf(førsteVurdering, sisteVurdering))
            assertThat(grunnlag.gjeldendeVurdering()).isEqualTo(sisteVurdering)
        }

        @Test
        fun `prioriterer manuell vurdering over automatisk i siste behandling`() {
            val automatisk = automatiskVurdering(
                sluttdato = LocalDate.of(2025, 6, 1),
                behandlingId = behandling1,
                opprettet = Instant.parse("2024-06-01T12:00:00Z"),
            )
            val manuell = manuellVurdering(
                sluttdato = LocalDate.of(2025, 3, 1),
                behandlingId = behandling1,
                opprettet = Instant.parse("2024-05-01T12:00:00Z"),
            )
            val grunnlag = VedtakslengdeGrunnlag(listOf(automatisk, manuell))
            assertThat(grunnlag.gjeldendeVurdering()).isEqualTo(manuell)
        }

        @Test
        fun `prioriterer manuell selv om automatisk er nyere i siste behandling`() {
            val automatisk = automatiskVurdering(
                sluttdato = LocalDate.of(2025, 6, 1),
                behandlingId = behandling2,
                opprettet = Instant.parse("2025-02-01T12:00:00Z"),
            )
            val manuell = manuellVurdering(
                sluttdato = LocalDate.of(2025, 3, 1),
                behandlingId = behandling2,
                opprettet = Instant.parse("2025-01-01T12:00:00Z"),
            )
            val gammelAutomatisk = automatiskVurdering(
                sluttdato = LocalDate.of(2024, 12, 1),
                behandlingId = behandling1,
                opprettet = Instant.parse("2024-01-01T12:00:00Z"),
            )
            val grunnlag = VedtakslengdeGrunnlag(listOf(gammelAutomatisk, manuell, automatisk))

            // Siste behandling er behandling2 (har nyeste opprettet)
            // I behandling2 finnes både manuell og automatisk — manuell prioriteres
            assertThat(grunnlag.gjeldendeVurdering()).isEqualTo(manuell)
        }

        @Test
        fun `velger automatisk i siste behandling hvis ingen manuell finnes der`() {
            val manuellIForrige = manuellVurdering(
                sluttdato = LocalDate.of(2025, 3, 1),
                behandlingId = behandling1,
                opprettet = Instant.parse("2024-01-01T12:00:00Z"),
            )
            val automatiskINy = automatiskVurdering(
                sluttdato = LocalDate.of(2026, 1, 1),
                behandlingId = behandling2,
                opprettet = Instant.parse("2025-01-01T12:00:00Z"),
            )
            val grunnlag = VedtakslengdeGrunnlag(listOf(manuellIForrige, automatiskINy))

            // Siste behandling er behandling2 — kun automatisk der
            assertThat(grunnlag.gjeldendeVurdering()).isEqualTo(automatiskINy)
        }
    }

    @Nested
    inner class GjeldendeVurderingerTest {

        private val fraDato = LocalDate.of(2024, 1, 1)

        @Test
        fun `returnerer tom tidslinje når det ikke finnes vurderinger`() {
            val grunnlag = VedtakslengdeGrunnlag(emptyList())
            assertThat(grunnlag.gjeldendeVurderinger(fraDato).segmenter()).isEmpty()
        }

        @Test
        fun `returnerer ett segment med fraDato som fom og sluttdato som tom`() {
            val sluttdato = LocalDate.of(2025, 1, 1)
            val vurdering = automatiskVurdering(sluttdato = sluttdato)
            val grunnlag = VedtakslengdeGrunnlag(listOf(vurdering))

            val segmenter = grunnlag.gjeldendeVurderinger(fraDato).segmenter().toList()
            assertThat(segmenter).hasSize(1)
            assertThat(segmenter[0].fom()).isEqualTo(fraDato)
            assertThat(segmenter[0].tom()).isEqualTo(sluttdato)
            assertThat(segmenter[0].verdi).isEqualTo(vurdering)
        }

        @Test
        fun `kjeder fom-datoer fra forrige sluttdato pluss en dag`() {
            val sluttdato1 = LocalDate.of(2025, 1, 1)
            val sluttdato2 = LocalDate.of(2026, 1, 1)

            val vurdering1 = automatiskVurdering(
                sluttdato = sluttdato1,
                behandlingId = behandling1,
                opprettet = Instant.parse("2024-01-01T12:00:00Z"),
            )
            val vurdering2 = automatiskVurdering(
                sluttdato = sluttdato2,
                behandlingId = behandling2,
                opprettet = Instant.parse("2025-01-01T12:00:00Z"),
            )
            val grunnlag = VedtakslengdeGrunnlag(listOf(vurdering1, vurdering2))

            val segmenter = grunnlag.gjeldendeVurderinger(fraDato).segmenter().toList()
            assertThat(segmenter).hasSize(2)

            assertThat(segmenter[0].fom()).isEqualTo(fraDato)
            assertThat(segmenter[0].tom()).isEqualTo(sluttdato1)

            assertThat(segmenter[1].fom()).isEqualTo(sluttdato1.plusDays(1))
            assertThat(segmenter[1].tom()).isEqualTo(sluttdato2)
        }

        @Test
        fun `tar kun siste vurdering per behandling`() {
            val førsteVurdering = automatiskVurdering(
                sluttdato = LocalDate.of(2025, 1, 1),
                behandlingId = behandling1,
                opprettet = Instant.parse("2024-01-01T12:00:00Z"),
            )
            val nesteVurdering = automatiskVurdering(
                sluttdato = LocalDate.of(2025, 6, 1),
                behandlingId = behandling1,
                opprettet = Instant.parse("2024-06-01T12:00:00Z"),
            )
            val grunnlag = VedtakslengdeGrunnlag(listOf(førsteVurdering, nesteVurdering))

            val segmenter = grunnlag.gjeldendeVurderinger(fraDato).segmenter().toList()
            assertThat(segmenter).hasSize(1)
            assertThat(segmenter[0].verdi).isEqualTo(nesteVurdering)
        }

        @Test
        fun `prioriterer manuell vurdering over automatisk i samme behandling`() {
            val automatisk = automatiskVurdering(
                sluttdato = LocalDate.of(2025, 6, 1),
                behandlingId = behandling1,
                opprettet = Instant.parse("2024-06-01T12:00:00Z"),
            )
            val manuell = manuellVurdering(
                sluttdato = LocalDate.of(2025, 3, 1),
                behandlingId = behandling1,
                opprettet = Instant.parse("2024-05-01T12:00:00Z"),
            )
            val grunnlag = VedtakslengdeGrunnlag(listOf(automatisk, manuell))

            val segmenter = grunnlag.gjeldendeVurderinger(fraDato).segmenter().toList()
            assertThat(segmenter).hasSize(1)
            assertThat(segmenter[0].verdi).isEqualTo(manuell)
            assertThat(segmenter[0].tom()).isEqualTo(manuell.sluttdato)
        }

        @Test
        fun `tre behandlinger med kjedet tidslinje`() {
            val behandling3 = BehandlingId(3)
            val sluttdato1 = LocalDate.of(2025, 1, 1)
            val sluttdato2 = LocalDate.of(2026, 1, 1)
            val sluttdato3 = LocalDate.of(2027, 1, 1)

            val vurdering1 = automatiskVurdering(
                sluttdato = sluttdato1,
                behandlingId = behandling1,
                opprettet = Instant.parse("2024-01-01T12:00:00Z"),
            )
            val vurdering2 = automatiskVurdering(
                sluttdato = sluttdato2,
                behandlingId = behandling2,
                opprettet = Instant.parse("2025-01-01T12:00:00Z"),
            )
            val vurdering3 = automatiskVurdering(
                sluttdato = sluttdato3,
                behandlingId = behandling3,
                opprettet = Instant.parse("2026-01-01T12:00:00Z"),
            )
            val grunnlag = VedtakslengdeGrunnlag(listOf(vurdering1, vurdering2, vurdering3))

            val segmenter = grunnlag.gjeldendeVurderinger(fraDato).segmenter().toList()
            assertThat(segmenter).hasSize(3)

            assertThat(segmenter[0].fom()).isEqualTo(fraDato)
            assertThat(segmenter[0].tom()).isEqualTo(sluttdato1)

            assertThat(segmenter[1].fom()).isEqualTo(sluttdato1.plusDays(1))
            assertThat(segmenter[1].tom()).isEqualTo(sluttdato2)

            assertThat(segmenter[2].fom()).isEqualTo(sluttdato2.plusDays(1))
            assertThat(segmenter[2].tom()).isEqualTo(sluttdato3)
        }

        @Test
        fun `prioriterer manuell i en behandling mens annen behandling har kun automatisk`() {
            val automatiskBehandling1 = automatiskVurdering(
                sluttdato = LocalDate.of(2025, 6, 1),
                behandlingId = behandling1,
                opprettet = Instant.parse("2024-06-01T12:00:00Z"),
            )
            val manuellBehandling1 = manuellVurdering(
                sluttdato = LocalDate.of(2025, 3, 1),
                behandlingId = behandling1,
                opprettet = Instant.parse("2024-05-01T12:00:00Z"),
            )
            val automatiskBehandling2 = automatiskVurdering(
                sluttdato = LocalDate.of(2026, 1, 1),
                behandlingId = behandling2,
                opprettet = Instant.parse("2025-01-01T12:00:00Z"),
            )
            val grunnlag = VedtakslengdeGrunnlag(listOf(automatiskBehandling1, manuellBehandling1, automatiskBehandling2))

            val segmenter = grunnlag.gjeldendeVurderinger(fraDato).segmenter().toList()
            assertThat(segmenter).hasSize(2)

            // Behandling1: manuell prioriteres
            assertThat(segmenter[0].verdi).isEqualTo(manuellBehandling1)
            assertThat(segmenter[0].fom()).isEqualTo(fraDato)
            assertThat(segmenter[0].tom()).isEqualTo(manuellBehandling1.sluttdato)

            // Behandling2: kun automatisk
            assertThat(segmenter[1].verdi).isEqualTo(automatiskBehandling2)
            assertThat(segmenter[1].fom()).isEqualTo(manuellBehandling1.sluttdato.plusDays(1))
            assertThat(segmenter[1].tom()).isEqualTo(automatiskBehandling2.sluttdato)
        }
    }
}
