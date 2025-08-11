package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvarDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingerForBarnetillegg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarnDto
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
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
                                LocalDate.now().minusMonths(1),
                                false,
                                "neida"
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

    @Test
    fun `deserialisere json-representasjon av VurdertBarn`() {
        @Language("JSON")
        val barnMedIdentJson = """
{
  "ident": [
    "BarnIdent",
    "123"
  ],
  "vurderinger": []
}  
        """.trimIndent()

        assertThat(DefaultJsonMapper.fromJson<VurdertBarn>(barnMedIdentJson)).isEqualTo(VurdertBarn(
            ident = BarnIdentifikator.BarnIdent("123"),
            vurderinger = listOf()
        ))

        println(DefaultJsonMapper.toJson(VurdertBarn(
            ident = BarnIdentifikator.NavnOgFødselsdato("Jojo Jobbi", Fødselsdato(LocalDate.now())),
            vurderinger = listOf()
        )))

        @Language("JSON")
        val barnMedNavnOgFødselsdato = """
{
  "ident" : {
    "type" : "NavnOgFødselsdato",
    "navn" : "JoJo Jobbi",
    "fødselsdato" : "2025-08-11"
  },
  "vurderinger" : [ ]
}            
        """.trimIndent()

        assertThat(DefaultJsonMapper.fromJson<VurdertBarn>(barnMedNavnOgFødselsdato)).isEqualTo(VurdertBarn(
            ident = BarnIdentifikator.NavnOgFødselsdato("JoJo Jobbi", Fødselsdato(LocalDate.of(2025, 8, 11))),
            vurderinger = listOf()
        ))
    }

}