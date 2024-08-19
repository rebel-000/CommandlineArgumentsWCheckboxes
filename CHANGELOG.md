<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# CommandlineArgumentsWCheckboxes Changelog

## Unreleased

## 2.0.6 - 2024-08-19

### Changed

- Rider 2024.2 support

## 2.0.5 - 2024-05-13

### Fixed

- Fixed arguments save/reload (issue#20)

## 2.0.4 - 2024-05-02

### Fixed

- Fix for unreal projects support (PR#18)
- Minor fix for arguments rename/edit

## 2.0.3 - 2024-04-18

### Fixed

- Bypass C++ config args

## 2.0.2 - 2024-04-11

### Fixed

- Shared arguments sync

## 2.0.1 - 2024-04-09

### Fixed

- Default enabled state

## 2.0.0 - 2024-04-07

### Added

- Shared arguments group
- Inline arguments editor
- Quick edit arguments (up/down to edit previous/next)
- Not supported warning for not supported run configurations
- Probably bugs, but not sure

### Changed

- Reworked arguments patching logic: for project other than c++ it now overrides program parameters directly in the run configuration to support more different project types (dotnet, exe, uwp)

## 1.3.4 - 2024-03-27

### Changed

- Rider 2024.1 support

## 1.3.3 - 2023-12-07

### Changed

- Rider 2023.3 support

## 1.3.2 - 2023-08-14

### Fixed

- Args loading if toolwindow is hidden

## 1.3.1 - 2023-08-02

### Changed

- Rider 2023.2 support

## 1.3.0 - 2023-05-22

### Changed

- Use radio buttons for single choice group items
- Show root handle to be able to add new arguments to the root set

### Fixed

- Fixed drag and drop
- Fixed tree checkbox states

## 1.2.2 - 2023-04-26

### Fixed

- Fixed #6 "Hitting Enter elsewhere in Rider may bring up "Argument Properties" dialog"

## 1.2.1 - 2023-04-19

### Added

- New toolwindow icon
- Configurable shortcuts

### Changed

- Improvements for single choice groups

## 1.2.0 - 2023-04-05

### Fixed

- Disappearing elements after drag and drop
- Move up/down actions
- Duplicates when copying selected arguments to clipboard

## 1.1.0 - 2023-03-16

### Added

- Copy/Paste actions
- Configuration filters
- Single choice groups
- dotNet projects support

### Fixed

- Drag and Drop
- Save/Restore expand state

## 1.0.0 - 2023-03-09

### Added

- Initial release
