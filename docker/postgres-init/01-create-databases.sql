-- Runs once, on first container startup (empty data volume).
-- Creates one database per service; all owned by the default 'insta' user.
CREATE DATABASE userdb;
CREATE DATABASE postdb;
CREATE DATABASE notificationdb;
