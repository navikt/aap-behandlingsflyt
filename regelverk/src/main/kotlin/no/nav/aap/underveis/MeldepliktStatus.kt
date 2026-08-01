package no.nav.aap.underveis

import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.verdityper.dokument.JournalpostId

enum class MeldepliktStatus {
    FØR_VEDTAK,
    FØRSTE_MELDEPERIODE_MED_RETT,
    UTEN_RETT,
    FRITAK,
    MELDT_SEG,
    IKKE_MELDT_SEG,
    RIMELIG_GRUNN,

    @Suppress("unused")
    @Deprecated("Verdien produseres ikke lenger, men vil kunne leses ut fra databasen.")
    FREMTIDIG_OPPFYLT,

    @Suppress("unused") // leses fra database
    @Deprecated("Verdien brukes ikke, men vil kunne leses ut fra databasen.")
    FREMTIDIG_IKKE_OPPFYLT,
}

interface MeldepliktVurdering {
    val utfall: Utfall
    val status: MeldepliktStatus

    data object Fritak : MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val status = MeldepliktStatus.FRITAK
    }

    data object FørVedtak : MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val status = MeldepliktStatus.FØR_VEDTAK
    }

    data object UtenRett : MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val status = MeldepliktStatus.UTEN_RETT
    }

    data object FørsteMeldeperiodeMedRett : MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val status = MeldepliktStatus.FØRSTE_MELDEPERIODE_MED_RETT
    }

    data class MeldtSeg(
        val journalpostId: JournalpostId,
    ) : MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val status = MeldepliktStatus.MELDT_SEG
    }

    data object IkkeMeldtSeg : MeldepliktVurdering {
        override val utfall = Utfall.IKKE_OPPFYLT
        override val status = MeldepliktStatus.IKKE_MELDT_SEG
    }

    data object RimeligGrunnOverstyring : MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val status = MeldepliktStatus.RIMELIG_GRUNN
    }

    data object MeldtSegOverstyring : MeldepliktVurdering {
        override val utfall = Utfall.OPPFYLT
        override val status = MeldepliktStatus.MELDT_SEG
    }
}