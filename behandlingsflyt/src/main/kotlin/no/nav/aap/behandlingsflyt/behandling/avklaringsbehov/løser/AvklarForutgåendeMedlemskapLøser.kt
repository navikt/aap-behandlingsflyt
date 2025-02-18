package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarForutgåendeMedlemskapLøser(connection: DBConnection): AvklaringsbehovsLøser<AvklarForutgåendeMedlemskapLøsning> {
    private val repositoryProvider = RepositoryProvider(connection)
    private val forutgåendeMedlemskapRepository = repositoryProvider.provide<MedlemskapArbeidInntektForutgåendeRepository>()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarForutgåendeMedlemskapLøsning): LøsningsResultat {
        forutgåendeMedlemskapRepository.lagreManuellVurdering(kontekst.behandlingId(), løsning.manuellVurderingForForutgåendeMedlemskap)
        return LøsningsResultat("Vurdert forutgående medlemskap manuelt.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP
    }
}