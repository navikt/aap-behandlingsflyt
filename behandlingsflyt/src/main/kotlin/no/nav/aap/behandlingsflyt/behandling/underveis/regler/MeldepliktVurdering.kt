package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus.FREMTIDIG_IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus.FREMTIDIG_OPPFYLT
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus.FRITAK
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus.FØR_VEDTAK
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus.FØRSTE_MELDEPERIODE_MED_RETT
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus.IKKE_MELDT_SEG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus.MELDT_SEG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus.UTEN_RETT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.verdityper.dokument.JournalpostId

enum class MeldepliktStatus {
    FØR_VEDTAK,
    FØRSTE_MELDEPERIODE_MED_RETT,
    UTEN_RETT,
    FRITAK,
    MELDT_SEG,
    IKKE_MELDT_SEG,
    FREMTIDIG_IKKE_OPPFYLT,
    FREMTIDIG_OPPFYLT,
    RIMELIG_GRUNN
}

interface MeldepliktVurdering {
    val utfall: Utfall
    val årsak: UnderveisÅrsak?
    val status: MeldepliktStatus

    data object Fritak: MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val årsak = null
        override val status = FRITAK
    }

    data object FørVedtak: MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val årsak = null
        override val status = FØR_VEDTAK
    }

    data object UtenRett: MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val årsak = null
        override val status = UTEN_RETT
    }

    data object FørsteMeldeperiodeMedRett: MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val årsak = null
        override val status = FØRSTE_MELDEPERIODE_MED_RETT
    }

    data class MeldtSeg(
        val journalpostId: JournalpostId,
    ): MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val årsak = null
        override val status = MELDT_SEG
    }

    data object IkkeMeldtSeg: MeldepliktVurdering {
        override val utfall = Utfall.IKKE_OPPFYLT
        override val årsak = UnderveisÅrsak.IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON
        override val status = IKKE_MELDT_SEG
    }

    data object FremtidigIkkeOppfylt: MeldepliktVurdering {
        override val utfall = Utfall.IKKE_OPPFYLT
        override val årsak = UnderveisÅrsak.MELDEPLIKT_FRIST_IKKE_PASSERT
        override val status = FREMTIDIG_IKKE_OPPFYLT
    }

    data object FremtidigOppfylt: MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val årsak = null
        override val status = FREMTIDIG_OPPFYLT
    }
    
    data object RimeligGrunn: MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val årsak = null
        override val status = MeldepliktStatus.RIMELIG_GRUNN
    }
}