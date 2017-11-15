-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema ffs
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema ffs
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `ffs` DEFAULT CHARACTER SET utf8 ;
USE `ffs` ;

-- -----------------------------------------------------
-- Table `ffs`.`accounts`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ffs`.`accounts` (
  `twitch_id` INT(11) NOT NULL,
  `username` VARCHAR(40) NOT NULL,
  `email` VARCHAR(320) NOT NULL,
  `views` INT(11) NOT NULL,
  `followers` INT(11) NOT NULL,
  `broadcaster_type` ENUM('affiliate', 'partner', 'none') NOT NULL,
  `url` VARCHAR(512) NOT NULL,
  `grade` INT(11) NOT NULL,
  `email_activation_key` VARCHAR(36) NULL DEFAULT NULL,
  `logo` VARCHAR(512) NULL DEFAULT NULL,
  PRIMARY KEY (`twitch_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `ffs`.`events`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ffs`.`events` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(200) NOT NULL,
  `description` VARCHAR(2048) NOT NULL,
  `status` ENUM('OPEN', 'CLOSED', 'STARTED', 'ENDED') NOT NULL,
  `reserved_to_affiliates` TINYINT(1) NOT NULL,
  `reserved_to_partners` TINYINT(1) NOT NULL,
  `is_current` TINYINT(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `ffs`.`account_event_status`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ffs`.`account_event_status` (
  `account_id` INT(11) NOT NULL,
  `event_id` INT(11) NOT NULL,
  `status` ENUM('VALIDATED', 'AWAITING_FOR_EMAIL_VALIDATION', 'AWAITING_FOR_ADMIN_VALIDATION', 'REFUSED') NOT NULL,
  `email_activation_key` VARCHAR(36) NOT NULL,
  INDEX `account_id` (`account_id` ASC),
  INDEX `event_id` (`event_id` ASC),
  PRIMARY KEY (`account_id`, `event_id`),
  CONSTRAINT `account_event_status_ibfk_1`
    FOREIGN KEY (`account_id`)
    REFERENCES `ffs`.`accounts` (`twitch_id`)
    ON DELETE CASCADE,
  CONSTRAINT `account_event_status_ibfk_2`
    FOREIGN KEY (`event_id`)
    REFERENCES `ffs`.`events` (`id`)
    ON DELETE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `ffs`.`auth_tokens`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ffs`.`auth_tokens` (
  `account_id` INT(11) NOT NULL,
  `token` VARCHAR(60) NOT NULL,
  `last_used_timestamp` BIGINT(20) NOT NULL,
  PRIMARY KEY (`token`),
  INDEX `account_id` (`account_id` ASC),
  CONSTRAINT `auth_tokens_ibfk_2`
    FOREIGN KEY (`account_id`)
    REFERENCES `ffs`.`accounts` (`twitch_id`)
    ON DELETE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `ffs`.`event_rounds`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ffs`.`event_rounds` (
  `round_id` INT(11) NOT NULL AUTO_INCREMENT,
  `event_id` INT(11) NOT NULL,
  PRIMARY KEY (`round_id`),
  INDEX `event_id` (`event_id` ASC),
  CONSTRAINT `event_rounds_ibfk_2`
    FOREIGN KEY (`event_id`)
    REFERENCES `ffs`.`events` (`id`)
    ON DELETE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `ffs`.`round_scores`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ffs`.`round_scores` (
  `round_id` INT(11) NOT NULL,
  `account_id` INT(11) NOT NULL,
  `score` INT(11) NOT NULL,
  INDEX `round_id` (`round_id` ASC),
  INDEX `account_id` (`account_id` ASC),
  PRIMARY KEY (`round_id`, `account_id`),
  CONSTRAINT `round_scores_ibfk_3`
    FOREIGN KEY (`round_id`)
    REFERENCES `ffs`.`event_rounds` (`round_id`)
    ON DELETE CASCADE,
  CONSTRAINT `round_scores_ibfk_4`
    FOREIGN KEY (`account_id`)
    REFERENCES `ffs`.`accounts` (`twitch_id`)
    ON DELETE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = latin1;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
