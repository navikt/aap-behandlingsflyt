package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarLovvalgMedlemskapLøser(connection: DBConnection): AvklaringsbehovsLøser<AvklarLovvalgMedlemskapLøsning> {
    private val repositoryProvider = RepositoryProvider(connection)
    private val medlemskapArbeidInntektRepository = repositoryProvider.provide<MedlemskapArbeidInntektRepository>()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarLovvalgMedlemskapLøsning): LøsningsResultat {
        medlemskapArbeidInntektRepository.lagreManuellVurdering(kontekst.behandlingId(), løsning.manuellVurderingForLovvalgMedlemskap)
        return LøsningsResultat("Vurdert lovvalg & medlemskap manuelt.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP
    }
}