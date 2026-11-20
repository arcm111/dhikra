# «برنامج المرحلة الثانية» (backup)

The detailed program that was the app's built-in default up to defaults version 9, with its own
library (145 texts, 21 groups, 5 subsections), as it was on the phone on 2026-11-17, before the
starter program «الأذكار اليومية» became the only built-in program.

- `second_stage_program.store.json`: the app's store format (`files/programs.json` of a debug
  build, and what `app/src/main/resources/default_program.json` used to be).
- `second_stage_program.dhikra.json`: the shareable program format, importable in the app from
  Settings → Import (it becomes a new program next to the others).
