package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvarDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingerForBarnetillegg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarnDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

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
                )
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