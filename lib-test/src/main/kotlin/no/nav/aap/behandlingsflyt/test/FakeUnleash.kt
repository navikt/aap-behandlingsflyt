package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.FeatureToggle
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.Factory

class FakeUnleash(private val flags: Map<FeatureToggle, Boolean>): UnleashGateway {
    override fun isEnabled(featureToggle: FeatureToggle) = requireNotNull(flags[featureToggle]) {
        "feature toggle $featureToggle ikke definert for fake"
    }

    override fun isEnabled(
        featureToggle: FeatureToggle,
        ident: String
    ): Boolean {
       return requireNotNull(flags[featureToggle]) {
            "feature toggle $featureToggle ikke definert for fake"
        }
    }

    companion object: Factory<UnleashGateway> {
        fun med(vararg flags: Pair<FeatureToggle, Boolean>) = FakeUnleash(flags.toMap())

        override fun konstruer(): UnleashGateway {
            return FakeUnleash(mapOf(
                BehandlingsflytFeature.OverstyrStarttidspunkt to true,
                BehandlingsflytFeature.AvventUtbetaling to true,
                BehandlingsflytFeature.Manuellinntekt to true,
                BehandlingsflytFeature.FjernAutomatiskOppdateringAvBarnetillegg to true,
                BehandlingsflytFeature.InnhentEnhetsregisterData to true
            ))
        }
    }
}