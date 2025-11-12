package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType

enum class Kvote(val avslagsårsak: VarighetVurdering.Avslagsårsak, val tellerMotKvote: (Vurdering) -> Boolean) {
    ORDINÆR(VarighetVurdering.Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP, ::skalTelleMotOrdinærKvote),
    STUDENT(VarighetVurdering.Avslagsårsak.STUDENTKVOTE_BRUKT_OPP, ::skalTelleMotStudentKvote),
    /**
     * § 11-15. Arbeidsavklaringspenger til et medlem som etablerer egen virksomhet
     */
    @Deprecated("Ikke kvote")
    ETABLERINGSFASE(VarighetVurdering.Avslagsårsak.ETABLERINGSFASEKVOTE_BRUKT_OPP, { false }),

    /**
     * § 11-15. Arbeidsavklaringspenger til et medlem som etablerer egen virksomhet
     */
    @Deprecated("Ikke kvote")
    UTVIKLINGSFASE(VarighetVurdering.Avslagsårsak.UTVIKLINGSFASEKVOTE_BRUKT_OPP, { false }),

    /**
     * 11-13. Telles når rettighetstype er [RettighetsType.SYKEPENGEERSTATNING].
     */
    SYKEPENGEERSTATNING(
        VarighetVurdering.Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP,
        ::skalTelleMotSykepengeKvote
    );
}

private fun skalTelleMotOrdinærKvote(vurdering: Vurdering): Boolean {
    return vurdering.harRett() && vurdering.rettighetsType() in setOf(
        RettighetsType.BISTANDSBEHOV,
        RettighetsType.STUDENT
    ) && !skalTelleMotSykepengeKvote(vurdering)
}

private fun skalTelleMotStudentKvote(vurdering: Vurdering): Boolean {
    return vurdering.harRett() && vurdering.rettighetsType() == RettighetsType.STUDENT
}

private fun skalTelleMotSykepengeKvote(vurdering: Vurdering): Boolean {
    return vurdering.harRett() && vurdering.rettighetsType() == RettighetsType.SYKEPENGEERSTATNING
}