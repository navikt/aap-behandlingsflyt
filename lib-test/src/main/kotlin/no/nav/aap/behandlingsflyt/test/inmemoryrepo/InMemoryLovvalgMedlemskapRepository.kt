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
        grunnlag.compute(behandlingId) { _, eksisterende ->
            LovvalgMedlemskapGrunnlag(
                registerGrunnlag = registerGrunnlag,
                inntekterINorgeRegisterGrunnlag = InntekterINorgeRegisterGrunnlag(
                    setOf(InntekterForKrav(relevantDato, registerGrunnlag.inntekterINorgeGrunnlag.toSet()))
                ),
                vurderinger = eksisterende?.vurderinger.orEmpty(),
            )
        }
    }

    override fun lagreVurderinger(behandlingId: BehandlingId, vurderinger: List<ManuellVurderingForLovvalgMedlemskap>) {
        grunnlag.compute(behandlingId) { _, eksisterende ->
            (eksisterende ?: LovvalgMedlemskapGrunnlag(
                registerGrunnlag = LovvalgMedlemskapRegisterGrunnlag(null, emptyList(), emptyList()),
                inntekterINorgeRegisterGrunnlag = InntekterINorgeRegisterGrunnlag(emptySet()),
            )).copy(vurderinger = vurderinger)
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        grunnlag[fraBehandling]?.let { grunnlag[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
    }
}
