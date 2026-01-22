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


/** Devlik unleash. */
object FakeUnleash : FakeUnleashBase(
    mapOf(
        BehandlingsflytFeature.IngenValidering to false,
        BehandlingsflytFeature.NyBrevtype11_17 to false,
        BehandlingsflytFeature.OverforingsdatoNullForAvregning to false,
        BehandlingsflytFeature.OvergangArbeid to false,
        BehandlingsflytFeature.KvalitetssikringsSteg to false,
        BehandlingsflytFeature.EOSBeregning to false,
        BehandlingsflytFeature.NyBrevbyggerV3 to false,
        BehandlingsflytFeature.BedreUttrekkAvSakerMedFritakMeldeplikt to false,
        BehandlingsflytFeature.LagreVedtakIFatteVedtak to false,
        BehandlingsflytFeature.PeriodisertSykepengeErstatningNyAvklaringsbehovService to false,
        BehandlingsflytFeature.ValiderOvergangUfore to false,
        BehandlingsflytFeature.Under18 to false,
        BehandlingsflytFeature.MigrerMeldepliktFritak to false,
        BehandlingsflytFeature.SosialRefusjon to false,
        BehandlingsflytFeature.HentSykepengerVedOverlapp to false,
        BehandlingsflytFeature.SendBrevVedMottattKlage to false,
        BehandlingsflytFeature.MigrerRettighetsperiode to false,
        BehandlingsflytFeature.PeriodisertSykdom to false,
        BehandlingsflytFeature.Sykestipend to false,
        BehandlingsflytFeature.Forlengelse to false,
        BehandlingsflytFeature.ForlengelseIManuellBehandling to false,
        BehandlingsflytFeature.UtvidVedtakslengdeJobb to false,
        BehandlingsflytFeature.InstitusjonsoppholdJobb to false,
        BehandlingsflytFeature.TrekkSoeknadOpprettetFraLegeerklaering to false,

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

        )
)
