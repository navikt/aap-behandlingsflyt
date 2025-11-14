package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class AvklarForutgåendeMedlemskapLøser(
    private val forutgåendeMedlemskapRepository: MedlemskapArbeidInntektForutgåendeRepository,
    private val sakRepository: SakRepository
) : AvklaringsbehovsLøser<AvklarForutgåendeMedlemskapLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        forutgåendeMedlemskapRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarForutgåendeMedlemskapLøsning): LøsningsResultat {
        val sak = sakRepository.hent(kontekst.kontekst.sakId)

        forutgåendeMedlemskapRepository.lagreManuellVurdering(
            kontekst.behandlingId(),
            ManuellVurderingForForutgåendeMedlemskap(
                begrunnelse = løsning.manuellVurderingForForutgåendeMedlemskap.begrunnelse,
                harForutgåendeMedlemskap = løsning.manuellVurderingForForutgåendeMedlemskap.harForutgåendeMedlemskap,
                varMedlemMedNedsattArbeidsevne = løsning.manuellVurderingForForutgåendeMedlemskap.varMedlemMedNedsattArbeidsevne,
                medlemMedUnntakAvMaksFemAar = løsning.manuellVurderingForForutgåendeMedlemskap.medlemMedUnntakAvMaksFemAar,
                vurdertAv = kontekst.bruker.ident,
                vurdertTidspunkt = LocalDateTime.now(),
                overstyrt = false,
                vurdertIBehandling = kontekst.behandlingId(),
                fom = sak.rettighetsperiode.fom
            )
        )
        return LøsningsResultat("Vurdert forutgående medlemskap manuelt.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP
    }
}