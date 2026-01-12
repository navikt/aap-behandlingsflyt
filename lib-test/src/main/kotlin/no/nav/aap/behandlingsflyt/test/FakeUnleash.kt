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
        BehandlingsflytFeature.OverforingsdatoNullForAvregning to true,
        BehandlingsflytFeature.OvergangArbeid to true,
        BehandlingsflytFeature.KvalitetssikringsSteg to true,
        BehandlingsflytFeature.EOSBeregning to true,
        BehandlingsflytFeature.NyBrevbyggerV3 to false,
        BehandlingsflytFeature.BedreUttrekkAvSakerMedFritakMeldeplikt to false,
        BehandlingsflytFeature.LagreVedtakIFatteVedtak to true,
        BehandlingsflytFeature.PeriodisertSykepengeErstatningNyAvklaringsbehovService to true,
        BehandlingsflytFeature.ValiderOvergangUfore to true,
        BehandlingsflytFeature.KravOmInntektsbortfall to true,
        BehandlingsflytFeature.Under18 to true,
        BehandlingsflytFeature.MigrerMeldepliktFritak to true,
        BehandlingsflytFeature.SosialRefusjon to true,
        BehandlingsflytFeature.HentSykepengerVedOverlapp to true,
        BehandlingsflytFeature.MigrerRettighetsperiode to true,
        BehandlingsflytFeature.PeriodisertSykdom to true,
        BehandlingsflytFeature.Sykestipend to false,
    )
)

object LokalUnleash : FakeUnleashBase(
    mapOf(
        BehandlingsflytFeature.IngenValidering to true,
        BehandlingsflytFeature.NyBrevtype11_17 to true,
        BehandlingsflytFeature.OverforingsdatoNullForAvregning to true,
        BehandlingsflytFeature.OvergangArbeid to true,
        BehandlingsflytFeature.KvalitetssikringsSteg to true,
        BehandlingsflytFeature.EOSBeregning to true,
        BehandlingsflytFeature.NyBrevbyggerV3 to false,
        BehandlingsflytFeature.BedreUttrekkAvSakerMedFritakMeldeplikt to true,
        BehandlingsflytFeature.LagreVedtakIFatteVedtak to true,
        BehandlingsflytFeature.PeriodisertSykepengeErstatningNyAvklaringsbehovService to true,
        BehandlingsflytFeature.ValiderOvergangUfore to true,
        BehandlingsflytFeature.KravOmInntektsbortfall to true,
        BehandlingsflytFeature.Under18 to true,
        BehandlingsflytFeature.MigrerMeldepliktFritak to true,
        BehandlingsflytFeature.SosialRefusjon to true,
        BehandlingsflytFeature.HentSykepengerVedOverlapp to true,
        BehandlingsflytFeature.PeriodisertSykdom to true,
        BehandlingsflytFeature.Sykestipend to true,
    )
)
