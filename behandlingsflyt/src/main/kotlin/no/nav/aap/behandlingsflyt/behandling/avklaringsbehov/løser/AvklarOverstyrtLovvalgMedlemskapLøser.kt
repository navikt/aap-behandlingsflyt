package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOverstyrtLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarOverstyrtLovvalgMedlemskapLøser(connection: DBConnection): AvklaringsbehovsLøser<AvklarOverstyrtLovvalgMedlemskapLøsning> {
    private val repositoryProvider = RepositoryProvider(connection)
    private val medlemskapArbeidInntektRepository = repositoryProvider.provide<MedlemskapArbeidInntektRepository>()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarOverstyrtLovvalgMedlemskapLøsning): LøsningsResultat {
        medlemskapArbeidInntektRepository.lagreManuellVurdering(kontekst.behandlingId(), løsning.manuellVurderingForLovvalgMedlemskap, true)
        return LøsningsResultat("OVERSTYRT: Vurdert lovvalg & medlemskap manuelt.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.MANUELL_OVERSTYRING_LOVVALG
    }
}