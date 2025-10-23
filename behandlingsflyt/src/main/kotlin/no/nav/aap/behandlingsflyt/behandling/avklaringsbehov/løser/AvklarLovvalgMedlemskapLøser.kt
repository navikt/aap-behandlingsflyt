package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class AvklarLovvalgMedlemskapLøser(
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository,
    private val sakRepository: SakRepository,
) : AvklaringsbehovsLøser<AvklarLovvalgMedlemskapLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        medlemskapArbeidInntektRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarLovvalgMedlemskapLøsning): LøsningsResultat {
        medlemskapArbeidInntektRepository.lagreVurderinger(
            kontekst.behandlingId(),
            listOf(
                ManuellVurderingForLovvalgMedlemskap(
                    lovvalgVedSøknadsTidspunkt = løsning.manuellVurderingForLovvalgMedlemskap.lovvalgVedSøknadsTidspunkt,
                    medlemskapVedSøknadsTidspunkt = løsning.manuellVurderingForLovvalgMedlemskap.medlemskapVedSøknadsTidspunkt,
                    vurdertAv = kontekst.bruker.ident,
                    vurdertDato = LocalDateTime.now(),
                    fom = sakRepository.hent(kontekst.kontekst.sakId).rettighetsperiode.fom,
                    vurdertIBehandling = kontekst.behandlingId(),
                    overstyrt = false
                )
            )
        )

        return LøsningsResultat("Vurdert lovvalg & medlemskap manuelt.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP
    }
}