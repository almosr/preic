# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.4.0] - 2025-01-11

### Added

- Parameters `code`, `data`, `remark` and `print` to `#include` directive for reading binary files and converting them
  into source code.
- Pre-processing directives `#function` and `#call` for declaring and calling functions (subroutines) with parameters.

### Fixed

- Missing file and line info from source reading error.

## [1.3.0] - 2025-01-01

### Added

- Optimisation `i` for simplifying `IF` statements when a variable is checked against non-zero value that can be reduced
  to the variable itself. (Thanks to Valentino "SVS" Zenari for the idea.)

### Changed

- Literal labels are ordered by name instead of value in label list dump.

### Fixed

- Missing file and line info from pre-processing directive error.
- Line joining when previous line finished with an `END` command.
- Changed order of source processing steps. Previous version completed code optimisation before pre-processing and
  produced less optimal code. In this version pre-processing is completed before optimisation then line number
  assignment. This way line joining and other optimisations are producing better outcome.
- Line joining to also join following lines to a line with line label at the beginning.
- Line joining to ignore strings in double quotes when assessing whether the line could be joined with others.

## [1.2.1] - 2024-12-29

### Added

- Check for variables that are marked as frequently used to assign short BASIC names to these variables.

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