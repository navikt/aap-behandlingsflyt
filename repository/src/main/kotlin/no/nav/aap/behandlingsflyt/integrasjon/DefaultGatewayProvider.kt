package no.nav.aap.behandlingsflyt.integrasjon

import no.nav.aap.behandlingsflyt.integrasjon.aordning.InntektkomponentenGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold.AARegisterGateway
import no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold.EREGGateway
import no.nav.aap.behandlingsflyt.integrasjon.brev.BrevGateway
import no.nav.aap.behandlingsflyt.integrasjon.datadeling.ApiInternGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.datadeling.SamGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.dokumentinnhenting.DokumentinnhentingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoBulkGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.inntekt.InntektGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.institusjonsopphold.InstitusjonsoppholdGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.kabal.KabalGateway
import no.nav.aap.behandlingsflyt.integrasjon.medlemsskap.MedlemskapGateway
import no.nav.aap.behandlingsflyt.integrasjon.meldekort.MeldekortGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.oppgave.GosysGateway
import no.nav.aap.behandlingsflyt.integrasjon.oppgave.OppgavestyringGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomInfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NorgGateway
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlBarnGateway
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlPersonopplysningGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusForeldrepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusSykepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.TjenestePensjonGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.statistikk.StatistikkGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.tilgang.TilgangGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.ufore.UføreGateway
import no.nav.aap.behandlingsflyt.integrasjon.unleash.UnleashGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.utbetaling.UtbetalingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.YrkesskadeRegisterGatewayImpl
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.gateway.GatewayRegistry

fun createGatewayProvider(body: GatewayRegistry.() -> Unit): GatewayProvider {
    return GatewayProvider(GatewayRegistry().apply(body))
}

/* Burde endre GatewayRegistry til å ikke være stateful. */
fun defaultGatewayProvider(utvidelser: GatewayRegistry.() -> Unit = {}) = createGatewayProvider {
    register<PdlBarnGateway>()
    register<PdlIdentGateway>()
    register<PdlPersoninfoBulkGateway>()
    register<PdlPersoninfoGateway>()
    register<PdlPersonopplysningGateway>()
    register<AbakusForeldrepengerGateway>()
    register<AbakusSykepengerGateway>()
    register<DokumentinnhentingGatewayImpl>()
    register<MedlemskapGateway>()
    register<ApiInternGatewayImpl>()
    register<UtbetalingGatewayImpl>()
    register<AARegisterGateway>()
    register<EREGGateway>()
    register<StatistikkGatewayImpl>()
    register<InntektGatewayImpl>()
    register<BrevGateway>()
    register<OppgavestyringGatewayImpl>()
    register<UføreGateway>()
    register<YrkesskadeRegisterGatewayImpl>()
    register<MeldekortGatewayImpl>()
    register<TilgangGatewayImpl>()
    register<TjenestePensjonGatewayImpl>()
    register<UnleashGatewayImpl>()
    register<SamGatewayImpl>()
    register<NomInfoGateway>()
    register<NorgGateway>()
    register<KabalGateway>()
    register<InntektkomponentenGatewayImpl>()
    register<InstitusjonsoppholdGatewayImpl>()
    register<GosysGateway>()
    utvidelser()
}