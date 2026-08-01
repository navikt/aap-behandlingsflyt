package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.barnetillegg.BarnIdentifikator
import no.nav.aap.barnetillegg.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.steg.barnetillegg.VurderingAvForeldreAnsvarDto
import no.nav.aap.behandlingsflyt.steg.barnetillegg.VurderingerForBarnetillegg
import no.nav.aap.barnetillegg.VurdertBarn
import no.nav.aap.behandlingsflyt.steg.barnetillegg.VurdertBarnDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.oppdaterTilstandBasertPåNyeVurderinger

class AvklarBarnetilleggLøserTest {

    @Test
    fun `skal slå sammen vurderinger ved nye`() {
        val barnIdent = BarnIdentifikator.BarnIdent("12341234")
        val eksisterendeVurderinger = listOf(
            VurdertBarn(
                barnIdent,
                listOf(VurderingAvForeldreAnsvar(LocalDate.now().minusMonths(2), true, "jada"))
            )
        )

        val nyeVurderinger = AvklarBarnetilleggLøsning(
            vurderingerForBarnetillegg = VurderingerForBarnetillegg(
                listOf(
                    VurdertBarnDto(
                        barnIdent.ident.identifikator,
                        fødselsdato = null,
                        dødsdato = null,
                        vurderinger = listOf(
                            VurderingAvForeldreAnsvarDto(
                               fraDato =  LocalDate.now().minusMonths(1),
                                harForeldreAnsvar = false,
                                begrunnelse = "neida",
                                erFosterForelder = null,
                            )
                        ),
                        navn = null,
                    )
                ),
                saksbehandlerOppgitteBarn = emptyList()
            )
        )

        val oppdaterteVurderinger = oppdaterTilstandBasertPåNyeVurderinger(
            eksisterendeVurderinger,
            nyeVurderinger.vurderingerForBarnetillegg.vurderteBarn
        )

        assertThat(oppdaterteVurderinger).hasSize(1)
        assertThat(oppdaterteVurderinger.single { it.ident.er(barnIdent) }.vurderinger).hasSize(2)
    }
}