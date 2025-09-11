package no.nav.aap.behandlingsflyt.integrasjon.datadeling

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.datadeling.SakStatusDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.hendelse.datadeling.MeldekortPerioderDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.RettighetsTypePeriode
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.SakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.UnderveisDTO
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate

class ApiInternGatewayImpl() : ApiInternGateway {

    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)

    companion object : Factory<ApiInternGateway> {
        override fun konstruer(): ApiInternGateway {
            return ApiInternGatewayImpl()
        }
    }

    private val restClient = RestClient.withDefaultResponseHandler(
        config = ClientConfig(scope = requiredConfigForKey("integrasjon.datadeling.scope")),
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    private val uri = URI.create(requiredConfigForKey("integrasjon.datadeling.url"))

    override fun sendPerioder(ident: String, perioder: List<Periode>) {
        restClient.post(
            uri = uri.resolve("/api/insert/meldeperioder"),
            request = PostRequest(body = MeldekortPerioderDTO(ident, perioder)),
            mapper = { _, _ ->
            })
    }

    override fun sendSakStatus(ident: String, sakStatus: SakStatus) {
        restClient.post(
            uri = uri.resolve("/api/insert/sakStatus"),
            request = PostRequest(body = SakStatusDTO(ident, sakStatus)),
            mapper = { _, _ ->
            })
    }

    override fun sendBehandling(
        sak: Sak,
        behandling: Behandling,
        vedtakId: Long,
        samId: String?,
        tilkjent: List<TilkjentYtelsePeriode>,
        beregningsgrunnlag: BigDecimal,
        underveis: List<Underveisperiode>,
        vedtaksDato: LocalDate,
        rettighetsTypeTidslinje: Tidslinje<no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType>
    ) {
        log.info("Sender behandling for behandlingId=${behandling.id} med vedtakId=$vedtakId, sak: ${sak.saksnummer}. Beregningsgrunnlag: $beregningsgrunnlag")
        restClient.post(
            uri = uri.resolve("/api/insert/vedtak"),
            request = PostRequest(
                body = DatadelingDTO(
                    behandlingsId = behandling.id.id.toString(),
                    behandlingsReferanse = behandling.referanse.toString(),
                    underveisperiode = underveis.map {
                        UnderveisDTO(
                            underveisFom = it.periode.fom,
                            underveisTom = it.periode.tom,
                            meldeperiodeFom = it.meldePeriode.fom,
                            meldeperiodeTom = it.meldePeriode.tom,
                            utfall = it.utfall.name,
                            rettighetsType = it.rettighetsType?.name,
                            avslagsårsak = it.avslagsårsak?.name
                        )
                    },
                    rettighetsPeriodeFom = sak.rettighetsperiode.fom,
                    rettighetsPeriodeTom = sak.rettighetsperiode.tom,
                    behandlingStatus = behandling.status(),
                    vedtaksDato = vedtaksDato,
                    beregningsgrunnlag = beregningsgrunnlag,
                    sak = SakDTO(
                        saksnummer = sak.saksnummer.toString(),
                        status = sak.status(),
                        fnr = sak.person.identer().map { ident -> ident.identifikator },
                        opprettetTidspunkt = sak.opprettetTidspunkt
                    ),
                    tilkjent = tilkjent.map { tilkjentPeriode ->
                        no.nav.aap.behandlingsflyt.kontrakt.datadeling.TilkjentDTO(
                            tilkjentFom = tilkjentPeriode.periode.fom,
                            tilkjentTom = tilkjentPeriode.periode.tom,
                            dagsats = tilkjentPeriode.tilkjent.dagsats.verdi.toInt(),
                            gradering = tilkjentPeriode.tilkjent.gradering.endeligGradering.prosentverdi(),
                            samordningUføregradering = tilkjentPeriode.tilkjent.gradering.samordningUføregradering?.prosentverdi(),
                            // TODO: fjern
                            grunnlag = tilkjentPeriode.tilkjent.dagsats.verdi,
                            grunnlagsfaktor = tilkjentPeriode.tilkjent.grunnlagsfaktor.verdi(),
                            grunnbeløp = tilkjentPeriode.tilkjent.grunnbeløp.verdi,
                            antallBarn = tilkjentPeriode.tilkjent.antallBarn,
                            barnetilleggsats = tilkjentPeriode.tilkjent.barnetilleggsats.verdi,
                            barnetillegg = tilkjentPeriode.tilkjent.barnetillegg.verdi,
                        )
                    },
                    rettighetsTypeTidsLinje = rettighetsTypeTidslinje.map { segment ->
                        RettighetsTypePeriode(
                            segment.periode.fom,
                            segment.periode.tom,
                            segment.verdi.toString()
                        )
                    },
                    vedtakId = vedtakId,
                    samId = samId,
                ),
            ),
            mapper = { _, _ ->
            })
    }

    override fun sendDetaljertMeldekortListe(detaljertMeldekortListe: List<DetaljertMeldekortDTO>) {
        if (detaljertMeldekortListe.isEmpty()) {
            log.info("Ingen meldekort-detaljer å sende")
            return
        }

        if (log.isInfoEnabled) {
            val meldekort = detaljertMeldekortListe.first()
            val saksnummer = meldekort.saksnummer
            val fom = meldekort.meldeperiodeFom
            val tom = meldekort.meldeperiodeTom
            log.info("Sender meldekort-detaljer for sak=${saksnummer}, meldeperiode=${fom}-${tom}-")
        }

        restClient.post(
            uri = uri.resolve("/api/insert/meldekort-detaljer"),
            request = PostRequest(body = detaljertMeldekortListe),
            mapper = { _, _ ->
            })
    }
}
