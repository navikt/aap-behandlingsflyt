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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.server.respondWithStatus
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
                    BeregningDTO(
                        beregningstypeDTO = BeregningstypeDTO.YRKESSKADE_UFØRE,
                        grunnlag = beregning.grunnlaget().verdi(),
                        grunnlag11_19 = null,
                        grunnlagYrkesskadeUføre = GrunnlagYrkesskadeUføreDTO(
                            grunnlaget = beregning.grunnlaget().verdi(),
                            beregningsgrunnlag = grunnlagUføre_to_DTO(underliggende),
                            terskelverdiForYrkesskade = beregning.terskelverdiForYrkesskade().prosentverdi(),
                            andelSomSkyldesYrkesskade = beregning.andelSomSkyldesYrkesskade().verdi(),
                            andelYrkesskade = beregning.andelYrkesskade().prosentverdi(),
                            benyttetAndelForYrkesskade = beregning.benyttetAndelForYrkesskade().prosentverdi(),
                            andelSomIkkeSkyldesYrkesskade = beregning.andelSomIkkeSkyldesYrkesskade().verdi(),
                            antattÅrligInntektYrkesskadeTidspunktet = beregning.antattÅrligInntektYrkesskadeTidspunktet()
                                .verdi(),
                            yrkesskadeTidspunkt = beregning.yrkesskadeTidspunkt().value,
                            grunnlagForBeregningAvYrkesskadeandel = beregning.grunnlagForBeregningAvYrkesskadeandel()
                                .verdi(),
                            yrkesskadeinntektIG = beregning.yrkesskadeinntektIG().verdi(),
                            grunnlagEtterYrkesskadeFordel = beregning.grunnlagEtterYrkesskadeFordel().verdi(),
                            er6GBegrenset = beregning.er6GBegrenset(),
                            erGjennomsnitt = beregning.erGjennomsnitt(),
                        )
                    )
                }

                is Grunnlag11_19 -> {
                    BeregningDTO(
                        beregningstypeDTO = BeregningstypeDTO.YRKESSKADE,
                        grunnlag = beregning.grunnlaget().verdi(),
                        grunnlagYrkesskade = GrunnlagYrkesskadeDTO(
                            grunnlaget = beregning.grunnlaget().verdi(),
                            beregningsgrunnlag = grunnlag11_19_to_DTO(underliggende),
                            terskelverdiForYrkesskade = beregning.terskelverdiForYrkesskade().prosentverdi(),
                            andelSomSkyldesYrkesskade = beregning.andelSomSkyldesYrkesskade().verdi(),
                            andelYrkesskade = beregning.andelYrkesskade().prosentverdi(),
                            benyttetAndelForYrkesskade = beregning.benyttetAndelForYrkesskade().prosentverdi(),
                            andelSomIkkeSkyldesYrkesskade = beregning.andelSomIkkeSkyldesYrkesskade().verdi(),
                            antattÅrligInntektYrkesskadeTidspunktet = beregning.antattÅrligInntektYrkesskadeTidspunktet()
                                .verdi(),
                            yrkesskadeTidspunkt = beregning.yrkesskadeTidspunkt().value,
                            grunnlagForBeregningAvYrkesskadeandel = beregning.grunnlagForBeregningAvYrkesskadeandel()
                                .verdi(),
                            yrkesskadeinntektIG = beregning.yrkesskadeinntektIG().verdi(),
                            grunnlagEtterYrkesskadeFordel = beregning.grunnlagEtterYrkesskadeFordel().verdi(),
                            er6GBegrenset = beregning.er6GBegrenset(),
                            erGjennomsnitt = beregning.erGjennomsnitt(),
                        )
                    )

                }

                is GrunnlagYrkesskade -> throw IllegalStateException("GrunnlagYrkesskade kan ikke ha grunnlag som også er GrunnlagYrkesskade")
            }
        }

        is GrunnlagUføre -> {
            BeregningDTO(
                beregningstypeDTO = BeregningstypeDTO.UFØRE,
                grunnlag = beregning.grunnlaget().verdi(),
                grunnlagUføre = grunnlagUføre_to_DTO(beregning)
            )
        }

        is Grunnlag11_19 -> {
            BeregningDTO(
                beregningstypeDTO = BeregningstypeDTO.STANDARD,
                grunnlag = beregning.grunnlaget().verdi(),
                grunnlag11_19 = grunnlag11_19_to_DTO(beregning),
            )
        }
    }
}

private fun grunnlag11_19_to_DTO(grunnlag: Grunnlag11_19): Grunnlag11_19DTO {
    return Grunnlag11_19DTO(
        grunnlaget = grunnlag.grunnlaget().verdi(),
        er6GBegrenset = grunnlag.er6GBegrenset(),
        erGjennomsnitt = grunnlag.erGjennomsnitt(),
        inntekter = grunnlag.inntekter().map { it.år.value.toString() to it.beløp.verdi() }.toMap()
    )
}

private fun grunnlagUføre_to_DTO(grunnlag: GrunnlagUføre): GrunnlagUføreDTO {
    return GrunnlagUføreDTO(
        grunnlaget = grunnlag.grunnlaget().verdi(),
        type = grunnlag.type().name,
        grunnlag = grunnlag11_19_to_DTO(grunnlag.underliggende()),
        grunnlagYtterligereNedsatt = grunnlag11_19_to_DTO(grunnlag.underliggendeYtterligereNedsatt()),
        uføregrad = grunnlag.uføregrad().prosentverdi(),
        uføreInntekterFraForegåendeÅr = grunnlag.underliggendeYtterligereNedsatt()
            .inntekter().map { it.år.value.toString() to it.beløp.verdi() }.toMap(),
        uføreInntektIKroner = grunnlag.uføreInntektIKroner().verdi(),
        uføreYtterligereNedsattArbeidsevneÅr = grunnlag.uføreYtterligereNedsattArbeidsevneÅr().value,
        er6GBegrenset = grunnlag.er6GBegrenset(),
        erGjennomsnitt = grunnlag.erGjennomsnitt()
    )
}
