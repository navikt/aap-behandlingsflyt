package no.nav.aap.behandlingsflyt.test

import kotlinx.io.bytestring.ByteString
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.FeatureToggle
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway

object FakeUnleash : UnleashGateway {
    private val flags = mapOf(
        BehandlingsflytFeature.OverstyrStarttidspunkt to true,
        BehandlingsflytFeature.FasttrackMeldekort to false,
        BehandlingsflytFeature.Samvarsling to true,
        BehandlingsflytFeature.IngenValidering to false,
        BehandlingsflytFeature.SendForvaltningsmelding to true,
        BehandlingsflytFeature.AvrundingInntekt to true,
        BehandlingsflytFeature.HoppOverForeslaaVedtak to true,
        BehandlingsflytFeature.SosialHjelpFlereKontorer to false,
    )

    override fun isEnabled(featureToggle: FeatureToggle) = requireNotNull(flags[featureToggle]) {
        "feature toggle $featureToggle ikke definert for fake"
    }

    override fun isEnabled(
        featureToggle: FeatureToggle,
        ident: String,
    ): Boolean {
        return requireNotNull(flags[featureToggle]) {
            "feature toggle $featureToggle ikke definert for fake"
        }
    }
}
