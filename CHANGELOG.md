# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.1] - unreleased

### Fixed

- Variable type was not considered when BASIC variable names are picked, now same name can be reused for different
  types.

## [1.2.0] - 2024-12-27

### Added

- Conditional section definitions using `#ifdef` - `#endif` pre-processing directives.
- Pre-processing directive `#define` and `#undef` for creating and removing pre-processing flags.
- Command line option `-d` for creating pre-processing flags.
- Optimisation `t` for removing `GOTO` command after `THEN` and `ELSE` commands. (Thanks to Rafael Gerlicze for the
  idea.)
- Processing flag `v` for using short, one character long variable names when possible. (Thanks to Csaba "Csabo"
  Pankaczy for the idea.)
- Pre-processing directive `#frequent` and `#endfrequent` for marking certain part of the code as frequently executed.
  (Thanks to Rafael Gerlicze for the idea.)
- Flag for frequently used variables to be defined as early as possible. (Thanks to Rafael Gerlicze for the idea.)

### Fixed

- Handling of space character in path names for included files.
- Remove whitespace from beginning and end of literal label values.
- Selecting BASIC variable names to accept non-alphanumeric characters in the label name, but pick valid BASIC variable
  name instead.

## [1.1.0] - 2024-12-21

### Added

- Optional command line parameter `-ld` for specifying source library directory.
- Optimisation `r` for removing REM commands from source code.
- Processing flag `$` for converting hexadecimal numbers to decimal in the source code.

### Changed

- When literal label is located at the beginning of the line, but not followed by equal sign then it is considered a
  label usage instead of definition and processing doesn't throw an error.
- Literal labels can be embedded into each other to create more complex structures, infinite recursion is detected.

### Fixed

- Handling of one character long variable names, previously these names could cause processing errors.
- Join line optimisation when a line started with a label then it was joined with the next line if that did not start
  with a label.

## [1.0.0] - 2024-12-12

Initial public release.