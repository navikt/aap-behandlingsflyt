package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.integrasjon.aordning.InntektkomponentenGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold.AARegisterGateway
import no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold.EREGGateway
import no.nav.aap.behandlingsflyt.integrasjon.brev.BrevGateway
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.datadeling.SamGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.dokumentinnhenting.DokumentinnhentingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.gosys.GosysGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoBulkGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.inntekt.InntektGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.institusjonsopphold.InstitusjonsoppholdGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.kabal.KabalGateway
import no.nav.aap.behandlingsflyt.integrasjon.medlemsskap.MedlemskapGateway
import no.nav.aap.behandlingsflyt.integrasjon.meldekort.MeldekortGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.oppgave.OppgavestyringGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomInfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NorgGateway
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlBarnGateway
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlPersonopplysningGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusForeldrepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusSykepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.DagpengerGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.samordning.TjenestePensjonGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.statistikk.StatistikkGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.tilgang.TilgangGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.ufore.UføreGateway
import no.nav.aap.behandlingsflyt.integrasjon.utbetaling.UtbetalingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.YrkesskadeRegisterGatewayImpl
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import kotlin.reflect.KClass

fun testGatewayProvider(unleashGateway: KClass<out UnleashGateway> = AlleAvskruddUnleash::class): GatewayProvider {
    listOf(
        "inntekt",
        "institusjonsopphold",
        "institusjonsoppholdenkelt",
        "oppgavestyring",
        "pesys",
        "yrkesskade",
        "utbetal",
    ).forEach {
        if (System.getProperty("integrasjon.$it.url") != null) return@forEach
        System.setProperty("integrasjon.$it.url", "dummy")
        System.setProperty("integrasjon.$it.scope", "dummy")
    }
    // Only set these if not already configured (e.g. by FakeServers), to avoid overwriting real fake server URLs
    if (System.getProperty("azure.openid.config.token.endpoint") == null) {
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:123/token/x12345")
        System.setProperty("azure.app.client.id", "behandlingsflyt")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.issuer", "behandlingsflyt")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:12/jwks")
    }

    return createGatewayProvider {
        register<PdlBarnGateway>()
        register<PdlIdentGateway>()
        register<PdlPersoninfoBulkGateway>()
        register<PdlPersoninfoGateway>()
        register<PdlPersonopplysningGateway>()
        register<AbakusSykepengerGateway>()
        register<AbakusForeldrepengerGateway>()
        register<DokumentinnhentingGatewayImpl>()
        register<MedlemskapGateway>()
        register<FakeApiInternGateway>()
        register<UtbetalingGatewayImpl>()
        register<AARegisterGateway>()
        register<EREGGateway>()
        register<StatistikkGatewayImpl>()
        register<InntektGatewayImpl>()
        register<InstitusjonsoppholdGatewayImpl>()
        register<InntektkomponentenGatewayImpl>()
        register<BrevGateway>()
        register<OppgavestyringGatewayImpl>()
        register<UføreGateway>()
        register<YrkesskadeRegisterGatewayImpl>()
        register<MeldekortGatewayImpl>()
        register<TjenestePensjonGatewayImpl>()
        register(unleashGateway)
        register<SamGatewayImpl>()
        register<NomInfoGateway>()
        register<KabalGateway>()
        register<NorgGateway>()
        register<TilgangGatewayImpl>()
        register<GosysGateway>()
        register<DagpengerGatewayImpl>()
    }
}