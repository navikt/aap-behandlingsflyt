package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.beregning.år.InntektsBehov
import no.nav.aap.behandlingsflyt.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.GUnit
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.adapter.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomRepository
import java.time.Year

class BeregningService(
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val sykdomRepository: SykdomRepository
) {

    fun beregnGrunnlag(behandlingId: BehandlingId): GUnit {
        val inntektGrunnlag = inntektGrunnlagRepository.hent(behandlingId)
        val sykdomGrunnlag = sykdomRepository.hent(behandlingId)

        val inntekter = utledInput(sykdomGrunnlag)

        val grunnlag11_19 =
            GrunnlagetForBeregningen(inntekter.utledForOrdinær(inntektGrunnlag.inntekter)).beregnGrunnlaget()

        val skadetidspunkt = sykdomGrunnlag.yrkesskadevurdering?.skadetidspunkt
        val antattÅrligInntekt = sykdomGrunnlag.yrkesskadevurdering?.antattÅrligInntekt
        val andelAvNedsettelsenSomSkyldesYrkesskaden = sykdomGrunnlag.yrkesskadevurdering?.andelAvNedsettelse
        if (skadetidspunkt != null && antattÅrligInntekt != null && andelAvNedsettelsenSomSkyldesYrkesskaden != null) {
            val inntektPerÅr = InntektPerÅr(Year.from(skadetidspunkt), antattÅrligInntekt)
            val yrkesskaden = YrkesskadeBeregning(
                grunnlag11_19 = grunnlag11_19,
                antattÅrligInntekt = inntektPerÅr,
                andelAvNedsettelsenSomSkyldesYrkesskaden = andelAvNedsettelsenSomSkyldesYrkesskaden
            ).beregnYrkesskaden()
            return yrkesskaden.grunnlaget()
        }

        return grunnlag11_19.grunnlaget()
    }

    private fun utledInput(sykdomGrunnlag: SykdomGrunnlag): InntektsBehov {
        return InntektsBehov(Input(sykdomGrunnlag.sykdomsvurdering?.nedsattArbeidsevneDato!!))
    }
}