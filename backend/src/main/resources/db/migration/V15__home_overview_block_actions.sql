ALTER TABLE home_overview_blocks
    ADD COLUMN IF NOT EXISTS max_items INTEGER NOT NULL DEFAULT 4;

ALTER TABLE home_overview_blocks
    ADD COLUMN IF NOT EXISTS cta_label VARCHAR(64);

ALTER TABLE home_overview_blocks
    ADD COLUMN IF NOT EXISTS cta_path VARCHAR(255);

UPDATE home_overview_blocks
SET max_items = CASE block_type
                    WHEN 'quick_links' THEN 6
                    ELSE 4
    END
WHERE max_items IS NULL OR max_items <= 0;

UPDATE home_overview_blocks
SET cta_label = 'Ver tarefas',
    cta_path = '/tasks'
WHERE block_type = 'in_progress'
  AND (cta_label IS NULL OR cta_label = '')
  AND (cta_path IS NULL OR cta_path = '');

UPDATE home_overview_blocks
SET cta_label = 'Abrir biblioteca',
    cta_path = '/library'
WHERE block_type = 'recent'
  AND (cta_label IS NULL OR cta_label = '')
  AND (cta_path IS NULL OR cta_path = '');

UPDATE home_overview_blocks
SET cta_label = 'Ver equipe',
    cta_path = '/users'
WHERE block_type = 'team_context'
  AND (cta_label IS NULL OR cta_label = '')
  AND (cta_path IS NULL OR cta_path = '');
