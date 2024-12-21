# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - unreleased

### Added

- Optional command line parameter `-ld` for specifying source library directory.
- Optimisation `r` for removing REM commands from source code.
- Processing flag `$` for converting hexadecimal numbers to decimal in the source code.

### Changed

- When literal label is located at the beginning of the line, but not followed by equal sign then it is considered a
  label usage instead of definition and processing doesn't throw an error.
- Literal labels can be embedded into each other to create more complex structures, infinite recursion is detected.

### Fixed

- Handling of one character variable names, previously these names could cause processing errors.
- Join line optimisation when a line started with a label then it was joined with the next line if that did not start
  with a label.

## [1.0.0] - 2024-12-12

Initial public release.