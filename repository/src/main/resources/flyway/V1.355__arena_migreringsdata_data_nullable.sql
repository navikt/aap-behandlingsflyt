-- NULL betyr at Arena svarte uten data, f.eks. at det ikke finnes refusjonskrav.
ALTER TABLE arena_migreringsdata
    ALTER COLUMN data DROP NOT NULL;
