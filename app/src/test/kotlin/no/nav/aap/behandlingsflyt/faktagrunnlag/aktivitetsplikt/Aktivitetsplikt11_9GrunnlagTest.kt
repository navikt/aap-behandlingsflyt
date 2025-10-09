package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import org.junit.jupiter.api.Assertions.*
import java.time.Instant
import kotlin.test.Test

class Aktivitetsplikt11_9GrunnlagTest {

    @Test
    fun `Gjeldende vurderinger er nyeste vurdering gitt opprettettidspunkt for en gitt dato`() {
        val grunnlag = Aktivitetsplikt11_9Grunnlag(
            vurderinger = setOf(
                Aktivitetsplikt11_9Vurdering(
                    dato = 1 januar 2023,
                    begrunnelse = "Begrunnelse 2",
                    grunn = Grunn.RIMELIG_GRUNN,
                    brudd = Brudd.IKKE_MØTT_TIL_MØTE,
                    vurdertAv = "ident",
                    opprettet = Instant.parse("2023-01-01T13:00:00Z"),
                    vurdertIBehandling = BehandlingId(1),
                ),
                Aktivitetsplikt11_9Vurdering(
                    dato = 1 januar 2023,
                    begrunnelse = "Begrunnelse 1",
                    grunn = Grunn.IKKE_RIMELIG_GRUNN,
                    brudd = Brudd.IKKE_MØTT_TIL_MØTE,
                    vurdertAv = "ident",
                    opprettet = Instant.parse("2023-01-01T12:00:00Z"),
                    vurdertIBehandling = BehandlingId(2),
                ),
                Aktivitetsplikt11_9Vurdering(
                    dato = 2 januar 2023,
                    begrunnelse = "Begrunnelse 3",
                    grunn = Grunn.IKKE_RIMELIG_GRUNN,
                    brudd = Brudd.IKKE_MØTT_TIL_MØTE,
                    vurdertAv = "ident",
                    opprettet = Instant.parse("2023-01-02T12:00:00Z"),
                    vurdertIBehandling = BehandlingId(1),
                ),
                Aktivitetsplikt11_9Vurdering(
                    dato = 2 januar 2023,
                    begrunnelse = "Begrunnelse 4",
                    grunn = Grunn.IKKE_RIMELIG_GRUNN,
                    brudd = Brudd.IKKE_MØTT_TIL_TILTAK,
                    vurdertAv = "ident",
                    opprettet = Instant.parse("2023-01-02T13:00:00Z"),
                    vurdertIBehandling = BehandlingId(2),
                )
            )
        )

        val gjeldende = grunnlag.gjeldendeVurderinger()
        assertEquals(2, gjeldende.size)
        assertTrue(gjeldende.any { it.begrunnelse == "Begrunnelse 2" })
        assertTrue(gjeldende.any { it.begrunnelse == "Begrunnelse 4" })
    }

}