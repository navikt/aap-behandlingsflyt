package no.nav.aap.behandlingsflyt.behandling.beregning.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.server.respondWithStatus
import no.nav.aap.verdityper.Prosent
import java.math.BigDecimal
import java.math.RoundingMode
import javax.sql.DataSource

fun NormalOpenAPIRoute.beregningsGrunnlagApi(dataSource: DataSource) {
    route("/api/beregning") {
        route("/grunnlag/{referanse}") {
            get<BehandlingReferanse, BeregningDTO> { req ->
                val begregningsgrunnlag = dataSource.transaction { connection ->
                    val behandling: Behandling =
                        BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(req)
                    val beregning = BeregningsgrunnlagRepository(connection).hentHvisEksisterer(behandling.id)
                    if (beregning == null) {
                        return@transaction null
                    }
                    beregningDTO(beregning)
                }

                if (begregningsgrunnlag == null) {
                    respondWithStatus(HttpStatusCode.NoContent)
                } else {
                    respond(begregningsgrunnlag)
                }
            }
        }
    }
}

internal fun beregningDTO(beregning: Beregningsgrunnlag): BeregningDTO {
    return when (beregning) {
        is GrunnlagYrkesskade -> {
            when (val underliggende = beregning.underliggende()) {
                is GrunnlagUføre -> {
                    val inntekter = inntekterTilDTO(underliggende.underliggende().inntekter())
                    val uføre = uføreGrunnlagDTO(underliggende)
                    val yrkesskade = yrkesskadeGrunnlagDTO(inntekter, beregning, underliggende)
                    BeregningDTO(
                        beregningstypeDTO = BeregningstypeDTO.YRKESSKADE_UFØRE,
                        grunnlagYrkesskadeUføre = YrkesskadeUføreGrunnlagDTO(
                            uføreGrunnlag = uføre,
                            yrkesskadeGrunnlag = yrkesskade
                        )
                    )
                }

                is Grunnlag11_19 -> {
                    val inntekter = inntekterTilDTO(underliggende.inntekter())
                    BeregningDTO(
                        beregningstypeDTO = BeregningstypeDTO.YRKESSKADE,
                        grunnlagYrkesskade = yrkesskadeGrunnlagDTO(inntekter, beregning, underliggende)
                    )

                }

                is GrunnlagYrkesskade -> throw IllegalStateException("GrunnlagYrkesskade kan ikke ha grunnlag som også er GrunnlagYrkesskade")
            }
        }

        is GrunnlagUføre -> {
            BeregningDTO(
                beregningstypeDTO = BeregningstypeDTO.UFØRE,
                grunnlagUføre = uføreGrunnlagDTO(beregning)
            )
        }

        is Grunnlag11_19 -> {
            BeregningDTO(
                beregningstypeDTO = BeregningstypeDTO.STANDARD,
                grunnlag11_19 = grunnlag11_19_to_DTO(beregning),
            )
        }
    }
}

private fun grunnlag11_19_to_DTO(grunnlag: Grunnlag11_19): Grunnlag11_19DTO {
    val inntekter = inntekterTilDTO(grunnlag.inntekter())
    return Grunnlag11_19DTO(
        inntekter = inntekter,
        gjennomsnittligInntektSiste3år = gjennomsnittInntekter(inntekter), //FIXME
        inntektSisteÅr = inntekter.maxBy(InntektDTO::år),
        grunnlag = grunnlag.grunnlaget().verdi()
    )
}

@Deprecated("Må hentes fra beregningen")
private fun gjennomsnittInntekter(inntekter: List<InntektDTO>): BigDecimal =
    inntekter.sumOf { it.inntektIG }.divide(BigDecimal(3), RoundingMode.HALF_UP)

private fun inntekterTilDTO(inntektPerÅr: List<InntektPerÅr>): List<InntektDTO> {
    return inntektPerÅr.map(::inntekterTilDTO)
}

private fun inntekterTilDTO(inntektPerÅr: InntektPerÅr): InntektDTO {
    //FIXME Må hente lagrede verdier
    return InntektDTO(
        år = inntektPerÅr.år.value.toString(),
        inntektIKroner = inntektPerÅr.beløp.verdi(),
        inntektIG = inntektPerÅr.gUnit().verdi(),
        justertTilMaks6G = inntektPerÅr.gUnit().begrensTil6GUnits().verdi()
    )
}

@Deprecated("Må hentes fra beregningen")
private fun gjennomsnittInntekterUføre(inntekter: List<UføreInntektDTO>): BigDecimal =
    inntekter.sumOf { it.inntektIG }.divide(BigDecimal(3), RoundingMode.HALF_UP)

private fun inntekterTilUføreDTO(inntektPerÅr: List<InntektPerÅr>, uføregrad: Prosent): List<UføreInntektDTO> {
    return inntektPerÅr.map { inntekterTilUføreDTO(it, uføregrad) }
}

private fun inntekterTilUføreDTO(inntektPerÅr: InntektPerÅr, uføregrad: Prosent): UføreInntektDTO {
    //FIXME Må hente lagrede verdier
    return UføreInntektDTO(
        år = inntektPerÅr.år.value.toString(),
        inntektIKroner = inntektPerÅr.beløp.verdi(),
        inntektIG = inntektPerÅr.gUnit().verdi(), //FIXME Finn inntekt før justering for uføregrad
        justertTilMaks6G = inntektPerÅr.gUnit().begrensTil6GUnits().verdi(),
        justertForUføreGrad = inntektPerÅr.gUnit().verdi(),
        uføreGrad = uføregrad.prosentverdi() //FIXME Uføregrad pr inntekt
    )
}

private fun uføreGrunnlagDTO(grunnlag: GrunnlagUføre): UføreGrunnlagDTO {
    val inntekter = inntekterTilDTO(grunnlag.underliggende().inntekter())
    val uføreInntekter =
        inntekterTilUføreDTO(grunnlag.underliggendeYtterligereNedsatt().inntekter(), grunnlag.uføregrad())
    return UføreGrunnlagDTO(
        inntekter = inntekter,
        gjennomsnittligInntektSiste3år = gjennomsnittInntekter(inntekter),
        inntektSisteÅr = inntekter.maxBy(InntektDTO::år),
        uføreInntekter = uføreInntekter,
        gjennomsnittligInntektSiste3årUfør = gjennomsnittInntekterUføre(uføreInntekter),
        inntektSisteÅrUfør = uføreInntekter.maxBy(UføreInntektDTO::år),
        grunnlag = grunnlag.grunnlaget().verdi()
    )
}

private fun yrkesskadeGrunnlagDTO(
    inntekter: List<InntektDTO>,
    beregning: GrunnlagYrkesskade,
    underliggende: Beregningsgrunnlag
) = YrkesskadeGrunnlagDTO(
    inntekter = inntekter,
    yrkesskadeinntekt = YrkesskadeInntektDTO(
        prosentVekting = beregning.andelYrkesskade().prosentverdi(),
        antattÅrligInntektIKronerYrkesskadeTidspunktet = beregning.antattÅrligInntektYrkesskadeTidspunktet()
            .verdi(),
        antattÅrligInntektIGYrkesskadeTidspunktet = beregning.yrkesskadeinntektIG().verdi(),
        justertTilMaks6G = beregning.yrkesskadeinntektIG()
            .verdi() //TODO: Skal YS reduseres til maks 6G
    ),
    standardBeregning = StandardBeregningDTO(
        prosentVekting = beregning.andelYrkesskade().komplement().prosentverdi(),
        inntektIG = underliggende.grunnlaget().verdi(),
        justertTilMaks6G = underliggende.grunnlaget().verdi()
    ),
    gjennomsnittligInntektSiste3år = gjennomsnittInntekter(inntekter),
    inntektSisteÅr = inntekter.maxBy(InntektDTO::år),
    yrkesskadeGrunnlag = beregning.grunnlaget().verdi(),
    grunnlag = beregning.grunnlaget().verdi()
)
