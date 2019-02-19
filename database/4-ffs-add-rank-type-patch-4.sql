ALTER TABLE `ffs`.`events` 
ADD COLUMN `ranking_type` ENUM('SCORE_ASC', 'SCORE_DESC') NOT NULL DEFAULT 'SCORE_ASC' AFTER `minimum_followers`;
