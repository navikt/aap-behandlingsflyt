package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.FeatureToggle
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway

open class FakeUnleashBase(
    private val flags: Map<BehandlingsflytFeature, Boolean>,
) : UnleashGateway {
    override fun isEnabled(featureToggle: FeatureToggle) = requireNotNull(flags[featureToggle]) {
        "feature toggle $featureToggle ikke definert for fake"
    }

    override fun isEnabled(featureToggle: FeatureToggle, ident: String) = isEnabled(featureToggle)

    override fun isEnabled(featureToggle: FeatureToggle, ident: String, typeBrev: TypeBrev) = isEnabled(featureToggle)
}

open class FakeUnleashBaseWithDefaultDisabled(
    private val enabledFlags: List<BehandlingsflytFeature>,
) : UnleashGateway {
    override fun isEnabled(featureToggle: FeatureToggle) =
        enabledFlags.contains(featureToggle)

    override fun isEnabled(featureToggle: FeatureToggle, ident: String) = isEnabled(featureToggle)

    override fun isEnabled(featureToggle: FeatureToggle, ident: String, typeBrev: TypeBrev) = isEnabled(featureToggle)
}


/** Mocket unleash, brukes til å teste ting lokalt hvor features i større grad er skrudd på */
object LokalUnleash : FakeUnleashBase(
    mapOf(
        BehandlingsflytFeature.IngenValidering to true,
        BehandlingsflytFeature.NyBrevtype11_17 to true,
        BehandlingsflytFeature.OverforingsdatoNullForAvregning to true,
        BehandlingsflytFeature.OvergangArbeid to true,
        BehandlingsflytFeature.KvalitetssikringsSteg to true,
        BehandlingsflytFeature.NyBrevbyggerV3 to false,
        BehandlingsflytFeature.LagreVedtakIFatteVedtak to true,
        BehandlingsflytFeature.PeriodisertSykepengeErstatningNyAvklaringsbehovService to true,
        BehandlingsflytFeature.ValiderOvergangUfore to true,
        BehandlingsflytFeature.Under18 to true,
        BehandlingsflytFeature.MigrerMeldepliktFritak to true,
        BehandlingsflytFeature.SosialRefusjon to true,
        BehandlingsflytFeature.HentSykepengerVedOverlapp to true,
        BehandlingsflytFeature.SendBrevVedMottattKlage to true,
        BehandlingsflytFeature.PeriodisertSykdom to true,
        BehandlingsflytFeature.Sykestipend to true,
        BehandlingsflytFeature.Forlengelse to true,
        BehandlingsflytFeature.ForlengelseIManuellBehandling to true,
        BehandlingsflytFeature.UtvidVedtakslengdeJobb to true,
        BehandlingsflytFeature.InstitusjonsoppholdJobb to true,
        BehandlingsflytFeature.TrekkSoeknadOpprettetFraLegeerklaering to true,
        BehandlingsflytFeature.UbehandledeMeldekortJobb to true,
        )
)

/** Unleash for bruk i tester - for å teste "prodlikt", hvor alle toggles er skrudd av
 * For det meste brukes denne i integrasjonstester og flyt-tester for å sjekke at ting som
 * flyt-testene fungerer selv om toggles er skrudd av
 * */
object AlleAvskruddUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.IngenValidering, // Vi må ha på validering, slik oppførselen er i prod. Dette er egentlig for å støtte superbruker
        BehandlingsflytFeature.KvalitetssikringsSteg // Mange tester tryner om denne er av, fikser ikke det nå, men denne fjernes snart
    )
)

