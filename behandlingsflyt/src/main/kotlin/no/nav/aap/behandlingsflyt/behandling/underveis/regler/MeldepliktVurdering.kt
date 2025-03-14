package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.verdityper.dokument.JournalpostId

interface MeldepliktVurdering {
    val utfall: Utfall
    val årsak: UnderveisÅrsak?

    data object Fritak: MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val årsak = null
    }

    data object FørVedtak: MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val årsak = null
    }

    data class MeldtSeg(
        val journalpostId: JournalpostId,
    ): MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val årsak = null
    }

    data object IkkeMeldtSeg: MeldepliktVurdering {
        override val utfall = Utfall.IKKE_OPPFYLT
        override val årsak = UnderveisÅrsak.IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON
    }

    data object FremtidigIkkeOppfylt: MeldepliktVurdering {
        override val utfall = Utfall.IKKE_OPPFYLT
        override val årsak = UnderveisÅrsak.MELDEPLIKT_FRIST_IKKE_PASSERT
    }

    data object FremtidigOppfylt: MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val årsak = null
    }
}