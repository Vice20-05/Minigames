-- The LIMITED database user that goes inside Minigames.jar (see build.sh).
--
-- Anyone can unzip a jar and read config.properties, so the jar must NOT have the
-- postgres admin password. This user can ONLY:
--   - read the two leaderboards     (SELECT)
--   - add a new player's score       (INSERT)
--   - change a score                 (UPDATE, only the score column)
-- It can't delete rows, drop tables, or see any other table.
--
-- HOW TO USE (one time, by the project manager):
--   1. Change CHANGE_ME_TO_A_LONG_RANDOM_PASSWORD below to a new long password.
--   2. Supabase dashboard -> SQL Editor -> paste all of this -> Run.
--   3. In config.properties:
--        db.user=minigames_game.YOUR_PROJECT_REF
--        db.password=the password from step 1
--      (db.url stays the same Session pooler address.)
--   4. ./build.sh
--
-- Someone found the password in a jar and is misusing it? Make a new password with
--   ALTER ROLE minigames_game WITH PASSWORD 'a new long password';
-- put it in config.properties, run ./build.sh and upload the new zip.

CREATE ROLE minigames_game WITH LOGIN PASSWORD 'CHANGE_ME_TO_A_LONG_RANDOM_PASSWORD';

GRANT USAGE ON SCHEMA public TO minigames_game;

GRANT SELECT, INSERT ON public.f1_scores, public.tank_scores TO minigames_game;
GRANT UPDATE (score) ON public.f1_scores, public.tank_scores TO minigames_game;

-- The id column counts up by itself (a sequence), INSERT needs to use it.
-- pg_get_serial_sequence finds that sequence's real name, whatever it is.
DO $$
BEGIN
    EXECUTE format('GRANT USAGE ON SEQUENCE %s TO minigames_game',
                   pg_get_serial_sequence('public.f1_scores', 'id'));
    EXECUTE format('GRANT USAGE ON SEQUENCE %s TO minigames_game',
                   pg_get_serial_sequence('public.tank_scores', 'id'));
END $$;

-- Row Level Security ON: only users with a rule below can touch the rows. Without it,
-- Supabase's public web API (the "anon" key) could change the tables too.
-- The postgres admin user owns the tables, so it still works like before.
ALTER TABLE public.f1_scores ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tank_scores ENABLE ROW LEVEL SECURITY;

CREATE POLICY "game can read" ON public.f1_scores FOR SELECT TO minigames_game USING (true);
CREATE POLICY "game can add" ON public.f1_scores FOR INSERT TO minigames_game WITH CHECK (true);
CREATE POLICY "game can update" ON public.f1_scores FOR UPDATE TO minigames_game USING (true) WITH CHECK (true);
CREATE POLICY "game can read" ON public.tank_scores FOR SELECT TO minigames_game USING (true);
CREATE POLICY "game can add" ON public.tank_scores FOR INSERT TO minigames_game WITH CHECK (true);
CREATE POLICY "game can update" ON public.tank_scores FOR UPDATE TO minigames_game USING (true) WITH CHECK (true);
