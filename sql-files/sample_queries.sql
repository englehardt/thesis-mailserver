--
-- Basic stats
--

/* Distinct first parties that sent us mail */
SELECT DISTINCT(`register_domain`)
FROM `users`
WHERE `emails_received` > 0;

/* Distinct first parties that leaked our email address */
SELECT DISTINCT(`register_domain`)
FROM `users`
WHERE `tp_leak_count` > 0;

/* Distinct third parties that received a leaked email address */
SELECT DISTINCT(`url_domain`)
FROM `leaked_emails`
WHERE `url_domain` != `sender_domain`
ORDER BY `url_domain` ASC;

/* Distinct first parties that contact a third party in an image redirect */
SELECT DISTINCT(`sender_domain`)
FROM `redirects`
WHERE `sender_domain` != `redirect_domain`
ORDER BY `sender_domain` ASC;

/* Distinct third parties that were contacted in an image redirect */
SELECT DISTINCT(`redirect_domain`)
FROM `redirects`
WHERE `sender_domain` != `redirect_domain`
ORDER BY `redirect_domain` ASC;

/* Distinct first parties with email links embedding an email address */
SELECT DISTINCT(`sender_domain`)
FROM `leaked_emails`
WHERE `url_type` = 'link' AND (`encoding` = 'raw' OR `encoding` = 'base64' OR `encoding` = 'urlencoded')
ORDER BY `sender_domain` ASC;

/* Distinct third parties that received a plain email address via URL/referrer/POST */
SELECT DISTINCT(`url_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `url_type` LIKE 'link-%' AND (`encoding` = 'raw' OR `encoding` = 'base64' OR `encoding` = 'urlencoded')
ORDER BY `url_domain` ASC;

/* Distinct third parties that received a plain email address via email images */
SELECT DISTINCT(`url_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `url_type` = 'image' AND (`encoding` = 'raw' OR `encoding` = 'base64' OR `encoding` = 'urlencoded')
ORDER BY `url_domain` ASC;

/* Distinct third parties that received a plain email address via URL/referrer/POST (separately) */
SELECT DISTINCT(`sender_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `url_type` = 'link-request' AND (`encoding` = 'raw' OR `encoding` = 'base64' OR `encoding` = 'urlencoded')
ORDER BY `sender_domain` ASC;
SELECT DISTINCT(`sender_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `url_type` = 'link-referrer' AND (`encoding` = 'raw' OR `encoding` = 'base64' OR `encoding` = 'urlencoded')
ORDER BY `sender_domain` ASC;
SELECT DISTINCT(`sender_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `url_type` = 'link-post' AND (`encoding` = 'raw' OR `encoding` = 'base64' OR `encoding` = 'urlencoded')
ORDER BY `sender_domain` ASC;

/* Distinct first parties that leak a plain email address via URL/referrer/POST (separately) */
SELECT DISTINCT(`url_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `url_type` = 'link-request' AND (`encoding` = 'raw' OR `encoding` = 'base64' OR `encoding` = 'urlencoded')
ORDER BY `url_domain` ASC;
SELECT DISTINCT(`url_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `url_type` = 'link-referrer' AND (`encoding` = 'raw' OR `encoding` = 'base64' OR `encoding` = 'urlencoded')
ORDER BY `url_domain` ASC;
SELECT DISTINCT(`url_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `url_type` = 'link-post' AND (`encoding` = 'raw' OR `encoding` = 'base64' OR `encoding` = 'urlencoded')
ORDER BY `url_domain` ASC;

/* Types of email address encoding used */
SELECT DISTINCT(`encoding`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain`
ORDER BY `encoding` ASC;

/* Distinct first parties that leak a hashed email address */
SELECT DISTINCT(`sender_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `encoding` != 'raw' AND `encoding` != 'base64' AND `encoding` != 'urlencoded'
ORDER BY `sender_domain` ASC;

/* Distinct third parties that received a hashed email address */
SELECT DISTINCT(`url_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `encoding` != 'raw' AND `encoding` != 'base64' AND `encoding` != 'urlencoded'
ORDER BY `url_domain` ASC;

/* Distinct third parties that received a hashed email address, not counting referrer */
SELECT DISTINCT(`url_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `encoding` != 'raw' AND `encoding` != 'base64' AND `encoding` != 'urlencoded' AND `url_type` != 'link-referrer'
ORDER BY `url_domain` ASC;

/* Distinct third parties that received a hashed email address using MD5/SHA1/SHA256/SHA384 (separately) */
SELECT DISTINCT(`sender_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `encoding` = 'md5'
ORDER BY `sender_domain` ASC;
SELECT DISTINCT(`sender_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `encoding` = 'sha1'
ORDER BY `sender_domain` ASC;
SELECT DISTINCT(`sender_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `encoding` = 'sha256'
ORDER BY `sender_domain` ASC;
SELECT DISTINCT(`sender_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `encoding` = 'sha384'
ORDER BY `sender_domain` ASC;

/* Distinct first parties that leak a hashed email address using MD5/SHA1/SHA256/SHA384 (separately) */
SELECT DISTINCT(`url_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `encoding` = 'md5'
ORDER BY `url_domain` ASC;
SELECT DISTINCT(`url_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `encoding` = 'sha1'
ORDER BY `url_domain` ASC;
SELECT DISTINCT(`url_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `encoding` = 'sha256'
ORDER BY `url_domain` ASC;
SELECT DISTINCT(`url_domain`)
FROM `leaked_emails`
WHERE `sender_domain` != `url_domain` AND `encoding` = 'sha384'
ORDER BY `url_domain` ASC;

--
-- Rankings
--

/* Create a new table to speed up queries... */
CREATE TABLE `leaked_emails_3p` AS (SELECT * FROM `leaked_emails` WHERE `url_domain` != `sender_domain`);
ALTER TABLE `leaked_emails_3p` ADD INDEX `domain` (`url_domain`);
ALTER TABLE `leaked_emails_3p` ADD INDEX `domain_type` (`url_domain`, `url_type`);
ALTER TABLE `leaked_emails_3p` ADD INDEX `org` (`url_organization`);
ALTER TABLE `leaked_emails_3p` ADD INDEX `org_type` (`url_organization`, `url_type`);

/* Overall top third parties receiving leaked email addresses */
CREATE TEMPORARY TABLE `tmp1` AS (SELECT DISTINCT(`url_domain`) FROM `leaked_emails_3p` ORDER BY `url_domain` ASC);
SELECT `tmp1`.`url_domain`, (SELECT COUNT(DISTINCT(`sender_domain`)) FROM `leaked_emails_3p` WHERE `url_domain` = `tmp1`.`url_domain`) AS `cnt` FROM `tmp1` ORDER BY `cnt` DESC;

/* Overall top organizations receiving leaked email addresses */
CREATE TEMPORARY TABLE `tmp2` AS (SELECT DISTINCT(`url_organization`) FROM `leaked_emails_3p` WHERE `url_organization` IS NOT NULL ORDER BY `url_organization` ASC);
SELECT `tmp2`.`url_organization`, (SELECT COUNT(DISTINCT(`sender_domain`)) FROM `leaked_emails_3p` WHERE `url_organization` IS NOT NULL AND `url_organization` = `tmp2`.`url_organization`) AS `cnt` FROM `tmp2` ORDER BY `cnt` DESC;

/* Top third parties contacted during image redirects */
CREATE TEMPORARY TABLE `tmp3` AS (SELECT DISTINCT(`redirect_domain`) FROM `redirects` WHERE `redirect_domain` != `sender_domain` ORDER BY `redirect_domain` ASC);
SELECT `tmp3`.`redirect_domain`, (SELECT COUNT(DISTINCT(`sender_domain`)) FROM `redirects` WHERE `redirect_domain` != `sender_domain` AND `redirect_domain` = `tmp3`.`redirect_domain`) AS `cnt` FROM `tmp3` ORDER BY `cnt` DESC;

/* Top first parties contacting third parties during image redirects */
CREATE TEMPORARY TABLE `tmp4` AS (SELECT DISTINCT(`sender_domain`) FROM `redirects` ORDER BY `sender_domain` ASC);
SELECT `tmp4`.`sender_domain`, (SELECT COUNT(DISTINCT(`redirect_domain`)) FROM `redirects` WHERE `redirect_domain` != `sender_domain` AND `sender_domain` = `tmp4`.`sender_domain`) AS `cnt` FROM `tmp4` ORDER BY `cnt` DESC;

/* Top third parties receiving plain email addresses via referrer */
CREATE TEMPORARY TABLE `tmp5` AS (SELECT DISTINCT(`url_domain`) FROM `leaked_emails_3p` WHERE `url_type` = 'link-referrer' ORDER BY `url_domain` ASC);
SELECT `tmp5`.`url_domain`, (SELECT COUNT(DISTINCT(`sender_domain`)) FROM `leaked_emails_3p` WHERE `url_domain` = `tmp5`.`url_domain` AND `url_type` = 'link-referrer') AS `cnt` FROM `tmp5` ORDER BY `cnt` DESC;

/* Top organizations receiving plain email addresses via referrer */
CREATE TEMPORARY TABLE `tmp6` AS (SELECT DISTINCT(`url_organization`) FROM `leaked_emails_3p` WHERE `url_organization` IS NOT NULL AND `url_type` = 'link-referrer' ORDER BY `url_organization` ASC);
SELECT `tmp6`.`url_organization`, (SELECT COUNT(DISTINCT(`sender_domain`)) FROM `leaked_emails_3p` WHERE `url_organization` IS NOT NULL AND `url_organization` = `tmp6`.`url_organization` AND `url_type` = 'link-referrer') AS `cnt` FROM `tmp6` ORDER BY `cnt` DESC;

/* Top third parties receiving plain email addresses via URL/post */
CREATE TEMPORARY TABLE `tmp7` AS (SELECT DISTINCT(`url_domain`) FROM `leaked_emails_3p` WHERE `url_type` = 'link-request' OR `url_type` = 'link-post' ORDER BY `url_domain` ASC);
SELECT `tmp7`.`url_domain`, (SELECT COUNT(DISTINCT(`sender_domain`)) FROM `leaked_emails_3p` WHERE `url_domain` = `tmp7`.`url_domain` AND (`url_type` = 'link-request' OR `url_type` = 'link-post')) AS `cnt` FROM `tmp7` ORDER BY `cnt` DESC;

/* Top organizations receiving plain email addresses via URL/post */
CREATE TEMPORARY TABLE `tmp8` AS (SELECT DISTINCT(`url_organization`) FROM `leaked_emails_3p` WHERE `url_organization` IS NOT NULL AND (`url_type` = 'link-request' OR `url_type` = 'link-post') ORDER BY `url_organization` ASC);
SELECT `tmp8`.`url_organization`, (SELECT COUNT(DISTINCT(`sender_domain`)) FROM `leaked_emails_3p` WHERE `url_organization` IS NOT NULL AND `url_organization` = `tmp8`.`url_organization` AND (`url_type` = 'link-request' OR `url_type` = 'link-post')) AS `cnt` FROM `tmp8` ORDER BY `cnt` DESC;

/* Top third parties receiving hashed email addresses, not counting referrer */
CREATE TEMPORARY TABLE `tmp9` AS (SELECT DISTINCT(`url_domain`) FROM `leaked_emails_3p` WHERE `encoding` != 'raw' AND `encoding` != 'base64' AND `encoding` != 'urlencoded' AND `url_type` != 'link-referrer' ORDER BY `url_domain` ASC);
SELECT `tmp9`.`url_domain`, (SELECT COUNT(DISTINCT(`sender_domain`)) FROM `leaked_emails_3p` WHERE `url_domain` = `tmp9`.`url_domain` AND `encoding` != 'raw' AND `encoding` != 'base64' AND `encoding` != 'urlencoded' AND `url_type` != 'link-referrer') AS `cnt` FROM `tmp9` ORDER BY `cnt` DESC;

/* Top organizations receiving hashed email addresses, not counting referrer */
CREATE TEMPORARY TABLE `tmp10` AS (SELECT DISTINCT(`url_organization`) FROM `leaked_emails_3p` WHERE `url_organization` IS NOT NULL AND `encoding` != 'raw' AND `encoding` != 'base64' AND `encoding` != 'urlencoded' AND `url_type` != 'link-referrer' ORDER BY `url_organization` ASC);
SELECT `tmp10`.`url_organization`, (SELECT COUNT(DISTINCT(`sender_domain`)) FROM `leaked_emails_3p` WHERE `url_organization` IS NOT NULL AND `url_organization` = `tmp10`.`url_organization` AND `encoding` != 'raw' AND `encoding` != 'base64' AND `encoding` != 'urlencoded' AND `url_type` != 'link-referrer') AS `cnt` FROM `tmp10` ORDER BY `cnt` DESC;

/* Top first parties sending hashed email addresses, not counting referrer */
CREATE TEMPORARY TABLE `tmp11` AS (SELECT DISTINCT(`sender_domain`) FROM `leaked_emails_3p` WHERE `encoding` != 'raw' AND `encoding` != 'base64' AND `encoding` != 'urlencoded' AND `url_type` != 'link-referrer' ORDER BY `sender_domain` ASC);
SELECT `tmp11`.`sender_domain`, (SELECT COUNT(DISTINCT(`url_domain`)) FROM `leaked_emails_3p` WHERE `sender_domain` = `tmp11`.`sender_domain` AND `encoding` != 'raw' AND `encoding` != 'base64' AND `encoding` != 'urlencoded' AND `url_type` != 'link-referrer') AS `cnt` FROM `tmp11` ORDER BY `cnt` DESC;
