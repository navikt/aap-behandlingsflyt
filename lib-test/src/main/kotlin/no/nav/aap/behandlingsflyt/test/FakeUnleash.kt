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

    override fun isVariantEnabled(featureToggle: FeatureToggle, variantName: String) = false

    override fun getVariantValue(featureToggle: FeatureToggle, variantName: String) = ""
}

open class FakeUnleashBaseWithDefaultDisabled(
    private val enabledFlags: List<BehandlingsflytFeature>,
) : UnleashGateway {
    override fun isEnabled(featureToggle: FeatureToggle) =
        enabledFlags.contains(featureToggle)

    override fun isEnabled(featureToggle: FeatureToggle, ident: String) = isEnabled(featureToggle)

    override fun isEnabled(featureToggle: FeatureToggle, ident: String, typeBrev: TypeBrev) = isEnabled(featureToggle)

    override fun isVariantEnabled(featureToggle: FeatureToggle, variantName: String) = false

    override fun getVariantValue(featureToggle: FeatureToggle, variantName: String) = ""
}


/** Mocket unleash, brukes til å teste ting lokalt hvor features i større grad er skrudd på */
object LokalUnleash : FakeUnleashBase(
    mapOf(
        BehandlingsflytFeature.IngenValidering to true,
        BehandlingsflytFeature.NyBrevbyggerV3 to false,
        BehandlingsflytFeature.Under18 to true,
        BehandlingsflytFeature.TrekkSoeknadOpprettetFraLegeerklaering to true,
        BehandlingsflytFeature.KvalitetssikringVed2213 to true,
        BehandlingsflytFeature.VisIkkeRelevantPeriode to true,
        BehandlingsflytFeature.MigrerStansOgOpphor to true,
        BehandlingsflytFeature.SamordningFaktagrunnlagBrev to true,
        BehandlingsflytFeature.GReguleringUtplukkJobb to true,
        BehandlingsflytFeature.ForutgaaendeGap to true,
        BehandlingsflytFeature.MaritimtArbeid to true,
        BehandlingsflytFeature.ForstegangsbehandlingEtterAvslagOppgave to true,
        BehandlingsflytFeature.AlleEndringerKreverKvalitetssikring to true,
        BehandlingsflytFeature.MeldepliktForsteFraForsteInnvilgelse to true,
        BehandlingsflytFeature.RevurderingEtterAvslagSkalKvalitetssikres to true,
        BehandlingsflytFeature.MeldekortEndretAvSaksbehandler to true,
        BehandlingsflytFeature.AutomatiskStans1118 to true,
        BehandlingsflytFeature.KravSteg to true,
        )
)

/** Unleash for bruk i tester - for å teste "prodlikt", hvor alle toggles er skrudd av
 * For det meste brukes denne i integrasjonstester og flyt-tester for å sjekke at ting som
 * flyt-testene fungerer selv om toggles er skrudd av
 * */
object AlleAvskruddUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.IngenValidering, // Vi må ha på validering, slik oppførselen er i prod. Dette er egentlig for å støtte superbruker
    )
)
