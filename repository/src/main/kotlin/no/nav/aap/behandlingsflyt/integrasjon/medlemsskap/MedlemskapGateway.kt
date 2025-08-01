package no.nav.aap.behandlingsflyt.integrasjon.medlemsskap

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemKode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemMedl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapDataIntern
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapGateway
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import java.net.URI

class MedlemskapGateway : MedlemskapGateway {
    private val url = requiredConfigForKey("integrasjon.medl.url")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.medl.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    companion object : Factory<MedlemskapGateway> {
        override fun konstruer(): MedlemskapGateway {
            return MedlemskapGateway()
        }
    }

    private fun query(request: MedlemskapRequest): List<MedlemskapResponse> {
        val urlWithParam = URI.create(url+"?fraOgMed=${request.periode.fom}&tilOgMed=${request.periode.tom}&inkluderSporingsinfo=true")

        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Nav-Personident", request.ident),
                Header("Accept", "application/json"),
            )
        )

        return requireNotNull(
            client.get(
                uri = urlWithParam,
                request = httpRequest,
                mapper = { body, _ ->
                    DefaultJsonMapper.fromJson(body)
                },
            )
        )
    }

    override fun innhent(person: Person, periode: Periode): List<MedlemskapDataIntern> {
        val request = MedlemskapRequest(
            ident = person.aktivIdent().identifikator,
            periode = periode
        )
        val medlemskapResultat = query(request)

        return medlemskapResultat.map {
            MedlemskapDataIntern(
                unntakId = it.unntakId,
                ident = it.ident,
                fraOgMed = it.fraOgMed,
                tilOgMed = it.tilOgMed,
                status = it.status,
                statusaarsak = it.statusaarsak,
                medlem = it.medlem,
                grunnlag = it.grunnlag,
                lovvalg = it.lovvalg,
                helsedel = it.helsedel,
                lovvalgsland = it.lovvalgsland?.uppercase(),
                kilde = mapTilKildenavn(it.sporingsinformasjon)
            )
        }
    }

    private fun mapTilKildenavn(sporing: Sporingsinformasjon?): KildesystemMedl? {
        val kilde = sporing?.kilde

        return when (kilde) {
            "APPBRK" -> KildesystemMedl(KildesystemKode.APPBRK, "Applikasjonsbruker")
            "AVGSYS" -> KildesystemMedl(KildesystemKode.AVGSYS, "Avgiftsystemet")
            "E500" -> KildesystemMedl(KildesystemKode.E500, "E-500")
            "INFOTR" -> KildesystemMedl(KildesystemKode.INFOTR, "Infotrygd")
            "LAANEKASSEN" -> KildesystemMedl(KildesystemKode.LAANEKASSEN, "Laanekassen")
            "MEDL" -> KildesystemMedl(KildesystemKode.MEDL, "MEDL")
            "PP01" -> KildesystemMedl(KildesystemKode.PP01, "Pensjon")
            "srvgosys" -> KildesystemMedl(KildesystemKode.srvgosys, "Gosys")
            "srvmelosys" -> KildesystemMedl(KildesystemKode.srvmelosys, "Melosys")
            "TP" -> KildesystemMedl(KildesystemKode.TP, "TP")
            "TPS" -> KildesystemMedl(KildesystemKode.TPS, "TPS")
            else -> null
        }
    }
}