package no.nav.aap.behandlingsflyt.behandling.beregning

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.UføreInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

private val årFormatter = DateTimeFormatter.ofPattern("yyyy")

fun NormalOpenAPIRoute.beregningsGrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/beregning") {
        route("/grunnlag/{referanse}").tag(Tags.Grunnlag) {
            authorizedGet<BehandlingReferanse, BeregningDTO>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam(
                        "referanse"
                    )
                )
            ) { req ->
                val begregningsgrunnlag = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val beregningsgrunnlagRepository =
                        repositoryProvider.provide<BeregningsgrunnlagRepository>()
                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    val beregning = beregningsgrunnlagRepository.hentHvisEksisterer(behandling.id)
                    if (beregning == null) {
                        return@transaction null
                    }
                    val behandlingOpprettet = behandling.opprettetTidspunkt.toLocalDate()
                    beregningDTO(beregning, behandlingOpprettet)
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

internal fun beregningDTO(beregning: Beregningsgrunnlag, behandlingOpprettet: LocalDate): BeregningDTO {
    val gjeldendeGrunnbeløp = hentGjeldendeGrunnbeløp(behandlingOpprettet)

    return when (beregning) {
        is GrunnlagYrkesskade -> {
            when (val underliggende = beregning.underliggende()) {
                is GrunnlagUføre -> {
                    val inntekter = underliggende.underliggende().inntekter()
                    val gjennomsnittligInntektIG = underliggende.underliggende().gjennomsnittligInntektIG()
                    val uføre = uføreGrunnlagDTO(underliggende)
                    val yrkesskade =
                        yrkesskadeGrunnlagDTO(inntekter, beregning, underliggende, gjennomsnittligInntektIG.verdi())
                    BeregningDTO(
                        beregningstypeDTO = BeregningstypeDTO.YRKESSKADE_UFØRE,
                        grunnlagYrkesskadeUføre = YrkesskadeUføreGrunnlagDTO(
                            uføreGrunnlag = uføre,
                            yrkesskadeGrunnlag = yrkesskade,
                            grunnlag = beregning.grunnlaget().verdi()
                        ),
                        gjeldendeGrunnbeløp = gjeldendeGrunnbeløp
                    )
                }

                is Grunnlag11_19 -> {
                    val inntekter = underliggende.inntekter()
                    val gjennomsnittligInntektIG = underliggende.gjennomsnittligInntektIG()
                    BeregningDTO(
                        beregningstypeDTO = BeregningstypeDTO.YRKESSKADE,
                        grunnlagYrkesskade = yrkesskadeGrunnlagDTO(
                            inntekter,
                            beregning,
                            underliggende,
                            gjennomsnittligInntektIG.verdi()
                        ),
                        gjeldendeGrunnbeløp = gjeldendeGrunnbeløp
                    )

                }

                is GrunnlagYrkesskade -> throw IllegalStateException("GrunnlagYrkesskade kan ikke ha grunnlag som også er GrunnlagYrkesskade")
            }
        }

        is GrunnlagUføre -> {
            BeregningDTO(
                beregningstypeDTO = BeregningstypeDTO.UFØRE,
                grunnlagUføre = uføreGrunnlagDTO(beregning),
                gjeldendeGrunnbeløp = gjeldendeGrunnbeløp
            )
        }

        is Grunnlag11_19 -> {
            BeregningDTO(
                beregningstypeDTO = BeregningstypeDTO.STANDARD,
                grunnlag11_19 = grunnlag11_19_to_DTO(beregning),
                gjeldendeGrunnbeløp = gjeldendeGrunnbeløp
            )
        }
    }
}

private fun hentGjeldendeGrunnbeløp(behandlingOpprettet: LocalDate): GjeldendeGrunnbeløpDTO {
    return GjeldendeGrunnbeløpDTO(
        grunnbeløp = Grunnbeløp.finnGrunnbeløp(behandlingOpprettet).verdi,
        dato = behandlingOpprettet
    )
}

private fun grunnlag11_19_to_DTO(grunnlag: Grunnlag11_19): Grunnlag11_19DTO {
    val inntekter = inntekterTilDTO(grunnlag.inntekter())
    return Grunnlag11_19DTO(
        inntekter = inntekter,
        gjennomsnittligInntektSiste3år = grunnlag.gjennomsnittligInntektIG().verdi(),
        inntektSisteÅr = inntekter.maxBy(InntektDTO::år),
        grunnlag = grunnlag.grunnlaget().verdi(),
        nedsattArbeidsevneÅr =
            grunnlag.inntekter().maxOf { inntekt -> inntekt.år }.plusYears(1).format(årFormatter)
    )
}

private fun inntekterTilDTO(inntekter: List<GrunnlagInntekt>): List<InntektDTO> {
    return inntekter.map(::inntekterTilDTO)
}

private fun inntekterTilDTO(inntekt: GrunnlagInntekt): InntektDTO {
    return InntektDTO(
        år = inntekt.år.format(årFormatter),
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
        år = uføreInntekt.år.format(årFormatter),
        inntektIKroner = uføreInntekt.inntektIKroner.verdi(),
        inntektIG = uføreInntekt.inntektIG.verdi(),
        justertTilMaks6G = grunnlagInntekt.inntekt6GBegrenset.verdi(),
        justertForUføreGrad = grunnlagInntekt.inntektIKroner.verdi(),
        justertForUføreGradiG = grunnlagInntekt.inntektIG.verdi(),
        uføreGrad = uføreInntekt.uføregrad.prosentverdi(),
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
        grunnlag = grunnlag.grunnlaget().verdi(),
        nedsattArbeidsevneÅr =
            grunnlag.underliggende().inntekter().maxOf { inntekt -> inntekt.år }.plusYears(1).format(årFormatter),
        ytterligereNedsattArbeidsevneÅr = grunnlag.uføreYtterligereNedsattArbeidsevneÅr().format(årFormatter)
    )
}

private fun yrkesskadeGrunnlagDTO(
    inntekter: List<GrunnlagInntekt>,
    beregning: GrunnlagYrkesskade,
    underliggende: Beregningsgrunnlag,
    gjennomsnittligInntektIG: BigDecimal
): YrkesskadeGrunnlagDTO {
    val inntekterDTO = inntekterTilDTO(inntekter)
    return YrkesskadeGrunnlagDTO(
        inntekter = inntekterDTO,
        yrkesskadeinntekt = YrkesskadeInntektDTO(
            prosentVekting = beregning.benyttetAndelForYrkesskade().prosentverdi(),
            antattÅrligInntektIKronerYrkesskadeTidspunktet = beregning.antattÅrligInntektYrkesskadeTidspunktet()
                .verdi(),
            antattÅrligInntektIGYrkesskadeTidspunktet = beregning.yrkesskadeinntektIG().verdi(),
            justertTilMaks6G = beregning.yrkesskadeinntektIG()
                .verdi(),
            andelGangerInntekt = beregning.antattÅrligInntektYrkesskadeTidspunktet()
                .multiplisert(beregning.benyttetAndelForYrkesskade()).verdi(),
            andelGangerInntektIG = beregning.yrkesskadeinntektIG().multiplisert(beregning.benyttetAndelForYrkesskade()).verdi()
        ),
        standardBeregning = StandardBeregningDTO(
            prosentVekting = beregning.benyttetAndelForYrkesskade().komplement().prosentverdi(),
            inntektIG = underliggende.grunnlaget().verdi(),
            andelGangerInntekt = underliggende.grunnlaget().multiplisert(beregning.benyttetAndelForYrkesskade().komplement())
                .verdi(),
            andelGangerInntektIG = underliggende.grunnlaget().multiplisert(beregning.benyttetAndelForYrkesskade().komplement())
                .verdi(),
        ),
        standardYrkesskade = StandardYrkesskadeDTO(
            prosentVekting = beregning.benyttetAndelForYrkesskade().prosentverdi(),
            inntektIG = underliggende.grunnlaget().verdi(),
            andelGangerInntekt = underliggende.grunnlaget().multiplisert(beregning.benyttetAndelForYrkesskade()).verdi(),
            andelGangerInntektIG = underliggende.grunnlaget().multiplisert(beregning.benyttetAndelForYrkesskade()).verdi(),
        ),
        gjennomsnittligInntektSiste3år = gjennomsnittligInntektIG,
        inntektSisteÅr = inntekterDTO.maxBy(InntektDTO::år),
        nedsattArbeidsevneÅr =
            inntekter.maxOf { inntekt -> inntekt.år }.plusYears(1).format(årFormatter),
        yrkesskadeTidspunkt = beregning.yrkesskadeTidspunkt().format(årFormatter),
        yrkesskadeGrunnlag = beregning.grunnlaget().verdi(),
        grunnlag = beregning.grunnlaget().verdi()
    )
}
