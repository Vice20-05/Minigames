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

CREATE ROLE minigames_game WITH LOGIN PASSWORD 'CHANGE_ME_TO_A_LONG_RANDOM_PASSWORD';

GRANT USAGE ON SCHEMA public TO minigames_game;

GRANT SELECT, INSERT ON public.f1_scores, public.tank_scores TO minigames_game;
GRANT UPDATE (score) ON public.f1_scores, public.tank_scores TO minigames_game;

-- The id column counts up by itself (a sequence), INSERT needs to use it
GRANT USAGE ON SEQUENCE public.f1_scores_id_seq, public.tank_scores_id_seq TO minigames_game;

-- If Row Level Security is ON for the tables, these rules let the game user in.
-- (If it's OFF they do nothing, so it's safe to run them either way.)
CREATE POLICY "game can read" ON public.f1_scores FOR SELECT TO minigames_game USING (true);
CREATE POLICY "game can add" ON public.f1_scores FOR INSERT TO minigames_game WITH CHECK (true);
CREATE POLICY "game can update" ON public.f1_scores FOR UPDATE TO minigames_game USING (true) WITH CHECK (true);
CREATE POLICY "game can read" ON public.tank_scores FOR SELECT TO minigames_game USING (true);
CREATE POLICY "game can add" ON public.tank_scores FOR INSERT TO minigames_game WITH CHECK (true);
CREATE POLICY "game can update" ON public.tank_scores FOR UPDATE TO minigames_game USING (true) WITH CHECK (true);
