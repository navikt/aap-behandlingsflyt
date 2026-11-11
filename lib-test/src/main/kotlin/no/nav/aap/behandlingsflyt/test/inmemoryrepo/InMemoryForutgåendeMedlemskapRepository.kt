package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.EnhetGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMåned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.tilDomene
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.ForutgåendeMedlemskapRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryForutgåendeMedlemskapRepository : ForutgåendeMedlemskapRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, ForutgåendeMedlemskapGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): ForutgåendeMedlemskapGrunnlag? = grunnlag[behandlingId]

    override fun lagreArbeidsforholdOgInntektINorge(
        behandlingId: BehandlingId,
        arbeidGrunnlag: List<ArbeidINorgeGrunnlag>,
        inntektGrunnlag: List<ArbeidsInntektMåned>,
        medlId: Long?,
        enhetGrunnlag: List<EnhetGrunnlag>
    ) {
        val medlemskap = medlId?.let(InMemoryMedlemskapUnntakForutgåendeRepository::hent)
        val arbeider = arbeidGrunnlag.map { arbeid ->
            arbeid.copy(
                organisasjonsNavn = enhetGrunnlag.firstOrNull { it.orgnummer == arbeid.identifikator }?.orgNavn,
                ansettelsesdetaljer = arbeid.ansettelsesdetaljer.toList()
            )
        }
        val inntekter = inntektGrunnlag.tilDomene(enhetGrunnlag)
        grunnlag.compute(behandlingId) { _, eksisterende ->
            ForutgåendeMedlemskapGrunnlag(medlemskap, inntekter, arbeider, eksisterende?.vurderinger.orEmpty())
        }
    }

    override fun lagreVurderinger(behandlingId: BehandlingId, vurderinger: List<ManuellVurderingForForutgåendeMedlemskap>) {
        val overstyrt = vurderinger.any { it.overstyrt }
        val nyeVurderinger = vurderinger.map { it.copy(overstyrt = overstyrt) }
        grunnlag.compute(behandlingId) { _, eksisterende ->
            eksisterende?.copy(vurderinger = nyeVurderinger)
                ?: ForutgåendeMedlemskapGrunnlag(null, emptyList(), emptyList(), nyeVurderinger)
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        grunnlag[fraBehandling]?.let { grunnlag[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
    }
}
