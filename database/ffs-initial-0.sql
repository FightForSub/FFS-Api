-- phpMyAdmin SQL Dump
-- version 3.4.11.1deb2+deb7u8
-- http://www.phpmyadmin.net
--
-- Client: 5.196.154.204
-- Généré le: Mer 15 Novembre 2017 à 15:58
-- Version du serveur: 5.5.46
-- Version de PHP: 5.4.45-0+deb7u11

SET FOREIGN_KEY_CHECKS=0;
SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Base de données: `ffs`
--

-- --------------------------------------------------------

--
-- Structure de la table `accounts`
--

CREATE TABLE IF NOT EXISTS `accounts` (
  `twitch_id` int(11) NOT NULL,
  `username` varchar(40) NOT NULL,
  `email` varchar(320) NOT NULL,
  `views` int(11) NOT NULL,
  `followers` int(11) NOT NULL,
  `broadcaster_type` enum('affiliate','partner','none') NOT NULL,
  `url` varchar(512) NOT NULL,
  `grade` int(11) NOT NULL,
  `email_activation_key` varchar(36) DEFAULT NULL,
  `logo` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`twitch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Structure de la table `account_event_status`
--

CREATE TABLE IF NOT EXISTS `account_event_status` (
  `account_id` int(11) NOT NULL,
  `event_id` int(11) NOT NULL,
  `status` enum('VALIDATED','AWAITING_FOR_EMAIL_VALIDATION','AWAITING_FOR_ADMIN_VALIDATION','REFUSED') NOT NULL,
  `email_activation_key` varchar(36) NOT NULL,
  KEY `account_id` (`account_id`),
  KEY `event_id` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Structure de la table `auth_tokens`
--

CREATE TABLE IF NOT EXISTS `auth_tokens` (
  `account_id` int(11) NOT NULL,
  `token` varchar(60) NOT NULL,
  `last_used_timestamp` bigint(20) NOT NULL,
  PRIMARY KEY (`token`),
  KEY `account_id` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Structure de la table `events`
--

CREATE TABLE IF NOT EXISTS `events` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL,
  `description` varchar(2048) NOT NULL,
  `status` enum('OPEN','CLOSED','STARTED','ENDED') NOT NULL,
  `reserved_to_affiliates` tinyint(1) NOT NULL,
  `reserved_to_partners` tinyint(1) NOT NULL,
  `is_current` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=9 ;

-- --------------------------------------------------------

--
-- Structure de la table `event_rounds`
--

CREATE TABLE IF NOT EXISTS `event_rounds` (
  `event_id` int(11) NOT NULL,
  `round_id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`round_id`),
  KEY `event_id` (`event_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=34 ;

-- --------------------------------------------------------

--
-- Structure de la table `round_scores`
--

CREATE TABLE IF NOT EXISTS `round_scores` (
  `round_id` int(11) NOT NULL,
  `account_id` int(11) NOT NULL,
  `score` int(11) NOT NULL,
  KEY `round_id` (`round_id`),
  KEY `account_id` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Contraintes pour les tables exportées
--

--
-- Contraintes pour la table `account_event_status`
--
ALTER TABLE `account_event_status`
  ADD CONSTRAINT `account_event_status_ibfk_2` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `account_event_status_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`twitch_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `auth_tokens`
--
ALTER TABLE `auth_tokens`
  ADD CONSTRAINT `auth_tokens_ibfk_2` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`twitch_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `event_rounds`
--
ALTER TABLE `event_rounds`
  ADD CONSTRAINT `event_rounds_ibfk_2` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `round_scores`
--
ALTER TABLE `round_scores`
  ADD CONSTRAINT `round_scores_ibfk_4` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`twitch_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `round_scores_ibfk_3` FOREIGN KEY (`round_id`) REFERENCES `event_rounds` (`round_id`) ON DELETE CASCADE;
SET FOREIGN_KEY_CHECKS=1;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
