package no.nav.aap.behandlingsflyt.behandling.beregning.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.UføreInntekt
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.verdityper.GUnit
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
                    val gjennomsnittligInntektIG = underliggende.underliggende().gjennomsnittligInntektIG()
                    val uføre = uføreGrunnlagDTO(underliggende)
                    val yrkesskade =
                        yrkesskadeGrunnlagDTO(inntekter, beregning, underliggende, gjennomsnittligInntektIG)
                    BeregningDTO(
                        beregningstypeDTO = BeregningstypeDTO.YRKESSKADE_UFØRE,
                        grunnlagYrkesskadeUføre = YrkesskadeUføreGrunnlagDTO(
                            uføreGrunnlag = uføre,
                            yrkesskadeGrunnlag = yrkesskade,
                            grunnlag = beregning.grunnlaget().verdi()
                        )
                    )
                }

                is Grunnlag11_19 -> {
                    val inntekter = inntekterTilDTO(underliggende.inntekter())
                    val gjennomsnittligInntektIG = underliggende.gjennomsnittligInntektIG()
                    BeregningDTO(
                        beregningstypeDTO = BeregningstypeDTO.YRKESSKADE,
                        grunnlagYrkesskade = yrkesskadeGrunnlagDTO(
                            inntekter,
                            beregning,
                            underliggende,
                            gjennomsnittligInntektIG
                        )
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
        gjennomsnittligInntektSiste3år = grunnlag.gjennomsnittligInntektIG().verdi(),
        inntektSisteÅr = inntekter.maxBy(InntektDTO::år),
        grunnlag = grunnlag.grunnlaget().verdi()
    )
}

private fun inntekterTilDTO(inntekter: List<GrunnlagInntekt>): List<InntektDTO> {
    return inntekter.map(::inntekterTilDTO)
}

private fun inntekterTilDTO(inntekt: GrunnlagInntekt): InntektDTO {
    return InntektDTO(
        år = inntekt.år.value.toString(),
        inntektIKroner = inntekt.inntektIKroner.verdi(),
        inntektIG = inntekt.inntektIG.verdi(),
        justertTilMaks6G = inntekt.inntekt6GBegrenset.verdi()
    )
}

private fun inntekterTilUføreDTO(inntekter: List<Pair<UføreInntekt, GrunnlagInntekt>>): List<UføreInntektDTO> {
    return inntekter.map { (uføreInntekt, grunnlagInntekt) -> inntekterTilUføreDTO(uføreInntekt, grunnlagInntekt) }
}

private fun inntekterTilUføreDTO(uføreInntekt: UføreInntekt, grunnlagInntekt: GrunnlagInntekt): UføreInntektDTO {
    return UføreInntektDTO(
        år = uføreInntekt.år.value.toString(),
        inntektIKroner = uføreInntekt.inntektIKroner.verdi(),
        inntektIG = grunnlagInntekt.inntektIG.verdi(),
        justertTilMaks6G = grunnlagInntekt.inntekt6GBegrenset.verdi(),
        justertForUføreGrad = grunnlagInntekt.inntektIKroner.verdi(),
        uføreGrad = uføreInntekt.uføregrad.prosentverdi()
    )
}

private fun uføreGrunnlagDTO(grunnlag: GrunnlagUføre): UføreGrunnlagDTO {
    val inntekter = inntekterTilDTO(grunnlag.underliggende().inntekter())
    val inntekterForUføre = grunnlag.uføreInntekterFraForegåendeÅr().map { uføreInntekt ->
        Pair(
            uføreInntekt,
            grunnlag.underliggendeYtterligereNedsatt().inntekter()
                .single { grunnlagInntekt -> grunnlagInntekt.år == uføreInntekt.år }
        )
    }
    val uføreInntekter = inntekterTilUføreDTO(inntekterForUføre)
    return UføreGrunnlagDTO(
        inntekter = inntekter,
        gjennomsnittligInntektSiste3år = grunnlag.underliggende().gjennomsnittligInntektIG().verdi(),
        inntektSisteÅr = inntekter.maxBy(InntektDTO::år),
        uføreInntekter = uføreInntekter,
        gjennomsnittligInntektSiste3årUfør =
        grunnlag.underliggendeYtterligereNedsatt().gjennomsnittligInntektIG().verdi(),
        inntektSisteÅrUfør = uføreInntekter.maxBy(UføreInntektDTO::år),
        grunnlag = grunnlag.grunnlaget().verdi()
    )
}

private fun yrkesskadeGrunnlagDTO(
    inntekter: List<InntektDTO>,
    beregning: GrunnlagYrkesskade,
    underliggende: Beregningsgrunnlag,
    gjennomsnittligInntektIG: GUnit
) = YrkesskadeGrunnlagDTO(
    inntekter = inntekter,
    yrkesskadeinntekt = YrkesskadeInntektDTO(
        prosentVekting = beregning.andelYrkesskade().prosentverdi(),
        antattÅrligInntektIKronerYrkesskadeTidspunktet = beregning.antattÅrligInntektYrkesskadeTidspunktet()
            .verdi(),
        antattÅrligInntektIGYrkesskadeTidspunktet = beregning.yrkesskadeinntektIG().verdi(),
        justertTilMaks6G = beregning.yrkesskadeinntektIG()
            .verdi() // TODO: Skal YS reduseres til maks 6G?
    ),
    standardBeregning = StandardBeregningDTO(
        prosentVekting = beregning.andelYrkesskade().komplement().prosentverdi(),
        inntektIG = underliggende.grunnlaget().verdi(),
        justertTilMaks6G = underliggende.grunnlaget().verdi()
    ),
    gjennomsnittligInntektSiste3år = gjennomsnittligInntektIG.verdi(),
    inntektSisteÅr = inntekter.maxBy(InntektDTO::år),
    yrkesskadeGrunnlag = beregning.grunnlaget().verdi(),
    grunnlag = beregning.grunnlaget().verdi()
)
