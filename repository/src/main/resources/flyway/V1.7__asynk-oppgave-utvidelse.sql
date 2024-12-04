-- clob til strukturert dokument og params

ALTER TABLE oppgave
    ADD parameters text;
ALTER TABLE oppgave
    ADD payload text;