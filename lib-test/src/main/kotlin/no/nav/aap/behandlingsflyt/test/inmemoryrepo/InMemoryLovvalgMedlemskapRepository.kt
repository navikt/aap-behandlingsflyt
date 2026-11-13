package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntekterForKrav
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntekterINorgeRegisterGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.LovvalgMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.LovvalgMedlemskapRegisterGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.LovvalgMedlemskapRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

object InMemoryLovvalgMedlemskapRepository : LovvalgMedlemskapRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, LovvalgMedlemskapGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): LovvalgMedlemskapGrunnlag? = grunnlag[behandlingId]

    override fun lagreRegisterGrunnlag(
        behandlingId: BehandlingId,
        relevantDato: LocalDate,
        registerGrunnlag: LovvalgMedlemskapRegisterGrunnlag,
        medlId: Long?
    ) {
        val nyttRegister = registerGrunnlag.copy(
            medlemskapUnntakGrunnlag = medlId?.let(InMemoryMedlemskapUnntakRepository::hent),
            inntekterINorgeGrunnlag = registerGrunnlag.inntekterINorgeGrunnlag.toList(),
            arbeiderINorgeGrunnlag = registerGrunnlag.arbeiderINorgeGrunnlag.map {
                it.copy(ansettelsesdetaljer = it.ansettelsesdetaljer.toList())
            }
        )
        val inntekterForKrav = if (nyttRegister.inntekterINorgeGrunnlag.isEmpty()) {
            emptySet()
        } else {
            setOf(InntekterForKrav(relevantDato, nyttRegister.inntekterINorgeGrunnlag.toSet()))
        }
        grunnlag.compute(behandlingId) { _, eksisterende ->
            LovvalgMedlemskapGrunnlag(
                nyttRegister,
                InntekterINorgeRegisterGrunnlag(inntekterForKrav),
                eksisterende?.vurderinger.orEmpty()
            )
        }
    }

    override fun lagreVurderinger(behandlingId: BehandlingId, vurderinger: List<ManuellVurderingForLovvalgMedlemskap>) {
        val overstyrt = vurderinger.any { it.overstyrt }
        val nyeVurderinger = vurderinger.map { it.copy(overstyrt = overstyrt) }
        grunnlag.compute(behandlingId) { _, eksisterende ->
            eksisterende?.copy(vurderinger = nyeVurderinger) ?: LovvalgMedlemskapGrunnlag(
                LovvalgMedlemskapRegisterGrunnlag(null, emptyList(), emptyList()),
                InntekterINorgeRegisterGrunnlag(emptySet()),
                nyeVurderinger
            )
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
