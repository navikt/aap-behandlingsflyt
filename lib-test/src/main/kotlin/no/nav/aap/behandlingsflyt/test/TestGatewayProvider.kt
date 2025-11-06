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
import no.nav.aap.behandlingsflyt.integrasjon.samordning.TjenestePensjonGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.statistikk.StatistikkGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.tilgang.TilgangGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.ufore.UføreGateway
import no.nav.aap.behandlingsflyt.integrasjon.utbetaling.UtbetalingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.YrkesskadeRegisterGatewayImpl
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import kotlin.reflect.KClass

fun testGatewayProvider(unleashGateway: KClass<out UnleashGateway>) = createGatewayProvider {
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
}