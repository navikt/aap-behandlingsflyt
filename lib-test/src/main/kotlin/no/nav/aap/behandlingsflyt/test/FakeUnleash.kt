package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.FeatureToggle
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.verdityper.Bruker

open class FakeUnleashBase(
    private val flags: Map<BehandlingsflytFeature, Boolean>,
) : UnleashGateway {
    override fun isEnabled(featureToggle: FeatureToggle) = requireNotNull(flags[featureToggle]) {
        "feature toggle $featureToggle ikke definert for fake"
    }

    override fun isEnabled(featureToggle: FeatureToggle, ident: Bruker) = isEnabled(featureToggle)

    override fun isEnabled(featureToggle: FeatureToggle, ident: Bruker, typeBrev: TypeBrev) = isEnabled(featureToggle)

    override fun isVariantEnabled(featureToggle: FeatureToggle, variantName: String) = false

    override fun getVariantValue(featureToggle: FeatureToggle, variantName: String) = ""

    override fun erPåskruddForSak(featureToggle: FeatureToggle, variantName: String, saksnummer: Saksnummer) = false

    override fun erPåskruddForSak(
        featureToggle: FeatureToggle,
        variantName: String,
        saksnummerResolver: () -> Saksnummer
    ): Boolean {
        return erPåskruddForSak(featureToggle, variantName, saksnummerResolver())
    }
}

open class FakeUnleashBaseWithDefaultDisabled(
    private val enabledFlags: List<BehandlingsflytFeature>,
) : UnleashGateway {
    override fun isEnabled(featureToggle: FeatureToggle) =
        enabledFlags.contains(featureToggle)

    override fun isEnabled(featureToggle: FeatureToggle, ident: Bruker) = isEnabled(featureToggle)

    override fun isEnabled(featureToggle: FeatureToggle, ident: Bruker, typeBrev: TypeBrev) = isEnabled(featureToggle)

    override fun isVariantEnabled(featureToggle: FeatureToggle, variantName: String) = false

    override fun getVariantValue(featureToggle: FeatureToggle, variantName: String) = ""

    override fun erPåskruddForSak(featureToggle: FeatureToggle, variantName: String, saksnummer: Saksnummer) = false

    override fun erPåskruddForSak(
        featureToggle: FeatureToggle,
        variantName: String,
        saksnummerResolver: () -> Saksnummer
    ): Boolean {
        return erPåskruddForSak(featureToggle, variantName, saksnummerResolver())
    }
}


/** Mocket unleash, brukes til å teste ting lokalt hvor features i større grad er skrudd på */
object LokalUnleash : FakeUnleashBase(
    mapOf(
        BehandlingsflytFeature.IngenValidering to true,
        BehandlingsflytFeature.NyBrevbyggerV3 to false,
        BehandlingsflytFeature.Under18 to true,
        BehandlingsflytFeature.TrekkSoeknadOpprettetFraLegeerklaering to true,
        BehandlingsflytFeature.VisIkkeRelevantPeriode to true,
        BehandlingsflytFeature.MigrerStansOgOpphor to true,
        BehandlingsflytFeature.SamordningFaktagrunnlagBrev to true,
        BehandlingsflytFeature.GReguleringUtplukkJobb to true,
        BehandlingsflytFeature.MeldekortEndretAvSaksbehandler to true,
        BehandlingsflytFeature.AutomatiskStans1118 to true,
        BehandlingsflytFeature.StudentV2 to true,
        BehandlingsflytFeature.BackfillStansOpphor to true,
        BehandlingsflytFeature.LagreVurderRettighetsperiodeSomKrav to true,
        BehandlingsflytFeature.VentStatusForTilbakekrevingIBehandlingsflyt to true,
        // --- Krav ---
        BehandlingsflytFeature.KravSteg to true,
        BehandlingsflytFeature.KravManuellVurdering to true,
        BehandlingsflytFeature.KravAutomatiskVurdering to true,
        BehandlingsflytFeature.NyttKravPeriodiserteAvklaringsbehov to true,
        // ------
        BehandlingsflytFeature.ManuellInntektDelvisUfore to true,
        BehandlingsflytFeature.Avslag11_27 to true,
        BehandlingsflytFeature.SkalViseAlleSykdomssteg to true,
    )
) {
    override fun getVariantValue(featureToggle: FeatureToggle, variantName: String): String {
        return when (Pair(featureToggle, variantName)) {
            Pair(
                BehandlingsflytFeature.NyttKravPeriodiserteAvklaringsbehov,
                "saksnumre"
            ) -> "LoCAL_4LDW2A8"

            else -> "1,100"
        }
    }

    override fun erPåskruddForSak(featureToggle: FeatureToggle, variantName: String, saksnummer: Saksnummer) = isEnabled(featureToggle)

    override fun erPåskruddForSak(
        featureToggle: FeatureToggle,
        variantName: String,
        saksnummerResolver: () -> Saksnummer
    ): Boolean {
        return erPåskruddForSak(featureToggle, variantName, saksnummerResolver())
    }}

/** Unleash for bruk i tester - for å teste "prodlikt", hvor alle toggles er skrudd av
 * For det meste brukes denne i integrasjonstester og flyt-tester for å sjekke at ting som
 * flyt-testene fungerer selv om toggles er skrudd av
 * */
object AlleAvskruddUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.IngenValidering, // Vi må ha på validering, slik oppførselen er i prod. Dette er egentlig for å støtte superbruker
    )
)
