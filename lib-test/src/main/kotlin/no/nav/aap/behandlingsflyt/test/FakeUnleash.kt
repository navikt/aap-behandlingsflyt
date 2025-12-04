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


/** Devlik unleash. */
object FakeUnleash : FakeUnleashBase(
    mapOf(
        BehandlingsflytFeature.IngenValidering to false,
        BehandlingsflytFeature.NyBrevtype11_17 to true,
        BehandlingsflytFeature.NyBrevtype11_18 to true,
        BehandlingsflytFeature.Aktivitetsplikt11_9 to true,
        BehandlingsflytFeature.OverforingsdatoNullForAvregning to true,
        BehandlingsflytFeature.OvergangArbeid to false,
        BehandlingsflytFeature.KvalitetssikringsSteg to true,
        BehandlingsflytFeature.FatteVedtakAvklaringsbehovService to true,
        BehandlingsflytFeature.EOSBeregning to false,
        BehandlingsflytFeature.NyeBarn to true,
        BehandlingsflytFeature.NyBrevbyggerV3 to false,
        BehandlingsflytFeature.BedreUttrekkAvSakerMedFritakMeldeplikt to false,
        BehandlingsflytFeature.LagreVedtakIFatteVedtak to true,
        BehandlingsflytFeature.UnntakMeldepliktDesember to true,
        BehandlingsflytFeature.ReduksjonArbeidOverGrense to true,
        BehandlingsflytFeature.ReduksjonIkkeMeldtSeg to true,
    )
)

/** Før du merger så kan det være lurt å sjekke om feature-togglene
 * under matcher prod og hvis du flipper "din" feature toggle, så er testene
 * fortsatt grønne. */
object ProdlikUnleash : FakeUnleashBase(
    mapOf(
        BehandlingsflytFeature.IngenValidering to false,
        BehandlingsflytFeature.NyBrevtype11_17 to false,
        BehandlingsflytFeature.NyBrevtype11_18 to true,
        BehandlingsflytFeature.Aktivitetsplikt11_9 to true,
        BehandlingsflytFeature.OverforingsdatoNullForAvregning to false,
        BehandlingsflytFeature.OvergangArbeid to false,
        BehandlingsflytFeature.KvalitetssikringsSteg to false,
        BehandlingsflytFeature.FatteVedtakAvklaringsbehovService to true,
        BehandlingsflytFeature.EOSBeregning to false,
        BehandlingsflytFeature.NyeBarn to false,
        BehandlingsflytFeature.NyBrevbyggerV3 to false,
        BehandlingsflytFeature.BedreUttrekkAvSakerMedFritakMeldeplikt to false,
        BehandlingsflytFeature.LagreVedtakIFatteVedtak to false,
        BehandlingsflytFeature.ReduksjonArbeidOverGrense to false,
        BehandlingsflytFeature.ReduksjonIkkeMeldtSeg to false,
        BehandlingsflytFeature.UnntakMeldepliktDesember to false,
    )
)

object LokalUnleash : FakeUnleashBase(
    mapOf(
        BehandlingsflytFeature.IngenValidering to true,
        BehandlingsflytFeature.NyBrevtype11_17 to true,
        BehandlingsflytFeature.NyBrevtype11_18 to true,
        BehandlingsflytFeature.Aktivitetsplikt11_9 to true,
        BehandlingsflytFeature.OverforingsdatoNullForAvregning to true,
        BehandlingsflytFeature.OvergangArbeid to true,
        BehandlingsflytFeature.KvalitetssikringsSteg to true,
        BehandlingsflytFeature.FatteVedtakAvklaringsbehovService to true,
        BehandlingsflytFeature.EOSBeregning to false,
        BehandlingsflytFeature.NyeBarn to true,
        BehandlingsflytFeature.NyBrevbyggerV3 to false,
        BehandlingsflytFeature.BedreUttrekkAvSakerMedFritakMeldeplikt to true,
        BehandlingsflytFeature.LagreVedtakIFatteVedtak to true,
        BehandlingsflytFeature.UnntakMeldepliktDesember to true,
        BehandlingsflytFeature.ReduksjonArbeidOverGrense to true,
        BehandlingsflytFeature.ReduksjonIkkeMeldtSeg to true,
    )
)
