package no.nav.aap.behandlingsflyt.unleash

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.verdityper.Bruker

interface UnleashGateway : Gateway {
    fun isEnabled(featureToggle: FeatureToggle): Boolean
    fun isDisabled(featureToggle: FeatureToggle): Boolean = !isEnabled(featureToggle)
    fun isEnabled(featureToggle: FeatureToggle, ident: Bruker): Boolean
    fun isDisabled(featureToggle: FeatureToggle, ident: Bruker): Boolean = !isEnabled(featureToggle, ident)
    fun isEnabled(featureToggle: FeatureToggle, ident: Bruker, typeBrev: TypeBrev): Boolean

    fun isVariantEnabled(featureToggle: FeatureToggle, variantName: String): Boolean
    fun getVariantValue(featureToggle: FeatureToggle, variantName: String): String
    fun erPåskruddForSak(featureToggle: FeatureToggle, variantName: String, saksnummer: Saksnummer): Boolean
    fun erPåskruddForSak(featureToggle: FeatureToggle, variantName: String, saksnummerResolver: () -> Saksnummer): Boolean
}
