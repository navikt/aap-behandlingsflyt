package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.november
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

class OvergangUføreGrunnlagTest {

    @Test
    fun `kravdatoUføretrygd`() {
        val gittKravdato = 30 november 2025
        val grunnlag = OvergangUføreGrunnlag(
            vurderinger = listOf(
                OvergangUføreVurdering(
                    begrunnelse = "test",
                    brukerHarSøktOmUføretrygd = true,
                    brukerHarFåttVedtakOmUføretrygd = UføreSøknadVedtakResultat.JA_INNVILGET_GRADERT,
                    brukerRettPåAAP = true,
                    fom = gittKravdato,
                    tom = 31 desember 2025,
                    vurdertAv = "meg",
                    vurdertIBehandling = BehandlingId(Random.nextLong()),
                    opprettet = LocalDateTime.now().atZone(ZoneId.of("Europe/Oslo")).toInstant(),
                )
            )
        )

        val kravdato = grunnlag.kravdatoUføretrygd()

        assertThat(kravdato).isEqualTo(gittKravdato)
    }

}