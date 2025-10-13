package no.nav.aap.behandlingsflyt.test

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
}


object FakeUnleash : FakeUnleashBase(
    mapOf(
        BehandlingsflytFeature.IngenValidering to false,
        BehandlingsflytFeature.SendForvaltningsmelding to true,
        BehandlingsflytFeature.SosialHjelpFlereKontorer to false,
        BehandlingsflytFeature.NyBrevtype11_18 to true,
        BehandlingsflytFeature.BrevBeregningsgrunnlag to true,
        BehandlingsflytFeature.Aktivitetsplikt11_7 to false,
        BehandlingsflytFeature.Aktivitetsplikt11_9 to false,
        BehandlingsflytFeature.OverforingsdatoNullForAvregning to true,
        BehandlingsflytFeature.OvergangUfore to false,
        BehandlingsflytFeature.IverksettUtbetalingSomSelvstendigJobb to true,
        BehandlingsflytFeature.RefaktorereFastsettSykdomsvilkar to true,
        BehandlingsflytFeature.SykepengerPeriodisert to false
    )
)

object FakeUnleashFasttrackAktivitetsplikt : FakeUnleashBase(
    mapOf(
        BehandlingsflytFeature.IngenValidering to false,
        BehandlingsflytFeature.SendForvaltningsmelding to true,
        BehandlingsflytFeature.SosialHjelpFlereKontorer to false,
        BehandlingsflytFeature.BrevBeregningsgrunnlag to true,
        BehandlingsflytFeature.Aktivitetsplikt11_7 to true,
        BehandlingsflytFeature.Aktivitetsplikt11_9 to true,
        BehandlingsflytFeature.NyBrevtype11_18 to true,
        BehandlingsflytFeature.OvergangUfore to true,
        BehandlingsflytFeature.IverksettUtbetalingSomSelvstendigJobb to true,
        BehandlingsflytFeature.RefaktorereFastsettSykdomsvilkar to true,
        BehandlingsflytFeature.SykepengerPeriodisert to true
    )
)
