-- Rebrand Matjarona -> Get STORE : met à jour les libellés visibles en base
-- (les migrations V18/V30 restent inchangées pour préserver leurs checksums Flyway).

UPDATE plans
SET description = REPLACE(description, 'Matjarona', 'Get STORE')
WHERE description LIKE '%Matjarona%';
