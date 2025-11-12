package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.RefusjonkravLøsning
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class RefusjonkravLøser(
    private val refusjonkravRepository: RefusjonkravRepository,
    private val sakRepository: SakRepository,
    private val vedtakService: VedtakService
    ) : AvklaringsbehovsLøser<RefusjonkravLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        refusjonkravRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        vedtakService = VedtakService(repositoryProvider),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: RefusjonkravLøsning): LøsningsResultat {
        val vurderinger = validerKrav(løsning).let {
            it.map { dto ->
                RefusjonkravVurdering(
                    harKrav = dto.harKrav,
                    navKontor = dto.navKontor,
                    vurdertAv = kontekst.bruker.ident
                )
            }

        }
        refusjonkravRepository.lagre(kontekst.kontekst.sakId, kontekst.behandlingId(), vurderinger)
        return LøsningsResultat("Vurdert refusjonskrav")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.REFUSJON_KRAV
    }

    private fun validerKrav(
        løsning: RefusjonkravLøsning
    ): List<RefusjonkravVurderingDto> {

        return løsning.refusjonkravVurderinger.map { vurdering ->
            if (vurdering.harKrav) {
                val navKontor = vurdering.navKontor
                RefusjonkravVurderingDto(
                    harKrav = true,
                    fom = null,
                    tom = null,
                    navKontor = navKontor
                )
            } else {
                vurdering
            }
        }
    }
}
