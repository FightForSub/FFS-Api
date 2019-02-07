# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Be able to save rank into the database #17 by @azaret.  
  - Add a nullable `INT` field name rank on the table `account_event_status`.  
  - Add rank field to the endpoint `GET:/event/:id/users`.  
  - Add endpoint `PUT:/event/:id/user/:id/rank` to save rank.
- CHANGELOG.md file

## [0.9.0] - 2018-03-12
### Changed
- Allow float scores #14 by @azaret.
