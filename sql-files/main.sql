--
-- Table structure for table `users`
--
CREATE TABLE IF NOT EXISTS `users` (
	`id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'unique auto-incrementing ID',
	`email` VARCHAR(254) NOT NULL UNIQUE COMMENT 'unique email address',
	`register_site` VARCHAR(255) NOT NULL COMMENT 'registration site name',
	`register_url` VARCHAR(2048) NOT NULL COMMENT 'registration url',
	`register_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'registration time',
	PRIMARY KEY (`id`)
) ENGINE=MyISAM;

--
-- Table structure for table `inbox`
--
CREATE TABLE IF NOT EXISTS `inbox` (
	`recipient` VARCHAR(254) COMMENT 'mail recipient',
	`sender` VARCHAR(254) NOT NULL COMMENT 'mail sender',
	`sent_date` DATETIME COMMENT 'mail sent date',
	`subject` TEXT COMMENT 'mail subject',
	`affiliation` VARCHAR(255) COMMENT 'inferred sender affiliation',
	`is_spam` TINYINT(1) COMMENT 'inferred spam flag',
	`filename` VARCHAR(255) NOT NULL COMMENT 'file location',
	PRIMARY KEY (`recipient`, `filename`)
) ENGINE=MyISAM;
