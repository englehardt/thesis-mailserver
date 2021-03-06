--
-- Table structure for table `users`
--
CREATE TABLE IF NOT EXISTS `users` (
	`id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'unique auto-incrementing ID',
	`email` VARCHAR(254) NOT NULL UNIQUE COMMENT 'unique email address',
	`register_site` VARCHAR(255) NOT NULL COMMENT 'registration site name',
	`register_url` VARCHAR(2048) NOT NULL COMMENT 'registration URL',
	`register_domain` VARCHAR(255) NOT NULL COMMENT 'domain of the registration URL',
	`register_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'registration time',
	`emails_received` INT(11) UNSIGNED NOT NULL DEFAULT 0 COMMENT 'number of emails received',
	`leak_count` INT(11) UNSIGNED NOT NULL DEFAULT 0 COMMENT 'number of times this email address was leaked',
	`tp_leak_count` INT(11) UNSIGNED NOT NULL DEFAULT 0 COMMENT 'number of times this email address was leaked to a third party',
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
	`filename` VARCHAR(255) NOT NULL COMMENT 'file location',
	PRIMARY KEY (`recipient`, `filename`)
) ENGINE=MyISAM;

--
-- Table structure for table `redirects`
--
CREATE TABLE IF NOT EXISTS `redirects` (
	`sender_domain` VARCHAR(255) NOT NULL COMMENT 'domain of mail sender',
	`sender_address` VARCHAR(254) NOT NULL COMMENT 'mail sender',
	`recipient_id` INT(11) UNSIGNED NOT NULL COMMENT 'mail recipient ID',
	`request_url` VARCHAR(2048) NOT NULL COMMENT 'URL of the original request',
	`redirect_domain` VARCHAR(255) NOT NULL COMMENT 'domain of the redirected site',
	`redirect_organization` VARCHAR(255) COMMENT 'organization of the redirected site',
	`redirect_url` VARCHAR(2048) NOT NULL COMMENT 'URL of the redirected site',
	`redirect_index` INT(11) UNSIGNED NOT NULL COMMENT 'index in the redirect chain'
) ENGINE=MyISAM;

--
-- Table structure for table `leaked_emails`
--
CREATE TABLE IF NOT EXISTS `leaked_emails` (
	`sender_domain` VARCHAR(255) NOT NULL COMMENT 'domain of mail sender',
	`sender_address` VARCHAR(254) NOT NULL COMMENT 'mail sender',
	`recipient_id` INT(11) UNSIGNED NOT NULL COMMENT 'mail recipient ID',
	`encoding` VARCHAR(64) NOT NULL COMMENT 'recipient address encoding',
	`url` VARCHAR(2048) NOT NULL COMMENT 'URL containing the recipient address',
	`url_domain` VARCHAR(255) NOT NULL COMMENT 'domain of the URL',
	`url_organization` VARCHAR(255) COMMENT 'organization of the URL',
	`url_type` VARCHAR(24) NOT NULL COMMENT 'type of URL',
	`is_redirect` TINYINT(1) NOT NULL COMMENT 'whether or not the URL was a redirect',
	`is_intentional` TINYINT(1) NOT NULL COMMENT 'whether or not the leak was probably intentional'
) ENGINE=MyISAM;

--
-- Table structure for table `link_groups`
--
CREATE TABLE IF NOT EXISTS `link_groups` (
	`id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'unique auto-incrementing ID',
	`sender_domain` VARCHAR(255) NOT NULL COMMENT 'domain of mail sender',
	`sender_address` VARCHAR(254) NOT NULL COMMENT 'mail sender',
	`recipient_id` INT(11) UNSIGNED NOT NULL COMMENT 'mail recipient ID',
	`urls` TEXT NOT NULL COMMENT 'list of link URLs (delimited)',
	PRIMARY KEY (`id`)
) ENGINE=MyISAM;
