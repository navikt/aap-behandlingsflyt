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

internal fun beregningDTO(beregning: Beregningsgrunnlag) =
    when (beregning) {
        is GrunnlagYrkesskade -> {
            BeregningDTO(
                grunnlag = beregning.grunnlaget(),
                faktagrunnlag = beregning.faktagrunnlag(),
                grunnlag11_19 = Grunnlag11_19DTO(
                    grunnlaget = beregning.grunnlaget().verdi(),
                    er6GBegrenset = beregning.er6GBegrenset(),
                    erGjennomsnitt = beregning.erGjennomsnitt(),
                    inntekter = (beregningsgrunnlag(beregning).inntekter()).associate { it.år.value.toString() to it.beløp.verdi() }
                ),
                grunnlagYrkesskade = GrunnlagYrkesskadeDTO(
                    grunnlaget = beregning.grunnlaget().verdi(),
                    beregningsgrunnlag = grunnlag11_19_to_DTO(beregningsgrunnlag(beregning)),
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

        is GrunnlagUføre -> {
            BeregningDTO(
                grunnlag = beregning.grunnlaget(),
                faktagrunnlag = beregning.faktagrunnlag(),
                grunnlag11_19 = grunnlag11_19_to_DTO(beregning.underliggende()),
                grunnlagUføre = GrunnlagUføreDTO(
                    grunnlaget = beregning.grunnlaget().verdi(),
                    type = beregning.type().name,
                    grunnlag = grunnlag11_19_to_DTO(beregning.underliggende()),
                    grunnlagYtterligereNedsatt = grunnlag11_19_to_DTO(beregning.underliggendeYtterligereNedsatt()),
                    uføregrad = beregning.uføregrad().prosentverdi(),
                    uføreInntekterFraForegåendeÅr = beregning.underliggendeYtterligereNedsatt()
                        .inntekter().map { it.år.value.toString() to it.beløp.verdi() }.toMap(),
                    uføreInntektIKroner = beregning.uføreInntektIKroner().verdi(),
                    uføreYtterligereNedsattArbeidsevneÅr = beregning.uføreYtterligereNedsattArbeidsevneÅr().value,
                    er6GBegrenset = beregning.er6GBegrenset(),
                    erGjennomsnitt = beregning.erGjennomsnitt()
                )
            )
        }

        is Grunnlag11_19 -> {
            BeregningDTO(
                grunnlag = beregning.grunnlaget(),
                faktagrunnlag = beregning.faktagrunnlag(),
                grunnlag11_19 = grunnlag11_19_to_DTO(beregning),
            )
        }
    }

private fun beregningsgrunnlag(beregning: GrunnlagYrkesskade): Grunnlag11_19 {
    return when (val underliggende = beregning.underliggende()) {
        is Grunnlag11_19 -> underliggende
        is GrunnlagUføre -> underliggende.underliggende()
        is GrunnlagYrkesskade -> throw IllegalStateException("GrunnlagYrkesskade kan ikke ha grunnlag som også er GrunnlagYrkesskade")
    }
}

fun grunnlag11_19_to_DTO(grunnlag: Grunnlag11_19): Grunnlag11_19DTO {
    return Grunnlag11_19DTO(
        grunnlaget = grunnlag.grunnlaget().verdi(),
        er6GBegrenset = grunnlag.er6GBegrenset(),
        erGjennomsnitt = grunnlag.erGjennomsnitt(),
        inntekter = grunnlag.inntekter().map { it.år.value.toString() to it.beløp.verdi() }.toMap()
    )
}